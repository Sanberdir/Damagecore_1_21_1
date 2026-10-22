package ru.imaginaerum.damagecore.Init.fishing.fight;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Init.fishing.net.FightPosPayload;
import ru.imaginaerum.damagecore.Init.fishing.net.FightStatePayload;
import ru.imaginaerum.damagecore.mixin.fishing.FishingHookAccessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * One fight between a player and a hooked fish.
 *
 * Model (all tunables are constants below):
 *  - the fish periodically "dashes" left or right, pushing the bobber sideways and raising line tension;
 *  - mouse X pushes the bobber sideways (move the mouse against the dash to hold the fish);
 *  - mouse Y: up = pull (more tension, faster reeling), down = give slack (less tension);
 *  - tension inside the safe zone slowly reels the fish in; too high for too long = snap, too low = fish escapes.
 */
public final class FishFight {
    // ---- tunables ----

    static final float MAX_INPUT = 60.0f;       // max mouse pixels counted per tick
    public static final float SAFE_HIGH = 0.71f;
    static final float BASE_TENSION = 0.575f;   // спокойное натяжение, центр зелёной зоны
    static final float SNAP_LEVEL = 0.92f;
    static final int SNAP_TICKS = 24;
    private final ServerPlayer player;
    private final ServerLevel level;
    private final FishingHook hook;
    private final FishingHookAccessor hookAcc;
    private final InteractionHand hand;
    private final List<ItemStack> loot;
    static final float RUN_SPEED = 0.11f;    // блоков/тик при полной силе рыбы (~2 блока/с)
    static final float REEL_SPEED = 0.085f;  // скорость подмотки в окне
    static final float MAX_LAT = 8.0f;       // дальше в сторону - сорвалась
    static final float MAX_OUT = 9.0f;       // дальше от места поклёвки - сорвалась

    private int phase;                       // 0 отдых (окно), 1 замах, 2 рывок
    private int runAge;
    private boolean keyReel;
    @Nullable
    private final Entity visual;
    private final float strength;
    private final Vec3 baseDir;
    private final Vec3 right;
    private final double startDist;
    private final double endDist;
    private final double baseY;
    private int dashAge;
    private final boolean real;
    private float lat, latVel, tension = BASE_TENSION, progress, pull, wander;
    private float inputX, inputY;
    private int dashLat, dashOut, phaseTicks, ticks, over, slack;
    private double dist;
    private float distVel;
    private float dashPower;
    private boolean finished;
    private double vx, vy, vz;
    private float vyaw;
    private double prevHx, prevHz;
    private double mvx, mvz;          // сглаженная скорость крючка
    private boolean yawInit;     // насколько дальше исходной точки она может утащить


    // шанс, что следующее действие рыбы - нырок
    private float rod, stamina = 1f;
    private boolean diving;
    private float diveDepth;
    private int keySide;
    private boolean keyTighten, keySlack;
    public void setKeys(int side, boolean tighten, boolean slack, boolean reel) {
        this.keySide = Mth.clamp(side, -1, 1);
        this.keyTighten = tighten;
        this.keySlack = slack && !tighten;
        this.keyReel = reel;
    }
    private FishFight(ServerPlayer player, FishingHook hook, InteractionHand hand, List<ItemStack> loot,
                      @Nullable Entity visual, boolean real, float strength, Vec3 baseDir,
                      double startDist, double endDist) {
        this.player = player;
        this.level = player.serverLevel();
        this.hook = hook;
        this.hookAcc = (FishingHookAccessor) hook;
        this.hand = hand;
        this.loot = loot;
        this.real = real;
        this.visual = visual;
        this.strength = strength;
        this.baseDir = baseDir;
        this.right = new Vec3(-baseDir.z, 0, baseDir.x);
        this.startDist = startDist;
        this.baseY = hook.getY();
        this.endDist = endDist;
        this.dist = startDist;
        this.phaseTicks = 20 + player.getRandom().nextInt(15);
    }

    /** Tries to start a fight for a hook that currently has a bite. Returns null if vanilla behaviour should be used. */
    @Nullable
    public static FishFight start(ServerPlayer player, FishingHook hook, @Nullable FightManager.Approach pre) {
        InteractionHand hand;
        if (player.getMainHandItem().getItem() instanceof FishingRodItem) {
            hand = InteractionHand.MAIN_HAND;
        } else if (player.getOffhandItem().getItem() instanceof FishingRodItem) {
            hand = InteractionHand.OFF_HAND;
        } else {
            return null;
        }

        Vec3 diff = hook.position().subtract(player.position());
        double len = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (len < 1.0) {
            return null;
        }
        Vec3 baseDir = new Vec3(diff.x / len, 0, diff.z / len);

        ServerLevel level = player.serverLevel();
        ItemStack rod = player.getItemInHand(hand);
        List<ItemStack> loot = pre != null ? pre.loot : rollLoot(player, hook, rod);
        if (loot.isEmpty()) {
            return null;
        }

        ItemStack first = loot.get(0);
        float strength = strengthFor(first);
        Entity visual = (pre != null && pre.fish != null) ? pre.fish : createVisual(level, hook, first);
        double endDist = Math.min(1.8, len * 0.5);

        boolean real = pre != null && pre.fish != null && pre.real;
        FishFight fight = new FishFight(player, hook, hand, loot, visual, real, strength, baseDir, len, endDist);
        fight.sendState(true);
        return fight;
    }

    static List<ItemStack> rollLoot(ServerPlayer player, FishingHook hook, ItemStack rod) {
        ServerLevel level = player.serverLevel();
        FishingHookAccessor acc = (FishingHookAccessor) hook;
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withLuck(acc.deepfishing$getLuck() + player.getLuck())
                .create(LootContextParamSets.FISHING);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        return new ArrayList<>(table.getRandomItems(params));
    }

    static boolean isMobFish(ItemStack s) {
        return s.is(Items.COD) || s.is(Items.SALMON) || s.is(Items.PUFFERFISH) || s.is(Items.TROPICAL_FISH);
    }

    private static float strengthFor(ItemStack s) {
        if (s.is(Items.PUFFERFISH)) return 1.4f;
        if (s.is(Items.SALMON)) return 1.2f;
        if (s.is(Items.COD) || s.is(Items.TROPICAL_FISH)) return 1.0f;
        return 0.6f; // junk / treasure: heavy but passive
    }

    @Nullable
    static Entity createVisual(ServerLevel level, FishingHook hook, ItemStack first) {
        EntityType<?> type = null;
        if (first.is(Items.COD)) type = EntityType.COD;
        else if (first.is(Items.SALMON)) type = EntityType.SALMON;
        else if (first.is(Items.PUFFERFISH)) type = EntityType.PUFFERFISH;
        else if (first.is(Items.TROPICAL_FISH)) type = EntityType.TROPICAL_FISH;

        Entity e;
        if (type != null) {
            e = type.create(level);
        } else {
            ItemEntity ie = new ItemEntity(level, hook.getX(), hook.getY() - 0.25, hook.getZ(), first.copy());
            ie.setPickUpDelay(32767);
            ie.setUnlimitedLifetime();
            e = ie;
        }
        if (e == null) {
            return null;
        }
        e.setNoGravity(true);
        e.setInvulnerable(true);
        e.setSilent(true);
        e.addTag(FightManager.TAG);
        if (e instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        e.setPos(hook.getX(), hook.getY() - 0.28, hook.getZ());
        FightManager.LIVE_VISUALS.add(e.getUUID());
        level.addFreshEntity(e);
        return e;
    }

    public void addInput(float dx, float dy) {
        inputX = Mth.clamp(inputX + dx, -MAX_INPUT * 2, MAX_INPUT * 2);
        inputY = Mth.clamp(inputY + dy, -MAX_INPUT * 2, MAX_INPUT * 2);
    }

    /** Called every server tick. Returns false when the fight is over. */
    public boolean tick() {
        if (finished) {
            return false;
        }
        if (hook.isRemoved() || player.fishing != hook || player.isRemoved() || !player.isAlive()
                || hook.level() != player.level()
                || !(player.getItemInHand(hand).getItem() instanceof FishingRodItem)) {
            abort();
            return false;
        }
        ticks++;
        updateFish(player.getRandom());
        wander = Mth.sin(ticks * 0.21f) * 0.6f + Mth.sin(ticks * 0.07f) * 0.4f;

        boolean warnPhase = phase == 1;
        boolean run = phase == 2;
        float st = strength < 0.7f ? 0f : stamina;
        float vigor = 0.35f + 0.65f * st;
        boolean reeling = keyReel && ticks > 10;      // первые 0.5 с после подсечки не мотаем

        runAge = run ? runAge + 1 : 0;
        float ramp = Mth.clamp(runAge / 6f, 0f, 1f);

        // ---- сопротивление: по осям независимо ----
        float cLat = 0f, cOut = 0f;      // 1 = держишь, <0 = поддаёшься
        if (run) {
            if (dashLat != 0) {
                cLat = keySide == -dashLat ? 1f : (keySide == dashLat ? -0.3f : 0f);
            }
            if (dashOut != 0) {
                cOut = keyTighten ? 1f : (keySlack ? -0.3f : 0f);
            }
        }
        int axes = (dashLat != 0 ? 1 : 0) + (dashOut != 0 ? 1 : 0);
        float c = axes == 0 ? 0f : Mth.clamp((cLat + cOut) / axes, -0.3f, 1f);

        // ---- скорость рыбы и подмотка ----
        float power = dashPower * strength * vigor;
        float diag = axes == 2 ? 0.75f : 1f;
        float baseSpeed = run ? RUN_SPEED * power * ramp * diag : 0f;
        float speedLat = baseSpeed * (1f - 0.95f * cLat);
        float speedOut = baseSpeed * (1f - 0.95f * cOut);
        float reelPow = REEL_SPEED * (0.6f + 0.8f * (1f - st)) / (0.8f + 0.2f * strength);

        float vLat = dashLat * speedLat + (run ? 0f : wander * 0.012f * vigor);
        float vOut = dashOut * speedOut + (run ? 0f : 0.004f * vigor);

        // игрок может вести рыбу всегда, но во время рывка в 2.5 раза слабее
        float pullK = run ? 0.4f : 1f;
        vLat += keySide * 0.03f * pullK;                       // A/D ведут рыбу в сторону нажатия
        if (keyTighten) vOut -= reelPow * 0.3f * (run ? 1.3f : 1f); // S подтягивает
        if (keySlack) vOut += 0.03f;                           // W травит леску
        if (reeling) vOut -= run ? reelPow * 0.4f : (warnPhase ? reelPow * 0.5f : reelPow);

        latVel += (vLat - latVel) * 0.25f;
        distVel += (vOut - distVel) * 0.25f;

        // ---- натяжение ----
        float load = run ? Math.min(power, 1.3f) : 0f;
        float tTarget = 0.30f
                + 0.10f * load
                + 0.30f * load * Math.max(c, 0f)       // держишь рывок: леска нагружена
                + (reeling ? (run ? 0.30f : 0.10f) : 0f) // мотать во время рывка опасно
                - (keySlack ? 0.25f : 0f);
        tension = Mth.clamp(tension + (tTarget - tension) * 0.2f, 0f, 1f);

        if (tension > 0.7f && ticks % 8 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_CLICK_ON,
                    SoundSource.PLAYERS, 0.6f, 1.4f + (tension - 0.7f) * 3f);
        }

        // ---- усталость ----
        if (strength >= 0.7f) {
            if (run) {
                stamina -= 0.0012f + 0.0035f * Math.max(c, 0f);   // чем лучше держишь, тем быстрее устаёт
            } else if (!warnPhase) {
                stamina += reeling ? -0.0012f : 0.0005f;
            }
            stamina = Mth.clamp(stamina, 0f, 1f);
        }

        // ---- движение ----
        Vec3 o = player.position();
        float nl = lat + latVel;
        double nd = dist + distVel;

        if (!waterAt(o, nd, nl)) {
            boolean dOk = waterAt(o, nd, lat);
            boolean lOk = waterAt(o, dist, nl);
            if (dOk) {
                nl = lat;
                latVel *= -0.4f;
            } else if (lOk) {
                if (distVel < 0) { win(); return false; }    // подтянули к берегу
                nd = dist;
                distVel *= -0.3f;
            } else {
                if (distVel < 0) { win(); return false; }
                nl = lat;
                nd = dist;
                latVel *= -0.4f;
                distVel *= -0.3f;
            }
        }
        lat = nl;
        dist = nd;

        // ---- конец боя ----
        if (dist > startDist + MAX_OUT || Math.abs(lat) > MAX_LAT) {
            escape();
            return false;
        }
        progress = (float) Mth.clamp((startDist - dist) / (startDist - endDist), 0.0, 1.0);
        if (dist <= endDist) {
            progress = 1f;
            win();
            return false;
        }
        over = tension > SNAP_LEVEL ? over + 1 : Math.max(0, over - 2);
        if (over >= SNAP_TICKS) {
            snap();
            return false;
        }

        // ---- позиция ----
        double x = o.x + baseDir.x * dist + right.x * lat;
        double z = o.z + baseDir.z * dist + right.z * lat;
        double dip = run ? 0.10 * Math.abs(Mth.sin(ticks * 0.8f)) : 0.03 * Math.abs(Mth.sin(ticks * 0.3f));
        double y = baseY + (tension - 0.4f) * 0.15 - dip;
        hook.setPos(x, y, z);
        hook.setDeltaMovement(Vec3.ZERO);
        hookAcc.deepfishing$setNibble(20);
        moveVisual(x, y, z);
        sendPos(x, y, z);

        if (ticks % 2 == 0) {
            sendState(true);
        }
        return true;
    }
    private boolean waterAt(Vec3 o, double d, double l) {
        return isWater(o.x + baseDir.x * d + right.x * l, o.z + baseDir.z * d + right.z * l);
    }
    private void updateFish(RandomSource r) {
        if (strength < 0.7f) {                 // мусор и сокровища: пассивный груз
            phase = 0;
            dashLat = 0;
            dashOut = 0;
            dashPower = 0f;
            return;
        }
        if (--phaseTicks > 0) return;

        switch (phase) {
            case 0 -> {                        // отдых -> замах
                if (stamina < 0.12f) {         // выдохлась, больше не рвётся
                    phaseTicks = 40;
                    return;
                }
                int pl = dashLat, po = dashOut;
                do {
                    switch (r.nextInt(5)) {
                        case 0 -> { dashLat = -1; dashOut = 0; }
                        case 1 -> { dashLat = 1;  dashOut = 0; }
                        case 2 -> { dashLat = 0;  dashOut = 1; }
                        case 3 -> { dashLat = -1; dashOut = 1; }
                        default -> { dashLat = 1; dashOut = 1; }
                    }
                } while (dashLat == pl && dashOut == po);
                dashPower = 0.8f + r.nextFloat() * 0.2f;
                phase = 1;
                phaseTicks = 10;               // 0.5 с на реакцию
                level.playSound(null, hook.getX(), hook.getY(), hook.getZ(), SoundEvents.FISHING_BOBBER_SPLASH,
                        SoundSource.NEUTRAL, 0.7f, 1.0f + r.nextFloat() * 0.3f);
                level.sendParticles(ParticleTypes.SPLASH, hook.getX(), hook.getY(), hook.getZ(), 10, 0.2, 0.05, 0.2, 0.1);
            }
            case 1 -> {                        // замах -> рывок
                phase = 2;
                runAge = 0;
                phaseTicks = 45 + r.nextInt(40);   // 2.2-4.2 с
            }
            default -> {                       // рывок -> отдых (окно)
                phase = 0;
                dashLat = 0;
                dashOut = 0;
                dashPower = 0f;
                phaseTicks = (int) ((30 + r.nextInt(20)) * (1f + 1.5f * (1f - stamina)));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.35f, 1.7f);
            }
        }
    }

    private boolean isWater(double x, double z) {
        return level.getFluidState(BlockPos.containing(x, baseY, z)).is(FluidTags.WATER);
    }


    private void moveVisual(double hx, double hy, double hz) {
        // скорость крючка за тик, со сглаживанием
        if (!yawInit) {
            prevHx = hx;
            prevHz = hz;
            vyaw = (float) (Mth.atan2(-baseDir.x, baseDir.z) * 180.0 / Math.PI);
            yawInit = true;
        }
        mvx = mvx * 0.75 + (hx - prevHx) * 0.25;
        mvz = mvz * 0.75 + (hz - prevHz) * 0.25;
        prevHx = hx;
        prevHz = hz;

        // поворачиваем рыбу туда, куда её ведут (если есть заметное движение)
        if (mvx * mvx + mvz * mvz > 1.0E-5) {
            float target = (float) (Mth.atan2(-mvx, mvz) * 180.0 / Math.PI);
            vyaw = Mth.approachDegrees(vyaw, target, 12.0f);   // макс. 12° за тик, поворот плавный
        }

        vx = hx;
        vy = hy;
        vz = hz;
        if (visual == null || visual.isRemoved()) {
            return;
        }

        if (visual instanceof ItemEntity) {
            vy = hy - 0.25;
        } else {
            // смещаем рыбу назад от носа, чтобы крючок был у рта
            double rad = Math.toRadians(vyaw);
            vx = hx + Math.sin(rad) * 0.3;
            vy = hy - 0.28;
            vz = hz - Math.cos(rad) * 0.3;
            visual.setYRot(vyaw);
            visual.yRotO = vyaw;
            visual.setXRot(0f);
            if (visual instanceof LivingEntity le) {
                le.yBodyRot = vyaw;
                le.yBodyRotO = vyaw;
                le.yHeadRot = vyaw;
                le.yHeadRotO = vyaw;
            }
        }
        visual.setPos(vx, vy, vz);
        visual.setDeltaMovement(Vec3.ZERO);
    }
    private void sendPos(double hx, double hy, double hz) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(hook, new FightPosPayload(
                hook.getId(), visual == null ? -1 : visual.getId(),
                hx, hy, hz, vx, vy, vz, vyaw));
    }



    private void sendState(boolean active) {
        PacketDistributor.sendToPlayer(player,
                new FightStatePayload(active, tension, progress, lat / MAX_LAT, (byte) phase, (byte) 0));
    }

    private EquipmentSlot slot() {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    // ---- endings ----

    private void win() {
        finished = true;
        ItemStack rod = player.getItemInHand(hand);
        ItemFishedEvent event = new ItemFishedEvent(new ArrayList<>(loot), hook.onGround() ? 2 : 1, hook);
        NeoForge.EVENT_BUS.post(event);

        if (!event.isCanceled()) {
            for (ItemStack stack : event.getDrops()) {
                ItemStack s = stack.copy();
                player.getInventory().add(s);
                if (!s.isEmpty()) {
                    player.drop(s, false);       // инвентарь полон
                }
                if (stack.is(ItemTags.FISHES)) {
                    player.awardStat(Stats.FISH_CAUGHT, 1);
                }
            }
            player.giveExperiencePoints(level.getRandom().nextInt(6) + 1);
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, event.getDrops());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.4f, 1.0f + level.getRandom().nextFloat() * 0.4f);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL, 1.0f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        cleanup();
        hook.discard();
        rod.hurtAndBreak(event.getRodDamage(), player, slot());
    }

    private void snap() {
        finished = true;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK,
                SoundSource.PLAYERS, 0.8f, 1.2f);
        ItemStack rod = player.getItemInHand(hand);
        cleanup(true);
        hook.discard();
        rod.hurtAndBreak(2, player, slot());
    }

    private void escape() {
        finished = true;
        level.playSound(null, hook.getX(), hook.getY(), hook.getZ(), SoundEvents.FISHING_BOBBER_SPLASH,
                SoundSource.NEUTRAL, 0.8f, 0.8f);
        cleanup(true);
        hook.discard();
    }

    public void release() {
        if (finished) return;
        finished = true;
        hookAcc.deepfishing$setNibble(0);
        cleanup(true);
    }

    public void abort() {
        if (finished) return;
        finished = true;
        if (!hook.isRemoved()) {
            hookAcc.deepfishing$setNibble(0);
        }
        cleanup(true);
    }

    private void cleanup() {
        cleanup(false);   // только для win(): пойманная рыба становится предметом
    }

    private void cleanup(boolean keepFish) {
        if (visual != null) {
            FightManager.LIVE_VISUALS.remove(visual.getUUID());
            boolean keep = real && keepFish && !visual.isRemoved();
            if (keep) {
                visual.setDeltaMovement(Vec3.ZERO);
                FightManager.releaseFish(visual);
            } else {
                visual.discard();
            }
        }
        sendState(false);
    }
}

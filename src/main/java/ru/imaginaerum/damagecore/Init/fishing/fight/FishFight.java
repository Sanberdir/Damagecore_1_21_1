package ru.imaginaerum.damagecore.Init.fishing.fight;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import ru.imaginaerum.damagecore.sounds.DCSoundEvents;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Бой в стиле Sea of Thieves.
 *
 * Цикл: WARN (0.5 с) -> STRUGGLE [3 даша × 1.75 с] -> WINDOW (3–4.5 с) -> WARN ...
 *  - Каждый даш: рыба рвётся в одном из 5 направлений.
 *  - Игрок ОБЯЗАН отклонить удочку противоположно рыбе (A/D/S).
 *  - Не отклонил -> tension растёт к snap; рыба уходит в сторону/вперёд.
 *  - Отклонил правильно -> рыба тормозит до 15%, tension падает, stamina ↓.
 *  - Stamina < 0.35 ИЛИ 3 даша -> WINDOW: рыба замирает, ЛКМ мотает.
 *  - ЛКМ во время STRUGGLE -> мгновенный snap (крупная рыба рвёт сразу).
 *  - WASD НИКОГДА не тянут рыбу, только сопротивляются.
 */
public final class FishFight {
    // ── tunables ──
    static final float BASE_TENSION = 0.42f;
    static final float SNAP_LEVEL   = 0.92f;
    static final int    SNAP_TICKS        = 22;    // было 15
    static final int    SNAP_GRACE        = 10;    // тиков после начала рывка без накопления обрыва
    static final double SHORE_ZONE        = 5.0;   // на таком расстоянии от берега рыба слабеет
    static final float  SHORE_MIN         = 0.35f; // минимальная сила рыбы у самого берега
    static final int    MIN_DASHES        = 2;     // минимум рывков в борьбе
    static final float  WINDOW_RESTORE    = 0.55f; // восстановление силы за окно
    static final float  WINDOW_MIN_STAMINA = 0.90f;

    private int rounds;
    static final float REEL_LOAD_RISE = 0.025f;  // за тик, x сила рыбы x близость к берегу; ~2,5 с до обрыва у сильной рыбы
    static final float REEL_LOAD_FALL = 0.04f;   // за тик, когда не мотаешь (W удваивает)
    static final float JERK_LOAD      = 0.20f;   // тап ЛКМ вне борьбы тоже нагружает леску

    private float reelLoad;
    private double shX, shY, shZ;
    static final float RUN_SPEED   = 0.22f;   // скорость рыбы в даше
    static final float REEL_SPEED  = 0.10f;   // скорость подмотки в окне
    static final float MAX_LAT     = 15.0f;   // защитный кап
    static final float MAX_OUT     = 10.0f;   // дальше вперёд — срыв

    static final int PHASE_WARN     = 1;
    static final int PHASE_STRUGGLE = 2;
    static final int PHASE_WINDOW   = 3;
    static final int   TAP_TICKS      = 7;      // ЛКМ короче этого = тап (рывок)
    static final int   RELEASE_HOLD   = 30;     // удержание ЛКМ = отпустить (1,5 с)
    static final int   JERK_TICKS     = 5;      // сколько тиков длится рывок
    static final float JERK_SPEED     = 0.35f;  // сила рывка (блоков/тик в расчёте)
    static final int   JERK_COOLDOWN  = 15;     // пауза между рывками
    static final float JERK_STRAIN    = 0.30f;  // рывок в борьбе резко добавляет натяжение
    private boolean keyAttack;
    private int attackTicks, jerkTicks, jerkCd;
    static final int   WARN_TICKS        = 10;
    static final int   DASH_TICKS        = 35;
    static final int   DASHES_PER_STRUGGLE = 3;
    static final float TIRED_THRESHOLD   = 0.35f;
    static final float STAMINA_PER_TICK  = 0.008f;   // при полном counter
    static final int   REEL_GRACE        = 5;        // тиков после старта struggle, когда reel не рвёт
    static final float[] CREAK_AT = {0.40f, 0.58f, 0.74f, 0.90f};   // четыре ступени
    private int creakStage;
    private final ServerPlayer player;
    private final ServerLevel level;
    private final FishingHook hook;
    private final FishingHookAccessor hookAcc;
    private final InteractionHand hand;
    private final List<ItemStack> loot;

    @Nullable private final Entity visual;
    private final float strength;
    private final Vec3 baseDir;
    private final Vec3 right;
    private final double startDist;
    private final double endDist;
    private final double baseY;
    private final boolean real;

    private int phase;
    private int phaseTicks;
    private int ticks;
    private int over;
    private int dashIndex;
    private int struggleTicks;
    private int runAge;
    private float lat, latVel;
    private float tension = BASE_TENSION;
    private float progress;
    private float stamina = 1f;

    private double dist;
    private float distVel;
    static final float STRAIN_RISE = 0.025f;  // за тик, x strength. 0.025 = ~2 с для обычной рыбы
    static final float STRAIN_FALL = 0.06f;   // за тик, как быстро отпускает
    static final double SHAKE_AMP  = 0.18;
    private float strain;
    private int dashLat, dashOut;
    private float dashPower;
    private int keySide;
    private boolean keyTighten, keySlack, keyReel;

    private boolean finished;

    private double vx, vy, vz;
    private float vyaw;
    private double prevHx, prevHz;
    private double mvx, mvz;
    private boolean yawInit;

    public void setKeys(int side, boolean tighten, boolean slack, boolean reel, boolean attack) {
        this.keySide    = Mth.clamp(side, -1, 1);
        this.keyTighten = tighten;
        this.keySlack   = slack && !tighten;
        this.keyReel    = reel;
        this.keyAttack  = attack;
    }

    private FishFight(ServerPlayer player, FishingHook hook, InteractionHand hand, List<ItemStack> loot,
                      @Nullable Entity visual, boolean real, float strength, Vec3 baseDir,
                      double startDist, double endDist) {
        this.player    = player;
        this.level     = player.serverLevel();
        this.hook      = hook;
        this.hookAcc   = (FishingHookAccessor) hook;
        this.hand      = hand;
        this.loot      = loot;
        this.real      = real;
        this.visual    = visual;
        this.strength  = strength;
        this.baseDir   = baseDir;
        this.right     = new Vec3(-baseDir.z, 0, baseDir.x);
        this.startDist = startDist;
        this.baseY     = hook.getY();
        this.endDist   = endDist;
        this.dist      = startDist;
        this.phase     = PHASE_WARN;
        this.phaseTicks = WARN_TICKS;
    }

    @Nullable
    public static FishFight start(ServerPlayer player, FishingHook hook, @Nullable FightManager.Approach pre) {
        InteractionHand hand;
        if (player.getMainHandItem().getItem() instanceof FishingRodItem) hand = InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem().getItem() instanceof FishingRodItem) hand = InteractionHand.OFF_HAND;
        else return null;

        Vec3 diff = hook.position().subtract(player.position());
        double len = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (len < 1.0) return null;
        Vec3 baseDir = new Vec3(diff.x / len, 0, diff.z / len);

        ServerLevel level = player.serverLevel();
        ItemStack rod = player.getItemInHand(hand);
        List<ItemStack> loot = pre != null ? pre.loot : rollLoot(player, hook, rod);
        if (loot.isEmpty()) return null;

        ItemStack first = loot.get(0);
        float strength = strengthFor(first);
        Entity visual = (pre != null && pre.fish != null) ? pre.fish : createVisual(level, hook, first);
        double endDist = Math.min(1.8, len * 0.5);
        boolean real = pre != null && pre.fish != null && pre.real;

        FishFight fight = new FishFight(player, hook, hand, loot, visual, real, strength, baseDir, len, endDist);
        level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6f, 1.1f);
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
        if (s.is(Items.SALMON))     return 1.2f;
        if (s.is(Items.COD) || s.is(Items.TROPICAL_FISH)) return 1.0f;
        return 0.6f;
    }

    @Nullable
    static Entity createVisual(ServerLevel level, FishingHook hook, ItemStack first) {
        return createVisual(level, first, hook.getX(), hook.getY() - 0.28, hook.getZ());
    }

    @Nullable
    static Entity createVisual(ServerLevel level, ItemStack first, double x, double y, double z) {
        EntityType<?> type = null;
        if (first.is(Items.COD)) type = EntityType.COD;
        else if (first.is(Items.SALMON)) type = EntityType.SALMON;
        else if (first.is(Items.PUFFERFISH)) type = EntityType.PUFFERFISH;
        else if (first.is(Items.TROPICAL_FISH)) type = EntityType.TROPICAL_FISH;

        Entity e;
        if (type != null) {
            e = type.create(level);
        } else {
            ItemEntity ie = new ItemEntity(level, x, y, z, first.copy());
            ie.setPickUpDelay(32767);
            ie.setUnlimitedLifetime();
            e = ie;
        }
        if (e == null) return null;
        e.setNoGravity(true);
        e.setInvulnerable(true);
        e.setSilent(true);
        e.addTag(FightManager.TAG);
        if (e instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        e.setPos(x, y, z);
        FightManager.LIVE_VISUALS.add(e.getUUID());
        level.addFreshEntity(e);
        return e;
    }

    public void addInput(float dx, float dy) { /* no-op: SoT-механика = клавиши + ЛКМ */ }

    public boolean tick() {
        if (finished) return false;
        if (hook.isRemoved() || player.fishing != hook || player.isRemoved() || !player.isAlive()
                || hook.level() != player.level()
                || !(player.getItemInHand(hand).getItem() instanceof FishingRodItem)
                || player.distanceToSqr(hook) > 30 * 30) {
            abort();
            return false;
        }

        ticks++;
        updateFish(player.getRandom());

        boolean struggling = phase == PHASE_STRUGGLE;
        boolean warn       = phase == PHASE_WARN;
        boolean window     = phase == PHASE_WINDOW;
        boolean passive    = strength < 0.7f;

        if (struggling) struggleTicks++;

        // ── countering ──
        int axes = (dashLat != 0 ? 1 : 0) + (dashOut != 0 ? 1 : 0);
        int counteredAxes = 0;
        if (struggling) {
            if (dashLat != 0 && keySide == -dashLat) counteredAxes++;
            if (dashOut != 0 && keyTighten)          counteredAxes++;
        }
        float counterFrac = axes == 0 ? 0f : (float) counteredAxes / axes;
        boolean countering = counterFrac > 0f;

        runAge = struggling ? runAge + 1 : 0;
        float ramp = Mth.clamp(runAge / 3f, 0f, 1f);

        // у берега рыба слабее: чем ближе к концу дистанции, тем меньше сила и натяг
        float shore = (float) Mth.clamp((dist - endDist) / SHORE_ZONE, SHORE_MIN, 1.0);

        // ── stamina ──
        if (struggling && !passive) {
            if (counterFrac > 0f) {
                stamina -= STAMINA_PER_TICK * counterFrac * (1f + strength * 0.25f);
            } else {
                stamina -= 0.0002f;
            }
            stamina = Mth.clamp(stamina, 0f, 1f);
        }

        boolean reeling = keyReel && ticks > 10;

        // ── ЛКМ: тап = рывок, удержание = отпустить рыбу ──
        if (jerkCd > 0) jerkCd--;
        if (keyAttack) {
            attackTicks++;
            if (attackTicks >= RELEASE_HOLD) {
                release();
                return false;
            }
            if (attackTicks > TAP_TICKS && attackTicks % 6 == 0) {
                level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                        SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.NEUTRAL,
                        0.35f, 0.8f + 0.8f * attackTicks / RELEASE_HOLD);
            }
        } else {
            if (attackTicks > 0 && attackTicks <= TAP_TICKS && jerkCd == 0) {
                jerkTicks = JERK_TICKS;
                jerkCd = JERK_COOLDOWN;
                if (struggling) strain += JERK_STRAIN;                       // рывок против бегущей рыбы опасен
                else reelLoad = Math.min(1f, reelLoad + JERK_LOAD);          // и вне борьбы нагружает леску
                level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                        SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 0.5f, 1.5f);
            }
            attackTicks = 0;
        }

        // ── strain (борьба) ──
        boolean straining = struggling && reeling && struggleTicks > REEL_GRACE;
        if (straining) {
            strain += STRAIN_RISE * strength;
        } else {
            strain -= STRAIN_FALL;
        }
        strain = Mth.clamp(strain, 0f, 1f);

        // ── нагрузка от подмотки (окно и разминка): тянешь без передышки, леска рвётся ──
        boolean loading = reeling && !struggling;
        if (loading) {
            reelLoad += REEL_LOAD_RISE * strength * shore * (warn ? 0.5f : 1f);
        } else {
            reelLoad -= REEL_LOAD_FALL * (keySlack ? 2f : 1f);
        }
        reelLoad = Mth.clamp(reelLoad, 0f, 1f);

        // ── tension ──
        float tTarget;
        if (struggling) {
            tTarget = 0.55f + 0.40f * shore * (1f - counterFrac);
            if (keySlack) tTarget -= 0.20f;
            if (keyReel)  tTarget += 0.40f * strain;
        } else if (warn) {
            tTarget = 0.50f + 0.45f * reelLoad;
        } else {
            tTarget = 0.30f + 0.70f * reelLoad;
        }
        tension = Mth.clamp(tension + (tTarget - tension) * 0.25f, 0f, 1f);

        // ── snap ──
        boolean grace = struggling && runAge < SNAP_GRACE;
        over = (tension > SNAP_LEVEL && !grace) ? over + 1 : Math.max(0, over - 2);
        if (strain >= 1f || over >= SNAP_TICKS) { snap(); return false; }

        // звук зарядки арбалета, пока леска натягивается: по ступеням вверх, при ослаблении сбрасывается
        float load = Math.max(tension, Math.max(strain, reelLoad));
        while (creakStage < CREAK_AT.length && load >= CREAK_AT[creakStage]) {
            playCreak(creakStage);
            creakStage++;
        }
        while (creakStage > 0 && load < CREAK_AT[creakStage - 1] - 0.12f) {
            creakStage--;   // запас 0.12, чтобы звук не дребезжал на границе
        }
        // ── fish movement ──
        Vec3 o = player.position();
        float fishPower = dashPower * strength * ramp * shore
                * (1f + Math.min(rounds, 3) * 0.08f);
        float baseSpeed = struggling ? RUN_SPEED * fishPower : 0f;

        float vLat = 0f, vOut = 0f;
        if (struggling && dashLat != 0) {
            float slow = 1f - 0.85f * counterFrac;
            if (counteredAxes == 0) slow = 1.20f;
            vLat = dashLat * baseSpeed * slow;
        }
        if (struggling && dashOut != 0) {
            float slow = 1f - 0.85f * counterFrac;
            if (!countering) slow = 1.0f;
            vOut = dashOut * baseSpeed * 0.5f * slow;
        }

        // ПКМ: плавная подмотка
        if (reeling) {
            if (window || passive)      vOut -= REEL_SPEED;
            else if (warn)              vOut -= REEL_SPEED * 0.5f;
            else if (struggling)        vOut -= REEL_SPEED * 0.15f;
        }

        // ЛКМ: резкий рывок
        if (jerkTicks > 0) {
            jerkTicks--;
            if (window || passive)  vOut -= JERK_SPEED;
            else if (warn)          vOut -= JERK_SPEED * 0.5f;
            else                    vOut -= JERK_SPEED * 0.2f;
        }

        // A/D/S/W рыбу НЕ двигают: они только сопротивляются рывку и меняют натяг

        latVel  = latVel  * 0.65f + vLat  * 0.35f;
        distVel = distVel * 0.65f + vOut  * 0.35f;

        float  nl = lat  + latVel;
        double nd = dist + distVel;

        boolean pulling = reeling || jerkTicks > 0;
        if (pulling) {
            // подтягиваем вдоль линии к игроку, но боковое смещение почти не трогаем
            nl -= nl * (struggling ? 0.01f : 0.02f);
        }

        if (!waterAt(o, nd, nl)) {
            // победа только если рыба реально рядом с игроком, а не просто упёрлась в берег
            double real = Math.sqrt(nd * nd + (double) nl * nl);
            if (distVel < 0 && real < endDist + 2.5) { win(); return false; }

            boolean dOk = waterAt(o, nd, lat);
            boolean lOk = waterAt(o, dist, nl);
            if (dOk) {
                nl = lat; latVel *= -0.4f;
            } else if (lOk) {
                nd = dist; distVel *= -0.3f;
            } else {
                nl = lat; nd = dist;
                latVel *= -0.4f; distVel *= -0.3f;
                // упёрлись в берег: плавно уходим к оси, если там вода
                if (waterAt(o, dist, lat * 0.9f)) nl = lat * 0.9f;
            }
        }
        lat = nl;
        dist = nd;

        if (dist > startDist + MAX_OUT) {
            escape(); return false;
        }
        if (Math.abs(lat) > MAX_LAT) {
            lat = Math.signum(lat) * MAX_LAT;
            latVel *= -0.5f;
        }

        progress = (float) Mth.clamp((startDist - dist) / (startDist - endDist), 0.0, 1.0);
        if (dist <= endDist) { progress = 1f; win(); return false; }

        // ── позиция ──
        double x = o.x + baseDir.x * dist + right.x * lat;
        double z = o.z + baseDir.z * dist + right.z * lat;

// плавное покачивание: без острых изломов (раньше abs(sin))
        double dip = struggling
                ? 0.03 * (1f - Mth.cos(ticks * 0.4f))
                : 0.01 * (1f - Mth.cos(ticks * 0.15f));
        double y = baseY + (tension - 0.4f) * 0.15 - dip;

// дрожь от натяжения: медленные наложенные волны + сглаживание вместо случайного шума
        double amp = SHAKE_AMP * tension * tension * 0.5;
        double tx = (Mth.sin(ticks * 0.15f) + 0.5 * Mth.sin(ticks * 0.38f + 1.3f)) * amp * 0.6;
        double tz = (Mth.sin(ticks * 0.13f + 2.1f) + 0.5 * Mth.sin(ticks * 0.34f + 0.4f)) * amp * 0.6;
        double ty = Mth.sin(ticks * 0.22f + 0.8f) * amp * 0.25;
        shX += (tx - shX) * 0.1;
        shY += (ty - shY) * 0.1;
        shZ += (tz - shZ) * 0.1;
        x += shX;
        y += shY;
        z += shZ;

        if (tension > 0.35f && ticks % 3 == 0) {
            level.sendParticles(ParticleTypes.FISHING, hook.getX(), hook.getY(), hook.getZ(),
                    2, 0.15, 0.02, 0.15, 0.02);
        }
        hook.setPos(x, y, z);
        hook.setDeltaMovement(Vec3.ZERO);
        hookAcc.deepfishing$setNibble(20);
        moveVisual(x, y, z);
        sendPos(x, y, z);

        if (ticks % 2 == 0) sendState(true);
        return true;
    }

    private boolean waterAt(Vec3 o, double d, double l) {
        return isWater(o.x + baseDir.x * d + right.x * l, o.z + baseDir.z * d + right.z * l);
    }

    // ── фазовая машина ──
    private void updateFish(RandomSource r) {
        if (strength < 0.7f) {
            phase = PHASE_WINDOW;
            dashLat = 0; dashOut = 0; dashPower = 0f;
            return;
        }
        if (--phaseTicks > 0) return;

        switch (phase) {
            case PHASE_WARN -> {
                phase = PHASE_STRUGGLE;
                dashIndex = 0;
                struggleTicks = 0;
                runAge = 0;
                phaseTicks = DASH_TICKS;
                pickDash(r);
                playDashCue();
            }
            case PHASE_STRUGGLE -> {
                dashIndex++;
                boolean tired = stamina < TIRED_THRESHOLD && dashIndex >= MIN_DASHES;
                if (tired || dashIndex >= DASHES_PER_STRUGGLE) {
                    phase = PHASE_WINDOW;
                    phaseTicks = 60 + r.nextInt(30);   // 3–4.5 с
                    dashLat = 0; dashOut = 0; dashPower = 0f;
                    playTiredCue();
                } else {
                    pickDash(r);
                    phaseTicks = DASH_TICKS;
                    runAge = 0;
                    playDashCue();
                }
            }
            case PHASE_WINDOW -> {
                stamina = Mth.clamp(Math.max(stamina + WINDOW_RESTORE, WINDOW_MIN_STAMINA), 0f, 1f);
                rounds++;
                phase = PHASE_WARN;
                phaseTicks = WARN_TICKS;
                playWarnCue();
            }
        }
    }

    private void pickDash(RandomSource r) {
        int pl = dashLat, po = dashOut;
        do {
            switch (r.nextInt(5)) {
                case 0 -> { dashLat = -1; dashOut = 0; }
                case 1 -> { dashLat =  1; dashOut = 0; }
                case 2 -> { dashLat =  0; dashOut = 1; }
                case 3 -> { dashLat = -1; dashOut = 1; }
                default -> { dashLat = 1; dashOut = 1; }
            }
        } while (dashLat == pl && dashOut == po);
        dashPower = 0.85f + r.nextFloat() * 0.15f;
    }



    private void playDashCue() {
        level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.7f,
                1.0f + player.getRandom().nextFloat() * 0.3f);
        level.sendParticles(ParticleTypes.SPLASH,
                hook.getX(), hook.getY(), hook.getZ(), 12, 0.3, 0.05, 0.3, 0.1);
    }
    private void playCreak(int stage) {
        var snd = switch (stage) {
            case 0 -> DCSoundEvents.TENSION_0;
            case 1 -> DCSoundEvents.TENSION_1;
            case 2 -> DCSoundEvents.TENSION_2;
            default -> DCSoundEvents.TENSION_3;
        };
        level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                snd, SoundSource.NEUTRAL, 0.5f + 0.1f * stage, 0.85f + 0.1f * stage);
    }
    private void playWarnCue() {
        level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.NEUTRAL, 0.5f, 1.6f);
    }

    private void playTiredCue() {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.55f, 0.8f);
        level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 0.4f, 1.4f);
    }

    private boolean isWater(double x, double z) {
        return level.getFluidState(BlockPos.containing(x, baseY, z)).is(FluidTags.WATER);
    }

    private void moveVisual(double hx, double hy, double hz) {
        if (!yawInit) {
            prevHx = hx; prevHz = hz;
            vyaw = (float) (Mth.atan2(-baseDir.x, baseDir.z) * 180.0 / Math.PI);
            yawInit = true;
        }
        mvx = mvx * 0.75 + (hx - prevHx) * 0.25;
        mvz = mvz * 0.75 + (hz - prevHz) * 0.25;
        prevHx = hx; prevHz = hz;

        if (mvx * mvx + mvz * mvz > 1.0E-5) {
            float target = (float) (Mth.atan2(-mvx, mvz) * 180.0 / Math.PI);
            vyaw = Mth.approachDegrees(vyaw, target, 12.0f);
        }

        vx = hx; vy = hy; vz = hz;
        if (visual == null || visual.isRemoved()) return;

        if (visual instanceof ItemEntity) {
            vy = hy - 0.25;
        } else {
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
                new FightStatePayload(active, Math.max(tension, strain), progress, lat / MAX_LAT,
                        (byte) phase, (byte) (phase == PHASE_WARN ? 1 : 0)));
    }

    private EquipmentSlot slot() {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    // ── endings ──
    private void win() {
        finished = true;
        ItemStack rod = player.getItemInHand(hand);
        ItemFishedEvent event = new ItemFishedEvent(new ArrayList<>(loot), hook.onGround() ? 2 : 1, hook);
        NeoForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            for (ItemStack stack : event.getDrops()) {
                ItemStack s = stack.copy();
                player.getInventory().add(s);
                if (!s.isEmpty()) player.drop(s, false);
                if (stack.is(ItemTags.FISHES)) player.awardStat(Stats.FISH_CAUGHT, 1);
            }
            player.giveExperiencePoints(level.getRandom().nextInt(6) + 1);
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, event.getDrops());
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.0f + level.getRandom().nextFloat() * 0.4f);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        cleanup(false);
        hook.discard();
        rod.hurtAndBreak(event.getRodDamage(), player, slot());
    }

    private void snap() {
        finished = true;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        ItemStack rod = player.getItemInHand(hand);
        cleanup(true);
        hook.discard();
        rod.hurtAndBreak(2, player, slot());
    }

    private void escape() {
        finished = true;
        level.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.8f, 0.8f);
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
        if (!hook.isRemoved()) hookAcc.deepfishing$setNibble(0);
        cleanup(true);
    }

    private void cleanup(boolean keepFish) {
        FightManager.lockCast(player);
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
package ru.imaginaerum.damagecore.Init.fishing.fight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.fishing.net.FightPosPayload;
import ru.imaginaerum.damagecore.Init.fishing.net.FightStatePayload;
import ru.imaginaerum.damagecore.Init.items.DCItems;
import ru.imaginaerum.damagecore.entity.ModEntities;
import ru.imaginaerum.damagecore.entity.sunken_lockbox.SunkenLockboxEntity;
import ru.imaginaerum.damagecore.mixin.fishing.FishingHookAccessor;
import ru.imaginaerum.damagecore.sounds.DCSoundEvents;

import java.util.*;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class FightManager {
    public static final String TAG = "deepfishing_visual";
    static final double SHORE_DIST = 0.9;
    public static final Map<UUID, FishFight> FIGHTS = new HashMap<>();
    static final Set<UUID> LIVE_VISUALS = new HashSet<>();
    static final Set<UUID> REELING = new HashSet<>();
    static final double REEL_PULL = 0.12;
    static final int YANK_HOLD = 8; // тиков удержания ЛКМ для рывка
    static final Set<UUID> ATTACK_HELD = new HashSet<>();
    static final Map<UUID, Integer> YANK_TICKS = new HashMap<>();
    static final Map<UUID, Long> CAST_LOCK = new HashMap<>();
    static final double SCARE_VERT      = 0.16;  // вертикальная скорость рывка, блоков/тик
    static final int    SCARE_VERT_FLIP = 8;     // как часто рыба меняет вверх/вниз, тиков
    static final double SCARE_DIVE      = 3.0;   // на сколько блоков вверх/вниз может уйти цель
    public static final double SCARE_NEAR = 4.0;   // радиус испуга, когда игрок стоит или идёт
    static final double SCARE_RUN  = 8.0;          // радиус испуга, когда игрок бежит
    static final double SCARE_PUSH = 0.22;         // физический толчок рыбы прочь
    private record Flee(Vec3 to, long until) {}
    static final Map<UUID, Flee> FLEEING = new HashMap<>();
    static final Map<UUID, Long> REEL_SND = new HashMap<>();
    static final int REEL_SND_INTERVAL = 4;   // тиков между повторами звука
    static final int BITE_COOLDOWN_TICKS = 60;     // пауза после упущенной рыбы
    static final int BITE_WINDOW_MIN = 20;         // ваниль: 20
    static final int BITE_WINDOW_VAR = 20;         // ваниль: 20 (получается 20–40 тиков)
    static final Map<UUID, Long> BITE_COOLDOWN = new HashMap<>();
    static final float CHEST_CHANCE = 0.15f;   // 15% забросов
    static final float CHEST_LUCK_BONUS = 0.05f;  // прибавка за каждый уровень Морской удачи
    static final float CHEST_CHANCE_MAX = 0.40f;  // потолок
    static final Map<UUID, Integer> CHEST_ROLLED = new HashMap<>();   // id крючка, для которого уже кидали шанс
    static final boolean CHEST_OCEAN_ONLY = false;  // true, если нужен только океан
    static final int CHEST_LINGER = 25;   // пауза у крючка перед клёвом, ~1,2 с
    static final double CHEST_PULL        = 0.035;  // как скорость подплывания, ~0,7 блока/с
    static final float  CHEST_LOAD_RISE   = 0.010f; // за тик при зажатом ПКМ, ~5 с до предела
    static final float  CHEST_LOAD_FALL   = 0.020f; // за тик, когда отпустил
    static final float  CHEST_SNAP_LEVEL  = 0.92f;
    static final int    CHEST_SNAP_TICKS  = 15;     // тиков выше предела до обрыва
    private FightManager() {}
    static void reelSound(ServerPlayer p, float pitch) {
        long now = p.serverLevel().getGameTime();
        Long last = REEL_SND.get(p.getUUID());
        if (last != null && now - last < REEL_SND_INTERVAL) return;
        REEL_SND.put(p.getUUID(), now);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(),
                DCSoundEvents.REEL.get(), SoundSource.PLAYERS, 0.6f, pitch);
    }

    public static void lockCast(ServerPlayer p) {
        CAST_LOCK.put(p.getUUID(), p.serverLevel().getGameTime() + 8);
    }
    private static void sendChestState(ServerPlayer p, boolean active, float tension, float progress) {
        PacketDistributor.sendToPlayer(p, new FightStatePayload(active, tension, progress, 0f, (byte) 3, (byte) 0));
    }
    public static void onInput(ServerPlayer player, float dx, float dy) {
        FishFight fight = FIGHTS.get(player.getUUID());
        if (fight != null) {
            fight.addInput(dx, dy);
        }
    }
    private static float chestChance(FishingHook hook, ServerPlayer player) {
        int luck = ((FishingHookAccessor) hook).deepfishing$getLuck();
        return Math.min(CHEST_CHANCE_MAX, CHEST_CHANCE + luck * CHEST_LUCK_BONUS);
    }
    private static double scareRadius(ServerPlayer p) {
        return p.isSprinting() ? SCARE_RUN : SCARE_NEAR;
    }

    /** Отцепляет от пойманной рыбы её стаю: подчинённые больше не плывут за ней. */
    static void detachSchool(Entity hooked) {
        if (!(hooked instanceof AbstractSchoolingFish h) || hooked.isRemoved()) return;
        if (h.isFollower()) h.stopFollowing();
        if (!h.hasFollowers()) return;

        ServerLevel level = (ServerLevel) hooked.level();
        BlockPos at = hooked.blockPosition();
        for (AbstractSchoolingFish f : level.getEntitiesOfClass(AbstractSchoolingFish.class,
                hooked.getBoundingBox().inflate(16), e -> e != hooked && e.isFollower())) {
            BlockPos t = f.getNavigation().getTargetPos();
            if (t != null && t.distSqr(at) < 16) {
                f.stopFollowing();
                f.getNavigation().stop();
            }
        }
    }

    /** Пугает рыбу рядом с игроком: она быстро уплывает, а подплывающая к крючку срывается. */
    private static void scareFish(ServerPlayer player) {
        UUID id = player.getUUID();
        if (FIGHTS.containsKey(id)) return;
        ServerLevel level = player.serverLevel();
        double r = scareRadius(player);
        double rSq = r * r;

        Approach a = APPROACH.get(id);
        if (a != null && a.real && a.fish != null && !a.fish.isRemoved()
                && a.fish.distanceToSqr(player) <= rSq) {
            FishingHook hook = player.fishing;
            if (hook != null && !hook.isRemoved()) {
                ((FishingHookAccessor) hook).deepfishing$setNibble(0);
            }
            dropApproach(id);
            BITE_COOLDOWN.put(id, level.getGameTime() + BITE_COOLDOWN_TICKS);
        }

        long now = level.getGameTime();
        if (player.tickCount % 20 == 0) FLEEING.values().removeIf(fl -> fl.until() < now);

        Vec3 pp = player.position();
        int budget = 32;
        AABB box = player.getBoundingBox().inflate(r);
        for (AbstractFish f : level.getEntitiesOfClass(AbstractFish.class, box,
                e -> e.isInWater() && !e.isNoAi() && !e.getTags().contains(TAG))) {
            if (budget <= 0) break;
            double dSq = f.distanceToSqr(player);
            if (dSq > rSq) continue;
            budget--;
            fleeOne(level, f, pp, dSq, now);
        }
    }

    private static void fleeOne(ServerLevel level, AbstractFish f, Vec3 pp, double dSq, long now) {
        Vec3 fp = f.position();
        double baseAng = Math.atan2(fp.z - pp.z, fp.x - pp.x);

        double[] offsets = {0, 0.39, -0.39, 0.785, -0.785, 1.18, -1.18, 1.571, -1.571, 1.96, -1.96, Math.PI};
        double chosenX = Math.cos(baseAng), chosenZ = Math.sin(baseAng);
        boolean found = false;
        for (double off : offsets) {
            double ang = baseAng + off;
            double cx = Math.cos(ang), cz = Math.sin(ang);
            if (isWaterPathClear(level, fp, cx, cz)) {
                chosenX = cx;
                chosenZ = cz;
                found = true;
                break;
            }
        }
        if (!found) {
            FLEEING.remove(f.getUUID());
            f.getNavigation().stop();
            return;
        }

        Flee fl = FLEEING.get(f.getUUID());
        boolean needNew = fl == null || fl.until() < now
                || fp.distanceToSqr(fl.to()) < 4.0;
        if (needNew) {
            double tx = fp.x + chosenX * 8.0;
            double tz = fp.z + chosenZ * 8.0;
            double ty = pickDiveY(level, f, fp, tx, tz);       // цель на другой глубине
            if (f.getNavigation().moveTo(tx, ty, tz, 2.0)) {
                FLEEING.put(f.getUUID(), new Flee(new Vec3(tx, ty, tz), now + 40));
            } else {
                FLEEING.remove(f.getUUID());
            }
        }

        // горизонтальный толчок
        double push = SCARE_PUSH * (dSq < 4.0 ? 1.6 : 1.0);
        Vec3 v = f.getDeltaMovement();

        // резкий зигзаг по вертикали: направление меняется каждые SCARE_VERT_FLIP тиков, у каждой рыбы своя фаза
        double dir = (((now / SCARE_VERT_FLIP) + f.getId()) & 1L) == 0 ? 1.0 : -1.0;
        if (!waterAt(level, fp.x, fp.y + dir * 0.8, fp.z)) dir = -dir;           // упёрлись в дно или поверхность
        double vy = waterAt(level, fp.x, fp.y + dir * 0.8, fp.z) ? dir * SCARE_VERT : 0.0;

        f.setDeltaMovement(v.x + chosenX * push, vy, v.z + chosenZ * push);
        f.hurtMarked = true;
    }

    /** Цель бегства на случайной глубине: вверх или вниз до SCARE_DIVE блоков, только по воде. */
    private static double pickDiveY(ServerLevel level, AbstractFish f, Vec3 fp, double tx, double tz) {
        for (int i = 0; i < 6; i++) {
            double dy = (f.getRandom().nextDouble() * 2 - 1) * SCARE_DIVE;
            double ty = fp.y + dy;
            if (waterAt(level, tx, ty, tz)
                    && waterAt(level, fp.x + (tx - fp.x) * 0.5, fp.y + dy * 0.5, fp.z + (tz - fp.z) * 0.5)) {
                return ty;
            }
        }
        return adjustTargetY(level, tx, fp.y, tz);
    }

    /** Вода на 0.6 / 1.2 / 1.8 блока в направлении (dx, dz). */
    private static boolean isWaterPathClear(ServerLevel level, Vec3 from, double dx, double dz) {
        return waterAt(level, from.x + dx * 0.6, from.y, from.z + dz * 0.6)
                && waterAt(level, from.x + dx * 1.2, from.y, from.z + dz * 1.2)
                && waterAt(level, from.x + dx * 1.8, from.y, from.z + dz * 1.8);
    }

    /** Подбираем Y цели по столбу воды над точкой (x,z). */
    private static double adjustTargetY(ServerLevel level, double x, double y, double z) {
        for (double d = 0; d <= 1.5; d += 0.5) {
            if (waterAt(level, x, y + d, z)) return y + d;
            if (d > 0 && waterAt(level, x, y - d, z)) return y - d;
        }
        return y;
    }

    private static boolean waterAt(ServerLevel level, double x, double y, double z) {
        return level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)
                || level.getFluidState(BlockPos.containing(x, y - 0.3, z)).is(FluidTags.WATER);
    }

    public static final class Approach {
        Entity fish;
        final List<ItemStack> loot;
        final boolean real;
        float yaw;
        int age, wait;
        int window;
        double chestY = Double.NaN;
        int biteAge;
        int linger;
        boolean arrived;

        ServerPlayer owner;
        float load;
        int over, creak;
        double startDist;
        boolean stateOn;

        Approach(Entity fish, List<ItemStack> loot, boolean real) {
            this.fish = fish;
            this.loot = loot;
            this.real = real;
        }
    }
    static final Map<UUID, Approach> APPROACH = new HashMap<>();

    private static void dropApproach(UUID id) {
        Approach a = APPROACH.remove(id);
        if (a == null || a.fish == null) return;
        if (a.stateOn && a.owner != null && a.owner.connection != null) {
            sendChestState(a.owner, false, 0f, 0f);
        }
        a.stateOn = false;
        if (a.real && !a.fish.isRemoved()) {
            releaseFish(a.fish);
        } else {
            LIVE_VISUALS.remove(a.fish.getUUID());
            a.fish.discard();
        }
    }

    /** Холостая подтяжка поплавка: тянем к игроку, у берега вытаскиваем пустой крючок. */
    /** Холостая подтяжка поплавка: тянем к игроку, у берега вытаскиваем пустой крючок. */
    private static void tickReel(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!REELING.contains(id) || FIGHTS.containsKey(id)) return;

        // сундук ещё плывёт или висит на крючке: подтяжкой занимается tickApproach
        Approach ap = APPROACH.get(id);
        if (ap != null && !ap.real) return;

        FishingHook hook = player.fishing;
        if (hook == null || hook.isRemoved() || hook.getHookedIn() != null || isBiting(hook)) return;

        InteractionHand hand = rodHand(player);
        if (hand == null) return;

        // в полёте не трогаем
        if (!hook.isInWater() && !hook.onGround()) return;
        reelSound(player, 1.0f);
        double dx = player.getX() - hook.getX();
        double dz = player.getZ() - hook.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-4) d = 1.0E-4;
        double nx = dx / d, nz = dz / d;

        ServerLevel level = player.serverLevel();
        boolean shore = hook.onGround() && !hook.isInWater()
                || !waterAt(level, hook.getX() + nx * SHORE_DIST, hook.getY(), hook.getZ() + nz * SHORE_DIST)
                || d < 1.5;

        if (shore) {
            REELING.remove(id);
            dropApproach(id);
            retrieve(player, hook, hand, 1.0f);
            return;
        }

        Vec3 v = hook.getDeltaMovement();
        hook.setDeltaMovement(nx * REEL_PULL, v.y, nz * REEL_PULL);
    }
    private static void sendHookPos(FishingHook hook) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(hook, new FightPosPayload(
                hook.getId(), -1,
                hook.getX(), hook.getY(), hook.getZ(),
                hook.getX(), hook.getY(), hook.getZ(),
                0f));
    }
    private static InteractionHand rodHand(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof FishingRodItem) return InteractionHand.MAIN_HAND;
        if (p.getOffhandItem().getItem() instanceof FishingRodItem) return InteractionHand.OFF_HAND;
        return null;
    }

    @javax.annotation.Nullable
    private static ItemStack itemOf(Entity e) {
        if (e instanceof Cod) return new ItemStack(Items.COD);
        if (e instanceof Salmon) return new ItemStack(Items.SALMON);
        if (e instanceof Pufferfish) return new ItemStack(Items.PUFFERFISH);
        if (e instanceof TropicalFish) return new ItemStack(Items.TROPICAL_FISH);
        return null;
    }

    @Nullable
    private static AbstractFish findFish(ServerPlayer player, ServerLevel level, FishingHook hook, double r) {
        double sr = scareRadius(player);
        AbstractFish best = null;
        double bd = Double.MAX_VALUE;
        for (AbstractFish f : level.getEntitiesOfClass(AbstractFish.class, hook.getBoundingBox().inflate(r),
                e -> e.isInWater() && !e.getTags().contains(TAG) && itemOf(e) != null
                        && e.distanceToSqr(player) > sr * sr)) {
            double d = f.distanceToSqr(hook);
            if (d < bd) { bd = d; best = f; }
        }
        return best;
    }

    private static void tickApproach(ServerPlayer player) {
        UUID id = player.getUUID();
        FishingHook hook = player.fishing;
        if (hook == null || hook.isRemoved()) {
            dropApproach(id);
            REELING.remove(id);
            ATTACK_HELD.remove(id);
            YANK_TICKS.remove(id);
            return;
        }

        if (FIGHTS.containsKey(id)) return;

        FishingHookAccessor acc = (FishingHookAccessor) hook;
        int nibble = acc.deepfishing$getNibble();
        Approach a = APPROACH.get(id);

        // ждём, пока поплавок успокоится на воде, потом ищем настоящую рыбу
        if (a == null) {
            // ванильную поклёвку глушим всегда: клюёт только подплывшая рыба
            if (nibble > 0) acc.deepfishing$setNibble(0);
            if (hook.tickCount < 20 || !hook.isInWater()) return;   // 1 секунда после заброса
            if (hook.tickCount % 5 != 0) return;                    // проверка 4 раза в секунду
            Long cd = BITE_COOLDOWN.get(id);
            if (cd != null && player.serverLevel().getGameTime() < cd) return;
            ServerLevel level = player.serverLevel();

            // шанс на сундук кидаем один раз на заброс, а не при каждой проверке
            boolean rolled = CHEST_ROLLED.getOrDefault(id, -1) == hook.getId();
            if (!rolled) CHEST_ROLLED.put(id, hook.getId());

            if (!rolled && player.getRandom().nextFloat() < chestChance(hook, player)
                    && (!CHEST_OCEAN_ONLY || level.getBiome(hook.blockPosition()).is(BiomeTags.IS_OCEAN))) {
                Entity chest = spawnChest(level, hook, player);
                if (chest != null) {
                    LIVE_VISUALS.add(chest.getUUID());

                    a = new Approach(chest, List.of(new ItemStack(DCItems.SUNKEN_LOCKBOX.get())), false);
                    a.owner = player;
                    a.wait = Math.max(120, (int) (chest.distanceTo(hook) * 28)); // медленно: около 20 секунд
                    APPROACH.put(id, a);
                }
            }
            if (a == null) {
                AbstractFish f = findFish(player, level, hook, 8.0);
                if (f == null) return;

                f.setNoAi(true);
                f.setNoGravity(true);
                f.setInvulnerable(true);
                f.setSilent(true);
                f.setPersistenceRequired();
                f.addTag(TAG);
                LIVE_VISUALS.add(f.getUUID());
                detachSchool(f);

                List<ItemStack> loot = new ArrayList<>();
                loot.add(itemOf(f));
                a = new Approach(f, loot, true);
                a.wait = Math.max(20, (int) (f.distanceTo(hook) * 10));
                APPROACH.put(id, a);
            }
        }
        if (a.fish.isRemoved()) {
            APPROACH.remove(id);
            return;
        }
        if (a.real && player.tickCount % 5 == 0) detachSchool(a.fish);

        // плывём к крючку; к концу a.wait рыба на месте
        if (!a.arrived) {
            int steps = Math.max(1, a.wait - a.age++);
            double fx = a.fish.getX(), fy = a.fish.getY(), fz = a.fish.getZ();
            double tgtX = hook.getX(), tgtZ = hook.getZ();
            a.fish.setDeltaMovement(Vec3.ZERO);
            double dx = (tgtX - fx) / steps;
            double targetY = approachY(player.serverLevel(), hook, a.fish);
            double dy = (targetY - fy) / steps;
            double dz = (tgtZ - fz) / steps;
            if (dx * dx + dz * dz > 1.0E-6) {
                a.yaw = (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
            }
            a.fish.setPos(fx + dx, fy + dy, fz + dz);
            a.fish.setDeltaMovement(Vec3.ZERO);
            a.fish.setYRot(a.yaw);
            a.fish.yRotO = a.yaw;
            if (a.fish instanceof LivingEntity le) {
                le.yBodyRot = a.yaw;
                le.yHeadRot = a.yaw;
            }
            if (a.age >= a.wait) {
                a.arrived = true;
                player.serverLevel().playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                        SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5f, 1.2f);
            }
            // ванильную поклёвку глушим, пока рыба не доплыла
            if (nibble > 0) acc.deepfishing$setNibble(0);
        } else {
            ServerLevel lv = player.serverLevel();
            double ty;
            if (!a.real) {
                if (Double.isNaN(a.chestY)) a.chestY = approachY(lv, hook, a.fish);
                ty = a.chestY;
            } else {
                ty = approachY(lv, hook, a.fish);
            }

            if (a.real) {
                // рыба: как раньше, с окном подсечки
                a.fish.setPos(hook.getX(), ty, hook.getZ());
                a.fish.setDeltaMovement(Vec3.ZERO);

                if (a.biteAge == 0) {
                    a.window = BITE_WINDOW_MIN + player.getRandom().nextInt(BITE_WINDOW_VAR + 1);
                    Vec3 dm = hook.getDeltaMovement();
                    hook.setDeltaMovement(dm.x, -0.25, dm.z);
                    lv.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                            SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.9f, 1.4f);
                    lv.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.35f, 1.8f);
                    lv.sendParticles(ParticleTypes.FISHING, hook.getX(), hook.getY(), hook.getZ(),
                            8, 0.2, 0.02, 0.2, 0.04);
                    lv.sendParticles(ParticleTypes.SPLASH, hook.getX(), hook.getY(), hook.getZ(),
                            10, 0.25, 0.05, 0.25, 0.1);
                }
                if (++a.biteAge > a.window) {
                    acc.deepfishing$setNibble(0);
                    dropApproach(id);
                    BITE_COOLDOWN.put(id, lv.getGameTime() + BITE_COOLDOWN_TICKS);
                    lv.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                            SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.4f, 0.7f);
                } else {
                    acc.deepfishing$setNibble(Math.max(nibble, 20));
                }
                return;
            }

            // ── сундук: тяжёлый, тянется медленно, леска нагружается и может порваться ──
            boolean pulling = false;
            InteractionHand hand = rodHand(player);
            boolean reeling = REELING.contains(id) && hand != null;

            double cdx = player.getX() - hook.getX();
            double cdz = player.getZ() - hook.getZ();
            double cd = Math.max(1.0E-4, Math.sqrt(cdx * cdx + cdz * cdz));
            double cnx = cdx / cd, cnz = cdz / cd;
            if (a.startDist <= 0) a.startDist = cd;

// у берега нагрузка слабее
            float shoreK = (float) Mth.clamp((cd - 1.5) / 5.0, 0.5, 1.0);
            if (reeling) {
                reelSound(player, 0.7f);
                a.load += CHEST_LOAD_RISE * shoreK;
            } else {
                a.load -= CHEST_LOAD_FALL;
            }
            a.load = Mth.clamp(a.load, 0f, 1f);

// обрыв: слишком долго тянешь без передышки
            a.over = a.load > CHEST_SNAP_LEVEL ? a.over + 1 : Math.max(0, a.over - 2);
            if (a.over >= CHEST_SNAP_TICKS) {
                snapChest(player, hook, a);
                return;
            }

            if (reeling) {
                boolean shore = cd < 1.5
                        || !waterAt(lv, hook.getX() + cnx * SHORE_DIST, hook.getY(), hook.getZ() + cnz * SHORE_DIST);
                if (shore) {
                    catchChest(player, hook, hand, a);
                    return;
                }
                // чем сильнее нагрузка, тем медленнее идёт
                double pull = CHEST_PULL * (1.0 - 0.5 * a.load);
                hook.setPos(hook.getX() + cnx * pull, hook.getY(), hook.getZ() + cnz * pull);
                hook.setDeltaMovement(0, hook.getDeltaMovement().y, 0);
                pulling = true;
            }

// скрип лески по ступеням, как в бою
            while (a.creak < FishFight.CREAK_AT.length && a.load >= FishFight.CREAK_AT[a.creak]) {
                var snd = switch (a.creak) {
                    case 0 -> DCSoundEvents.TENSION_0;
                    case 1 -> DCSoundEvents.TENSION_1;
                    case 2 -> DCSoundEvents.TENSION_2;
                    default -> DCSoundEvents.TENSION_3;
                };
                lv.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                        snd, SoundSource.NEUTRAL, 0.5f + 0.1f * a.creak, 0.85f + 0.1f * a.creak);
                a.creak++;
            }
            while (a.creak > 0 && a.load < FishFight.CREAK_AT[a.creak - 1] - 0.12f) a.creak--;

// центр сундука = крючок по X/Z, считаем после сдвига крючка
            a.fish.setPos(hook.getX(), ty, hook.getZ());
            a.fish.setDeltaMovement(Vec3.ZERO);
            if (pulling) sendHookPos(hook);

// натяг и прогресс на клиент: пока тянешь или леска ещё нагружена
            boolean stateNow = reeling || a.load > 0.02f;
            if (stateNow) {
                if (player.tickCount % 2 == 0) {
                    float prog = (float) Mth.clamp(1.0 - cd / a.startDist, 0.0, 1.0);
                    sendChestState(player, true, 0.30f + 0.70f * a.load, prog);
                }
                a.stateOn = true;
            } else if (a.stateOn) {
                sendChestState(player, false, 0f, 0f);
                a.stateOn = false;
            }
            if (pulling) sendHookPos(hook);

            if (a.linger < CHEST_LINGER) {
                a.linger++;
                if (nibble > 0) acc.deepfishing$setNibble(0);
                return;
            }

            if (a.biteAge == 0) {
                a.biteAge = 1;   // сигнал клёва один раз
                lv.playSound(null, hook.getX(), hook.getY(), hook.getZ(),
                        SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6f, 1.2f);
                lv.sendParticles(ParticleTypes.FISHING, hook.getX(), hook.getY(), hook.getZ(),
                        6, 0.2, 0.02, 0.2, 0.03);
            }
            acc.deepfishing$setNibble(Math.max(nibble, 20));
        }
    }

    /** Сундук дотянут до берега: выдаём предмет игроку, без боя и ванильного лута. */
    private static void catchChest(ServerPlayer player, FishingHook hook, InteractionHand hand, Approach a) {
        UUID id = player.getUUID();
        ServerLevel level = player.serverLevel();
        ItemStack rod = player.getItemInHand(hand);

        REELING.remove(id);
        APPROACH.remove(id);
        if (a.stateOn) { sendChestState(player, false, 0f, 0f); a.stateOn = false; }
        LIVE_VISUALS.remove(a.fish.getUUID());
        a.fish.discard();

        ItemStack s = new ItemStack(DCItems.SUNKEN_LOCKBOX.get());
        player.getInventory().add(s);
        if (!s.isEmpty()) player.drop(s, false);
        player.giveExperiencePoints(level.getRandom().nextInt(6) + 1);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.0f + level.getRandom().nextFloat() * 0.4f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0f, 0.8f);

        lockCast(player);
        hook.discard();
        rod.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
    }
    private static void snapChest(ServerPlayer player, FishingHook hook, Approach a) {
        UUID id = player.getUUID();
        ServerLevel level = player.serverLevel();
        REELING.remove(id);
        dropApproach(id);   // сундук уходит на дно, клиенту уходит active=false
        BITE_COOLDOWN.put(id, level.getGameTime() + BITE_COOLDOWN_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        InteractionHand hand = rodHand(player);
        lockCast(player);
        hook.discard();
        if (hand != null) {
            player.getItemInHand(hand).hurtAndBreak(2, player, LivingEntity.getSlotForHand(hand));
        }
    }
    @Nullable
    private static Entity spawnChest(ServerLevel level, FishingHook hook, ServerPlayer player) {
        RandomSource r = player.getRandom();
        for (int i = 0; i < 8; i++) {
            float yaw = player.getYRot() + (r.nextBoolean() ? 1 : -1) * (35f + r.nextFloat() * 25f);
            double dist = 12 + r.nextDouble() * 6;
            double rad = Math.toRadians(yaw);
            double x = player.getX() - Math.sin(rad) * dist;
            double z = player.getZ() + Math.cos(rad) * dist;

            if (!level.hasChunk(((int) x) >> 4, ((int) z) >> 4)) continue;

            // путь от точки появления до крючка целиком по воде
            boolean clear = true;
            for (int s = 0; s <= 6 && clear; s++) {
                double k = s / 6.0;
                clear = waterAt(level, x + (hook.getX() - x) * k, hook.getY(), z + (hook.getZ() - z) * k);
            }
            if (!clear) continue;

            BlockPos.MutableBlockPos p = BlockPos.containing(x, hook.getY(), z).mutable();
            for (int u = 0; u < 8 && level.getFluidState(p.above()).is(FluidTags.WATER); u++) p.move(0, 1, 0);
            double surf = p.getY() + level.getFluidState(p).getHeight(level, p);

            SunkenLockboxEntity chest = ModEntities.SUNKEN_LOCKBOX.get().create(level);
            if (chest == null) return null;
            chest.setFishingVisual(true);
            chest.setPos(x, surf - 0.35, z);
            chest.setYRot(r.nextFloat() * 360f);

            chest.addTag(TAG);
            LIVE_VISUALS.add(chest.getUUID());   // до addFreshEntity, иначе onJoin его отменит
            level.addFreshEntity(chest);
            return chest;
        }
        return null;
    }
    /** Высота, на которой рыба подплывает к крючку: целиком под водой, верх хитбокса ниже поверхности. */
    static double approachY(ServerLevel level, FishingHook hook, Entity fish) {
        BlockPos.MutableBlockPos p = hook.blockPosition().mutable();
        if (!level.getFluidState(p).is(FluidTags.WATER)) {
            p.move(0, -1, 0);
            if (!level.getFluidState(p).is(FluidTags.WATER)) return hook.getY() - 0.28;
        }
        for (int i = 0; i < 64 && level.getFluidState(p.above()).is(FluidTags.WATER); i++) p.move(0, 1, 0);
        double surf = p.getY() + level.getFluidState(p).getHeight(level, p);
        // верх хитбокса на 0.25 ниже поверхности
        if (fish instanceof SunkenLockboxEntity) return surf - 0.35;
        return surf - fish.getBbHeight() - 0.25;
    }
    /** Пустая подтяжка: просто вытаскиваем крючок, без добычи. */
    static void retrieve(ServerPlayer player, FishingHook hook, InteractionHand hand, float pitch) {
        ServerLevel level = player.serverLevel();
        ItemStack rod = player.getItemInHand(hand);
        int dmg = hook.retrieve(rod);
        lockCast(player);
        rod.hurtAndBreak(dmg, player, LivingEntity.getSlotForHand(hand));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0f, pitch);
    }

    private static boolean isBiting(FishingHook hook) {
        return !hook.isRemoved() && ((FishingHookAccessor) hook).deepfishing$getNibble() > 0;
    }

    public static void onKeys(ServerPlayer player, int side, boolean tighten, boolean slack, boolean reel, boolean attack) {
        UUID id = player.getUUID();
        FishFight fight = FIGHTS.get(id);
        if (fight == null) {
            if (reel) REELING.add(id); else REELING.remove(id);
            if (attack) ATTACK_HELD.add(id);
            else { ATTACK_HELD.remove(id); YANK_TICKS.remove(id); }
            return;
        }
        REELING.remove(id);
        ATTACK_HELD.remove(id);
        YANK_TICKS.remove(id);
        fight.setKeys(side, tighten, slack, reel, attack);
    }

    private static void tickYank(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!ATTACK_HELD.contains(id) || FIGHTS.containsKey(id)) { YANK_TICKS.remove(id); return; }

        FishingHook hook = player.fishing;
        // во время поклёвки рывок не работает: там начинается бой по ПКМ
        if (hook == null || hook.isRemoved() || isBiting(hook)) { YANK_TICKS.remove(id); return; }

        int t = YANK_TICKS.merge(id, 1, Integer::sum);
        if (t < YANK_HOLD) return;

        InteractionHand hand = rodHand(player);
        if (hand == null) { YANK_TICKS.remove(id); return; }
        YANK_TICKS.remove(id);
        ATTACK_HELD.remove(id);
        REELING.remove(id);
        dropApproach(id);
        retrieve(player, hook, hand, 1.6f);
    }

    static void releaseFish(Entity e) {
        LIVE_VISUALS.remove(e.getUUID());
        e.removeTag(TAG);
        e.setNoGravity(false);
        e.setInvulnerable(false);
        e.setSilent(false);
        if (e instanceof net.minecraft.world.entity.Mob m) m.setNoAi(false);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            scareFish(p);
            tickApproach(p);
            tickReel(p);
            tickYank(p);
        }

        Iterator<FishFight> it = FIGHTS.values().iterator();
        while (it.hasNext()) {
            if (!it.next().tick()) {
                it.remove();
            }
        }
        for (Iterator<Pending> pi = PENDING.iterator(); pi.hasNext(); ) {
            Pending p = pi.next();
            if (--p.left <= 0 || p.visual.isRemoved() || p.visual.onGround()) {
                pi.remove();
                finish(p);
            }
        }
    }

    static final class Pending {
        final Entity visual;
        final Consumer<Vec3> onLand;
        int left;
        Pending(Entity visual, int left, Consumer<Vec3> onLand) {
            this.visual = visual;
            this.left = left;
            this.onLand = onLand;
        }
    }

    static final List<Pending> PENDING = new ArrayList<>();

    private static void finish(Pending p) {
        Vec3 pos = p.visual.position();
        LIVE_VISUALS.remove(p.visual.getUUID());
        p.visual.discard();
        p.onLand.accept(pos);
    }

    /** Right click with a rod: release the fish during a fight, or start the fight if a bite was just registered. */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getItemStack().getItem() instanceof FishingRodItem)) return;
        Long until = CAST_LOCK.get(player.getUUID());
        if (until != null) {
            if (player.serverLevel().getGameTime() < until) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            CAST_LOCK.remove(player.getUUID());
        }
        FishFight fight = FIGHTS.get(player.getUUID());
        if (fight != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        FishingHook hook = player.fishing;

        if (hook != null && isBiting(hook)) {
            Approach peek = APPROACH.get(player.getUUID());
            if (peek != null && !peek.real) {
                // сундук: подсечки нет, клик ничего не делает, тянет удержание ПКМ
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            Approach pre = APPROACH.remove(player.getUUID());
            if (pre == null) {
                ((FishingHookAccessor) hook).deepfishing$setNibble(0);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            FishFight nf = FishFight.start(player, hook, pre);
            if (nf != null) {
                FIGHTS.put(player.getUUID(), nf);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else {
                APPROACH.put(player.getUUID(), pre);
                dropApproach(player.getUUID());
            }
        } else if (hook != null && hook.getHookedIn() == null) {
            // клёва нет: ванильный мгновенный вытяг отменяем, подтягивает tickReel
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID pid = event.getEntity().getUUID();
        REELING.remove(pid);
        ATTACK_HELD.remove(pid);
        CHEST_ROLLED.remove(pid);
        YANK_TICKS.remove(pid);
        BITE_COOLDOWN.remove(pid);
        FishFight fight = FIGHTS.remove(pid);
        if (fight != null) fight.abort();
        dropApproach(pid);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (FishFight fight : FIGHTS.values()) fight.abort();
        for (Pending p : new ArrayList<>(PENDING)) finish(p);
        for (UUID id : new ArrayList<>(APPROACH.keySet())) dropApproach(id);
        PENDING.clear();
        FIGHTS.clear();
        LIVE_VISUALS.clear();
        FLEEING.clear();
    }

    /** Never let a leftover visual mob (e.g. from a crash) come back from disk. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (e.getTags().contains(TAG) && !LIVE_VISUALS.contains(e.getUUID())) {
            if (e instanceof AbstractFish) {
                releaseFish(e);
            } else {
                event.setCanceled(true);
            }
        }
    }
}
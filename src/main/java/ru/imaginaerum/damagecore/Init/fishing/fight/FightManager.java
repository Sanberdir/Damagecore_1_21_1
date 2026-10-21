package ru.imaginaerum.damagecore.Init.fishing.fight;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.mixin.fishing.FishingHookAccessor;

import java.util.*;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class FightManager {
    public static final String TAG = "deepfishing_visual";

    static final Map<UUID, FishFight> FIGHTS = new HashMap<>();
    static final Set<UUID> LIVE_VISUALS = new HashSet<>();

    private FightManager() {}

    public static void onInput(ServerPlayer player, float dx, float dy) {
        FishFight fight = FIGHTS.get(player.getUUID());
        if (fight != null) {
            fight.addInput(dx, dy);
        }
    }
    public static final class Approach {
        Entity fish;
        final List<ItemStack> loot;
        final boolean real;
        float yaw;
        int age, wait;
        boolean arrived;
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
        if (a.real && !a.fish.isRemoved()) {
            releaseFish(a.fish);
        } else {
            LIVE_VISUALS.remove(a.fish.getUUID());
            a.fish.discard();
        }
    }

    @javax.annotation.Nullable
    private static ItemStack itemOf(Entity e) {
        if (e instanceof Cod) return new ItemStack(Items.COD);
        if (e instanceof Salmon) return new ItemStack(Items.SALMON);
        if (e instanceof Pufferfish) return new ItemStack(Items.PUFFERFISH);
        if (e instanceof TropicalFish) return new ItemStack(Items.TROPICAL_FISH);
        return null;
    }

    @javax.annotation.Nullable
    private static AbstractFish findFish(ServerLevel level, FishingHook hook, double r) {
        AbstractFish best = null;
        double bd = Double.MAX_VALUE;
        for (AbstractFish f : level.getEntitiesOfClass(AbstractFish.class, hook.getBoundingBox().inflate(r),
                e -> e.isInWater() && !e.getTags().contains(TAG) && itemOf(e) != null)) {
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
            return;
        }
        if (FIGHTS.containsKey(id)) return;

        FishingHookAccessor acc = (FishingHookAccessor) hook;
        int nibble = acc.deepfishing$getNibble();
        Approach a = APPROACH.get(id);

        // ждём, пока поплавок успокоится на воде, потом выбираем рыбу
        if (a == null) {
            if (hook.tickCount < 40 || !hook.isInWater()) return;
            if (hook.tickCount % 20 != 0) return;
            // небольшой шанс в секунду, что рыба заинтересуется
            if (player.getRandom().nextFloat() > 0.35f) return;
            AbstractFish f = findFish(player.serverLevel(), hook, 8.0);
            if (f == null) return;
            f.setNoAi(true);
            f.setNoGravity(true);
            f.setInvulnerable(true);
            f.setSilent(true);
            f.setPersistenceRequired();
            f.addTag(TAG);
            LIVE_VISUALS.add(f.getUUID());
            List<ItemStack> loot = new java.util.ArrayList<>();
            loot.add(itemOf(f));
            a = new Approach(f, loot, true);
            double d = f.distanceTo(hook);
            a.wait = Math.max(20, (int) (d * 10));      // ~0.5 с на блок
            APPROACH.put(id, a);
        }
        if (a.fish.isRemoved()) {
            APPROACH.remove(id);
            return;
        }

        // плывём к крючку; к концу a.wait рыба на месте
        if (!a.arrived) {
            int steps = Math.max(1, a.wait - a.age++);
            double fx = a.fish.getX(), fy = a.fish.getY(), fz = a.fish.getZ();
            double dx = (hook.getX() - fx) / steps;
            double dy = (hook.getY() - 0.28 - fy) / steps;
            double dz = (hook.getZ() - fz) / steps;
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
            acc.deepfishing$setNibble(Math.max(nibble, 20));   // клюёт: можно подсекать
        }
    }
    private static boolean isBiting(FishingHook hook) {
        return !hook.isRemoved() && ((FishingHookAccessor) hook).deepfishing$getNibble() > 0;
    }
    public static void onKeys(ServerPlayer player, int side, boolean tighten, boolean slack) {
        FishFight fight = FIGHTS.get(player.getUUID());
        if (fight != null) {
            fight.setKeys(side, tighten, slack);
        }
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
            tickApproach(p);
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

        FishFight fight = FIGHTS.remove(player.getUUID());
        if (fight != null) {
            fight.release(); // vanilla then pulls in an empty hook
            return;
        }
        FishingHook hook = player.fishing;
        if (hook != null && isBiting(hook)) {
            Approach pre = APPROACH.remove(player.getUUID());
            FishFight nf = FishFight.start(player, hook, pre);
            if (nf != null) {
                FIGHTS.put(player.getUUID(), nf);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else if (pre != null && pre.fish != null) {
                APPROACH.put(player.getUUID(), pre);
                dropApproach(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FishFight fight = FIGHTS.remove(event.getEntity().getUUID());
        if (fight != null) fight.abort();
        for (UUID id : new java.util.ArrayList<>(APPROACH.keySet())) dropApproach(id);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (FishFight fight : FIGHTS.values()) fight.abort();
        for (Pending p : new ArrayList<>(PENDING)) finish(p);
        PENDING.clear();
        FIGHTS.clear();
        LIVE_VISUALS.clear();
    }

    /** Never let a leftover visual mob (e.g. from a crash) come back from disk. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (e.getTags().contains(TAG) && !LIVE_VISUALS.contains(e.getUUID())) {
            if (e instanceof net.minecraft.world.entity.animal.AbstractFish) {
                releaseFish(e);
            } else {
                event.setCanceled(true);
            }
        }
    }
}

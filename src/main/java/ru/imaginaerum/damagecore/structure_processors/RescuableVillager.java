package ru.imaginaerum.damagecore.structure_processors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.GlobalPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.client.CaptiveData;
import ru.imaginaerum.damagecore.client.TomeEvents;

import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = "damagecore")
public class RescuableVillager {
    public static final String TAG    = "damagecore_rescuable";
    public static final String FOLLOW = "damagecore_follow";
    public static final String OWNER  = "damagecore_owner";

    private static final ResourceKey<Structure> CASTLE = ResourceKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("damagecore", "pillager_castle"));

    // ПКМ: вместо торговли открываем экран с командами
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager v)) return;
        if (event.getLevel().isClientSide()) return;
        if (!v.getTags().contains(TAG)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (event.getHand() == InteractionHand.MAIN_HAND
                && event.getEntity() instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new RescueNetwork.OpenScreen(v.getId()));
        }
    }
    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Villager v && v.level() instanceof ServerLevel l
                && v.getTags().contains(TAG)) {
            CaptiveData.get(l).remove(v.getUUID());
        }
    }
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager v)) return;
        if (!(v.level() instanceof ServerLevel level)) return;
        if (!v.getTags().contains(TAG)) return;

        // --- освобождение ---
        if (v.tickCount % 20 == 0 && tryRelease(level, v)) return;

        // --- управление движением ---
        var brain = v.getBrain();
        ServerPlayer owner = null;
        if (v.getTags().contains(FOLLOW) && v.getPersistentData().hasUUID(OWNER)) {
            UUID id = v.getPersistentData().getUUID(OWNER);
            owner = level.getServer().getPlayerList().getPlayer(id);
            if (owner != null && owner.level() != level) owner = null;
        }

        if (owner == null) {
            // стоим на месте
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            v.getNavigation().stop();
            return;
        }

        double d = v.distanceToSqr(owner);
        if (d > 32 * 32) {
            v.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        } else if (d > 3 * 3) {
            brain.setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(new EntityTracker(owner, false), 0.6F, 2));
        } else {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            v.getNavigation().stop();
        }
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(owner, true));
    }

    /** Житель вне замка и занял кровать вне замка -> становится обычным. */
    private static boolean tryRelease(ServerLevel level, Villager v) {
        if (insideCastle(level, v.blockPosition())) return false;

        Optional<BlockPos> bed = level.getPoiManager().findClosest(
                h -> h.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),
                v.blockPosition(), 10,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
        if (bed.isEmpty() || insideCastle(level, bed.get())) return false;

        // назначаем кровать домом
        v.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), bed.get()));

        // кого уведомлять: владелец, иначе спасатель, иначе ближайший игрок
        ServerPlayer p = null;
        var data = v.getPersistentData();
        if (data.hasUUID(OWNER)) p = level.getServer().getPlayerList().getPlayer(data.getUUID(OWNER));
        if (p == null && data.hasUUID(TomeEvents.RESCUER_TAG)) {
            p = level.getServer().getPlayerList().getPlayer(
                    data.getUUID(TomeEvents.RESCUER_TAG));
        }
        if (p == null && level.getNearestPlayer(v, 32.0) instanceof ServerPlayer near) p = near;

        v.removeTag(TAG);
        v.removeTag(FOLLOW);
        data.remove(OWNER);
        data.remove(TomeEvents.RESCUER_TAG);
        v.getNavigation().stop();
        v.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        CaptiveData.get(level).remove(v.getUUID());

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                v.getX(), v.getY() + 1, v.getZ(), 12, 0.4, 0.5, 0.4, 0.0);

        if (p != null) {
            p.displayClientMessage(Component.translatable("message.damagecore.villager_rescued"), true);
            TomeEvents.finishRescue(p, "success");
        }
        return true;
    }

    private static boolean insideCastle(ServerLevel level, BlockPos pos) {
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(CASTLE);
        return s != null && level.structureManager().getStructureAt(pos, s).isValid();
    }
}
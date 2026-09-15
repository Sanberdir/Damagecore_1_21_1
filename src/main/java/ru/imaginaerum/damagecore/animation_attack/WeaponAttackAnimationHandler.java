package ru.imaginaerum.damagecore.animation_attack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.animation_attack.combat.WeaponCombatController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class WeaponAttackAnimationHandler {

    private static final Map<UUID, WeaponCombatController> CONTROLLERS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        LivingEntity target = event.getTarget() instanceof LivingEntity l ? l : null;
        if (controller(player).onPrimaryDown(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        controller(player).onPrimaryDown(null);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        long now = System.currentTimeMillis();
        WeaponCombatController c = controller(player);

        if (!Minecraft.getInstance().options.keyAttack.isDown()) {
            c.onPrimaryUp();
        }
        c.tick(now);
    }

    private static WeaponCombatController controller(AbstractClientPlayer player) {
        WeaponCombatController existing = CONTROLLERS.get(player.getUUID());
        if (existing != null && existing.context().player == player) {
            return existing;
        }
        WeaponCombatController fresh = new WeaponCombatController(player);
        CONTROLLERS.put(player.getUUID(), fresh);
        return fresh;
    }
}
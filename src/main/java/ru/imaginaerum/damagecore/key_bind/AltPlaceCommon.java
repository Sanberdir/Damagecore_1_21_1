package ru.imaginaerum.damagecore.key_bind;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class AltPlaceCommon {

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    public static void set(UUID id, boolean down) {
        if (down) ACTIVE.add(id);
        else ACTIVE.remove(id);
    }

    /** То же условие, что в ванили для приседа: хоть что-то в любой руке. */
    public static boolean hasItemInHand(Player player) {
        return !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getEntity();
        if (!ACTIVE.contains(player.getUUID())) return;
        if (!hasItemInHand(player)) return;
        event.setUseBlock(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }
}
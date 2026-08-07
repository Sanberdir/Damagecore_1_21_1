package ru.imaginaerum.damagecore.libraty_effects;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class DamageCoreReloadListeners {

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new FoodProtectionReloadListener());
    }
}

package ru.imaginaerum.damagecore.animation_attack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponTypeManager;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class WeaponTypeManagerRegistration {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(WeaponTypeManager.INSTANCE);

        // Регистрируем менеджер анимаций оружия
        event.addListener(WeaponAnimationManager.INSTANCE);
    }
}
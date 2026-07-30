package ru.imaginaerum.damagecore.Init.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow.ArrowLightningStorm;

@EventBusSubscriber(modid = "damagecore") // Замени на свой MODID, если нужно
public class CommonModEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Вызываем обновление активных штормов каждый тик на сервере
        ArrowLightningStorm.tick();
    }
}
package ru.imaginaerum.damagecore.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import ru.imaginaerum.damagecore.Init.blocks.entity.DCBlockEntities;
import ru.imaginaerum.damagecore.entity.sunken_lockbox.SunkenLockboxRenderer;
import ru.imaginaerum.damagecore.entity.ModEntities;

import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SUNKEN_LOCKBOX.get(), SunkenLockboxRenderer::new);
        event.registerBlockEntityRenderer(DCBlockEntities.SUNKEN_LOCKBOX.get(),
                ctx -> new ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox.SunkenLockboxRenderer());
    }

}
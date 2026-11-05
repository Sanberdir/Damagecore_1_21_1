package ru.imaginaerum.damagecore.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import ru.imaginaerum.damagecore.Init.blocks.entity.DCBlockEntities;
import ru.imaginaerum.damagecore.entity.chest_of_wild_currents.ChestOfWildCurrentsRenderer;
import ru.imaginaerum.damagecore.entity.chest_of_wild_currents.SunkenBottleRenderer;
import ru.imaginaerum.damagecore.entity.ModEntities;

import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CHEST_OF_WILD_CURRENTS.get(), ChestOfWildCurrentsRenderer::new);
        event.registerBlockEntityRenderer(DCBlockEntities.CHEST_OF_WILD_CURRENTS.get(),
                ctx -> new ru.imaginaerum.damagecore.Init.blocks.custom.chest_of_wild_currents.ChestOfWildCurrentsRenderer());

        event.registerEntityRenderer(ModEntities.SUNKEN_BOTTLE.get(), SunkenBottleRenderer::new);
        event.registerBlockEntityRenderer(DCBlockEntities.SUNKEN_BOTTLE.get(),
                ctx -> new ru.imaginaerum.damagecore.Init.blocks.custom.sunken_bottle.SunkenBottleRenderer());
    }

}
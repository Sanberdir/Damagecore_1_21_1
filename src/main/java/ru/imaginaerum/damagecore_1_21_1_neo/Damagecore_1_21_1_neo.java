package ru.imaginaerum.damagecore_1_21_1_neo;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import ru.imaginaerum.damagecore_1_21_1_neo.Init.items.DCItems;
import ru.imaginaerum.damagecore_1_21_1_neo.Init.tab.DCTabs;

@Mod(Damagecore_1_21_1_neo.MODID)
public class Damagecore_1_21_1_neo {
    public static final String MODID = "damagecore_1_21_1_neo";


    public Damagecore_1_21_1_neo(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        DCItems.ITEMS.register(modEventBus);
        DCTabs.CREATIVE_MODE_TAB.register(modEventBus);
        modEventBus.register(DCTabs.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}

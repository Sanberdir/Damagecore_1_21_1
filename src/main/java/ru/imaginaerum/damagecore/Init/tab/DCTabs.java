package ru.imaginaerum.damagecore.Init.tab;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.items.DCItems;

public class DCTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Damagecore_1_21_1_neo.MODID);
    @SubscribeEvent
    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        System.out.println("TAB: " + event.getTabKey());

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(DCItems.BIRCH_LEAF.get());
        }
    }
}

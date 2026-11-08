package ru.imaginaerum.damagecore.Init.fishing.gui.types;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class RodModelSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                Items.FISHING_ROD,
                ResourceLocation.withDefaultNamespace("cast"),
                (stack, level, entity, seed) -> {
                    if (!(entity instanceof Player p) || p.fishing == null) return 0f;
                    ItemStack main = p.getMainHandItem();
                    ItemStack off = p.getOffhandItem();
                    boolean held = main.getItem() instanceof FishingRodItem
                            ? ItemStack.isSameItem(stack, main)
                            : ItemStack.isSameItem(stack, off);
                    return held ? 1f : 0f;
                }));
    }
}
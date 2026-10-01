package ru.imaginaerum.damagecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import ru.imaginaerum.damagecore.Init.items.DCItems;
import ru.imaginaerum.damagecore.client.aspects.ElementOverlay;


@EventBusSubscriber(modid = "damagecore", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                DCItems.TOME_OF_ATTAINING_MEANINGS.get(), // подставьте вашу регистрацию
                ResourceLocation.fromNamespaceAndPath("damagecore", "searching"),
                (stack, level, entity, seed) -> {
                    Player p = entity instanceof Player pl ? pl : Minecraft.getInstance().player;
                    return p != null && p.getData(ModAttachments.TOME_STEP) == 3 ? 1.0F : 0.0F;
                }));
    }
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("damagecore", "element_overlay"),
                ElementOverlay::render);
    }
}
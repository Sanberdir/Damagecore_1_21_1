package ru.imaginaerum.damagecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
        event.enqueueWork(() -> {
            Item tome = DCItems.TOME_OF_ATTAINING_MEANINGS.get();

            ItemProperties.register(tome,
                    ResourceLocation.fromNamespaceAndPath("damagecore", "searching"),
                    (stack, level, entity, seed) -> {
                        Player p = entity instanceof Player pl ? pl : Minecraft.getInstance().player;
                        return p != null
                                && p.getData(ModAttachments.TOME_STEP) == 3
                                && p.getData(ModAttachments.TOME_LENS_ON) ? 1.0F : 0.0F;
                    });

            ItemProperties.register(tome,
                    ResourceLocation.fromNamespaceAndPath("damagecore", "locating"),
                    (stack, level, entity, seed) -> {
                        Player p = entity instanceof Player pl ? pl : Minecraft.getInstance().player;
                        return p != null
                                && p.getData(ModAttachments.TOME_STEP) == TomeLocator.COMPASS_STEP
                                && p.getData(ModAttachments.TOME_COMPASS_ON) ? 1.0F : 0.0F;
                    });

            ItemProperties.register(tome,
                    ResourceLocation.fromNamespaceAndPath("damagecore", "angle"),
                    new CompassItemPropertyFunction((level, stack, entity) -> {
                        Player p = Minecraft.getInstance().player;
                        if (p == null) return null;
                        long l = p.getData(ModAttachments.TOME_TARGET);
                        return l == Long.MIN_VALUE
                                ? null
                                : GlobalPos.of(level.dimension(), BlockPos.of(l));
                    }));
        });
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("damagecore", "element_overlay"),
                ElementOverlay::render);
    }
}
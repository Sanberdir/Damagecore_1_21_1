package ru.imaginaerum.damagecore.Init.fishing.food;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public final class FishTooltip {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        float kg = FishFood.getKg(e.getItemStack());
        if (kg <= 0f) return;
        e.getToolTip().add(1, Component.translatable("tooltip.damagecore.fish_weight",
                String.format("%.2f", kg)).withStyle(ChatFormatting.GRAY));
    }
}
package ru.imaginaerum.damagecore.library_enchantments;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class HigherEnchantTooltipHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        for (HigherEnchant he : HigherEnchants.get(event.getItemStack())) {
            event.getToolTip().add(he.getDisplayName());
        }
    }
}
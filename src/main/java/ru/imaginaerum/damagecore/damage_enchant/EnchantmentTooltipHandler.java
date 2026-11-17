package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import ru.imaginaerum.damagecore.damage_enchant.EnchantmentTooltips;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_enchantments.EnchantmentElements;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class EnchantmentTooltipHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IDamageCoreWeapon)) return;

        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();

        for (Holder<Enchantment> holder : enchantments.keySet()) {
            int level = enchantments.getLevel(holder);
            String name = Enchantment.getFullname(holder, level).getString();

            int index = -1;
            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getString().equals(name)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) continue;

            List<Component> extra = new ArrayList<>();

            EnchantmentElements.get(holder).ifPresent(element ->
                    extra.add(Component.literal(" ").append(
                            Component.translatable("damagecore.element", element.getDisplayName())
                    ).withStyle(ChatFormatting.DARK_GREEN)));

            holder.unwrapKey().ifPresent(key ->
                    extra.addAll(EnchantmentTooltips.describe(key, level, stack)));

            tooltip.addAll(index + 1, extra);
        }
    }
}
package ru.imaginaerum.damagecore.library_enchantments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class HigherEnchants {

    /** ID кнопок меню: 100 + порядковый номер чара (ванильные кнопки 0..2). */
    public static final int BUTTON_BASE = 100;

    private HigherEnchants() {}

    public static List<HigherEnchant> get(ItemStack stack) {
        return stack.getOrDefault(DCDataComponents.HIGHER_ENCHANTS.get(), List.of());
    }

    public static boolean has(ItemStack stack, HigherEnchant e) {
        return get(stack).contains(e);
    }

    /** Все высшие чары, стихия которых есть среди обычных чар предмета (включая уже наложенные). */
    public static List<HigherEnchant> shownFor(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        Set<Element> elements = EnchantmentElements.getElements(stack);
        if (elements.isEmpty()) return List.of();

        List<HigherEnchant> out = new ArrayList<>();
        for (HigherEnchant e : HigherEnchant.values()) {
            if (elements.contains(e.getElement())) out.add(e);
        }
        return out;
    }

    public static boolean canApply(Player player, ItemStack stack, HigherEnchant e) {
        if (stack.isEmpty() || has(stack, e)) return false;
        if (!EnchantmentElements.getElements(stack).contains(e.getElement())) return false;
        return player.hasInfiniteMaterials() || player.experienceLevel >= e.getLevelCost();
    }

    public static boolean tryApply(Player player, ItemStack stack, int ordinal) {
        HigherEnchant[] all = HigherEnchant.values();
        if (ordinal < 0 || ordinal >= all.length) return false;

        HigherEnchant e = all[ordinal];
        if (!canApply(player, stack, e)) return false;

        if (!player.hasInfiniteMaterials()) {
            player.onEnchantmentPerformed(stack, e.getLevelCost());
        }

        List<HigherEnchant> updated = new ArrayList<>(get(stack));
        updated.add(e);
        stack.set(DCDataComponents.HIGHER_ENCHANTS.get(), List.copyOf(updated));
        return true;
    }
}
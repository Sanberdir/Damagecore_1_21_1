package ru.imaginaerum.damagecore.library_enchantments;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class EnchantmentElements {

    private static final Map<ResourceKey<Enchantment>, Element> MAP = Map.ofEntries(
            Map.entry(Enchantments.FIRE_ASPECT, Element.FIRE),
            Map.entry(Enchantments.SMITE, Element.LIGHT),
            Map.entry(Enchantments.BANE_OF_ARTHROPODS, Element.AIR),
            Map.entry(Enchantments.CHANNELING, Element.AIR),
            Map.entry(Enchantments.SHARPNESS, Element.EARTH),
            Map.entry(Enchantments.IMPALING, Element.WATER),
            Map.entry(Enchantments.BREACH, Element.EARTH),
            Map.entry(ModEnchantments.LIFE_DRAIN, Element.DARK),
            Map.entry(ModEnchantments.DARK_INSCRIPTION, Element.DARK),
            Map.entry(ModEnchantments.WIND_SQUALL, Element.AIR),
            Map.entry(ModEnchantments.DENSITY, Element.EARTH)
    );

    private EnchantmentElements() {}

    /** Стихия конкретного зачарования, если она ему назначена. */
    public static Optional<Element> get(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(MAP::get);
    }

    /** Конфликт стихий: сейчас запрещены любые две разные стихии. */
    public static boolean conflicts(Holder<Enchantment> a, Holder<Enchantment> b) {
        Element ea = get(a).orElse(null);
        if (ea == null) return false;
        Element eb = get(b).orElse(null);
        return eb != null && ea != eb;
        // только противоположные стихии: return eb != null && ea.isOppositeOf(eb);
    }

    /** Все стихии зачарований на предмете (без повторов). */
    public static Set<Element> getElements(ItemStack stack) {
        Set<Element> result = EnumSet.noneOf(Element.class);
        ItemEnchantments enchantments = stack.getEnchantments();
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            get(holder).ifPresent(result::add);
        }
        return result;
    }
}
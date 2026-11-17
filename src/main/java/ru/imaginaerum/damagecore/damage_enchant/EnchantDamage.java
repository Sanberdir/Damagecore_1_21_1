package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import ru.imaginaerum.damagecore.Init.enchantments.DensityConfig;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.Map;

public final class EnchantDamage {

    private EnchantDamage() {}

    /** Уровень чара на предмете (0, если нет). */
    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        ItemEnchantments ench = stack.getEnchantments();
        for (Holder<Enchantment> h : ench.keySet()) {
            if (h.is(key)) return ench.getLevel(h);
        }
        return 0;
    }

    /** Урон оружия + урон, который дают чары (в порядке enum, без повторов). */
    public static Map<DamageType, Double> merged(ItemStack stack, Map<DamageType, Double> base) {
        Map<DamageType, Double> out = new EnumMap<>(DamageType.class);
        out.putAll(base);

        int smite = level(stack, Enchantments.SMITE);
        if (smite > 0) add(out, DamageType.LUMINOUS_RADIANT, SmiteConfig.RADIANT_PER_LEVEL * smite);

        int bane = level(stack, Enchantments.BANE_OF_ARTHROPODS);
        if (bane > 0) add(out, DamageType.THUNDER, BaneConfig.THUNDER_PER_LEVEL * bane);

        int fire = level(stack, Enchantments.FIRE_ASPECT);
        if (fire > 0) add(out, DamageType.FIRE, FireAspectConfig.FIRE_PER_LEVEL * fire);

        int sharp = level(stack, Enchantments.SHARPNESS);
        if (sharp > 0 && base.getOrDefault(DamageType.SLASHING, 0.0) > 0) {
            add(out, DamageType.SLASHING, SharpnessConfig.SLASHING_PER_LEVEL * sharp);
        }

        int density = level(stack, ModEnchantments.DENSITY);
        if (density > 0) {
            boolean has = base.getOrDefault(DamageType.BLUDGEONING, 0.0) > 0;
            add(out, DamageType.BLUDGEONING, DensityConfig.bludgeoningBonus(density, has));
        }
        return out;
    }

    private static void add(Map<DamageType, Double> map, DamageType type, double value) {
        map.merge(type, value, Double::sum);
    }
}
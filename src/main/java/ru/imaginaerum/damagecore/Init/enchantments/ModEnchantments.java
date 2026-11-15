package ru.imaginaerum.damagecore.Init.enchantments;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {

    public static final ResourceKey<Enchantment> LIFE_DRAIN = key("life_drain");
    public static final ResourceKey<Enchantment> DARK_INSCRIPTION = key("dark_inscription");
    public static final ResourceKey<Enchantment> WIND_SQUALL = key("wind_squall");
    public static final ResourceKey<Enchantment> DENSITY = key("density");
    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("damagecore", name));
    }

    private ModEnchantments() {}
}
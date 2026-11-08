package ru.imaginaerum.damagecore.Init.fishing.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class ModItemTags {
    public static final TagKey<Item> LINES = create("fishing_lines");
    public static final TagKey<Item> HOOKS = create("fishing_hooks");
    public static final TagKey<Item> BAITS = create("fishing_baits"); // Учтите, на скрине было bailts
    public static final TagKey<Item> REELS = create("fishing_reels");

    private static TagKey<Item> create(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, name));
    }
}
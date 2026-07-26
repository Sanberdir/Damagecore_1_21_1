package ru.imaginaerum.damagecore_1_21_1_neo.Init.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore_1_21_1_neo.Damagecore_1_21_1_neo;

public class DCItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Damagecore_1_21_1_neo.MODID);

    public static final DeferredItem<Item> BIRCH_LEAF = ITEMS.register("birch_leaf",
            () -> new Item(new Item.Properties()));
}

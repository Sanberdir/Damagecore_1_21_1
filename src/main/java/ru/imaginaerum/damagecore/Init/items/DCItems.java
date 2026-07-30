package ru.imaginaerum.damagecore.Init.items;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.items.custom.ChainLightningArrowItem;

public class DCItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Damagecore_1_21_1_neo.MODID);

    public static final DeferredItem<Item> BIRCH_LEAF = ITEMS.register("birch_leaf",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHAIN_LIGHT_ARROW = ITEMS.register("chain_light_arrow",
            () -> new ChainLightningArrowItem(new Item.Properties()));
}

package ru.imaginaerum.damagecore.Init.items;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.BlocksDC;
import ru.imaginaerum.damagecore.Init.fishing.gui.types.BaitItem;
import ru.imaginaerum.damagecore.Init.fishing.gui.types.HookItem;
import ru.imaginaerum.damagecore.Init.fishing.gui.types.LineItem;
import ru.imaginaerum.damagecore.Init.fishing.gui.types.ReelItem;
import ru.imaginaerum.damagecore.Init.items.custom.ChainLightningArrowItem;
import ru.imaginaerum.damagecore.Init.items.custom.ChestOfWildCurrentsBlockItem;
import ru.imaginaerum.damagecore.Init.items.custom.SunkenBottleBlockItem;
import ru.imaginaerum.damagecore.Init.items.custom.TomeOfAttainingMeanings;

public class DCItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Damagecore_1_21_1_neo.MODID);
    public static final DeferredItem<Item> REEL = ITEMS.register("reel",
            () -> new ReelItem(new Item.Properties(),
                    1.0f));  // reelSpeed: скорость подмотки (1.0 = базовая, 1.3 = на 30% быстрее, 0.8 = медленнее)
    public static final DeferredItem<Item> IRON_HOOK = ITEMS.register("iron_hook",
            () -> new HookItem(new Item.Properties().durability(64), // прочность: хватит на 64 боя при износе 1
                    0.7f,  // fightTime: длительность боя (<1 = короче, 0.7 = на 30% короче; заодно ускоряет подмотку)
                    1));   // wear: сколько прочности крюк теряет за один бой
    public static final DeferredItem<Item> FISHING_LINE = ITEMS.register("fishing_line",
            () -> new LineItem(new Item.Properties(),
                    1.0f));  // lineStrength: прочность лески (>1 = дольше терпит натяжение и реже рвётся, 0.7 = рвётся легче)
    public static final DeferredItem<Item> EARTH_WORM = ITEMS.register("earth_worm",
            () -> new BaitItem(new Item.Properties(),
                    1.0f,  // biteChance: шанс клёва на каждую проверку (4 раза/с), 1.0 = максимум, 0.5 = вдвое реже
                    1.5f)); // biteRadius: множитель радиуса поиска рыбы (база 8 блоков, 1.5 = 12 блоков)

    public static final DeferredItem<Item> BIRCH_LEAF = ITEMS.register("birch_leaf",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> KEY_OF_WILD_CURRENTS = ITEMS.register("key_of_wild_currents",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOME_OF_ATTAINING_MEANINGS = ITEMS.register("tome_of_attaining_meanings",
            () -> new TomeOfAttainingMeanings(new Item.Properties()));
    public static final DeferredItem<Item> CHAIN_LIGHT_ARROW = ITEMS.register("chain_light_arrow",
            () -> new ChainLightningArrowItem(new Item.Properties()));
    public static final DeferredItem<Item> CHEST_OF_WILD_CURRENTS = ITEMS.register("chest_of_wild_currents",
            () -> new ChestOfWildCurrentsBlockItem(BlocksDC.CHEST_OF_WILD_CURRENTS.get(), new Item.Properties()));
    public static final DeferredItem<Item> SUNKEN_BOTTLE = ITEMS.register("sunken_bottle",
            () -> new SunkenBottleBlockItem(BlocksDC.SUNKEN_BOTTLE.get(), new Item.Properties()));
}

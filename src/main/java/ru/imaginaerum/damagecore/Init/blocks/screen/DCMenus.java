package ru.imaginaerum.damagecore.Init.blocks.screen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class DCMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Damagecore_1_21_1_neo.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EmbodimentTableMenu>> EMBODIMENT_TABLE =
            MENUS.register("embodiment_table",
                    () -> new MenuType<>(EmbodimentTableMenu::new, FeatureFlags.DEFAULT_FLAGS));
}

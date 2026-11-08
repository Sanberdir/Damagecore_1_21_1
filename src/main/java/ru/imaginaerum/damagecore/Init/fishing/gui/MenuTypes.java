package ru.imaginaerum.damagecore.Init.fishing.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static ru.imaginaerum.damagecore.Damagecore_1_21_1_neo.MODID;

public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final Supplier<MenuType<FishingRodMenu>> FISHING_ROD_MENU = MENU_TYPES.register("fishing_rod_menu",
            () -> IMenuTypeExtension.create(FishingRodMenu::new)
    );
}

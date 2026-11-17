package ru.imaginaerum.damagecore.library_weapon_types;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum WeaponType {
    SWORD       ("weapon.type.sword",    ChatFormatting.DARK_GRAY),
    AXE         ("weapon.type.axe",      ChatFormatting.DARK_GRAY),
    TRIDENT     ("weapon.type.trident",  ChatFormatting.DARK_GRAY),
    SPEAR       ("weapon.type.spear",    ChatFormatting.DARK_GRAY),
    BLUNT       ("weapon.type.blunt",    ChatFormatting.DARK_GRAY), // Булава/Молот
    BOW         ("weapon.type.bow",      ChatFormatting.DARK_GRAY),
    CROSSBOW    ("weapon.type.crossbow", ChatFormatting.DARK_GRAY);

    private final String translationKey;
    private final ChatFormatting color;

    WeaponType(String translationKey, ChatFormatting color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey).withStyle(color);
    }
}
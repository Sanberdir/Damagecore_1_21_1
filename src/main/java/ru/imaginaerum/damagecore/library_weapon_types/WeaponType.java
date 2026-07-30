package ru.imaginaerum.damagecore.library_weapon_types;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum WeaponType {
    SWORD       ("weapon.type.sword",    ChatFormatting.YELLOW),
    AXE         ("weapon.type.axe",      ChatFormatting.YELLOW),
    TRIDENT     ("weapon.type.trident",  ChatFormatting.YELLOW),
    SPEAR       ("weapon.type.spear",    ChatFormatting.YELLOW),
    BLUNT       ("weapon.type.blunt",    ChatFormatting.YELLOW), // Булава/Молот
    BOW         ("weapon.type.bow",      ChatFormatting.YELLOW),
    CROSSBOW    ("weapon.type.crossbow", ChatFormatting.YELLOW);

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
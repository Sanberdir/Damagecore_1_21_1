package ru.imaginaerum.damagecore.armor;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

public interface DamageArmorProvider {

    ArmorMaterial getDamageArmorMaterial();

    ArmorItem.Type getDamageArmorType();
}
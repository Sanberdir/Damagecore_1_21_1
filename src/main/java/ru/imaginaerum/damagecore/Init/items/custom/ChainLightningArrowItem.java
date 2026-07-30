package ru.imaginaerum.damagecore.Init.items.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.item.ChainLightningArrow;

public class ChainLightningArrowItem extends ArrowItem {

    public ChainLightningArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        ChainLightningArrow arrow = new ChainLightningArrow(level, shooter, ammo.copyWithCount(1));

        // Если стреляли из оружия (лука)
        if (weapon != null) {
            // Ищем уровень зачарования "Сила" (Power) на этом луке
            // Для 1.21.1 получаем доступ черезHolder ванильного реестра зачарований
            var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var powerEnchant = registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER);

            int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(powerEnchant, weapon);

            // Записываем уровень Силы в нашу стрелу
            arrow.setPowerLevel(powerLevel);
        }

        return arrow;
    }
}
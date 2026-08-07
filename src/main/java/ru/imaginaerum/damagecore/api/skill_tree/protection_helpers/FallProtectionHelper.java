package ru.imaginaerum.damagecore.api.skill_tree.protection_helpers;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import ru.imaginaerum.damagecore.library_damage.DamageType;

public final class FallProtectionHelper {
    // central value — меняй здесь, и все потребители увидят изменение
    public static float FALL_PROTECTION_PER_LEVEL = 0.05f; // 5% за уровень

    private FallProtectionHelper() {}

    public static float getProtectionPerLevel() {
        return FALL_PROTECTION_PER_LEVEL;
    }
    public static float getEnchantProtectionPercentForStack(ItemStack stack, LivingEntity entity, DamageType type) {
        if (stack.isEmpty() || entity == null) return 0f;
        if (type != DamageType.BLUDGEONING) return 0f;

        Holder<Enchantment> fallProtectionHolder = entity.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FEATHER_FALLING);

        int level = stack.getEnchantmentLevel(fallProtectionHolder);
        return level * getProtectionPerLevel();
    }
    /** Возвращает защиту от чар (в долях, 0..1) для конкретной сущности и типа урона. */
    public static float getEnchantProtectionPercent(LivingEntity entity, DamageType type) {
        if (entity == null) return 0f;
        if (type != DamageType.BLUDGEONING) {
            return 0f;
        }

        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Извлекаем Holder чар Невесомости из реестра игры
        Holder<Enchantment> fallProtectionHolder = entity.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FEATHER_FALLING);

        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Используем новый метод получения уровня чар
        int totalLevel = EnchantmentHelper.getEnchantmentLevel(fallProtectionHolder, entity);
        return totalLevel * getProtectionPerLevel();
    }
}

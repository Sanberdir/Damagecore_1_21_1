package ru.imaginaerum.damagecore.api.skill_tree.protection_helpers;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import ru.imaginaerum.damagecore.library_damage.DamageType;

public final class FireProtectionHelper {
    // central value — меняй здесь, и все потребители увидят изменение
    public static float FIRE_PROTECTION_PER_LEVEL = 0.1f; // 10% за уровень

    private FireProtectionHelper() {}

    public static float getProtectionPerLevel() {
        return FIRE_PROTECTION_PER_LEVEL;
    }
    public static float getEnchantProtectionPercentForStack(ItemStack stack, LivingEntity entity, DamageType type) {
        if (stack.isEmpty() || entity == null) return 0f;
        if (type != DamageType.FIRE) return 0f;

        Holder<Enchantment> fireProtectionHolder = entity.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FIRE_PROTECTION);

        int level = stack.getEnchantmentLevel(fireProtectionHolder);
        return level * getProtectionPerLevel();
    }
    /** Возвращает защиту от чар (в долях, 0..1) для конкретной сущности и типа урона. */
    public static float getEnchantProtectionPercent(LivingEntity entity, DamageType type) {
        if (entity == null) return 0f;
        if (type != DamageType.FIRE) {
            return 0f;
        }

        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Извлекаем Holder чар Огнеупорности из реестра игры
        Holder<Enchantment> fireProtectionHolder = entity.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FIRE_PROTECTION);

        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Передаем Holder в EnchantmentHelper
        int totalLevel = EnchantmentHelper.getEnchantmentLevel(fireProtectionHolder, entity);
        return totalLevel * getProtectionPerLevel();
    }
}

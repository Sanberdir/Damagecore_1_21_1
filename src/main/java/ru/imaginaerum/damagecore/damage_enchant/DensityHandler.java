package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.imaginaerum.damagecore.Init.enchantments.DensityConfig;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

@EventBusSubscriber(modid = "damagecore")
public class DensityHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Только ближний бой
        if (event.getSource().getDirectEntity() != attacker) return;

        ItemStack held = attacker.getMainHandItem();
        int level = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.DENSITY)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, held))
                .orElse(0);
        if (level <= 0) return;

        // Дробящий урон самого оружия
        double existing = 0.0;
        if (held.getItem() instanceof IDamageCoreWeapon weapon) {
            existing = weapon.damagecore$getDamageMap().getOrDefault(DamageType.BLUDGEONING, 0.0);
        }

        // 1) Прибавка к дробящему урону
        float bonus = DensityConfig.bludgeoningBonus(level, existing > 0);

        // 2) Множитель высоты: только на дробящую часть
        float heightExtra = 0.0F;
        if (attacker.fallDistance > 0.0F && !attacker.onGround()) {
            float mult = Math.min(DensityConfig.HEIGHT_MULT_MAX,
                    DensityConfig.HEIGHT_MULT_PER_BLOCK * attacker.fallDistance);
            heightExtra = ((float) existing + bonus) * mult;
        }

        float total = bonus + heightExtra;
        DamageContext.add(event.getEntity(), DamageType.BLUDGEONING, total);
        event.setAmount(event.getAmount() + total);
    }
}
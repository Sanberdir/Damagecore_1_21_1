package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import ru.imaginaerum.damagecore.Init.enchantments.LifeDrainConfig;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.library_enchantments.Element;
import ru.imaginaerum.damagecore.library_enchantments.EnchantmentElements;

@EventBusSubscriber(modid = "damagecore")
public class LifeDrainHandler {

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Только ближний бой
        if (event.getSource().getDirectEntity() != attacker) return;
        if (attacker.level().isClientSide) return;
        if (attacker == event.getEntity()) return;

        Holder<Enchantment> lifeDrain = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.LIFE_DRAIN);

        int level = EnchantmentHelper.getItemEnchantmentLevel(lifeDrain, attacker.getMainHandItem());
        if (level <= 0) return;

        Element element = EnchantmentElements.get(lifeDrain).orElse(null); // Element.DARK

        // Реально нанесённый урон (после брони и сопротивлений)
        float dealt = event.getNewDamage();
        if (dealt <= 0) return;

        float heal = dealt * LifeDrainConfig.HEAL_PERCENT_PER_LEVEL * level;
        attacker.heal(heal);
    }
}
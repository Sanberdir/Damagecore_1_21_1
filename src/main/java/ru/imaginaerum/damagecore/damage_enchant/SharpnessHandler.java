package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_enchantments.Element;
import ru.imaginaerum.damagecore.library_enchantments.EnchantmentElements;

@EventBusSubscriber(modid = "damagecore")
public class SharpnessHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Только ближний бой
        if (event.getSource().getDirectEntity() != attacker) return;

        ItemStack held = attacker.getMainHandItem();
        if (!(held.getItem() instanceof IDamageCoreWeapon weapon)) return;

        // Острота усиливает только уже имеющийся режущий урон
        double slashing = weapon.damagecore$getDamageMap()
                .getOrDefault(DamageType.SLASHING, 0.0);
        if (slashing <= 0) return;

        Holder<Enchantment> sharpness = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SHARPNESS);

        int level = EnchantmentHelper.getItemEnchantmentLevel(sharpness, held);
        if (level <= 0) return;

        Element element = EnchantmentElements.get(sharpness).orElse(null); // Element.EARTH

        float bonus = SharpnessConfig.SLASHING_PER_LEVEL * level;

        DamageContext.add(event.getEntity(), DamageType.SLASHING, bonus);
        event.setAmount(event.getAmount() + bonus);
    }
}
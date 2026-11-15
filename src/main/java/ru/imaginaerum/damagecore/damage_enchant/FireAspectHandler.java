package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_enchantments.Element;
import ru.imaginaerum.damagecore.library_enchantments.EnchantmentElements;

@EventBusSubscriber(modid = "damagecore")
public class FireAspectHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Только ближний бой
        if (event.getSource().getDirectEntity() != attacker) return;

        Holder<Enchantment> fireAspect = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FIRE_ASPECT);

        int level = EnchantmentHelper.getItemEnchantmentLevel(fireAspect, attacker.getMainHandItem());
        if (level <= 0) return;
        Element element = EnchantmentElements.get(fireAspect).orElse(null); // Element.FIRE
        LivingEntity target = event.getEntity();

        // 1) Огненный урон добавляется всегда
        float fire = FireAspectConfig.FIRE_PER_LEVEL * level;
        DamageContext.add(target, DamageType.FIRE, fire);
        event.setAmount(event.getAmount() + fire);

        // 2) Шанс поджечь
        float chance = FireAspectConfig.IGNITE_CHANCE_PER_LEVEL * level;
        if (attacker.getRandom().nextFloat() < chance) {
            target.igniteForSeconds(FireAspectConfig.IGNITE_SECONDS);
        }
    }
}
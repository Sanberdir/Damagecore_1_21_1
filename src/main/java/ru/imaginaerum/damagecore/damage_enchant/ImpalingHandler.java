package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
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
import ru.imaginaerum.damagecore.library_enchantments.Element;
import ru.imaginaerum.damagecore.library_enchantments.EnchantmentElements;

@EventBusSubscriber(modid = "damagecore")
public class ImpalingHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        // Оружие: брошенный трезубец или предмет в руке при ударе
        ItemStack weapon;
        var direct = event.getSource().getDirectEntity();
        if (direct instanceof ThrownTrident trident) {
            weapon = trident.getWeaponItem();
        } else if (direct == attacker) {
            weapon = attacker.getMainHandItem();
        } else {
            return;
        }
        if (weapon == null || weapon.isEmpty()) return;

        Holder<Enchantment> impaling = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.IMPALING);

        int level = EnchantmentHelper.getItemEnchantmentLevel(impaling, weapon);
        if (level <= 0) return;

        Element element = EnchantmentElements.get(impaling).orElse(null); // Element.WATER

        LivingEntity target = event.getEntity();

// 1) Колющий урон только по цели в воде
        if (target.isInWater()) {
            float piercing = ImpalingConfig.PIERCING_PER_LEVEL * level;
            DamageContext.add(target, DamageType.PIERCING, piercing);
            event.setAmount(event.getAmount() + piercing);
        }

// 2) Шанс замедления на любой цели
        float chance = ImpalingConfig.SLOW_CHANCE_PER_LEVEL * level;
        if (attacker.getRandom().nextFloat() < chance) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    ImpalingConfig.SLOW_SECONDS * 20,
                    ImpalingConfig.SLOW_AMPLIFIER));
        }
    }
}
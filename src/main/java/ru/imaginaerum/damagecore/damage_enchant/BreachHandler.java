package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Map;

@EventBusSubscriber(modid = "damagecore")
public class BreachHandler {

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Только ближний бой
        if (event.getSource().getDirectEntity() != attacker) return;

        ItemStack held = attacker.getMainHandItem();
        int level = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.BREACH)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, held))
                .orElse(0);
        if (level <= 0) return;

        DamageContainer container = event.getContainer();
        float absorbed = container.getReduction(DamageContainer.Reduction.ARMOR);
        if (absorbed <= 0) return;

        float fraction = penetratingFraction(held);
        if (fraction <= 0) return;

        float ignore = Math.min(1.0F, BreachConfig.ARMOR_IGNORE_PER_LEVEL * level) * fraction;
        container.setReduction(DamageContainer.Reduction.ARMOR, absorbed * (1.0F - ignore));
    }

    /** Доля колющего и дробящего урона в оружии (0..1) */
    public static float penetratingFraction(ItemStack stack) {
        if (!(stack.getItem() instanceof IDamageCoreWeapon weapon)) return 1.0F; // нет карты урона: считаем полностью
        Map<DamageType, Double> map = weapon.damagecore$getDamageMap();
        double total = 0.0, pen = 0.0;
        for (Map.Entry<DamageType, Double> e : map.entrySet()) {
            total += e.getValue();
            if (e.getKey() == DamageType.PIERCING || e.getKey() == DamageType.BLUDGEONING) {
                pen += e.getValue();
            }
        }
        return total <= 0 ? 0.0F : (float) (pen / total);
    }
}
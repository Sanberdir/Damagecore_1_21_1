package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
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
public class BaneOfArthropodsHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Только ближний бой
        if (event.getSource().getDirectEntity() != attacker) return;

        Holder<Enchantment> bane = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.BANE_OF_ARTHROPODS);

        int level = EnchantmentHelper.getItemEnchantmentLevel(bane, attacker.getMainHandItem());
        if (level <= 0) return;
        Element element = EnchantmentElements.get(bane).orElse(null);
        LivingEntity target = event.getEntity();

        // Урон громом добавляется всегда
        float thunder = BaneConfig.THUNDER_PER_LEVEL * level;

        // Множитель только на громовую часть и только против членистоногих
        if (target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            thunder *= 1.0F + BaneConfig.MULT_PER_LEVEL * level;
        }

        // Остальные типы урона оружия не трогаем
        DamageContext.add(target, DamageType.THUNDER, thunder);
    }
}
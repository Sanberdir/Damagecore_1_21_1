package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
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
public class SmiteHandler {

    /** Базовый лучистый урон: 0.3 за каждый уровень */
    private static final float RADIANT_PER_LEVEL = 0.3F;
    /** Прибавка к множителю лучистого урона против целей из тега, за уровень */
    private static final float MULT_PER_LEVEL = 0.3F;

    private static final TagKey<EntityType<?>> TARGETS = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("damagecore", "sensitive_to_celestial_wrath"));

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        // Только ближний бой
        if (event.getSource().getDirectEntity() != attacker) return;

        Holder<Enchantment> smite = attacker.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SMITE);

        int level = EnchantmentHelper.getItemEnchantmentLevel(smite, attacker.getMainHandItem());
        if (level <= 0) return;
        Element element = EnchantmentElements.get(smite).orElse(null);
        LivingEntity target = event.getEntity();

        // Лучистый урон добавляется всегда
        float radiant = RADIANT_PER_LEVEL * level;

        // Множитель применяется только к лучистой части и только против целей из тега
        if (target.getType().is(TARGETS)) {
            radiant *= 1.0F + MULT_PER_LEVEL * level;
        }

        // Остальные типы урона оружия не трогаем
        DamageContext.add(target, DamageType.LUMINOUS_RADIANT, radiant);
        event.setAmount(event.getAmount() + radiant);
    }
}
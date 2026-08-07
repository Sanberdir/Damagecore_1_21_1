package ru.imaginaerum.damagecore.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;

import java.util.HashMap;
import java.util.Map;

/**
 * NeoForge 1.21.1: заменяет старую логику из
 * TridentItemMixin#damagecore$overrideAttributes (мискин на
 * getDefaultAttributeModifiers, которого больше не существует).
 *
 * Оригинал отбрасывал ВСЕ дефолтные модификаторы трезубца и оставлял
 * только один ATTACK_DAMAGE = суммарный кастомный урон — здесь это
 * воспроизведено через event.clearModifiers().
 */
@EventBusSubscriber(modid = "damagecore")
public final class TridentDamageAttributeHandler {

    // Отдельный id от мечей: разные типы оружия не должны делить один и тот же id модификатора
    private static final ResourceLocation TRIDENT_ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath("damagecore", "trident_base_attack_damage");

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof TridentItem)) return;

        WeaponDamageData customData =
                Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(stack.getItem());

        boolean hasCustom = customData != null && !customData.isEmpty();

        double totalDamage;

        if (stack.getItem() instanceof IDamageCoreWeapon weapon) {
            if (hasCustom) {
                Map<DamageType, Double> customMap = new HashMap<>(customData.getDamageMap());
                weapon.damagecore$setCustomDamage(customMap);
            } else {
                Map<DamageType, Double> defaultMap = new HashMap<>();
                defaultMap.put(DamageType.PIERCING, (double) TridentItem.BASE_DAMAGE);
                weapon.damagecore$setDefaultDamage(defaultMap);
            }

            totalDamage = weapon.damagecore$getDamageMap()
                    .values()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
        } else {
            totalDamage = TridentItem.BASE_DAMAGE;
        }

        // Полностью отбрасываем дефолтные модификаторы трезубца (как и в оригинале)
        event.clearModifiers();

        event.addModifier(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        TRIDENT_ATTACK_DAMAGE_ID,
                        totalDamage,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
    }
}
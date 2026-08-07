package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.HashMap;
import java.util.Map;

/**
 * NeoForge 1.21.1: полностью заменяет старую логику из
 * SwordItemMixin#damagecore$replaceDamageTypes (мискин на
 * getDefaultAttributeModifiers, которого больше не существует).
 *
 * Здесь мы:
 *  1) вычисляем карту типизированного урона (кастомную из
 *     WEAPON_DAMAGE_MANAGER либо дефолтную 30% piercing / 70% slashing)
 *     и сохраняем её в IDamageCoreWeapon для чтения в других местах;
 *  2) обнуляем ванильный ATTACK_DAMAGE и (при наличии) подменяем
 *     ATTACK_SPEED через ItemAttributeModifierEvent.
 */
@EventBusSubscriber(modid = "damagecore")
public final class SwordDamageAttributeHandler {

    // ИСПРАВЛЕНО: в 1.21 AttributeModifier идентифицируется ResourceLocation, а не UUID
    private static final ResourceLocation BASE_ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath("damagecore", "base_attack_damage");

    private static final ResourceLocation BASE_ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("damagecore", "base_attack_speed");

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof SwordItem sword)) return;

        WeaponDamageData customData =
                Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(stack.getItem());

        boolean hasCustom = customData != null && !customData.isEmpty();

        // ─── Сохраняем карту урона в IDamageCoreWeapon ───
        // ВАЖНО: проверяем интерфейс IDamageCoreWeapon, а не класс миксина —
        // миксин вплетается в байткод SwordItem только в рантайме, для javac
        // SwordItemMixin не является реальным супертипом Item.
        if (stack.getItem() instanceof IDamageCoreWeapon weapon) {
            if (hasCustom) {
                weapon.damagecore$setCustomDamage(new HashMap<>(customData.getDamageMap()));
            } else {
                double baseDamage = sword.getDamage(stack);

                Map<DamageType, Double> defaultMap = new HashMap<>();
                defaultMap.put(DamageType.PIERCING, baseDamage * 0.3);
                defaultMap.put(DamageType.SLASHING, baseDamage * 0.7);

                weapon.damagecore$setDefaultDamage(defaultMap);
            }
        }

        // ─── Полностью отключаем ванильный урон оружия ───
        event.removeAllModifiersFor(Attributes.ATTACK_DAMAGE);
        event.addModifier(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        -1.0D,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        // ─── Кастомная скорость атаки ───
        if (hasCustom && customData.hasAttackSpeed()) {
            event.removeAllModifiersFor(Attributes.ATTACK_SPEED);
            event.addModifier(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                            BASE_ATTACK_SPEED_ID,
                            customData.getAttackSpeed() - 4.0,
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
        }
    }
}
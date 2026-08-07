package ru.imaginaerum.damagecore.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;

import java.util.HashMap;
import java.util.Map;

// МЕНЯЕМ ЦЕЛЬ: Миксим в базовый Item, так как именно там объявлен getDefaultAttributeModifiers в 1.21.1
@Mixin(Item.class)
public abstract class DiggerItemMixin implements IDamageCoreWeapon {

    @Unique
    private final Map<DamageType, Double> damagecore$damageMap = new HashMap<>();

    @Unique
    private Map<DamageType, Double> damagecore$customDamage = null;

    @Unique
    private boolean damagecore$hasCustom = false;

    @Unique
    private boolean damagecore$initialized = false;

    @Unique
    private Double damagecore$cachedAttackSpeed = null;

    @Unique
    private static final ResourceLocation BASE_ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("damagecore", "base_attack_damage");
    @Unique
    private static final ResourceLocation BASE_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("damagecore", "base_attack_speed");

    @Inject(
            method = "getDefaultAttributeModifiers",
            at = @At("HEAD"),
            cancellable = true
    )
    private void damagecore$overrideAttributes(
            CallbackInfoReturnable<ItemAttributeModifiers> cir
    ) {
        // Проверяем, является ли текущий предмет инструментом (DiggerItem)
        if (!((Object) this instanceof DiggerItem tool)) {
            return; // Если это обычный предмет или меч, этот миксин его пропускает
        }

        Item item = (Item) (Object) this;

        // Инициализируем данные один раз
        if (!damagecore$initialized) {
            damagecore$initializeData(tool, item);
            damagecore$initialized = true;
        }

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        double totalDamage = damagecore$getTotalDamage();

        // Добавляем урон
        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        totalDamage,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        // Добавляем скорость атаки
        double attackSpeed = damagecore$getAttackSpeed(tool);
        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        attackSpeed - 4.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        cir.setReturnValue(builder.build());
    }

    @Unique
    private void damagecore$initializeData(DiggerItem tool, Item item) {
        WeaponDamageData customData = Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(item);

        if (customData != null && !customData.isEmpty()) {
            damagecore$customDamage = new HashMap<>(customData.getDamageMap());
            damagecore$hasCustom = true;

            if (customData.hasAttackSpeed()) {
                damagecore$cachedAttackSpeed = customData.getAttackSpeed();
            }
        } else {
            damagecore$hasCustom = false;

            double baseDamage = damagecore$getApproximateBaseDamage(tool);

            double bludgeoning = baseDamage * 0.6;
            double slashing = baseDamage * 0.2;
            double piercing = baseDamage * 0.2;

            if (tool instanceof AxeItem) {
                slashing = baseDamage * 0.75;
                bludgeoning = baseDamage * 0.25;
                piercing = 0;
            } else if (tool instanceof PickaxeItem) {
                bludgeoning = baseDamage * 0.8;
                piercing = baseDamage * 0.2;
                slashing = 0;
            } else if (tool instanceof ShovelItem) {
                bludgeoning = baseDamage * 0.5;
                piercing = baseDamage * 0.3;
                slashing = baseDamage * 0.2;
            } else if (tool instanceof HoeItem) {
                slashing = baseDamage;
                piercing = baseDamage + 1;
                bludgeoning = baseDamage;
            }

            damagecore$damageMap.clear();
            if (bludgeoning > 0) damagecore$damageMap.put(DamageType.BLUDGEONING, bludgeoning);
            if (slashing > 0) damagecore$damageMap.put(DamageType.SLASHING, slashing);
            if (piercing > 0) damagecore$damageMap.put(DamageType.PIERCING, piercing);

            damagecore$cachedAttackSpeed = getDefaultAttackSpeed(tool);
        }
    }

    @Unique
    private double damagecore$getApproximateBaseDamage(DiggerItem tool) {
        Tier tier = tool.getTier();
        float materialDamage = tier != null ? tier.getAttackDamageBonus() : 0.0f;

        if (tool instanceof AxeItem) {
            return materialDamage + 6.0;
        } else if (tool instanceof PickaxeItem) {
            return materialDamage + 1.0;
        } else if (tool instanceof ShovelItem) {
            return materialDamage + 1.5;
        } else if (tool instanceof HoeItem) {
            return materialDamage + 0.0;
        }

        return materialDamage + 1.0;
    }

    @Unique
    private double damagecore$getAttackSpeed(DiggerItem tool) {
        if (damagecore$cachedAttackSpeed != null) {
            return damagecore$cachedAttackSpeed;
        }
        return getDefaultAttackSpeed(tool);
    }

    @Unique
    private double getDefaultAttackSpeed(DiggerItem tool) {
        if (tool instanceof AxeItem) return 1.0;
        if (tool instanceof PickaxeItem) return 1.2;
        if (tool instanceof ShovelItem) return 1.5;
        if (tool instanceof HoeItem) return 2.0;
        return 1.0;
    }

    @Override
    public Map<DamageType, Double> damagecore$getDamageMap() {
        if (!damagecore$initialized) {
            Item item = (Item) (Object) this;
            WeaponDamageData customData = Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(item);
            if (customData != null && !customData.isEmpty()) {
                damagecore$customDamage = new HashMap<>(customData.getDamageMap());
                damagecore$hasCustom = true;
            }
            damagecore$initialized = true;
        }
        return damagecore$hasCustom ? damagecore$customDamage : damagecore$damageMap;
    }

    @Override
    public void damagecore$setCustomDamage(Map<DamageType, Double> customDamage) {
        this.damagecore$customDamage = customDamage;
        this.damagecore$hasCustom = true;
    }

    @Override
    public boolean damagecore$hasCustomDamage() {
        return damagecore$hasCustom;
    }

    @Unique
    public double damagecore$getTotalDamage() {
        Map<DamageType, Double> map = damagecore$getDamageMap();
        return map.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}

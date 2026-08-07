package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

import java.util.HashMap;
import java.util.Map;

/**
 * В 1.21+ атрибуты предмета задаются через DataComponent
 * (ItemAttributeModifiers) и подменяются через NeoForge-событие
 * ItemAttributeModifierEvent (см. TridentDamageAttributeHandler).
 *
 * Этот мискин оставлен только для того, чтобы TridentItem реализовывал
 * IDamageCoreWeapon и хранил вычисленную карту урона.
 */
@Mixin(TridentItem.class)
public abstract class TridentItemMixin implements IDamageCoreWeapon {

    @Unique
    private Map<DamageType, Double> damagecore$damageMap = new HashMap<>();

    @Unique
    private Map<DamageType, Double> damagecore$customDamage = null;

    @Unique
    private boolean damagecore$hasCustom = false;

    @Override
    public Map<DamageType, Double> damagecore$getDamageMap() {
        return damagecore$hasCustom ? damagecore$customDamage : damagecore$damageMap;
    }

    @Override
    public void damagecore$setCustomDamage(Map<DamageType, Double> customDamage) {
        this.damagecore$customDamage = customDamage;
        this.damagecore$hasCustom = true;
    }

    @Override
    public void damagecore$setDefaultDamage(Map<DamageType, Double> defaultDamage) {
        this.damagecore$damageMap = defaultDamage;
        this.damagecore$hasCustom = false;
    }

    @Override
    public boolean damagecore$hasCustomDamage() {
        return damagecore$hasCustom;
    }

    @Unique
    public double damagecore$getTotalDamage() {
        return damagecore$getDamageMap()
                .values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
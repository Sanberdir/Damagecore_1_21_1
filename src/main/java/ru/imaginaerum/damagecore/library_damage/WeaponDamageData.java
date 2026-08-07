package ru.imaginaerum.damagecore.library_damage;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;

public class WeaponDamageData {
    private final Map<DamageType, Double> damageMap;
    private Double attackSpeed;
    private final Map<DamageType, Double> effectChances;

    public WeaponDamageData() {
        this.damageMap = new HashMap<>();
        this.attackSpeed = null;
        this.effectChances = new HashMap<>();
    }

    public static WeaponDamageData fromJson(JsonObject json) {
        WeaponDamageData data = new WeaponDamageData();

        // Загружаем урон
        if (json.has("piercing")) {
            data.damageMap.put(DamageType.PIERCING, GsonHelper.getAsDouble(json, "piercing"));
        }
        if (json.has("slashing")) {
            data.damageMap.put(DamageType.SLASHING, GsonHelper.getAsDouble(json, "slashing"));
        }
        if (json.has("bludgeoning")) {
            data.damageMap.put(DamageType.BLUDGEONING, GsonHelper.getAsDouble(json, "bludgeoning"));
        }
        if (json.has("fire")) {
            data.damageMap.put(DamageType.FIRE, GsonHelper.getAsDouble(json, "fire"));
        }

        // Загружаем скорость атаки
        if (json.has("attack_speed")) {
            data.attackSpeed = GsonHelper.getAsDouble(json, "attack_speed");
        }

        // Загружаем шансы эффектов
        if (json.has("fire_chance")) {
            data.effectChances.put(DamageType.FIRE, GsonHelper.getAsDouble(json, "fire_chance"));
        }

        return data;
    }

    public Map<DamageType, Double> getDamageMap() {
        return damageMap;
    }

    public Double getAttackSpeed() {
        return attackSpeed;
    }

    public double getEffectChance(DamageType type) {
        return effectChances.getOrDefault(type, 0.0);
    }

    public boolean hasAttackSpeed() {
        return attackSpeed != null;
    }

    public boolean hasEffectChance(DamageType type) {
        return effectChances.containsKey(type);
    }

    public double getTotalDamage() {
        return damageMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public boolean isEmpty() {
        return damageMap.isEmpty() && !hasAttackSpeed() && effectChances.isEmpty();
    }
}
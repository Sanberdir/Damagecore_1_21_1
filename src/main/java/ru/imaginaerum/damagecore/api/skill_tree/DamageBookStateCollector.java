package ru.imaginaerum.damagecore.api.skill_tree;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;

import java.util.*;

public final class DamageBookStateCollector {
    private DamageBookStateCollector() {}

    public static class ProtectionData {
        public final Map<DamageType, DamageResistance> armorResistances;
        public final Map<DamageType, Float> foodProtection;
        public final List<EffectDisplayInfo> activeEffects;

        public ProtectionData(Map<DamageType, DamageResistance> armorResistances,
                              Map<DamageType, Float> foodProtection,
                              List<EffectDisplayInfo> activeEffects) {
            this.armorResistances = armorResistances;
            this.foodProtection = foodProtection;
            this.activeEffects = activeEffects;
        }

        public boolean hasFoodProtection() {
            return foodProtection.values().stream().anyMatch(v -> v > 0);
        }

        public boolean hasActiveEffects() {
            return !activeEffects.isEmpty();
        }
    }

    public static class EffectDisplayInfo {
        public final DamageType damageType;
        public final float armorPercent;
        public final float foodPercent;
        public final int remainingSeconds;
        public final String source; // "armor", "food", "potion", etc

        public EffectDisplayInfo(DamageType damageType, float armorPercent, float foodPercent,
                                 int remainingSeconds, String source) {
            this.damageType = damageType;
            this.armorPercent = armorPercent;
            this.foodPercent = foodPercent;
            this.remainingSeconds = remainingSeconds;
            this.source = source;
        }

        public float getTotalPercent() {
            return armorPercent + foodPercent;
        }

        public boolean isTemporary() {
            return remainingSeconds > 0;
        }
    }

    public static ProtectionData collectProtectionData(Player player) {
        Map<DamageType, DamageResistance> armorTotals = new EnumMap<>(DamageType.class);
        Map<DamageType, Float> foodTotals = new EnumMap<>(DamageType.class);
        List<EffectDisplayInfo> activeEffects = new ArrayList<>();

        if (player == null) return new ProtectionData(armorTotals, foodTotals, activeEffects);

        // Собираем защиту от брони
        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armorItem)) continue;

            Map<DamageType, DamageResistance> part =
                    DamageArmorModifier.getDamageResistances(armorItem.getMaterial().value(), armorItem.getType());

            for (Map.Entry<DamageType, DamageResistance> e : part.entrySet()) {
                DamageResistance dr = e.getValue();
                if (dr.getFlat() <= 0 && dr.getPercent() <= 0) continue;

                armorTotals.merge(
                        e.getKey(),
                        new DamageResistance(dr.getFlat(), dr.getPercent()),
                        (a, b) -> new DamageResistance(
                                a.getFlat() + b.getFlat(),
                                Math.min(1.0f, a.getPercent() + b.getPercent())
                        )
                );
            }
        }

        // Собираем защиту от эффектов еды и формируем информацию для отображения
        FoodProtectionManager foodManager = FoodProtectionCapability.get(player);
        if (foodManager != null) {
            Map<DamageType, List<FoodProtectionEffect>> foodEffects = foodManager.getActiveEffects();

            // Добавляем эффекты от брони
            for (Map.Entry<DamageType, DamageResistance> entry : armorTotals.entrySet()) {
                DamageType type = entry.getKey();
                DamageResistance dr = entry.getValue();

                if (dr.getPercent() > 0) {
                    activeEffects.add(new EffectDisplayInfo(
                            type,
                            dr.getPercent(),
                            0.0f,
                            0, // Постоянный эффект
                            "armor"
                    ));
                }
            }

            // Добавляем эффекты от еды
            for (Map.Entry<DamageType, List<FoodProtectionEffect>> entry : foodEffects.entrySet()) {
                DamageType type = entry.getKey();
                List<FoodProtectionEffect> list = entry.getValue();

                float totalFoodPercent = 0f;
                int maxTicks = 0;

                for (FoodProtectionEffect effect : list) {
                    totalFoodPercent += effect.getProtectionPercent();
                    if (effect.getRemainingTicks() > maxTicks) {
                        maxTicks = effect.getRemainingTicks();
                    }
                }

                if (totalFoodPercent > 1f) totalFoodPercent = 1f;

                if (totalFoodPercent > 0) {
                    boolean found = false;

                    for (int i = 0; i < activeEffects.size(); i++) {
                        EffectDisplayInfo existing = activeEffects.get(i);
                        if (existing.damageType == type) {
                            activeEffects.set(i, new EffectDisplayInfo(
                                    type,
                                    existing.armorPercent,
                                    totalFoodPercent,
                                    maxTicks / 20,
                                    "food"
                            ));
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        activeEffects.add(new EffectDisplayInfo(
                                type,
                                0.0f,
                                totalFoodPercent,
                                maxTicks / 20,
                                "food"
                        ));
                    }

                    foodTotals.put(type, totalFoodPercent);
                }
            }

        } else {
            // Если нет менеджера еды, все равно добавляем эффекты от брони
            for (Map.Entry<DamageType, DamageResistance> entry : armorTotals.entrySet()) {
                DamageType type = entry.getKey();
                DamageResistance dr = entry.getValue();

                if (dr.getPercent() > 0) {
                    activeEffects.add(new EffectDisplayInfo(
                            type,
                            dr.getPercent(),
                            0.0f,
                            0,
                            "armor"
                    ));
                }
            }
        }

        // Сортируем эффекты по типу урона для стабильного отображения
        activeEffects.sort(Comparator.comparing(e -> e.damageType.name()));

        return new ProtectionData(armorTotals, foodTotals, activeEffects);
    }
}
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Информация о зелье (или атаке моба), наложившем один или несколько mob-эффектов на игрока.
 * Хранится только на клиенте, исключительно для отображения в GUI —
 * не влияет на игровую логику и не синхронизируется с сервером.
 */
public final class PotionEffectEntry {

    private final ItemStack potionStack;
    private final PotionApplicationType applicationType;
    private final List<MobEffect> grantedEffects;

    /**
     * Тип сущности-источника (тот, кто бросил зелье / выстрелил / ударил).
     * Null, если источник неизвестен или неприменим (например, DRINK — игрок сам себя).
     * Храним именно EntityType, а не саму Entity: к моменту рендера исходная сущность
     * может уже исчезнуть (деспавн снаряда, смерть моба), а тип всегда доступен
     * сразу в момент события и не требует отдельной синхронизации.
     */
    private final EntityType<?> sourceEntityType;

    public PotionEffectEntry(ItemStack potionStack,
                             PotionApplicationType applicationType,
                             List<MobEffect> grantedEffects) {
        this(potionStack, applicationType, grantedEffects, null);
    }

    public PotionEffectEntry(ItemStack potionStack,
                             PotionApplicationType applicationType,
                             List<MobEffect> grantedEffects,
                             EntityType<?> sourceEntityType) {
        this.potionStack      = potionStack;
        this.applicationType  = applicationType;
        this.grantedEffects   = List.copyOf(grantedEffects);
        this.sourceEntityType = sourceEntityType;
    }

    public ItemStack getPotionStack() {
        return potionStack;
    }

    public PotionApplicationType getApplicationType() {
        return applicationType;
    }

    public List<MobEffect> getGrantedEffects() {
        return grantedEffects;
    }

    public EntityType<?> getSourceEntityType() {
        return sourceEntityType;
    }

    public boolean grants(MobEffect effect) {
        return grantedEffects.contains(effect);
    }

    /**
     * Два entry считаются "тем же источником" (и потому стакаются под одним значком в GUI),
     * если это одно и то же зелье (тип предмета + компоненты/NBT), тот же способ наложения
     * и тот же тип сущности-источника. Набор эффектов сравнивается тоже — на случай, если
     * одно и то же зелье почему-то дало разные эффекты (защита от редких edge-кейсов).
     * Конкретное время/длительность MobEffectInstance здесь не участвует — этот класс
     * их вообще не хранит, только сам факт "чем и кто наложил".
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PotionEffectEntry other)) return false;

        return applicationType == other.applicationType
                && sourceEntityType == other.sourceEntityType
                && ItemStack.isSameItemSameComponents(potionStack, other.potionStack)
                && grantedEffects.equals(other.grantedEffects);
    }

    @Override
    public int hashCode() {
        // ItemStack сам по себе не даёт стабильный/осмысленный hashCode для componentов,
        // поэтому используем связку Item + количество компонентов как приближение.
        // Основную работу по корректности делает equals (ItemStack.isSameItemSameComponents),
        // hashCode лишь обязан быть согласован с ним (равные объекты -> равный хэш).
        return Objects.hash(
                potionStack.getItem(),
                applicationType,
                sourceEntityType,
                grantedEffects
        );
    }
}
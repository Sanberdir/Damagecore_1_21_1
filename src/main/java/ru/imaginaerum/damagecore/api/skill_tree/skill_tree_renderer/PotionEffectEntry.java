package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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
}
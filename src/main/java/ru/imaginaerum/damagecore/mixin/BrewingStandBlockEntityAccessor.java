package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandBlockEntityAccessor {

    // Геттер для получения значения brewTime
    @Accessor("brewTime")
    int getBrewTime();

    // Сеттер для изменения значения brewTime
    @Accessor("brewTime")
    void setBrewTime(int brewTime);
}
package ru.imaginaerum.damagecore.mixin.mobs;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSkeleton.class)
public class SkeletonWeaponMixin {

    @Unique
    private void damagecore1_21_1Neo$onAfterPopulateEquipment(ServerLevelAccessor level, DifficultyInstance instance, MobSpawnType mobSpawnType, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        RandomSource random = level.getRandom();
            if (random.nextFloat() < 0.95f) {
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.085f);
        }
    }
    @Inject(
            method = "finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;",
            at = @At(
                    value = "INVOKE",
                    // Перехватываем момент СРАЗУ после того, как ванильный код выдал мобу лук
                    target = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;populateDefaultEquipmentSlots(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/DifficultyInstance;)V",
                    shift = At.Shift.AFTER
        )
    )
    private void skeletonEquipment(ServerLevelAccessor level, DifficultyInstance instance, MobSpawnType mobSpawnType, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        if (skeleton instanceof Skeleton || skeleton instanceof Stray || skeleton instanceof Bogged) {
            damagecore1_21_1Neo$onAfterPopulateEquipment(level, instance, mobSpawnType, spawnGroupData, cir);
        }
    }
}

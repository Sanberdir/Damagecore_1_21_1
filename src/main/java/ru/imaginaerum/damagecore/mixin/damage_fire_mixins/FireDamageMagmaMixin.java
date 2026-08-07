package ru.imaginaerum.damagecore.mixin.damage_fire_mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(MagmaBlock.class)
public class FireDamageMagmaMixin {

    @Inject(
            method = "stepOn",
            at = @At("HEAD")
    )
    private void damagecore$onStepOn(Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;

        // 1.21.1: Извлекаем Frost Walker из реестра через ResourceKey (Enchantments.FROST_WALKER)
        var frostWalkerHolder = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.FROST_WALKER);

        // Проверяем скрытность или уровень Ледохода на сущности
        if (living.isSteppingCarefully() || EnchantmentHelper.getEnchantmentLevel(frostWalkerHolder, living) > 0) {
            return;
        }

        // 1) Устанавливаем последний тип урона для IHasDamageType
        if (living instanceof IHasDamageType has) {
            has.setLastDamageType(DamageType.FIRE);
        }

        // 2) Добавляем в DamageContext
        DamageContext.add(living, DamageType.FIRE, 1.0F);
    }
}

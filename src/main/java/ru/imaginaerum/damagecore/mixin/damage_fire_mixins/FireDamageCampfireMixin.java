package ru.imaginaerum.damagecore.mixin.damage_fire_mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(CampfireBlock.class)
public class FireDamageCampfireMixin {

    @Inject(
            method = "entityInside",
            at = @At("HEAD")
    )
    private void damagecore$entityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        // Только LivingEntity
        if (!(entity instanceof LivingEntity living)) return;

        // Игнорируем если костёр не горит или Frost Walker
        if (!state.getValue(CampfireBlock.LIT) || entity.isSteppingCarefully()) return;

        // Устанавливаем тип урона FIRE
        if (living instanceof IHasDamageType has) {
            has.setLastDamageType(DamageType.FIRE);
        }

        // Добавляем в DamageContext урон от костра
        float fireDamage = state.getValue(CampfireBlock.LIT) ? 1.0F : 0.0F;// можно заменить на реальное this.fireDamage, если доступно
        DamageContext.add(living, DamageType.FIRE, fireDamage);

    }
}
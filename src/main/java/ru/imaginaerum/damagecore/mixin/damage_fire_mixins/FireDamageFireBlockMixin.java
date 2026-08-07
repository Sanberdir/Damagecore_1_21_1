package ru.imaginaerum.damagecore.mixin.damage_fire_mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(BaseFireBlock.class)
public class FireDamageFireBlockMixin {

    @Inject(
            method = "entityInside",
            at = @At("HEAD")
    )
    private void damagecore$onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) return;
        if (entity.fireImmune()) return;
        DamageContext.add(living, DamageType.FIRE, 1.0F);
        if (living instanceof IHasDamageType has) {
            has.setLastDamageType(DamageType.FIRE);
        }
    }
}
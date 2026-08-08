package ru.imaginaerum.damagecore.mixin.tree_mixins.shooting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting.HundredArmedMixinContext;

@Mixin(BowItem.class)
public class BowItemServerMixin {

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    public void captureEntity(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        HundredArmedMixinContext.currentEntity.set(entity);
    }

    @Inject(method = "releaseUsing", at = @At("RETURN"))
    public void clearEntity(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        HundredArmedMixinContext.currentEntity.remove();
    }

    // Перехватываем локальную переменную i (заряд) сразу после её вычисления
    @ModifyVariable(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F",
                    shift = At.Shift.BEFORE
            ),
            ordinal = 0
    )
    private int modifyCharge(int charge) {
        LivingEntity entity = HundredArmedMixinContext.currentEntity.get();
        if (entity instanceof ServerPlayer player
                && SkillTreeServerHandler.isNodeLearned(player, "hundred_armed")) {
            return Math.min(charge * 2, 20);
        }
        return charge;
    }
}
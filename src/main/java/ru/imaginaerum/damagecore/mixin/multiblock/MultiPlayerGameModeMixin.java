package ru.imaginaerum.damagecore.mixin.multiblock;   // подберите свой пакет

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.imaginaerum.damagecore.Init.blocks.custom.multiblock.IDelegateProvider;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @ModifyVariable(method = "startDestroyBlock", at = @At("HEAD"), argsOnly = true)
    private BlockPos df$start(BlockPos pos) { return df$center(pos); }

    @ModifyVariable(method = "continueDestroyBlock", at = @At("HEAD"), argsOnly = true)
    private BlockPos df$continue(BlockPos pos) { return df$center(pos); }

    @Unique
    private static BlockPos df$center(BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return pos;
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof IDelegateProvider d) {
            BlockPos c = d.getDelegatePos(s, pos);
            if (level.getBlockState(c).is(s.getBlock())) return c;
        }
        return pos;
    }
}
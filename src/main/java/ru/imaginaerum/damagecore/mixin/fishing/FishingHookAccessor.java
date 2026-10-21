package ru.imaginaerum.damagecore.mixin.fishing;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("nibble")
    int deepfishing$getNibble();

    @Accessor("nibble")
    void deepfishing$setNibble(int value);
    @Accessor("timeUntilHooked")
    int deepfishing$getTimeUntilHooked();
    @Accessor("luck")
    int deepfishing$getLuck();
}

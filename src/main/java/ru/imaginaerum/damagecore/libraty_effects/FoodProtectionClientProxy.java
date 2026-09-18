package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class FoodProtectionClientProxy {

    public static void apply(CompoundTag data) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        net.minecraft.core.HolderLookup.Provider provider = player.level().registryAccess();
        FoodProtectionManager manager = FoodProtectionCapability.get(player);

        if (manager != null) {
            manager.load(data, provider);

        }
    }

}

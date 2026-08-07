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

        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Получаем провайдер реестров из клиентского мира
        net.minecraft.core.HolderLookup.Provider provider = player.level().registryAccess();

        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Используем статический метод get() вместо удаленного getCapability
        FoodProtectionManager manager = FoodProtectionCapability.get(player);

        if (manager != null) {
            // ✅ ИСПРАВЛЕНО: Передаем полученный провайдер в метод load()
            manager.load(data, provider);
        }
    }

}

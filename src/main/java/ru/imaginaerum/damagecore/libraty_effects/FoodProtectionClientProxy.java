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

        // ДОБАВЛЕНО: подтверждаем что пакет вообще дошёл до клиента
        System.out.println("[FoodProtection] Client received sync packet, data=" + data);

        net.minecraft.core.HolderLookup.Provider provider = player.level().registryAccess();
        FoodProtectionManager manager = FoodProtectionCapability.get(player);

        if (manager != null) {
            manager.load(data, provider);
            // ДОБАВЛЕНО: сколько эффектов распарсилось на клиенте после load()
            System.out.println("[FoodProtection] Client manager loaded, now has "
                    + manager.getAllEffects().size() + " effects");
        } else {
            // ДОБАВЛЕНО: если capability на клиенте вообще не найдена
            System.err.println("[FoodProtection] Client manager is NULL!");
        }
    }

}

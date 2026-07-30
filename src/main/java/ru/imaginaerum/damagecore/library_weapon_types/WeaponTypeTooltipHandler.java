package ru.imaginaerum.damagecore.library_weapon_types;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class WeaponTypeTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Получаем ID предмета через ванильный BuiltInRegistries, актуальный для NeoForge
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        // Проверка на случай, если предмет не зарегистрирован (возвращает "minecraft:air")
        if (itemId.equals(BuiltInRegistries.ITEM.getDefaultKey())) return;

        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return;

        // Вставляем тип первой строкой (индекс 1 — сразу после названия предмета)
        event.getToolTip().add(1, type.getDisplayName());
    }
}

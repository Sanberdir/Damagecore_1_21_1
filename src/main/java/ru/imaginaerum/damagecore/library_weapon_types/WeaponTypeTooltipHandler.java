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

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId.equals(BuiltInRegistries.ITEM.getDefaultKey())) return;

        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return;

        // Защита: если список тултипа ещё пуст (нет даже названия предмета),
        // вставка по индексу 1 упадёт с IndexOutOfBoundsException.
        // В таком случае просто добавляем строку в конец.
        var tooltip = event.getToolTip();
        int insertIndex = Math.min(1, tooltip.size());
        tooltip.add(insertIndex, type.getDisplayName());
    }
}
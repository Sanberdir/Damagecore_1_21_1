package ru.imaginaerum.damagecore.mixin.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Collectors;

@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin extends Item {

    public ArmorItemMixin(Properties properties) {
        super(properties);
    }

    /**
     * Перехватываем получение стандартных модификаторов атрибутов
     * и отфильтровываем базовую броню и её прочность (toughness).
     */
    @Inject(method = "getDefaultAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void damagecore$onGetDefaultAttributeModifiers(CallbackInfoReturnable<ItemAttributeModifiers> cir) {
        ItemAttributeModifiers original = cir.getReturnValue();

        // Фильтруем список модификаторов через Stream API
        var filteredModifiers = original.modifiers().stream()
                .filter(entry -> {
                    Holder<Attribute> attribute = entry.attribute();
                    // Удаляем только стандартные атрибуты защиты
                    return !attribute.is(Attributes.ARMOR) && !attribute.is(Attributes.ARMOR_TOUGHNESS);
                })
                .collect(Collectors.toList());

        // Создаем новый контейнер модификаторов с сохранением флага отображения в подсказках (showInTooltip)
        ItemAttributeModifiers updated = new ItemAttributeModifiers(filteredModifiers, original.showInTooltip());

        cir.setReturnValue(updated);
    }
}

package ru.imaginaerum.damagecore.Init.fishing.food;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class FishFood {
    public static final String KEY = "fish_kg";
    private FishFood() {}

    /** Записывает вес в предмет и пересчитывает сытость. Таблица Foods считается для 1 кг. */
    public static void apply(ItemStack stack, float kg) {
        FoodProperties base = stack.getItem().components().get(DataComponents.FOOD);
        if (base == null) return;

        kg = Math.round(kg * 100f) / 100f;   // два знака, чтобы одинаковые рыбы стакались

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putFloat(KEY, kg);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        stack.set(DataComponents.FOOD, new FoodProperties(
                Math.max(1, Math.round(base.nutrition() * kg)),
                base.saturation() * kg,            // в 1.21.1 это абсолютная сытость, масштабируем так же
                base.canAlwaysEat(),
                base.eatSeconds(),
                base.usingConvertsTo(),
                base.effects()));
    }

    /** Вес из предмета, 0 если не задан. */
    public static float getKg(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0f : d.copyTag().getFloat(KEY);
    }
}
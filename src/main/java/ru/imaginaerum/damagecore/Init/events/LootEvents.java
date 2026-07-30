package ru.imaginaerum.damagecore.Init.events;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Optional;

@EventBusSubscriber(modid = "damagecore")
public class LootEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity killedEntity = event.getEntity();

        // Выполняем проверку только на сервере и только если на мобе есть наша метка электричества
        if (!killedEntity.level().isClientSide() && killedEntity.getPersistentData().contains("damaged_chain_light_arrow")) {

            // Проходимся по всем сущностям предметов, которые собираются выпасть из моба
            for (ItemEntity itemEntity : event.getDrops()) {
                ItemStack originalStack = itemEntity.getItem();

                // ИСПРАВЛЕНО: Оборачиваем ItemStack в SingleRecipeInput для совместимости с 1.21.1
                SingleRecipeInput recipeInput = new SingleRecipeInput(originalStack);

                // Ищем ванильный рецепт переплавки (печи)
                Optional<RecipeHolder<SmeltingRecipe>> recipe = killedEntity.level().getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, recipeInput, killedEntity.level());

                // Если рецепт прожарки существует (например, из сырой говядины получается стейк)
                if (recipe.isPresent()) {
                    // Получаем результат прожарки, сохраняя изначальное количество выпавших предметов
                    ItemStack cookedResult = recipe.get().value().getResultItem(killedEntity.level().registryAccess()).copy();
                    cookedResult.setCount(originalStack.getCount());

                    // Подменяем сырой стак в сущности дропа на прожаренный аналог
                    itemEntity.setItem(cookedResult);
                }
            }
        }
    }
}

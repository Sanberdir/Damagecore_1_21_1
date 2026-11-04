package ru.imaginaerum.damagecore.Init.fishing.food;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.fishing.weight.FishWeight;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class FishDrops {
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof AbstractFish fish)) return;
        float kg = FishWeight.storedKg(fish);
        if (kg <= 0f) return;

        for (ItemEntity ie : e.getDrops()) {
            ItemStack s = ie.getItem();
            if (!s.is(ItemTags.FISHES)) continue;   // костная мука не трогаем
            ItemStack c = s.copy();
            FishFood.apply(c, kg);
            ie.setItem(c);
        }
    }
}
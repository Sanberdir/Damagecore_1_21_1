package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.alchemy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "damagecore")
public class AlchemyXpHandler {

    private static final String NODE_ID = "alchemy_experience";
    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final String NODE_LEVEL_PREFIX = "node_level_";
    private static final int XP_REWARD = 5;

    // Запоминаем предыдущие зелья
    private static final List<ItemStack> lastPotions = new ArrayList<>();

    // Был ли реальный крафт
    private static boolean brewed = false;

    public static void tryGiveXp(ServerPlayer player) {
        if (!brewed) return;
        if (!hasNode(player)) return;

        player.giveExperiencePoints(XP_REWARD);
        brewed = false;
    }

    // 1. ДО варки
    @SubscribeEvent
    public static void onBrewPre(PotionBrewEvent.Pre event) {
        lastPotions.clear();

        // В NeoForge 1.21.1 предметы извлекаются методом event.getItem(index)
        // Индексы 0, 1, 2 соответствуют трем нижним слотам варочной стойки
        for (int i = 0; i < 3; i++) {
            lastPotions.add(event.getItem(i).copy());
        }

        brewed = false;
    }

    // 2. ПОСЛЕ варки
    @SubscribeEvent
    public static void onBrewPost(PotionBrewEvent.Post event) {
        for (int i = 0; i < 3; i++) {
            ItemStack before = lastPotions.get(i);
            ItemStack after = event.getItem(i); // Читаем результаты варки через event.getItem(i)

            // Сравниваем предметы и их новые Data Components
            if (!ItemStack.isSameItemSameComponents(before, after)) {
                brewed = true;
                return;
            }
        }
    }

    // 3. Игрок забирает готовое зелье из стойки
    @SubscribeEvent
    public static void onTake(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!brewed) return; // Не было реальной варки

        ItemStack stack = event.getStack();
        if (stack.isEmpty()) return;

        // Извлекаем содержимое зелья через Data Components
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);

        // Если компонента нет или в итераторе эффектов нет элементов (например, бутылочка воды)
        if (contents == null || !contents.getAllEffects().iterator().hasNext()) {
            return;
        }

        if (!hasNode(player)) return;

        player.giveExperiencePoints(XP_REWARD);
        brewed = false; // Сбрасываем флаг крафта
    }

    private static boolean hasNode(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag modTag = persisted.getCompound(ROOT_KEY);

        for (String key : modTag.getAllKeys()) {
            if (!key.startsWith("tree_")) continue;

            CompoundTag tree = modTag.getCompound(key);
            if (tree.contains(NODE_LEVEL_PREFIX + NODE_ID)) {
                if (tree.getInt(NODE_LEVEL_PREFIX + NODE_ID) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}

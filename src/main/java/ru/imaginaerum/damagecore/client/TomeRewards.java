package ru.imaginaerum.damagecore.client;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

public final class TomeRewards {
    private TomeRewards() {}
    public static final int FINAL_STEP = 12;

    private static final String CLAIMED_TAG = "damagecore_tome_rewarded";
    /** Предмет за исход с жителем: индекс = rescue - 1. */
    private static final List<Supplier<ItemStack>> OUTCOME = List.of(
            () -> new ItemStack(Items.EMERALD, 5),     // 1: пленник выжил
            () -> new ItemStack(Items.IRON_INGOT, 5),  // 2: хорошая цель, но что-то пошло не так
            () -> new ItemStack(Items.COAL, 5)         // 3: ужасный поступок
    );

    /** Предмет за ступень света: индекс = TomeTitles.lightLevel(good). */
    private static final List<Supplier<ItemStack>> LIGHT = List.of(
            () -> new ItemStack(Items.GOLD_INGOT, 3),      // 0: свет
            () -> new ItemStack(Items.QUARTZ, 3),          // 1: почти свет
            () -> new ItemStack(Items.AMETHYST_SHARD, 3),  // 2: почти тьма
            () -> new ItemStack(Items.OBSIDIAN, 3)         // 3: тьма
    );
    public static void grantFinal(ServerPlayer p) {
        if (p.getPersistentData().getBoolean(CLAIMED_TAG)) return;
        p.getPersistentData().putBoolean(CLAIMED_TAG, true);

        grant(p,
                p.getData(ModAttachments.TOME_RESCUE),
                p.getData(ModAttachments.TOME_GOOD),
                p.getData(ModAttachments.TOME_BAD));
    }
    public static void grant(ServerPlayer p, int rescue, int good, int bad) {
        if (rescue >= 1 && rescue <= OUTCOME.size()) {
            giveOrDrop(p, OUTCOME.get(rescue - 1).get());
        }
        if (good + bad >= TomeTitles.DEEDS_NEEDED) {
            giveOrDrop(p, LIGHT.get(TomeTitles.lightLevel(good)).get());
        }
    }

    /** Кладёт в инвентарь, что не влезло, выбрасывает под ноги игроку. */
    public static void giveOrDrop(ServerPlayer p, ItemStack stack) {
        ItemStack s = stack.copy();
        p.getInventory().add(s);
        if (!s.isEmpty()) p.drop(s, false);
    }
}
// PotionTabRenderer.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionApplicationType;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionEffectEntry;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionTrackingClient;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.FoodEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.MobEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.PlayerEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.PotionEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.TrapEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.UnknownEffectIconRenderer;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;

import java.util.*;
import static ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.EffectIconLayout.*;

/**
 * Компоновка вкладки "Эффекты": заголовки, ряды и группировка данных.
 * Отрисовку конкретных значков + их тултипов делегирует:
 * {@link FoodEffectIconRenderer}, {@link PotionEffectIconRenderer},
 * {@link MobEffectIconRenderer}, {@link PlayerEffectIconRenderer},
 * {@link TrapEffectIconRenderer}, {@link UnknownEffectIconRenderer}.
 */
public final class PotionTabRenderer {

    private PotionTabRenderer() {}

    private static boolean isPlayerSource(EntityType<?> sourceEntityType) {
        return sourceEntityType == null || sourceEntityType == EntityType.PLAYER;
    }

    public static void render(GuiGraphics gui, int areaX, int areaY, Minecraft mc, int mouseX, int mouseY) {
        Player player = mc.player;
        if (player == null) return;

        FoodProtectionManager manager = FoodProtectionCapability.get(player);
        List<FoodProtectionEffect> allActive = manager == null
                ? List.of()
                : manager.getAllEffects().stream().filter(e -> !e.isExpired()).toList();

        Set<Holder<MobEffect>> foodGranted = new HashSet<>();
        for (FoodProtectionEffect eff : allActive) {
            for (MobEffectInstance inst : eff.getMobEffects()) {
                foodGranted.add(inst.getEffect());
            }
        }

        List<MobEffectInstance> playerEffects = player.getActiveEffects().stream()
                .filter(e -> !foodGranted.contains(e.getEffect()))
                .toList();

        final int padL = 4, padT = 4;
        int x = areaX + padL;
        int y = areaY + padT;

        // ===== FOOD =====
        if (!allActive.isEmpty()) {
            gui.drawString(mc.font, Component.translatable("damagecore.potion_tab.food_header"), x, y, 0xFFFFFF, true);
            y += mc.font.lineHeight + 4;

            Map<Item, List<FoodProtectionEffect>> byItem = new LinkedHashMap<>();
            for (FoodProtectionEffect eff : allActive) {
                byItem.computeIfAbsent(eff.getItem(), k -> new ArrayList<>()).add(eff);
            }

            Item hoveredItem = null;
            List<FoodProtectionEffect> hoveredEffects = null;
            int iconX = x;

            for (var entry : byItem.entrySet()) {
                Item item = entry.getKey();

                FoodEffectIconRenderer.renderIcon(gui, iconX, y);
                FoodEffectIconRenderer.renderBar(gui, iconX, y + ICON_SIZE + BAR_GAP, item, entry.getValue());

                if (mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE) {
                    hoveredItem = item;
                    hoveredEffects = entry.getValue();
                }
                iconX += ICON_SIZE + GAP;
            }

            if (hoveredItem != null) {
                FoodEffectIconRenderer.renderTooltip(gui, mc, hoveredItem, hoveredEffects, mouseX, mouseY);
            }

            y += ICON_SIZE + ROW_GAP;
        }

        // ===== SPLIT PLAYER EFFECTS =====
        Map<PotionEffectEntry, List<MobEffectInstance>> byPotion = new LinkedHashMap<>();
        Map<EntityType<?>, List<MobEffectInstance>> byMobType = new LinkedHashMap<>();
        List<MobEffectInstance> byPlayer = new ArrayList<>();
        List<MobEffectInstance> byTrap   = new ArrayList<>();   // <-- NEW
        List<MobEffectInstance> unknown  = new ArrayList<>();

        for (MobEffectInstance inst : playerEffects) {
            MobEffect effect = inst.getEffect().value();
            PotionEffectEntry entry = PotionTrackingClient.get(effect);

            if (entry == null) {
                unknown.add(inst);
                continue;
            }

            // TRAP: ловушка/раздатчик — отдельная категория.   // <-- NEW
            if (entry.getApplicationType() == PotionApplicationType.TRAP) {
                byTrap.add(inst);
                continue;
            }

            if (isPlayerSource(entry.getSourceEntityType())) {
                ItemStack stack = entry.getPotionStack();
                if (stack != null && !stack.isEmpty()) {
                    byPotion.computeIfAbsent(entry, k -> new ArrayList<>()).add(inst);
                } else {
                    byPlayer.add(inst);
                }
            } else {
                byMobType.computeIfAbsent(entry.getSourceEntityType(), k -> new ArrayList<>()).add(inst);
            }
        }

        ItemStack hoveredPotion = null;
        PotionEffectEntry hoveredEntry = null;
        List<MobEffectInstance> hoveredPotionEffects = null;
        MobEffectInstance hoveredUnknown = null;
        EntityType<?> hoveredMobType = null;
        List<MobEffectInstance> hoveredMobEffects = null;
        List<MobEffectInstance> hoveredPlayerEffects = null;
        List<MobEffectInstance> hoveredTrapEffects = null;   // <-- NEW

        // ===== POTIONS =====
        boolean hasPotionRow = !byPotion.isEmpty() || !unknown.isEmpty();
        if (hasPotionRow) {
            gui.drawString(mc.font, Component.translatable("damagecore.potion_tab.potions_header"), x, y, 0xFFFFFF, true);
            y += mc.font.lineHeight + 4;

            int iconX = x;
            int rowY = y;

            for (var entry : byPotion.entrySet()) {
                PotionEffectEntry source = entry.getKey();
                ItemStack stack = source.getPotionStack();

                PotionEffectIconRenderer.renderIcon(gui, iconX, rowY);
                PotionEffectIconRenderer.renderBar(gui, iconX, rowY + ICON_SIZE + BAR_GAP, entry.getValue());

                if (mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= rowY && mouseY < rowY + ICON_SIZE) {
                    hoveredPotion = stack;
                    hoveredEntry = source;
                    hoveredPotionEffects = entry.getValue();
                }
                iconX += ICON_SIZE + GAP;
            }

            for (MobEffectInstance inst : unknown) {
                UnknownEffectIconRenderer.renderIcon(gui, mc, iconX, rowY, inst);
                UnknownEffectIconRenderer.renderBar(gui, iconX, rowY + ICON_SIZE + BAR_GAP, inst);

                if (mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= rowY && mouseY < rowY + ICON_SIZE) {
                    hoveredUnknown = inst;
                }
                iconX += ICON_SIZE + GAP;
            }

            y += ICON_SIZE + ROW_GAP;
        }

        // ===== PLAYERS (не зелье: стрелы, удары и т.п.) =====
        if (!byPlayer.isEmpty()) {
            gui.drawString(mc.font, Component.translatable("damagecore.potion_tab.players_header"), x, y, 0xFFFFFF, true);
            y += mc.font.lineHeight + 4;

            int pX = x;
            int pRowY = y;

            PlayerEffectIconRenderer.renderIcon(gui, pX, pRowY);
            PlayerEffectIconRenderer.renderBar(gui, pX, pRowY + ICON_SIZE + BAR_GAP, byPlayer);

            if (mouseX >= pX && mouseX < pX + ICON_SIZE && mouseY >= pRowY && mouseY < pRowY + ICON_SIZE) {
                hoveredPlayerEffects = byPlayer;
            }

            y += ICON_SIZE + ROW_GAP;
        }

        // ===== MOBS =====
        if (!byMobType.isEmpty()) {
            gui.drawString(mc.font, Component.translatable("damagecore.potion_tab.mobs_header"), x, y, 0xFFFFFF, true);
            y += mc.font.lineHeight + 4;

            int mobX = x;
            int mobRowY = y;

            for (var entry : byMobType.entrySet()) {
                EntityType<?> sourceType = entry.getKey();

                MobEffectIconRenderer.renderIcon(gui, mobX, mobRowY);
                MobEffectIconRenderer.renderBar(gui, mobX, mobRowY + ICON_SIZE + BAR_GAP, entry.getValue());

                if (mouseX >= mobX && mouseX < mobX + ICON_SIZE && mouseY >= mobRowY && mouseY < mobRowY + ICON_SIZE) {
                    hoveredMobType = sourceType;
                    hoveredMobEffects = entry.getValue();
                }
                mobX += ICON_SIZE + GAP;
            }

            y += ICON_SIZE + ROW_GAP;
        }

        // ===== TRAPS (ловушки/раздатчики) =====   // <-- NEW
        if (!byTrap.isEmpty()) {
            gui.drawString(mc.font, Component.translatable("damagecore.potion_tab.traps_header"), x, y, 0xFFFFFF, true);
            y += mc.font.lineHeight + 4;

            int tX = x;
            int tRowY = y;

            TrapEffectIconRenderer.renderIcon(gui, tX, tRowY);
            TrapEffectIconRenderer.renderBar(gui, tX, tRowY + ICON_SIZE + BAR_GAP, byTrap);

            if (mouseX >= tX && mouseX < tX + ICON_SIZE && mouseY >= tRowY && mouseY < tRowY + ICON_SIZE) {
                hoveredTrapEffects = byTrap;
            }

            y += ICON_SIZE + ROW_GAP;
        }

        // ===== TOOLTIPS =====
        if (hoveredPotion != null) {
            PotionEffectIconRenderer.renderTooltip(gui, mc, hoveredPotion, hoveredEntry, hoveredPotionEffects, mouseX, mouseY);
        }
        if (hoveredUnknown != null) {
            UnknownEffectIconRenderer.renderTooltip(gui, mc, hoveredUnknown, mouseX, mouseY);
        }
        if (hoveredPlayerEffects != null) {
            PlayerEffectIconRenderer.renderTooltip(gui, mc, hoveredPlayerEffects, mouseX, mouseY);
        }
        if (hoveredMobType != null) {
            MobEffectIconRenderer.renderTooltip(gui, mc, hoveredMobType, hoveredMobEffects, mouseX, mouseY);
        }
        if (hoveredTrapEffects != null) {
            TrapEffectIconRenderer.renderTooltip(gui, mc, hoveredTrapEffects, mouseX, mouseY);
        }
    }
}
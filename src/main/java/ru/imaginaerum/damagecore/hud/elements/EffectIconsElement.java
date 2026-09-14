package ru.imaginaerum.damagecore.hud.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionApplicationType;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionEffectEntry;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionTrackingClient;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.MobEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.PlayerEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.PotionEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.TrapEffectIconRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion.UnknownEffectIconRenderer;
import ru.imaginaerum.damagecore.hud.effect_hud.EffectCountBadgeRenderer;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;

import java.util.*;
import java.util.function.Consumer;

/**
 * HUD-элемент: один значок на каждый тип наложения, число эффектов рядом.
 * Иконки отрисовываются в 2 раза мельче, чем в исходных рендерерах (которые
 * рассчитаны на ICON_SIZE=10 для вкладки скилл-дерева) — масштабируем через PoseStack,
 * не трогая сами рендереры (они переиспользуются и там, где нужен полный размер).
 */
public class EffectIconsElement {

    private static final int BASE_ICON_SIZE = 10; // исходный размер, на который рассчитаны рендереры
    private static final float SCALE = 0.5F;       // уменьшение в 2 раза
    private static final int ICON_SIZE = (int) (BASE_ICON_SIZE * SCALE); // = 5, для раскладки по X
    private static final int ICON_GAP  = 6;

    private static final int ICON_Y = 4;
    private static final int ICON_START_X = 51;

    private static boolean isPlayerSource(EntityType<?> sourceEntityType) {
        return sourceEntityType == null || sourceEntityType == EntityType.PLAYER;
    }

    /**
     * Рисует значок (и, если нужно, бейдж с числом) в 2 раза мельче исходного.
     * drawCall получает GuiGraphics и рисует так, будто ICON_Y=0, x=0 — как если бы
     * рисовал в полный (10x10) размер; PoseStack-трансформация уменьшает результат.
     */
    private static void renderScaled(GuiGraphics gui, int screenX, int screenY, Consumer<GuiGraphics> drawCall) {
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(screenX, screenY, 0.0);
        pose.scale(SCALE, SCALE, 1.0F);
        drawCall.accept(gui);
        pose.popPose();
    }

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        // ===== Исключаем эффекты, выданные едой =====
        FoodProtectionManager manager = FoodProtectionCapability.get(mc.player);
        List<FoodProtectionEffect> allActiveFood = manager == null
                ? List.of()
                : manager.getAllEffects().stream().filter(e -> !e.isExpired()).toList();

        Set<Holder<MobEffect>> foodGranted = new HashSet<>();
        for (FoodProtectionEffect eff : allActiveFood) {
            for (MobEffectInstance inst : eff.getMobEffects()) {
                foodGranted.add(inst.getEffect());
            }
        }

        List<MobEffectInstance> playerEffects = mc.player.getActiveEffects().stream()
                .filter(e -> !foodGranted.contains(e.getEffect()))
                .toList();

        if (playerEffects.isEmpty()) return;

        // ===== Группировка по типу наложения =====
        List<MobEffectInstance> byPotion = new ArrayList<>();
        Map<EntityType<?>, List<MobEffectInstance>> byMobType = new LinkedHashMap<>();
        List<MobEffectInstance> byPlayer = new ArrayList<>();
        List<MobEffectInstance> byTrap   = new ArrayList<>();
        List<MobEffectInstance> unknown  = new ArrayList<>();

        for (MobEffectInstance inst : playerEffects) {
            MobEffect effect = inst.getEffect().value();
            PotionEffectEntry entry = PotionTrackingClient.get(effect);

            if (entry == null) {
                unknown.add(inst);
                continue;
            }

            if (entry.getApplicationType() == PotionApplicationType.TRAP) {
                byTrap.add(inst);
                continue;
            }

            if (isPlayerSource(entry.getSourceEntityType())) {
                ItemStack stack = entry.getPotionStack();
                if (stack != null && !stack.isEmpty()) {
                    byPotion.add(inst);
                } else {
                    byPlayer.add(inst);
                }
            } else {
                byMobType.computeIfAbsent(entry.getSourceEntityType(), k -> new ArrayList<>()).add(inst);
            }
        }

        int iconDrawX = ICON_START_X;

        // ===== Зелье =====
        if (!byPotion.isEmpty()) {
            final int count = byPotion.size();
            renderScaled(gui, iconDrawX, ICON_Y, g -> {
                PotionEffectIconRenderer.renderIcon(g, 0, 0);
                EffectCountBadgeRenderer.render(g, mc.font, 0, 0, BASE_ICON_SIZE, count);
            });
            iconDrawX += ICON_SIZE + ICON_GAP;
        }

        // ===== Неизвестный источник =====
        for (MobEffectInstance inst : unknown) {
            renderScaled(gui, iconDrawX, ICON_Y, g -> UnknownEffectIconRenderer.renderIcon(g, mc, 0, 0, inst));
            iconDrawX += ICON_SIZE + ICON_GAP;
        }

        // ===== Игрок =====
        if (!byPlayer.isEmpty()) {
            final int count = byPlayer.size();
            renderScaled(gui, iconDrawX, ICON_Y, g -> {
                PlayerEffectIconRenderer.renderIcon(g, 0, 0);
                EffectCountBadgeRenderer.render(g, mc.font, 0, 0, BASE_ICON_SIZE, count);
            });
            iconDrawX += ICON_SIZE + ICON_GAP;
        }

        // ===== Мобы =====
        for (var entry : byMobType.entrySet()) {
            final int count = entry.getValue().size();
            renderScaled(gui, iconDrawX, ICON_Y, g -> {
                MobEffectIconRenderer.renderIcon(g, 0, 0);
                EffectCountBadgeRenderer.render(g, mc.font, 0, 0, BASE_ICON_SIZE, count);
            });
            iconDrawX += ICON_SIZE + ICON_GAP;
        }

        // ===== Ловушки =====
        if (!byTrap.isEmpty()) {
            final int count = byTrap.size();
            renderScaled(gui, iconDrawX, ICON_Y, g -> {
                TrapEffectIconRenderer.renderIcon(g, 0, 0);
                EffectCountBadgeRenderer.render(g, mc.font, 0, 0, BASE_ICON_SIZE, count);
            });
        }
    }
}
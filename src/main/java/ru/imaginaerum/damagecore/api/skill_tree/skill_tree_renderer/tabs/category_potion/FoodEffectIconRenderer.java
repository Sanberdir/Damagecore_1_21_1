// FoodEffectIconRenderer.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

import java.util.*;

/** Значок "источник — еда" (общая иконка категории), полоска и тултип с % защиты по типам урона. */
public final class FoodEffectIconRenderer {

    private static final int ICON_U = 3;
    private static final int ICON_V = 4;
    private static final int ICON_REGION = 10;

    /** Самокалибрующийся кэш максимального remainingTicks на предмет — чтобы полоска не "прыгала". */
    private static final Map<Item, Integer> maxTicksSeen = new HashMap<>();

    private FoodEffectIconRenderer() {}

    public static void renderIcon(GuiGraphics gui, int x, int y) {
        StatusIconSheet.blit(gui, x, y, ICON_U, ICON_V, ICON_REGION);
    }

    public static void renderBar(GuiGraphics gui, int x, int y, Item item, List<FoodProtectionEffect> effects) {
        DurationBarRenderer.render(gui, x, y, EffectIconLayout.ICON_SIZE, getFraction(item, effects));
    }

    /** Fraction = максимальный remainingTicks среди эффектов этой еды / максимум, увиденный ранее для неё. */
    private static float getFraction(Item item, List<FoodProtectionEffect> effects) {
        int maxRemaining = 0;
        for (FoodProtectionEffect eff : effects) {
            maxRemaining = Math.max(maxRemaining, eff.getRemainingTicks());
        }
        if (maxRemaining <= 0) return 0f;

        int knownMax = maxTicksSeen.getOrDefault(item, 0);
        if (maxRemaining > knownMax) {
            knownMax = maxRemaining;
            maxTicksSeen.put(item, knownMax);
        }
        return knownMax > 0 ? (float) maxRemaining / knownMax : 1f;
    }

    public static void renderTooltip(GuiGraphics gui, Minecraft mc, Item item,
                                     List<FoodProtectionEffect> effects,
                                     int mouseX, int mouseY) {

        List<Component> lines = new ArrayList<>();

        // Название еды
        lines.add(
                new ItemStack(item)
                        .getHoverName()
                        .copy()
                        .withStyle(s -> s.withColor(0xFFFF55))
        );

        /*
         * ==========================================
         * ЗАЩИТА ОТ ТИПОВ УРОНА
         * ==========================================
         *
         * DamageType может быть null для обычной еды,
         * которая просто даёт MobEffect.
         *
         * Поэтому null сюда не добавляем.
         */
        Map<DamageType, Float> percent =
                new EnumMap<>(DamageType.class);

        Map<DamageType, Integer> time =
                new EnumMap<>(DamageType.class);

        for (FoodProtectionEffect eff : effects) {

            DamageType type = eff.getDamageType();

            // Обычная еда без защиты от урона.
            if (type == null) {
                continue;
            }

            percent.merge(
                    type,
                    eff.getProtectionPercent(),
                    Float::sum
            );

            time.merge(
                    type,
                    eff.getRemainingTicks(),
                    Math::max
            );
        }

        /*
         * Показываем защиту только если она действительно есть.
         */
        for (var e : percent.entrySet()) {

            DamageType type = e.getKey();

            lines.add(
                    Component.translatable(
                            EffectTextUtils.getDamageTypeKey(type)
                    ).append(
                            Component.literal(
                                    String.format(
                                            ": §a%.0f%%§7 (%s)§r",
                                            Math.min(e.getValue(), 1f) * 100,
                                            EffectTextUtils.formatTicks(
                                                    time.getOrDefault(type, 0)
                                            )
                                    )
                            )
                    )
            );
        }

        /*
         * ==========================================
         * VANILLA / MOB EFFECTS ОТ ЕДЫ
         * ==========================================
         */

        var player = mc.player;

        Map<Holder<MobEffect>, MobEffectInstance> best =
                new LinkedHashMap<>();

        for (FoodProtectionEffect eff : effects) {

            for (MobEffectInstance saved : eff.getMobEffects()) {

                Holder<MobEffect> type = saved.getEffect();

                if (best.containsKey(type)) {
                    continue;
                }

                MobEffectInstance live =
                        player != null
                                ? player.getEffect(type)
                                : null;

                if (live != null) {
                    best.put(type, live);
                } else {
                    /*
                     * Если эффект уже успел исчезнуть у игрока,
                     * всё равно используем сохранённый эффект,
                     * который пришёл от еды.
                     */
                    best.put(type, saved);
                }
            }
        }

        /*
         * ==========================================
         * РАЗДЕЛИТЕЛЬ
         * ==========================================
         */

        if (!percent.isEmpty() && !best.isEmpty()) {
            lines.add(Component.literal("§8————————————"));
        }

        /*
         * ==========================================
         * ЭФФЕКТЫ ЕДЫ
         * ==========================================
         */

        if (!best.isEmpty()) {

            for (MobEffectInstance inst : best.values()) {

                lines.add(
                        Component.translatable(
                                inst.getEffect()
                                        .value()
                                        .getDescriptionId()
                        ).append(
                                Component.literal(
                                        " "
                                                + EffectTextUtils.toRoman(
                                                inst.getAmplifier() + 1
                                        )
                                                + " §7("
                                                + EffectTextUtils.formatTicks(
                                                inst.getDuration()
                                        )
                                                + ")§r"
                                )
                        )
                );
            }
        }

        gui.renderTooltip(
                mc.font,
                lines,
                Optional.empty(),
                mouseX,
                mouseY
        );
    }

}
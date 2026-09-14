// TrapEffectIconRenderer.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.DurationBarTooltip;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionTrackingClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Значок "источник — ловушка/раздатчик" (не игрок и не моб), полоска и тултип. */
public final class TrapEffectIconRenderer {

    // Ячейка 96..112 x 0..16, контент 10x10 с паддингом (3,4) — как у Food/Mob.
    private static final int ICON_U = 96 + 3;
    private static final int ICON_V = 0  + 4;
    private static final int ICON_REGION = 10;

    private TrapEffectIconRenderer() {}

    public static void renderIcon(GuiGraphics gui, int x, int y) {
        StatusIconSheet.blit(gui, x, y, ICON_U, ICON_V, ICON_REGION);
    }

    public static void renderBar(GuiGraphics gui, int x, int y, List<MobEffectInstance> effects) {
        MobEffectInstance longest = longestOf(effects);
        float fraction = longest != null
                ? PotionTrackingClient.getRemainingFraction(longest.getEffect().value(), longest.getDuration())
                : 1f;
        DurationBarRenderer.render(gui, x, y, EffectIconLayout.ICON_SIZE, fraction);
    }

    public static void renderTooltip(GuiGraphics gui, Minecraft mc,
                                     List<MobEffectInstance> effects, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.translatable("damagecore.potion_tab.source_trap")
                .copy().withStyle(s -> s.withColor(0xFFFF55)));

        for (MobEffectInstance inst : effects) {
            lines.add(Component.translatable(inst.getEffect().value().getDescriptionId())
                    .append(Component.literal(" " + EffectTextUtils.toRoman(inst.getAmplifier() + 1))));
        }

        MobEffectInstance longest = longestOf(effects);
        float progress = longest != null
                ? PotionTrackingClient.getRemainingFraction(longest.getEffect().value(), longest.getDuration())
                : 1f;
        Component effectName = longest != null
                ? Component.translatable(longest.getEffect().value().getDescriptionId())
                : Component.empty();
        int amplifier = longest != null ? longest.getAmplifier() + 1 : 1;

        gui.renderTooltip(mc.font, lines,
                Optional.of(new DurationBarTooltip(progress, effectName, amplifier)),
                mouseX, mouseY);
    }

    private static MobEffectInstance longestOf(List<MobEffectInstance> effects) {
        return effects.stream().max(Comparator.comparingInt(MobEffectInstance::getDuration)).orElse(null);
    }
}
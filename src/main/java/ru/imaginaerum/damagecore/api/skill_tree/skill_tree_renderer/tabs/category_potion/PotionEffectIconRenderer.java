// PotionEffectIconRenderer.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.DurationBarTooltip;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionEffectEntry;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionTrackingClient;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Значок "источник — зелье игрока" (общая иконка категории), полоска и тултип с эффектами зелья. */
public final class PotionEffectIconRenderer {

    private static final int ICON_U = 83;
    private static final int ICON_V = 4;
    private static final int ICON_REGION = 10;

    private PotionEffectIconRenderer() {}

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

    public static void renderTooltip(GuiGraphics gui, Minecraft mc, ItemStack stack,
                                     PotionEffectEntry entry, List<MobEffectInstance> effects,
                                     int mouseX, int mouseY) {
        List<Component> lines = List.of(stack.getHoverName()
                .copy().withStyle(s -> s.withColor(0xFFFF55)));

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
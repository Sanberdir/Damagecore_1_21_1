// UnknownEffectIconRenderer.java
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.category_potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.DurationBarTooltip;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.PotionTrackingClient;

import java.util.List;
import java.util.Optional;

/** Значок для эффекта без записи в трекере — рисуется ванильным спрайтом эффекта, не вырезкой из атласа. */
public final class UnknownEffectIconRenderer {

    private UnknownEffectIconRenderer() {}

    public static void renderIcon(GuiGraphics gui, Minecraft mc, int x, int y, MobEffectInstance inst) {
        var sprite = mc.getMobEffectTextures().get(inst.getEffect());
        if (sprite != null) {
            gui.blit(x, y, 0, EffectIconLayout.ICON_SIZE, EffectIconLayout.ICON_SIZE, sprite);
        }
    }

    public static void renderBar(GuiGraphics gui, int x, int y, MobEffectInstance inst) {
        float fraction = PotionTrackingClient.getRemainingFraction(inst.getEffect().value(), inst.getDuration());
        DurationBarRenderer.render(gui, x, y, EffectIconLayout.ICON_SIZE, fraction);
    }

    public static void renderTooltip(GuiGraphics gui, Minecraft mc, MobEffectInstance inst, int mouseX, int mouseY) {
        List<Component> lines = List.of(
                Component.translatable(inst.getEffect().value().getDescriptionId())
                        .withStyle(s -> s.withColor(0xFFFF55)),
                Component.literal(EffectTextUtils.toRoman(inst.getAmplifier() + 1))
        );

        float progress = PotionTrackingClient.getRemainingFraction(inst.getEffect().value(), inst.getDuration());
        Component effectName = Component.translatable(inst.getEffect().value().getDescriptionId());
        int amplifier = inst.getAmplifier() + 1;

        gui.renderTooltip(mc.font, lines,
                Optional.of(new DurationBarTooltip(progress, effectName, amplifier)),
                mouseX, mouseY);
    }
}
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record DurationBarTooltip(float progress, Component name, int amplifier) implements TooltipComponent {}
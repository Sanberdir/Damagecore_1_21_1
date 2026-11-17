package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import ru.imaginaerum.damagecore.Init.enchantments.DensityConfig;
import ru.imaginaerum.damagecore.Init.enchantments.LifeDrainConfig;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.Init.enchantments.WindSquallConfig;
import ru.imaginaerum.damagecore.damage_enchant.*;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EnchantmentTooltips {

    private static final ChatFormatting COLOR = ChatFormatting.DARK_GREEN;

    private EnchantmentTooltips() {}

    private static Component line(Component content) {
        return Component.literal(" ").append(content).withStyle(COLOR);
    }

    private static Component damage(DamageType type, float value) {
        return line(Component.translatable(
                "damagecore.damage." + type.getDamageName(), String.format("%.1f", value)));
    }

    private static String pct(float fraction) {
        return String.format("%.0f", Math.min(1.0F, fraction) * 100);
    }

    public static List<Component> describe(ResourceKey<Enchantment> key, int level, ItemStack stack) {
        List<Component> out = new ArrayList<>();
        Map<DamageType, Double> map = stack.getItem() instanceof IDamageCoreWeapon w
                ? w.damagecore$getDamageMap() : Map.of();

        if (key.equals(Enchantments.FIRE_ASPECT)) {
            out.add(line(Component.translatable("damagecore.fire_aspect.chance",
                    pct(FireAspectConfig.IGNITE_CHANCE_PER_LEVEL * level))));

        } else if (key.equals(Enchantments.IMPALING)) {
            out.add(line(Component.translatable("damagecore.impaling.damage",
                    String.format("%.1f", ImpalingConfig.PIERCING_PER_LEVEL * level))));
            out.add(line(Component.translatable("damagecore.impaling.chance",
                    pct(ImpalingConfig.SLOW_CHANCE_PER_LEVEL * level))));

        } else if (key.equals(Enchantments.BREACH)) {
            out.add(line(Component.translatable("damagecore.breach.ignore",
                    pct(BreachConfig.ARMOR_IGNORE_PER_LEVEL * level))));

        } else if (key.equals(ModEnchantments.LIFE_DRAIN)) {
            out.add(line(Component.translatable("damagecore.life_drain.heal",
                    pct(LifeDrainConfig.HEAL_PERCENT_PER_LEVEL * level))));

        } else if (key.equals(ModEnchantments.WIND_SQUALL)) {
            out.add(line(Component.translatable("damagecore.wind_squall.launch")));
            out.add(line(Component.translatable("damagecore.wind_squall.crit",
                    pct(WindSquallConfig.CRIT_CHANCE_PER_LEVEL * level))));

        } else if (key.equals(ModEnchantments.DENSITY)) {
            out.add(line(Component.translatable("damagecore.density.height",
                    String.format("%.0f", DensityConfig.HEIGHT_MULT_PER_BLOCK * 100))));
            out.add(line(Component.translatable("damagecore.density.strength",
                    String.valueOf(DensityConfig.STRENGTH_REQ_PER_LEVEL * level))));

        } else if (key.equals(ModEnchantments.DARK_INSCRIPTION)) {
            DamageType type = DarkInscriptionConfig.getMaxType(map);
            if (type == null) type = DarkInscriptionConfig.NIGHT_DAMAGE_TYPE;
            String dmg = String.format("%.1f", DarkInscriptionConfig.DAMAGE_PER_LEVEL * level);
            out.add(line(Component.translatable("damagecore.dark_inscription.damage", dmg, dmg,
                    Component.translatable("damagecore.type." + type.getDamageName()))));
            out.add(line(Component.translatable("damagecore.dark_inscription.crit_chance",
                    String.format("%.0f", DarkInscriptionConfig.NIGHT_CRIT_CHANCE * 100))));
            out.add(line(Component.translatable("damagecore.dark_inscription.crit_mult",
                    String.format("%.1f", DarkInscriptionConfig.CRIT_MULT_PER_LEVEL * level))));
        }
        // Небесная кара, Бич, Острота, Громовержец: урон уже в характеристиках,
        // под названием остаётся только стихия.

        return out;
    }
}
package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.skill_tree.protection_helpers.*;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

import java.util.*;

public final class ArmorTabRenderer {

    private ArmorTabRenderer() {}

    public static void render(GuiGraphics gui,int areaX,int areaY,Minecraft mc,int mouseX,int mouseY,
                              ItemStack previewStack) {

        final int leftPadding = 6;
        final int topPadding  = 4;
        final int lineHeight  = mc.font.lineHeight + 2;

        int textX = areaX + leftPadding;
        int y     = areaY + topPadding;

        Player player = mc.player;
        if (player == null) return;

        Map<DamageType, Float> armorFlat = new EnumMap<>(DamageType.class);
        Map<DamageType, Float> armorPercent = new EnumMap<>(DamageType.class);

        boolean hasAnyArmor = false;

        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armorItem)) continue;
            hasAnyArmor = true;

            var resistances =
                    DamageArmorModifier.getDamageResistances(
                            armorItem.getMaterial().value(),
                            armorItem.getType()
                    );

            for (var e : resistances.entrySet()) {
                DamageType type = e.getKey();
                DamageResistance res = e.getValue();

                armorFlat.merge(type, res.getFlat(), Float::sum);
                armorPercent.merge(type, res.getPercent(), Float::sum);
            }
        }

        Map<DamageType, Float> enchantPercent = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float p = getTotalEnchantProtection(player, type);
            if (p > 0) enchantPercent.put(type, p);
        }

        Map<DamageType, DamageResistance> totals = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float flat = armorFlat.getOrDefault(type, 0f);
            float percent = Math.min(1f,
                    armorPercent.getOrDefault(type, 0f)
                            + enchantPercent.getOrDefault(type, 0f));

            if (flat > 0 || percent > 0) {
                totals.put(type, new DamageResistance(flat, percent));
            }
        }

        // ==== Превью: наведение на предмет брони в инвентаре ====
        boolean previewing = previewStack != null && !previewStack.isEmpty()
                && previewStack.getItem() instanceof ArmorItem;
        Map<DamageType, DamageResistance> previewTotals =
                previewing ? computePreviewTotals(player, previewStack) : Collections.emptyMap();

        DamageType hovered = null;

        boolean showList = (hasAnyArmor && !totals.isEmpty()) || (previewing && !previewTotals.isEmpty());

        if (showList) {
            gui.drawString(mc.font,
                    Component.translatable("damagecore.armor_tab.header"),
                    textX, y, 0xFFFFFF, true);

            y += lineHeight + 2;

            for (DamageType type : DamageType.values()) {
                boolean inCurrent = totals.containsKey(type);
                boolean inPreview = previewing && previewTotals.containsKey(type);
                if (!inCurrent && !inPreview) continue;

                DamageResistance current = totals.getOrDefault(type, new DamageResistance(0f, 0f));

                Component line;
                int lineColor;

                if (previewing) {
                    DamageResistance previewRes = previewTotals.getOrDefault(type, new DamageResistance(0f, 0f));
                    String value = previewRes.toString();

                    line = Component.translatable(getKey(type))
                            .append(Component.literal(": " + value));

                    int cmp = compareResistance(current, previewRes);
                    if (cmp > 0) {
                        lineColor = ChatFormatting.BLUE.getColor();   // с превью-бронёй лучше
                    } else if (cmp < 0) {
                        lineColor = ChatFormatting.RED.getColor();    // с превью-бронёй хуже
                    } else {
                        boolean hasEnchantBonus = enchantPercent.getOrDefault(type, 0f) > 0f;
                        lineColor = hasEnchantBonus ? ChatFormatting.YELLOW.getColor() : 0xFFFFFF;
                    }
                } else {
                    String value = current.toString();
                    line = Component.translatable(getKey(type))
                            .append(Component.literal(": " + value));

                    boolean hasEnchantBonus = enchantPercent.getOrDefault(type, 0f) > 0f;
                    lineColor = hasEnchantBonus ? ChatFormatting.YELLOW.getColor() : 0xFFFFFF;
                }

                gui.drawString(mc.font, line, textX, y, lineColor, true);

                int w = mc.font.width(line);

                if (mouseX >= textX && mouseX <= textX + w
                        && mouseY >= y && mouseY < y + lineHeight) {
                    hovered = type;
                }

                y += lineHeight;
            }
        }

        if (hovered != null && !previewing) {
            float af = armorFlat.getOrDefault(hovered, 0f);
            float ap = armorPercent.getOrDefault(hovered, 0f);
            float ep = enchantPercent.getOrDefault(hovered, 0f);

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(getKey(hovered)));

            // Броня и чары — раздельными подписанными строками, без цветового выделения
            if (af > 0 || ap > 0) {
                tooltip.add(Component.translatable(
                        "damagecore.tooltip.armor",
                        format(af, ap)
                ));
            }

            if (ep > 0) {
                tooltip.add(Component.translatable(
                        "damagecore.tooltip.enchant",
                        pct(ep)
                ));
            }

            gui.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY);
        }

        // =========================
        // ✔ ВОЗВРАТ РАБОЧЕГО ИММУНИТЕТА
        // =========================

        int lf = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.LIVE_FORCE))
                .orElse(0);

        int en = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.ENDURANCE))
                .orElse(0);

        boolean hasImmunity = lf > 0 || en > 0;

        if (hasImmunity) {
            y += 6;

            gui.drawString(mc.font,
                    Component.translatable("damagecore.immunity_tab.header"),
                    textX, y, 0xFFFFFF, true);

            y += lineHeight + 2;

            if (lf > 0) {
                for (DamageType type : new DamageType[]{
                        DamageType.BLEEDING,
                        DamageType.FIRE,
                        DamageType.COLD,
                        DamageType.POISON
                }) {
                    Component line = Component.translatable(getKey(type))
                            .append(Component.literal(": " + lf));

                    gui.drawString(mc.font, line, textX, y, 0xEEEEEE, true);
                    y += lineHeight;
                }
            }

            if (en > 0) {
                Component line = Component.translatable(getKey(DamageType.SUFFOCATION))
                        .append(Component.literal(": " + en));

                gui.drawString(mc.font, line, textX, y, 0xEEEEEE, true);
                y += lineHeight;
            }
        }
    }

    // ==== Пересчёт "как если бы" наведённая броня была надета вместо предмета того же слота ====
    private static Map<DamageType, DamageResistance> computePreviewTotals(
            Player player, ItemStack previewStack) {

        if (!(previewStack.getItem() instanceof ArmorItem previewArmor)) return Collections.emptyMap();

        Map<DamageType, Float> flat = new EnumMap<>(DamageType.class);
        Map<DamageType, Float> percent = new EnumMap<>(DamageType.class);
        Map<DamageType, Float> enchantMax = new EnumMap<>(DamageType.class);

        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armorItem)) continue;
            if (armorItem.getType() == previewArmor.getType()) continue; // этот слот заменяется превью-предметом

            var resistances = DamageArmorModifier.getDamageResistances(
                    armorItem.getMaterial().value(), armorItem.getType());
            for (var e : resistances.entrySet()) {
                flat.merge(e.getKey(), e.getValue().getFlat(), Float::sum);
                percent.merge(e.getKey(), e.getValue().getPercent(), Float::sum);
            }

            // чары ОСТАЮЩИХСЯ надетых предметов — по стаку, максимум как в ваниле
            for (DamageType type : DamageType.values()) {
                float p = getTotalEnchantProtectionForStack(stack, player, type);
                if (p > 0) enchantMax.merge(type, p, Math::max);
            }
        }

        // базовая броня превью-предмета
        var previewResistances = DamageArmorModifier.getDamageResistances(
                previewArmor.getMaterial().value(), previewArmor.getType());
        for (var e : previewResistances.entrySet()) {
            flat.merge(e.getKey(), e.getValue().getFlat(), Float::sum);
            percent.merge(e.getKey(), e.getValue().getPercent(), Float::sum);
        }

        // чары ИМЕННО превью-предмета
        for (DamageType type : DamageType.values()) {
            float p = getTotalEnchantProtectionForStack(previewStack, player, type);
            if (p > 0) enchantMax.merge(type, p, Math::max);
        }

        Map<DamageType, DamageResistance> result = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float f = flat.getOrDefault(type, 0f);
            float p = Math.min(1f, percent.getOrDefault(type, 0f) + enchantMax.getOrDefault(type, 0f));
            if (f > 0 || p > 0) result.put(type, new DamageResistance(f, p));
        }
        return result;
    }

    // ==== Сравнение: сначала flat, при равенстве — percent ====
    private static int compareResistance(DamageResistance current, DamageResistance preview) {
        int cmp = Float.compare(preview.getFlat(), current.getFlat());
        if (cmp != 0) return cmp;
        return Float.compare(preview.getPercent(), current.getPercent());
    }

    // ИСПРАВЛЕНО: format() теперь использует округлённые значения через formatFlat()/pct()
    private static String format(float flat, float percent) {
        if (flat > 0 && percent > 0) return formatFlat(flat) + " + " + pct(percent);
        if (flat > 0) return formatFlat(flat);
        if (percent > 0) return pct(percent);
        return "0";
    }

    // ДОБАВЛЕНО: убирает "хвост" плавающей точки при выводе процентов (0.15f*100 -> "15%", а не "15.000001%")
    private static String pct(float fraction) {
        return Math.round(fraction * 100) + "%";
    }

    // ДОБАВЛЕНО: то же самое для плоского значения (3.0f -> "3", 3.5f -> "3.5")
    private static String formatFlat(float flat) {
        if (flat == Math.floor(flat) && !Float.isInfinite(flat)) {
            return String.valueOf((int) flat);
        }
        return String.format(Locale.ROOT, "%.1f", flat);
    }
    // Текущая (реально надетая) броня: чары считаются как максимум по всем надетым предметам,
// т.к. один и тот же тип защиты может быть на нескольких слотах (напр. Protection на груди и штанах).
    private static float getTotalEnchantProtection(Player player, DamageType type) {
        float max = 0f;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) continue;
            max = Math.max(max, getTotalEnchantProtectionForStack(stack, player, type));
        }
        return max;
    }
    private static float getTotalEnchantProtectionForStack(ItemStack stack, Player player, DamageType type) {
        float sum = 0f;
        sum += ProtectionHelper.getEnchantProtectionPercentForStack(stack, player, type);

        switch (type) {
            case FIRE -> sum += FireProtectionHelper.getEnchantProtectionPercentForStack(stack, player, type);
            case PIERCING -> sum += ProjectileProtectionHelper.getEnchantProtectionPercentForStack(stack, player, type);
            case BLUDGEONING -> {
                sum += ExplosionProtectionHelper.getEnchantProtectionPercentForStack(stack, player, type);
                sum += FallProtectionHelper.getEnchantProtectionPercentForStack(stack, player, type);
            }
        }
        return sum;
    }

    private static String getKey(DamageType type) {
        return switch (type) {
            case PIERCING -> "damagecore.damage_type.piercing";
            case SLASHING -> "damagecore.damage_type.slashing";
            case FIRE -> "damagecore.damage_type.fire";
            case COLD -> "damagecore.damage_type.cold";
            case SUFFOCATION -> "damagecore.damage_type.suffocation";
            case BLEEDING -> "damagecore.damage_type.bleeding";
            case LUMINOUS_RADIANT -> "damagecore.damage_type.luminous_radiant";
            case NECROTIC -> "damagecore.damage_type.necrotic";
            case LIGHTNING -> "damagecore.damage_type.lightning";
            case POISON -> "damagecore.damage_type.poison";
            case SOUNDER -> "damagecore.damage_type.sounder";
            case PSY -> "damagecore.damage_type.psy";
            case BLUDGEONING -> "damagecore.damage_type.bludgeoning";
        };
    }
}
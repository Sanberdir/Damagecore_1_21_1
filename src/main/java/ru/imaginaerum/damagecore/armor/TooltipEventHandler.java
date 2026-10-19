package ru.imaginaerum.damagecore.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class TooltipEventHandler {
    private static final java.util.regex.Pattern BRACKET_TAIL =
            java.util.regex.Pattern.compile(".*\\[[+-]?\\d+([.,]\\d+)?]\\s*$");
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof IDamageCoreWeapon) {
            stripAttributeLines(tooltip);
            stripLeftoverAttributeLines(tooltip);
        }
        // 2. Дальше только броня
        if (!(stack.getItem() instanceof ArmorItem armorItem)) return;

        Map<DamageType, DamageResistance> resistances = DamageArmorModifier.getDamageResistances(
                armorItem.getMaterial().value(),
                armorItem.getType()
        );

        Map<DamageType, Float> enchantPercents = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float p = getItemEnchantProtection(stack, type);
            if (p > 0) enchantPercents.put(type, p);
        }

        Map<DamageType, DamageResistance> combined = new EnumMap<>(DamageType.class);
        Set<DamageType> allTypes = new HashSet<>(resistances.keySet());
        allTypes.addAll(enchantPercents.keySet());

        for (DamageType type : allTypes) {
            DamageResistance base = resistances.getOrDefault(type, new DamageResistance(0, 0));
            float extraPercent = enchantPercents.getOrDefault(type, 0f);
            float totalPercent = Math.min(1f, base.getPercent() + extraPercent);
            combined.put(type, new DamageResistance(base.getFlat(), totalPercent));
        }

        if (!combined.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Защита от типов урона:").withStyle(ChatFormatting.GRAY));

            for (Map.Entry<DamageType, DamageResistance> entry : combined.entrySet()) {
                DamageResistance res = entry.getValue();
                if (res.getFlat() > 0 || res.getPercent() > 0) {
                    String damageName = getTranslatedDamageName(entry.getKey());
                    boolean hasEnchant = enchantPercents.containsKey(entry.getKey());
                    ChatFormatting color = hasEnchant ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
                    tooltip.add(Component.literal("  " + damageName + ": " + res).withStyle(color));
                }
            }
        }
        // старый removeIf по словам "armor/броня" удалён
    }

    // ===== Удаление ванильных строк атрибутов =====
    private static List<String> attributeNames() {
        List<Holder<Attribute>> attrs = List.of(
                Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED,
                Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
                Attributes.ATTACK_KNOCKBACK, Attributes.KNOCKBACK_RESISTANCE);
        return attrs.stream()
                .map(a -> Component.translatable(a.value().getDescriptionId()).getString())
                .toList();
    }
    private static void stripLeftoverAttributeLines(List<Component> tooltip) {
        List<String> names = attributeNames();
        // индекс 0 - название предмета, его не трогаем
        for (int i = tooltip.size() - 1; i >= 1; i--) {
            Component c = tooltip.get(i);
            String key = translationKey(c);

            if (key.startsWith("damagecore.")) continue; // свои строки не трогаем

            String text = c.getString();
            boolean looksLikeAttribute =
                    key.startsWith("attribute.modifier.")
                            || (BRACKET_TAIL.matcher(text).matches()
                            && names.stream().anyMatch(text::contains));

            if (looksLikeAttribute) tooltip.remove(i);
        }
    }
    /** Возвращает ключ перевода компонента (или его первого вложенного), либо "". */
    private static String translationKey(Component c) {
        if (c.getContents() instanceof TranslatableContents tc) return tc.getKey();
        for (Component s : c.getSiblings()) {
            if (s.getContents() instanceof TranslatableContents tc) return tc.getKey();
        }
        return "";
    }

    /**
     * Удаляет заголовки слотов ("item.modifiers.mainhand", "item.modifiers.head" и т.д.)
     * вместе с пустой строкой перед ними и всеми строками "attribute.modifier.*" под ними.
     */
    private static void stripAttributeLines(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (!translationKey(tooltip.get(i)).startsWith("item.modifiers.")) continue;

            int start = i;
            if (start > 0 && tooltip.get(start - 1).getString().isEmpty()) start--;

            int end = i + 1;
            while (end < tooltip.size()
                    && translationKey(tooltip.get(end)).startsWith("attribute.modifier.")) {
                end++;
            }

            tooltip.subList(start, end).clear();
            i = start - 1; // продолжаем с места удаления

        }
    }

    // ===== Остальное без изменений =====

    private static float getItemEnchantProtection(ItemStack stack, DamageType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return 0f;
        RegistryAccess registryAccess = mc.getConnection().registryAccess();

        float sum = 0f;

        if (type == DamageType.PIERCING || type == DamageType.SLASHING || type == DamageType.BLUDGEONING) {
            int lvl = getEnchantLevel(stack, registryAccess, Enchantments.PROTECTION);
            if (lvl > 0) sum += lvl * 0.04f;
        }

        switch (type) {
            case FIRE -> {
                int lvl = getEnchantLevel(stack, registryAccess, Enchantments.FIRE_PROTECTION);
                if (lvl > 0) sum += lvl * 0.08f;
            }
            case PIERCING -> {
                int lvl = getEnchantLevel(stack, registryAccess, Enchantments.PROJECTILE_PROTECTION);
                if (lvl > 0) sum += lvl * 0.08f;
            }
            case BLUDGEONING -> {
                int lvl1 = getEnchantLevel(stack, registryAccess, Enchantments.BLAST_PROTECTION);
                if (lvl1 > 0) sum += lvl1 * 0.08f;
                int lvl2 = getEnchantLevel(stack, registryAccess, Enchantments.FEATHER_FALLING);
                if (lvl2 > 0) sum += lvl2 * 0.08f;
            }
            default -> {}
        }

        return sum;
    }

    private static int getEnchantLevel(ItemStack stack, RegistryAccess registryAccess, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return stack.getEnchantmentLevel(holder);
    }

    private static String getTranslatedDamageName(DamageType damageType) {
        return switch (damageType) {
            case PIERCING -> "Колющий";
            case SLASHING -> "Режущий";
            case FIRE -> "Огненный";
            case THUNDER -> "Погодный";
            case THERMAL -> "Термальный";
            case WITHERING -> "Иссушающий";
            case EXPLOSIVE -> "Взрывной";
            case ACID -> "Ядовитый";
            case ETHEREAL -> "Эфирный";
            case COLD -> "Холодный";
            case BLEEDING -> "Кровотечение";
            case TEMPERATURE -> "Температурный";
            case SUFFOCATION -> "Удушье";
            case LUMINOUS_RADIANT -> "Лучистый";
            case NECROTIC -> "Некротический";
            case LIGHTNING -> "Молния";
            case POISON -> "Ядовитый";
            case SOUNDER -> "Звуковой";
            case PSY -> "Психический";
            case BLUDGEONING -> "Дробящий";
        };
    }
}
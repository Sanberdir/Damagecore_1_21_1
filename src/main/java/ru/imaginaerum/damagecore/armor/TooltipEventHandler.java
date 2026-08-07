package ru.imaginaerum.damagecore.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class TooltipEventHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof ArmorItem armorItem)) return;

        Map<DamageType, DamageResistance> resistances = DamageArmorModifier.getDamageResistances(
                armorItem.getMaterial().value(),
                armorItem.getType()
        );

        // Собираем процентные бонусы от чар этого конкретного предмета
        Map<DamageType, Float> enchantPercents = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float p = getItemEnchantProtection(stack, type);
            if (p > 0) enchantPercents.put(type, p);
        }

        // Объединяем материал + чары
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
            event.getToolTip().add(Component.literal(""));
            event.getToolTip().add(Component.literal("Защита от типов урона:")
                    .withStyle(ChatFormatting.GRAY));

            for (Map.Entry<DamageType, DamageResistance> entry : combined.entrySet()) {
                DamageResistance res = entry.getValue();
                if (res.getFlat() > 0 || res.getPercent() > 0) {
                    String damageName = getTranslatedDamageName(entry.getKey());
                    boolean hasEnchant = enchantPercents.containsKey(entry.getKey());
                    ChatFormatting color = hasEnchant ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
                    Component line = Component.literal("  " + damageName + ": " + res)
                            .withStyle(color);
                    event.getToolTip().add(line);
                }
            }
        }

        event.getToolTip().removeIf(component -> {
            String text = component.getString().toLowerCase();
            return text.contains("armor") || text.contains("броня") ||
                    text.contains("toughness") || text.contains("прочность") ||
                    text.matches(".*\\+\\s*\\d+.*(armor|броня).*");
        });
    }

    private static float getItemEnchantProtection(ItemStack stack, DamageType type) {
        // В 1.21.1 зачарования — датадривен реестр, поэтому уровень читаем
        // через RegistryAccess + Holder<Enchantment>, а не через старый
        // Map<Enchantment, Integer> из EnchantmentHelper.getEnchantments(stack).
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return 0f;
        RegistryAccess registryAccess = mc.getConnection().registryAccess();

        float sum = 0f;

        // Protection — физические типы
        if (type == DamageType.PIERCING || type == DamageType.SLASHING || type == DamageType.BLUDGEONING) {
            int lvl = getEnchantLevel(stack, registryAccess, Enchantments.PROTECTION);
            if (lvl > 0) sum += lvl * 0.04f; // ваша формула из ProtectionHelper
        }

        switch (type) {
            case FIRE -> {
                int lvl = getEnchantLevel(stack, registryAccess, Enchantments.FIRE_PROTECTION);
                if (lvl > 0) sum += lvl * 0.08f; // ваша формула из FireProtectionHelper
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
            case COLD -> "Холодный";
            case BLEEDING -> "Кровотечение";
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
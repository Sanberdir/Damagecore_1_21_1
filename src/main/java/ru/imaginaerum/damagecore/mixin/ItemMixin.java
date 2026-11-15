package ru.imaginaerum.damagecore.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.enchantments.DensityConfig;
import ru.imaginaerum.damagecore.Init.enchantments.LifeDrainConfig;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.Init.enchantments.WindSquallConfig;
import ru.imaginaerum.damagecore.damage_enchant.*;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;
import ru.imaginaerum.damagecore.library_enchantments.Element;
import ru.imaginaerum.damagecore.library_enchantments.EnchantmentElements;

import java.util.List;
import java.util.Map;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "appendHoverText", at = @At("RETURN"))
    private void damagecore$addDamageTypeTooltip(ItemStack stack, Item.TooltipContext context,
                                                 List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {

        if (!(stack.getItem() instanceof IDamageCoreWeapon weapon)) return;

        final ChatFormatting vanillaColor = ChatFormatting.DARK_GREEN;

        Map<DamageType, Double> damageMap = weapon.damagecore$getDamageMap();

        // Уровни зачарований
        int smiteLevel = 0;
        int baneLevel = 0;
        int sharpLevel = 0;
        int fireLevel = 0;
        int lifeDrainLevel = 0;
        int impalingLevel = 0;
        int darkLevel = 0;
        int windLevel = 0;
        int densityLevel = 0;
        int breachLevel = 0;
        if (context.registries() != null) {
            var enchantments = context.registries().lookupOrThrow(Registries.ENCHANTMENT);
            smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    enchantments.getOrThrow(Enchantments.SMITE), stack);
            baneLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    enchantments.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), stack);
            sharpLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    enchantments.getOrThrow(Enchantments.SHARPNESS), stack);
            fireLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    enchantments.getOrThrow(Enchantments.FIRE_ASPECT), stack);
            impalingLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    enchantments.getOrThrow(Enchantments.IMPALING), stack);
            breachLevel = enchantments.get(Enchantments.BREACH)
                    .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack))
                    .orElse(0);
            windLevel = enchantments.get(ModEnchantments.WIND_SQUALL)
                    .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
            darkLevel = enchantments.get(ModEnchantments.DARK_INSCRIPTION)
                    .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
            lifeDrainLevel = enchantments.get(ModEnchantments.LIFE_DRAIN)
                    .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
            densityLevel = enchantments.get(ModEnchantments.DENSITY)
                    .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
        }

        // Поправка Начертания тьмы (зависит от времени суток), считается ДО цикла
        DamageType darkMaxType = null;
        double darkDelta = 0.0;
        if (darkLevel > 0) {
            darkMaxType = DarkInscriptionConfig.getMaxType(damageMap);
            Level world = context.level();
            if (darkMaxType != null && world != null) {
                double v = DarkInscriptionConfig.DAMAGE_PER_LEVEL * darkLevel;
                if (DarkInscriptionConfig.isNight(world)) {
                    darkDelta = v;
                } else if (DarkInscriptionConfig.isDay(world)) {
                    darkDelta = -v;
                }
            }
        }

        boolean headerShown = false;

        // Типы урона самого оружия
        if (!damageMap.isEmpty()) {
            tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
            headerShown = true;

            for (Map.Entry<DamageType, Double> entry : damageMap.entrySet()) {
                double value = entry.getValue();
                if (entry.getKey() == DamageType.SLASHING && value > 0 && sharpLevel > 0) {
                    value += SharpnessConfig.SLASHING_PER_LEVEL * sharpLevel;
                }
                if (entry.getKey() == DamageType.FIRE && fireLevel > 0) {
                    value += FireAspectConfig.FIRE_PER_LEVEL * fireLevel;
                }
                if (entry.getKey() == DamageType.BLUDGEONING && densityLevel > 0) {
                    value += DensityConfig.bludgeoningBonus(densityLevel, true);
                }
                // Начертание тьмы: ночью плюс, днём минус, не ниже нуля
                if (entry.getKey() == darkMaxType) {
                    value = Math.max(0.0, value + darkDelta);
                }
                tooltip.add(Component.literal(" ").append(
                        Component.translatable(
                                "damagecore.damage." + entry.getKey().getDamageName(),
                                String.format("%.1f", value)
                        )
                ).withStyle(vanillaColor));
            }
        }
// Пробитие
        if (breachLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.breach.ignore",
                            String.format("%.0f", Math.min(1.0F, BreachConfig.ARMOR_IGNORE_PER_LEVEL * breachLevel) * 100))
            ).withStyle(vanillaColor));
        }
        // Начертание тьмы (описание)
        if (darkLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            DamageType shownType = darkMaxType != null ? darkMaxType : DarkInscriptionConfig.NIGHT_DAMAGE_TYPE;
            Component typeName = Component.translatable("damagecore.type." + shownType.getDamageName());
            String dmg = String.format("%.1f", DarkInscriptionConfig.DAMAGE_PER_LEVEL * darkLevel);

            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.dark_inscription.damage", dmg, dmg, typeName)
            ).withStyle(vanillaColor));
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.dark_inscription.crit_chance",
                            String.format("%.0f", DarkInscriptionConfig.NIGHT_CRIT_CHANCE * 100))
            ).withStyle(vanillaColor));
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.dark_inscription.crit_mult",
                            String.format("%.1f", DarkInscriptionConfig.CRIT_MULT_PER_LEVEL * darkLevel))
            ).withStyle(vanillaColor));
        }

        // Плотность
        if (densityLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            if (!damageMap.containsKey(DamageType.BLUDGEONING)) {
                tooltip.add(Component.literal(" ").append(
                        Component.translatable("damagecore.damage.bludgeoning",
                                String.format("%.1f", DensityConfig.bludgeoningBonus(densityLevel, false)))
                ).withStyle(vanillaColor));
            }
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.density.height",
                            String.format("%.0f", DensityConfig.HEIGHT_MULT_PER_BLOCK * 100))
            ).withStyle(vanillaColor));
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.density.strength",
                            String.valueOf(DensityConfig.STRENGTH_REQ_PER_LEVEL * densityLevel))
            ).withStyle(vanillaColor));
        }

        // Ветряной шквал
        if (windLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.wind_squall.launch")
            ).withStyle(vanillaColor));
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.wind_squall.crit",
                            String.format("%.0f", Math.min(1.0F, WindSquallConfig.CRIT_CHANCE_PER_LEVEL * windLevel) * 100))
            ).withStyle(vanillaColor));
        }

        // Пронзатель
        if (impalingLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.impaling.damage",
                            String.format("%.1f", ImpalingConfig.PIERCING_PER_LEVEL * impalingLevel))
            ).withStyle(vanillaColor));
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.impaling.chance",
                            String.format("%.0f", Math.min(1.0F, ImpalingConfig.SLOW_CHANCE_PER_LEVEL * impalingLevel) * 100))
            ).withStyle(vanillaColor));
        }

        // Вытягивание жизни
        if (lifeDrainLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.life_drain.heal",
                            String.format("%.0f", LifeDrainConfig.HEAL_PERCENT_PER_LEVEL * lifeDrainLevel * 100))
            ).withStyle(vanillaColor));
        }

        // Лучистый урон от Небесной кары
        if (smiteLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            float radiant = SmiteConfig.RADIANT_PER_LEVEL * smiteLevel;
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.damage." + DamageType.LUMINOUS_RADIANT.getDamageName(),
                            String.format("%.1f", radiant))
            ).withStyle(vanillaColor));
        }

        // Заговор огня
        if (fireLevel > 0) {
            if (!damageMap.containsKey(DamageType.FIRE)) {
                if (!headerShown) {
                    tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                    headerShown = true;
                }
                tooltip.add(Component.literal(" ").append(
                        Component.translatable("damagecore.damage." + DamageType.FIRE.getDamageName(),
                                String.format("%.1f", FireAspectConfig.FIRE_PER_LEVEL * fireLevel))
                ).withStyle(vanillaColor));
            }
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.fire_aspect.chance",
                            String.format("%.0f", Math.min(1.0F, FireAspectConfig.IGNITE_CHANCE_PER_LEVEL * fireLevel) * 100))
            ).withStyle(vanillaColor));
        }

        // Урон громом от Бича членистоногих
        if (baneLevel > 0) {
            if (!headerShown) {
                tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));
                headerShown = true;
            }
            float thunder = BaneConfig.THUNDER_PER_LEVEL * baneLevel;
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.damage." + DamageType.THUNDER.getDamageName(),
                            String.format("%.1f", thunder))
            ).withStyle(vanillaColor));
        }

        // Стихии зачарований
        for (Element element : EnchantmentElements.getElements(stack)) {
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.element", element.getDisplayName())
            ).withStyle(vanillaColor));
        }

        // Скорость атаки (с учётом Плотности)
        WeaponDamageData data = Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(stack.getItem());

        if (data != null && data.hasAttackSpeed()) {
            double speed = data.getAttackSpeed() + DensityConfig.ATTACK_SPEED_PER_LEVEL * densityLevel;
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.attack_speed",
                            String.format("%.1f", Math.max(0.1, speed)))
            ).withStyle(vanillaColor));
        }
    }
}
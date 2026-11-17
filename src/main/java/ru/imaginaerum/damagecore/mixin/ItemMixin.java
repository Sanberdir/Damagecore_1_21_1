package ru.imaginaerum.damagecore.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.enchantments.DensityConfig;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.damage_enchant.DarkInscriptionConfig;
import ru.imaginaerum.damagecore.damage_enchant.EnchantDamage;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;

import java.util.List;
import java.util.Map;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "appendHoverText", at = @At("RETURN"))
    private void damagecore$addDamageTypeTooltip(ItemStack stack, Item.TooltipContext context,
                                                 List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {

        if (!(stack.getItem() instanceof IDamageCoreWeapon weapon)) return;

        final ChatFormatting vanillaColor = ChatFormatting.DARK_GREEN;
        Map<DamageType, Double> baseMap = weapon.damagecore$getDamageMap();

        // Урон оружия + урон от чаров
        Map<DamageType, Double> damageMap = EnchantDamage.merged(stack, baseMap);

        // Начертание тьмы: поправка к наибольшему типу по времени суток
        int darkLevel = EnchantDamage.level(stack, ModEnchantments.DARK_INSCRIPTION);
        DamageType darkMaxType = null;
        double darkDelta = 0.0;
        if (darkLevel > 0) {
            darkMaxType = DarkInscriptionConfig.getMaxType(baseMap);
            Level world = context.level();
            if (darkMaxType != null && world != null) {
                double v = DarkInscriptionConfig.DAMAGE_PER_LEVEL * darkLevel;
                if (DarkInscriptionConfig.isNight(world)) darkDelta = v;
                else if (DarkInscriptionConfig.isDay(world)) darkDelta = -v;
            }
        }

        int densityLevel = EnchantDamage.level(stack, ModEnchantments.DENSITY);
        WeaponDamageData data = Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(stack.getItem());
        boolean hasSpeed = data != null && data.hasAttackSpeed();

        if (damageMap.isEmpty() && !hasSpeed) return;

        tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));

        for (Map.Entry<DamageType, Double> entry : damageMap.entrySet()) {
            double value = entry.getValue();
            if (entry.getKey() == darkMaxType) {
                value = Math.max(0.0, value + darkDelta);
            }
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.damage." + entry.getKey().getDamageName(),
                            String.format("%.1f", value))
            ).withStyle(vanillaColor));
        }

        if (hasSpeed) {
            double speed = data.getAttackSpeed() + DensityConfig.ATTACK_SPEED_PER_LEVEL * densityLevel;
            tooltip.add(Component.literal(" ").append(
                    Component.translatable("damagecore.attack_speed",
                            String.format("%.1f", Math.max(0.1, speed)))
            ).withStyle(vanillaColor));
        }
    }
}
package ru.imaginaerum.damagecore.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
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

        // Единый цвет, как у ванильных атрибутов
        final ChatFormatting vanillaColor = ChatFormatting.DARK_GREEN;

        Map<DamageType, Double> damageMap = weapon.damagecore$getDamageMap();

        if (!damageMap.isEmpty()) {
            tooltip.add(Component.translatable("damagecore.characteristics").withStyle(ChatFormatting.GRAY));

            for (Map.Entry<DamageType, Double> entry : damageMap.entrySet()) {
                tooltip.add(Component.literal(" ").append(
                        Component.translatable(
                                "damagecore.damage." + entry.getKey().getDamageName(),
                                String.format("%.1f", entry.getValue())
                        )
                ).withStyle(vanillaColor));
            }
        }

        WeaponDamageData data = Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(stack.getItem());

        if (data != null && data.hasAttackSpeed()) {
            tooltip.add(Component.literal(" ").append(
                    Component.translatable(
                            "damagecore.attack_speed",
                            String.format("%.1f", data.getAttackSpeed())
                    )
            ).withStyle(vanillaColor));
        }
    }
}
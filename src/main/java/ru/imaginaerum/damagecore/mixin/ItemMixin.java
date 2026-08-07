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

        Map<DamageType, Double> damageMap = weapon.damagecore$getDamageMap();

        if (!damageMap.isEmpty()) {
            tooltip.add(Component.translatable("damagecore.possible_damage").withStyle(ChatFormatting.GRAY));

            for (Map.Entry<DamageType, Double> entry : damageMap.entrySet()) {
                DamageType damageType = entry.getKey();

                ChatFormatting color = switch (damageType) {
                    case PIERCING, SLASHING, BLUDGEONING -> ChatFormatting.GREEN;
                    case FIRE, BLEEDING -> ChatFormatting.RED;
                    case COLD -> ChatFormatting.AQUA;
                    case LIGHTNING -> ChatFormatting.YELLOW;
                    case NECROTIC -> ChatFormatting.DARK_PURPLE;
                    case POISON -> ChatFormatting.DARK_GREEN;
                    case LUMINOUS_RADIANT -> ChatFormatting.WHITE;
                    case PSY -> ChatFormatting.LIGHT_PURPLE;
                    case SOUNDER -> ChatFormatting.BLUE;
                    case SUFFOCATION -> ChatFormatting.DARK_GRAY;
                };

                tooltip.add(Component.literal(" ").append(
                        Component.translatable(
                                "damagecore.damage." + damageType.getDamageName(),
                                String.format("%.1f", entry.getValue())
                        ).withStyle(color)
                ));
            }
        }

        Item item = stack.getItem();
        WeaponDamageData data = Damagecore_1_21_1_neo.WEAPON_DAMAGE_MANAGER.getDamageData(item);

        if (data != null && data.hasAttackSpeed()) {
            tooltip.add(Component.translatable(
                    "damagecore.attack_speed",
                    String.format("%.1f", data.getAttackSpeed())
            ).withStyle(ChatFormatting.BLUE));
        }
    }
}
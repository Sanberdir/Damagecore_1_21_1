package ru.imaginaerum.damagecore.mixin.tree_mixins.shooting;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting.ClientHundredArmedData;

@OnlyIn(Dist.CLIENT)
@Mixin(ItemProperties.class)
public class ItemPropertiesMixin {

    private static final ResourceLocation PULL = ResourceLocation.withDefaultNamespace("pull");

    @Inject(
            method = "getProperty",
            at = @At("RETURN"),
            cancellable = true,
            remap = false // Оставляем false, если вы компилируете с официальными маппингами (Mojang), либо смените на true при необходимости
    )
    private static void interceptPull(ItemStack stack, ResourceLocation id,
                                      CallbackInfoReturnable<ItemPropertyFunction> cir) {
        // Проверяем, что запрашивается именно свойство натяжения ("pull")
        if (!PULL.equals(id)) return;

        // В 1.21.1 проверяем предмет через ItemStack
        if (stack == null || !(stack.getItem() instanceof BowItem)) return;

        // Подменяем возвращаемую функцию натяжения
        cir.setReturnValue((s, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (entity.getUseItem() != s) return 0.0F;

            // Динамический расчет скорости натяжения лука на основе перка
            float divisor = ClientHundredArmedData.hasSkill ? 10.0F : 20.0F;
            float pull = (float)(entity.getTicksUsingItem()) / divisor;
            return Math.min(pull, 1.0F);
        });
    }
}

package ru.imaginaerum.damagecore.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import ru.imaginaerum.damagecore.hud.elements.ThirstBarElement;

@EventBusSubscriber(value = Dist.CLIENT)
public class ThirstEventHandler {

    // ПКМ по блоку воды
    private static int drinkTicks = 0;
    private static final int DRINK_DURATION = 25; // 2 секунды

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean rmb = mc.options.keyUse.isDown();
        boolean emptyHands = mc.player.getMainHandItem().isEmpty();
        boolean lookingAtWater = isLookingAtWater(mc);

        if (rmb && emptyHands && lookingAtWater) {
            drinkTicks++;
            if (drinkTicks % 4 == 0) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            if (drinkTicks >= DRINK_DURATION) {
                drinkTicks = 0;
                mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1f, 1f);
            }
        } else {
            drinkTicks = 0;
        }
    }

    private static boolean isLookingAtWater(Minecraft mc) {
        if (mc.player == null || mc.level == null) return false;

        double reach = mc.player.blockInteractionRange();
        var start = mc.player.getEyePosition();
        var look = mc.player.getViewVector(1.0f);
        var end = start.add(look.x * reach, look.y * reach, look.z * reach);

        var hit = mc.level.clip(new ClipContext(
                start, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY,
                mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return false;

        return mc.level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER);
    }

}
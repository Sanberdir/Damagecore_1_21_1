package ru.imaginaerum.damagecore.Init.fishing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.FishingRodItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.fishing.net.FightInputPayload;

public final class ClientEvents {
    private ClientEvents() {}

    /** Game-bus events (client only). */
    @EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            FightClient.tickPositions();
            FightClient.smoothTick();
            if (!FightClient.active) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                FightClient.reset();
                return;
            }
            double[] m = FightClient.consumeMouse();
            PacketDistributor.sendToServer(new FightInputPayload((float) m[0], (float) m[1]));
        }
        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent e) {
            if (!(e.getItemStack().getItem() instanceof FishingRodItem)) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (Math.abs(FightClient.rodPitch) < 0.01f && Math.abs(FightClient.rodRoll) < 0.01f) return;

            boolean right = (e.getHand() == InteractionHand.MAIN_HAND)
                    == (mc.player.getMainArm() == HumanoidArm.RIGHT);
            float side = right ? 1f : -1f;

            PoseStack ps = e.getPoseStack();
            // поворот вокруг точки хвата, чтобы рука не "уезжала"
            ps.translate(side * 0.4, -0.35, -0.6);
            ps.mulPose(Axis.XP.rotationDegrees(-FightClient.rodPitch));
            ps.mulPose(Axis.ZP.rotationDegrees(side * FightClient.rodRoll));
            ps.translate(-side * 0.4, 0.35, 0.6);
        }
        /** Рыба на крючке не должна выходить из воды: при отрисовке прижимаем её под поверхность. */
        @SubscribeEvent
        public static void onRenderFish(RenderLivingEvent.Pre<?, ?> e) {
            var en = e.getEntity();
            double top = FightClient.maxTopY(en);
            if (Double.isNaN(top)) top = FightClient.maxTopYNearHook(en);
            if (Double.isNaN(top)) return;
            double over = (en.getY() + en.getBbHeight()) - top;
            if (over > 0) e.getPoseStack().translate(0.0, -over, 0.0);
        }
        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            FightClient.reset();
        }
    }

}

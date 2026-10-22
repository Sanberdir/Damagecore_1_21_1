package ru.imaginaerum.damagecore.Init.fishing.fight;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.fishing.client.FightClient;
import ru.imaginaerum.damagecore.Init.fishing.net.FightKeysPayload;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public final class FightKeys {
    private static int lastSide;
    private static boolean lastTighten, lastSlack, lastReel;
    private static int resend;

    private FightKeys() {}

    private static boolean raw(Minecraft mc, int glfwKey) {
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), glfwKey);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int side = 0;
        boolean tighten = false, slack = false, reel = false;
        if (FightClient.active && mc.screen == null) {
            boolean left  = mc.options.keyLeft.isDown()  || raw(mc, GLFW.GLFW_KEY_LEFT);
            boolean right = mc.options.keyRight.isDown() || raw(mc, GLFW.GLFW_KEY_RIGHT);
            side    = (right ? 1 : 0) - (left ? 1 : 0);
            tighten = mc.options.keyDown.isDown() || raw(mc, GLFW.GLFW_KEY_DOWN);   // S = натянуть/отклонить назад
            slack   = mc.options.keyUp.isDown()   || raw(mc, GLFW.GLFW_KEY_UP);     // W = ослабить
            reel    = mc.options.keyAttack.isDown();   // ЛКМ = крутить катушку
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
        } else if (lastSide == 0 && !lastTighten && !lastSlack && !lastReel) {
            return;
        }

        boolean changed = side != lastSide || tighten != lastTighten
                || slack != lastSlack || reel != lastReel;
        if (changed || ++resend >= 5) {
            resend = 0;
            lastSide = side;
            lastTighten = tighten;
            lastSlack = slack;
            lastReel = reel;
            PacketDistributor.sendToServer(new FightKeysPayload((byte) side, tighten, slack, reel, false));
        }
    }

    /**
     * Во время боя:
     *  - ЛКМ (attack) - это reel; обрабатывается через keyAttack в onClientTick.
     *  - ПКМ (use)   - отпустить рыбу (abort).
     *  - Все остальные interaction-триггеры просто гасим, чтобы игрок не махал мечом.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered e) {
        if (!FightClient.active) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        if (e.isUseItem()) {
            // ПКМ = отпустить
            PacketDistributor.sendToServer(new FightKeysPayload((byte) 0, false, false, false, true));
        }
        // ЛКМ не посылаем здесь - его обрабатывает onClientTick через keyAttack.
    }

    /** Во время боя WASD не двигает игрока, только сопротивляется рыбе. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!FightClient.active) return;
        var in = event.getInput();
        in.leftImpulse  = 0;
        in.forwardImpulse = 0;
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }
}
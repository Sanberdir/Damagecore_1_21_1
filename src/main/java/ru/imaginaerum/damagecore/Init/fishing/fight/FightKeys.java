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
    private static boolean hadRod;
    private static boolean blockCast;
    private static boolean lastAttack;
    private static boolean attackOk; // ЛКМ отпускали после заброса (защита от «залипшего» зажатия)
    private FightKeys() {}

    private static boolean raw(Minecraft mc, int glfwKey) {
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), glfwKey);
    }
    /** Игрок «привязан» к удочке: идёт бой или поплавок закинут. */
    private static boolean rodLocked(Minecraft mc) {
        return FightClient.active || (mc.player != null && mc.player.fishing != null);
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { hadRod = false; blockCast = false; return; }

        boolean locked = rodLocked(mc);
        // бой/зацеп не может идти без поплавка: если сервер не прислал active=false, сбрасываем сами
        if (FightClient.active && mc.player.fishing == null) {
            FightClient.reset();
            locked = rodLocked(mc);
        }
        if (mc.player.fishing == null && !FightClient.active) {
            attackOk = !mc.options.keyAttack.isDown();
        }
        boolean useDown = mc.options.keyUse.isDown();
        if (hadRod && !locked && useDown) blockCast = true;
        if (!useDown) blockCast = false;
        hadRod = locked;

        int side = 0;
        boolean tighten = false, slack = false, reel = false, attack = false;
        if (FightClient.active && mc.screen == null) {
            boolean left  = mc.options.keyLeft.isDown()  || raw(mc, GLFW.GLFW_KEY_LEFT);
            boolean right = mc.options.keyRight.isDown() || raw(mc, GLFW.GLFW_KEY_RIGHT);
            side    = (right ? 1 : 0) - (left ? 1 : 0);
            tighten = mc.options.keyDown.isDown() || raw(mc, GLFW.GLFW_KEY_DOWN);   // S = сопротивление
            slack   = mc.options.keyUp.isDown()   || raw(mc, GLFW.GLFW_KEY_UP);     // W = ослабить
            reel    = mc.options.keyUse.isDown();                                    // ПКМ = катушка
            attack  = mc.options.keyAttack.isDown();                                 // ЛКМ: тап = рывок, удержание = отпустить
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
        } else if (mc.screen == null && mc.player.fishing != null) {
            reel = mc.options.keyUse.isDown();
            boolean atk = mc.options.keyAttack.isDown();
            if (!atk) attackOk = true;
            attack = atk && attackOk;
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
        }else if (lastSide == 0 && !lastTighten && !lastSlack && !lastReel && !lastAttack) {
            return;
        }

        boolean changed = side != lastSide || tighten != lastTighten
                || slack != lastSlack || reel != lastReel || attack != lastAttack;
        if (changed || ++resend >= 5) {
            resend = 0;
            lastSide = side;
            lastTighten = tighten;
            lastSlack = slack;
            lastReel = reel;
            lastAttack = attack;
            // последний аргумент = «ЛКМ зажат» (поле release в payload)
            PacketDistributor.sendToServer(new FightKeysPayload((byte) side, tighten, slack, reel, attack));
        }
    }

    /**
     * Во время боя:
     *  - ПКМ (use)   - это reel; обрабатывается через keyUse в onClientTick.
     *  - ЛКМ (attack) - отпустить рыбу (abort).
     *  - Все остальные interaction-триггеры просто гасим, чтобы игрок не махал мечом.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered e) {
        if (blockCast && e.isUseItem()) {
            e.setCanceled(true);
            e.setSwingHand(false);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (e.isAttack() && mc.player != null && mc.player.fishing != null) {
            e.setCanceled(true);
            e.setSwingHand(false);
            return;
        }
        // подмотка/подсечка: клик уходит на сервер, но рука не машет

        if (!FightClient.active) return;
        e.setCanceled(true);
        e.setSwingHand(false);
    }

    /** Во время боя WASD не двигает игрока, только сопротивляется рыбе. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!rodLocked(mc)) return;
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
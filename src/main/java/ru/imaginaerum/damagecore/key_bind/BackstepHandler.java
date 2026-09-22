package ru.imaginaerum.damagecore.key_bind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.animation_attack.WeaponAttackAnimationHandler;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class BackstepHandler {

    private static final double HORIZONTAL_SPEED = 0.85;
    private static final double VERTICAL_SPEED = 0.30;
    private static final long COOLDOWN_MS = 800;
    private static final long INPUT_LOCK_MS = 300;

    public static final KeyMapping BACKSTEP_KEY = new KeyMapping(
            "key.damagecore.backstep",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.movement"
    );

    private static long lastUsed = 0;
    private static long lockedUntil = 0;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(BACKSTEP_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        while (BACKSTEP_KEY.consumeClick()) {
            tryBackstep(player);
        }
    }

    private static void tryBackstep(LocalPlayer player) {
        if (Minecraft.getInstance().screen != null) return;
        if (!player.onGround() || player.isInWater() || player.isPassenger()
                || player.getAbilities().flying || player.isFallFlying()) return;

        long now = System.currentTimeMillis();
        if (now - lastUsed < COOLDOWN_MS) return;
        if (WeaponAttackAnimationHandler.isBusy(player)) return;

        lastUsed = now;
        lockedUntil = now + INPUT_LOCK_MS;

        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0, Mth.cos(yawRad));
        Vec3 right   = new Vec3(Mth.cos(yawRad), 0, Mth.sin(yawRad));

        // Читаем зажатые клавиши напрямую, а не input.forwardImpulse —
        // тот уже мог быть обнулён нашим же обработчиком на предыдущем тике.
        var kb = Minecraft.getInstance().options;
        double fwdAxis = 0;
        if (kb.keyUp.isDown())   fwdAxis += 3;
        if (kb.keyDown.isDown()) fwdAxis -= 3;
        double sideAxis = 0;
        if (kb.keyRight.isDown()) sideAxis -= 3;
        if (kb.keyLeft.isDown())  sideAxis += 3;

        Vec3 dir;
        if (fwdAxis == 0 && sideAxis == 0) {
            // Ничего не зажато — уворот по умолчанию: назад
            dir = forward.scale(-1);
        } else {
            dir = forward.scale(fwdAxis).add(right.scale(sideAxis)).normalize();
        }

        Vec3 dash = dir.scale(HORIZONTAL_SPEED);
        player.setDeltaMovement(dash.x, VERTICAL_SPEED, dash.z);
        player.hasImpulse = true;
    }

    public static boolean isInputLocked() {
        return System.currentTimeMillis() < lockedUntil;
    }
}
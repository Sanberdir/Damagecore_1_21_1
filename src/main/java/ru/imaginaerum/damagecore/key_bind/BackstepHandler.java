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

    public static final KeyMapping BACKSTEP_KEY = new KeyMapping(
            "key.damagecore.backstep",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.movement"
    );

    private static long lastUsed = 0;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(BACKSTEP_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // consumeClick срабатывает один раз на одно нажатие
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

        // Не отскакиваем во время замаха/удара
        if (WeaponAttackAnimationHandler.isBusy(player)) return;

        lastUsed = now;

        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        // Вперёд = (-sin, cos), назад = (sin, -cos)
        Vec3 back = new Vec3(Mth.sin(yawRad), 0, -Mth.cos(yawRad)).scale(HORIZONTAL_SPEED);

        player.setDeltaMovement(back.x, VERTICAL_SPEED, back.z);
        player.hasImpulse = true;
    }
}
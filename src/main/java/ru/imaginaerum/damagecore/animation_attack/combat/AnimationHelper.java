package ru.imaginaerum.damagecore.animation_attack.combat;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationSetup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimationHelper {
    private static final Map<ResourceLocation, Long> DURATION_CACHE = new ConcurrentHashMap<>();
    private static final long FALLBACK_DURATION_MS = 500L;

    // Полный список костей из PlayerAnimationController.registerBones()
    private static final String[] ROTATABLE_BONES = {
            "body", "right_arm", "left_arm", "right_leg", "left_leg",
            "head", "torso"
    };

    private AnimationHelper() {}

    public static PlayerAnimationController controller(AbstractClientPlayer player) {
        var layer = PlayerAnimationAccess.getPlayerAnimationLayer(
                player, WeaponAnimationSetup.ATTACK_LAYER_ID);
        return layer instanceof PlayerAnimationController c ? c : null;
    }
    private static float wrapRadians(float rad) {
        final float twoPi = (float) (2 * Math.PI);
        float d = rad % twoPi;
        if (d >= Math.PI) d -= twoPi;
        else if (d < -Math.PI) d += twoPi;
        return d;
    }
    public static void trigger(AbstractClientPlayer player, ResourceLocation animationId) {
        PlayerAnimationController c = controller(player);
        if (c != null) c.triggerAnimation(animationId);
    }

    public static void stop(AbstractClientPlayer player) {
        PlayerAnimationController c = controller(player);
        if (c != null) c.stop();
    }

    /**
     * Плавный возврат в дефолт. Перед стартом фейда нормализует углы костей
     * (кратчайший эквивалент в диапазоне [-180, 180]), иначе кости, накопившие
     * несколько полных оборотов за анимацию, будут "раскручиваться" обратно.
     */
    public static void fadeOut(AbstractClientPlayer player, int fadeTicks) {
        PlayerAnimationController c = controller(player);
        if (c == null) return;

        for (String name : ROTATABLE_BONES) {
            AdvancedPlayerAnimBone bone = c.getBone(name);
            if (bone == null) continue;
            bone.setRotX(wrapRadians(bone.getRotX()));
            bone.setRotY(wrapRadians(bone.getRotY()));
            bone.setRotZ(wrapRadians(bone.getRotZ()));
        }

        RawAnimation waitAnim = RawAnimation.begin().thenWait(fadeTicks + 20);
        c.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(fadeTicks, EasingType.EASE_IN_OUT_SINE),
                waitAnim);
    }


    public static long durationMs(ResourceLocation animationId) {
        return DURATION_CACHE.computeIfAbsent(animationId, id -> {
            if (!PlayerAnimResources.hasAnimation(id)) return FALLBACK_DURATION_MS;
            Animation anim = PlayerAnimResources.getAnimation(id);
            return (long) (anim.length() * 50);
        });
    }
}
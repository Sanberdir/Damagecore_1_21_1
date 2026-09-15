package ru.imaginaerum.damagecore.animation_attack.combat;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationSetup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimationHelper {
    private static final Map<ResourceLocation, Long> DURATION_CACHE = new ConcurrentHashMap<>();
    private static final long FALLBACK_DURATION_MS = 500L;

    private AnimationHelper() {}

    public static PlayerAnimationController controller(AbstractClientPlayer player) {
        var layer = PlayerAnimationAccess.getPlayerAnimationLayer(
                player, WeaponAnimationSetup.ATTACK_LAYER_ID);
        return layer instanceof PlayerAnimationController c ? c : null;
    }

    public static void trigger(AbstractClientPlayer player, ResourceLocation animationId) {
        PlayerAnimationController c = controller(player);
        if (c != null) c.triggerAnimation(animationId);
    }

    public static void stop(AbstractClientPlayer player) {
        PlayerAnimationController c = controller(player);
        if (c != null) c.stop();
    }

    public static long durationMs(ResourceLocation animationId) {
        return DURATION_CACHE.computeIfAbsent(animationId, id -> {
            if (!PlayerAnimResources.hasAnimation(id)) return FALLBACK_DURATION_MS;
            Animation anim = PlayerAnimResources.getAnimation(id);
            return (long) (anim.length() * 50);
        });
    }
}
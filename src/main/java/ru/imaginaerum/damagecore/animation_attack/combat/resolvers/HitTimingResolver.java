package ru.imaginaerum.damagecore.animation_attack.combat.resolvers;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.animation_attack.WeaponAnimationManager.AnimEntry;
import ru.imaginaerum.damagecore.animation_attack.combat.AnimationHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HitTimingResolver {

    public static final String HIT_MARKER_BONE = "hit_marker";

    private static final float FALLBACK_FRACTION = 0.4f;
    private static final long FALLBACK_MS = 200L;

    private static final Map<ResourceLocation, Long> CACHE = new ConcurrentHashMap<>();

    private HitTimingResolver() {}

    public static long resolveMs(AnimEntry entry) {
        if (entry.hitTimeMs() != null) {
            return Math.max(0L, entry.hitTimeMs());
        }
        if (entry.hitTimeFraction() != null) {
            long duration = AnimationHelper.durationMs(entry.animation());
            return Math.max(0L, (long) (duration * entry.hitTimeFraction()));
        }
        return resolveMs(entry.animation());
    }

    public static long resolveMs(ResourceLocation animationId) {
        return CACHE.computeIfAbsent(animationId, id -> {
            if (!PlayerAnimResources.hasAnimation(id)) return FALLBACK_MS;
            Animation anim = PlayerAnimResources.getAnimation(id);
            float hitSec = readHitMarkerTimeSec(anim);
            if (hitSec < 0f) {
                hitSec = anim.length() * FALLBACK_FRACTION;
            }
            return Math.max(1L, (long) (hitSec * 50));
        });
    }

    public static void clearCache() { CACHE.clear(); }

    private static float readHitMarkerTimeSec(Animation anim) {
        BoneAnimation marker = anim.getBone(HIT_MARKER_BONE);
        if (marker == null) return -1f;

        float t = Float.MAX_VALUE;
        t = Math.min(t, firstKeyframeEnd(marker.rotationKeyFrames()));
        t = Math.min(t, firstKeyframeEnd(marker.positionKeyFrames()));
        return t == Float.MAX_VALUE ? -1f : t;
    }

    private static float firstKeyframeEnd(KeyframeStack stack) {
        if (stack == null || !stack.hasKeyframes()) return Float.MAX_VALUE;
        float t = Float.MAX_VALUE;
        for (List<Keyframe> axis : new List[]{ stack.xKeyframes(), stack.yKeyframes(), stack.zKeyframes() }) {
            if (axis != null && !axis.isEmpty()) {
                t = Math.min(t, axis.get(0).length());
            }
        }
        return t;
    }
}
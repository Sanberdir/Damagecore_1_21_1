package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SyncEffectSourceClientProxy {
    public static void apply(MobEffect effect, EntityType<?> sourceType) {
        PotionTrackingClient.registerMobEffectFromServer(effect, sourceType);
    }
}
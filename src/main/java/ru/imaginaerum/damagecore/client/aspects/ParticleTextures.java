package ru.imaginaerum.damagecore.client.aspects;

import net.minecraft.resources.ResourceLocation;

public final class ParticleTextures {
    /** Атлас частиц: 1024x1024, сетка 64x64 кадра по 16px. */
    public static final ResourceLocation PARTICLES =
            ResourceLocation.fromNamespaceAndPath("damagecore", "textures/particle/particles.png");

    private ParticleTextures() {}
}
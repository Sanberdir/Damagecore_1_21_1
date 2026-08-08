package ru.imaginaerum.damagecore.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class DCParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Damagecore_1_21_1_neo.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STUN =
            PARTICLE_TYPES.register("stun", () -> new SimpleParticleType(true));
}
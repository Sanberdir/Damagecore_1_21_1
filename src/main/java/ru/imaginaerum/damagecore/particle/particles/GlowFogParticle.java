package ru.imaginaerum.damagecore.particle.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.particle.DCParticles;

public class GlowFogParticle extends TextureSheetParticle {
    private final float baseSize;

    protected GlowFogParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.pickSprite(sprites);
        this.lifetime = 36 + this.random.nextInt(24);
        this.baseSize = (0.30f + this.random.nextFloat() * 0.25f) * 0.63f;
        this.quadSize = baseSize;
        this.gravity = 0f;
        this.friction = 0.94f;
        this.hasPhysics = false;
        this.xd = vx + (this.random.nextFloat() - 0.5f) * 0.004;
        this.yd = vy + 0.004 + this.random.nextFloat() * 0.004;
        this.zd = vz + (this.random.nextFloat() - 0.5f) * 0.004;
        // сине-фиолетовый, как у лучей
        float m = this.random.nextFloat();
        this.setColor(Mth.lerp(m, 0.40f, 0.72f), Mth.lerp(m, 0.50f, 0.38f), 1.0f);
        this.alpha = 0f;
    }

    @Override
    public void tick() {
        super.tick();
        float p = (float) this.age / this.lifetime;
        this.alpha = 0.42f * Mth.sin(p * Mth.PI);       // появилась -> растаяла
        this.quadSize = baseSize * (1f + 0.9f * p);     // расплывается
        this.oRoll = this.roll;
        this.roll += 0.004f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;   // полная яркость
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new GlowFogParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }

    @EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegister(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(DCParticles.GLOW_FOG.get(), Provider::new);
        }
    }
}
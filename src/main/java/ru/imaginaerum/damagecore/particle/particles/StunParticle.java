package ru.imaginaerum.damagecore.particle.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StunParticle extends TextureSheetParticle {

    StunParticle(ClientLevel level, double x, double y, double z,
                 double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z);
        // Настройки
        this.setSize(0.02F, 0.02F);
        this.quadSize = 0.15F + this.random.nextFloat() * 0.05F;
        this.lifetime = 4; // Короткая жизнь (0.5 секунды)

        // Легкое движение вверх
        this.xd = (Math.random() * 2.0D - 1.0D) * 0.01;
        this.yd = 0.01 + (Math.random() * 2.0D - 1.0D) * 0.005;
        this.zd = (Math.random() * 2.0D - 1.0D) * 0.01;

        // Слабая гравитация
        this.gravity = -0.001F;
    }


    public void tick() {
        super.tick();

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.lifetime-- <= 0) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);

            // Плавное исчезновение
            if (this.age > this.lifetime - 5) {
                this.alpha = 0.8f * (1.0f - (float)(this.age - (this.lifetime - 5)) / 5.0f);
            }
        }
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            StunParticle particle = new StunParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
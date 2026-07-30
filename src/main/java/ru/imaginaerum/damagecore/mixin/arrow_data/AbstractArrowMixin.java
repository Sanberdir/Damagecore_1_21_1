package ru.imaginaerum.damagecore.mixin.arrow_data;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.arrow_data.ArrowDamageData;
import ru.imaginaerum.damagecore.library_damage.arrow_data.ArrowDamageManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Shadow protected boolean inGround;
    @Shadow protected abstract ItemStack getPickupItem();

    // Инвокер для protected AbstractArrow#doKnockback(LivingEntity, DamageSource)
    // (в 1.21+ вся логика нокбэка спрятана здесь, отдельного поля knockback больше нет)
    @Invoker("doKnockback")
    public abstract void invokeDoKnockback(LivingEntity target, DamageSource source);

    @Unique private boolean dc_flatDamage = false;
    @Unique private float dc_distanceTraveled     = 0f;
    @Unique private boolean dc_rangeCrossed       = false;
    @Unique private Map<DamageType, Double> dc_finalDamageMap = null;
    @Unique private float dc_maxRange             = -1f;
    @Unique private float dc_postRangeDamageScale = 0f;
    @Unique private float dc_postRangeSpeedScale  = 0f;
    @Unique private float dc_gravityScale         = 1.0f;
    @Unique private static final java.util.Random DC_RANDOM = new java.util.Random();
    @Unique private boolean dc_initialized = false;
    @Unique private boolean dc_particlesEnabled = true;
    @Unique private String dc_particleType = "default";
    @Unique private boolean dc_trailParticles = true;
    @Unique private List<ArrowDamageData.OnHitEffect> dc_onHitEffects = null;
    @Unique private int dc_onHitFire = 0;

    @Inject(method = "shoot", at = @At("TAIL"))
    private void dc_onShoot(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        ItemStack stack = getPickupItem();

        if (stack == null || ArrowDamageManager.INSTANCE == null
                || !ArrowDamageManager.INSTANCE.hasData(stack.getItem())) return;

        var data = ArrowDamageManager.INSTANCE.getData(stack.getItem());

        float speedMultiplier = data.getBaseSpeed() / 3.0f;
        if (Math.abs(speedMultiplier - 1.0f) > 0.01f) {
            self.setDeltaMovement(self.getDeltaMovement().scale(speedMultiplier));
        }

        dc_initialized = false;
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void dc_suppressTrailParticles(
            net.minecraft.world.level.Level level,
            net.minecraft.core.particles.ParticleOptions particle,
            double x, double y, double z,
            double dx, double dy, double dz) {

        if (level.isClientSide && !dc_trailParticles) return;
        level.addParticle(particle, x, y, z, dx, dy, dz);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dc_applyCustomGravity(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        if (inGround) return;

        ItemStack stack = getPickupItem();
        if (stack == null || ArrowDamageManager.INSTANCE == null ||
                !ArrowDamageManager.INSTANCE.hasData(stack.getItem())) return;

        if (!dc_initialized) {
            var data = ArrowDamageManager.INSTANCE.getData(stack.getItem());
            dc_gravityScale     = data.getGravityScale();
            dc_flatDamage       = data.isFlatDamage();
            dc_particlesEnabled = data.isParticlesEnabled();
            dc_particleType     = data.getParticleType();
            dc_trailParticles = data.isTrailParticles();
            dc_onHitEffects   = data.getOnHitEffects();
            dc_onHitFire      = data.getOnHitFire();
            dc_setArrowStats(
                    data.getDamageMap(),
                    data.getBaseRange(),
                    data.getPostRangeDamageScale(),
                    data.getPostRangeSpeedScale()
            );
            dc_initialized = true;
        }

        if (dc_gravityScale == 1.0f) return;

        Vec3 vel = self.getDeltaMovement();
        double correction = 0.05 * (1.0 - dc_gravityScale);
        self.setDeltaMovement(vel.x, vel.y + correction, vel.z);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dc_onTick(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        if (inGround) return;

        ItemStack stack = getPickupItem();
        boolean hasData = stack != null && ArrowDamageManager.INSTANCE != null
                && ArrowDamageManager.INSTANCE.hasData(stack.getItem());
        if (!hasData) return;

        if (!dc_trailParticles) {
            self.setNoGravity(self.isNoGravity());
        }
        if (dc_gravityScale == 0.0f) {
            Vec3 vel2 = self.getDeltaMovement();
            self.setDeltaMovement(vel2.x / 0.99, vel2.y, vel2.z / 0.99);
        }
        Vec3 vel = self.getDeltaMovement();
        dc_distanceTraveled += (float) vel.length();

        if (dc_maxRange > 0 && !dc_rangeCrossed && dc_distanceTraveled >= dc_maxRange) {
            dc_rangeCrossed = true;
            if (dc_postRangeSpeedScale != 0f)
                self.setDeltaMovement(self.getDeltaMovement().scale(1.0 + dc_postRangeSpeedScale));
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void dc_onHitEntity(EntityHitResult result, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        if (inGround) return;

        Map<DamageType, Double> damageMap = dc_finalDamageMap;
        ItemStack stack = getPickupItem();
        ArrowDamageData arrowData = (stack != null && ArrowDamageManager.INSTANCE != null)
                ? ArrowDamageManager.INSTANCE.getData(stack.getItem()) : null;

        if (damageMap == null && arrowData != null) {
            damageMap = arrowData.getDamageMap();
        }

        float damageMult = 1.0f;
        if (dc_maxRange > 0 && dc_distanceTraveled > dc_maxRange && dc_postRangeDamageScale != 0f) {
            float over = (dc_distanceTraveled - dc_maxRange) / dc_maxRange;
            damageMult = 1.0f + over * dc_postRangeDamageScale;
            damageMult = Math.max(damageMult, 0.1f);
        }

        if (damageMap == null || damageMap.isEmpty()) return;

        Entity target = result.getEntity();
        Entity owner  = self.getOwner();
        if (target == null) return;

        boolean isCrit = self.isCritArrow();
        float speed = (float) self.getDeltaMovement().length();

        // Entity#setSecondsOnFire(int) -> Entity#igniteForSeconds(float) в 1.21+
        if (dc_onHitFire > 0) target.igniteForSeconds((float) dc_onHitFire);

        DamageSource lastSource = null;

        // Ванильные эффекты
        if (dc_onHitEffects != null && target instanceof LivingEntity le) {
            for (ArrowDamageData.OnHitEffect eff : dc_onHitEffects) {
                net.minecraft.resources.ResourceLocation rl =
                        net.minecraft.resources.ResourceLocation.parse(eff.effect);
                MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(rl);
                if (mobEffect != null) {
                    // MobEffectInstance теперь требует Holder<MobEffect>, а не голый MobEffect
                    Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect);
                    le.addEffect(new MobEffectInstance(effectHolder, eff.duration, eff.amplifier));
                }
            }
        }
        for (Map.Entry<DamageType, Double> entry : damageMap.entrySet()) {
            DamageType type  = entry.getKey();
            double rawDamage = entry.getValue();

            if (isCrit) {
                rawDamage += DC_RANDOM.nextInt((int)(rawDamage / 2) + 2);
                rawDamage  = Math.min(rawDamage, Integer.MAX_VALUE);
            }

            int damage = Mth.ceil(rawDamage * (dc_flatDamage ? 1.0f : speed) * damageMult);
            DamageSource source = self.damageSources().arrow(self, owner != null ? owner : self);
            lastSource = source;
            target.hurt(source, damage);
        }

        handlePostHit(self, target, lastSource, dc_particlesEnabled, dc_particleType);
        ci.cancel();
    }

    @Unique
    private void handlePostHit(AbstractArrow self, Entity target, DamageSource source,
                               boolean particlesEnabled, String particleType) {
        // Раньше: ручной расчёт нокбэка на основе self.getKnockback() (метод удалён в 1.21).
        // Теперь используем ванильный protected AbstractArrow#doKnockback через инвокер —
        // он сам берёт нужное значение (в т.ч. из атрибутов/зачарований оружия).
        if (target instanceof LivingEntity le && source != null) {
            invokeDoKnockback(le, source);
        }

        if (particlesEnabled) {
            switch (particleType) {
                case "crit"  -> self.level().broadcastEntityEvent(self, (byte) 1);
                case "magic" -> self.level().broadcastEntityEvent(self, (byte) 2);
            }
        }

        self.playSound(net.minecraft.sounds.SoundEvents.ARROW_HIT,
                1.0f, 1.2f / (DC_RANDOM.nextFloat() * 0.2f + 0.9f));

        if (self.getPierceLevel() <= 0) self.discard();
    }

    @Unique
    public void dc_setArrowStats(Map<DamageType, Double> damageMap,
                                 float maxRange,
                                 float postRangeDmgScale,
                                 float postRangeSpeedScale) {
        this.dc_finalDamageMap       = new HashMap<>(damageMap);
        this.dc_maxRange             = maxRange;
        this.dc_postRangeDamageScale = postRangeDmgScale;
        this.dc_postRangeSpeedScale  = postRangeSpeedScale;
        this.dc_distanceTraveled     = 0f;
        this.dc_rangeCrossed         = false;
    }
}
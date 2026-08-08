package ru.imaginaerum.damagecore.effect.replace_vanila;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = "damagecore")
public class VanillaResistanceOverrideHandler {

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        if (!entity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) return;
        MobEffectInstance inst = entity.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (inst == null) return;

        int level = inst.getAmplifier() + 1; // 0 -> level 1
        float perLevel = 0.10f; // 10% per level
        float totalReduction = perLevel * level;

        DamageContainer container = event.getContainer();
        DamageSource source = container.getSource();
        DamageType dt = mapDamageSourceToDamageType(source);

        if (dt == null) return;

        if (dt == DamageType.PIERCING || dt == DamageType.SLASHING || dt == DamageType.BLUDGEONING) {
            float old = container.getNewDamage();
            float updated = old * (1.0f - totalReduction);
            if (updated < 0f) updated = 0f;
            container.setNewDamage(updated);
        }
    }

    private static DamageType mapDamageSourceToDamageType(DamageSource source) {
        if (source == null) return null;

        try {
            ResourceLocation id = source.typeHolder().unwrapKey().orElseThrow().location();
            String path = id.getPath();
            switch (path) {
                case "arrow":
                case "trident":
                case "piercing":
                    return DamageType.PIERCING;
                case "player":
                case "mob":
                case "bludgeoning":
                case "fall":
                case "fly_into_wall":
                    return DamageType.BLUDGEONING;
                case "slashing":
                    return DamageType.SLASHING;
            }
        } catch (Exception ignored) {}

        return null;
    }
}
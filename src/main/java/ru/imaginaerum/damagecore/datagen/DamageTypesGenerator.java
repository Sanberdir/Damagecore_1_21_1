package ru.imaginaerum.damagecore.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class DamageTypesGenerator {
    public static final ResourceKey<DamageType> POISON =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "poison"));

    public static final ResourceKey<DamageType> BLEEDING_1 =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "bleeding_1"));
    public static final ResourceKey<DamageType> BLEEDING_2 =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "bleeding_2"));
    public static final ResourceKey<DamageType> BLEEDING_3 =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "bleeding_3"));

    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(POISON,
                new DamageType("poison",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F,               // exhaustion (истощение)
                        DamageEffects.HURT, // эффект при получении урона
                        DeathMessageType.DEFAULT // 1.21.1: Новый обязательный параметр
                ));

        context.register(BLEEDING_1,
                new DamageType("bleeding_1",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F,
                        DamageEffects.HURT,
                        DeathMessageType.DEFAULT // 1.21.1: Новый обязательный параметр
                ));

        context.register(BLEEDING_2,
                new DamageType("bleeding_2",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F,
                        DamageEffects.HURT,
                        DeathMessageType.DEFAULT // 1.21.1: Новый обязательный параметр
                ));

        context.register(BLEEDING_3,
                new DamageType("bleeding_3",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F,
                        DamageEffects.HURT,
                        DeathMessageType.DEFAULT // 1.21.1: Новый обязательный параметр
                ));
    }
}
package ru.imaginaerum.damagecore.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.effect.effects.Bleeding2Effect;
import ru.imaginaerum.damagecore.effect.effects.Bleeding3Effect;
import ru.imaginaerum.damagecore.effect.effects.BleedingEffect;
import ru.imaginaerum.damagecore.effect.effects.DeathPoisonEffect;
import ru.imaginaerum.damagecore.effect.effects.Drunkenness2Effect;
import ru.imaginaerum.damagecore.effect.effects.Drunkenness3Effect;
import ru.imaginaerum.damagecore.effect.effects.DrunkennessEffect;
import ru.imaginaerum.damagecore.effect.effects.StunningEffect;

public class DCEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(
                    Registries.MOB_EFFECT,
                    Damagecore_1_21_1_neo.MODID
            );

    public static final DeferredHolder<MobEffect, StunningEffect> STUNNING =
            MOB_EFFECTS.register(
                    "stunning",
                    () -> new StunningEffect(
                            MobEffectCategory.HARMFUL,
                            0x7d746d
                    )
            );

    public static final DeferredHolder<MobEffect, DeathPoisonEffect> DEATH_POISON =
            MOB_EFFECTS.register(
                    "death_poison",
                    () -> new DeathPoisonEffect(
                            MobEffectCategory.HARMFUL,
                            0x556832
                    )
            );

    public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING_1 =
            MOB_EFFECTS.register(
                    "bleeding_1",
                    () -> new BleedingEffect(
                            MobEffectCategory.HARMFUL,
                            0xAA2232
                    )
            );

    public static final DeferredHolder<MobEffect, Bleeding2Effect> BLEEDING_2 =
            MOB_EFFECTS.register(
                    "bleeding_2",
                    () -> new Bleeding2Effect(
                            MobEffectCategory.HARMFUL,
                            0xAA2232
                    )
            );

    public static final DeferredHolder<MobEffect, Bleeding3Effect> BLEEDING_3 =
            MOB_EFFECTS.register(
                    "bleeding_3",
                    () -> new Bleeding3Effect(
                            MobEffectCategory.HARMFUL,
                            0xAA2232
                    )
            );

    public static final DeferredHolder<MobEffect, DrunkennessEffect> DRUNKENNESS_1 =
            MOB_EFFECTS.register(
                    "drunkenness_1",
                    () -> new DrunkennessEffect(
                            MobEffectCategory.HARMFUL,
                            0x082567
                    )
            );

    public static final DeferredHolder<MobEffect, Drunkenness2Effect> DRUNKENNESS_2 =
            MOB_EFFECTS.register(
                    "drunkenness_2",
                    () -> new Drunkenness2Effect(
                            MobEffectCategory.HARMFUL,
                            0x0000ff
                    )
            );

    public static final DeferredHolder<MobEffect, Drunkenness3Effect> DRUNKENNESS_3 =
            MOB_EFFECTS.register(
                    "drunkenness_3",
                    () -> new Drunkenness3Effect(
                            MobEffectCategory.HARMFUL,
                            0xa6caf0
                    )
            );
}

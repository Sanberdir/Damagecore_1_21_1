package ru.imaginaerum.damagecore.sounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class DCSoundEvents {
    // В 1.21.1 используем Registries.SOUND_EVENT вместо ForgeRegistries
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Damagecore_1_21_1_neo.MODID);

    // В NeoForge используется DeferredHolder вместо RegistryObject
    public static final DeferredHolder<SoundEvent, SoundEvent> LEARNING_SKILL =
            registerSoundEvents("learning_skill");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvents(String name) {
        // Ньюанс 1.21+: 'new ResourceLocation()' теперь приватный, используем .fromNamespaceAndPath()
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}


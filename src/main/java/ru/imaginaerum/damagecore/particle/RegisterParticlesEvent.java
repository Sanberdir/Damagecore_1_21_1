package ru.imaginaerum.damagecore.particle;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import ru.imaginaerum.damagecore.particle.particles.StunParticle;

public class RegisterParticlesEvent {
    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSideHandler {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Вызвать на КЛИЕНТСКОЙ стороне
        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {
            Minecraft.getInstance().particleEngine.register(DCParticles.STUN.get(),
                    StunParticle.Provider::new);
        }
    }
}
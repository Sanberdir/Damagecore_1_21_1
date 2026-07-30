package ru.imaginaerum.damagecore.animation_attack;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;

import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class WeaponAnimationSetup {

    public static final ResourceLocation ATTACK_LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "weapon_attack");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        ATTACK_LAYER_ID,
                        1500,
                        player -> new PlayerAnimationController(
                                player,
                                (controller, state, animSetter) -> PlayState.STOP
                        )
                )
        );
    }
}
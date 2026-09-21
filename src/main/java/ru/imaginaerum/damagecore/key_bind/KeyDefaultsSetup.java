package ru.imaginaerum.damagecore.key_bind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class KeyDefaultsSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Options o = Minecraft.getInstance().options;

            InputConstants.Key shift = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT);
            InputConstants.Key ctrl  = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL);

            KeyMapping sprint = o.keySprint;
            KeyMapping sneak  = o.keyShift;

            // Игрок ещё не менял клавишу вручную (стоит старый дефолт)?
            boolean sprintUntouched = sprint.getKey().equals(sprint.getDefaultKey());
            boolean sneakUntouched  = sneak.getKey().equals(sneak.getDefaultKey());

            // Новые значения по умолчанию (для кнопки «Сбросить»)
            sprint.defaultKey = shift;
            sneak.defaultKey  = ctrl;

            // Текущие клавиши меняем только если игрок их не переназначал
            if (sprintUntouched) sprint.setKey(shift);
            if (sneakUntouched)  sneak.setKey(ctrl);

            KeyMapping.resetMapping();
            o.save();
        });
    }
}
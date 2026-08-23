package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.ICombatModeEntity;
import ru.imaginaerum.damagecore.library_extra_slots.SwapTwoSlotsPacket;
import ru.imaginaerum.damagecore.library_extra_slots.network.SwapOffhandWithSlotZeroPacket;

@Mixin(Minecraft.class)
public abstract class MinecraftKeybindsMixin {

    @Unique
    private boolean damagecore$ctrlEWasPressed = false;

    @Unique
    private boolean damagecore$ctrlQWasPressed = false;

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void damagecore$interceptCombinations(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player == null || mc.screen != null) {
            damagecore$ctrlEWasPressed = false;
            damagecore$ctrlQWasPressed = false;
            return;
        }

        long windowHandle = mc.getWindow().getWindow();

        boolean isCtrlDown = InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);

        KeyMapping keyInventory = mc.options.keyInventory;
        boolean eDownNow = isCtrlDown && damagecore$isKeyMappingPressed(keyInventory);

        KeyMapping keyDrop = mc.options.keyDrop;
        boolean qDownNow = isCtrlDown && damagecore$isKeyMappingPressed(keyDrop);

        if (eDownNow && !damagecore$ctrlEWasPressed) {
            while (keyInventory.consumeClick()) {}
            PacketDistributor.sendToServer(new SwapTwoSlotsPacket());
            ci.cancel();
        }
        damagecore$ctrlEWasPressed = eDownNow;

        if (ci.isCancelled()) {
            damagecore$ctrlQWasPressed = qDownNow;
            return;
        }

        if (qDownNow && !damagecore$ctrlQWasPressed) {
            while (keyDrop.consumeClick()) {}
            PacketDistributor.sendToServer(new SwapOffhandWithSlotZeroPacket());
            ci.cancel();
        }
        damagecore$ctrlQWasPressed = qDownNow;
    }

    @Unique
    private boolean damagecore$isKeyMappingPressed(KeyMapping keyMapping) {
        if (keyMapping.isUnbound()) return false;

        InputConstants.Key key = keyMapping.getKey();
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();

        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(windowHandle, key.getValue());
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
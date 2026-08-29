package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class AccessorySlotHudMixin {

    private static final ResourceLocation SLOT_LEFT_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final ResourceLocation SLOT_RIGHT_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_right");

    private static final int SLOT_W = 29;
    private static final int SLOT_H = 24;

    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderAccessorySlotIcon(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;

        // Получаем состояние режима боя
        boolean combatMode = ((ru.imaginaerum.damagecore.library_extra_slots.ICombatModeEntity) player).damagecore$isCombatMode();
        if (!combatMode) return; // Скрываем абсолютно всё, если не в боевом режиме

        ItemStackHandler handler =
                ((ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory) player.getInventory()).damagecore$getExtraSlots();
        if (handler == null) return;

        // Предметы из кастомных слотов
        ItemStack stack0 = handler.getStackInSlot(0); // Слот 0 (Левее щита)
        ItemStack stack1 = handler.getStackInSlot(1); // Слот 1 (Правее хотбара, первый)
        ItemStack stack2 = handler.getStackInSlot(2); // Слот 2 (Теперь крайний справа)

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int hotbarLeftEdge  = screenWidth / 2 - 91;
        int hotbarRightEdge = screenWidth / 2 + 91;
        int slotY = screenHeight - SLOT_H + 1;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // ==========================================
        // ЛЕВАЯ СТОРОНА (Слот 0 и Ванильный Offhand)
        // ==========================================

        // 1. Слот 0 (Левее слота щита)
        int slot0X = hotbarLeftEdge - 51;
        guiGraphics.blitSprite(SLOT_LEFT_SPRITE, slot0X, slotY, SLOT_W, SLOT_H);
        if (!stack0.isEmpty()) {
            int itemX = slot0X + 3;
            int itemY = slotY + 4;
            guiGraphics.renderItem(stack0, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack0, itemX, itemY);
        }

        // 2. Ванильный слот щита (Показывается ВСЕГДА в боевом режиме)
        if (player.getOffhandItem().isEmpty()) {
            int offhandX = hotbarLeftEdge - 29;
            guiGraphics.blitSprite(SLOT_LEFT_SPRITE, offhandX, slotY, SLOT_W, SLOT_H);
        }


        // ==========================================
        // ПРАВАЯ СТОРОНА (Слот 1, Слот 2, Слот 3)
        // ==========================================

        // 3. Слот 1 (Правее хотбара, первый по счету)
        int slot1X = hotbarRightEdge + 22;
        guiGraphics.blitSprite(SLOT_RIGHT_SPRITE, slot1X, slotY, SLOT_W, SLOT_H);
        if (!stack1.isEmpty()) {
            int itemX = slot1X + 10;
            int itemY = slotY + 4;
            guiGraphics.renderItem(stack1, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack1, itemX, itemY);
        }

        // 4. Слот 2 (Логический Слот 3 — индекс 2 в handler. Выводим его ПЕРЕД вторым)
        int slot2X = hotbarRightEdge;
        guiGraphics.blitSprite(SLOT_RIGHT_SPRITE, slot2X, slotY, SLOT_W, SLOT_H);
        if (!stack2.isEmpty()) {
            int itemX = slot2X + 10;
            int itemY = slotY + 4;
            guiGraphics.renderItem(stack2, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack2, itemX, itemY);
        }

    }
}

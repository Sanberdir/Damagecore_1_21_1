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

        // ИСПРАВЛЕНО: Убраны нерабочие проверки старых ModAttachments.
        // Запрашиваем хендлер предметов напрямую через интерфейс-утку расширенного инвентаря
        ItemStackHandler handler =
                ((ru.imaginaerum.damagecore.library_extra_slots.IExtraSlotsInventory) player.getInventory()).damagecore$getExtraSlots();

        // Если по какой-то причине хендлер не инициализировался (подстраховка)
        if (handler == null) return;

        ItemStack stack0 = handler.getStackInSlot(0); // Пара к щиту (слева)
        ItemStack stack1 = handler.getStackInSlot(1); // Пара к 3-му слоту (крайний справа)
        ItemStack stack2 = handler.getStackInSlot(2); // 3-й слот (справа, встык к хотбару)

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Базовые опорные точки ванильного хотбара
        int hotbarLeftEdge  = screenWidth / 2 - 91;
        int hotbarRightEdge = screenWidth / 2 + 91;

        int slotY = screenHeight - SLOT_H + 1;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // ==========================================
        // 1. ЛЕВАЯ СТОРОНА: Слот 1 (индекс 0) — пара для щита
        // ==========================================
        if (!stack0.isEmpty()) {
            int leftSlotX = hotbarLeftEdge - 51;

            guiGraphics.blitSprite(SLOT_LEFT_SPRITE, leftSlotX, slotY, SLOT_W, SLOT_H);

            int itemX = leftSlotX + 3;
            int itemY = slotY + 4;
            guiGraphics.renderItem(stack0, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack0, itemX, itemY);
        }

        // ==========================================
        // 2. ПРАВАЯ СТОРОНА: Слот 3 (индекс 2) — прилегает к хотбару
        // ==========================================
        if (!stack2.isEmpty()) {
            int rightSlotX2 = hotbarRightEdge;

            guiGraphics.blitSprite(SLOT_RIGHT_SPRITE, rightSlotX2, slotY, SLOT_W, SLOT_H);

            int itemX = rightSlotX2 + 10;
            int itemY = slotY + 4;
            guiGraphics.renderItem(stack2, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack2, itemX, itemY);
        }

        // ==========================================
        // 3. КРАЙНЯЯ ПРАВАЯ СТОРОНА: Слот 2 (индекс 1) — пара к Слотоу 3
        // ==========================================
        if (!stack1.isEmpty()) {
            int rightSlotX1 = hotbarRightEdge + 22;

            guiGraphics.blitSprite(SLOT_RIGHT_SPRITE, rightSlotX1, slotY, SLOT_W, SLOT_H);

            int itemX = rightSlotX1 + 10;
            int itemY = slotY + 4;
            guiGraphics.renderItem(stack1, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack1, itemX, itemY);
        }

        RenderSystem.disableBlend();
    }
}

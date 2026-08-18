package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.ModAttachments;

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
        if (!ModAttachments.EXTRA_SLOTS.isBound()) return;
        if (!player.hasData(ModAttachments.EXTRA_SLOTS)) return;

        ModAttachments.ExtraSlotsHandler handler =
                (ModAttachments.ExtraSlotsHandler) player.getData(ModAttachments.EXTRA_SLOTS);

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
        // Ванильный щит прилегает к хотбару слева. Сдвиг для идеальной стыковки встык
        // с учетом прозрачных полей текстуры составляет ровно -51 пиксель (а не -58).
        if (!stack0.isEmpty()) {
            int leftSlotX = hotbarLeftEdge - 51;

            guiGraphics.blitSprite(SLOT_LEFT_SPRITE, leftSlotX, slotY, SLOT_W, SLOT_H);

            // Смещение предмета скорректировано под новую позицию спрайта
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
        // Вместо +29 сдвигаем всего на +22 пикселя.
        // Это полностью перекроет прозрачные поля и сольет рамки слотов в единую линию.
        if (!stack1.isEmpty()) {
            int rightSlotX1 = hotbarRightEdge + 22;

            guiGraphics.blitSprite(SLOT_RIGHT_SPRITE, rightSlotX1, slotY, SLOT_W, SLOT_H);

            // Сдвигаем предмет на ту же разницу (-7 пикселей относительно геометрического центра нового спрайта),
            // чтобы он остался визуально по центру своей рамки: +10.
            int itemX = rightSlotX1 + 10;
            int itemY = slotY + 4;
            guiGraphics.renderItem(stack1, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack1, itemX, itemY);
        }

        RenderSystem.disableBlend();
    }
}

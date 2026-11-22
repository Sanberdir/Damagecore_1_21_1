package ru.imaginaerum.damagecore.Init.blocks.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.Init.blocks.entity.EmbodimentTableBlockEntity;
import ru.imaginaerum.damagecore.Init.blocks.entity.EmbodimentTableRenderer;

import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import java.util.List;

public class EmbodimentTableScreen extends AbstractContainerScreen<EmbodimentTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "textures/gui/container/embodiment_table.png");

    private static final int RAIL_X = 15, RAIL_Y = 57, RAIL_W = 36;
    private static final int HANDLE_W = 11, HANDLE_H = 6;
    private BookModel bookModel;
    private final RandomSource random = RandomSource.create();
    private float flip, oFlip, flipT, flipA;
    private float open, oOpen;
    private int lastTarget = -1, lastInput = -1;
    private boolean dragging;
    private int lastSent = -1;
    private Button toggleButton;
    public EmbodimentTableScreen(EmbodimentTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        this.inventoryLabelY = this.imageHeight - 94;
    }
    @Override
    public void containerTick() {
        super.containerTick();

        int target = menu.getTarget();
        int input = menu.getInputCount();
        if (!dragging && (target != lastTarget || input != lastInput)) {
            do {
                flipT += (float) (random.nextInt(4) - random.nextInt(4));
            } while (flip <= flipT + 1.0F && flip >= flipT - 1.0F);
        }
        lastTarget = target;
        lastInput = input;

        oFlip = flip;
        oOpen = open;
        boolean active = maxSelectable() > 0 || target > 0;
        open += active ? 0.2F : -0.2F;
        open = Mth.clamp(open, 0.0F, 1.0F);

        float f = Mth.clamp((flipT - flip) * 0.4F, -0.2F, 0.2F);
        flipA += (f - flipA) * 0.9F;
        flip += flipA;
        if (toggleButton != null) {
            toggleButton.active = !menu.isRunning() && menu.getTarget() > 0;
        }
    }
    private void renderBook(GuiGraphics g, float partialTick) {
        float o = Mth.lerp(partialTick, oOpen, open);
        float fl = Mth.lerp(partialTick, oFlip, flip);

        Lighting.setupForEntityInInventory();
        g.flush();
        g.pose().pushPose();
        g.pose().translate(leftPos + 33.0F, topPos + 31.0F, 100.0F);
        g.pose().scale(-40.0F, 40.0F, 40.0F);
        g.pose().mulPose(Axis.XP.rotationDegrees(25.0F));
        g.pose().translate((1.0F - o) * 0.2F, (1.0F - o) * 0.1F, (1.0F - o) * 0.25F);
        g.pose().mulPose(Axis.YP.rotationDegrees(-(1.0F - o) * 90.0F - 90.0F));
        g.pose().mulPose(Axis.XP.rotationDegrees(180.0F));

        float p1 = Mth.clamp(Mth.frac(fl + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float p2 = Mth.clamp(Mth.frac(fl + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        bookModel.setupAnim(0.0F, p1, p2, o);

        VertexConsumer vc = g.bufferSource().getBuffer(bookModel.renderType(EmbodimentTableRenderer.BOOK_TEXTURE));
        bookModel.renderToBuffer(g.pose(), vc, 15728880, OverlayTexture.NO_OVERLAY, -1);
        g.flush();
        g.pose().popPose();
        Lighting.setupFor3DItems();
    }
    private int maxSelectable() {
        int xp = EmbodimentTableBlockEntity.getPlayerXp(minecraft.player);
        return Math.min(menu.getInputCount(), menu.getTarget() + xp / EmbodimentTableBlockEntity.XP_PER_BOTTLE);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // 1. Фон
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 2. Книга
        renderBook(g, partialTick);

        // 3. Стрелка: заполняется сверху вниз
        int h = Mth.ceil(22 * menu.getProgress() / (float) EmbodimentTableBlockEntity.TICKS_PER_BOTTLE);
        if (h > 0) g.blit(TEXTURE, leftPos + 80, topPos + 39, 176, 0, 16, h);

        // 4. Ползунок: тёмный, если нечего выбирать
        int max = maxSelectable();
        int target = menu.getTarget();
        boolean enabled = !menu.isRunning() && (max > 0 || target > 0);
        float frac = target <= 0 ? 0f : Math.min(1f, target / (float) Math.max(max, 1));
        int hx = leftPos + RAIL_X + Math.round(frac * (RAIL_W - HANDLE_W));
        g.blit(TEXTURE, hx, topPos + RAIL_Y, 192, enabled ? 0 : 6, HANDLE_W, HANDLE_H);

        // 5. Число в красной рамке
        Component text = Component.literal("×" + target);
        int tx = leftPos + 14 + (38 - font.width(text)) / 2;
        g.drawString(font, text, tx, topPos + 47, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (isOverRail(mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            if (maxSelectable() <= 0 && menu.getTarget() <= 0) {
                lines.add(Component.translatable("gui.damagecore.embodiment_table.unavailable"));
            } else {
                lines.add(Component.translatable("gui.damagecore.embodiment_table.selected", menu.getTarget()));
                lines.add(Component.translatable("gui.damagecore.embodiment_table.cost",
                        menu.getTarget() * EmbodimentTableBlockEntity.XP_PER_BOTTLE));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private boolean isOverRail(double mx, double my) {
        return mx >= leftPos + 14 && mx < leftPos + 52 && my >= topPos + 55 && my < topPos + 65;
    }

    private void updateFromMouse(double mx) {
        int max = maxSelectable();
        double f = (mx - (leftPos + RAIL_X) - HANDLE_W / 2.0) / (RAIL_W - HANDLE_W);
        int n = Mth.clamp((int) Math.round(f * max), 0, max);
        if (n != lastSent) {
            lastSent = n;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, n);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && !menu.isRunning() && isOverRail(mx, my) && maxSelectable() > 0) {
            dragging = true;
            lastSent = -1;
            updateFromMouse(mx);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            updateFromMouse(mx);
            // Страницы листаются пропорционально движению мыши; направление зависит от стороны
            flipT += (float) dx * 0.12F;
            flipT = Mth.clamp(flipT, flip - 3.0F, flip + 3.0F);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override
    protected void init() {
        super.init();
        this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));

        this.toggleButton = addRenderableWidget(
                Button.builder(Component.translatable("gui.damagecore.embodiment_table.start"),
                                b -> minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, EmbodimentTableMenu.BUTTON_START))
                        .bounds(leftPos + 14, topPos + 70, 38, 20)
                        .build());
    }
    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mx, my, button);
    }
}
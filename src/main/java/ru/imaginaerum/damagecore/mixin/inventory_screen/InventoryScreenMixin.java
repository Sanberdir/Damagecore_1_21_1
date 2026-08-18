package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.skill_tree.*;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.Render;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.tabs.SideTabsRenderer;
import ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer.StatsPanelRenderer;
import ru.imaginaerum.damagecore.library_stats.StatChangePacket;
import ru.imaginaerum.damagecore.library_stats.StatsType;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;
import ru.imaginaerum.damagecore.mixin.ScreenInvoker;

/**
 * Миксин InventoryScreen.
 *
 * Вся отрисовка панели статов и боковых вкладок вынесена в
 * {@link StatsPanelRenderer}. Здесь остаются только: состояние UI
 * (скролл/драг/активная вкладка), обработка кликов/init и позиционирование кнопок.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements ISkillTreeAccessor {

    private static final ResourceLocation SKILL_TREE_BUTTON =
            ResourceLocation.fromNamespaceAndPath("damagecore", "skill_tree_button"); // без .png и без textures/gui/

    private static final ResourceLocation SKILL_TREE_BUTTON_HOVER =
            ResourceLocation.fromNamespaceAndPath("damagecore", "skill_tree_button_hover"); // если сделали отдельный hover

    @Unique private ImageButton damagecore$recipeButton;
    @Unique private ImageButton damagecore$skillTreeButton;
    @Unique private static final int BUTTON_WIDTH  = 20;
    @Unique private static final int BUTTON_HEIGHT = 18;
    @Unique private boolean damageBookVisible = false;
    @Unique private boolean skillTreeVisible  = false;
    @Unique private static final int TAB_WIDTH             = DamageBookRenderer.TAB_WIDTH;
    @Unique private static final int RIGHT_INTERFACE_WIDTH = 289;
    @Unique private int recipeButtonOffsetX = 0;
    @Unique private int recipeButtonOffsetY = 0;
    @Unique private int selectedSmall       = 0;

    @Unique private int     damagecore$stripOffsetY  = 0;
    @Unique private boolean damagecore$draggingStrip = false;
    @Unique private double  damagecore$dragMouseY0   = 0;
    @Unique private int     damagecore$dragStripY0   = 0;

    @Unique private int damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;

    // -------------------------------------------------------------------------
    @Unique
    @Override
    public void damagecore$scrollList(double delta) {
        damagecore$stripOffsetY = Math.max(0,
                Math.min(StatsPanelRenderer.STRIP_DRAG_RANGE,
                        damagecore$stripOffsetY - (int) (delta * StatsPanelRenderer.PLUS_STEP)));
    }

    // -------------------------------------------------------------------------
    // renderBg — позиционирование кнопок + делегирование отрисовки панели
    // -------------------------------------------------------------------------
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void damagecore$replaceVanillaInventoryBg(GuiGraphics gui, float partialTick,
                                                      int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int leftPos = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        if (this.damagecore$recipeButton != null && this.damagecore$skillTreeButton != null) {
            int newRecipeX = leftPos + this.recipeButtonOffsetX;
            int newRecipeY = topPos  + this.recipeButtonOffsetY;
            this.damagecore$recipeButton.setPosition(newRecipeX, newRecipeY);
            int skillX = newRecipeX + this.damagecore$recipeButton.getWidth() + 2;
            this.damagecore$skillTreeButton.setPosition(skillX, newRecipeY);

        }

        if (!this.skillTreeVisible) return;

        StatsPanelRenderer.renderAll(gui, leftPos, topPos, mouseX, mouseY,
                damagecore$stripOffsetY, damagecore$activeSideTab, partialTick);

        // Ручная отрисовка модели игрока, т.к. ванильный renderBg отменён ниже
        // и стандартный вызов renderEntityInInventoryFollowsMouse (внутри ванильного renderBg) не происходит.
        if (Minecraft.getInstance().player != null) {
            int dollX1 = leftPos + 26;   // левая граница области под модель — ПОДСТАВЬТЕ СВОЮ
            int dollY1 = topPos + 8;     // верхняя граница — ПОДСТАВЬТЕ СВОЮ
            int dollX2 = leftPos + 76;   // правая граница — ПОДСТАВЬТЕ СВОЮ
            int dollY2 = topPos + 78;    // нижняя граница — ПОДСТАВЬТЕ СВОЮ
            int dollSize = 30;           // масштаб модели

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gui,
                    dollX1, dollY1, dollX2, dollY2,
                    dollSize,
                    (float) dollSize,
                    (float) (dollX1 + (dollX2 - dollX1) / 2) - mouseX,
                    (float) (dollY1 + (dollY2 - dollY1) / 2 - dollSize) - mouseY,
                    Minecraft.getInstance().player);
        }

        ci.cancel();
    }

    // Рисуем доп. фрагмент текстуры ПОСЛЕ ванильного фона (иначе он будет перекрыт,
    // т.к. при закрытом дереве умений renderBg не отменяется и ванильный код рисуется
    // уже после нашей HEAD-инъекции).
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void damagecore$renderExtraPanelOnTop(GuiGraphics gui, float partialTick,
                                                  int mouseX, int mouseY, CallbackInfo ci) {
        if (this.skillTreeVisible) return; // при открытой панели уже отрисовано в renderAll

        InventoryScreen screen = (InventoryScreen) (Object) this;
        int leftPos = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        StatsPanelRenderer.renderExtraPanel(gui, leftPos, topPos);
    }

    // -------------------------------------------------------------------------
    // renderLabels
    // -------------------------------------------------------------------------
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void damagecore$hideLabels(GuiGraphics gui, int mouseX, int mouseY, CallbackInfo ci) {
        if (!this.skillTreeVisible) return;
        ci.cancel();
    }

    // -------------------------------------------------------------------------
    // render TAIL — правая панель (skill tree / side tabs)
    // -------------------------------------------------------------------------
    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderAll(GuiGraphics gui, int mouseX, int mouseY,
                                      float partialTicks, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        if (damagecore$draggingStrip) {
            int delta = (int) (mouseY - damagecore$dragMouseY0);
            damagecore$stripOffsetY = Math.max(0,
                    Math.min(StatsPanelRenderer.STRIP_DRAG_RANGE, damagecore$dragStripY0 + delta));
        }

        if (this.skillTreeVisible) {
            int panelScreenX = guiLeft + imageWidth + 2;
            int panelScreenY = guiTop;
            int tabX         = guiLeft + imageWidth;
            boolean sideTabActive = damagecore$activeSideTab != SideTabsRenderer.TAB_NONE;

            DamageBookRenderer.renderRightInterface(gui, screen, tabX, guiTop, mouseX, mouseY, sideTabActive);

            if (sideTabActive) {
                ItemStack previewArmorStack = ItemStack.EMPTY;

                if (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) {
                    Slot hoveredSlot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
                    if (hoveredSlot != null && Minecraft.getInstance().player != null
                            && hoveredSlot.container == Minecraft.getInstance().player.getInventory()) {

                        int slotIndex = hoveredSlot.getContainerSlot();
                        // основной инвентарь + хотбар: 0..35. Слоты брони (36..39) и офф-хенд (40) — исключаем.
                        if (slotIndex >= 0 && slotIndex <= 35) {
                            ItemStack stack = hoveredSlot.getItem();
                            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
                                previewArmorStack = stack;
                            }
                        }
                    }
                }

                SideTabsRenderer.render(gui, panelScreenX, panelScreenY,
                        damagecore$activeSideTab, mouseX, mouseY, previewArmorStack);
            } else {
                SkillTreeRenderer.mouseDragged(mouseX, mouseY, 0, panelScreenX, panelScreenY);
                Render.currentHoveredNode = Render.getHoveredNodeUnderMouse(mouseX, mouseY);
            }

            StatsPanelRenderer.renderSideTabIcons(gui, guiLeft, guiTop, damagecore$activeSideTab);
        }
    }

    // -------------------------------------------------------------------------
    // mouseClicked — боковые вкладки
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$sideTabsClick(double mouseX, double mouseY, int button,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible || button != 0) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;
        int leftPos = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        // Вкладка броня
        if (mouseX >= leftPos + 466 && mouseX < leftPos + 491
                && mouseY >= topPos + 4 && mouseY < topPos + 32) {
            if (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) {
                damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
                DamageBookRenderer.setBottomTab(0);
                SkillTreeRenderer.setActiveTree(0);
            } else {
                damagecore$activeSideTab = SideTabsRenderer.TAB_ARMOR;
                DamageBookRenderer.setBottomTab(Integer.MAX_VALUE);
                SkillTreeRenderer.resetTreePosition();
            }
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
            return;
        }

        // Вкладка эффекты
        if (mouseX >= leftPos + 466 && mouseX < leftPos + 491
                && mouseY >= topPos + 33 && mouseY < topPos + 61) {
            if (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) {
                damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
                DamageBookRenderer.setBottomTab(0);
                SkillTreeRenderer.setActiveTree(0);
            } else {
                damagecore$activeSideTab = SideTabsRenderer.TAB_POTION;
                DamageBookRenderer.setBottomTab(Integer.MAX_VALUE);
                SkillTreeRenderer.resetTreePosition();
            }
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
        }
    }

    // -------------------------------------------------------------------------
    // mouseClicked — кнопки статов
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$statsButtonsClick(double mouseX, double mouseY, int button,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible || button != 0) return;
        if (Minecraft.getInstance().player == null) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;

        // В 1.21.1 Mojang маппингах эти поля доступны напрямую у контейнер-экранов,
        // так как они имеют модификатор protected. Аксессор больше не нужен.
        int leftPos = screen.getGuiLeft(); // или screen.leftPos в зависимости от вашей конфигурации миксина (в InventoryScreen они видны)
        int topPos  = screen.getGuiTop();  // или screen.topPos

        // Если выше выдает ошибку доступа, можно использовать ванильные геттеры:
        // int leftPos = screen.getGuiLeft();
        // int topPos = screen.getGuiTop();

        int scrollPx = StatsPanelRenderer.STRIP_DRAG_RANGE > 0
                ? (damagecore$stripOffsetY * StatsPanelRenderer.SCROLL_MAX_PX) / StatsPanelRenderer.STRIP_DRAG_RANGE
                : 0;

        for (int i = 0; i < StatsPanelRenderer.ROWS_TOTAL; i++) {
            StatsType statType = StatsType.values()[i];
            int minusScreenX = leftPos + StatsPanelRenderer.MINUS_X;
            int minusScreenY = topPos  + StatsPanelRenderer.MINUS_Y + i * StatsPanelRenderer.MINUS_STEP - scrollPx;
            int plusScreenX  = leftPos + StatsPanelRenderer.PLUS_X;
            int plusScreenY  = topPos  + StatsPanelRenderer.PLUS_Y  + i * StatsPanelRenderer.PLUS_STEP  - scrollPx;

            if (mouseX >= plusScreenX && mouseX < plusScreenX + 11 / 1.2f
                    && mouseY >= plusScreenY && mouseY < plusScreenY + 7 / 1.2f) {

                // ИСПРАВЛЕНО: Новый синтаксис отправки пакетов на сервер для NeoForge 1.21.1
                PacketDistributor.sendToServer(new StatChangePacket(statType, true));

                cir.setReturnValue(true);
                return;
            }
            if (mouseX >= minusScreenX && mouseX < minusScreenX + 11 / 1.2f
                    && mouseY >= minusScreenY && mouseY < minusScreenY + 7 / 1.2f) {

                // ИСПРАВЛЕНО: Новый синтаксис отправки пакетов на сервер для NeoForge 1.21.1
                PacketDistributor.sendToServer(new StatChangePacket(statType, false));

                cir.setReturnValue(true);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // mouseClicked — полоска скролла
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$stripMouseClicked(double mouseX, double mouseY, int button,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible || button != 0) return;
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int leftPos = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        int sx = leftPos + 165;
        int sy = topPos  + StatsPanelRenderer.STRIP_Y_MIN_OFF + damagecore$stripOffsetY;

        if (mouseX >= sx && mouseX < sx + 5 &&
                mouseY >= sy && mouseY < sy + 15) {
            damagecore$draggingStrip = true;
            damagecore$dragMouseY0   = mouseY;
            damagecore$dragStripY0   = damagecore$stripOffsetY;
            cir.setReturnValue(true);
        }
    }

    // -------------------------------------------------------------------------
    // mouseReleased
    // -------------------------------------------------------------------------
    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void damagecore$stripMouseReleased(double mouseX, double mouseY, int button,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && damagecore$draggingStrip)
            damagecore$draggingStrip = false;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void damagecore$skillTree_mouseReleased(double mouseX, double mouseY, int button,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;
        boolean consumed = SkillTreeRenderer.mouseReleased((int) mouseX, (int) mouseY, button);
        if (button == 0) {
            Render.currentHoveredNode = null;
            Render.mousePressTime = 0L;
        }
        if (consumed) cir.setReturnValue(true);
    }

    // -------------------------------------------------------------------------
    // mouseClicked — bottom tabs, skill tree press
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$bottomTabsClick(double mouseX, double mouseY, int button,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelLeft = guiLeft + imageWidth + 2;
        int panelTop  = guiTop;
        int PANEL_W   = 289;
        int TAB_W     = 28;
        int bottomY   = panelTop + 163;
        int topY      = panelTop - 25;

        if (DamageBookRenderer.handleArrowClick(mouseX, mouseY, panelLeft, panelTop, PANEL_W)) {
            cir.setReturnValue(true); return;
        }
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, bottomY, true)) {
            damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
            cir.setReturnValue(true); return;
        }
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, topY, false)) {
            damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$closeDamageBookWhenRecipeOpen(double mouseX, double mouseY, int button,
                                                          CallbackInfoReturnable<Boolean> cir) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        if (screen.getRecipeBookComponent().isVisible()) {
            boolean changed = false;
            if (this.damageBookVisible) { this.damageBookVisible = false; changed = true; }
            if (this.skillTreeVisible)  { this.skillTreeVisible  = false; changed = true; }
            if (changed) damagecore$updateInventoryPosition(screen);
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$handleSmallClick(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!this.damageBookVisible) return;
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();
        this.selectedSmall = DamageBookInputHandler.handleSmallTabsClick(
                mouseX, mouseY, screen, this.selectedSmall,
                guiLeft - TAB_WIDTH, guiTop);
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"), cancellable = true)
    private void damagecore$skillTree_mousePressed(double mouseX, double mouseY, int button,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;
        if (damagecore$activeSideTab != SideTabsRenderer.TAB_NONE) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelScreenX = guiLeft + imageWidth + 2;
        int panelScreenY = guiTop;

        boolean consumed = SkillTreeRenderer.mousePressed(
                (int) mouseX, (int) mouseY, button, panelScreenX, panelScreenY);

        if (button == 0) {
            SkillTreeNode hovered = Render.getHoveredNodeUnderMouse((int) mouseX, (int) mouseY);
            if (hovered != null && !hovered.isMaxLevel() && !hovered.locked) {
                Render.currentHoveredNode = hovered;
                Render.mousePressTime = System.currentTimeMillis();
            } else {
                Render.currentHoveredNode = null;
                Render.mousePressTime = 0L;
            }
        }
        if (consumed) cir.setReturnValue(true);
    }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------
    @Inject(method = "init", at = @At("HEAD"))
    private void damagecore$resetSyncFlagOnInit(CallbackInfo ci) {
        ClientSyncState.syncRequested = false;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void damagecore$init(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        SkillTreeRenderer.loadAllTrees("skill_tree");

        // Получаем координаты левого и верхнего угла экрана напрямую через ванильные методы
        int leftPos = screen.getGuiLeft();
        int topPos  = screen.getGuiTop();

        for (var child : screen.children()) {
            if (child instanceof ImageButton btn
                    && btn.getWidth()  == BUTTON_WIDTH
                    && btn.getHeight() == BUTTON_HEIGHT) {
                this.damagecore$recipeButton = btn;
                this.recipeButtonOffsetX = btn.getX() - leftPos;
                this.recipeButtonOffsetY = btn.getY() - topPos;

                break;
            }
        }

        // ИСПРАВЛЕНО: В 1.21.1 для ImageButton нужны WidgetSprites (для текстур кнопки)
        // Укажите текстуру обычной кнопки и текстуру при наведении (или одну и ту же, если она не меняется)
        WidgetSprites buttonSprites = new WidgetSprites(SKILL_TREE_BUTTON, SKILL_TREE_BUTTON_HOVER);

        this.damagecore$skillTreeButton = new ImageButton(
                0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, buttonSprites, btn -> {
            InventoryScreen s = (InventoryScreen) (Object) this;
            this.skillTreeVisible = !this.skillTreeVisible;
            if (this.skillTreeVisible) {
                if (s.getRecipeBookComponent().isVisible())
                    s.getRecipeBookComponent().toggleVisibility();
                this.damageBookVisible = false;
                int currentTab = DamageBookRenderer.selectedBottomTab;
                if (currentTab < SkillTreeRenderer.getTotalTrees())
                    SkillTreeRenderer.setActiveTree(currentTab);
            } else {
                SkillTreeRenderer.resetTreePosition();
                damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
            }
            damagecore$updateInventoryPosition(s);
        });

        ((ScreenInvoker) (Object) this).damagecore$addRenderableWidget(this.damagecore$skillTreeButton);

        // ИСПРАВЛЕНО: this не наследует Screen в исходниках миксина (только через байткод-мердж),
        // поэтому addRenderableWidget (protected у Screen) недоступен напрямую — зовём через
        // @Invoker-интерфейс ScreenInvoker, как и остальные protected-члены в этом файле.
        ((ScreenInvoker) (Object) this).damagecore$addRenderableWidget(this.damagecore$skillTreeButton);

        if (Minecraft.getInstance().player != null && !ClientSyncState.syncRequested) {
            // ИСПРАВЛЕНО: Новый синтаксис отправки пакетов NeoForge 1.21.1
            PacketDistributor.sendToServer(new RequestFullSyncPacket());
            ClientSyncState.syncRequested = true;
        }
    }


    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------
    @Override
    public boolean damagecore$isSkillTreeVisible() {
        return this.skillTreeVisible;
    }

    @Unique
    private boolean clickTab(double mx, double my, int x, int y, int w, int globalId) {
        if (!inside(mx, my, x, y - 3, w, 32)) return false;
        if (DamageBookRenderer.selectedBottomTab != globalId) {
            DamageBookRenderer.setBottomTab(globalId);
            SkillTreeRenderer.setActiveTree(globalId);
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        return true;
    }

    @Unique
    private boolean handleRowClick(double mx, double my, int panelLeft, int panelW,
                                   int tabW, int y, boolean bottom) {
        int rowBase = bottom ? 0 : DamageBookRenderer.TABS_PER_ROW;

        int globalLeft = DamageBookRenderer.globalIdForSlot(rowBase);
        if (SkillTreeRenderer.hasTreeForTab(globalLeft))
            if (clickTab(mx, my, panelLeft, y, tabW, globalLeft)) return true;

        for (int i = 0; i < DamageBookRenderer.MIDDLE_TABS; i++) {
            int globalId = DamageBookRenderer.globalIdForSlot(rowBase + 1 + i);
            if (!SkillTreeRenderer.hasTreeForTab(globalId)) continue;
            int x = DamageBookRenderer.calcMiddleX(i, panelLeft, panelW, tabW);
            if (clickTab(mx, my, x, y, tabW, globalId)) return true;
        }

        int globalRight = DamageBookRenderer.globalIdForSlot(rowBase + DamageBookRenderer.TABS_PER_ROW - 1);
        return SkillTreeRenderer.hasTreeForTab(globalRight)
                && clickTab(mx, my, panelLeft + panelW - tabW, y, tabW, globalRight);
    }

    @Unique
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Unique
    private void damagecore$updateInventoryPosition(InventoryScreen screen) {
        DamageBookPositionHelper.updateInventoryPosition(
                screen, this.damageBookVisible, this.skillTreeVisible,
                TAB_WIDTH, RIGHT_INTERFACE_WIDTH);
    }

}
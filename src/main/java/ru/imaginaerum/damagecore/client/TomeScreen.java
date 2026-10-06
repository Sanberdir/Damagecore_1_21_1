package ru.imaginaerum.damagecore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TomeScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "damagecore", "textures/gui/book/tome_of_attaining_meanings.png");

    private static final int TEX_SIZE = 256;
    private static final int PAGE_U = 20;
    private static final int PAGE_V = 1;
    private static final int PAGE_W = 166 - 20; // 146
    private static final int PAGE_H = 181 - 1;  // 180

    private static final int TEXT_X = 36 - PAGE_U;        // 16
    private static final int TEXT_Y = 14 - PAGE_V;        // 13
    private static final int TEXT_W = 148 - 36;           // 112
    private static final int TEXT_H = 161 - 14;           // 147
    private static final int LINE_H = 9;
    private static final int MAX_LINES = TEXT_H / LINE_H; // 16
    private static final int TEXT_COLOR = 0x3B2A1A;

    /** Шаг книги: 1 - галактический текст, 2 - загадка 1, 5 - видение, 6 - видение + фраза после рейда. */
    private final int step;

    // Текст первого шага (генерируется при каждом открытии книги)
    private final String stepOneLeft;
    private final String stepOneRight;

    private List<FormattedCharSequence> leftLines = List.of();
    private List<FormattedCharSequence> rightLines = List.of();

    public TomeScreen(int step) {
        super(Component.empty());
        this.step = step;
        RandomSource random = RandomSource.create();
        this.stepOneLeft = GalacticText.random(random, 600);
        this.stepOneRight = GalacticText.random(random, 600);
    }

    // ---------- Шаги ----------

    private void buildStepOne() {
        List<FormattedCharSequence> all = new ArrayList<>();
        List<FormattedCharSequence> symbols =
                GalacticText.wrap(this.font, stepOneLeft, TEXT_W, MAX_LINES);
        all.addAll(symbols);
        for (int i = symbols.size(); i < MAX_LINES; i++) all.add(FormattedCharSequence.EMPTY);
        all.addAll(this.font.split(Component.translatable("message.damagecore.tome.open"), TEXT_W));
        this.allLines = all;
        showSpread();
    }

    /** Добивает страницу пустыми строками, чтобы следующий текст начался с нового разворота. */
    private static void padPage(List<FormattedCharSequence> dst, List<FormattedCharSequence> src) {
        dst.addAll(src);
        for (int i = src.size(); i < MAX_LINES; i++) dst.add(FormattedCharSequence.EMPTY);
    }

    /** Загадка обычным шрифтом: сначала заполняется левая страница, остаток идёт на правую. */
    private static final int LINES_PER_SPREAD = MAX_LINES * 2;

    private List<FormattedCharSequence> allLines = List.of();
    private int spread = 0;

    private void showSpread() {
        int size = allLines.size();
        int from = Math.min(spread * LINES_PER_SPREAD, size);
        int mid = Math.min(from + MAX_LINES, size);
        int end = Math.min(from + LINES_PER_SPREAD, size);
        this.leftLines = allLines.subList(from, mid);
        this.rightLines = allLines.subList(mid, end);
    }

    private boolean turnPage(int delta) {
        int maxSpread = Math.max(0, (allLines.size() - 1) / LINES_PER_SPREAD);
        int next = Math.max(0, Math.min(maxSpread, spread + delta));
        if (next == spread) return false;
        spread = next;
        showSpread();
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT && turnPage(1)) return true;
        if (keyCode == GLFW.GLFW_KEY_LEFT && turnPage(-1)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && turnPage(mouseX < this.width / 2.0 ? -1 : 1)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------- Жизненный цикл ----------

    /** Текст по одному или нескольким ключам локализации (через перенос строки), с листанием разворотов. */
    private void buildRiddle(String... keys) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) text.append("\n");
            text.append(Component.translatable(keys[i]));
        }
        this.allLines = this.font.split(text, TEXT_W);
        showSpread();
    }

    /** Ключи prefix1 ... prefixN. */
    private static void addSeq(List<String> keys, String prefix, int n) {
        for (int i = 1; i <= n; i++) keys.add(prefix + i);
    }

    private static String rescueKey() {
        var p = Minecraft.getInstance().player;
        int r = p == null ? 1 : p.getData(ModAttachments.TOME_RESCUE);
        return switch (r) {
            case 2 -> "message.damagecore.tome.rescue.death";
            case 3 -> "message.damagecore.tome.rescue.kill";
            default -> "message.damagecore.tome.rescue.success";
        };
    }

    @Override
    protected void init() {
        List<String> k = new ArrayList<>();
        switch (step) {
            case 1 -> { buildStepOne(); return; }
            case 2 -> addSeq(k, "message.damagecore.tome.step_1.", 8);
            case 3 -> addSeq(k, "message.damagecore.tome.step_1.", 8);   // глава шага 2
            case 4 -> k.add("message.damagecore.tome.finish");
            case 5 -> k.add("book.damagecore.tome.riddle_3");
            case 6 -> k.add("message.damagecore.tome.raid");
            case 8 -> k.add("message.damagecore.tome.cure");
            case 9 -> addSeq(k, "book.damagecore.tome.riddle_5_", 10);
            case 10 -> addSeq(k, "book.damagecore.tome.riddle_5_", 10);  // глава шага 9
            case 11 -> {
                k.add(rescueKey());
                addSeq(k, "book.damagecore.tome.riddle_6_", 10);
            }
            default -> {
                this.leftLines = List.of();
                this.rightLines = List.of();
                return;
            }
        }
        buildRiddle(k.toArray(new String[0]));
    }

    // ---------- Отрисовка ----------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int top = (this.height - PAGE_H) / 2;

        // Правая страница
        g.blit(TEXTURE, centerX, top, PAGE_U, PAGE_V, PAGE_W, PAGE_H, TEX_SIZE, TEX_SIZE);

        // Левая страница (зеркально)
        g.flush();
        drawMirrored(g, centerX - PAGE_W, top);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int top = (this.height - PAGE_H) / 2;

        drawLines(g, leftLines, centerX - (TEXT_X + TEXT_W), top + TEXT_Y);
        drawLines(g, rightLines, centerX + TEXT_X, top + TEXT_Y);
    }

    private void drawLines(GuiGraphics g, List<FormattedCharSequence> lines, int x, int y) {
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(this.font, lines.get(i), x, y + i * LINE_H, TEXT_COLOR, false);
        }
    }

    private void drawMirrored(GuiGraphics g, int x, int y) {
        float u0 = (PAGE_U + PAGE_W) / (float) TEX_SIZE;
        float u1 = PAGE_U / (float) TEX_SIZE;
        float v0 = PAGE_V / (float) TEX_SIZE;
        float v1 = (PAGE_V + PAGE_H) / (float) TEX_SIZE;

        int x0 = x, x1 = x + PAGE_W;
        int y0 = y, y1 = y + PAGE_H;

        Matrix4f m = g.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();

        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, x0, y1, 0).setUv(u0, v1);
        b.addVertex(m, x1, y1, 0).setUv(u1, v1);
        b.addVertex(m, x1, y0, 0).setUv(u1, v0);
        b.addVertex(m, x0, y0, 0).setUv(u0, v0);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
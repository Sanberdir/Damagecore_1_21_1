package ru.imaginaerum.damagecore.Init.blocks.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Init.blocks.custom.BlockConfigNetwork;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlockConfigScreen extends Screen {
    private static final int SLOT = 18, COLS = 9, ROWS = 8;

    private record Entry(String id, ItemStack icon, Component name) {}

    /** Одна сетка иконок с прокруткой и множественным выбором. */
    private static class Grid {
        final List<Entry> all;
        List<Entry> shown;
        final Set<String> selected;
        int x, y, scroll;

        Grid(List<Entry> all, List<String> selected) {
            this.all = all;
            this.shown = all;
            this.selected = new LinkedHashSet<>(selected);
        }

        void filter(String q) {
            q = q.toLowerCase(Locale.ROOT);
            List<Entry> r = new ArrayList<>();
            for (Entry e : all) {
                if (q.isEmpty() || e.id.contains(q)
                        || e.name.getString().toLowerCase(Locale.ROOT).contains(q)) r.add(e);
            }
            shown = r;
            scroll = 0;
        }

        int maxScroll() { return Math.max(0, (shown.size() + COLS - 1) / COLS - ROWS); }

        Entry at(double mx, double my) {
            if (mx < x || my < y) return null;
            int cx = (int) (mx - x) / SLOT, cy = (int) (my - y) / SLOT;
            if (cx >= COLS || cy >= ROWS) return null;
            int i = (scroll + cy) * COLS + cx;
            return i < shown.size() ? shown.get(i) : null;
        }

        boolean contains(double mx, double my) {
            return mx >= x && my >= y && mx < x + COLS * SLOT && my < y + ROWS * SLOT;
        }
    }

    private final BlockPos pos;
    private final Grid entities;
    private final Grid blocks;
    private EditBox search;

    public static void open(BlockPos pos, List<String> entityIds, List<String> blockIds) {
        Minecraft.getInstance().setScreen(new BlockConfigScreen(pos, entityIds, blockIds));
    }

    private BlockConfigScreen(BlockPos pos, List<String> entityIds, List<String> blockIds) {
        super(Component.translatable("gui.damagecore.config.title"));
        this.pos = pos;

        List<Entry> ents = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            SpawnEggItem egg = SpawnEggItem.byId(t);
            if (egg == null) continue;                       // иконка есть только у существ с яйцом
            ents.add(new Entry(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(),
                    new ItemStack(egg), t.getDescription()));
        }
        List<Entry> blks = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            Item item = b.asItem();
            if (item == Items.AIR) continue;
            blks.add(new Entry(BuiltInRegistries.BLOCK.getKey(b).toString(),
                    new ItemStack(item), b.getName()));
        }
        this.entities = new Grid(ents, entityIds);
        this.blocks = new Grid(blks, blockIds);
    }

    @Override
    protected void init() {
        int gridW = COLS * SLOT;
        int gap = 20;
        int left = (this.width - (gridW * 2 + gap)) / 2;
        int top = (this.height - (ROWS * SLOT + 70)) / 2 + 40;

        entities.x = left;
        blocks.x = left + gridW + gap;
        entities.y = top;
        blocks.y = top;

        search = new EditBox(this.font, left, top - 28, gridW * 2 + gap, 16,
                Component.translatable("gui.damagecore.config.search"));
        search.setResponder(s -> { entities.filter(s); blocks.filter(s); });
        addRenderableWidget(search);

        int by = top + ROWS * SLOT + 8;

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.damagecore.config.reset_entities"),
                        b -> entities.selected.clear())
                .bounds(entities.x, by, 110, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.damagecore.config.save"), b -> {
                    PacketDistributor.sendToServer(new BlockConfigNetwork.SaveConfig(
                            pos,
                            new ArrayList<>(entities.selected),
                            new ArrayList<>(blocks.selected)));
                    onClose();
                }).bounds(this.width / 2 - 50, by, 100, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.damagecore.config.reset_blocks"),
                        b -> blocks.selected.clear())
                .bounds(blocks.x + gridW - 110, by, 110, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width / 2, entities.y - 44, 0xFFFFFF);

        g.drawString(this.font, Component.translatable("gui.damagecore.config.entities"),
                entities.x, entities.y - 10, 0xAAAAAA);
        g.drawString(this.font, Component.literal(String.valueOf(entities.selected.size())),
                entities.x + 80, entities.y - 10, 0x55FF55);

        g.drawString(this.font, Component.translatable("gui.damagecore.config.blocks"),
                blocks.x, blocks.y - 10, 0xAAAAAA);
        g.drawString(this.font, Component.literal(String.valueOf(blocks.selected.size())),
                blocks.x + 80, blocks.y - 10, 0x55FF55);

        Entry hover = renderGrid(g, entities, mx, my);
        Entry hover2 = renderGrid(g, blocks, mx, my);
        Entry h = hover != null ? hover : hover2;
        if (h != null) {
            g.renderTooltip(this.font,
                    Component.literal(h.name.getString() + " (" + h.id + ")"), mx, my);
        }
    }

    private Entry renderGrid(GuiGraphics g, Grid grid, int mx, int my) {
        g.fill(grid.x - 1, grid.y - 1, grid.x + COLS * SLOT + 1, grid.y + ROWS * SLOT + 1, 0xAA000000);
        Entry hovered = grid.at(mx, my);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = (grid.scroll + r) * COLS + c;
                if (i >= grid.shown.size()) return hovered;
                Entry e = grid.shown.get(i);
                int sx = grid.x + c * SLOT, sy = grid.y + r * SLOT;
                if (grid.selected.contains(e.id)) {
                    g.fill(sx, sy, sx + SLOT, sy + SLOT, 0x8055FF55);
                } else if (e == hovered) {
                    g.fill(sx, sy, sx + SLOT, sy + SLOT, 0x40FFFFFF);
                }
                g.renderItem(e.icon, sx + 1, sy + 1);
            }
        }
        return hovered;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Grid grid : new Grid[]{entities, blocks}) {
                Entry e = grid.at(mx, my);
                if (e != null) {
                    if (!grid.selected.remove(e.id)) grid.selected.add(e.id);  // добавить или убрать
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        for (Grid grid : new Grid[]{entities, blocks}) {
            if (grid.contains(mx, my)) {
                grid.scroll = Math.max(0, Math.min(grid.maxScroll(),
                        grid.scroll - (int) Math.signum(sy)));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
package ru.imaginaerum.damagecore.structure_processors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class RescueScreen extends Screen {
    private final int entityId;

    public RescueScreen(int entityId) {
        super(Component.translatable("gui.damagecore.rescue.title"));
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        int cx = this.width / 2, cy = this.height / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.damagecore.rescue.follow"),
                b -> send(true)).bounds(cx - 100, cy - 24, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.damagecore.rescue.stay"),
                b -> send(false)).bounds(cx - 100, cy + 4, 200, 20).build());
    }

    private void send(boolean follow) {
        PacketDistributor.sendToServer(new RescueNetwork.Command(entityId, follow));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 48, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
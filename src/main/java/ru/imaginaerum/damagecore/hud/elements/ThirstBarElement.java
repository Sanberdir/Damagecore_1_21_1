package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import ru.imaginaerum.damagecore.Config;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.hud.net.ThirstDamagePacket;

public class ThirstBarElement {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("damagecore", "textures/hud/damage_core_hud.png");

    private static final int TEXTURE_W = 160, TEXTURE_H = 208;

    private static final int ICON_SRC_X = 16, ICON_SRC_Y = 109, ICON_W = 18, ICON_H = 20;
    private static final int FILL_SRC_X = 20, FILL_SRC_Y = 109, FILL_W = 16, FILL_H = 10;
    private static final int EMPTY_SRC_X = 0, EMPTY_SRC_Y = 156, EMPTY_W = 16, EMPTY_H = 10;
    private static final int BAR_OFFSET_X = 2, BAR_OFFSET_Y = 0;

    private static final int DAMAGE_INTERVAL_BASE = 80; // 4 сек на HARD/по умолчанию

    public static float thirst = 20f;
    public static final float MAX_THIRST = 20f;

    private static int tickTimer = 0, damageTimer = 0;
    private static float drainAccum = 0f;

    public static void tick(Minecraft mc) {
        if (mc.player == null || !Config.enableThirst) return;

        if (mc.player.level().getDifficulty() == Difficulty.PEACEFUL) {
            thirst = Math.min(MAX_THIRST, thirst + 1f);
            damageTimer = 0;
            return;
        }

        Difficulty difficulty = mc.player.level().getDifficulty();

        tickTimer++;
        if (tickTimer >= 20) {
            tickTimer = 0;

            boolean sprinting = mc.player.isSprinting(), moving = mc.player.zza != 0 || mc.player.xxa != 0;
            float drain = 0f;

            if (sprinting) {
                if (Math.random() < 0.5) drain = 0.05f;
            } else if (moving) {
                if (Math.random() < 0.5) drain = 0.025f;
            } else {
                drain = 1f / 450f;
            }

            float diffMult = switch (difficulty) {
                case EASY -> 1f / (1.5f * 1.5f);
                case NORMAL -> 1f / 1.5f;
                default -> 1f;
            };

            drainAccum += drain * diffMult * Config.thirstDrainMultiplier;

            while (drainAccum >= 1f) {
                drainAccum -= 1f;
                thirst = Math.max(0f, thirst - 1f);
            }
        }

        // Частота урона от обезвоживания зависит от сложности:
        // HARD — база (4 сек), NORMAL — в 1.5 раза реже, EASY — ещё в 1.5 раза реже
        float damageIntervalMult = switch (difficulty) {
            case EASY -> 1.5f * 1.5f;
            case NORMAL -> 1.5f;
            default -> 1f;
        };
        int damageInterval = Math.round(DAMAGE_INTERVAL_BASE * damageIntervalMult);

        if (thirst <= 0f) {
            if (++damageTimer >= damageInterval) {
                damageTimer = 0;
                ModNetwork.sendToServer(new ThirstDamagePacket());            }
        } else {
            damageTimer = 0;
        }
    }

    public static void drink(float amount) {
        thirst = Math.min(MAX_THIRST, thirst + amount);
    }

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null || !Config.enableThirst) return;

        int screenH = mc.getWindow().getGuiScaledHeight(), screenW = mc.getWindow().getGuiScaledWidth();
        int hotbarLeft = screenW / 2 - 50, heartsY = screenH - 49;
        int screenX = hotbarLeft - 38 + 18 - 3 - 4, screenY = heartsY + 7;

        gui.blit(TEXTURE, screenX, screenY, ICON_SRC_X, ICON_SRC_Y - 1, ICON_W, ICON_H, TEXTURE_W, TEXTURE_H);

        int barX = screenX + BAR_OFFSET_X, barY = screenY + BAR_OFFSET_Y;

        gui.blit(TEXTURE, barX, barY, EMPTY_SRC_X, EMPTY_SRC_Y, EMPTY_W, EMPTY_H, TEXTURE_W, TEXTURE_H);

        int fillH = Math.round(FILL_H * (thirst / MAX_THIRST));

        if (fillH > 0) {
            int cut = FILL_H - fillH;
            gui.blit(TEXTURE, barX + 2, barY + cut, FILL_SRC_X, FILL_SRC_Y + cut - 1, FILL_W, fillH, TEXTURE_W, TEXTURE_H);
        }
    }
}
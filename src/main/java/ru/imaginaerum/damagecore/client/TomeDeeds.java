package ru.imaginaerum.damagecore.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import ru.imaginaerum.damagecore.sounds.DCSoundEvents;

public final class TomeDeeds {
    public static final int MAX = 3;

    private TomeDeeds() {}

    /** Записывает поступок в сферу. true, если он засчитан. */
    public static boolean add(ServerPlayer p, boolean good) {
        if (p.getData(ModAttachments.TOME_STEP) != TomeLocator.SPHERE_STEP) return false;

        int g = p.getData(ModAttachments.TOME_GOOD);
        int b = p.getData(ModAttachments.TOME_BAD);
        if (g + b >= MAX) return false; // сфера заполнена

        if (good) p.setData(ModAttachments.TOME_GOOD, g + 1);
        else p.setData(ModAttachments.TOME_BAD, b + 1);

        p.playNotifySound(DCSoundEvents.LEARNING_SKILL.get(), SoundSource.PLAYERS, 1.0F, good ? 1.0F : 0.6F);

        if (g + b + 1 >= MAX) {
            p.sendSystemMessage(Component.translatable("message.damagecore.tome.sphere_full")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        return true;
    }

    /** Код текстуры: 0 - пустая сфера, 1..9 - см. override'ы в модели. */
    public static int stateCode(int g, int b) {
        return switch (g * 10 + b) {
            case 10 -> 1; // light_1
            case 20 -> 2; // light_2
            case 30 -> 3; // light_3
            case 1  -> 4; // dark_1
            case 2  -> 5; // dark_2
            case 3  -> 6; // dark_3
            case 11 -> 7; // light_dark
            case 21 -> 8; // light_2_dark
            case 12 -> 9; // light_dark_2
            default -> 0;
        };
    }
}
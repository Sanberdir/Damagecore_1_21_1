package ru.imaginaerum.damagecore.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;

import java.util.List;

public final class GalacticText {
    private static final ResourceLocation FONT = ResourceLocation.withDefaultNamespace("alt");

    private GalacticText() {}

    /** Случайная строка из латинских букв с пробелами (в шрифте alt отображается как галактический алфавит). */
    public static String random(RandomSource random, int length) {
        StringBuilder sb = new StringBuilder(length);
        int untilSpace = 2 + random.nextInt(6);
        for (int i = 0; i < length; i++) {
            if (untilSpace-- <= 0) {
                sb.append(' ');
                untilSpace = 2 + random.nextInt(6);
            } else {
                sb.append((char) ('a' + random.nextInt(26)));
            }
        }
        return sb.toString();
    }

    /** Разбивает текст галактическим шрифтом на строки заданной ширины и обрезает по maxLines. */
    public static List<FormattedCharSequence> wrap(Font font, String raw, int width, int maxLines) {
        Component c = Component.literal(raw).withStyle(Style.EMPTY.withFont(FONT));
        List<FormattedCharSequence> all = font.split(c, width);
        return all.size() > maxLines ? all.subList(0, maxLines) : all;
    }
}
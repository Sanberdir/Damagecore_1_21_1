package ru.imaginaerum.damagecore.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class TomeTitles {
    /** Сколько поступков нужно набрать, чтобы титул определился. */
    public static final int DEEDS_NEEDED = 3;

    // строка = исход (1 пленник выжил, 2 пошло не так, 3 ужасный поступок),
    // столбец = 3 - добрых поступков (свет, почти свет, почти тьма, тьма)
    private static final String[][] IDS = {
            {"guardian",      "defender",          "drifter",  "withering"},
            {"light_warrior", "seeker_of_justice", "vicious",  "lost"},
            {"penitent",      "traitor",           "hardened", "absolute_evil"}
    };

    private TomeTitles() {}

    @Nullable
    public static String idFor(int rescue, int good, int bad) {
        if (rescue < 1 || rescue > 3) return null;
        if (good + bad < DEEDS_NEEDED) return null;
        return IDS[rescue - 1][3 - Math.min(Math.max(good, 0), 3)];
    }

    public static ResourceLocation icon(String id) {
        return ResourceLocation.fromNamespaceAndPath("damagecore", "textures/gui/title/title_" + id + ".png");
    }

    public static int lightLevel(int good) {   // 0 = свет ... 3 = тьма
        return 3 - Math.min(Math.max(good, 0), 3);
    }
}
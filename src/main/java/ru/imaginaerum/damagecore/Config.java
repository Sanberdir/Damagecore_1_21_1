package ru.imaginaerum.damagecore;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber; // Важный импорт
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// Исправлено: аннотация пишется без "Mod.", а тип шины как EventBusSubscriber.Bus.MOD
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue SHOW_STAMINA_HUD = BUILDER
            .comment("Whether to show the stamina HUD overlay")
            .define("showStaminaHud", true);

    private static final ModConfigSpec.DoubleValue THIRST_DRAIN_MULTIPLIER = BUILDER
            .comment("Multiplier for thirst drain rate")
            .defineInRange("thirstDrainMultiplier", 1.0, 0.0, 10.0);

    private static final ModConfigSpec.BooleanValue ENABLE_THIRST = BUILDER
            .comment("Enable/disable thirst system")
            .define("enableThirst", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableThirst;
    public static boolean showStaminaHud;
    public static double thirstDrainMultiplier;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        showStaminaHud = SHOW_STAMINA_HUD.get();
        enableThirst = ENABLE_THIRST.get();
        thirstDrainMultiplier = THIRST_DRAIN_MULTIPLIER.get();
    }
}

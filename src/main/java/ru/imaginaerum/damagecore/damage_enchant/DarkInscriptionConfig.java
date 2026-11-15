package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.world.level.Level;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.Map;

public final class DarkInscriptionConfig {
    /** Тип урона, который добавляется ночью */
    public static final DamageType NIGHT_DAMAGE_TYPE = DamageType.NECROTIC;
    /** Ночью + за уровень, днём - за уровень: 0.5, 1.0, 1.5, 2.0, 2.5 */
    public static final float DAMAGE_PER_LEVEL = 0.5F;
    /** Дополнительный шанс крита ночью (не зависит от уровня) */
    public static final float NIGHT_CRIT_CHANCE = 0.10F;
    /** Прибавка к множителю крита за уровень */
    public static final float CRIT_MULT_PER_LEVEL = 0.2F;
    /** Базовый множитель крита (ванильный) */
    public static final float BASE_CRIT_MULT = 1.5F;

    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;

    public static boolean isNight(Level level) {
        if (level.dimensionType().hasFixedTime()) return false;
        long t = level.getDayTime() % 24000L;
        return t >= NIGHT_START && t < NIGHT_END;
    }
    public static DamageType getMaxType(Map<DamageType, Double> map) {
        DamageType max = null;
        double maxValue = 0.0;
        for (Map.Entry<DamageType, Double> e : map.entrySet()) {
            if (e.getValue() > maxValue) {
                maxValue = e.getValue();
                max = e.getKey();
            }
        }
        return max;
    }
    public static boolean isDay(Level level) {
        return !level.dimensionType().hasFixedTime() && !isNight(level);
    }

    private DarkInscriptionConfig() {}
}
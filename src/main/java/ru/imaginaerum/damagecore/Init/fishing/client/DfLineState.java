package ru.imaginaerum.damagecore.Init.fishing.client;

import java.util.HashMap;
import java.util.Map;

/** Состояние провиса лески для каждого крючка (вынесено из миксина). */
public final class DfLineState {
    public float slack;
    public long ms;
    public boolean landed;
    public float slackVel;     // скорость изменения провиса
    public float flightSpeed;  // последняя скорость крючка в полёте
    private static final Map<Integer, DfLineState> LINES = new HashMap<>();

    private DfLineState() {}

    /** Состояние крючка; новый бросок стартует с slack = 0. */
    public static DfLineState get(int hookId, long now) {
        LINES.values().removeIf(l -> now - l.ms > 2000);   // чистим старые крючки
        return LINES.computeIfAbsent(hookId, k -> new DfLineState());
    }
}
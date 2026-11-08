package ru.imaginaerum.damagecore.Init.fishing.gui.types;

public enum RodPartType {
    LINE, HOOK, BAIT, REEL;   // ordinal() = индекс слота в оверлее

    /** Что происходит, если слот пуст. */
    public void onEmpty(RodStats s) {
        if (this == BAIT) {      // без наживки клюёт гораздо хуже
            s.biteChance *= 0.08f;
            s.biteRadius *= 0.6f;
        }
    }
}
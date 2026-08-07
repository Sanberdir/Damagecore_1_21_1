package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

public enum PotionApplicationType {
    DRINK,       // выпито вручную
    SPLASH,      // брошенное (разбивающееся) зелье
    LINGERING,   // зелье длительного действия / туман
    MOB_ATTACK;  // эффект наложен атакой моба (стрела/удар), без зелья-предмета

    public String getTranslationKey() {
        return switch (this) {
            case DRINK      -> "damagecore.potion_application.drink";
            case SPLASH     -> "damagecore.potion_application.splash";
            case LINGERING  -> "damagecore.potion_application.lingering";
            case MOB_ATTACK -> "damagecore.potion_application.mob_attack";
        };
    }
}
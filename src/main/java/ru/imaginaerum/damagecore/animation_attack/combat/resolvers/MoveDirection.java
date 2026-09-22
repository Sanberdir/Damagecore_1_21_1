package ru.imaginaerum.damagecore.animation_attack.combat.resolvers;

public enum MoveDirection {
    FORWARD, BACKWARD, LEFT, RIGHT;

    public static MoveDirection fromName(String name) {
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[MoveDirection] Unknown direction '" + name + "', falling back to FORWARD");
            return FORWARD;
        }
    }
}
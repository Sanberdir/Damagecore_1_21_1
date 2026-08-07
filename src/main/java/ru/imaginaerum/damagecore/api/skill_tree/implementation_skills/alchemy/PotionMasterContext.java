package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.alchemy;

import java.util.UUID;

public class PotionMasterContext {
    public static final ThreadLocal<UUID> pickingUpPlayer = new ThreadLocal<>();
    public static final ThreadLocal<UUID> menuPlayer = new ThreadLocal<>();
}
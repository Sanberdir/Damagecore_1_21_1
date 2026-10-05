package ru.imaginaerum.damagecore.structure_processors;

import net.minecraft.client.Minecraft;

public class RescueClient {
    public static void open(int entityId) {
        Minecraft.getInstance().setScreen(new RescueScreen(entityId));
    }
}
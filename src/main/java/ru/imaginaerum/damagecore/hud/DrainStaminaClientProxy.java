package ru.imaginaerum.damagecore.hud;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ru.imaginaerum.damagecore.hud.elements.StaminaManager;

/**
 * Прокси для вызова клиентских методов из сетевых пакетов.
 *
 * Этот класс помечен @OnlyIn(Dist.CLIENT) — JVM не загрузит его на сервере.
 * DrainStaminaPacket вызывает этот класс только через DistExecutor,
 * поэтому StaminaManager (и Minecraft.getInstance()) никогда не будут
 * затронуты на выделенном сервере.
 */
@OnlyIn(Dist.CLIENT)
public class DrainStaminaClientProxy {

    public static void drain(float amount) {
        StaminaManager.drainFromServer(amount);
    }
}

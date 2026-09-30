package ru.imaginaerum.damagecore.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "damagecore")
public class TomeEvents {
    private static final String CURER_TAG = "damagecore_curer";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int step = player.getData(ModAttachments.TOME_STEP);

        if (step == 1 && player.getData(ModAttachments.TOME_OPENED) && player.isSleeping()) {
            player.setData(ModAttachments.TOME_STEP, 2);

        } else if (step == 4 && player.isSleeping()) {
            player.setData(ModAttachments.TOME_STEP, 5);

        } else if (step == 5 && player.tickCount % 10 == 0) {
            Raid raid = player.serverLevel().getRaids()
                    .getNearbyRaid(player.blockPosition(), 128 * 128);
            if (raid != null && !raid.isOver()) {
                player.setData(ModAttachments.TOME_STEP, 6);
                player.sendSystemMessage(Component.translatable("message.damagecore.tome.raid")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }

        } else if (step == 6 && player.isSleeping()) {
            player.setData(ModAttachments.TOME_STEP, 7);
        }
    }

    /** Запоминаем игрока, который начал лечение зомби-жителя золотым яблоком. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof ZombieVillager zombie)) return;
        if (!event.getItemStack().is(Items.GOLDEN_APPLE)) return;
        if (!zombie.hasEffect(MobEffects.WEAKNESS) || zombie.isConverting()) return;

        zombie.getPersistentData().putUUID(CURER_TAG, event.getEntity().getUUID());
    }

    /** Зомби-житель вылечен: шаг 7 -> 8, сообщение в чат. */
    @SubscribeEvent
    public static void onZombieVillagerCured(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof ZombieVillager zombie)) return;
        if (zombie.level().isClientSide()) return;

        Player curer = null;
        if (zombie.getPersistentData().hasUUID(CURER_TAG)) {
            curer = zombie.level().getPlayerByUUID(zombie.getPersistentData().getUUID(CURER_TAG));
        }
        if (curer == null) {
            // запасной вариант: ближайший игрок на шаге 7
            curer = zombie.level().getNearestPlayer(zombie, 32.0);
        }
        if (!(curer instanceof ServerPlayer player)) return;
        if (player.getData(ModAttachments.TOME_STEP) != 7) return;

        player.setData(ModAttachments.TOME_STEP, 8);
        player.sendSystemMessage(Component.translatable("message.damagecore.tome.cure")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
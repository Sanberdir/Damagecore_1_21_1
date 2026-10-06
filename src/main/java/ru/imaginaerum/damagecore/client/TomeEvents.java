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
    public static final String RESCUER_TAG = "damagecore_rescuer";
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TomeLocator.tick(player);
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
            }

        } else if (step == 6 && player.isSleeping()) {
            player.setData(ModAttachments.TOME_STEP, 7);

        } else if (step == 8 && player.isSleeping()) {
            player.setData(ModAttachments.TOME_STEP, 9);

        } else if (step == 9 && player.isSleeping()) {
            // <-- ВОТ ЭТОГО НЕ ХВАТАЛО
            player.setData(ModAttachments.TOME_STEP, 10);
        }
    }
    /** Вызывать в момент открытия клетки (там, где житель начинает следовать за игроком). */
    public static void markRescuer(net.minecraft.world.entity.LivingEntity villager, ServerPlayer player) {
        villager.getPersistentData().putUUID(RESCUER_TAG, player.getUUID());
    }

    public static void finishRescue(ServerPlayer player, String outcome) {
        if (player.getData(ModAttachments.TOME_STEP) != TomeLocator.COMPASS_STEP) return;
        int code = switch (outcome) { case "kill" -> 3; case "death" -> 2; default -> 1; };
        player.setData(ModAttachments.TOME_RESCUE, code);
        player.setData(ModAttachments.TOME_STEP, 11);
        player.setData(ModAttachments.TOME_COMPASS_ON, false);
        player.setData(ModAttachments.TOME_TARGET, Long.MIN_VALUE);
    }

    @SubscribeEvent
    public static void onRescuedVillagerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        var victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        boolean captive = victim.getPersistentData().hasUUID(RESCUER_TAG)
                || victim.getTags().contains(ru.imaginaerum.damagecore.structure_processors.RescuableVillager.TAG);
        if (!captive) return;

        var level = (net.minecraft.server.level.ServerLevel) victim.level();
        var killerEntity = event.getSource().getEntity();

        ServerPlayer target = null;
        if (killerEntity instanceof ServerPlayer killer) {
            finishRescue(killer, "kill");
            return;
        }
        if (victim.getPersistentData().hasUUID(RESCUER_TAG)) {
            var p = level.getPlayerByUUID(victim.getPersistentData().getUUID(RESCUER_TAG));
            if (p instanceof ServerPlayer sp) target = sp;
        }
        if (target == null && level.getNearestPlayer(victim, 64.0) instanceof ServerPlayer sp) target = sp;
        if (target != null) finishRescue(target, "death");
    }

    @SubscribeEvent
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("tomestep")
                        .requires(s -> s.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("step",
                                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int s = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "step");
                                    p.setData(ModAttachments.TOME_STEP, s);
                                    p.setData(ModAttachments.TOME_COMPASS_ON, false);
                                    p.setData(ModAttachments.TOME_TARGET, Long.MIN_VALUE);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Tome step = " + s), false);
                                    return 1;
                                })));
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
package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

import java.util.Random;

@EventBusSubscriber
public class FatalShotEvent {

    private static final float CRIT_CHANCE = 0.30f; // 30% шанс крита
    private static final float CRIT_MULTIPLIER = 1.5f; // х1.5 урона при крите
    private static final Random random = new Random();



    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        // Источник урона — стрела
        Entity directEntity = event.getSource().getDirectEntity();
        if (!(directEntity instanceof AbstractArrow arrow)) {
            return;
        }

        // Владелец стрелы — игрок
        Entity owner = arrow.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }

        // Проверка ноды талантов
        if (!SkillTreeServerHandler.isNodeLearned(player, "fatal_shot")) {
            return;
        }

        // Бросок на крит
        if (player.getRandom().nextFloat() < CRIT_CHANCE) {
            float originalDamage = event.getNewDamage();
            float newDamage = originalDamage * CRIT_MULTIPLIER;

            // В 1.21.1 для изменения урона используется setNewDamage
            event.setNewDamage(newDamage);
        }
    }

}
package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.blocking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber
public class ShieldBreakEvent {

    private static final int STRENGTH_DURATION_TICKS = 300; // 15 секунд
    private static final int STRENGTH_AMPLIFIER = 1;         // Сила II
    private static final float SHIELD_DAMAGE_THRESHOLD = 3.0f;

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!SkillTreeServerHandler.isNodeLearned(player, "cornered")) {
            return;
        }

        ItemStack shield = player.getUseItem();
        if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) {
            return;
        }

        float blockedDamage = event.getBlockedDamage();
        if (blockedDamage < SHIELD_DAMAGE_THRESHOLD) {
            return;
        }

        int durabilityDamage = 1 + (int) Math.floor(blockedDamage);
        int remaining = shield.getMaxDamage() - shield.getDamageValue();


        if (durabilityDamage >= remaining) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    STRENGTH_DURATION_TICKS,
                    STRENGTH_AMPLIFIER,
                    false,
                    true,
                    true
            ));
        }
    }
}
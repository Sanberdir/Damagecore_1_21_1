package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.shooting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber
public class RangerAttributeEvent {

    // В 1.21.1 вместо UUID используется ResourceLocation (укажите ваш MODID вместо "damagecore")
    private static final ResourceLocation SPEED_MOD_ID = ResourceLocation.fromNamespaceAndPath("damagecore", "ranger_bow_speed_bonus");
    private static final double SPEED_BONUS = 0.2; // +20% к скорости (значение 1 означает +100%, поэтому для +20% используем 0.2)

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Проверка стороны: только логический сервер
        if (event.getEntity().level().isClientSide) {
            return;
        }

        // В 1.21+ вместо event.player используется event.getEntity()
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Проверка ноды ranger
        if (!SkillTreeServerHandler.isNodeLearned(player, "ranger")) {
            removeSpeedModifier(player);
            return;
        }

        ItemStack activeItem = player.getUseItem();
        boolean pullingBow = !activeItem.isEmpty() &&
                (activeItem.getItem() instanceof BowItem || activeItem.getItem() instanceof CrossbowItem) &&
                player.isUsingItem();

        if (pullingBow) {
            addSpeedModifier(player);
        } else {
            removeSpeedModifier(player);
        }
    }

    private static void addSpeedModifier(ServerPlayer player) {
        var attributeInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeInstance != null && !attributeInstance.hasModifier(SPEED_MOD_ID)) {
            // Новый конструктор AttributeModifier в 1.21.1
            AttributeModifier mod = new AttributeModifier(SPEED_MOD_ID, SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            attributeInstance.addPermanentModifier(mod);
        }
    }

    private static void removeSpeedModifier(ServerPlayer player) {
        var attributeInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeInstance != null && attributeInstance.hasModifier(SPEED_MOD_ID)) {
            attributeInstance.removeModifier(SPEED_MOD_ID);
        }
    }
}

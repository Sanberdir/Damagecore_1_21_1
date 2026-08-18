package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.blocking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber
public class ShieldDurabilityEvent {

    private static final String HALF_DAMAGE_KEY = "half_damage_accum";

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!SkillTreeServerHandler.isNodeLearned(player, "reliable_material")) {
            return;
        }

        ItemStack shield = player.getUseItem();
        if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) {
            return;
        }

        float damage = event.getBlockedDamage();

        float acc = 0f;
        CustomData customData = shield.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(HALF_DAMAGE_KEY)) {
                acc = tag.getFloat(HALF_DAMAGE_KEY);
            }
        }

        acc += damage / 2f;

        int applied = (int) acc;
        acc -= applied;

        // Говорим движку не списывать прочность самостоятельно —
        // применим её сами вручную ниже, через hurtAndBreak
        event.setShieldDamage(0);

        if (applied > 0) {
            EquipmentSlot handSlot = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            shield.hurtAndBreak(applied, player, handSlot);
        }

        final float finalAcc = acc;
        shield.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, currentCustomData -> {
            CompoundTag tag = currentCustomData.copyTag();
            tag.putFloat(HALF_DAMAGE_KEY, finalAcc);
            return CustomData.of(tag);
        });
    }
}
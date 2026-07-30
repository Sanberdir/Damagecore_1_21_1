package ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.DCEntities;
import ru.imaginaerum.damagecore.Init.items.DCItems;
import ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow.ArrowLightningStorm;

public class ChainLightningArrow extends AbstractArrow {

    public ChainLightningArrow(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public ChainLightningArrow(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        super(DCEntities.CHAIN_LIGHTNING_ARROW.get(), shooter, level, pickupItemStack, null);
    }
    private int powerLevel = 0;

    public void setPowerLevel(int level) {
        this.powerLevel = level;
    }

    public int getPowerLevel() {
        return this.powerLevel;
    }
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide() && result.getEntity() instanceof LivingEntity target) {
            target.getPersistentData().putBoolean("damaged_chain_light_arrow", true);
        }

        super.onHitEntity(result);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (result.getEntity() instanceof LivingEntity target) {
                Entity shooter = this.getOwner();

                // Передаем getPowerLevel() в метод start нашего менеджера шторма
                ArrowLightningStorm.start(serverLevel, shooter, target, 4, this.getPowerLevel());
            }
            this.discard();
        }
    }



    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(DCItems.CHAIN_LIGHT_ARROW.get());
    }
}

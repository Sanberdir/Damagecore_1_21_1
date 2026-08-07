package ru.imaginaerum.damagecore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Mixin(LightningBolt.class)
public class LightningBoltMixin {

    // тени полей, которые есть в твоём классе LightningBolt
    @Shadow private int life;
    @Shadow private int flashes;
    @Shadow private boolean visualOnly;
    @Shadow @Nullable private ServerPlayer cause;
    @Shadow private Set<Entity> hitEntities;
    @Inject(method = "tick", at = @At("HEAD"))
    private void damagecore$onTick(CallbackInfo ci) {
        LightningBolt bolt = (LightningBolt) (Object) this;

        // работаем только на сервере и только если молния "активна"
        if (!(bolt.level() instanceof ServerLevel serverLevel)) return;
        if (this.visualOnly) return;
        if (this.life < 0) return;

        // область поиска — как в ваниле: радиус 3 по XYZ, Y расширено на +6 как в original
        AABB area = new AABB(
                bolt.getX() - 3.0D, bolt.getY() - 3.0D, bolt.getZ() - 3.0D,
                bolt.getX() + 3.0D, bolt.getY() + 6.0D + 3.0D, bolt.getZ() + 3.0D
        );

        List<Entity> entities = serverLevel.getEntities(bolt, area, Entity::isAlive);

        for (Entity entity : entities) {
            if (this.hitEntities.contains(entity)) continue;
            if (EventHooks.onEntityStruckByLightning(entity, bolt)) continue;

            float damage = bolt.getDamage();

            if (entity instanceof IHasDamageType has) {
                has.setLastDamageType(DamageType.LIGHTNING);
            }

            if (entity instanceof LivingEntity living) {
                DamageContext.add(living, DamageType.LIGHTNING, damage);
            }

            entity.thunderHit(serverLevel, bolt);
        }
    }
}
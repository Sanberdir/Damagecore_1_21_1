package ru.imaginaerum.damagecore.api.skill_tree.skill_tree_renderer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class EffectSourceTracker {

    private static final boolean DEBUG = true;

    private EffectSourceTracker() {}

    @Nullable
    private static Entity resolveOwnerFromEffectInstance(net.minecraft.world.effect.MobEffectInstance effectInstance, ServerLevel level) {
        // 1. Сначала попробуем int ID полей (sourceEntityId и аналоги)
        String[] intFieldCandidates = {"sourceEntityId", "effectSourceId", "ownerId", "f_19554_", "c_5184_"};
        for (String fieldName : intFieldCandidates) {
            try {
                Field f = net.minecraft.world.effect.MobEffectInstance.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                Object value = f.get(effectInstance);
                if (value instanceof Number num && num.intValue() != 0) {
                    return level.getEntity(num.intValue());
                }
            } catch (Throwable ignored) {}
        }

        // 2. Попробуем UUID-поле owner (в новых маппингах он так и может называться)
        String[] uuidFieldCandidates = {"owner", "ownerUUID", "sourceUUID", "effectSourceUUID", "f_19553_"}; // SRG для UUID поля owner
        for (String fieldName : uuidFieldCandidates) {
            try {
                Field f = net.minecraft.world.effect.MobEffectInstance.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                Object value = f.get(effectInstance);
                if (value instanceof UUID uuid) {
                    return level.getEntity(uuid);
                }
            } catch (Throwable ignored) {}
        }

        // 3. NBT-бакап: если в MobEffectInstance есть UUID source — вытащим его через NBT
        // В 1.21.1 save() без аргументов возвращает Tag (используем instanceof с CompoundTag)
        try {
            Tag rawTag = effectInstance.save();
            if (rawTag instanceof CompoundTag tag) {
                if (tag.hasUUID("Owner")) {
                    Entity e = level.getEntity(tag.getUUID("Owner"));
                    if (e != null) return e;
                }
                if (tag.contains("SourceEntityId", 99)) {
                    int id = tag.getInt("SourceEntityId");
                    if (id != 0) {
                        Entity e = level.getEntity(id);
                        if (e != null) return e;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 4. Попробуем публичные методы-геттеры
        String[] methodCandidates = {
                "getSourceEntityId", "getEffectSourceId", "getOwnerId",
                "getOwnerUUID", "getSourceUUID", "getSource", "getEffectSource", "getOwner"
        };
        for (String methodName : methodCandidates) {
            try {
                Method m = net.minecraft.world.effect.MobEffectInstance.class.getDeclaredMethod(methodName);
                m.setAccessible(true);
                Object value = m.invoke(effectInstance);
                if (value instanceof Number num && num.intValue() != 0) {
                    Entity e = level.getEntity(num.intValue());
                    if (e != null) return e;
                }
                if (value instanceof UUID uuid) {
                    Entity e = level.getEntity(uuid);
                    if (e != null) return e;
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    @Nullable
    private static Entity resolveViaEventApi(MobEffectEvent event) {
        // 1. Перебор всех публичных методов события — может есть нужный геттер
        if (DEBUG) {
            for (Method m : MobEffectEvent.class.getMethods()) {
                try {
                    if (m.getParameterCount() == 0
                            && (m.getName().contains("Source") || m.getName().contains("Caus")
                            || m.getName().contains("Owner") || m.getName().contains("Entity")
                            || m.getName().contains("Attack"))) {
                    }
                } catch (Throwable ignored) {}
            }
            for (Field f : MobEffectEvent.class.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                } catch (Throwable ignored) {}
            }
            net.minecraft.world.effect.MobEffectInstance inst = null;
            try {
                inst = event.getEffectInstance();
            } catch (Throwable ignored) {}
            if (inst != null) {
                for (Field f : net.minecraft.world.effect.MobEffectInstance.class.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(inst);
                        if (val != null) {
                            String extra = "";
                            if (val instanceof UUID) extra = " [UUID]";
                            else if (val instanceof Number) extra = " [num=" + val + "]";
                            else if (val instanceof Entity) extra = " [entity=" + ((Entity) val).getType().getDescription().getString() + "]";
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        try {
            Method getEffectSource = MobEffectEvent.class.getMethod("getEffectSource");
            Object value = getEffectSource.invoke(event);
            if (value instanceof Entity entity) return entity;
        } catch (Throwable t) {}

        try {
            Method m = MobEffectEvent.class.getMethod("getCausingEntity");
            Object value = m.invoke(event);
            if (value instanceof Entity entity) return entity;
        } catch (Throwable t) {}

        return null;
    }

    @Nullable
    private static EntityType<?> resolveViaNearbyMobs(ServerLevel level, ServerPlayer player, MobEffect effect) {
        // Ванильные мобы, которые накладывают эффекты БЕЗ урона и не передают owner:
        // WARDEN -> DARKNESS (радиус 30 блоков — его дальность обнаружения)
        // ELDER_GUARDIAN -> MINING_FATIGUE (радиус 50 блоков)
        // EVOKER -> BAD_OMEN не накладывает, но вдруг будет нужен
        double radius;
        EntityType<?> targetType;

        if (effect == MobEffects.DARKNESS.value()) {
            radius = 32.0;
            targetType = EntityType.WARDEN;
        } else if (effect == MobEffects.DIG_SLOWDOWN.value()) {
            radius = 60.0;
            targetType = EntityType.ELDER_GUARDIAN;
        } else {
            // Для любых других эффектов — радиус 10 блоков, ищем ближайшую живую сущность не-игрока
            radius = 10.0;
            targetType = null;
        }

        AABB aabb = player.getBoundingBox().inflate(radius);

        if (targetType != null) {
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e.getType() == targetType);
            if (nearby.isEmpty()) return null;

            // Ближайший
            nearby.sort((a, b) -> Double.compare(a.distanceTo(player), b.distanceTo(player)));
            return nearby.get(0).getType();
        }

        // Общий случай — неспецифичный эффект, берём ближайшую не-игрока живую сущность
        List<LivingEntity> any = level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e != player && e.getType() != EntityType.PLAYER);
        if (any.isEmpty()) return null;

        any.sort((a, b) -> Double.compare(a.distanceTo(player), b.distanceTo(player)));
        return any.get(0).getType();
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;

        MobEffect effect = event.getEffectInstance().getEffect().value();

        EntityType<?> sourceType = null;
        ServerLevel serverLevel = player.serverLevel();

        Entity viaEvent = resolveViaEventApi(event);
        if (viaEvent instanceof LivingEntity livingViaEvent && viaEvent.getType() != EntityType.PLAYER) {
            sourceType = livingViaEvent.getType();
        }

        if (sourceType == null) {
            Entity owner = resolveOwnerFromEffectInstance(event.getEffectInstance(), serverLevel);
            if (owner instanceof LivingEntity livingOwner && owner.getType() != EntityType.PLAYER) {
                sourceType = livingOwner.getType();
            }
        }

        if (sourceType == null) {
            LivingEntity lastHurtBy = player.getLastHurtByMob();
            if (lastHurtBy != null) {
                EntityType<?> hurtByType = lastHurtBy.getType();
                if (hurtByType != EntityType.PLAYER) sourceType = hurtByType;
            }
        }

        // Финальный fallback: поиск ближайших мобов (для Warden/Darkness и других спецэффектов без owner)
        if (sourceType == null) {
            sourceType = resolveViaNearbyMobs(serverLevel, player, effect);
        }

        PacketDistributor.sendToPlayer(player, new SyncEffectSourcePayload(effect, sourceType));
    }
}
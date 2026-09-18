package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.api.ModNetwork;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class FoodProtectionCapability {

    // 1. Создаем новую EntityCapability вместо старого CapabilityManager
    public static final EntityCapability<FoodProtectionManager, Void> FOOD_PROTECTION =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "food_protection"),
                    FoodProtectionManager.class
            );

    // 2. Регистрация капабилити для Игрока (заменяет AttachCapabilitiesEvent)
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                FOOD_PROTECTION,
                EntityType.PLAYER,
                (player, context) -> {
                    // ИСПРАВЛЕНО: получаем ОДИН И ТОТ ЖЕ закешированный объект через attachment,
                    // а не создаём новый FoodProtectionManager при каждом вызове getCapability()
                    FoodProtectionManager manager = player.getData(
                            ru.imaginaerum.damagecore.libraty_effects.FoodProtectionAttachments.FOOD_PROTECTION_MANAGER.get());
                    manager.bindPlayer(player);
                    return manager;
                }
        );
    }

    // 3. Обновленный TickEvent в NeoForge 1.21.1
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Получаем менеджер через новый метод getCapability
        FoodProtectionManager manager = player.getCapability(FOOD_PROTECTION);
        if (manager != null) {
            manager.tick();

            // Синхронизируем только с сервера раз в секунду
            if (!player.level().isClientSide && player.tickCount % 20 == 0) {
                syncToClient((ServerPlayer) player);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        Item item = event.getItem().getItem();

        List<FoodProtectionReloadListener.Effect> defs =
                FoodProtectionReloadListener.EFFECTS.get(item);

        if (defs == null) {
            defs = List.of();
        }

        /*
         * ==========================================
         * Получаем эффекты, которые реально дала еда
         * ==========================================
         */
        List<MobEffectInstance> vanillaMobEffects = new ArrayList<>();

        FoodProperties foodProperties =
                event.getItem().get(DataComponents.FOOD);

        if (foodProperties != null) {
            for (var pair : foodProperties.effects()) {
                MobEffectInstance live =
                        player.getEffect(pair.effect().getEffect());

                if (live != null) {
                    vanillaMobEffects.add(new MobEffectInstance(live));
                }
            }
        }

        /* Загадочное рагу: эффект хранится в самом ItemStack */
        if (item == Items.SUSPICIOUS_STEW) {
            SuspiciousStewEffects stewEffects =
                    event.getItem().get(DataComponents.SUSPICIOUS_STEW_EFFECTS);

            if (stewEffects != null) {
                for (SuspiciousStewEffects.Entry entry : stewEffects.effects()) {

                    MobEffectInstance live =
                            player.getEffect(entry.effect());

                    if (live != null) {
                        vanillaMobEffects.add(new MobEffectInstance(live));
                    }
                }
            }
        }

        /*
         * Если еда вообще не имеет ни защиты,
         * ни MobEffect — нам нечего отображать.
         */
        if (defs.isEmpty() && vanillaMobEffects.isEmpty()) {
            return;
        }

        FoodProtectionManager manager =
                player.getCapability(FOOD_PROTECTION);

        if (manager == null) {
            System.err.println(
                    "[FoodProtection] FATAL: manager is NULL for player "
                            + player.getName().getString()
            );
            return;
        }

        /*
         * ==========================================
         * ЕДА БЕЗ ЗАЩИТЫ
         * ==========================================
         *
         * Например:
         * - золотая морковь
         * - подозрительное рагу
         * - любая модовая еда с MobEffect
         *
         * У неё нет DamageType, но она всё равно
         * должна отображаться в категории "Еда".
         */
        if (defs.isEmpty()) {

            int duration = 0;

            for (MobEffectInstance effect : vanillaMobEffects) {
                duration = Math.max(
                        duration,
                        effect.getDuration()
                );
            }

            FoodProtectionEffect foodEffect =
                    new FoodProtectionEffect(
                            item,
                            duration,
                            vanillaMobEffects
                    );

            manager.addEffect(foodEffect);

            System.out.println(
                    "[FoodProtection] Registered vanilla food: "
                            + BuiltInRegistries.ITEM
                            .getKey(item)
                            + " effects="
                            + vanillaMobEffects.size()
                            + " duration="
                            + duration
            );

            syncToClient((ServerPlayer) player);
            return;
        }

        /*
         * ==========================================
         * ЕДА С ЗАЩИТОЙ ОТ УРОНА
         * ==========================================
         */

        Set<ResourceLocation> allRemoved = new HashSet<>();

        for (var def : defs) {
            if (def.overrideVanilla()) {
                allRemoved.addAll(def.removeEffects());
            }
        }

        List<MobEffectInstance> filteredMobEffects =
                vanillaMobEffects.stream()
                        .filter(inst -> {

                            ResourceLocation loc =
                                    BuiltInRegistries.MOB_EFFECT
                                            .getKey(inst.getEffect().value());

                            return loc == null
                                    || !allRemoved.contains(loc);
                        })
                        .toList();

        for (var def : defs) {

            /*
             * Для защиты используем duration из конфига.
             *
             * Но если duration <= 0, а еда дала обычный
             * MobEffect, используем его длительность.
             */
            int duration = def.duration();

            if (duration <= 0 && !filteredMobEffects.isEmpty()) {

                for (MobEffectInstance effect : filteredMobEffects) {
                    duration = Math.max(
                            duration,
                            effect.getDuration()
                    );
                }
            }

            FoodProtectionEffect newEffect =
                    new FoodProtectionEffect(
                            item,
                            def.damageType(),
                            def.protection(),
                            duration,
                            filteredMobEffects
                    );

            manager.addEffect(newEffect);

            System.out.println(
                    "[FoodProtection] addEffect: item="
                            + BuiltInRegistries.ITEM.getKey(item)
                            + " type="
                            + def.damageType()
                            + " protection="
                            + def.protection()
                            + " duration="
                            + duration
                            + " mobEffects="
                            + filteredMobEffects.size()
            );

            /*
             * Удаляем vanilla-эффекты, если overrideVanilla=true.
             */
            if (def.overrideVanilla()) {

                for (ResourceLocation effLoc : def.removeEffects()) {

                    try {

                        MobEffect mob =
                                BuiltInRegistries.MOB_EFFECT.get(effLoc);

                        if (mob != null) {

                            player.removeEffect(
                                    BuiltInRegistries.MOB_EFFECT
                                            .wrapAsHolder(mob)
                            );
                        }

                    } catch (Exception ignored) {
                    }
                }
            }
        }

        syncToClient((ServerPlayer) player);
    }

    // 4. Обновленный синтаксис отправки пакетов NeoForge 1.21.1
    public static void syncToClient(ServerPlayer player) {
        FoodProtectionManager manager = player.getCapability(FOOD_PROTECTION);
        if (manager != null) {
            // 1.21.1: FoodProtectionManager.save(...) теперь требует HolderLookup.Provider —
            // передаём player.registryAccess() (RegistryAccess реализует HolderLookup.Provider).
            CompoundTag saved = manager.save(player.registryAccess());
            PacketDistributor.sendToPlayer(player, (CustomPacketPayload) new FoodProtectionSyncPacket(saved));
        }
    }

    public static FoodProtectionManager get(Player player) {
        return player.getCapability(FOOD_PROTECTION);
    }
}
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
                (player, context) -> new FoodProtectionManager(player)
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
        List<FoodProtectionReloadListener.Effect> defs = FoodProtectionReloadListener.EFFECTS.get(item);
        if (defs == null || defs.isEmpty()) return;

        List<MobEffectInstance> vanillaMobEffects = new ArrayList<>();

        // В 1.21.1 свойства еды берутся из компонентов ItemStack
        FoodProperties foodProperties = event.getItem().get(DataComponents.FOOD);
        if (foodProperties != null) {
            for (var pair : foodProperties.effects()) {
                // В ванильном коде теперь используется Holder<MobEffect>
                MobEffectInstance inst = player.getEffect(pair.effect().getEffect());
                if (inst != null) {
                    vanillaMobEffects.add(new MobEffectInstance(inst));
                }
            }
        }

        FoodProtectionManager manager = player.getCapability(FOOD_PROTECTION);
        if (manager != null) {
            Set<ResourceLocation> allRemoved = new HashSet<>();
            for (var def : defs) {
                if (def.overrideVanilla()) {
                    allRemoved.addAll(def.removeEffects());
                }
            }

            List<MobEffectInstance> filteredMobEffects = vanillaMobEffects.stream()
                    .filter(inst -> {
                        // В 1.21.1 inst.getEffect() возвращает Holder<MobEffect>
                        ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value());
                        return loc == null || !allRemoved.contains(loc);
                    })
                    .toList();

            for (var def : defs) {
                manager.addEffect(new FoodProtectionEffect(
                        item,
                        def.damageType(),
                        def.protection(),
                        def.duration(),
                        filteredMobEffects
                ));

                if (def.overrideVanilla()) {
                    for (ResourceLocation effLoc : def.removeEffects()) {
                        try {
                            MobEffect mob = BuiltInRegistries.MOB_EFFECT.get(effLoc);
                            if (mob != null) {
                                // .removeEffect теперь принимает Holder, .wrapOptional оборачивает моб в Holder
                                player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mob));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            syncToClient((ServerPlayer) player);
        }
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
package ru.imaginaerum.damagecore.animation_attack;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class WeaponAttackAnimationHandler {

    private static final long COMBO_RESET_WINDOW_MS = 1000L;
    private static final long FALLBACK_DURATION_MS = 500L;

    private static final Map<ResourceLocation, Long> ANIMATION_DURATIONS_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, ComboState> COMBO_STATES = new ConcurrentHashMap<>();

    private static class ComboState {
        int nextIndex = 0;
        long lastAttackTime = 0L;
        long attackLockedUntil = 0L;
        boolean comboActive = false;
        ResourceLocation lastWeaponId = null;

        boolean isCharging = false; // Режим замаха
        boolean wantsToRelease = false; // Флаг, что игрок уже отпустил ЛКМ, но мы ждем конца замаха
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            if (!handleInputEvents(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        handleInputEvents(event.getEntity());
    }

    private static boolean handleInputEvents(Player player) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return true;

        ComboState state = COMBO_STATES.computeIfAbsent(player.getUUID(), id -> new ComboState());
        long now = System.currentTimeMillis();

        if (state.isCharging) return false;
        if (now < state.attackLockedUntil) return false;

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return true;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

        if (state.lastWeaponId != null && !state.lastWeaponId.equals(itemId)) {
            forceReset(clientPlayer, state);
        }
        state.lastWeaponId = itemId;

        boolean isShiftKeyDown = Minecraft.getInstance().options.keyShift.isDown();

        if (isShiftKeyDown) {
            // Инициализируем ЗАМАХ (Shift + ЛКМ)
            List<String> keys = WeaponAnimationManager.INSTANCE.getChargeKeysOrder(itemId);
            if (!keys.isEmpty()) {
                state.isCharging = true;
                state.wantsToRelease = false;
                state.nextIndex = 0;
                playChargeSegment(clientPlayer, state, keys, now);
                return false;
            }
        } else {
            // Обычная комба
            List<ResourceLocation> regularAnims = WeaponAnimationManager.INSTANCE.getRegularSwings(itemId);
            if (regularAnims.isEmpty()) return true;

            var animation = PlayerAnimationAccess.getPlayerAnimationLayer(clientPlayer, WeaponAnimationSetup.ATTACK_LAYER_ID);
            if (!(animation instanceof PlayerAnimationController controller)) return true;

            if (state.nextIndex >= regularAnims.size()) {
                state.nextIndex = 0;
            }

            ResourceLocation chosen = regularAnims.get(state.nextIndex);
            long duration = getAnimationDurationMs(chosen);

            controller.triggerAnimation(chosen);

            state.lastAttackTime = now;
            state.attackLockedUntil = now + duration;
            state.nextIndex = (state.nextIndex + 1) % regularAnims.size();
            state.comboActive = true;
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ComboState state = COMBO_STATES.computeIfAbsent(player.getUUID(), id -> new ComboState());
        long now = System.currentTimeMillis();

        if (state.isCharging) {
            ItemStack heldItem = player.getMainHandItem();
            ResourceLocation itemId = heldItem.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(heldItem.getItem());

            if (itemId == null || !itemId.equals(state.lastWeaponId)) {
                forceReset(player, state);
                return;
            }

            List<String> keys = WeaponAnimationManager.INSTANCE.getChargeKeysOrder(itemId);
            boolean isAttackKeyDown = Minecraft.getInstance().options.keyAttack.isDown();

            // Если игрок отпустил ЛКМ, взводим триггер релиза удара
            if (!isAttackKeyDown) {
                state.wantsToRelease = true;
            }

            // Ждем завершения текущей анимации замаха
            if (now >= state.attackLockedUntil) {
                if (!state.wantsToRelease) {
                    // Игрок всё еще удерживает ЛКМ
                    if (!keys.isEmpty()) {
                        // Если текущий замах не последний — шагаем дальше по цепочке
                        if (state.nextIndex < keys.size() - 1) {
                            state.nextIndex++;
                            playChargeSegment(player, state, keys, now);
                        } else {
                            // ФИКС ЗАСТЫВАНИЯ: Мы на последнем замахе.
                            // Просто не вызываем playChargeSegment повторно, чтобы не перезапускать анимацию.
                            // Персонаж автоматически замрёт в финальном кадре замаха.
                        }
                    }
                } else {
                    // Игрок отпустил ЛКМ И анимация замаха ПОЛНОСТЬЮ проигралась -> Срабатывает УДАР!
                    if (!keys.isEmpty()) {
                        String currentKey = keys.get(state.nextIndex % keys.size());
                        ResourceLocation releaseAnim = WeaponAnimationManager.INSTANCE.getReleaseAnimation(itemId, currentKey);

                        if (releaseAnim != null) {
                            var animation = PlayerAnimationAccess.getPlayerAnimationLayer(player, WeaponAnimationSetup.ATTACK_LAYER_ID);
                            if (animation instanceof PlayerAnimationController controller) {
                                controller.triggerAnimation(releaseAnim);

                                long duration = getAnimationDurationMs(releaseAnim);
                                state.lastAttackTime = now;
                                state.attackLockedUntil = now + duration;

                                state.nextIndex = (state.nextIndex + 1) % keys.size();
                                state.comboActive = true;
                            }
                        }
                    }
                    state.isCharging = false;
                    state.wantsToRelease = false;
                }
            }
        }

        // Автоматический сброс обычной комбы или позы по таймауту
        if (!state.isCharging && state.comboActive && (now - state.lastAttackTime > COMBO_RESET_WINDOW_MS)) {
            forceReset(player, state);
        }
    }

    private static void playChargeSegment(AbstractClientPlayer player, ComboState state, List<String> keys, long now) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

        String currentKey = keys.get(state.nextIndex % keys.size());
        ResourceLocation chargeAnim = WeaponAnimationManager.INSTANCE.getChargeAnimation(itemId, currentKey);

        if (chargeAnim != null) {
            var animation = PlayerAnimationAccess.getPlayerAnimationLayer(player, WeaponAnimationSetup.ATTACK_LAYER_ID);
            if (animation instanceof PlayerAnimationController controller) {

                controller.triggerAnimation(chargeAnim);

                long duration = getAnimationDurationMs(chargeAnim);

                System.out.println("[Charge] anim=" + chargeAnim
                        + " key=" + currentKey
                        + " duration=" + duration
                        + "ms");

                state.attackLockedUntil = now + duration;
                state.lastAttackTime = now;
            }
        }
    }


    private static void forceReset(AbstractClientPlayer player, ComboState state) {
        var animation = PlayerAnimationAccess.getPlayerAnimationLayer(player, WeaponAnimationSetup.ATTACK_LAYER_ID);
        if (animation instanceof PlayerAnimationController controller) {
            controller.stop();
        }
        state.nextIndex = 0;
        state.comboActive = false;
        state.isCharging = false;
        state.wantsToRelease = false;
    }

    private static long getAnimationDurationMs(ResourceLocation animationId) {
        return ANIMATION_DURATIONS_CACHE.computeIfAbsent(animationId, id -> {
            if (!PlayerAnimResources.hasAnimation(id)) {
                return FALLBACK_DURATION_MS;
            }

            Animation animation = PlayerAnimResources.getAnimation(id);

            System.out.println(
                    animationId +
                            " length=" + animation.length()
            );

            return (long) (animation.length() * 50);
        });
    }
}

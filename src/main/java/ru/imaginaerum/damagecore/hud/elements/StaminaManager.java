package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.imaginaerum.damagecore.Config;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;


@EventBusSubscriber(value = Dist.CLIENT)
public class StaminaManager {

    public static final float BASE_STAMINA = 40f;

    public static float getMaxStamina() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return BASE_STAMINA;
        return PlayerStatsCapability.get(mc.player)
                .map(stats -> BASE_STAMINA + stats.getStat(StatsType.ENDURANCE) * 0.5f)
                .orElse(BASE_STAMINA);
    }

    private static final float DRAIN_SPRINT              = 0.2f;
    private static final float DRAIN_SHIELD_HOLD         = 0.02f;
    private static final float DRAIN_SHIELD_HIT          = 6.0f;
    private static final float REGEN_WALK                = 0.2f;
    private static final float REGEN_STAND               = 0.45f;
    private static final float DRAIN_MINING              = 0.03f;

    private static final int EXHAUSTION_COOLDOWN         = 40;
    private static final int MINING_REGEN_DELAY          = 30;
    private static int miningRegenDelayTimer = 0;

    private static final float STAMINA_CRITICAL          = 4.0f;
    private static final float STAMINA_LOW               = 10.0f;

    private static final float SPEED_MULTIPLIER_CRITICAL = 0.3f;
    private static final float SPEED_MULTIPLIER_LOW      = 0.6f;

    private static final float DRAIN_BOAT                     = DRAIN_SPRINT / 4f;
    private static final float BOAT_SPEED_MULTIPLIER_CRITICAL = 0.2f;
    private static final float BOAT_SPEED_MULTIPLIER_LOW      = 0.4f;
    private static final double ORIGINAL_BOAT_MAX_SPEED       = 1.2;

    private static float stamina       = -1f;
    private static boolean exhausted   = false;
    private static int exhaustionTimer = 0;
    private static double originalSpeed = -1;

    public static boolean isExhausted() { return exhausted; }

    public static float getStamina() {
        if (stamina < 0f) stamina = getMaxStamina();
        return stamina;
    }

    private static float getShieldStaminaMultiplier(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // if (SkillTreeServerHandler.isNodeLearned(serverPlayer, "shield_bearer")) {
            //     return 0.5f;
            // }
        }
        return 1.0f;
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!Config.showStaminaHud) return;
        if (player.isCreative() || player.isSpectator()) return;

        if (exhausted) {
            event.setCanceled(true);
        }
    }

    public static void drainFromServer(float amount) {
        if (exhausted) return;
        if (stamina < 0f) stamina = getMaxStamina();
        stamina -= amount;
        if (stamina <= 0f) {
            stamina = 0f;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) triggerExhaustion(mc.player);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!Config.showStaminaHud) return;
        if (player.level().isClientSide()) return;

        // Если игрок блокировал щитом в момент получения урона — считаем это блоком
        if (player.isBlocking()) {
            if (stamina <= 0f || exhausted) {
                triggerExhaustion(player);
                return;
            }

            float mult = getShieldStaminaMultiplier(player);
            stamina -= DRAIN_SHIELD_HIT * mult;
            if (stamina <= 0f) {
                stamina = 0f;
                triggerExhaustion(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (!Config.showStaminaHud) return;
        if (player.level().isClientSide()) return;

        if (player.isCreative() || player.isSpectator()) {
            stamina   = getMaxStamina();
            exhausted = false;
            return;
        }

        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        if (originalSpeed == -1) {
            originalSpeed = speedAttribute.getBaseValue();
        }

        if (stamina < STAMINA_CRITICAL) {
            speedAttribute.setBaseValue(originalSpeed * SPEED_MULTIPLIER_CRITICAL);
        } else if (stamina < STAMINA_LOW) {
            speedAttribute.setBaseValue(originalSpeed * SPEED_MULTIPLIER_LOW);
        } else {
            speedAttribute.setBaseValue(originalSpeed);
        }

        handleBoatSlowdown(player);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.showStaminaHud) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) return;

        if (stamina < 0f) stamina = getMaxStamina();

        boolean sprinting      = player.isSprinting();
        boolean moving         = player.getDeltaMovement().horizontalDistanceSqr() > 1e-6;
        boolean inBoat         = player.getVehicle() instanceof Boat;
        boolean blockingShield = isHoldingShield(player) && player.isBlocking();
        boolean mining         = mc.gameMode != null && mc.gameMode.isDestroying();

        if (exhausted) {
            if (sprinting) player.setSprinting(false);
            if (blockingShield) player.getCooldowns().addCooldown(
                    player.getUseItem().getItem(), 100
            );
            exhaustionTimer--;
            if (exhaustionTimer <= 0) exhausted = false;
            return;
        }

        if (mining) {
            stamina -= DRAIN_MINING;
            if (stamina <= 0f) {
                stamina = 0f;
                triggerExhaustion(player);
            }
            miningRegenDelayTimer = MINING_REGEN_DELAY;
            return;
        }

        if (miningRegenDelayTimer > 0) {
            miningRegenDelayTimer--;
            return;
        }

        if (inBoat) {
            if (moving) {
                stamina -= DRAIN_BOAT;
                if (stamina <= 0f) stamina = 0f;
            } else {
                stamina = Math.min(stamina + REGEN_STAND, getMaxStamina());
            }
        } else if (sprinting) {
            stamina -= DRAIN_SPRINT;
            if (stamina <= 0f) {
                stamina = 0f;
                triggerExhaustion(player);
            }
        } else if (blockingShield) {
            float mult = getShieldStaminaMultiplier(player);
            stamina -= DRAIN_SHIELD_HOLD * mult;
            if (stamina <= 0f) {
                stamina = 0f;
                triggerExhaustion(player);
            }
        } else if (moving) {
            stamina = Math.min(stamina + REGEN_WALK, getMaxStamina());
        } else {
            stamina = Math.min(stamina + REGEN_STAND, getMaxStamina());
        }
    }

    private static boolean isHoldingShield(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ShieldItem
                || player.getOffhandItem().getItem() instanceof ShieldItem;
    }

    private static void triggerExhaustion(Player player) {
        exhausted       = true;
        exhaustionTimer = EXHAUSTION_COOLDOWN;
        player.setSprinting(false);
    }

    private static void handleBoatSlowdown(Player player) {
        if (!(player.getVehicle() instanceof Boat boat)) return;

        Vec3 vel = boat.getDeltaMovement();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        float multiplier;
        if (stamina < STAMINA_CRITICAL)     multiplier = BOAT_SPEED_MULTIPLIER_CRITICAL;
        else if (stamina < STAMINA_LOW)     multiplier = BOAT_SPEED_MULTIPLIER_LOW;
        else                                return;

        if (hSpeed > 0.01) {
            double maxAllowed = ORIGINAL_BOAT_MAX_SPEED * multiplier;
            if (hSpeed > maxAllowed) {
                double scale = maxAllowed / hSpeed;
                boat.setDeltaMovement(vel.x * scale, vel.y, vel.z * scale);
            }
        }
    }
}
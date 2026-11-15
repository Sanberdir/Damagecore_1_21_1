package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.Init.enchantments.WindSquallConfig;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "damagecore")
public class WindSquallHandler {

    private record PendingCrit(long tick) {}

    private static final Map<Player, PendingCrit> PENDING = new WeakHashMap<>();

    private static int getLevel(LivingEntity entity, ItemStack stack) {
        return entity.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.WIND_SQUALL)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack))
                .orElse(0);
    }

    /** Игрок падает (условия ванильного крита без проверки спринта) */
    private static boolean isFalling(Player p) {
        return p.fallDistance > 0.0F
                && !p.onGround()
                && !p.onClimbable()
                && !p.isInWater()
                && !p.isPassenger();
    }

    // 1) Повышенный шанс крита в падении
    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        int level = getLevel(player, player.getMainHandItem());
        if (level <= 0) return;
        if (!isFalling(player)) return;
        if (event.isCriticalHit()) return; // крит уже есть

        float chance = Math.min(1.0F, WindSquallConfig.CRIT_CHANCE_PER_LEVEL * level);
        if (player.getRandom().nextFloat() < chance) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(1.0F); // множитель применим сами
            synchronized (PENDING) {
                PENDING.put(player, new PendingCrit(player.level().getGameTime()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (event.getSource().getDirectEntity() != player) return;

        PendingCrit crit;
        synchronized (PENDING) {
            crit = PENDING.remove(player);
        }
        if (crit != null && crit.tick() == player.level().getGameTime()) {
            event.setAmount(event.getAmount() * WindSquallConfig.CRIT_MULT);
        }
    }

    // 2) Подброс после успешного удара в падении
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (event.getSource().getDirectEntity() != player) return;
        if (player.level().isClientSide) return;
        if (event.getNewDamage() <= 0) return;

        int level = getLevel(player, player.getMainHandItem());
        if (level <= 0) return;
        if (!isFalling(player)) return;
        if (player.fallDistance < WindSquallConfig.MIN_FALL_DISTANCE) return;

        double vy = WindSquallConfig.LAUNCH_BASE + WindSquallConfig.LAUNCH_PER_LEVEL * level;
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(m.x, vy, m.z);
        player.hurtMarked = true;   // отправить скорость клиенту
        player.fallDistance = 0.0F; // без урона от падения, и новое падение для следующего удара

        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.GUST_EMITTER_SMALL,
                    player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
        }
    }
}
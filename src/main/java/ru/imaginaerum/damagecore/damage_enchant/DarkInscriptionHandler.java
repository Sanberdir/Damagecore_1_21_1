package ru.imaginaerum.damagecore.damage_enchant;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import ru.imaginaerum.damagecore.Init.enchantments.ModEnchantments;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "damagecore")
public class DarkInscriptionHandler {

    private record PendingCrit(float multiplier, long tick) {}

    private static final Map<Player, PendingCrit> PENDING = new WeakHashMap<>();

    private static int getLevel(LivingEntity entity, ItemStack stack) {
        return entity.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.DARK_INSCRIPTION)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack))
                .orElse(0);
    }

    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        int level = getLevel(player, player.getMainHandItem());
        if (level <= 0) return;

        boolean crit = event.isCriticalHit();

        if (!crit && DarkInscriptionConfig.isNight(player.level())
                && player.getRandom().nextFloat() < DarkInscriptionConfig.NIGHT_CRIT_CHANCE) {
            crit = true;
            event.setCriticalHit(true);
        }
        if (!crit) return;

        event.setDamageMultiplier(1.0F);
        float multiplier = DarkInscriptionConfig.BASE_CRIT_MULT
                + DarkInscriptionConfig.CRIT_MULT_PER_LEVEL * level;

        synchronized (PENDING) {
            PENDING.put(player, new PendingCrit(multiplier, player.level().getGameTime()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return;

        int level = getLevel(attacker, attacker.getMainHandItem());
        if (level <= 0) return;

        Level world = attacker.level();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        float value = DarkInscriptionConfig.DAMAGE_PER_LEVEL * level;

        // Наибольший тип урона оружия
        DamageType maxType = null;
        if (attacker.getMainHandItem().getItem() instanceof IDamageCoreWeapon weapon) {
            maxType = DarkInscriptionConfig.getMaxType(weapon.damagecore$getDamageMap());
        }
        if (maxType == null) maxType = DarkInscriptionConfig.NIGHT_DAMAGE_TYPE;

        if (DarkInscriptionConfig.isNight(world)) {
            // Ночью: прибавка к наибольшему типу
            DamageContext.add(target, maxType, value);
            amount += value;
        } else if (DarkInscriptionConfig.isDay(world)) {
            // Днём: вычитаем из наибольшего типа, но не больше самого удара
            float reduction = Math.min(value, amount);
            float inContext = DamageContext.getMap(target).getOrDefault(maxType, 0.0F);
            float ctxReduction = Math.min(reduction, inContext);
            if (ctxReduction > 0) {
                DamageContext.add(target, maxType, -ctxReduction);
            }
            amount -= reduction;
        }

        // Крит: множитель на весь итоговый урон
        if (attacker instanceof Player player) {
            PendingCrit crit;
            synchronized (PENDING) {
                crit = PENDING.remove(player);
            }
            if (crit != null && crit.tick() == world.getGameTime()) {
                amount *= crit.multiplier();
            }
        }
        System.out.println("[Dark] level=" + level
                + " dayTime=" + (world.getDayTime() % 24000L)
                + " night=" + DarkInscriptionConfig.isNight(world)
                + " day=" + DarkInscriptionConfig.isDay(world)
                + " maxType=" + maxType
                + " before=" + event.getAmount()
                + " after=" + amount);
        event.setAmount(amount);
    }
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinal(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof LivingEntity a
                && a.getMainHandItem().isEnchanted()) {
            System.out.println("[Dark-final] dealt=" + event.getNewDamage()
                    + " hp=" + event.getEntity().getHealth());
        }
    }
}
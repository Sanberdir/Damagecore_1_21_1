package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_ranks.DamageRank;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

import java.util.Map;

/**
 * Обработчик урона от оружия с кастомными данными (WeaponDamageData).
 *
 * Логика:
 * 1. Если у предмета в руке атакующего есть запись в WeaponDamageManager -
 *    полностью пересчитываем урон удара на основе JSON-данных оружия.
 * 2а. Атакующий - ИГРОК: суммируем ВСЕ типы урона оружия, к физическим
 *     типам применяется суммарный бонус STRENGTH+DEXTERITY (как раньше).
 * 2б. Атакующий - НЕ игрок (моб/скелет и т.п.): суммирование не подходит,
 *     вместо этого выбирается ОДИН, самый большой по значению тип урона
 *     из карты оружия, и применяется только он, без бонусов от статов
 *     (у мобов нет PlayerStatsCapability).
 * 3. Разбивка по типам сохраняется в DamageContext.
 *
 * Если у предмета нет записи в WeaponDamageManager - обработчик ничего
 * не делает, урон считается полностью ванильным способом.
 */
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class WeaponDamageHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        // Обрабатываем только прямой ближний бой: атакующий должен быть
        // непосредственным источником урона (не стрела, не эффект и т.п.)
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (source.getDirectEntity() != attacker) {
            return;
        }

        WeaponDamageManager manager = WeaponDamageManager.getInstance();
        if (manager == null) {
            return;
        }

        ItemStack heldItem = attacker.getMainHandItem();
        if (heldItem.isEmpty()) {
            return;
        }

        WeaponDamageData weaponData = manager.getDamageData(heldItem.getItem());
        if (weaponData == null || weaponData.getDamageMap().isEmpty()) {
            return;
        }

        if (attacker instanceof Player player) {
            handlePlayerAttack(event, player, weaponData);
        } else {
            handleNonPlayerAttack(event, weaponData);
        }
    }

    /** Игрок: суммируем все типы урона, физическим типам - бонус STRENGTH+DEXTERITY. */
    private static void handlePlayerAttack(LivingIncomingDamageEvent event, Player attacker, WeaponDamageData weaponData) {
        int strengthLevel = PlayerStatsCapability.get(attacker)
                .map(stats -> stats.getStat(StatsType.STRENGTH))
                .orElse(0);

        int dexterityLevel = PlayerStatsCapability.get(attacker)
                .map(stats -> stats.getStat(StatsType.DEXTERITY))
                .orElse(0);

        int combinedLevel = strengthLevel + dexterityLevel;

        // TODO: ранг атакующего/оружия пока всегда S по умолчанию
        DamageRank attackerRank = DamageRank.S;

        float totalDamage = 0.0F;

        for (Map.Entry<DamageType, Double> entry : weaponData.getDamageMap().entrySet()) {
            DamageType type = entry.getKey();
            double baseTypeDamage = entry.getValue();

            double finalTypeDamage = baseTypeDamage;

            if (type.isPhysical() && combinedLevel > 0) {
                double bonus = attackerRank.getMultiplier() * baseTypeDamage / 100.0 * combinedLevel;
                finalTypeDamage = baseTypeDamage + bonus;
            }

            totalDamage += (float) finalTypeDamage;
            DamageContext.add(event.getEntity(), type, (float) finalTypeDamage);
        }

        event.setAmount(totalDamage);
    }

    /** Не игрок (моб): берём только наибольший по значению тип урона из карты оружия. */
    private static void handleNonPlayerAttack(LivingIncomingDamageEvent event, WeaponDamageData weaponData) {
        DamageType maxType = null;
        double maxDamage = Double.NEGATIVE_INFINITY;

        for (Map.Entry<DamageType, Double> entry : weaponData.getDamageMap().entrySet()) {
            if (entry.getValue() > maxDamage) {
                maxDamage = entry.getValue();
                maxType = entry.getKey();
            }
        }

        if (maxType == null) return; // карта была не пуста, но на всякий случай

        DamageContext.add(event.getEntity(), maxType, (float) maxDamage);
        event.setAmount((float) maxDamage);
    }
}
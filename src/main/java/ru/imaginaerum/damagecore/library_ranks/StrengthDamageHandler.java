package ru.imaginaerum.damagecore.library_ranks;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageManager;
import ru.imaginaerum.damagecore.library_stats.IPlayerStats;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

/**
 * Обработчик прибавки физического урона от характеристики STRENGTH
 * для ЛЮБОГО удара игрока (голая рука, инструмент, оружие без кастомных
 * JSON-данных), ЗА ИСКЛЮЧЕНИЕМ предметов, зарегистрированных в
 * WeaponDamageManager - для них STRENGTH и DEXTERITY уже суммируются
 * внутри WeaponDamageHandler, и повторное применение здесь задвоило бы бонус.
 *
 * Формула бонуса: множитель_ранга * базовый_урон / 100 * уровень_STRENGTH.
 *
 * LivingIncomingDamageEvent выбран намеренно - это САМОЕ РАННЕЕ событие
 * в цепочке урона (fires до вычета брони/щита/чар), поэтому наш бонус
 * увеличивает "сырой" урон атаки, а затем этот увеличенный урон
 * уже обычным образом проходит через броню цели - логично для физического урона.
 */
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class StrengthDamageHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        // Бонус применяется только к урону, классифицированному ванильным тегом
        // как "атака игрока" (обычный ближний бой рукой/оружием)
        if (!source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
            return;
        }

        // Атакующий (source.getEntity()) - это тот, кто нанёс удар,
        // в отличие от event.getEntity(), которая возвращает ПОЛУЧАТЕЛЯ урона
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        // КРИТИЧЕСКИ ВАЖНАЯ ПРОВЕРКА: если у предмета в руке ЕСТЬ запись
        // в WeaponDamageManager - этим ударом уже полностью занимается
        // WeaponDamageHandler (там STRENGTH + DEXTERITY суммируются вместе).
        // Без этой проверки бонус STRENGTH применился бы ДВАЖДЫ на таком оружии.
        WeaponDamageManager manager = WeaponDamageManager.getInstance();
        if (manager != null) {
            ItemStack heldItem = attacker.getMainHandItem();
            if (!heldItem.isEmpty() && manager.getDamageData(heldItem.getItem()) != null) {
                return;
            }
        }

        PlayerStatsCapability.get(attacker).ifPresent(stats -> applyStrengthBonus(event, stats));
    }

    /**
     * Считает и применяет бонус урона по формуле:
     * бонус = rank.getMultiplier() * baseDamage / 100 * strengthLevel.
     */
    private static void applyStrengthBonus(LivingIncomingDamageEvent event, IPlayerStats stats) {
        int strengthLevel = stats.getStat(StatsType.STRENGTH);

        // Нулевой уровень STRENGTH - бонус не считаем, чтобы не тратить вычисления впустую
        if (strengthLevel <= 0) {
            return;
        }

        // TODO: пока ранг атакующего всегда E (по умолчанию, как указано в задаче).
        // В будущем при добавлении системы рангов оружия/предметов ранг нужно будет
        // брать из держимого игроком ItemStack (например, через Data Component ранга),
        // а не хардкодить константу здесь.
        DamageRank attackerRank = DamageRank.E;

        float baseDamage = event.getAmount();
        if (baseDamage <= 0) {
            return;
        }

        float bonus = attackerRank.getMultiplier() * baseDamage / 100.0F * strengthLevel;

        event.setAmount(baseDamage + bonus);
    }
}
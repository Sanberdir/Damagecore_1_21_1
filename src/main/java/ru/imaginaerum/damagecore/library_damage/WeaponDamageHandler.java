package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageManager;
import ru.imaginaerum.damagecore.library_ranks.DamageRank;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

import java.util.Map;

/**
 * Обработчик урона от оружия с кастомными данными (WeaponDamageData).
 *
 * Логика:
 * 1. Если у предмета в руке атакующего есть запись в WeaponDamageManager -
 *    полностью пересчитываем урон удара на основе JSON-данных оружия
 *    (piercing/slashing/bludgeoning/fire и т.д.), вместо ванильного значения.
 * 2. К каждому ФИЗИЧЕСКОМУ типу урона (DamageType.isPhysical()) применяется
 *    СУММАРНЫЙ бонус от STRENGTH и DEXTERITY по формуле:
 *    бонус = rank.getMultiplier() * damageОфТипа / 100 * (уровень_STRENGTH + уровень_DEXTERITY).
 *    То есть каждая характеристика по отдельности даёт бонус по той же формуле,
 *    что и STRENGTH, а итоговые бонусы складываются (что математически эквивалентно
 *    подстановке суммы уровней в общую формулу).
 * 3. Итоговый урон - сумма всех типов (с учётом суммарного бонуса) - подставляется
 *    как финальный amount атаки.
 * 4. Разбивка по типам сохраняется в DamageContext для последующего
 *    использования (резисты, отображение источника урона и т.д.).
 *
 * DEXTERITY даёт бонус ТОЛЬКО оружию с кастомными данными (то есть именно здесь,
 * в WeaponDamageHandler) - в отличие от STRENGTH, которая дополнительно работает
 * и с обычным ванильным оружием без JSON-данных через StrengthDamageHandler.
 *
 * Если у предмета нет записи в WeaponDamageManager - обработчик ничего
 * не делает, и урон считается полностью ванильным способом (см.
 * StrengthDamageHandler для этого случая).
 */
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class WeaponDamageHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        // Обрабатываем только урон от ближнего боя игрока
        if (!source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
            return;
        }

        if (!(source.getEntity() instanceof Player attacker)) {
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

        // Уровни STRENGTH и DEXTERITY атакующего (0, если Data Attachment недоступен)
        int strengthLevel = PlayerStatsCapability.get(attacker)
                .map(stats -> stats.getStat(StatsType.STRENGTH))
                .orElse(0);

        int dexterityLevel = PlayerStatsCapability.get(attacker)
                .map(stats -> stats.getStat(StatsType.DEXTERITY))
                .orElse(0);

        // Суммарный "уровень силы удара" от обеих характеристик -
        // бонусы STRENGTH и DEXTERITY складываются перед применением к урону
        int combinedLevel = strengthLevel + dexterityLevel;

        // TODO: ранг атакующего/оружия пока всегда E по умолчанию (см. комментарий
        // в StrengthDamageHandler) - в будущем нужно брать ранг с предмета оружия
        DamageRank attackerRank = DamageRank.S;

        float totalDamage = 0.0F;

        for (Map.Entry<ru.imaginaerum.damagecore.library_damage.DamageType, Double> entry : weaponData.getDamageMap().entrySet()) {
            ru.imaginaerum.damagecore.library_damage.DamageType type = entry.getKey();
            double baseTypeDamage = entry.getValue();

            double finalTypeDamage = baseTypeDamage;

            // STRENGTH и DEXTERITY вместе усиливают только физические типы урона
            // (piercing/slashing/bludgeoning); их бонусы суммируются через combinedLevel
            if (type.isPhysical() && combinedLevel > 0) {
                double bonus = attackerRank.getMultiplier() * baseTypeDamage / 100.0 * combinedLevel;
                finalTypeDamage = baseTypeDamage + bonus;
            }

            totalDamage += (float) finalTypeDamage;

            // Фиксируем разбивку по типам в контексте (по цели удара - event.getEntity())
            DamageContext.add(event.getEntity(), type, (float) finalTypeDamage);
        }

        // Подставляем полностью пересчитанный урон вместо ванильного значения оружия
        event.setAmount(totalDamage);
    }
}
package ru.imaginaerum.damagecore.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class DamageResistanceHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        // В 1.21.1 источник и урон хранятся в специальном объекте DamageContainer
        net.neoforged.neoforge.common.damagesource.DamageContainer container = event.getContainer();

        // ✅ ИСПРАВЛЕНО: Извлекаем ваш собственный тип урона мода напрямую
        ru.imaginaerum.damagecore.library_damage.DamageType damageType = DamageContext.getLast(entity);

        // 2. Если контекста не было — берем ванильный холдер и сопоставляем с вашим типом урона
        if (damageType == null) {
            net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType> vanillaHolder = container.getSource().typeHolder();
            // Получаем строковый ID ванильного типа урона (например, "minecraft:explosion" или "minecraft:fall")
            String vanillaId = vanillaHolder.unwrapKey().map(key -> key.location().toString()).orElse("");

            // ТУТ НАДО СКОРРЕКТИРОВАТЬ ПОД ВАШУ ЛОГИКУ: Получаем ваш DamageType на основе ванильного ID или имени
            // Например, если у вас в DamageType есть метод сопоставления, или по имени:
            try {
                // Если имена ваших типов совпадают с ванильными/старыми:
                // damageType = ru.imaginaerum.damagecore.library_damage.DamageType.valueOf(vanillaHolder.value().msgId().toUpperCase());

                // Временная заглушка (замените на ваш метод конвертации ванильного урона в ваш DamageType):
                damageType = ru.imaginaerum.damagecore.library_damage.DamageType.BLUDGEONING;
            } catch (Exception e) {
                return;
            }
        }

        if (damageType == null) return;

        // Читаем начальный входящий урон из контейнера
        float incomingDamage = container.getNewDamage();

        // Собираем все защиты от каждой части брони
        List<DamageResistance> allResistances = new ArrayList<>();

        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                // В 1.21.1 .getMaterial() возвращает Holder<ArmorMaterial>. Передаем чистый материал через .value()
                DamageResistance resistance = DamageArmorModifier.getDamageResistance(
                        armorItem.getMaterial().value(),
                        armorItem.getType(),
                        damageType
                );
                if (resistance.getFlat() > 0 || resistance.getPercent() > 0) {
                    allResistances.add(resistance);
                }
            }
        }

        // Добавляем защиту от эффектов еды (только для игроков)
        float foodProtectionPercent = 0.0f;
        if (entity instanceof Player player) {
            FoodProtectionManager foodManager = FoodProtectionCapability.get(player);
            if (foodManager != null) {
                foodProtectionPercent = foodManager.getTotalProtectionPercent(damageType);
            }
        }

        // Применяем защиты в порядке: сначала вся абсолютная, потом вся процентная
        float totalFlat = 0f;
        float totalPercent = 0f;

        if (!allResistances.isEmpty()) {
            // Суммируем абсолютную защиту
            totalFlat = allResistances.stream()
                    .map(DamageResistance::getFlat)
                    .reduce(0f, Float::sum);

            // Суммируем процентную защиту от брони
            totalPercent = allResistances.stream()
                    .map(DamageResistance::getPercent)
                    .reduce(0f, Float::sum);
        }

        // Добавляем защиту от еды (только процентная)
        totalPercent += foodProtectionPercent;

        // Ограничиваем суммарную процентную защиту максимум 100%
        totalPercent = Math.min(1.0f, totalPercent);

        // Применяем формулу: (Урон - Абсолютная защита) * (1 - Процентная защита)
        float afterFlat = Math.max(0, incomingDamage - totalFlat);
        float finalDamage = afterFlat * (1 - totalPercent);

        // УСТАНОВКА НОВОГО УРОНА В 1.21.1:
        container.setNewDamage(finalDamage);

        // 🧹 3. ОБЯЗАТЕЛЬНО чистим контекст
        DamageContext.clear(entity);
    }



}
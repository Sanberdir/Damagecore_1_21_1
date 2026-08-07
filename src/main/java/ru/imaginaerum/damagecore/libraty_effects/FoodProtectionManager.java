package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.*;

/**
 * Менеджер для эффектов защиты от еды.
 *
 * Изменения по сравнению с предыдущей реализацией:
 * - Хранит для каждого DamageType список эффектов (Map<DamageType, List<FoodProtectionEffect>>).
 * - При добавлении эффектов они накапливаются (проценты суммируются).
 * - Метод получения суммарного процента возвращает сумму, ограниченную 1.0f (100%).
 * - Метод получения времени возвращает максимальное оставшееся время среди эффектов для типа.
 * - Сериализация/десериализация сохраняет все эффекты.
 */
public class FoodProtectionManager {
    private final Map<DamageType, List<FoodProtectionEffect>> activeEffects = new HashMap<>();
    private final Player player;

    public FoodProtectionManager(Player player) {
        this.player = player;
    }
    public List<ItemStack> getUniqueActiveFoods() {
        Set<ItemStack> uniqueStacks = new HashSet<>();
        for (FoodProtectionEffect effect : getAllEffects()) {
            if (effect.getProtectionPercent() > 0) { // только реально дающие бонус
                uniqueStacks.add(new ItemStack(effect.getItem())); // создаём ItemStack из Item
            }
        }
        return List.copyOf(uniqueStacks);
    }

    /**
     * Добавляет эффект — просто складываем в список.
     * Проценты будут суммироваться при запросе.
     */
    public void addEffect(FoodProtectionEffect effect) {
        Objects.requireNonNull(effect, "effect");
        activeEffects
                .computeIfAbsent(effect.getDamageType(), k -> new ArrayList<>())
                .add(effect);
    }

    /**
     * Удаляет все эффекты для типа.
     */
    public void removeEffect(DamageType type) {
        activeEffects.remove(type);
    }

    /**
     * Удаляет конкретный экземпляр эффекта (если нужно).
     * Возвращает true, если был удалён хотя бы один экземпляр.
     */
    public boolean removeEffect(FoodProtectionEffect effect) {
        List<FoodProtectionEffect> list = activeEffects.get(effect.getDamageType());
        if (list == null) return false;
        boolean removed = list.remove(effect);
        if (list.isEmpty()) activeEffects.remove(effect.getDamageType());
        return removed;
    }

    /**
     * Возвращает копию списка эффектов для типа (без возможности модифицировать внутреннее состояние менеджера).
     */
    public List<FoodProtectionEffect> getEffects(DamageType type) {
        List<FoodProtectionEffect> list = activeEffects.get(type);
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    /**
     * Суммарный процент защиты для данного DamageType.
     * Суммируем все protectionPercent и ограничиваем 1.0f (100%).
     */
    public float getTotalProtectionPercent(DamageType type) {
        List<FoodProtectionEffect> list = activeEffects.get(type);
        if (list == null) return 0.0f;

        float sum = 0.0f;
        for (FoodProtectionEffect e : list) {
            sum += e.getProtectionPercent();
        }
        return Math.min(sum, 1.0f);
    }

    /**
     * Максимальное оставшееся количество тиков среди всех эффектов данного DamageType.
     * Подходит для отображения "времени" в GUI.
     */
    public int getMaxRemainingTicks(DamageType type) {
        List<FoodProtectionEffect> list = activeEffects.get(type);
        if (list == null) return 0;

        int max = 0;
        for (FoodProtectionEffect e : list) {
            if (e.getRemainingTicks() > max) {
                max = e.getRemainingTicks();
            }
        }
        return max;
    }

    /**
     * Тик менеджера — уменьшаем таймеры и удаляем истёкшие эффекты.
     */
    public void tick() {
        Iterator<Map.Entry<DamageType, List<FoodProtectionEffect>>> it = activeEffects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DamageType, List<FoodProtectionEffect>> entry = it.next();
            List<FoodProtectionEffect> list = entry.getValue();

            // уменьшаем тики и удаляем истёкшие
            list.removeIf(effect -> {
                effect.tick();
                return effect.isExpired();
            });

            if (list.isEmpty()) {
                it.remove();
            }
        }
    }

    /**
     * Сохранение всех эффектов в NBT.
     * Формат: ListTag из CompoundTag'ов, каждый CompoundTag формируется FoodProtectionEffect.save().
     * 1.21.1: FoodProtectionEffect.save(...) теперь требует HolderLookup.Provider (нужен для
     * корректной сериализации ItemStack/компонентов), поэтому провайдер прокидывается сюда извне —
     * обычно это player.registryAccess() на сервере или Provider, который NeoForge передаёт
     * в IAttachmentSerializer#write(Provider, T) при сохранении attachment-данных.
     */
    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag effectsList = new ListTag();

        for (List<FoodProtectionEffect> list : activeEffects.values()) {
            for (FoodProtectionEffect effect : list) {
                effectsList.add(effect.save(provider));
            }
        }

        tag.put("foodEffects", effectsList);
        return tag;
    }

    /**
     * Загрузка эффектов из NBT (совместимо с сохранением выше).
     * 1.21.1: FoodProtectionEffect.load(...) требует HolderLookup.Provider вторым аргументом
     * (симметрично save(...)) — прокидываем его сюда извне так же, как и для save.
     */
    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        activeEffects.clear();

        if (tag.contains("foodEffects")) {
            ListTag effectsList = tag.getList("foodEffects", 10);
            for (int i = 0; i < effectsList.size(); i++) {
                CompoundTag effectTag = effectsList.getCompound(i);
                FoodProtectionEffect effect = FoodProtectionEffect.load(effectTag, provider);
                activeEffects
                        .computeIfAbsent(effect.getDamageType(), k -> new ArrayList<>())
                        .add(effect);
            }
        }
    }

    public boolean hasEffects() {
        return !activeEffects.isEmpty();
    }

    /**
     * Возвращает глубокую копию текущей карты эффектов.
     * Удобно для GUI: избежать concurrent-modification и не дать GUI менять внутреннее состояние.
     */
    public Map<DamageType, List<FoodProtectionEffect>> getActiveEffects() {
        Map<DamageType, List<FoodProtectionEffect>> copy = new HashMap<>();
        for (Map.Entry<DamageType, List<FoodProtectionEffect>> e : activeEffects.entrySet()) {
            copy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return copy;
    }

    /**
     * Удобный метод: все эффекты в одном списке (не группированные).
     */
    public List<FoodProtectionEffect> getAllEffects() {
        List<FoodProtectionEffect> out = new ArrayList<>();
        for (List<FoodProtectionEffect> list : activeEffects.values()) {
            out.addAll(list);
        }
        return out;
    }
    /**
     * Совместимость со старым GUI.
     * Возвращает эффект с максимальным временем действия.
     * Нужен только для отображения таймера.
     */
    public FoodProtectionEffect getEffect(DamageType type) {
        List<FoodProtectionEffect> list = activeEffects.get(type);
        if (list == null || list.isEmpty()) return null;

        FoodProtectionEffect best = null;
        int maxTicks = 0;

        for (FoodProtectionEffect e : list) {
            if (e.getRemainingTicks() > maxTicks) {
                maxTicks = e.getRemainingTicks();
                best = e;
            }
        }

        return best;
    }

    /**
     * Возвращает игрока, к которому привязан менеджер.
     */
    public Player getPlayer() {
        return player;
    }
}
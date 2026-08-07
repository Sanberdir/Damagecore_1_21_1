package ru.imaginaerum.damagecore.armor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.neoforged.fml.common.EventBusSubscriber;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DamageArmorModifier extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ArmorMaterial, Map<ArmorItem.Type, Map<DamageType, DamageResistance>>>
            CLIENT_CACHE = new ConcurrentHashMap<>();

    private final Map<String, ArmorMaterialConfig> materialConfigs = new ConcurrentHashMap<>();
    private static final Map<ArmorMaterial, Map<ArmorItem.Type, Map<DamageType, DamageResistance>>> cachedModifiers = new ConcurrentHashMap<>();

    public DamageArmorModifier() {
        super(GSON, "damage_armor_modifiers");
        initializeDefaultModifiers(); // <- добавь это
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {

        if (resources.isEmpty()) {
            return;
        }

        materialConfigs.clear();
        cachedModifiers.clear();


        resources.forEach((resourceLocation, jsonElement) -> {
            try {
                ArmorMaterialConfig config = GSON.fromJson(jsonElement, ArmorMaterialConfig.class);
                materialConfigs.put(resourceLocation.getPath(), config);
                cacheMaterialModifiers(config);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        cachedModifiers.forEach((material, types) -> {
        });
        initializeDefaultModifiers();

    }
    private void cacheMaterialModifiers(ArmorMaterialConfig config) {
        Map<ArmorItem.Type, Map<DamageType, DamageResistance>> typeModifiers = new EnumMap<>(ArmorItem.Type.class);

        // Обрабатываем каждую часть брони
        processArmorPart(config, "helmet", ArmorItem.Type.HELMET, typeModifiers);
        processArmorPart(config, "chestplate", ArmorItem.Type.CHESTPLATE, typeModifiers);
        processArmorPart(config, "leggings", ArmorItem.Type.LEGGINGS, typeModifiers);
        processArmorPart(config, "boots", ArmorItem.Type.BOOTS, typeModifiers);

        // Добавляем глобальные модификаторы
        applyGlobalModifiers(config, typeModifiers);

        // Получаем ArmorMaterial через новый реестр 1.21.1
        ArmorMaterial material = getMaterialByName(config.material);
        if (material != null) {
            cachedModifiers.put(material, typeModifiers);
        }
    }

    // Исправленный вспомогательный метод для поиска материала в реестре
    private ArmorMaterial getMaterialByName(String name) {
        // В 1.21.1 ResourceLocation.parse(name) безопасно обрабатывает строки вроде "iron" или "minecraft:iron"
        ResourceLocation location = ResourceLocation.parse(name);

        // Получаем чистый объект ArmorMaterial из центрального встроенного реестра материалов брони
        return net.minecraft.core.registries.BuiltInRegistries.ARMOR_MATERIAL.get(location);
    }


    private void processArmorPart(ArmorMaterialConfig config, String partName, ArmorItem.Type armorType,
                                  Map<ArmorItem.Type, Map<DamageType, DamageResistance>> result) {
        Map<DamageType, Float> flatMap = parseDamageTypes(
                getMapForPart(config, partName + "_flat")
        );
        Map<DamageType, Float> percentMap = parseDamageTypes(
                getMapForPart(config, partName + "_percent")
        );

        Map<DamageType, DamageResistance> resistances = new HashMap<>();

        // Объединяем все типы урона из обеих карт
        Set<DamageType> allTypes = new HashSet<>(flatMap.keySet());
        allTypes.addAll(percentMap.keySet());

        for (DamageType type : allTypes) {
            float flat = flatMap.getOrDefault(type, 0.0f);
            float percent = percentMap.getOrDefault(type, 0.0f);

            if (flat > 0 || percent > 0) {
                resistances.put(type, new DamageResistance(flat, percent));
            }
        }

        if (!resistances.isEmpty()) {
            result.put(armorType, resistances);
        }
    }

    private void applyGlobalModifiers(ArmorMaterialConfig config,
                                      Map<ArmorItem.Type, Map<DamageType, DamageResistance>> typeModifiers) {

        Map<DamageType, Float> globalFlat = parseDamageTypes(config.all_flat);
        Map<DamageType, Float> globalPercent = parseDamageTypes(config.all_percent);

        if (globalFlat.isEmpty() && globalPercent.isEmpty()) return;

        // Применяем глобальные модификаторы ко всем частям брони
        for (Map.Entry<ArmorItem.Type, Map<DamageType, DamageResistance>> entry : typeModifiers.entrySet()) {
            Map<DamageType, DamageResistance> partResistances = entry.getValue();

            // Обрабатываем типы урона из глобальных модификаторов
            for (DamageType type : globalFlat.keySet()) {
                DamageResistance existing = partResistances.get(type);
                float newFlat = globalFlat.get(type);
                float newPercent = globalPercent.getOrDefault(type, 0.0f);

                if (existing != null) {
                    // Суммируем абсолютную защиту
                    newFlat += existing.getFlat();
                    // Суммируем процентную защиту (максимум 90% чтобы не сделать неуязвимым)
                    newPercent = Math.min(1.0f, newPercent + existing.getPercent());
                }

                partResistances.put(type, new DamageResistance(newFlat, newPercent));
            }

            // Также добавляем типы урона которые есть только в percent
            for (DamageType type : globalPercent.keySet()) {
                if (!globalFlat.containsKey(type)) {
                    DamageResistance existing = partResistances.get(type);
                    float newPercent = globalPercent.get(type);

                    if (existing != null) {
                        newPercent = Math.min(1.0f, newPercent + existing.getPercent());
                        partResistances.put(type, new DamageResistance(
                                existing.getFlat(), newPercent
                        ));
                    } else {
                        partResistances.put(type, new DamageResistance(0, newPercent));
                    }
                }
            }
        }
    }

    private Map<String, Float> getMapForPart(ArmorMaterialConfig config, String part) {
        return switch (part) {
            case "helmet_flat" -> config.helmet_flat;
            case "helmet_percent" -> config.helmet_percent;
            case "chestplate_flat" -> config.chestplate_flat;
            case "chestplate_percent" -> config.chestplate_percent;
            case "leggings_flat" -> config.leggings_flat;
            case "leggings_percent" -> config.leggings_percent;
            case "boots_flat" -> config.boots_flat;
            case "boots_percent" -> config.boots_percent;
            default -> new HashMap<>();
        };
    }

    private Map<DamageType, Float> parseDamageTypes(Map<String, Float> source) {
        Map<DamageType, Float> result = new HashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                try {
                    DamageType damageType = DamageType.valueOf(key.toUpperCase());
                    result.put(damageType, value);
                } catch (IllegalArgumentException e) {
                }
            });
        }
        return result;
    }



    public void initializeDefaultModifiers() {
        // Эти значения будут использоваться если нет конфига в ресурспаке
        // или как fallback значения
        Map<ArmorItem.Type, Map<DamageType, DamageResistance>> defaultIron = new EnumMap<>(ArmorItem.Type.class);

        // Шлем
        Map<DamageType, DamageResistance> helmetResistances = new HashMap<>();
        helmetResistances.put(DamageType.SLASHING, new DamageResistance(1.0f, 0.1f)); // 1 + 10%
        helmetResistances.put(DamageType.BLUDGEONING, new DamageResistance(1.5f, 0.15f)); // 1.5 + 15%
        defaultIron.put(ArmorItem.Type.HELMET, helmetResistances);

        // Нагрудник
        Map<DamageType, DamageResistance> chestplateResistances = new HashMap<>();
        chestplateResistances.put(DamageType.PIERCING, new DamageResistance(2.0f, 0.2f)); // 2 + 20%
        chestplateResistances.put(DamageType.SLASHING, new DamageResistance(1.5f, 0.15f)); // 1.5 + 15%
        chestplateResistances.put(DamageType.BLUDGEONING, new DamageResistance(1.0f, 0.1f)); // 1 + 10%
        defaultIron.put(ArmorItem.Type.CHESTPLATE, chestplateResistances);

        // Поножи
        Map<DamageType, DamageResistance> leggingsResistances = new HashMap<>();
        leggingsResistances.put(DamageType.PIERCING, new DamageResistance(1.0f, 0.1f)); // 1 + 10%
        leggingsResistances.put(DamageType.SLASHING, new DamageResistance(1.0f, 0.1f)); // 1 + 10%
        defaultIron.put(ArmorItem.Type.LEGGINGS, leggingsResistances);

        // Ботинки
        Map<DamageType, DamageResistance> bootsResistances = new HashMap<>();
        bootsResistances.put(DamageType.BLUDGEONING, new DamageResistance(0.5f, 0.05f)); // 0.5 + 5%
        defaultIron.put(ArmorItem.Type.BOOTS, bootsResistances);

        if (!cachedModifiers.containsKey(ArmorMaterials.IRON.value())) {
            cachedModifiers.put(ArmorMaterials.IRON.value(), defaultIron);
        }

    }

    public static Map<DamageType, DamageResistance> getDamageResistances(ArmorMaterial material, ArmorItem.Type type) {
        if (Damagecore_1_21_1_neo.ARMOR_MODIFIER == null) {
            return Map.of();
        }

        // Если кэш ещё не загружен — инициализируем дефолты
        if (Damagecore_1_21_1_neo.ARMOR_MODIFIER.cachedModifiers.isEmpty()) {
            Damagecore_1_21_1_neo.ARMOR_MODIFIER.initializeDefaultModifiers();
        }

        Map<ArmorItem.Type, Map<DamageType, DamageResistance>> materialModifiers =
                Damagecore_1_21_1_neo.ARMOR_MODIFIER.cachedModifiers.get(material);

        if (materialModifiers == null) {
            return Map.of();
        }

        return materialModifiers.getOrDefault(type, Map.of());
    }

    public static DamageResistance getDamageResistance(ArmorMaterial material, ArmorItem.Type type, DamageType damageType) {
        return getDamageResistances(material, type)
                .getOrDefault(damageType, new DamageResistance(0, 0));
    }
}
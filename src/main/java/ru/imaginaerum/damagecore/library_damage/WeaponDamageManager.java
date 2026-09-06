package ru.imaginaerum.damagecore.library_damage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class WeaponDamageManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private final Map<Item, WeaponDamageData> weaponData = new HashMap<>();

    // Синглтон-ссылка на актуальный экземпляр менеджера, обновляется при apply().
    // Нужен, чтобы обработчики урона (StrengthDamageHandler и т.д.) могли получить
    // данные оружия без прямого доступа к DataPack-реестрам.
    private static WeaponDamageManager instance;

    public WeaponDamageManager() {
        super(GSON, "weapon_damage");
        instance = this;
    }

    /**
     * Возвращает текущий (актуальный после последнего /reload) экземпляр менеджера.
     * Может вернуть null, если datapack ещё не был загружен ни разу (крайне маловероятно
     * после старта сервера, но проверка на null всё равно обязательна у вызывающей стороны).
     */
    @Nullable
    public static WeaponDamageManager getInstance() {
        return instance;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
        weaponData.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            try {
                ResourceLocation id = entry.getKey();
                String path = id.getPath();

                // ПРАВИЛЬНОЕ извлечение имени предмета из пути
                // Файл: weapon_damage/iron_sword.json
                // path = "weapon_damage/iron_sword.json"
                String itemName = extractItemNameFromPath(path);

                // Ищем предмет
                Item item = findItem(itemName);

                if (item != null) {
                    JsonObject json = entry.getValue().getAsJsonObject();
                    WeaponDamageData data = WeaponDamageData.fromJson(json);
                    weaponData.put(item, data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        weaponData.forEach((item, data) -> {
            System.out.println("  " + BuiltInRegistries.ITEM.getKey(item) + " -> " + data.getDamageMap());
        });
    }

    private String extractItemNameFromPath(String path) {
        String[] parts = path.split("/");
        if (parts.length > 1) {
            String fileName = parts[parts.length - 1];
            if (fileName.endsWith(".json")) {
                return fileName.substring(0, fileName.length() - 5);
            }
            return fileName;
        }
        return path;
    }

    private Item findItem(String itemName) {
        ResourceLocation minecraftId = ResourceLocation.fromNamespaceAndPath("minecraft", itemName);
        Item item = BuiltInRegistries.ITEM.get(minecraftId);
        if (item != null) {
            return item;
        }
        return null;
    }

    public WeaponDamageData getDamageData(Item item) {
        return weaponData.get(item);
    }
}
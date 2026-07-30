package ru.imaginaerum.damagecore.library_damage.arrow_data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class ArrowDamageManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    // Синглтон для доступа из миксина
    public static ArrowDamageManager INSTANCE;

    private final Map<Item, ArrowDamageData> arrowData = new HashMap<>();

    public ArrowDamageManager() {
        super(GSON, "arrow_damage"); // папка: resources/data/<mod>/arrow_damage/
        INSTANCE = this;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared,
                         ResourceManager manager, ProfilerFiller profiler) {
        arrowData.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            try {
                String itemName = extractItemName(entry.getKey().getPath());
                Item item = findItem(entry.getKey().getNamespace(), itemName);

                if (item != null) {
                    JsonObject json = entry.getValue().getAsJsonObject();
                    arrowData.put(item, ArrowDamageData.fromJson(json));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String extractItemName(String path) {
        String[] parts = path.split("/");
        String file = parts[parts.length - 1];
        return file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
    }

    private Item findItem(String namespace, String name) {
        for (String ns : new String[]{namespace, "minecraft"}) {
            // Раньше: new ResourceLocation(ns, name)
            // В 1.21.1 конструктор ResourceLocation приватный, используем фабричный метод
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(ns, name);
            if (BuiltInRegistries.ITEM.containsKey(rl)) {
                return BuiltInRegistries.ITEM.get(rl);
            }
        }
        return null;
    }

    public ArrowDamageData getData(Item item) {
        return arrowData.get(item);
    }

    public boolean hasData(Item item) {
        return arrowData.containsKey(item);
    }
}
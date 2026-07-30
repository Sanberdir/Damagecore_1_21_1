package ru.imaginaerum.damagecore.library_weapon_types;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class WeaponTypeManager implements PreparableReloadListener {

    public static final WeaponTypeManager INSTANCE = new WeaponTypeManager();
    private static final Gson GSON = new Gson();

    // itemId (e.g. "minecraft:iron_sword") -> WeaponType
    private final Map<String, WeaponType> weaponTypes = new HashMap<>();

    public WeaponType getType(ResourceLocation itemId) {
        return weaponTypes.get(itemId.toString());
    }

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier,
            ResourceManager manager,
            ProfilerFiller prepProfiler,
            ProfilerFiller applyProfiler,
            Executor prepExecutor,
            Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, WeaponType> loaded = new HashMap<>();
            // ищем все файлы в data/*/weapon_types/*.json
            Map<ResourceLocation, Resource> resources =
                    manager.listResources("weapon_types", loc -> loc.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (var reader = new InputStreamReader(
                        entry.getValue().open(), StandardCharsets.UTF_8)) {

                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (!json.has("items") || !json.has("type")) continue;

                    String typeName = json.get("type").getAsString().toUpperCase();
                    WeaponType type;
                    try {
                        type = WeaponType.valueOf(typeName);
                    } catch (IllegalArgumentException e) {
                        System.err.println("[WeaponTypes] Unknown type: " + typeName
                                + " in " + entry.getKey());
                        continue;
                    }

                    for (var itemEl : json.getAsJsonArray("items")) {
                        loaded.put(itemEl.getAsString(), type);
                    }

                } catch (Exception e) {
                    System.err.println("[WeaponTypes] Failed to load " + entry.getKey());
                    e.printStackTrace();
                }
            }
            return loaded;
        }, prepExecutor).thenCompose(barrier::wait).thenAcceptAsync(loaded -> {
            weaponTypes.clear();
            weaponTypes.putAll(loaded);
        }, applyExecutor);
    }
}
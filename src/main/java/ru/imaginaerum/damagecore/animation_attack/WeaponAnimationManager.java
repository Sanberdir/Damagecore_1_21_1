package ru.imaginaerum.damagecore.animation_attack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponType;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponTypeManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class WeaponAnimationManager implements PreparableReloadListener {

    public static final WeaponAnimationManager INSTANCE = new WeaponAnimationManager();
    private static final Gson GSON = new Gson();

    private static class WeaponAnimContexts {
        final List<ResourceLocation> regularSwings = new ArrayList<>();
        final Map<String, ResourceLocation> chargeAnims = new HashMap<>();
        final Map<String, ResourceLocation> releaseAnims = new HashMap<>();
        final List<String> chargeOrderKeys = new ArrayList<>();
    }

    private final Map<WeaponType, WeaponAnimContexts> typeAnimationsMap = new EnumMap<>(WeaponType.class);

    public List<ResourceLocation> getRegularSwings(ResourceLocation itemId) {
        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return List.of();
        WeaponAnimContexts ctx = typeAnimationsMap.get(type);
        return ctx != null ? ctx.regularSwings : List.of();
    }

    public List<String> getChargeKeysOrder(ResourceLocation itemId) {
        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return List.of();
        WeaponAnimContexts ctx = typeAnimationsMap.get(type);
        return ctx != null ? ctx.chargeOrderKeys : List.of();
    }

    public ResourceLocation getChargeAnimation(ResourceLocation itemId, String key) {
        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return null;
        WeaponAnimContexts ctx = typeAnimationsMap.get(type);
        return ctx != null ? ctx.chargeAnims.get(key) : null;
    }

    public ResourceLocation getReleaseAnimation(ResourceLocation itemId, String key) {
        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return null;
        WeaponAnimContexts ctx = typeAnimationsMap.get(type);
        return ctx != null ? ctx.releaseAnims.get(key) : null;
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
            Map<WeaponType, WeaponAnimContexts> loaded = new EnumMap<>(WeaponType.class);
            Map<ResourceLocation, Resource> resources =
                    manager.listResources("weapon_animations", loc -> loc.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {

                    String path = entry.getKey().getPath();
                    String filename = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')).toUpperCase();

                    WeaponType type;
                    try {
                        type = WeaponType.valueOf(filename);
                    } catch (IllegalArgumentException e) {
                        System.err.println("[WeaponAnimations] Unknown WeaponType for file name: " + filename);
                        continue;
                    }

                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (!json.has("contexts")) continue;

                    JsonObject contextsObj = json.getAsJsonObject("contexts");
                    WeaponAnimContexts contexts = new WeaponAnimContexts();

                    // 1. Обычные взмахи
                    if (contextsObj.has("swing") && contextsObj.get("swing").isJsonArray()) {
                        for (JsonElement el : contextsObj.getAsJsonArray("swing")) {
                            contexts.regularSwings.add(ResourceLocation.parse(el.getAsString()));
                        }
                    }

                    // 2. Анимации замахов
                    if (contextsObj.has("swing_strike_animations") && contextsObj.get("swing_strike_animations").isJsonArray()) {
                        for (JsonElement el : contextsObj.getAsJsonArray("swing_strike_animations")) {
                            if (!el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();
                            for (Map.Entry<String, JsonElement> field : obj.entrySet()) {
                                String key = field.getKey();
                                ResourceLocation anim = ResourceLocation.parse(field.getValue().getAsString());
                                contexts.chargeAnims.put(key, anim);
                                contexts.chargeOrderKeys.add(key);
                            }
                        }
                    }

                    // 3. Анимации релизов (ударов) — теперь парсятся точно так же просто
                    if (contextsObj.has("swing_strike_release_animations") && contextsObj.get("swing_strike_release_animations").isJsonArray()) {
                        for (JsonElement el : contextsObj.getAsJsonArray("swing_strike_release_animations")) {
                            if (!el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();
                            for (Map.Entry<String, JsonElement> field : obj.entrySet()) {
                                String key = field.getKey();
                                ResourceLocation anim = ResourceLocation.parse(field.getValue().getAsString());
                                contexts.releaseAnims.put(key, anim);
                            }
                        }
                    }

                    loaded.put(type, contexts);

                } catch (Exception e) {
                    System.err.println("[WeaponAnimations] Failed to load " + entry.getKey());
                    e.printStackTrace();
                }
            }
            return loaded;
        }, prepExecutor).thenCompose(barrier::wait).thenAcceptAsync(loaded -> {
            typeAnimationsMap.clear();
            typeAnimationsMap.putAll(loaded);
        }, applyExecutor);
    }
}

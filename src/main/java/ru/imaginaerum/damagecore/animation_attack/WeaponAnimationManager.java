package ru.imaginaerum.damagecore.animation_attack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import ru.imaginaerum.damagecore.animation_attack.combat.resolvers.AttackShape;
import ru.imaginaerum.damagecore.library_damage.DamageType;
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

    /**
     * Одна анимация + опциональный тип урона, который она наносит.
     */
    public record AnimEntry(
            ResourceLocation animation,
            DamageType damageType,
            double damageMultiplier,
            AttackShape shape,
            double coneAngle,
            double reachBonus
    ) {
        public AnimEntry(ResourceLocation animation, DamageType damageType) {
            this(animation, damageType, 1.0, AttackShape.SINGLE, 30.0, 0.0);
        }
    }

    private static class WeaponAnimContexts {
        final List<AnimEntry> regularSwings = new ArrayList<>();
        final Map<String, AnimEntry> chargeAnims = new HashMap<>();
        final Map<String, AnimEntry> releaseAnims = new HashMap<>();
        final List<String> chargeOrderKeys = new ArrayList<>();
    }

    private final Map<WeaponType, WeaponAnimContexts> typeAnimationsMap = new EnumMap<>(WeaponType.class);

    public List<AnimEntry> getRegularSwings(ResourceLocation itemId) {
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

    public AnimEntry getChargeAnimation(ResourceLocation itemId, String key) {
        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return null;
        WeaponAnimContexts ctx = typeAnimationsMap.get(type);
        return ctx != null ? ctx.chargeAnims.get(key) : null;
    }

    public AnimEntry getReleaseAnimation(ResourceLocation itemId, String key) {
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
                            AnimEntry parsed = parseAnimEntry(el, entry.getKey());
                            if (parsed != null) contexts.regularSwings.add(parsed);
                        }
                    }

                    // 2. Анимации замахов
                    if (contextsObj.has("swing_strike_animations") && contextsObj.get("swing_strike_animations").isJsonArray()) {
                        for (JsonElement el : contextsObj.getAsJsonArray("swing_strike_animations")) {
                            parseKeyedEntry(el, entry.getKey(), (key, animEntry) -> {
                                contexts.chargeAnims.put(key, animEntry);
                                contexts.chargeOrderKeys.add(key);
                            });
                        }
                    }

                    // 3. Анимации релизов (ударов)
                    if (contextsObj.has("swing_strike_release_animations") && contextsObj.get("swing_strike_release_animations").isJsonArray()) {
                        for (JsonElement el : contextsObj.getAsJsonArray("swing_strike_release_animations")) {
                            parseKeyedEntry(el, entry.getKey(), (key, animEntry) ->
                                    contexts.releaseAnims.put(key, animEntry));
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

    /**
     * Парсит элемент вида:
     *   "damagecore:sword_swing_1"                                              (старый формат, без урона)
     *   { "animation": "damagecore:sword_swing_1", "damage_type": "slashing" }  (новый формат)
     */
    private static AnimEntry parseAnimEntry(JsonElement el, ResourceLocation source) {
        if (el.isJsonPrimitive()) {
            return new AnimEntry(ResourceLocation.parse(el.getAsString()), null);
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("animation")) {
                System.err.println("[WeaponAnimations] Missing 'animation' field in " + source);
                return null;
            }
            ResourceLocation anim = ResourceLocation.parse(obj.get("animation").getAsString());
            DamageType damageType = parseDamageType(obj, source);
            double multiplier = parseDamageMultiplier(obj, source);
            AttackShape shape = obj.has("attack_shape")
                    ? AttackShape.fromName(obj.get("attack_shape").getAsString())
                    : AttackShape.SINGLE;
            double coneAngle = obj.has("cone_angle")
                    ? obj.get("cone_angle").getAsDouble() : 90.0;
            double reachBonus = obj.has("reach_bonus")
                    ? obj.get("reach_bonus").getAsDouble() : 0.0;
            return new AnimEntry(anim, damageType, multiplier, shape, coneAngle, reachBonus);
        }
        return null;
    }

    /**
     * Парсит "ключевую" запись charge/release.
     * Поддерживает новый явный формат { "key": "...", "animation": "...", "damage_type": "..." }
     * и старый формат { "<key>": "<animation>" } (без урона, ключ = имя поля).
     */
    private static void parseKeyedEntry(JsonElement el, ResourceLocation source, java.util.function.BiConsumer<String, AnimEntry> consumer) {
        if (!el.isJsonObject()) return;
        JsonObject obj = el.getAsJsonObject();

        if (obj.has("key") && obj.has("animation")) {
            String key = obj.get("key").getAsString();
            ResourceLocation anim = ResourceLocation.parse(obj.get("animation").getAsString());
            DamageType damageType = parseDamageType(obj, source);
            double multiplier = parseDamageMultiplier(obj, source);
            AttackShape shape = obj.has("attack_shape")
                    ? AttackShape.fromName(obj.get("attack_shape").getAsString())
                    : AttackShape.SINGLE;
            double coneAngle = obj.has("cone_angle")
                    ? obj.get("cone_angle").getAsDouble() : 90.0;
            double reachBonus = obj.has("reach_bonus")
                    ? obj.get("reach_bonus").getAsDouble() : 0.0;
            consumer.accept(key, new AnimEntry(anim, damageType, multiplier, shape, coneAngle, reachBonus));
            return;
        }

        // Старый формат: произвольные поля key -> animation (строка), множитель недоступен -> 1.0
        for (Map.Entry<String, JsonElement> field : obj.entrySet()) {
            if ("damage_type".equals(field.getKey()) || "damage_multiplier".equals(field.getKey())) continue;
            String key = field.getKey();
            ResourceLocation anim = ResourceLocation.parse(field.getValue().getAsString());
            consumer.accept(key, new AnimEntry(anim, null));
        }
    }

    private static DamageType parseDamageType(JsonObject obj, ResourceLocation source) {
        if (!obj.has("damage_type")) return null;
        String raw = obj.get("damage_type").getAsString().toUpperCase();
        try {
            return DamageType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            System.err.println("[WeaponAnimations] Unknown damage_type '" + raw + "' in " + source);
            return null;
        }
    }
    private static double parseDamageMultiplier(JsonObject obj, ResourceLocation source) {
        if (!obj.has("damage_multiplier")) return 1.0;
        try {
            double value = obj.get("damage_multiplier").getAsDouble();
            if (value <= 0.0) {
                System.err.println("[WeaponAnimations] Invalid damage_multiplier '" + value + "' in " + source + ", falling back to 1.0");
                return 1.0;
            }
            return value;
        } catch (Exception e) {
            System.err.println("[WeaponAnimations] Malformed damage_multiplier in " + source + ", falling back to 1.0");
            return 1.0;
        }
    }
}
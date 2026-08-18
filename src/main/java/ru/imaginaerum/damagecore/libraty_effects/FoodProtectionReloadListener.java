package ru.imaginaerum.damagecore.libraty_effects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodProtectionReloadListener extends SimpleJsonResourceReloadListener {
    public static final Map<Item, List<Effect>> EFFECTS = new HashMap<>();

    public FoodProtectionReloadListener() {
        super(new Gson(), "food_protection");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
        EFFECTS.clear();

        // ДОБАВЛЕНО: entrySet() вместо values(), чтобы знать id файла для логов
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            // ДОБАВЛЕНО: try/catch вокруг всего парсинга одного файла —
            // раньше исключение (например из DamageType.valueOf) могло тихо
            // прервать обработку остальных файлов или вообще весь reload
            try {
                JsonObject json = entry.getValue().getAsJsonObject();

                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse(json.get("item").getAsString())
                );

                if (item == net.minecraft.world.item.Items.AIR) {
                    // ДОБАВЛЕНО: лог, если предмет не найден в реестре (опечатка / неверный namespace)
                    System.err.println("[FoodProtection] Skipped " + fileId
                            + ": item '" + json.get("item").getAsString() + "' not found in registry (AIR)");
                    continue;
                }

                List<Effect> effects = new ArrayList<>();
                JsonArray array = json.getAsJsonArray("effects");

                boolean overrideVanilla = json.has("override_vanilla_effects") && json.get("override_vanilla_effects").getAsBoolean();
                List<ResourceLocation> removeEffects = new ArrayList<>();
                if (json.has("remove_effects")) {
                    JsonArray rem = json.getAsJsonArray("remove_effects");
                    for (JsonElement r : rem) {
                        removeEffects.add(ResourceLocation.parse(r.getAsString()));
                    }
                }

                for (JsonElement e : array) {
                    JsonObject o = e.getAsJsonObject();
                    DamageType dtype = DamageType.valueOf(o.get("damage_type").getAsString());
                    float prot = o.get("protection").getAsFloat();
                    int duration = o.get("duration").getAsInt();

                    effects.add(new Effect(dtype, prot, duration, overrideVanilla, removeEffects));
                }

                EFFECTS.put(item, effects);

            } catch (Exception e) {
                // ДОБАВЛЕНО: лог конкретного битого файла вместо тихого падения
                System.err.println("[FoodProtection] Failed to parse " + fileId + ": " + e);
            }
        }

        // ДОБАВЛЕНО: итоговая сводка после загрузки — видно, сколько записей реально попало в EFFECTS
        System.out.println("[FoodProtection] Loaded " + EFFECTS.size() + " entries: "
                + EFFECTS.keySet().stream()
                .map(i -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).toString())
                .toList());
    }

    public record Effect(
            DamageType damageType,
            float protection,
            int duration,
            boolean overrideVanilla,
            List<ResourceLocation> removeEffects
    ) {}
}
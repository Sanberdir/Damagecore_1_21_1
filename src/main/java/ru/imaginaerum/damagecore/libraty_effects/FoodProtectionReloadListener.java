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
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, net.minecraft.util.profiling.ProfilerFiller profiler) {
        EFFECTS.clear();

        for (JsonElement element : jsons.values()) {
            JsonObject json = element.getAsJsonObject();

            // ИСПРАВЛЕНО: Вместо ForgeRegistries используем BuiltInRegistries, вместо new ResourceLocation — ResourceLocation.parse
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse(json.get("item").getAsString())
            );

            // В ванильном реестре .get() возвращает AIR, если предмет не найден, а не null
            if (item == net.minecraft.world.item.Items.AIR) continue;

            List<Effect> effects = new ArrayList<>();
            JsonArray array = json.getAsJsonArray("effects");

            boolean overrideVanilla = json.has("override_vanilla_effects") && json.get("override_vanilla_effects").getAsBoolean();
            List<ResourceLocation> removeEffects = new ArrayList<>();
            if (json.has("remove_effects")) {
                JsonArray rem = json.getAsJsonArray("remove_effects");
                for (JsonElement r : rem) {
                    // ИСПРАВЛЕНО: ResourceLocation.parse вместо конструктора
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
        }
    }


    public record Effect(
            DamageType damageType,
            float protection,
            int duration,
            boolean overrideVanilla,
            List<ResourceLocation> removeEffects
    ) {}
}

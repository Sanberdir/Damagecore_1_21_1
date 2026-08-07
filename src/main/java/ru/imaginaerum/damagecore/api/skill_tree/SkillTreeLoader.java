package ru.imaginaerum.damagecore.api.skill_tree;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLEnvironment;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SkillTreeLoader {
    private SkillTreeLoader() {}

    private static final Gson GSON = new Gson();

    // -------------------------------------------------------
    // Клиентский вариант (старый код, без изменений)
    // -------------------------------------------------------
    public static Object[] loadFromResource(String pathInNamespace) {
        ResourceManager rm = getResourceManager();
        if (rm == null) {
            System.err.println("[SkillTreeLoader] ResourceManager unavailable for: " + pathInNamespace);
            return new Object[]{ ItemStack.EMPTY, new ArrayList<>() };
        }
        return loadFromResource(pathInNamespace, rm);
    }

    // -------------------------------------------------------
    // Серверный вариант — принимает ResourceManager напрямую
    // -------------------------------------------------------
    public static Object[] loadFromResource(String pathInNamespace, ResourceManager resourceManager) {
        List<SkillTreeNode> result = new ArrayList<>();
        ItemStack tabIconStack = ItemStack.EMPTY;

        try {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("damagecore", pathInNamespace);
            Optional<Resource> optResource = resourceManager.getResource(rl);

            if (optResource.isEmpty()) {
                System.err.println("[SkillTreeLoader] Resource not found: " + pathInNamespace);
                return new Object[]{ tabIconStack, result };
            }

            Resource resource = optResource.get();
            try (InputStream is = resource.open();
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonElement rootEl = JsonParser.parseReader(reader);
                if (!rootEl.isJsonObject()) {
                    System.err.println("[SkillTreeLoader] Invalid JSON root in: " + pathInNamespace);
                    return new Object[]{ tabIconStack, result };
                }

                JsonObject root = rootEl.getAsJsonObject();

                if (root.has("tabIcon")) {
                    tabIconStack = parseItemStack(root.get("tabIcon"));
                }

                if (root.has("nodes") && root.get("nodes").isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray("nodes");
                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject obj = el.getAsJsonObject();

                        String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();

                        ItemStack itemStack = ItemStack.EMPTY;
                        if (obj.has("item")) itemStack = parseItemStack(obj.get("item"));

                        boolean lock = obj.has("lock") && obj.get("lock").getAsBoolean();

                        List<String> parentIds = new ArrayList<>();
                        if (obj.has("parents") && obj.get("parents").isJsonArray()) {
                            for (JsonElement pEl : obj.getAsJsonArray("parents")) {
                                if (pEl.isJsonPrimitive()) parentIds.add(pEl.getAsString());
                            }
                        } else if (obj.has("parent") && obj.get("parent").isJsonPrimitive()) {
                            parentIds.add(obj.get("parent").getAsString());
                        } else {
                            parentIds.add("start");
                        }

                        String sideStr = obj.has("side")
                                ? obj.get("side").getAsString().toUpperCase(Locale.ROOT) : "RIGHT";
                        SkillTreeNode.Side side;
                        try {
                            side = SkillTreeNode.Side.valueOf(sideStr);
                        } catch (Exception ex) {
                            side = SkillTreeNode.Side.RIGHT;
                        }

                        SkillTreeNode node = new SkillTreeNode(id, itemStack, lock, parentIds, side);

                        if (obj.has("maxLevel")) {
                            try {
                                node.setMaxLevel(Math.max(1, obj.get("maxLevel").getAsInt()));
                            } catch (Exception e) {
                                System.err.println("[SkillTreeLoader] Invalid maxLevel for node " + id);
                            }
                        }

                        if (obj.has("requiredTreeLevel")) {
                            try {
                                node.setRequiredTreeLevel(Math.max(0, obj.get("requiredTreeLevel").getAsInt()));
                            } catch (Exception e) {
                                System.err.println("[SkillTreeLoader] Invalid requiredTreeLevel for node " + id);
                            }
                        }

                        if (obj.has("level")) {
                            try {
                                node.setLevel(obj.get("level").getAsInt());
                            } catch (Exception e) {
                                System.err.println("[SkillTreeLoader] Invalid level for node " + id);
                            }
                        }

                        if (obj.has("gridX") && obj.has("gridY")) {
                            try {
                                node.setGridPos(
                                        obj.get("gridX").getAsInt(),
                                        obj.get("gridY").getAsInt()
                                );
                            } catch (Exception ignored) {}
                        }

                        if (obj.has("variants") && obj.get("variants").isJsonArray()) {
                            for (JsonElement ve : obj.getAsJsonArray("variants")) {
                                if (ve.isJsonObject()) {
                                    JsonObject vo = ve.getAsJsonObject();
                                    String variantId = vo.has("id")
                                            ? vo.get("id").getAsString()
                                            : UUID.randomUUID().toString();
                                    ItemStack variantStack = vo.has("item")
                                            ? parseItemStack(vo.get("item"))
                                            : ItemStack.EMPTY;
                                    if (!variantStack.isEmpty()) {
                                        node.variants.add(new SkillTreeNode.Variant(variantId, variantStack));
                                        node.options.add(variantStack);
                                    }
                                } else if (ve.isJsonPrimitive()) {
                                    String itemId = ve.getAsString();
                                    ItemStack variantStack = parseItemStackFromString(itemId);
                                    if (!variantStack.isEmpty()) {
                                        String variantId = itemId.replace(':', '_').replace('/', '_');
                                        node.variants.add(new SkillTreeNode.Variant(variantId, variantStack));
                                        node.options.add(variantStack);
                                    }
                                }
                            }
                        }

                        result.add(node);
                    }
                }
            }

        } catch (Exception ex) {
            System.err.println("[SkillTreeLoader] Failed to load: " + pathInNamespace + " — " + ex.getMessage());
            ex.printStackTrace();
        }

        return new Object[]{ tabIconStack, result };
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /**
     * Безопасно получает ResourceManager.
     * На сервере вернёт null — серверный код должен передавать RM явно.
     */
    private static ResourceManager getResourceManager() {
        try {
            if (FMLEnvironment.dist.isClient()) {
                return Minecraft.getInstance().getResourceManager();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static ItemStack parseItemStack(JsonElement element) {
        if (element == null || element.isJsonNull()) return ItemStack.EMPTY;

        if (element.isJsonPrimitive()) {
            return parseItemStackFromString(element.getAsString());
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("item")) return ItemStack.EMPTY;
            ItemStack stack = parseItemStackFromString(obj.get("item").getAsString());
            if (obj.has("count")) {
                try { stack.setCount(obj.get("count").getAsInt()); }
                catch (Exception ignored) {}
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack parseItemStackFromString(String itemId) {
        if (itemId == null || itemId.isBlank()) return ItemStack.EMPTY;
        try {
            // ИСПРАВЛЕНО: Вместо конструктора используем ResourceLocation.parse()
            ResourceLocation itemRL = ResourceLocation.parse(itemId);

            // ИСПРАВЛЕНО: Вместо ForgeRegistries используем ванильный реестр предметов
            Item maybe = BuiltInRegistries.ITEM.get(itemRL);

            // Ванильный реестр возвращает Items.AIR, если предмет не найден (вместо null)
            if (maybe != Items.AIR) {
                return new ItemStack(maybe);
            }

            System.err.println("[SkillTreeLoader] Unknown item: " + itemId);
            return new ItemStack(Items.BARRIER);
        } catch (Exception e) {
            System.err.println("[SkillTreeLoader] Failed to parse item: " + itemId + " — " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    public static List<SkillTreeNode> loadNodes(String pathInNamespace) {
        Object[] result = loadFromResource(pathInNamespace);
        if (result != null && result.length > 1 && result[1] instanceof List<?> list) {
            return (List<SkillTreeNode>) list;
        }
        return new ArrayList<>();
    }
}
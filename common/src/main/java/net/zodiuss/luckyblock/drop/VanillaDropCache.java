package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.zodiuss.luckyblock.LuckyBlock;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VanillaDropCache {
    private static final String DROPS_PATH = "drops";
    private static final Map<Identifier, LuckyDrop> DROPS_BY_ID = new LinkedHashMap<>();

    private VanillaDropCache() {}

    public static void reload(MinecraftServer server) {
        Map<Identifier, LuckyDrop> loaded = new LinkedHashMap<>();
        Map<Identifier, Resource> resources = server.getResourceManager()
                .listResources(DROPS_PATH, id -> id.getNamespace().equals(LuckyBlock.MOD_ID) && id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                LuckyDrop drop = readDrop(entry.getKey(), reader);
                loaded.put(drop.id(), drop);
            } catch (RuntimeException | IOException exception) {
                LuckyBlock.LOGGER.warn("Failed to load lucky drop {}", entry.getKey(), exception);
            }
        }
        synchronized (DROPS_BY_ID) {
            DROPS_BY_ID.clear();
            DROPS_BY_ID.putAll(loaded);
        }
        LuckyBlock.LOGGER.info("Loaded {} vanilla lucky drops", loaded.size());
    }

    public static void clear() {
        synchronized (DROPS_BY_ID) {
            DROPS_BY_ID.clear();
        }
    }

    public static List<LuckyDrop> getAllDrops() {
        synchronized (DROPS_BY_ID) {
            return List.copyOf(DROPS_BY_ID.values());
        }
    }

    public static List<LuckyDrop> getSelectableDrops() {
        synchronized (DROPS_BY_ID) {
            return DROPS_BY_ID.values().stream()
                    .filter(drop -> isDirectDropFile(drop.id()))
                    .toList();
        }
    }

    static boolean isDirectDropFile(Identifier id) {
        String path = id.getPath();
        if (!path.startsWith(DROPS_PATH + "/") || !path.endsWith(".json")) {
            return false;
        }
        String relativePath = path.substring(DROPS_PATH.length() + 1);
        return !relativePath.contains("/");
    }

    private static LuckyDrop readDrop(Identifier id, BufferedReader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        int luck = getRequiredInt(json, "luck", id);
        JsonElement drop = getRequired(json, "drop", id);
        double weight = json.has("weight") ? json.get("weight").getAsDouble() : 1.0;
        if (weight <= 0.0) {
            throw new IllegalArgumentException("Lucky drop " + id + " must have a positive weight");
        }
        return new LuckyDrop(id, weight, luck, drop);
    }

    private static JsonElement getRequired(JsonObject json, String key, Identifier id) {
        JsonElement element = json.get(key);
        if (element == null) {
            throw new IllegalArgumentException("Lucky drop " + id + " is missing required field '" + key + "'");
        }
        return element;
    }

    private static int getRequiredInt(JsonObject json, String key, Identifier id) {
        return getRequired(json, key, id).getAsInt();
    }
}

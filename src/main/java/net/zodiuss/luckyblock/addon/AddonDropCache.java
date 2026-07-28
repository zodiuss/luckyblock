package net.zodiuss.luckyblock.addon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.drop.LuckyDrop;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class AddonDropCache {
    private static final Map<String, List<LuckyDrop>> DROPS_BY_ADDON = new LinkedHashMap<>();
    private static final Map<Identifier, LuckyDrop> DROPS_BY_ID = new LinkedHashMap<>();

    private AddonDropCache() {
    }

    public static void reload(List<LuckyAddon> addons) {
        clear();

        for (LuckyAddon addon : addons) {
            List<LuckyDrop> drops = loadAddonDrops(addon);
            DROPS_BY_ADDON.put(addon.config().identifier(), drops);
            for (LuckyDrop drop : drops) {
                DROPS_BY_ID.put(drop.id(), drop);
            }
        }
    }

    public static void clear() {
        DROPS_BY_ADDON.clear();
        DROPS_BY_ID.clear();
    }

    public static List<LuckyDrop> getSelectableDrops(String addonIdentifier) {
        return DROPS_BY_ADDON.getOrDefault(addonIdentifier, List.of()).stream()
                .filter(AddonDropCache::isDirectDropFile)
                .toList();
    }

    public static List<LuckyDrop> getAllDrops() {
        return List.copyOf(DROPS_BY_ID.values());
    }

    public static LuckyDrop getDrop(Identifier id) {
        return DROPS_BY_ID.get(id);
    }

    private static List<LuckyDrop> loadAddonDrops(LuckyAddon addon) {
        Path dropsDirectory = addon.dropsDirectory();
        if (!Files.isDirectory(dropsDirectory)) {
            return List.of();
        }

        List<LuckyDrop> drops = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dropsDirectory)) {
            for (Path file : files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json")).toList()) {
                try {
                    drops.add(readDrop(addon, dropsDirectory, file));
                } catch (RuntimeException | IOException exception) {
                    LuckyBlock.LOGGER.warn("Failed to load addon drop {} for {}", file, addon.config().identifier(), exception);
                }
            }
        } catch (IOException exception) {
            LuckyBlock.LOGGER.warn("Failed to walk addon drops directory {}", dropsDirectory, exception);
        }

        return Collections.unmodifiableList(drops);
    }

    private static LuckyDrop readDrop(LuckyAddon addon, Path dropsDirectory, Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int luck = getRequiredInt(json, "luck", file);
            JsonElement drop = getRequired(json, "drop", file);
            double weight = json.has("weight") ? json.get("weight").getAsDouble() : 1.0;

            if (weight <= 0.0) {
                throw new IllegalArgumentException("Addon drop " + file + " must have a positive weight");
            }

            Path relativePath = dropsDirectory.relativize(file);
            String dropPath = relativePath.toString().replace('\\', '/');
            if (dropPath.endsWith(".json")) {
                dropPath = dropPath.substring(0, dropPath.length() - ".json".length());
            }

            Identifier id = Identifier.fromNamespaceAndPath(
                    LuckyBlock.MOD_ID,
                    "addons/" + addon.config().identifier() + "/drops/" + dropPath
            );
            return new LuckyDrop(id, weight, luck, drop);
        }
    }

    private static JsonElement getRequired(JsonObject json, String key, Path file) {
        JsonElement element = json.get(key);
        if (element == null) {
            throw new IllegalArgumentException("Addon drop " + file + " is missing required field '" + key + "'");
        }

        return element;
    }

    private static int getRequiredInt(JsonObject json, String key, Path file) {
        return getRequired(json, key, file).getAsInt();
    }

    private static boolean isDirectDropFile(LuckyDrop drop) {
        String path = drop.id().getPath();
        String prefix = "addons/";
        int dropsIndex = path.indexOf("/drops/");
        if (!path.startsWith(prefix) || dropsIndex < 0) {
            return false;
        }

        String relativeDropPath = path.substring(dropsIndex + "/drops/".length());
        return !relativeDropPath.contains("/");
    }
}

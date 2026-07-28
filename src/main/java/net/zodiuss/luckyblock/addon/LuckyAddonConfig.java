package net.zodiuss.luckyblock.addon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record LuckyAddonConfig(String name, String identifier, String texture, String dropsDirectory) {
    private static final String CONFIG_FILE = "lucky.config.json";

    public static LuckyAddonConfig parse(Path addonRoot) throws IOException {
        Path configPath = addonRoot.resolve(CONFIG_FILE);
        if (!Files.isRegularFile(configPath)) {
            throw new IOException("Missing " + CONFIG_FILE + " in " + addonRoot);
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String name = requireString(json, "name");
            String identifier = requireString(json, "identifier");
            String dropsDirectory = requireString(json, "drops");

            if (!json.has("appearance") || !json.get("appearance").isJsonObject()) {
                throw new IOException("lucky.config.json in " + addonRoot + " is missing appearance object");
            }

            JsonObject appearance = json.getAsJsonObject("appearance");
            String texture = requireString(appearance, "texture");
            return new LuckyAddonConfig(name, identifier, texture, dropsDirectory);
        }
    }

    private static String requireString(JsonObject json, String key) throws IOException {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IOException("lucky.config.json is missing required string field '" + key + "'");
        }

        String value = json.get(key).getAsString().trim();
        if (value.isEmpty()) {
            throw new IOException("lucky.config.json field '" + key + "' must not be empty");
        }

        return value;
    }
}

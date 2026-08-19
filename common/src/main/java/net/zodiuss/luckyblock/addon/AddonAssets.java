package net.zodiuss.luckyblock.addon;

import net.minecraft.resources.Identifier;
import net.zodiuss.luckyblock.LuckyBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class AddonAssets {
    private static final List<String> LEGACY_ASSET_PREFIXES = List.of(
            "models/",
            "textures/",
            "blockstates/",
            "items/",
            "lang/"
    );

    private AddonAssets() {
    }

    public static Map<String, Path> collectPackAssets(LuckyAddon addon) throws IOException {
        Path assetsRoot = addon.root().resolve("assets");
        if (!Files.isDirectory(assetsRoot)) {
            return Map.of();
        }

        Map<String, Path> assets = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(assetsRoot)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String relative = assetsRoot.relativize(file).toString().replace('\\', '/');
                Optional<String> resourcePath = remapToResourcePath(relative);
                if (resourcePath.isPresent()) {
                    assets.put(resourcePath.get(), file);
                }
            }
        }

        return assets;
    }

    public static Optional<String> remapToResourcePath(String relativeFromAssets) {
        if (relativeFromAssets.startsWith(LuckyBlock.MOD_ID + "/")) {
            return Optional.of("assets/" + relativeFromAssets);
        }

        for (String prefix : LEGACY_ASSET_PREFIXES) {
            if (relativeFromAssets.startsWith(prefix)) {
                return Optional.of("assets/" + LuckyBlock.MOD_ID + "/" + relativeFromAssets);
            }
        }

        return Optional.empty();
    }

    public static Optional<Identifier> resolveBlockModelId(Map<String, ?> assets, String blockId) {
        return resolveModelId(assets, "assets/" + LuckyBlock.MOD_ID + "/models/block/", blockId);
    }

    public static Optional<Identifier> resolveItemModelId(Map<String, ?> assets, String blockId) {
        return resolveModelId(assets, "assets/" + LuckyBlock.MOD_ID + "/models/item/", blockId);
    }

    private static Optional<Identifier> resolveModelId(Map<String, ?> assets, String prefix, String blockId) {
        List<String> modelNames = new ArrayList<>();

        for (String resourcePath : assets.keySet()) {
            if (!resourcePath.startsWith(prefix) || !resourcePath.endsWith(".json")) {
                continue;
            }

            modelNames.add(resourcePath.substring(prefix.length(), resourcePath.length() - ".json".length()));
        }

        if (modelNames.isEmpty()) {
            return Optional.empty();
        }

        modelNames.sort(Comparator.naturalOrder());

        String preferred = "lucky_block_" + blockId;
        if (modelNames.contains(preferred)) {
            return Optional.of(modelIdFromFolder(prefix, preferred));
        }

        if (modelNames.contains(blockId)) {
            return Optional.of(modelIdFromFolder(prefix, blockId));
        }

        if (modelNames.size() == 1) {
            return Optional.of(modelIdFromFolder(prefix, modelNames.getFirst()));
        }

        for (String modelName : modelNames) {
            if (modelName.contains(blockId)) {
                return Optional.of(modelIdFromFolder(prefix, modelName));
            }
        }

        return Optional.of(modelIdFromFolder(prefix, modelNames.getFirst()));
    }

    private static Identifier modelIdFromFolder(String assetPrefix, String modelName) {
        String folder = assetPrefix.substring(("assets/" + LuckyBlock.MOD_ID + "/models/").length(), assetPrefix.length() - 1);
        return Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, folder + "/" + modelName);
    }

    public static boolean hasResource(Map<String, ?> assets, String resourcePath) {
        return assets.containsKey(resourcePath);
    }
}

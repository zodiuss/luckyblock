package net.zodiuss.luckyblock.addon;

import net.zodiuss.luckyblock.LuckyBlock;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class AddonLoader {
    public static final String ADDONS_DIRECTORY = "addons";
    private static final String CONFIG_FILE = "lucky.config.json";

    private AddonLoader() {
    }

    public static List<LuckyAddon> discover(Path addonsDirectory) {
        List<LuckyAddon> addons = new ArrayList<>();
        if (!Files.isDirectory(addonsDirectory)) {
            return addons;
        }

        Map<String, Path> folderAddons = new HashMap<>();
        try (Stream<Path> entries = Files.list(addonsDirectory)) {
            for (Path entry : entries.toList()) {
                String fileName = entry.getFileName().toString();
                if (Files.isDirectory(entry)) {
                    loadFolderAddon(entry, fileName, addons, folderAddons);
                } else if (fileName.endsWith(".zip")) {
                    loadZipAddon(entry, fileName, addons, folderAddons);
                }
            }
        } catch (IOException exception) {
            LuckyBlock.LOGGER.warn("Failed to scan addons directory {}", addonsDirectory, exception);
        }

        return addons;
    }

    private static void loadFolderAddon(Path entry, String sourceName, List<LuckyAddon> addons, Map<String, Path> folderAddons) {
        Path configPath = entry.resolve(CONFIG_FILE);
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        try {
            LuckyAddonConfig config = LuckyAddonConfig.parse(entry);
            registerAddon(new LuckyAddon(sourceName, entry, null, config), addons, folderAddons);
        } catch (IOException exception) {
            LuckyBlock.LOGGER.warn("Failed to load lucky addon folder '{}'", sourceName, exception);
        } catch (RuntimeException exception) {
            LuckyBlock.LOGGER.warn("Failed to load lucky addon folder '{}'", sourceName, exception);
        }
    }

    private static void loadZipAddon(Path zipPath, String sourceName, List<LuckyAddon> addons, Map<String, Path> folderAddons) {
        try {
            FileSystem zipFileSystem = FileSystems.newFileSystem(zipPath, Map.of());
            Path root = zipFileSystem.getPath("/");
            if (!Files.isRegularFile(root.resolve(CONFIG_FILE))) {
                zipFileSystem.close();
                return;
            }

            LuckyAddonConfig config = LuckyAddonConfig.parse(root);
            registerAddon(new LuckyAddon(sourceName, root, zipFileSystem, config), addons, folderAddons);
        } catch (IOException exception) {
            LuckyBlock.LOGGER.warn("Failed to load lucky addon archive '{}'", sourceName, exception);
        } catch (RuntimeException exception) {
            LuckyBlock.LOGGER.warn("Failed to load lucky addon archive '{}'", sourceName, exception);
        }
    }

    private static void registerAddon(LuckyAddon addon, List<LuckyAddon> addons, Map<String, Path> folderAddons) throws IOException {
        String identifier = addon.config().identifier();
        if (!identifier.matches("[a-z0-9_]+")) {
            throw new IOException("Addon identifier '" + identifier + "' must use lowercase letters, numbers, and underscores");
        }

        if (!Files.isDirectory(addon.dropsDirectory())) {
            LuckyBlock.LOGGER.warn(
                    "Lucky addon '{}' ({}) has no drops directory at {}",
                    addon.config().name(),
                    identifier,
                    addon.dropsDirectory()
            );
        }

        if (folderAddons.containsKey(identifier)) {
            throw new IOException("Duplicate lucky addon identifier '" + identifier + "' from " + addon.sourceName());
        }

        addon.resolveTexturePath();
        folderAddons.put(identifier, addon.root());
        addons.add(addon);
        LuckyBlock.LOGGER.info(
                "Loaded lucky addon '{}' as {} from {}",
                addon.config().name(),
                addon.blockId(),
                addon.sourceName()
        );
    }
}

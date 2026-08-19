package net.zodiuss.luckyblock.addon;

import net.minecraft.world.level.block.Block;
import net.zodiuss.luckyblock.LuckyBlock;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AddonRegistry {
    private static final List<LuckyAddon> ADDONS = new ArrayList<>();
    private static final Map<String, LuckyAddon> ADDONS_BY_ID = new LinkedHashMap<>();
    private static final Map<Block, LuckyAddon> ADDONS_BY_BLOCK = new LinkedHashMap<>();

    private AddonRegistry() {
    }

    /** Loader-agnostic: caller passes the game directory */
    public synchronized static void loadFromGameDirectory(Path gameDir) {
        if (gameDir == null) {
            loadFromDirectory(null);
            return;
        }
        Path addonsDirectory = gameDir.resolve(AddonLoader.ADDONS_DIRECTORY);
        loadFromDirectory(addonsDirectory);
    }

    /** Direct directory version - used by loaders and tests */
    public synchronized static void loadFromDirectory(Path addonsDirectory) {
        closeAddons();
        if (addonsDirectory == null || !Files.isDirectory(addonsDirectory)) {
            // Try to discover anyway if null -> empty list (keeps previous behavior for tests)
            if (addonsDirectory == null) {
                return;
            }
            return;
        }
        List<LuckyAddon> discovered = AddonLoader.discover(addonsDirectory);
        ADDONS.addAll(discovered);

        for (LuckyAddon addon : discovered) {
            ADDONS_BY_ID.put(addon.config().identifier(), addon);
        }
    }

    /** Backwards-compatible no-arg: tries current working directory's `addons` folder - used only in tests */
    public synchronized static void loadFromGameDirectory() {
        Path cwd = Path.of("").toAbsolutePath();
        Path addonsDirectory = cwd.resolve(AddonLoader.ADDONS_DIRECTORY);
        if (Files.isDirectory(addonsDirectory)) {
            loadFromDirectory(addonsDirectory);
        } else {
            // fallback: search `run/addons` like loader runs
            Path runAddons = cwd.resolve("run").resolve(AddonLoader.ADDONS_DIRECTORY);
            if (Files.isDirectory(runAddons)) {
                loadFromDirectory(runAddons);
            } else {
                closeAddons();
            }
        }
    }

    public synchronized static void bindBlock(LuckyAddon addon, Block block) {
        addon.setBlock(block);
        ADDONS_BY_BLOCK.put(block, addon);
    }

    public synchronized static List<LuckyAddon> getAddons() {
        return Collections.unmodifiableList(ADDONS);
    }

    public synchronized static Optional<LuckyAddon> getAddon(String identifier) {
        return Optional.ofNullable(ADDONS_BY_ID.get(identifier));
    }

    public synchronized static Optional<LuckyAddon> getAddonForBlock(@Nullable Block block) {
        if (block == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(ADDONS_BY_BLOCK.get(block));
    }

    public synchronized static void reloadDrops() {
        AddonDropCache.reload(ADDONS);
    }

    private synchronized static void closeAddons() {
        for (LuckyAddon addon : ADDONS) {
            try {
                addon.close();
            } catch (IOException exception) {
                LuckyBlock.LOGGER.warn("Failed to close lucky addon '{}'", addon.config().identifier(), exception);
            }
        }

        ADDONS.clear();
        ADDONS_BY_ID.clear();
        ADDONS_BY_BLOCK.clear();
        AddonDropCache.clear();
    }
}

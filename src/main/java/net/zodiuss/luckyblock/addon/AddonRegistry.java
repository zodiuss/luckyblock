package net.zodiuss.luckyblock.addon;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Block;
import net.zodiuss.luckyblock.LuckyBlock;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
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

    public synchronized static void loadFromGameDirectory() {
        closeAddons();
        Path addonsDirectory = FabricLoader.getInstance().getGameDir().resolve(AddonLoader.ADDONS_DIRECTORY);
        List<LuckyAddon> discovered = AddonLoader.discover(addonsDirectory);
        ADDONS.addAll(discovered);

        for (LuckyAddon addon : discovered) {
            ADDONS_BY_ID.put(addon.config().identifier(), addon);
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

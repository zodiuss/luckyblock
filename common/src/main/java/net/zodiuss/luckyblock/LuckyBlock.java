package net.zodiuss.luckyblock;

import net.minecraft.resources.ResourceLocation;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.block.entity.ModBlockEntities;
import net.zodiuss.luckyblock.component.ModComponents;
import net.zodiuss.luckyblock.creativemodetab.ModTabs;
import net.zodiuss.luckyblock.recipe.ModRecipes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class LuckyBlock {
    public static final String MOD_ID = "lucky";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private LuckyBlock() {}

    /**
     * Common initialization for Fabric (does registries directly).
     * @param gameDir the game directory - passed by loader, used to discover addons.
     *                May be null in tests.
     */
    public static void init(Path gameDir) {
        LOGGER.info("Initializing Lucky Block Mod (common)");

        ModComponents.register();
        ModRecipes.register();
        if (gameDir != null) {
            AddonRegistry.loadFromGameDirectory(gameDir);
        } else {
            AddonRegistry.loadFromGameDirectory();
        }
        LuckyBlocks.register();
        ModBlockEntities.register();
        ModTabs.register();
    }

    /**
     * NeoForge pre-registry init: only loads addons, does not touch registries.
     * Registries are handled via DeferredRegister on the mod event bus.
     */
    public static void initPreRegistry(Path gameDir) {
        LOGGER.info("Initializing Lucky Block Mod (common pre-registry)");
        if (gameDir != null) {
            AddonRegistry.loadFromGameDirectory(gameDir);
        } else {
            AddonRegistry.loadFromGameDirectory();
        }
    }

    /** Backwards-compatible no-arg for tests or direct calls */
    public static void init() {
        init(null);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}

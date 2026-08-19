package net.zodiuss.luckyblock;

import net.minecraft.server.MinecraftServer;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.drop.VanillaDropCache;
import net.zodiuss.luckyblock.structure.LuckyStructureRegistry;

/**
 * Central facade for server-bound caches. Encapsulates the global static
 * state behind a single reload entry point so callers do not directly
 * touch individual registries' static maps. All mutable state remains
 * server-thread confined but is now coordinated.
 */
public final class LuckyCacheManager {
    private LuckyCacheManager() {}

    public static void reloadAll(MinecraftServer server) {
        AddonRegistry.reloadDrops();
        VanillaDropCache.reload(server);
        LuckyStructureRegistry.reload(server);
    }

    public static void reloadDrops(MinecraftServer server) {
        AddonRegistry.reloadDrops();
        VanillaDropCache.reload(server);
    }
}

package net.zodiuss.luckyblock.structure;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.zodiuss.luckyblock.LuckyBlock;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LuckyStructureRegistry {
    private static final String STRUCTURES_FOLDER = "structures";
    private static final String STRUCTURES_PATH_PREFIX = STRUCTURES_FOLDER + "/";

    private static volatile Map<String, LuckyStructureDefinition> templates = Map.of();

    private LuckyStructureRegistry() {
    }

    public static synchronized void reload(MinecraftServer server) {
        ResourceManager resourceManager = server.getResourceManager();
        HolderGetter<Block> blockLookup = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        Map<String, LuckyStructureDefinition> loaded = new HashMap<>();

        Map<Identifier, Resource> resources = resourceManager.listResources(
                STRUCTURES_FOLDER,
                id -> id.getNamespace().equals(LuckyBlock.MOD_ID) && id.getPath().endsWith(".nbt")
        );

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            String path = entry.getKey().getPath();
            if (!path.startsWith(STRUCTURES_PATH_PREFIX)) {
                continue;
            }

            String file = path.substring(STRUCTURES_PATH_PREFIX.length());
            try (InputStream stream = entry.getValue().open()) {
                StructureTemplate template = new StructureTemplate();
                template.load(blockLookup, NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap()));
                loaded.put(file, new LuckyStructureDefinition(file, template));
            } catch (IOException exception) {
                LuckyBlock.LOGGER.warn("Failed to load structure NBT {}", entry.getKey(), exception);
            }
        }

        templates = Map.copyOf(loaded);
        LuckyBlock.LOGGER.info("Loaded {} lucky structure templates", templates.size());
    }

    public static synchronized boolean isEmpty() {
        return templates.isEmpty();
    }

    public static synchronized Optional<LuckyStructureDefinition> find(String file) {
        return Optional.ofNullable(templates.get(normalizeFile(file)));
    }

    public static String normalizeFile(String file) {
        file = file.trim();
        if (file.contains(":")) {
            file = file.substring(file.lastIndexOf(':') + 1);
        }
        if (!file.endsWith(".nbt")) {
            file = file + ".nbt";
        }
        return file;
    }
}

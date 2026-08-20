package net.zodiuss.luckyblock.client.addon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.addon.AddonAssets;
import net.zodiuss.luckyblock.addon.LuckyAddon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AddonPackResources extends AbstractPackResources {
    private final LuckyAddon addon;
    private final PathPackResources delegate;
    private final Map<String, byte[]> generatedResources = new HashMap<>();

    public AddonPackResources(PackLocationInfo location, LuckyAddon addon) {
        super(location);
        this.addon = addon;
        this.delegate = new PathPackResources(location, addon.root());
        buildGeneratedResources();
    }

    private void buildGeneratedResources() {
        try {
            embedPackAssets();
        } catch (IOException exception) {
            LuckyBlock.LOGGER.warn("Failed to embed addon assets for {}", addon.blockId(), exception);
        }

        String blockId = addon.config().identifier();
        String textureId = addon.textureId();
        String displayName = addon.config().name();
        ResourceLocation blockModelId = resolveBlockModelId(blockId, textureId);
        String blockModelReference = blockModelId.getNamespace() + ":" + blockModelId.getPath();
        ResourceLocation itemModelId = resolveItemModelId(blockId, blockModelId);
        String itemModelReference = itemModelId.getNamespace() + ":" + itemModelId.getPath();

        String blockstatePath = "assets/" + LuckyBlock.MOD_ID + "/blockstates/" + blockId + ".json";
        if (!AddonAssets.hasResource(generatedResources, blockstatePath)) {
            putGenerated(blockstatePath, """
                    {
                      "multipart": [
                        {
                          "apply": {
                            "model": "%s"
                          }
                        }
                      ]
                    }
                    """.formatted(blockModelReference));
        }

        String itemPath = "assets/" + LuckyBlock.MOD_ID + "/items/" + blockId + ".json";
        if (!AddonAssets.hasResource(generatedResources, itemPath)) {
            putGenerated(itemPath, """
                    {
                      "model": {
                        "type": "minecraft:model",
                        "model": "%s"
                      }
                    }
                    """.formatted(itemModelReference));
        }

        String langPath = "assets/" + LuckyBlock.MOD_ID + "/lang/en_us.json";
        if (!AddonAssets.hasResource(generatedResources, langPath)) {
            putGenerated(langPath, """
                    {
                      "block.%s.%s": "%s",
                      "item.%s.%s": "%s"
                    }
                    """.formatted(
                    LuckyBlock.MOD_ID, blockId, escapeJson(displayName),
                    LuckyBlock.MOD_ID, blockId, escapeJson(displayName)
            ));
        }
    }

    private void embedPackAssets() throws IOException {
        for (Map.Entry<String, Path> entry : AddonAssets.collectPackAssets(addon).entrySet()) {
            putGeneratedFile(entry.getKey(), entry.getValue());
        }
    }

    private ResourceLocation resolveBlockModelId(String blockId, String textureId) {
        Optional<ResourceLocation> packModel = AddonAssets.resolveBlockModelId(generatedResources, blockId);
        if (packModel.isPresent()) {
            return packModel.get();
        }

        String defaultModelPath = "assets/" + LuckyBlock.MOD_ID + "/models/block/" + blockId + ".json";
        putGenerated(defaultModelPath, """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": {
                    "all": "%s:%s"
                  }
                }
                """.formatted(LuckyBlock.MOD_ID, textureId));

        return ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "block/" + blockId);
    }

    private ResourceLocation resolveItemModelId(String blockId, ResourceLocation blockModelId) {
        Optional<ResourceLocation> packModel = AddonAssets.resolveItemModelId(generatedResources, blockId);
        if (packModel.isPresent()) {
            return packModel.get();
        }

        String itemModelName = blockModelId.getPath().substring("block/".length());
        String itemModelPath = "assets/" + LuckyBlock.MOD_ID + "/models/item/" + itemModelName + ".json";
        if (!AddonAssets.hasResource(generatedResources, itemModelPath)) {
            putGenerated(itemModelPath, defaultItemModelJson(blockModelReference(blockModelId)));
        }

        return ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "item/" + itemModelName);
    }

    private static String blockModelReference(ResourceLocation blockModelId) {
        return blockModelId.getNamespace() + ":" + blockModelId.getPath();
    }

    private static String defaultItemModelJson(String blockModelReference) {
        return """
                {
                  "parent": "%s",
                  "display": {
                    "thirdperson_righthand": {
                      "rotation": [ 75, 45, 0 ],
                      "translation": [ 0, 2.5, 0 ],
                      "scale": [ 0.375, 0.375, 0.375 ]
                    },
                    "thirdperson_lefthand": {
                      "rotation": [ 75, 45, 0 ],
                      "translation": [ 0, 2.5, 0 ],
                      "scale": [ 0.375, 0.375, 0.375 ]
                    },
                    "firstperson_righthand": {
                      "rotation": [ 0, 45, 0 ],
                      "translation": [ 0, 0, 0 ],
                      "scale": [ 0.4, 0.4, 0.4 ]
                    },
                    "firstperson_lefthand": {
                      "rotation": [ 0, 225, 0 ],
                      "translation": [ 0, 0, 0 ],
                      "scale": [ 0.4, 0.4, 0.4 ]
                    },
                    "ground": {
                      "translation": [ 0, 3, 0 ],
                      "scale": [ 0.25, 0.25, 0.25 ]
                    },
                    "gui": {
                      "rotation": [ 30, 225, 0 ],
                      "scale": [ 0.625, 0.625, 0.625 ]
                    },
                    "head": {
                      "rotation": [ 0, 180, 0 ],
                      "scale": [ 1, 1, 1 ]
                    },
                    "fixed": {
                      "rotation": [ 0, 180, 0 ],
                      "scale": [ 0.5, 0.5, 0.5 ]
                    }
                  }
                }
                """.formatted(blockModelReference);
    }

    private void putGeneratedFile(String resourcePath, Path sourcePath) throws IOException {
        generatedResources.put(resourcePath, Files.readAllBytes(sourcePath));
    }

    private void putGenerated(String path, String contents) {
        generatedResources.put(path, contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... pathSegments) {
        return delegate.getRootResource(pathSegments);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        if (packType == PackType.CLIENT_RESOURCES && LuckyBlock.MOD_ID.equals(location.getNamespace())) {
            String resourcePath = "assets/" + location.getNamespace() + "/" + location.getPath();
            byte[] generated = generatedResources.get(resourcePath);
            if (generated == null && !hasKnownExtension(location.getPath())) {
                generated = generatedResources.get(resourcePath + ".json");
            }
            if (generated != null) {
                byte[] resourceBytes = generated;
                return () -> new ByteArrayInputStream(resourceBytes);
            }
        }

        return delegate.getResource(packType, location);
    }

    private static boolean hasKnownExtension(String path) {
        return path.endsWith(".json") || path.endsWith(".png") || path.endsWith(".mcmeta");
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        delegate.listResources(packType, namespace, path, resourceOutput);

        if (packType != PackType.CLIENT_RESOURCES || !LuckyBlock.MOD_ID.equals(namespace)) {
            return;
        }

        String prefix = "assets/" + namespace + "/" + path;
        for (Map.Entry<String, byte[]> entry : generatedResources.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }

            String remainder = entry.getKey().substring(prefix.length());
            if (remainder.startsWith("/")) {
                remainder = remainder.substring(1);
            }

            resourceOutput.accept(ResourceLocation.fromNamespaceAndPath(namespace, path + "/" + remainder), () -> new ByteArrayInputStream(entry.getValue()));
        }
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        Set<String> namespaces = new HashSet<>(delegate.getNamespaces(packType));
        if (packType == PackType.CLIENT_RESOURCES) {
            namespaces.add(LuckyBlock.MOD_ID);
        }
        return namespaces;
    }

    @Override
    public void close() {
        delegate.close();
    }
}

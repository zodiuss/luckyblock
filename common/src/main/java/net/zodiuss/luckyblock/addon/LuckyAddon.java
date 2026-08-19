package net.zodiuss.luckyblock.addon;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.zodiuss.luckyblock.LuckyBlock;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class LuckyAddon implements AutoCloseable {
    private final String sourceName;
    private final Path root;
    private final @Nullable FileSystem zipFileSystem;
    private final LuckyAddonConfig config;
    private final Path dropsDirectory;
    private final Identifier blockId;
    private @Nullable Block block;

    public LuckyAddon(String sourceName, Path root, @Nullable FileSystem zipFileSystem, LuckyAddonConfig config) {
        this.sourceName = sourceName;
        this.root = root;
        this.zipFileSystem = zipFileSystem;
        this.config = config;
        this.dropsDirectory = root.resolve(config.dropsDirectory()).normalize();
        this.blockId = Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, config.identifier());
    }

    public String sourceName() {
        return sourceName;
    }

    public Path root() {
        return root;
    }

    public LuckyAddonConfig config() {
        return config;
    }

    public Path dropsDirectory() {
        return dropsDirectory;
    }

    public Identifier blockId() {
        return blockId;
    }

    public Identifier dropsSourceId() {
        return Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "addons/" + config.identifier());
    }

    public String packId() {
        return "lucky_addon/" + config.identifier();
    }

    public @Nullable Block block() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public Path resolveTexturePath() throws IOException {
        String texture = config.texture().replace('\\', '/');
        if (texture.startsWith("/")) {
            texture = texture.substring(1);
        }

        Path namespaced = root.resolve("assets").resolve(LuckyBlock.MOD_ID).resolve("textures").resolve(texture);
        if (Files.isRegularFile(namespaced)) {
            return namespaced;
        }

        Path legacy = root.resolve("assets").resolve("textures").resolve(texture);
        if (Files.isRegularFile(legacy)) {
            return legacy;
        }

        throw new IOException("Could not find addon texture '" + config.texture() + "' in " + root);
    }

    public Optional<Path> resolveTextureMetadataPath() throws IOException {
        Path texturePath = resolveTexturePath();
        Path siblingMetadata = texturePath.resolveSibling(texturePath.getFileName().toString() + ".mcmeta");
        if (Files.isRegularFile(siblingMetadata)) {
            return Optional.of(siblingMetadata);
        }

        String texture = config.texture().replace('\\', '/');
        if (texture.startsWith("/")) {
            texture = texture.substring(1);
        }

        Path namespaced = root.resolve("assets")
                .resolve(LuckyBlock.MOD_ID)
                .resolve("textures")
                .resolve(texture + ".mcmeta");
        if (Files.isRegularFile(namespaced)) {
            return Optional.of(namespaced);
        }

        Path legacy = root.resolve("assets").resolve("textures").resolve(texture + ".mcmeta");
        if (Files.isRegularFile(legacy)) {
            return Optional.of(legacy);
        }

        return Optional.empty();
    }

    public String textureId() {
        String texture = config.texture().replace('\\', '/');
        if (texture.endsWith(".png")) {
            texture = texture.substring(0, texture.length() - 4);
        }
        return texture;
    }

    @Override
    public void close() throws IOException {
        if (zipFileSystem != null) {
            zipFileSystem.close();
        }
    }
}

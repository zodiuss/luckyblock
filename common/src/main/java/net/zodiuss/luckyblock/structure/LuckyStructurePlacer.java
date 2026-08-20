package net.zodiuss.luckyblock.structure;

import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.NopProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.component.StructureAnchor;
import net.zodiuss.luckyblock.drop.DropAnchor;
import net.zodiuss.luckyblock.drop.StructureCoords;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class LuckyStructurePlacer {
    private LuckyStructurePlacer() {
    }

    public static void place(
            JsonObject structure,
            MinecraftServer server,
            ServerLevel level,
            BlockPos blockPos,
            @Nullable Player player,
            RandomSource random,
            ResourceLocation dropId,
            String jsonPath
    ) {
        place(structure, server, level, blockPos, player, random, dropId, jsonPath, StructureAnchor.EMPTY);
    }

    public static void place(
            JsonObject structure,
            MinecraftServer server,
            ServerLevel level,
            BlockPos blockPos,
            @Nullable Player player,
            RandomSource random,
            ResourceLocation dropId,
            String jsonPath,
            StructureAnchor structureAnchor
    ) {
        ensureLoaded(server);

        String file = LuckyStructureRegistry.normalizeFile(
                structure.has("type") ? structure.get("type").getAsString() : structure.get("id").getAsString()
        );

        Optional<LuckyStructureDefinition> maybeDefinition = LuckyStructureRegistry.find(file);
        if (maybeDefinition.isEmpty()) {
            LuckyBlock.LOGGER.warn("Lucky drop {} could not find structure file '{}' at {}", dropId, file, jsonPath);
            return;
        }

        StructureTemplate template = maybeDefinition.get().template();
        BlockPos anchorPos = DropAnchor.resolve(structure, blockPos, player, null, structureAnchor);
        BlockPos worldCenter = anchorPos.offset(
                getInt(structure, "x", 0),
                getInt(structure, "y", 0),
                getInt(structure, "z", 0)
        );

        int rotation = resolveRotation(structure, player, structureAnchor);
        BlockPos centerOffset = resolveCenterOffset(structure, template);
        String blockMode = structure.has("blockMode") ? structure.get("blockMode").getAsString() : "replace";

        if ("replace".equals(blockMode)) {
            clearStructureArea(level, worldCenter, template.getSize(), centerOffset, rotation);
        }

        placeNbtStructure(level, template, worldCenter, centerOffset, rotation, blockMode, random);

        if (structure.has("overlay")) {
            String overlayFile = LuckyStructureRegistry.normalizeFile(structure.get("overlay").getAsString());
            LuckyStructureRegistry.find(overlayFile).ifPresentOrElse(
                    overlay -> placeNbtStructure(level, overlay.template(), worldCenter, centerOffset, rotation, "overlay", random),
                    () -> LuckyBlock.LOGGER.warn("Lucky drop {} could not find overlay structure '{}' for '{}'", dropId, overlayFile, file)
            );
        }
    }

    private static void ensureLoaded(MinecraftServer server) {
        if (LuckyStructureRegistry.isEmpty()) {
            LuckyStructureRegistry.reload(server);
        }
    }

    private static void placeNbtStructure(
            ServerLevel level,
            StructureTemplate template,
            BlockPos worldCenter,
            BlockPos centerOffset,
            int rotation,
            String blockMode,
            RandomSource random
    ) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.values()[Math.floorMod(rotation, 4)])
                .setRotationPivot(centerOffset)
                .setIgnoreEntities(false)
                .addProcessor(new BlockModeProcessor(blockMode));

        BlockPos cornerPos = worldCenter.subtract(centerOffset);
        boolean placed = template.placeInWorld(level, cornerPos, cornerPos, settings, random, 3);
        if (!placed) {
            LuckyBlock.LOGGER.warn("Failed to place structure template at {}", cornerPos);
        }
    }

    private static void clearStructureArea(ServerLevel level, BlockPos worldCenter, Vec3i size, BlockPos centerOffset, int rotation) {
        BlockPos min = getWorldPos(BlockPos.ZERO, centerOffset, worldCenter, rotation);
        BlockPos max = getWorldPos(new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1), centerOffset, worldCenter, rotation);

        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static BlockPos resolveCenterOffset(JsonObject structure, StructureTemplate template) {
        Vec3i size = template.getSize();
        int centerX = size.getX() / 2;
        int centerY = 0;
        int centerZ = size.getZ() / 2;

        if (structure.has("centerOffset")) {
            String[] parts = structure.get("centerOffset").getAsString().split(",");
            if (parts.length > 0) {
                centerX = (int) Math.floor(Double.parseDouble(parts[0].trim()));
            }
            if (parts.length > 1) {
                centerY = (int) Math.floor(Double.parseDouble(parts[1].trim()));
            }
            if (parts.length > 2) {
                centerZ = (int) Math.floor(Double.parseDouble(parts[2].trim()));
            }
        }

        if (structure.has("centerX")) {
            centerX = structure.get("centerX").getAsInt();
        }
        if (structure.has("centerY")) {
            centerY = structure.get("centerY").getAsInt();
        }
        if (structure.has("centerZ")) {
            centerZ = structure.get("centerZ").getAsInt();
        }

        return new BlockPos(centerX, centerY, centerZ);
    }

    private static int resolveRotation(JsonObject structure, @Nullable Player player, StructureAnchor structureAnchor) {
        if (!structure.has("rotation")) {
            return structureAnchor.isPresent() ? structureAnchor.rotation() : 0;
        }

        String rotationValue = structure.get("rotation").getAsString().trim();
        if (rotationValue.equals("#sRotation") || rotationValue.equalsIgnoreCase("sRotation")) {
            return structureAnchor.isPresent() ? structureAnchor.rotation() : 0;
        }
        if (rotationValue.equals("#pDirect") || rotationValue.equalsIgnoreCase("pDirect")) {
            return playerDirection(player);
        }

        try {
            return Math.floorMod((int) Math.round(Double.parseDouble(rotationValue)), 4);
        } catch (NumberFormatException exception) {
            LuckyBlock.LOGGER.warn("Invalid structure rotation '{}'", rotationValue);
            return structureAnchor.isPresent() ? structureAnchor.rotation() : 0;
        }
    }

    private static int playerDirection(@Nullable Player player) {
        if (player == null) {
            return 0;
        }
        int rotation = (int) Math.round((player.getYRot() + 180.0) / 90.0) % 4;
        return rotation < 0 ? rotation + 4 : rotation;
    }

    private static BlockPos getWorldPos(BlockPos localPos, BlockPos centerOffset, BlockPos worldCenter, int rotation) {
        return StructureCoords.templateLocalToWorld(localPos, centerOffset, worldCenter, rotation);
    }

    private static @Nullable String withBlockMode(String mode, String blockId) {
        return switch (mode) {
            case "air" -> blockId.equals("minecraft:air") ? null : "minecraft:air";
            case "overlay" -> blockId.equals("minecraft:air") ? null : blockId;
            case "replace" -> blockId;
            default -> {
                LuckyBlock.LOGGER.warn("Invalid structure block mode '{}'", mode);
                yield null;
            }
        };
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (!object.has(key)) {
            return fallback;
        }
        return object.get(key).getAsInt();
    }

    private static final class BlockModeProcessor extends StructureProcessor {
        private final String blockMode;

        private BlockModeProcessor(String blockMode) {
            this.blockMode = blockMode;
        }

        @Override
        public StructureTemplate.StructureBlockInfo processBlock(
                LevelReader level,
                BlockPos pos,
                BlockPos pivot,
                StructureTemplate.StructureBlockInfo original,
                StructureTemplate.StructureBlockInfo current,
                StructurePlaceSettings settings
        ) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(current.state().getBlock());
            String blockIdWithMode = withBlockMode(blockMode, blockId.toString());
            if (blockIdWithMode == null) {
                return new StructureTemplate.StructureBlockInfo(
                        current.pos(),
                        level.getBlockState(current.pos()),
                        current.nbt()
                );
            }
            if (blockIdWithMode.equals(blockId.toString())) {
                return current;
            }

            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockIdWithMode));
            if (block == null) {
                return current;
            }

            BlockState newState = block.defaultBlockState();
            if (newState == current.state()) {
                return current;
            }

            return new StructureTemplate.StructureBlockInfo(current.pos(), newState, current.nbt());
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.NOP;
        }
    }
}

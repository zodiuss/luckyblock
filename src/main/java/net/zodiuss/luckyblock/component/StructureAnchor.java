package net.zodiuss.luckyblock.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record StructureAnchor(int centerX, int centerY, int centerZ, int rotation) {
    public static final StructureAnchor EMPTY = new StructureAnchor(0, 0, 0, -1);

    public static final Codec<StructureAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(StructureAnchor::centerX),
            Codec.INT.fieldOf("y").forGetter(StructureAnchor::centerY),
            Codec.INT.fieldOf("z").forGetter(StructureAnchor::centerZ),
            Codec.INT.fieldOf("rotation").forGetter(StructureAnchor::rotation)
    ).apply(instance, StructureAnchor::new));

    public boolean isPresent() {
        return rotation >= 0;
    }

    public BlockPos center() {
        return new BlockPos(centerX, centerY, centerZ);
    }
}

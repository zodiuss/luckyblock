package net.zodiuss.luckyblock.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.zodiuss.luckyblock.component.CustomDropData;
import net.zodiuss.luckyblock.component.ModComponents;

public class LuckyBlockEntity extends BlockEntity {
    public LuckyBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.LUCKY_BLOCK, worldPosition, blockState);
    }

    public CustomDropData customDrop() {
        return this.components().getOrDefault(ModComponents.CUSTOM_DROP, CustomDropData.EMPTY);
    }
}

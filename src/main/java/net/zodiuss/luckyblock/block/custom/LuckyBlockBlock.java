package net.zodiuss.luckyblock.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.zodiuss.luckyblock.block.entity.LuckyBlockEntity;
import net.zodiuss.luckyblock.component.CustomDropData;
import net.zodiuss.luckyblock.component.ModComponents;
import net.zodiuss.luckyblock.component.StructureAnchor;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;
import net.zodiuss.luckyblock.drop.LuckyDropScheduler;
import net.zodiuss.luckyblock.drop.LuckyDropSelector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class LuckyBlockBlock extends BaseEntityBlock {
    public static final MapCodec<LuckyBlockBlock> CODEC = simpleCodec(LuckyBlockBlock::new);
    public static final int MIN_LUCK = -100;
    public static final int MAX_LUCK = 100;
    private static final int LUCK_OFFSET = -MIN_LUCK;
    public static final IntegerProperty LUCK = IntegerProperty.create("luck", 0, MAX_LUCK + LUCK_OFFSET);

    public LuckyBlockBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(0.3F).sound(SoundType.STONE));
        registerDefaultState(defaultBlockState().setValue(LUCK, encodeLuck(0)));
    }

    @Override
    protected @NonNull MapCodec<LuckyBlockBlock> codec() {
        return CODEC;
    }

    public static int clampLuck(int luck) {
        return Math.max(MIN_LUCK, Math.min(MAX_LUCK, luck));
    }

    public static int encodeLuck(int luck) {
        return clampLuck(luck) + LUCK_OFFSET;
    }

    public static int decodeLuck(int luck) {
        return luck - LUCK_OFFSET;
    }

    @Override
    public @NonNull BlockState playerWillDestroy(Level level, @NonNull BlockPos pos, @NonNull BlockState state, @NonNull Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            scheduleDrop(serverLevel, pos, state, player, level.getBlockEntity(pos));
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level instanceof ServerLevel serverLevel && level.hasNeighborSignal(pos)) {
            runDropAndRemove(serverLevel, pos, state, null);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        if (level instanceof ServerLevel serverLevel && level.hasNeighborSignal(pos)) {
            runDropAndRemove(serverLevel, pos, state, null);
        }
    }

    private void scheduleDrop(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player, @Nullable BlockEntity blockEntity) {
        CustomDropData customDrop = readCustomDrop(level, pos, blockEntity);
        StructureAnchor structureAnchor = readStructureAnchor(blockEntity);
        int luck = decodeLuck(state.getValue(LUCK));
        BlockPos dropPos = pos.immutable();

        LuckyDropScheduler.schedule(level, level.getGameTime() + 1, () -> LuckyDropSelector.resolve(level.getServer(), customDrop, state.getBlock())
                .or(() -> LuckyDropSelector.select(level.getServer(), luck, level.getRandom(), state.getBlock()))
                .ifPresent(drop -> LuckyDropExecutor.execute(drop, level, dropPos, player, structureAnchor)));
    }

    private void runDropAndRemove(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        CustomDropData customDrop = readCustomDrop(level, pos, blockEntity);
        StructureAnchor structureAnchor = readStructureAnchor(blockEntity);
        int luck = decodeLuck(state.getValue(LUCK));

        level.removeBlock(pos, false);
        level.removeBlockEntity(pos);

        LuckyDropSelector.resolve(level.getServer(), customDrop, state.getBlock())
                .or(() -> LuckyDropSelector.select(level.getServer(), luck, level.getRandom(), state.getBlock()))
                .ifPresent(drop -> LuckyDropExecutor.execute(drop, level, pos, player, structureAnchor));
    }

    private static CustomDropData readCustomDrop(Level level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            blockEntity = level.getBlockEntity(pos);
        }

        if (blockEntity instanceof LuckyBlockEntity luckyBlockEntity) {
            return luckyBlockEntity.customDrop();
        }

        return blockEntity != null
                ? blockEntity.components().getOrDefault(ModComponents.CUSTOM_DROP, CustomDropData.EMPTY)
                : CustomDropData.EMPTY;
    }

    private static StructureAnchor readStructureAnchor(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return StructureAnchor.EMPTY;
        }

        return blockEntity.components().getOrDefault(ModComponents.STRUCTURE_ANCHOR, StructureAnchor.EMPTY);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LUCK);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new LuckyBlockEntity(pos, state);
    }
}

package net.zodiuss.luckyblock.block.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.block.LuckyBlocks;

public class ModBlockEntities {
    public static BlockEntityType<LuckyBlockEntity> LUCKY_BLOCK;

    public static void register() {
        LUCKY_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                LuckyBlock.id("lucky_block"),
                new BlockEntityType<>(LuckyBlockEntity::new, LuckyBlocks.allLuckyBlocks())
        );

        LuckyBlock.LOGGER.info("Registering lucky block entities");
    }
}

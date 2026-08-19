package net.zodiuss.luckyblock.block.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.platform.RegistryHelper;

public class ModBlockEntities {
    public static BlockEntityType<LuckyBlockEntity> LUCKY_BLOCK;

    public static void register() {
        if (RegistryHelper.isNeoForgePublic()) {
            RegistryHelper.queueBlockEntityFactory(
                    Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_block"),
                    () -> new BlockEntityType<>(LuckyBlockEntity::new, LuckyBlocks.allLuckyBlocks())
            );
            LuckyBlock.LOGGER.info("Registering lucky block entities (NeoForge queued)");
            return;
        }
        LUCKY_BLOCK = (BlockEntityType<LuckyBlockEntity>) RegistryHelper.registerBlockEntity(
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_block"),
                new BlockEntityType<>(LuckyBlockEntity::new, LuckyBlocks.allLuckyBlocks())
        );

        LuckyBlock.LOGGER.info("Registering lucky block entities");
    }

    public static void setFromRegistered(BlockEntityType<?> type) {
        LUCKY_BLOCK = (BlockEntityType<LuckyBlockEntity>) type;
    }
}

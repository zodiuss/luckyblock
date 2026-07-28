package net.zodiuss.luckyblock.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.addon.LuckyAddon;
import net.zodiuss.luckyblock.block.custom.LuckyBlockBlock;
import net.zodiuss.luckyblock.item.LuckyBlockItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class LuckyBlocks {
    private static final Set<Block> LUCKY_BLOCKS = new LinkedHashSet<>();
    public static final Block LUCKY_BLOCK = registerLuckyBlock("lucky_block", LuckyBlockBlock::new);

    private static Block registerLuckyBlock(String name, Function<BlockBehaviour.Properties, Block> function) {
        Block toRegister = function.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, name))));
        registerLuckyBlockItem(name, toRegister);
        Block registered = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, name), toRegister);
        LUCKY_BLOCKS.add(registered);
        return registered;
    }

    private static void registerLuckyBlockItem(String name, Block block) {
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, name),
                new LuckyBlockItem(block, new Item.Properties().useBlockDescriptionPrefix()
                        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, name))))
        );
    }

    public static void register() {
        LuckyBlock.LOGGER.info("Registering lucky blocks");

        for (LuckyAddon addon : AddonRegistry.getAddons()) {
            Block block = registerLuckyBlock(addon.config().identifier(), LuckyBlockBlock::new);
            AddonRegistry.bindBlock(addon, block);
        }
    }

    public static Set<Block> allLuckyBlocks() {
        return Collections.unmodifiableSet(LUCKY_BLOCKS);
    }

    public static List<Block> addonBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (LuckyAddon addon : AddonRegistry.getAddons()) {
            if (addon.block() != null) {
                blocks.add(addon.block());
            }
        }
        return blocks;
    }

    public static boolean isLuckyBlock(Block block) {
        return LUCKY_BLOCKS.contains(block);
    }

    public static boolean isLuckyBlockId(String blockId) {
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) {
            return false;
        }

        return BuiltInRegistries.BLOCK.getOptional(identifier)
                .map(LuckyBlocks::isLuckyBlock)
                .orElse(false);
    }
}

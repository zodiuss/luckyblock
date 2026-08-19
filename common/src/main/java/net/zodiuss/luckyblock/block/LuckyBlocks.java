package net.zodiuss.luckyblock.block;

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
import net.zodiuss.luckyblock.platform.RegistryHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class LuckyBlocks {
    private static final Set<Block> LUCKY_BLOCKS = new LinkedHashSet<>();
    public static Block LUCKY_BLOCK;

    private static Block registerLuckyBlock(String name, Function<BlockBehaviour.Properties, Block> function) {
        Identifier id = Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, name);
        if (RegistryHelper.isNeoForgePublic()) {
            // On NeoForge, defer Block creation until RegisterEvent (registry frozen at mod construction)
            RegistryHelper.queueBlockFactory(id, function);
            // Don't create Block or Item now, will be done in RegisterEvent
            return null;
        }
        Block toRegister = function.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)));
        registerLuckyBlockItem(name, toRegister);
        Block registered = RegistryHelper.registerBlock(id, toRegister);
        LUCKY_BLOCKS.add(registered);
        return registered;
    }

    private static void registerLuckyBlockItem(String name, Block block) {
        // On NeoForge, Item creation is deferred to ITEM RegisterEvent, don't do now
        if (RegistryHelper.isNeoForgePublic()) {
            return;
        }
        RegistryHelper.registerItem(
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, name),
                new LuckyBlockItem(block, new Item.Properties().useBlockDescriptionPrefix()
                        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, name))))
        );
    }

    public static void register() {
        if (RegistryHelper.isNeoForgePublic()) {
            // NeoForge: just queue base and addon blocks, don't set LUCKY_BLOCK yet
            if (LUCKY_BLOCK == null) {
                // Queue base block factory (will be created in RegisterEvent)
                RegistryHelper.queueBlockFactory(
                        Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_block"),
                        LuckyBlockBlock::new);
            }
            LuckyBlock.LOGGER.info("Registering lucky blocks (NeoForge queued)");
            for (LuckyAddon addon : AddonRegistry.getAddons()) {
                if (addon.block() != null) continue;
                RegistryHelper.queueBlockFactory(
                        Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, addon.config().identifier()),
                        LuckyBlockBlock::new);
            }
            return;
        }
        // Fabric path
        if (LUCKY_BLOCK == null) {
            LUCKY_BLOCK = registerLuckyBlock("lucky_block", LuckyBlockBlock::new);
        }
        LuckyBlock.LOGGER.info("Registering lucky blocks");

        for (LuckyAddon addon : AddonRegistry.getAddons()) {
            if (addon.block() != null) continue;
            Block block = registerLuckyBlock(addon.config().identifier(), LuckyBlockBlock::new);
            AddonRegistry.bindBlock(addon, block);
        }
    }

    // Called on NeoForge after BLOCK RegisterEvent to populate LUCKY_BLOCK and set
    public static void setLuckyBlock(Block block) {
        LUCKY_BLOCK = block;
        if (block != null) {
            LUCKY_BLOCKS.add(block);
        }
    }

    public static void addLuckyBlock(Block block) {
        LUCKY_BLOCKS.add(block);
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
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(identifier)
                .map(LuckyBlocks::isLuckyBlock)
                .orElse(false);
    }
}

package net.zodiuss.luckyblock.creativemodetab;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.platform.RegistryHelper;

public class ModTabs {
    public static CreativeModeTab LUCKY_BLOCK_TAB;

    public static void register() {
        LUCKY_BLOCK_TAB = RegistryHelper.registerTab(
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_blocks"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .icon(() -> new ItemStack(LuckyBlocks.LUCKY_BLOCK))
                        .title(Component.literal("Lucky Blocks"))
                        .build());
        LuckyBlock.LOGGER.info("Registering creative mode tabs");
    }

    public static void setFromRegistered(CreativeModeTab tab) {
        LUCKY_BLOCK_TAB = tab;
    }
}

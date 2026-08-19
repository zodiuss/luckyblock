package net.zodiuss.luckyblock.creativemodetab;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.platform.RegistryHelper;

public class ModTabs {
    public static final CreativeModeTab LUCKY_BLOCK_TAB = RegistryHelper.registerTab(
            Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_blocks"),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(LuckyBlocks.LUCKY_BLOCK))
                    .title(Component.literal("Lucky Blocks"))
                    .build());

    public static void register() {
        LuckyBlock.LOGGER.info("Registering creative mode tabs");
    }
}

package net.zodiuss.luckyblock.creativemodetab;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.platform.RegistryHelper;

public class ModTabs {
    public static final CreativeModeTab LUCKY_BLOCK_TAB = RegistryHelper.registerTab(
            ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_blocks"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(LuckyBlocks.LUCKY_BLOCK))
                    .title(Component.literal("Lucky Blocks"))
                    .build());

    public static void register() {
        LuckyBlock.LOGGER.info("Registering creative mode tabs");
    }
}

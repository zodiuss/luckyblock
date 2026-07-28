package net.zodiuss.luckyblock.creativemodetab;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.addon.LuckyAddon;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.block.LuckyBlocks;

public class ModTabs {
    public static final CreativeModeTab LUCKY_BLOCK_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_blocks"),
            FabricCreativeModeTab.builder().icon(() -> new ItemStack(LuckyBlocks.LUCKY_BLOCK))
                    .title(Component.literal("Lucky Blocks"))
                    .displayItems(((parameters, output) -> {
                        output.accept(LuckyBlocks.LUCKY_BLOCK);
                        for (LuckyAddon addon : AddonRegistry.getAddons()) {
                            if (addon.block() != null) {
                                output.accept(addon.block());
                            }
                        }
                    })).build());

    public static void register() {
        LuckyBlock.LOGGER.info("Registering creative mode tabs");
    }
}

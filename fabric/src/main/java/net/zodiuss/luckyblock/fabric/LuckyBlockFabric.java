package net.zodiuss.luckyblock.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.LuckyCacheManager;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.addon.LuckyAddon;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.command.LuckyCommands;
import net.zodiuss.luckyblock.drop.LuckyDropScheduler;

public class LuckyBlockFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LuckyBlock.init(FabricLoader.getInstance().getGameDir());

        ServerTickEvents.END_SERVER_TICK.register(LuckyDropScheduler::tick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                LuckyCommands.register(dispatcher, registryAccess, environment));

        ServerLifecycleEvents.SERVER_STARTED.register(LuckyCacheManager::reloadAll);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                LuckyCacheManager.reloadAll(server);
            }
        });

        // Populate custom creative tab via Fabric's CreativeModeTabEvents (avoids protected Output via builder)
        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_blocks"));
        CreativeModeTabEvents.modifyOutputEvent(tabKey).register(output -> {
            output.accept(LuckyBlocks.LUCKY_BLOCK);
            for (LuckyAddon addon : AddonRegistry.getAddons()) {
                if (addon.block() != null) {
                    output.accept(addon.block());
                }
            }
        });
    }
}

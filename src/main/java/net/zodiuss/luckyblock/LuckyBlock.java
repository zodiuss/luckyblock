package net.zodiuss.luckyblock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.resources.Identifier;

import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.block.entity.ModBlockEntities;
import net.zodiuss.luckyblock.command.LuckyCommands;
import net.zodiuss.luckyblock.drop.LuckyDropScheduler;
import net.zodiuss.luckyblock.component.ModComponents;
import net.zodiuss.luckyblock.creativemodetab.ModTabs;
import net.zodiuss.luckyblock.structure.LuckyStructureRegistry;
import net.zodiuss.luckyblock.LuckyCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckyBlock implements ModInitializer {
	public static final String MOD_ID = "lucky";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Lucky Block Mod");

		ModComponents.register();
		AddonRegistry.loadFromGameDirectory();
		LuckyBlocks.register();
		ModBlockEntities.register();
		LuckyDropScheduler.register();
		ModTabs.register();
		LuckyCommands.register();

		ServerLifecycleEvents.SERVER_STARTED.register(LuckyCacheManager::reloadAll);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (success) {
				LuckyCacheManager.reloadAll(server);
			}
		});

	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

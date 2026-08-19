package net.zodiuss.luckyblock.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.LuckyCacheManager;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.addon.LuckyAddon;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.block.entity.ModBlockEntities;
import net.zodiuss.luckyblock.command.LuckyCommands;
import net.zodiuss.luckyblock.creativemodetab.ModTabs;
import net.zodiuss.luckyblock.drop.LuckyDropScheduler;
import net.zodiuss.luckyblock.item.LuckyBlockItem;
import net.zodiuss.luckyblock.platform.RegistryHelper;

@Mod(LuckyBlock.MOD_ID)
public class LuckyBlockNeoForge {

    public LuckyBlockNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        LuckyBlock.initPreRegistry(FMLPaths.GAMEDIR.get());

        net.zodiuss.luckyblock.component.ModComponents.register();
        LuckyBlocks.register();
        ModBlockEntities.register();
        ModTabs.register();

        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::onBuildCreativeTab);

        NeoForge.EVENT_BUS.register(this);
    }

    private void onRegister(RegisterEvent event) {
        var key = event.getRegistryKey();
        if (key.equals(Registries.DATA_COMPONENT_TYPE)) {
            event.register(Registries.DATA_COMPONENT_TYPE, helper -> {
                for (var e : RegistryHelper.PENDING_DATA_COMPONENTS) {
                    helper.register(e.id(), e.type());
                }
            });
        } else if (key.equals(Registries.BLOCK)) {
            event.register(Registries.BLOCK, helper -> {
                for (var e : RegistryHelper.PENDING_BLOCKS) {
                    helper.register(e.id(), e.block());
                    LuckyBlocks.addLuckyBlock(e.block());
                    if (e.id().getPath().equals("lucky_block")) {
                        LuckyBlocks.setLuckyBlock(e.block());
                    }
                }
                for (var e : RegistryHelper.PENDING_BLOCK_FACTORIES) {
                    Identifier id = e.id();
                    Block block = e.factory().apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)));
                    helper.register(id, block);
                    LuckyBlocks.addLuckyBlock(block);
                    if (id.getPath().equals("lucky_block")) {
                        LuckyBlocks.setLuckyBlock(block);
                    } else {
                        for (LuckyAddon addon : AddonRegistry.getAddons()) {
                            if (addon.config().identifier().equals(id.getPath()) && addon.block() == null) {
                                AddonRegistry.bindBlock(addon, block);
                                break;
                            }
                        }
                    }
                    Identifier itemId = id;
                    RegistryHelper.queueItemFactory(itemId, () -> new LuckyBlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, itemId))));
                }
            });
        } else if (key.equals(Registries.ITEM)) {
            event.register(Registries.ITEM, helper -> {
                for (var e : RegistryHelper.PENDING_ITEMS) {
                    helper.register(e.id(), e.item());
                }
                for (var e : RegistryHelper.PENDING_ITEM_FACTORIES) {
                    helper.register(e.id(), e.factory().get());
                }
            });
        } else if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
            event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
                for (var e : RegistryHelper.PENDING_BLOCK_ENTITIES) {
                    helper.register(e.id(), (net.minecraft.world.level.block.entity.BlockEntityType<?>) e.type());
                }
                for (var e : RegistryHelper.PENDING_BLOCK_ENTITY_FACTORIES) {
                    var type = e.factory().get();
                    helper.register(e.id(), type);
                    if (e.id().getPath().equals("lucky_block")) {
                        ModBlockEntities.setFromRegistered(type);
                    }
                }
            });
        } else if (key.equals(Registries.CREATIVE_MODE_TAB)) {
            event.register(Registries.CREATIVE_MODE_TAB, helper -> {
                for (var e : RegistryHelper.PENDING_TABS) {
                    helper.register(e.id(), e.tab());
                    if (e.id().getPath().equals("lucky_blocks")) {
                        ModTabs.setFromRegistered(e.tab());
                    }
                }
                for (var e : RegistryHelper.PENDING_TAB_FACTORIES) {
                    var tab = e.factory().get();
                    helper.register(e.id(), tab);
                    if (e.id().getPath().equals("lucky_blocks")) {
                        ModTabs.setFromRegistered(tab);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            LuckyDropScheduler.tick(server);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LuckyCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LuckyCacheManager.reloadAll(event.getServer());
    }

    // Mod bus event, registered via modEventBus.addListener, not NeoForge bus
    public void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        var tab = ModTabs.LUCKY_BLOCK_TAB;
        if (tab == null) {
            return;
        }
        if (event.getTab() == tab) {
            if (LuckyBlocks.LUCKY_BLOCK != null) {
                event.accept(LuckyBlocks.LUCKY_BLOCK);
            }
            for (LuckyAddon addon : AddonRegistry.getAddons()) {
                if (addon.block() != null) {
                    event.accept(addon.block());
                }
            }
        }
    }
}

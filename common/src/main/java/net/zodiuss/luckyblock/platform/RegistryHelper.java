package net.zodiuss.luckyblock.platform;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform-agnostic registry helper.
 * On Fabric, registers directly via Registry.register (registries not frozen during mod init).
 * On NeoForge, queues registrations and defers to RegisterEvent (registries frozen at mod construction).
 */
public class RegistryHelper {
    private static final boolean IS_NEOFORGE = isNeoForge();

    private static boolean isNeoForge() {
        try {
            Class.forName("net.neoforged.fml.ModList");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // Queues for NeoForge deferred registration
    public static final List<PendingBlock> PENDING_BLOCKS = new ArrayList<>();
    public static final List<PendingBlockFactory> PENDING_BLOCK_FACTORIES = new ArrayList<>();
    public static final List<PendingItem> PENDING_ITEMS = new ArrayList<>();
    public static final List<PendingItemFactory> PENDING_ITEM_FACTORIES = new ArrayList<>();
    public static final List<PendingDataComponent<?>> PENDING_DATA_COMPONENTS = new ArrayList<>();
    public static final List<PendingBlockEntity> PENDING_BLOCK_ENTITIES = new ArrayList<>();
    public static final List<PendingBlockEntityFactory> PENDING_BLOCK_ENTITY_FACTORIES = new ArrayList<>();
    public static final List<PendingTab> PENDING_TABS = new ArrayList<>();
    public static final List<PendingTabFactory> PENDING_TAB_FACTORIES = new ArrayList<>();
    public static final List<PendingRecipeSerializer<?>> PENDING_RECIPE_SERIALIZERS = new ArrayList<>();
    public static final List<PendingRecipeType<?>> PENDING_RECIPE_TYPES = new ArrayList<>();

    public record PendingBlock(Identifier id, Block block) {}
    public record PendingBlockFactory(Identifier id, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, Block> factory) {}
    public record PendingItem(Identifier id, Item item) {}
    public record PendingItemFactory(Identifier id, java.util.function.Supplier<Item> factory) {}
    public record PendingDataComponent<T>(Identifier id, DataComponentType<T> type) {}
    public record PendingBlockEntity(Identifier id, BlockEntityType<?> type) {}
    public record PendingBlockEntityFactory(Identifier id, java.util.function.Supplier<BlockEntityType<?>> factory) {}
    public record PendingTab(Identifier id, CreativeModeTab tab) {}
    public record PendingTabFactory(Identifier id, java.util.function.Supplier<CreativeModeTab> factory) {}
    public record PendingRecipeSerializer<T extends net.minecraft.world.item.crafting.Recipe<?>>(Identifier id, RecipeSerializer<T> serializer) {}
    public record PendingRecipeType<T extends net.minecraft.world.item.crafting.Recipe<?>>(Identifier id, RecipeType<T> type) {}

    public static Block registerBlock(Identifier id, Block block) {
        if (IS_NEOFORGE) {
            PENDING_BLOCKS.add(new PendingBlock(id, block));
            return block;
        } else {
            return Registry.register(BuiltInRegistries.BLOCK, id, block);
        }
    }

    public static void queueBlockFactory(Identifier id, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, Block> factory) {
        PENDING_BLOCK_FACTORIES.add(new PendingBlockFactory(id, factory));
    }

    public static Item registerItem(Identifier id, Item item) {
        if (IS_NEOFORGE) {
            PENDING_ITEMS.add(new PendingItem(id, item));
            return item;
        } else {
            return Registry.register(BuiltInRegistries.ITEM, id, item);
        }
    }

    public static void queueItemFactory(Identifier id, java.util.function.Supplier<Item> factory) {
        PENDING_ITEM_FACTORIES.add(new PendingItemFactory(id, factory));
    }

    @SuppressWarnings("unchecked")
    public static <T> DataComponentType<T> registerDataComponent(Identifier id, DataComponentType<T> type) {
        if (IS_NEOFORGE) {
            PENDING_DATA_COMPONENTS.add(new PendingDataComponent<>(id, type));
            return type;
        } else {
            return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type);
        }
    }

    public static BlockEntityType<?> registerBlockEntity(Identifier id, BlockEntityType<?> type) {
        if (IS_NEOFORGE) {
            PENDING_BLOCK_ENTITIES.add(new PendingBlockEntity(id, type));
            return type;
        } else {
            return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
        }
    }

    public static void queueBlockEntityFactory(Identifier id, java.util.function.Supplier<BlockEntityType<?>> factory) {
        PENDING_BLOCK_ENTITY_FACTORIES.add(new PendingBlockEntityFactory(id, factory));
    }

    public static CreativeModeTab registerTab(Identifier id, CreativeModeTab tab) {
        if (IS_NEOFORGE) {
            PENDING_TABS.add(new PendingTab(id, tab));
            return tab;
        } else {
            return Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        }
    }

    public static void queueTabFactory(Identifier id, java.util.function.Supplier<CreativeModeTab> factory) {
        PENDING_TAB_FACTORIES.add(new PendingTabFactory(id, factory));
    }

    public static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> registerRecipeSerializer(Identifier id, RecipeSerializer<T> serializer) {
        if (IS_NEOFORGE) {
            PENDING_RECIPE_SERIALIZERS.add(new PendingRecipeSerializer<>(id, serializer));
            return serializer;
        } else {
            return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializer);
        }
    }

    public static void queueRecipeSerializer(Identifier id, RecipeSerializer<?> serializer) {
        //noinspection unchecked
        PENDING_RECIPE_SERIALIZERS.add(new PendingRecipeSerializer<>((Identifier) id, (RecipeSerializer) serializer));
    }

    public static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeType<T> registerRecipeType(Identifier id, RecipeType<T> type) {
        if (IS_NEOFORGE) {
            PENDING_RECIPE_TYPES.add(new PendingRecipeType<>(id, type));
            return type;
        } else {
            return Registry.register(BuiltInRegistries.RECIPE_TYPE, id, type);
        }
    }

    public static void queueRecipeType(Identifier id, RecipeType<?> type) {
        //noinspection unchecked
        PENDING_RECIPE_TYPES.add(new PendingRecipeType<>((Identifier) id, (RecipeType) type));
    }

    public static boolean isNeoForgePublic() {
        return IS_NEOFORGE;
    }

    public static void clearPending() {
        PENDING_BLOCKS.clear();
        PENDING_BLOCK_FACTORIES.clear();
        PENDING_ITEMS.clear();
        PENDING_ITEM_FACTORIES.clear();
        PENDING_DATA_COMPONENTS.clear();
        PENDING_BLOCK_ENTITIES.clear();
        PENDING_BLOCK_ENTITY_FACTORIES.clear();
        PENDING_TABS.clear();
        PENDING_TAB_FACTORIES.clear();
        PENDING_RECIPE_SERIALIZERS.clear();
        PENDING_RECIPE_TYPES.clear();
    }
}

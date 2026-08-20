package net.zodiuss.luckyblock.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.zodiuss.luckyblock.block.custom.LuckyBlockBlock;
import net.zodiuss.luckyblock.component.ModComponents;
import net.zodiuss.luckyblock.item.LuckyBlockItem;

import java.util.Map;

public class LuckyLuckRecipe extends CustomRecipe {
    public static final LuckyLuckRecipe INSTANCE = new LuckyLuckRecipe(CraftingBookCategory.MISC);
    public static final MapCodec<LuckyLuckRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, LuckyLuckRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<LuckyLuckRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<LuckyLuckRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LuckyLuckRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    // Modifier deltas as per spec - singular + 9x block/ore versions
    private static final Map<Item, Integer> MODIFIERS = Map.ofEntries(
            Map.entry(Items.DIAMOND, 12),
            Map.entry(Items.DIAMOND_BLOCK, 108),
            Map.entry(Items.DIAMOND_ORE, 108),
            Map.entry(Items.DEEPSLATE_DIAMOND_ORE, 108),
            Map.entry(Items.GOLD_INGOT, 6),
            Map.entry(Items.GOLD_BLOCK, 54),
            Map.entry(Items.GOLD_ORE, 54),
            Map.entry(Items.DEEPSLATE_GOLD_ORE, 54),
            Map.entry(Items.RAW_GOLD_BLOCK, 54),
            Map.entry(Items.IRON_INGOT, 3),
            Map.entry(Items.IRON_BLOCK, 27),
            Map.entry(Items.IRON_ORE, 27),
            Map.entry(Items.DEEPSLATE_IRON_ORE, 27),
            Map.entry(Items.RAW_IRON_BLOCK, 27),
            Map.entry(Items.EMERALD, 8),
            Map.entry(Items.EMERALD_BLOCK, 72),
            Map.entry(Items.EMERALD_ORE, 72),
            Map.entry(Items.DEEPSLATE_EMERALD_ORE, 72),
            Map.entry(Items.NETHER_STAR, 100),
            Map.entry(Items.DRAGON_EGG, 100),
            Map.entry(Items.BEACON, 100),
            Map.entry(Items.ENCHANTED_GOLDEN_APPLE, 100),
            Map.entry(Items.TNT, -20),
            Map.entry(Items.SPIDER_EYE, -5),
            Map.entry(Items.FERMENTED_SPIDER_EYE, -20),
            Map.entry(Items.POISONOUS_POTATO, -10),
            Map.entry(Items.ROTTEN_FLESH, -5),
            Map.entry(Items.BONE, -5),
            Map.entry(Items.BONE_BLOCK, -45)
    );

    public LuckyLuckRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static boolean isLuckyBlock(ItemStack stack) {
        return stack.getItem() instanceof LuckyBlockItem;
    }

    private static class MatchResult {
        ItemStack luckyBlock;
        int delta;
    }

    private MatchResult findMatch(CraftingInput input) {
        if (input.ingredientCount() != 2) return null;

        ItemStack luckyStack = null;
        Integer delta = null;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (isLuckyBlock(stack)) {
                if (luckyStack != null) return null; // more than one lucky block
                luckyStack = stack;
            } else {
                Integer d = MODIFIERS.get(stack.getItem());
                if (d == null) return null; // not a modifier
                if (delta != null) return null; // more than one modifier
                delta = d;
            }
        }

        if (luckyStack == null || delta == null) return null;

        MatchResult r = new MatchResult();
        r.luckyBlock = luckyStack;
        r.delta = delta;
        return r;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findMatch(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        MatchResult match = findMatch(input);
        if (match == null) return ItemStack.EMPTY;

        int currentLuck = match.luckyBlock.getOrDefault(ModComponents.LUCK, 0);
        int newLuck = LuckyBlockBlock.clampLuck(currentLuck + match.delta);

        ItemStack result = match.luckyBlock.copy();
        result.setCount(1);
        result.set(ModComponents.LUCK, newLuck);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}

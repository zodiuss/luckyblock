package net.zodiuss.luckyblock.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.platform.RegistryHelper;

public class ModRecipes {
    public static RecipeSerializer<LuckyLuckRecipe> LUCKY_LUCK_SERIALIZER;
    public static RecipeType<LuckyLuckRecipe> LUCKY_LUCK_TYPE;

    public static void register() {
        LuckyBlock.LOGGER.info("Registering recipe serializers");

        // On NeoForge, queue for RegisterEvent; on Fabric, register directly
        if (RegistryHelper.isNeoForgePublic()) {
            RegistryHelper.queueRecipeSerializer(
                    ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_luck"),
                    LuckyLuckRecipe.SERIALIZER
            );
            RegistryHelper.queueRecipeType(
                    ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_luck"),
                    new RecipeType<LuckyLuckRecipe>() {
                        @Override
                        public String toString() {
                            return "lucky:lucky_luck";
                        }
                    }
            );
        } else {
            LUCKY_LUCK_SERIALIZER = net.minecraft.core.Registry.register(
                    BuiltInRegistries.RECIPE_SERIALIZER,
                    ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_luck"),
                    LuckyLuckRecipe.SERIALIZER
            );
            LUCKY_LUCK_TYPE = net.minecraft.core.Registry.register(
                    BuiltInRegistries.RECIPE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "lucky_luck"),
                    new RecipeType<LuckyLuckRecipe>() {
                        @Override
                        public String toString() {
                            return "lucky:lucky_luck";
                        }
                    }
            );
        }
    }

    // Called on NeoForge after RegisterEvent for RECIPE_SERIALIZER and RECIPE_TYPE
    public static void setFromRegistered(RecipeSerializer<?> serializer, RecipeType<?> type) {
        //noinspection unchecked
        LUCKY_LUCK_SERIALIZER = (RecipeSerializer<LuckyLuckRecipe>) serializer;
        //noinspection unchecked
        LUCKY_LUCK_TYPE = (RecipeType<LuckyLuckRecipe>) type;
    }

    public static void setSerializerFromRegistered(RecipeSerializer<?> serializer) {
        //noinspection unchecked
        LUCKY_LUCK_SERIALIZER = (RecipeSerializer<LuckyLuckRecipe>) serializer;
    }

    public static void setTypeFromRegistered(RecipeType<?> type) {
        //noinspection unchecked
        LUCKY_LUCK_TYPE = (RecipeType<LuckyLuckRecipe>) type;
    }
}

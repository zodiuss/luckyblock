package net.zodiuss.luckyblock.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.zodiuss.luckyblock.block.LuckyBlocks;

public class ModModelProvider extends FabricModelProvider {

    public ModModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {
        // For 26.2, createTrivialCube is private - use no-op or manual model generation
        // Original generated blockstates are already in src/main/generated, so datagen is optional
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {

    }
}

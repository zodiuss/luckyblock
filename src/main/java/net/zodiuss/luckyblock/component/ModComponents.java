package net.zodiuss.luckyblock.component;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.zodiuss.luckyblock.LuckyBlock;

import java.util.function.Function;

public class ModComponents {
    public static final DataComponentType<Integer> LUCK = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "luck"),
            DataComponentType.<Integer>builder().persistent(Codec.INT)
                    .build()
    );

    public static final DataComponentType<CustomDropData> CUSTOM_DROP = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "drop"),
            DataComponentType.<CustomDropData>builder().persistent(CustomDropData.CODEC)
                    .build()
    );

    public static final DataComponentType<StructureAnchor> STRUCTURE_ANCHOR = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "structure_anchor"),
            DataComponentType.<StructureAnchor>builder().persistent(StructureAnchor.CODEC)
                    .build()
    );

    public static void register() {
        LuckyBlock.LOGGER.info("Registering item component types");
    }
}

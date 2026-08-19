package net.zodiuss.luckyblock.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.zodiuss.luckyblock.LuckyBlock;

public class ModComponents {
    public static DataComponentType<Integer> LUCK;
    public static DataComponentType<CustomDropData> CUSTOM_DROP;
    public static DataComponentType<StructureAnchor> STRUCTURE_ANCHOR;

    public static void register() {
        LuckyBlock.LOGGER.info("Registering item component types");
        LUCK = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "luck"),
                DataComponentType.<Integer>builder().persistent(Codec.INT)
                        .build()
        );
        CUSTOM_DROP = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "drop"),
                DataComponentType.<CustomDropData>builder().persistent(CustomDropData.CODEC)
                        .build()
        );
        STRUCTURE_ANCHOR = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(LuckyBlock.MOD_ID, "structure_anchor"),
                DataComponentType.<StructureAnchor>builder().persistent(StructureAnchor.CODEC)
                        .build()
        );
    }
}

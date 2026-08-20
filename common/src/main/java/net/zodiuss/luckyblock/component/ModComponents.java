package net.zodiuss.luckyblock.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.platform.RegistryHelper;

public class ModComponents {
    public static DataComponentType<Integer> LUCK;
    public static DataComponentType<CustomDropData> CUSTOM_DROP;
    public static DataComponentType<StructureAnchor> STRUCTURE_ANCHOR;

    public static void register() {
        LuckyBlock.LOGGER.info("Registering item component types");
        LUCK = RegistryHelper.registerDataComponent(
                ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "luck"),
                DataComponentType.<Integer>builder().persistent(Codec.INT)
                        .build()
        );
        CUSTOM_DROP = RegistryHelper.registerDataComponent(
                ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "drop"),
                DataComponentType.<CustomDropData>builder().persistent(CustomDropData.CODEC)
                        .build()
        );
        STRUCTURE_ANCHOR = RegistryHelper.registerDataComponent(
                ResourceLocation.fromNamespaceAndPath(LuckyBlock.MOD_ID, "structure_anchor"),
                DataComponentType.<StructureAnchor>builder().persistent(StructureAnchor.CODEC)
                        .build()
        );
    }
}

package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

public record LuckyDrop(ResourceLocation id, double weight, int luck, JsonElement drop) {
}

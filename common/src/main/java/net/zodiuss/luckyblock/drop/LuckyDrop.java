package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

public record LuckyDrop(Identifier id, double weight, int luck, JsonElement drop) {
}

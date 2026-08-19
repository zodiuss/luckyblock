package net.zodiuss.luckyblock.component;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record CustomDropData(Optional<String> dropId, Optional<JsonElement> drop) {
    public static final CustomDropData EMPTY = new CustomDropData(Optional.empty(), Optional.empty());

    public static final Codec<CustomDropData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("drop_id").forGetter(CustomDropData::dropId),
            ExtraCodecs.JSON.optionalFieldOf("drop").forGetter(CustomDropData::drop)
    ).apply(instance, CustomDropData::new));

    public static CustomDropData ofId(String dropId) {
        return new CustomDropData(Optional.of(dropId), Optional.empty());
    }

    public static CustomDropData ofInline(JsonElement drop) {
        return new CustomDropData(Optional.empty(), Optional.of(drop));
    }

    public boolean isPresent() {
        return dropId.isPresent() || drop.isPresent();
    }
}

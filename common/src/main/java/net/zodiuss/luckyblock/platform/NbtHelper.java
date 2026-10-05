package net.zodiuss.luckyblock.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.InputStream;

/** Reads compressed NBT using the unobfuscated API shared by both loaders. */
public final class NbtHelper {
    private NbtHelper() {}

    public static CompoundTag readCompressed(InputStream stream) throws IOException {
        return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
    }
}

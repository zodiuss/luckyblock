package net.zodiuss.luckyblock.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public final class NbtHelper {
    private NbtHelper() {}

    public static CompoundTag readCompressed(InputStream stream) throws IOException {
        // 1.21.1: both Fabric (officialMojangMappings) and NeoForge are Mojang-mapped at runtime,
        // so a direct NbtIo reference is correct. Keep a reflective fallback for
        // environments where NbtAccounter was renamed to NbtSizeTracker (1.21.11/26.2 yarn)
        // without pulling FabricLoader into the common classpath.
        try {
            return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Fallback: try NbtSizeTracker via reflection (yarn rename)
            try {
                Class<?> trackerClass = Class.forName("net.minecraft.nbt.NbtSizeTracker");
                Method unlimitedHeap = trackerClass.getMethod("unlimitedHeap");
                Object accounter = unlimitedHeap.invoke(null);
                Method readCompressed = NbtIo.class.getMethod("readCompressed", InputStream.class, trackerClass);
                @SuppressWarnings("unchecked")
                CompoundTag result = (CompoundTag) readCompressed.invoke(null, stream, accounter);
                return result;
            } catch (ReflectiveOperationException ex) {
                throw new IOException("Failed to read compressed NBT via NbtSizeTracker fallback", ex);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to read compressed NBT", e);
        }
    }
}

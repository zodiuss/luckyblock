package net.zodiuss.luckyblock.platform;

import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Reads compressed NBT without hard-failing on Fabric's yarn mismatch.
 * Common is compiled with Mojang names for NeoForge but Fabric remaps via
 * yarn 1.21.11 which renames NbtAccounter -> NbtSizeTracker (class_2505) and
 * NbtIo -> class_2507. For MC 26.2 the intermediary is different, so a direct
 * reference compiled with yarn 1.21.11 is wrong at runtime (class_2507 not found).
 * This helper has NO direct constant-pool reference to NbtIo/NbtAccounter, so
 * it is not remapped incorrectly at compile time; it resolves the correct
 * runtime class via reflection + Fabric's mapping resolver.
 */
public final class NbtHelper {
    private NbtHelper() {}

    public static CompoundTag readCompressed(InputStream stream) throws IOException {
        // Reflective path that works on both loaders without compile-time remapping
        // This avoids NoClassDefFoundError: net/minecraft/class_2507 on Fabric 26.2

        // Reflective fallback that resolves the correct intermediary name at runtime
        try {
            Class<?> nbtIoClass;
            Class<?> accounterClass;
            Method unlimitedHeap;
            Method readCompressed;

            // Try Mojang names (NeoForge runtime is Mojang-mapped)
            try {
                nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
                // Try new name first, then old name
                try {
                    accounterClass = Class.forName("net.minecraft.nbt.NbtAccounter");
                } catch (ClassNotFoundException ex) {
                    accounterClass = Class.forName("net.minecraft.nbt.NbtSizeTracker");
                }
                unlimitedHeap = accounterClass.getMethod("unlimitedHeap");
                readCompressed = nbtIoClass.getMethod("readCompressed", InputStream.class, accounterClass);
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                // Fabric intermediary runtime: use mapping resolver to get correct intermediary names
                try {
                    Class<?> fabricLoader = Class.forName("net.fabricmc.loader.api.FabricLoader");
                    Object loader = fabricLoader.getMethod("getInstance").invoke(null);
                    Object mappingResolver = fabricLoader.getMethod("getMappingResolver").invoke(loader);
                    Method mapClassName = mappingResolver.getClass().getMethod("mapClassName", String.class, String.class);
                    String nbtIoIntermediary = (String) mapClassName.invoke(mappingResolver, "intermediary", "net.minecraft.nbt.NbtIo");
                    String accounterIntermediary = (String) mapClassName.invoke(mappingResolver, "intermediary", "net.minecraft.nbt.NbtAccounter");
                    // If yarn has no mapping for NbtAccounter (renamed to NbtSizeTracker in 1.21.11), fallback to old name
                    if (accounterIntermediary.equals("net/minecraft/nbt/NbtAccounter")) {
                        accounterIntermediary = (String) mapClassName.invoke(mappingResolver, "intermediary", "net.minecraft.nbt.NbtSizeTracker");
                    }
                    nbtIoClass = Class.forName(nbtIoIntermediary.replace('/', '.'));
                    accounterClass = Class.forName(accounterIntermediary.replace('/', '.'));
                    // Find readCompressed by parameter types (avoids needing to map method name)
                    for (Method m : nbtIoClass.getMethods()) {
                        if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == InputStream.class && m.getParameterTypes()[1] == accounterClass) {
                            if (m.getReturnType().getSimpleName().equals("CompoundTag") || m.getReturnType().getName().endsWith("CompoundTag")) {
                                readCompressed = m;
                                unlimitedHeap = accounterClass.getMethod("unlimitedHeap");
                                Object accounter = unlimitedHeap.invoke(null);
                                @SuppressWarnings("unchecked")
                                CompoundTag result = (CompoundTag) readCompressed.invoke(null, stream, accounter);
                                return result;
                            }
                        }
                    }
                    throw new NoSuchMethodException("readCompressed not found via intermediary");
                } catch (ReflectiveOperationException ex2) {
                    throw new IOException("Failed to resolve NBT intermediary mapping", ex2);
                }
            }

            Object accounter = unlimitedHeap.invoke(null);
            @SuppressWarnings("unchecked")
            CompoundTag result = (CompoundTag) readCompressed.invoke(null, stream, accounter);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to read compressed NBT via reflection", e);
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Failed to read compressed NBT", e);
        }
    }
}

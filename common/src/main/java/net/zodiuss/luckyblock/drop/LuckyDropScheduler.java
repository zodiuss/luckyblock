package net.zodiuss.luckyblock.drop;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.zodiuss.luckyblock.LuckyBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LuckyDropScheduler {
    private static final List<DelayedAction> PENDING = new ArrayList<>();

    private LuckyDropScheduler() {
    }

    /** @deprecated loader-specific - use loader's tick registration to call {@link #tick(MinecraftServer)} directly */
    @Deprecated
    public static void register() {
        // no-op: loader now registers tick directly
    }

    public static void schedule(ServerLevel level, long executeAtGameTime, Runnable action) {
        synchronized (PENDING) {
            PENDING.add(new DelayedAction(level.dimension(), executeAtGameTime, action));
        }
    }

    public static void tick(MinecraftServer server) {
        List<DelayedAction> ready = new ArrayList<>();

        synchronized (PENDING) {
            Iterator<DelayedAction> iterator = PENDING.iterator();

            while (iterator.hasNext()) {
                DelayedAction delayed = iterator.next();
                ServerLevel level = server.getLevel(delayed.dimension());

                if (level == null || level.getGameTime() < delayed.executeAtGameTime()) {
                    continue;
                }

                iterator.remove();
                ready.add(delayed);
            }
        }

        for (DelayedAction delayed : ready) {
            try {
                delayed.action().run();
            } catch (RuntimeException exception) {
                LuckyBlock.LOGGER.warn("Delayed lucky drop action failed in {}", delayed.dimension().location(), exception);
            }
        }
    }

    private record DelayedAction(ResourceKey<Level> dimension, long executeAtGameTime, Runnable action) {
    }
}

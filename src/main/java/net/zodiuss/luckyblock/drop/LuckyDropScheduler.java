package net.zodiuss.luckyblock.drop;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LuckyDropScheduler::tick);
    }

    public static void schedule(ServerLevel level, long executeAtGameTime, Runnable action) {
        synchronized (PENDING) {
            PENDING.add(new DelayedAction(level.dimension(), executeAtGameTime, action));
        }
    }

    private static void tick(MinecraftServer server) {
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
                LuckyBlock.LOGGER.warn("Delayed lucky drop action failed in {}", delayed.dimension().identifier(), exception);
            }
        }
    }

    private record DelayedAction(ResourceKey<Level> dimension, long executeAtGameTime, Runnable action) {
    }
}

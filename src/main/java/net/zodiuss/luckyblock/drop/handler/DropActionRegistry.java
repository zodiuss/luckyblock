package net.zodiuss.luckyblock.drop.handler;

import com.google.gson.JsonObject;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DropActionRegistry {
    private static final Map<String, DropActionHandler> HANDLERS = new LinkedHashMap<>();

    static {
        HANDLERS.put("message", new MessageHandler());
        HANDLERS.put("command", new CommandHandler());
        HANDLERS.put("item", new ItemHandler());
        HANDLERS.put("block", new BlockHandler());
        HANDLERS.put("entity", new EntityHandler());
        HANDLERS.put("repeat", new RepeatHandler());
        HANDLERS.put("random", new RandomHandler());
        HANDLERS.put("fill", new FillHandler());
        HANDLERS.put("explosion", new ExplosionHandler());
        HANDLERS.put("sound", new SoundHandler());
        HANDLERS.put("particle", new ParticleHandler());
        HANDLERS.put("time", new TimeHandler());
        HANDLERS.put("difficulty", new DifficultyHandler());
        HANDLERS.put("effect", new EffectHandler());
        HANDLERS.put("impulse", new ImpulseHandler());
        HANDLERS.put("structure", new StructureHandler());
    }

    private DropActionRegistry() {}

    public static void dispatch(JsonObject action, LuckyDropExecutor.Context context) {
        for (Map.Entry<String, DropActionHandler> entry : HANDLERS.entrySet()) {
            String key = entry.getKey();
            if (action.has(key)) {
                LuckyDropExecutor.runAction(key, context.child("." + key), () -> entry.getValue().handle(action, context.child("." + key)));
            }
        }
    }

    public static Map<String, DropActionHandler> handlers() {
        return Map.copyOf(HANDLERS);
    }
}

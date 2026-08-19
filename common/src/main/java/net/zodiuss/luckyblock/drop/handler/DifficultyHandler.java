package net.zodiuss.luckyblock.drop.handler;

import com.google.gson.JsonObject;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;

public class DifficultyHandler implements DropActionHandler {
    @Override
    public void handle(JsonObject action, LuckyDropExecutor.Context context) {
        LuckyDropExecutor.runAtBlock("difficulty " + LuckyDropExecutor.evaluateString(action.get("difficulty"), context.random()), context);
    }
}

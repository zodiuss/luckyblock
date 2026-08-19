package net.zodiuss.luckyblock.drop.handler;

import com.google.gson.JsonObject;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;

public class TimeHandler implements DropActionHandler {
    @Override
    public void handle(JsonObject action, LuckyDropExecutor.Context context) {
        LuckyDropExecutor.runAtBlock("time set " + LuckyDropExecutor.evaluateString(action.get("time"), context.random()), context);
    }
}

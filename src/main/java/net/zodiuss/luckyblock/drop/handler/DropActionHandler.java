package net.zodiuss.luckyblock.drop.handler;

import com.google.gson.JsonObject;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor.Context;

public interface DropActionHandler {
    void handle(JsonObject action, Context context);
}

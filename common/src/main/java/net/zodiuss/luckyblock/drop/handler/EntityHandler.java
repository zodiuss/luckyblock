package net.zodiuss.luckyblock.drop.handler;

import com.google.gson.JsonObject;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;

public class EntityHandler implements DropActionHandler {
    @Override
    public void handle(JsonObject action, LuckyDropExecutor.Context context) {
        LuckyDropExecutor.summonEntity(action.getAsJsonObject("entity"), context);
    }
}

package net.zodiuss.luckyblock.drop.handler;

import com.google.gson.JsonObject;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;

public class SoundHandler implements DropActionHandler {
    @Override
    public void handle(JsonObject action, LuckyDropExecutor.Context context) {
        LuckyDropExecutor.sound(action.getAsJsonObject("sound"), context);
    }
}

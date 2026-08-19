package net.zodiuss.luckyblock.drop.handler;

import com.google.gson.JsonObject;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;

public class MessageHandler implements DropActionHandler {
    @Override
    public void handle(JsonObject action, LuckyDropExecutor.Context context) {
        LuckyDropExecutor.sendMessage(action, context);
    }
}

package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.zodiuss.luckyblock.LuckyBlock;

import java.util.Map;

public final class DropMessages {
    private static final MiniMessage MINI_MESSAGE;

    static {
        MiniMessage instance = null;
        try {
            instance = MiniMessage.miniMessage();
        } catch (Throwable throwable) {
            LuckyBlock.LOGGER.warn("Failed to initialize MiniMessage; adventure will fall back to plain text", throwable);
        }
        MINI_MESSAGE = instance;
    }

    private static final Map<Character, String> LEGACY_COLOR_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>")
    );

    private static final Map<Character, String> LEGACY_FORMAT_TAGS = Map.of(
            'k', "<obfuscated>",
            'l', "<bold>",
            'm', "<strikethrough>",
            'n', "<underlined>",
            'o', "<italic>",
            'r', "<reset>"
    );

    private DropMessages() {
    }

    public static Component parse(String message, RegistryAccess registryAccess) {
        if (MINI_MESSAGE == null) {
            return Component.literal(message);
        }
        try {
            String prepared = convertLegacyDollarCodes(message);
            net.kyori.adventure.text.Component adventure = MINI_MESSAGE.deserialize(prepared);
            return fromAdventure(adventure, registryAccess);
        } catch (Throwable exception) {
            LuckyBlock.LOGGER.warn("Failed to parse lucky drop message '{}'; using plain text", message, exception);
            return Component.literal(message);
        }
    }

    public static String toJsonString(String message, RegistryAccess registryAccess) {
        return ComponentSerialization.CODEC
                .encodeStart(RegistryOps.create(JsonOps.INSTANCE, registryAccess), parse(message, registryAccess))
                .getOrThrow(error -> new IllegalStateException("Invalid message component: " + error))
                .toString();
    }

    public static boolean usesFormattedText(String message) {
        return message.contains("$") || message.contains("<");
    }

    private static String convertLegacyDollarCodes(String message) {
        if (!message.contains("$")) {
            return message;
        }

        StringBuilder result = new StringBuilder(message.length());
        for (int index = 0; index < message.length(); index++) {
            char current = message.charAt(index);
            if (current == '$' && index + 1 < message.length()) {
                char code = Character.toLowerCase(message.charAt(index + 1));
                String tag = LEGACY_COLOR_TAGS.get(code);
                if (tag == null) {
                    tag = LEGACY_FORMAT_TAGS.get(code);
                }
                if (tag != null) {
                    result.append(tag);
                    index++;
                    continue;
                }
            }

            result.append(current);
        }

        return result.toString();
    }

    private static Component fromAdventure(net.kyori.adventure.text.Component adventure, RegistryAccess registryAccess) {
        try {
            var json = JsonParser.parseString(GsonComponentSerializer.gson().serialize(adventure));
            return ComponentSerialization.CODEC
                    .parse(RegistryOps.create(JsonOps.INSTANCE, registryAccess), json)
                    .getOrThrow(error -> new IllegalStateException("Invalid message component: " + error));
        } catch (Throwable throwable) {
            LuckyBlock.LOGGER.warn("Failed to convert adventure component; using plain text", throwable);
            return Component.literal(adventure.toString());
        }
    }
}

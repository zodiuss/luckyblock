package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.addon.AddonDropCache;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.addon.LuckyAddon;
import net.zodiuss.luckyblock.component.CustomDropData;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LuckyDropSelector {
    private static final String DROPS_PATH = "drops";
    private static final double LUCK_STRENGTH = 0.77;
    private static final int ALEXSOCHA_LUCK_LIMIT = 2;
    private static final int EXTENDED_LUCK_LIMIT = 100;
    private static final double EXTENDED_TO_ALEXSOCHA_SCALE = 50.0;

    public static Optional<LuckyDrop> find(MinecraftServer server, String name) {
        return find(server, name, null);
    }

    public static Optional<LuckyDrop> find(MinecraftServer server, String name, @Nullable Block block) {
        String normalizedName = normalizeDropName(name);
        for (LuckyDrop drop : loadAllDrops(server, block)) {
            if (matchesDropName(drop.id(), normalizedName)) {
                return Optional.of(drop);
            }
        }

        return Optional.empty();
    }

    public static Optional<LuckyDrop> resolve(MinecraftServer server, CustomDropData customDrop) {
        return resolve(server, customDrop, null);
    }

    public static Optional<LuckyDrop> resolve(MinecraftServer server, CustomDropData customDrop, @Nullable Block block) {
        if (customDrop.drop().isPresent()) {
            return Optional.of(fromInlineDrop(customDrop.drop().get()));
        }

        if (customDrop.dropId().isPresent()) {
            return find(server, customDrop.dropId().get(), block);
        }

        return Optional.empty();
    }

    private static LuckyDrop fromInlineDrop(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("drop")) {
                double weight = object.has("weight") ? object.get("weight").getAsDouble() : 1.0;
                int luck = object.has("luck") ? object.get("luck").getAsInt() : 0;
                return new LuckyDrop(LuckyBlock.id("inline"), weight, luck, object.get("drop"));
            }
        }

        return new LuckyDrop(LuckyBlock.id("inline"), 1.0, 0, element);
    }

    public static Optional<LuckyDrop> select(MinecraftServer server, int blockLuck, RandomSource random) {
        return select(server, blockLuck, random, null);
    }

    public static Optional<LuckyDrop> select(MinecraftServer server, int blockLuck, RandomSource random, @Nullable Block block) {
        List<LuckyDrop> drops = loadSelectableDrops(server, block);
        return selectFromDrops(drops, blockLuck, random);
    }

    private static Optional<LuckyDrop> selectFromDrops(List<LuckyDrop> drops, int blockLuck, RandomSource random) {
        if (drops.isEmpty()) {
            return Optional.empty();
        }

        // AlexSocha packs traditionally use outcomes from -2 to +2. Preserve
        // that formula exactly for those packs. Extended packs can author in
        // the much more expressive -100 to +100 range; map that authoring
        // scale onto the original five-tier scale before applying the same
        // exponential weighting equation. Without this normalization, a
        // single +30 or -50 outcome makes a +/-100 block deterministic.
        boolean usesExtendedLuck = drops.stream()
                .anyMatch(drop -> Math.abs(drop.luck()) > ALEXSOCHA_LUCK_LIMIT);
        double luckScale = usesExtendedLuck ? EXTENDED_TO_ALEXSOCHA_SCALE : 1.0;
        double lowestLuck = 0.0;
        double highestLuck = 0.0;

        for (LuckyDrop drop : drops) {
            double scaledLuck = scaledOutcomeLuck(drop.luck(), luckScale);
            lowestLuck = Math.min(lowestLuck, scaledLuck);
            highestLuck = Math.max(highestLuck, scaledLuck);
        }

        double luckRange = highestLuck - lowestLuck + 1.0;
        double luckMagnitude = Math.abs(blockLuck);
        double levelIncrease = 1.0 / (1.0 - (luckMagnitude * LUCK_STRENGTH / 100.0));
        double totalWeight = 0.0;
        List<WeightedDrop> weightedDrops = new ArrayList<>();

        for (LuckyDrop drop : drops) {
            double normalizedLuck = scaledOutcomeLuck(drop.luck(), luckScale) - lowestLuck + 1.0;
            double exponent = blockLuck >= 0 ? normalizedLuck : luckRange + 1 - normalizedLuck;
            double adjustedWeight = drop.weight() * Math.pow(levelIncrease, exponent) * 100.0;
            totalWeight += adjustedWeight;
            weightedDrops.add(new WeightedDrop(drop, adjustedWeight));
        }

        if (weightedDrops.isEmpty() || totalWeight <= 0.0) {
            return Optional.empty();
        }

        double target = random.nextDouble() * totalWeight;
        double accumulatedWeight = 0.0;

        for (WeightedDrop weightedDrop : weightedDrops) {
            accumulatedWeight += weightedDrop.weight();
            if (target < accumulatedWeight) {
                return Optional.of(weightedDrop.drop());
            }
        }

        return Optional.of(weightedDrops.getLast().drop());
    }

    private static double scaledOutcomeLuck(int outcomeLuck, double luckScale) {
        int clampedLuck = Math.max(-EXTENDED_LUCK_LIMIT, Math.min(EXTENDED_LUCK_LIMIT, outcomeLuck));
        return clampedLuck / luckScale;
    }

    private static List<LuckyDrop> loadSelectableDrops(MinecraftServer server, @Nullable Block block) {
        Optional<LuckyAddon> addon = AddonRegistry.getAddonForBlock(block);
        if (addon.isPresent()) {
            return AddonDropCache.getSelectableDrops(addon.get().config().identifier());
        }

        return loadModSelectableDrops(server);
    }

    private static List<LuckyDrop> loadAllDrops(MinecraftServer server, @Nullable Block block) {
        Optional<LuckyAddon> addon = AddonRegistry.getAddonForBlock(block);
        if (addon.isPresent()) {
            String prefix = "addons/" + addon.get().config().identifier() + "/drops/";
            return AddonDropCache.getAllDrops().stream()
                    .filter(drop -> drop.id().getPath().startsWith(prefix))
                    .toList();
        }

        // The base Lucky Block owns only the drops packaged by this mod. Addon
        // drops are deliberately private to their matching addon block.
        return loadModAllDrops(server);
    }

    private static List<LuckyDrop> loadModSelectableDrops(MinecraftServer server) {
        return loadModAllDrops(server).stream().filter(drop -> isDirectDropFile(drop.id())).toList();
    }

    private static List<LuckyDrop> loadModAllDrops(MinecraftServer server) {
        Map<Identifier, Resource> resources = server.getResourceManager()
                .listResources(DROPS_PATH, id -> id.getNamespace().equals(LuckyBlock.MOD_ID) && id.getPath().endsWith(".json"));

        List<LuckyDrop> drops = new ArrayList<>();

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                drops.add(readDrop(entry.getKey(), reader));
            } catch (RuntimeException | IOException exception) {
                LuckyBlock.LOGGER.warn("Failed to load lucky drop {}", entry.getKey(), exception);
            }
        }

        return drops;
    }

    private static boolean isDirectDropFile(Identifier id) {
        String path = id.getPath();
        if (!path.startsWith(DROPS_PATH + "/") || !path.endsWith(".json")) {
            return false;
        }

        String relativePath = path.substring(DROPS_PATH.length() + 1);
        return !relativePath.contains("/");
    }

    private static String normalizeDropName(String name) {
        String normalizedName = name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
        if (normalizedName.startsWith(DROPS_PATH + "/")) {
            normalizedName = normalizedName.substring(DROPS_PATH.length() + 1);
        }
        if (normalizedName.startsWith(LuckyBlock.MOD_ID + ":")) {
            normalizedName = normalizedName.substring(LuckyBlock.MOD_ID.length() + 1);
        }
        if (normalizedName.startsWith(DROPS_PATH + "/")) {
            normalizedName = normalizedName.substring(DROPS_PATH.length() + 1);
        }
        return normalizedName;
    }

    private static boolean matchesDropName(Identifier id, String normalizedName) {
        String path = id.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        String fileNameWithoutExtension = fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length())
                : fileName;
        String relativePath = path.startsWith(DROPS_PATH + "/")
                ? path.substring(DROPS_PATH.length() + 1)
                : path;
        String relativePathWithoutExtension = relativePath.endsWith(".json")
                ? relativePath.substring(0, relativePath.length() - ".json".length())
                : relativePath;

        if (path.startsWith("addons/")) {
            String addonRelative = path.substring("addons/".length());
            int dropsIndex = addonRelative.indexOf("/drops/");
            if (dropsIndex >= 0) {
                relativePathWithoutExtension = addonRelative.substring(dropsIndex + "/drops/".length());
            }
        }

        return relativePathWithoutExtension.equals(normalizedName)
                || fileNameWithoutExtension.equals(normalizedName)
                || path.equals(normalizedName)
                || id.toString().equals(normalizedName);
    }

    private static LuckyDrop readDrop(Identifier id, BufferedReader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        int luck = getRequiredInt(json, "luck", id);
        JsonElement drop = getRequired(json, "drop", id);
        double weight = json.has("weight") ? json.get("weight").getAsDouble() : 1.0;

        if (weight <= 0.0) {
            throw new IllegalArgumentException("Lucky drop " + id + " must have a positive weight");
        }

        return new LuckyDrop(id, weight, luck, drop);
    }

    private static JsonElement getRequired(JsonObject json, String key, Identifier id) {
        JsonElement element = json.get(key);
        if (element == null) {
            throw new IllegalArgumentException("Lucky drop " + id + " is missing required field '" + key + "'");
        }

        return element;
    }

    private static int getRequiredInt(JsonObject json, String key, Identifier id) {
        return getRequired(json, key, id).getAsInt();
    }

    private record WeightedDrop(LuckyDrop drop, double weight) {
    }
}

package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.addon.AddonDropCache;
import net.zodiuss.luckyblock.addon.AddonRegistry;
import net.zodiuss.luckyblock.addon.LuckyAddon;
import net.zodiuss.luckyblock.component.CustomDropData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

        return loadModAllDrops(server);
    }

    private static List<LuckyDrop> loadModSelectableDrops(MinecraftServer server) {
        List<LuckyDrop> cached = VanillaDropCache.getSelectableDrops();
        if (!cached.isEmpty()) {
            return cached;
        }
        return VanillaDropCache.getAllDrops().stream().filter(drop -> VanillaDropCache.isDirectDropFile(drop.id())).toList();
    }

    private static List<LuckyDrop> loadModAllDrops(MinecraftServer server) {
        List<LuckyDrop> cached = VanillaDropCache.getAllDrops();
        if (!cached.isEmpty()) {
            return cached;
        }
        VanillaDropCache.reload(server);
        return VanillaDropCache.getAllDrops();
    }

    private static boolean isDirectDropFile(ResourceLocation id) {
        return VanillaDropCache.isDirectDropFile(id);
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

    private static boolean matchesDropName(ResourceLocation id, String normalizedName) {
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

    private record WeightedDrop(LuckyDrop drop, double weight) {
    }
}

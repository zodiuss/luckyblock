package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.component.CustomData;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.component.CustomDropData;
import net.zodiuss.luckyblock.component.ModComponents;
import net.zodiuss.luckyblock.component.StructureAnchor;
import net.zodiuss.luckyblock.drop.DropAnchor;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.structure.LuckyStructurePlacer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuckyDropExecutor {
    private static final Pattern RANDOM_PATTERN = Pattern.compile("#(?:random|rand)\\(\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\)");
    private static final RandomEnchantment[] RANDOM_ENCHANTMENTS = {
            new RandomEnchantment("minecraft:aqua_affinity", 1),
            new RandomEnchantment("minecraft:bane_of_arthropods", 5),
            new RandomEnchantment("minecraft:binding_curse", 1),
            new RandomEnchantment("minecraft:blast_protection", 4),
            new RandomEnchantment("minecraft:breach", 4),
            new RandomEnchantment("minecraft:channeling", 1),
            new RandomEnchantment("minecraft:density", 5),
            new RandomEnchantment("minecraft:depth_strider", 3),
            new RandomEnchantment("minecraft:efficiency", 5),
            new RandomEnchantment("minecraft:feather_falling", 4),
            new RandomEnchantment("minecraft:fire_aspect", 2),
            new RandomEnchantment("minecraft:fire_protection", 4),
            new RandomEnchantment("minecraft:flame", 1),
            new RandomEnchantment("minecraft:fortune", 3),
            new RandomEnchantment("minecraft:frost_walker", 2),
            new RandomEnchantment("minecraft:impaling", 5),
            new RandomEnchantment("minecraft:infinity", 1),
            new RandomEnchantment("minecraft:knockback", 2),
            new RandomEnchantment("minecraft:looting", 3),
            new RandomEnchantment("minecraft:loyalty", 3),
            new RandomEnchantment("minecraft:luck_of_the_sea", 3),
            new RandomEnchantment("minecraft:lure", 3),
            new RandomEnchantment("minecraft:mending", 1),
            new RandomEnchantment("minecraft:multishot", 1),
            new RandomEnchantment("minecraft:piercing", 4),
            new RandomEnchantment("minecraft:power", 5),
            new RandomEnchantment("minecraft:projectile_protection", 4),
            new RandomEnchantment("minecraft:protection", 4),
            new RandomEnchantment("minecraft:punch", 2),
            new RandomEnchantment("minecraft:quick_charge", 3),
            new RandomEnchantment("minecraft:respiration", 3),
            new RandomEnchantment("minecraft:riptide", 3),
            new RandomEnchantment("minecraft:sharpness", 5),
            new RandomEnchantment("minecraft:silk_touch", 1),
            new RandomEnchantment("minecraft:smite", 5),
            new RandomEnchantment("minecraft:soul_speed", 3),
            new RandomEnchantment("minecraft:sweeping_edge", 3),
            new RandomEnchantment("minecraft:swift_sneak", 3),
            new RandomEnchantment("minecraft:thorns", 3),
            new RandomEnchantment("minecraft:unbreaking", 3),
            new RandomEnchantment("minecraft:vanishing_curse", 1),
            new RandomEnchantment("minecraft:wind_burst", 3)
    };

    public static void execute(LuckyDrop drop, ServerLevel level, BlockPos pos, Player player) {
        execute(drop, level, pos, player, StructureAnchor.EMPTY);
    }

    public static void execute(LuckyDrop drop, ServerLevel level, BlockPos pos, Player player, StructureAnchor structureAnchor) {
        Context context = new Context(
                drop.id(),
                level,
                pos,
                player,
                level.getRandom(),
                "$.drop",
                structureAnchor != null ? structureAnchor : StructureAnchor.EMPTY,
                Context.NO_REPEAT_INDEX
        );
        LuckyBlock.LOGGER.info("Running lucky drop {} at {} in {}", drop.id(), pos, level.dimension().location());
        try {
            executeElement(drop.drop(), context);
        } catch (RuntimeException exception) {
            logFailure("execute selected drop", context, exception);
        }
    }

    private static void executeElement(JsonElement element, Context context) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonArray()) {
            JsonArray children = element.getAsJsonArray();
            for (int i = 0; i < children.size(); i++) {
                executeElement(children.get(i), context.child("[" + i + "]"));
            }
            return;
        }

        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Lucky drop action must be an object or array");
        }

        JsonObject action = element.getAsJsonObject();

        if (action.has("delay")) {
            int delay = Math.max(0, getInt(action, "delay", 0, context));
            if (delay > 0) {
                scheduleDelayed(context, delay, () -> executeActionObject(action, context));
                return;
            }
        }

        executeActionObject(action, context);
    }

    private static void executeActionObject(JsonObject action, Context context) {
        net.zodiuss.luckyblock.drop.handler.DropActionRegistry.dispatch(action, context);
    }

    static void scheduleDelayed(Context context, int ticks, Runnable runnable) {
        long executeAt = context.level().getGameTime() + ticks;
        LuckyDropScheduler.schedule(context.level(), executeAt, () -> {
            try {
                runnable.run();
            } catch (RuntimeException exception) {
                logFailure("delayed action", context, exception);
            }
        });
    }

    public static void runAction(String action, Context context, Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            logFailure(action, context, exception);
        }
    }

    public static void sendMessage(JsonObject action, Context context) {
        broadcastMessage(context, evaluateString(action.get("message"), context));
    }

    private static void broadcastMessage(Context context, String message) {
        Component component = DropMessages.parse(message, context.level().registryAccess());
        context.level().getServer().getPlayerList().broadcastSystemMessage(component, false);
    }

    public static void runCommand(JsonObject action, Context context) {
        String command = evaluateString(action.get("command"), context);
        runAtBlock(command, context, actionOrigin(action, context));
    }

    public static void dropItem(JsonObject item, Context context) {
        String id = resolveItemId(getString(item, "type", getString(item, "id", "minecraft:air", context.random()), context.random()), context);
        int amount = Math.max(1, getInt(item, "amount", 1, context));
        String components = item.has("components")
                ? toItemComponents(evaluateString(item.get("components"), context.random()), context)
                : "";
        String itemString = id + components;
        String nbtRaw = null;
        if (item.has("nbt")) {
            nbtRaw = evaluateString(item.get("nbt"), context.random());
            nbtRaw = legacyNbtBody(nbtRaw, context);
        }
        spawnItemDirect(itemString, nbtRaw, amount, item, context);
    }

    public static void spawnItemDirect(String itemString, String nbtRaw, int amount, JsonObject item, Context context) {
        try {
            BlockPos origin = actionOrigin(item, context);
            int[] offset = resolvePosOffset(item, context);
            double x = origin.getX() + 0.5 + offset[0] + getDouble(item, "x", 0.0, context);
            double y = origin.getY() + 0.5 + offset[1] + getDouble(item, "y", 0.0, context);
            double z = origin.getZ() + 0.5 + offset[2] + getDouble(item, "z", 0.0, context);
            ItemParser.ItemResult parsed = new ItemParser(context.level().registryAccess()).parse(new StringReader(itemString));
            ItemStack baseStack = new ItemStack(parsed.item(), 1);
            baseStack.applyComponents(parsed.components());
            if (nbtRaw != null && !nbtRaw.isBlank()) {
                try {
                    CompoundTag tag = TagParser.parseTag(nbtRaw);
                    if (!tag.isEmpty()) {
                        // Preserve arbitrary NBT as CustomData so no intended feature is lost.
                        // Known component mappings (enchantments, display, etc.) are already handled
                        // via the 'components' field; raw 'nbt' is kept as CustomData for addons
                        // that rely on legacy tag passthrough.
                        baseStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    }
                } catch (CommandSyntaxException e) {
                    LuckyBlock.LOGGER.warn("Failed to parse item nbt '{}' at {}: {}", nbtRaw, context.path(), e.getMessage());
                }
            }
            int remaining = amount;
            while (remaining > 0) {
                int stackSize = Math.min(remaining, baseStack.getMaxStackSize());
                ItemStack stack = baseStack.copyWithCount(stackSize);
                double[] motion = randomItemMotionValues(context);
                ItemEntity entity = new ItemEntity(context.level(), x, y, z, stack, motion[0], motion[1], motion[2]);
                context.level().addFreshEntity(entity);
                remaining -= stackSize;
            }
        } catch (CommandSyntaxException | RuntimeException exception) {
            logFailure("parse item '" + itemString + "'", context, exception instanceof RuntimeException re ? re : new IllegalArgumentException(exception));
        }
    }

    public static void setBlock(JsonObject block, Context context) {
        BlockPos origin = actionOrigin(block, context);
        String id = getString(block, "type", getString(block, "id", "minecraft:air", context.random()), context.random());
        String state = block.has("state") ? evaluateString(block.get("state"), context) : "";
        String nbt = block.has("nbt") ? rawCommandValue(block.get("nbt"), context) : "";
        CustomDropData customDrop = CustomDropData.EMPTY;

        if (block.has("customDrop")) {
            customDrop = parseCustomDrop(block.get("customDrop"));
        }

        if (customDrop.isPresent()) {
            nbt = "";
        }

        String mode = getString(block, "mode", "replace", context.random());
        String command = String.format(Locale.ROOT, "setblock %s %s%s%s %s", relativePos(block, context), id, state, nbt, mode);
        runAtBlock(command, context, origin);

        if (customDrop.isPresent() && LuckyBlocks.isLuckyBlockId(id)) {
            BlockPos target = resolveBlockTarget(block, origin, context);
            CustomDropData dropToApply = customDrop;
            BlockPos structureCenter = origin;
            int rotation = StructureCoords.playerDirection(context.player());
            boolean storeStructureAnchor = block.has("posOffset")
                    && evaluateString(block.get("posOffset"), context).startsWith("#sPos(");
            LuckyDropScheduler.schedule(context.level(), context.level().getGameTime() + 1, () ->
                    applyCustomDropToBlock(context.level(), target, dropToApply, structureCenter, rotation, storeStructureAnchor));
        }

        if (block.has("lootTable")) {
            runAtBlock("loot replace block " + relativePos(block, context) + " container.0 loot " + evaluateString(block.get("lootTable"), context), context, origin);
        }
    }

    private static CustomDropData parseCustomDrop(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("drop_id")) {
                return CustomDropData.ofId(object.get("drop_id").getAsString());
            }
            if (object.has("drop")) {
                return CustomDropData.ofInline(object.get("drop"));
            }
        }

        return CustomDropData.ofInline(element);
    }

    private static BlockPos resolveBlockTarget(JsonObject block, BlockPos origin, Context context) {
        int[] offset = resolvePosOffset(block, context);
        return origin.offset(offset[0], offset[1], offset[2]);
    }

    private static int[] resolvePosOffset(JsonObject object, Context context) {
        if (object.has("posOffset")) {
            String offset = evaluateString(object.get("posOffset"), context);
            if (offset.startsWith("#circleOffset(") && offset.endsWith(")")) {
                double[] vec = randomCircleOffset(offset, context.random());
                return toBlockOffset(vec[0], vec[1], vec[2]);
            }
            if (offset.startsWith("#pOffset(") && offset.endsWith(")")) {
                int[] rotated = parsePlayerOffset(offset, context.player());
                return new int[] {rotated[0], 0, rotated[1]};
            }
            if (StructureCoords.isStructurePosOffset(offset)) {
                int[] local = StructureCoords.parseCoords(offset);
                return StructureCoords.giantOffset(
                        local[0],
                        local[1],
                        local[2],
                        structureRotation(context)
                );
            }

            double[] vec = parseLegacyVec3(offset, context);
            return toBlockOffset(vec[0], vec[1], vec[2]);
        }

        return new int[] {
                (int) Math.round(getDouble(object, "x", 0.0, context)),
                (int) Math.round(getDouble(object, "y", 0.0, context)),
                (int) Math.round(getDouble(object, "z", 0.0, context))
        };
    }

    private static int[] toBlockOffset(double x, double y, double z) {
        return new int[] {(int) Math.round(x), (int) Math.round(y), (int) Math.round(z)};
    }

    private static void applyCustomDropToBlock(
            ServerLevel level,
            BlockPos pos,
            CustomDropData customDrop,
            BlockPos structureCenter,
            int rotation,
            boolean storeStructureAnchor
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            LuckyBlock.LOGGER.warn("Could not apply custom drop to lucky block at {} because no block entity was found", pos);
            return;
        }

        DataComponentMap.Builder components = DataComponentMap.builder();
        components.addAll(blockEntity.components());
        components.set(ModComponents.CUSTOM_DROP, customDrop);
        if (storeStructureAnchor) {
            components.set(ModComponents.STRUCTURE_ANCHOR, new StructureAnchor(
                    structureCenter.getX(),
                    structureCenter.getY(),
                    structureCenter.getZ(),
                    rotation
            ));
        }
        blockEntity.setComponents(components.build());
        blockEntity.setChanged();
    }

    private static void spawnParsedItem(String itemString, int amount, JsonObject item, Context context) {
        try {
            BlockPos origin = actionOrigin(item, context);
            int[] offset = resolvePosOffset(item, context);
            double x = origin.getX() + 0.5 + offset[0] + getDouble(item, "x", 0.0, context);
            double y = origin.getY() + 0.5 + offset[1] + getDouble(item, "y", 0.0, context);
            double z = origin.getZ() + 0.5 + offset[2] + getDouble(item, "z", 0.0, context);
            ItemParser.ItemResult parsed = new ItemParser(context.level().registryAccess()).parse(new StringReader(itemString));
            ItemStack baseStack = new ItemStack(parsed.item(), 1);
            baseStack.applyComponents(parsed.components());
            int remaining = amount;
            while (remaining > 0) {
                int stackSize = Math.min(remaining, baseStack.getMaxStackSize());
                ItemStack stack = baseStack.copyWithCount(stackSize);
                double[] motion = randomItemMotionValues(context);
                ItemEntity entity = new ItemEntity(context.level(), x, y, z, stack, motion[0], motion[1], motion[2]);
                context.level().addFreshEntity(entity);
                remaining -= stackSize;
            }
        } catch (CommandSyntaxException | RuntimeException exception) {
            logFailure("parse item '" + itemString + "'", context, exception instanceof RuntimeException runtimeException ? runtimeException : new IllegalArgumentException(exception));
        }
    }

    private static int maxStackSize(String itemId) {
        return switch (itemId) {
            case "minecraft:saddle",
                 "minecraft:music_disc_11",
                 "minecraft:music_disc_13",
                 "minecraft:music_disc_blocks",
                 "minecraft:music_disc_chirp",
                 "minecraft:music_disc_far",
                 "minecraft:music_disc_mall",
                 "minecraft:music_disc_mellohi",
                 "minecraft:music_disc_stal",
                 "minecraft:music_disc_strad",
                 "minecraft:music_disc_wait",
                 "minecraft:music_disc_ward",
                 "minecraft:enchanted_book",
                 "minecraft:potion",
                 "minecraft:splash_potion" -> 1;
            default -> 64;
        };
    }

    public static void summonEntity(JsonObject entity, Context context) {
        BlockPos origin = actionOrigin(entity, context);
        String type = namespaced(resolveTemplate(getString(entity, "type", "minecraft:pig", context.random()), context));
        int amount = Math.max(0, getInt(entity, "amount", 1, context));

        for (int i = 0; i < amount; i++) {
            Context entityContext = context.child("#" + i);
            String pos = relativePos(entity, entityContext);
            String nbt = entity.has("nbt") ? " " + rawEntityNbt(type, entity.get("nbt"), entityContext) : "";
            if (type.equals("minecraft:falling_block") && nbt.contains("BlockState") && !nbt.contains("Time:")) {
                nbt = nbt.substring(0, nbt.length() - 1) + ",Time:1}";
            }
            runAtBlock("summon " + type + " " + pos + nbt, entityContext, origin);
        }
    }

    public static void fill(JsonObject fill, Context context) {
        BlockPos origin = actionOrigin(fill, context);
        String id = getString(fill, "type", getString(fill, "id", "minecraft:air", context.random()), context.random());
        int xSize = Math.max(1, getInt(fill, "xSize", 1, context));
        int ySize = Math.max(1, getInt(fill, "ySize", 1, context));
        int zSize = Math.max(1, getInt(fill, "zSize", 1, context));

        if (context.structureAnchor().isPresent() && fill.has("posOffset")) {
            String posOffset = evaluateString(fill.get("posOffset"), context);
            if (StructureCoords.isStructurePosOffset(posOffset)) {
                fillStructureVolume(fill, context, origin, id, posOffset, xSize, ySize, zSize);
                return;
            }
        }

        int[] offset = resolvePosOffset(fill, context);
        // 1.21.1 fix: resolvePosOffset already includes x/y/z when no posOffset is present
        // (mirrors relativePos logic). Previously fill double-counted x/y/z as offset+base,
        // placing lava_pit_trap at ~ -40 instead of -20 (20 blocks too low, invisible).
        double baseX = fill.has("posOffset") ? getDouble(fill, "x", 0.0, context) : 0.0;
        double baseY = fill.has("posOffset") ? getDouble(fill, "y", 0.0, context) : 0.0;
        double baseZ = fill.has("posOffset") ? getDouble(fill, "z", 0.0, context) : 0.0;
        String from = formatRelative(offset[0] + baseX) + " "
                + formatRelative(offset[1] + baseY) + " "
                + formatRelative(offset[2] + baseZ);
        String to = formatRelative(offset[0] + baseX + xSize - 1) + " "
                + formatRelative(offset[1] + baseY + ySize - 1) + " "
                + formatRelative(offset[2] + baseZ + zSize - 1);
        runAtBlock("fill " + from + " " + to + " " + id + " replace", context, origin);
    }

    private static void fillStructureVolume(
            JsonObject fill,
            Context context,
            BlockPos worldCenter,
            String blockId,
            String posOffset,
            int xSize,
            int ySize,
            int zSize
    ) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));

        BlockState state = block.defaultBlockState();
        BlockPos centerOffset = new BlockPos(
                StructureCoords.GIANT_CENTER_X,
                StructureCoords.GIANT_CENTER_Y,
                StructureCoords.GIANT_CENTER_Z
        );
        int[] startLocal = StructureCoords.parseCoords(posOffset);
        int rotation = structureRotation(context);
        ServerLevel level = context.level();

        for (int localX = startLocal[0]; localX < startLocal[0] + xSize; localX++) {
            for (int localY = startLocal[1]; localY < startLocal[1] + ySize; localY++) {
                for (int localZ = startLocal[2]; localZ < startLocal[2] + zSize; localZ++) {
                    BlockPos worldPos = StructureCoords.templateLocalToWorld(
                            new BlockPos(localX, localY, localZ),
                            centerOffset,
                            worldCenter,
                            rotation
                    );
                    level.setBlock(worldPos, state, 3);
                }
            }
        }
    }

    public static void explosion(JsonObject explosion, Context context) {
        ServerLevel level = context.level();
        BlockPos origin = actionOrigin(explosion, context);
        int fuse = Math.max(0, getInt(explosion, "fuse", 0, context));
        float power = parseExplosionPower(explosion, context);
        if (false) return;
        Vec3 position = resolveActionPosition(explosion, context);
        if (fuse == 0) {
            runExplosion(level, context, position, power);
        } else if (Math.abs(power - 4.0F) < 0.001F) {
            PrimedTnt tnt = new PrimedTnt(level, position.x(), position.y(), position.z(), context.player());
            tnt.setFuse(fuse);
            level.addFreshEntity(tnt);
        } else {
            scheduleDelayed(context, fuse, () -> runExplosion(level, context, position, power));
        }
    }

    private static float parseExplosionPower(JsonObject explosion, Context context) {
        String key = explosion.has("size") ? "size" : "power";
        float power = (float) getDouble(explosion, key, 4.0, context);
        if (power <= 0.0F) throw new IllegalArgumentException("explosion " + key + " must be greater than 0");
        return power;
    }

    private static void runExplosion(ServerLevel level, Context context, Vec3 position, float power) {
        level.explode(context.player(), Explosion.getDefaultDamageSource(level, context.player()), null,
                position.x(), position.y(), position.z(), power, false, Level.ExplosionInteraction.TNT);
    }

    private static Vec3 resolveActionPosition(JsonObject action, Context context) {
        BlockPos origin = actionOrigin(action, context);
        int[] offset = resolvePosOffset(action, context);
        return new Vec3(origin.getX() + 0.5 + offset[0] + getDouble(action, "x", 0.0, context),
                origin.getY() + 0.5 + offset[1] + getDouble(action, "y", 0.0, context),
                origin.getZ() + 0.5 + offset[2] + getDouble(action, "z", 0.0, context));
    }

    public static void applyEffect(JsonObject effect, Context context) {
        String rawId = getString(effect, "id", getString(effect, "type", "", context.random()), context.random());
        if (rawId.isEmpty()) throw new IllegalArgumentException("effect requires id");
        String id = namespaced(resolveTemplate(rawId, context));
        MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.getOptional(ResourceLocation.parse(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown effect '" + id + "'"));
        int duration = Math.max(0, getInt(effect, "duration", 100, context));
        int amplifier = Math.max(0, getInt(effect, "amplifier", 0, context));
        var holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect);
        for (Entity target : resolveTargets(effect, context)) {
            if (target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(holder, duration, amplifier));
        }
    }

    public static void applyImpulse(JsonObject impulse, Context context) {
        Vec3 velocity = resolveImpulseVector(impulse, context);
        for (Entity target : resolveTargets(impulse, context)) {
            target.setDeltaMovement(target.getDeltaMovement().add(velocity));
            target.hurtMarked = true;
        }
    }

    private static Vec3 resolveImpulseVector(JsonObject impulse, Context context) {
        if (impulse.has("vector")) {
            JsonElement vector = impulse.get("vector");
            if (vector.isJsonArray()) {
                JsonArray parts = vector.getAsJsonArray();
                return new Vec3(parts.size() > 0 ? evaluateNumberOrFallback(parts.get(0), 0, context) : 0,
                        parts.size() > 1 ? evaluateNumberOrFallback(parts.get(1), 0, context) : 0,
                        parts.size() > 2 ? evaluateNumberOrFallback(parts.get(2), 0, context) : 0);
            }
            String rawVector = vector.getAsString().trim();
            if (rawVector.startsWith("#calc(") && rawVector.endsWith(")")) {
                String expression = applyTemplatesForCalc(rawVector.substring(6, rawVector.length() - 1), context);
                if (expression.contains("[") || expression.contains("#pLookVector")) {
                    double[] v = parseVectorExpression(expression, context);
                    return new Vec3(v[0], v[1], v[2]);
                }
            }
            double[] v = parseLegacyVec3(evaluateString(vector, context), context);
            return new Vec3(v[0], v[1], v[2]);
        }
        return new Vec3(getDouble(impulse, "x", 0, context), getDouble(impulse, "y", 0, context), getDouble(impulse, "z", 0, context));
    }

    private static List<Entity> resolveTargets(JsonObject action, Context context) {
        String target = action.has("target") ? action.get("target").getAsString().trim() : action.has("anchor") ? action.get("anchor").getAsString().trim() : "player";
        String lower = target.toLowerCase(Locale.ROOT);
        Matcher nearby = Pattern.compile("^#nearby(players|entities)\\((.*)\\)$", Pattern.CASE_INSENSITIVE).matcher(target);
        if (nearby.matches() && context.player() != null) {
            double radius = evaluateNumberOrFallback(new JsonPrimitive(nearby.group(2)), 0, context);
            double clampedRadius = Math.max(0, radius);
            double radiusSq = clampedRadius * clampedRadius;
            AABB area = context.player().getBoundingBox().inflate(clampedRadius);
            if (nearby.group(1).equalsIgnoreCase("players")) {
                return List.copyOf(context.level().getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, area,
                        p -> p != context.player() && p.distanceToSqr(context.player()) <= radiusSq));
            }
            return List.copyOf(context.level().getEntities(context.player(), area,
                    e -> e.distanceToSqr(context.player()) <= radiusSq));
        }
        if ((lower.equals("player") || lower.equals("#ppos")) && context.player() != null) return List.of(context.player());
        return context.player() != null ? List.of(context.player()) : List.of();
    }

    public static void sound(JsonObject sound, Context context) {
        BlockPos origin = actionOrigin(sound, context);
        String id = getString(sound, "type", getString(sound, "id", "minecraft:block.note_block.pling", context.random()), context.random());
        runAtBlock("playsound " + id + " master @a " + relativePos(sound, context), context, origin);
    }

    public static void particle(JsonObject particle, Context context) {
        BlockPos origin = actionOrigin(particle, context);
        String id = getString(particle, "type", getString(particle, "id", "minecraft:happy_villager", context.random()), context.random());
        int amount = Math.max(1, getInt(particle, "amount", getInt(particle, "particleAmount", 1, context), context));
        runAtBlock("particle " + id + " " + relativePos(particle, context) + " 0 0 0 0 " + amount, context, origin);
    }

    public static void structure(JsonObject structure, Context context) {
        JsonObject placement = structure.deepCopy();
        if (placement.has("rotation")) {
            placement.addProperty("rotation", evaluateString(placement.get("rotation"), context));
        }

        LuckyStructurePlacer.place(
                placement,
                context.level().getServer(),
                context.level(),
                context.pos(),
                context.player(),
                context.random(),
                context.dropId(),
                context.path(),
                context.structureAnchor()
        );
    }

    public static void repeat(JsonObject repeat, Context context) {
        if (!repeat.has("drops")) {
            return;
        }

        JsonElement drops = repeat.get("drops");
        if (drops == null || drops.isJsonNull()) {
            return;
        }

        int amount = Math.max(0, getInt(repeat, "amount", 1, context));
        for (int i = 0; i < amount; i++) {
            executeElement(drops, context.withRepeatIndex(i).child("[" + i + "]").child(".drops"));
        }
    }

    public static void random(JsonObject random, Context context) {
        JsonArray drops = random.getAsJsonArray("drops");
        if (drops == null || drops.isEmpty()) {
            return;
        }

        int amount = random.has("amount") ? Math.min(drops.size(), Math.max(0, getInt(random, "amount", 1, context))) : 1;
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < drops.size(); i++) {
            remaining.add(i);
        }

        for (int i = 0; i < amount; i++) {
            int remainingIndex = context.random().nextInt(remaining.size());
            int dropIndex = remaining.remove(remainingIndex);
            executeElement(drops.get(dropIndex), context.child(".drops[" + dropIndex + "]"));
        }
    }

    private static void group(JsonObject group, Context context) {
        JsonArray drops = group.getAsJsonArray("drops");
        if (drops == null || drops.isEmpty()) {
            return;
        }

        JsonObject defaults = group.has("defaults") ? group.getAsJsonObject("defaults") : null;
        if (!group.has("amount")) {
            for (int i = 0; i < drops.size(); i++) {
                executeElement(applyDefaults(drops.get(i), defaults), context.child(".drops[" + i + "]"));
            }
            return;
        }

        int amount = Math.min(drops.size(), Math.max(0, getInt(group, "amount", drops.size(), context)));
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < drops.size(); i++) {
            remaining.add(i);
        }

        for (int i = 0; i < amount; i++) {
            int remainingIndex = context.random().nextInt(remaining.size());
            int dropIndex = remaining.remove(remainingIndex);
            executeElement(applyDefaults(drops.get(dropIndex), defaults), context.child(".drops[" + dropIndex + "]"));
        }
    }

    private static JsonElement applyDefaults(JsonElement element, JsonObject defaults) {
        if (defaults == null || !element.isJsonObject()) {
            return element;
        }

        JsonObject object = element.getAsJsonObject();
        JsonObject merged = object.deepCopy();
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            if (!merged.has(entry.getKey())) {
                merged.add(entry.getKey(), entry.getValue());
            }
        }

        return merged;
    }

    private static void executeLegacy(String legacyDrop, Context context) {
        legacyDrop = legacyDrop.trim();
        if (legacyDrop.isEmpty()) {
            return;
        }

        if (legacyDrop.startsWith("group")) {
            executeLegacyGroup(legacyDrop, context);
            return;
        }

        Map<String, String> props = parseLegacyProps(legacyDrop);
        String type = props.getOrDefault("type", props.containsKey("ID") || props.containsKey("id") ? "item" : "");

        switch (type.toLowerCase(Locale.ROOT)) {
            case "item" -> executeLegacyItem(props, context);
            case "block" -> executeLegacyBlock(props, context);
            case "entity" -> executeLegacyEntity(props, context);
            case "command" -> runAtBlock(legacyValue(props, "ID", context), context);
            case "message" -> broadcastMessage(context, unquote(legacyValue(props, "ID", context)));
            case "difficulty" -> runAtBlock("difficulty " + legacyValue(props, "ID", context), context);
            case "time" -> runAtBlock("time set " + legacyValue(props, "ID", context), context);
            case "sound" -> runAtBlock("playsound " + namespaced(legacyValue(props, "ID", context)) + " master @a ~ ~ ~", context);
            case "particle" -> runAtBlock("particle " + namespaced(legacyValue(props, "ID", context)) + " ~ ~ ~ 0 0 0 0 1", context);
            case "explosion" -> runAtBlock("summon minecraft:tnt ~ ~ ~ {Fuse:0}", context);
            case "fill" -> executeLegacyFill(props, context);
            case "structure" -> {
                JsonObject structure = new JsonObject();
                structure.addProperty("type", legacyValue(props, "ID", context));
                LuckyStructurePlacer.place(
                        structure,
                        context.level().getServer(),
                        context.level(),
                        context.pos(),
                        context.player(),
                        context.random(),
                        context.dropId(),
                        context.path()
                );
            }
            default -> LuckyBlock.LOGGER.warn("Lucky drop {} has unsupported legacy action '{}' at jsonPath={} source={}", context.dropId(), type, context.path(), legacyDrop);
        }
    }

    private static void executeLegacyGroup(String legacyDrop, Context context) {
        int bodyStart = legacyDrop.indexOf('(');
        int bodyEnd = legacyDrop.lastIndexOf(')');
        if (bodyStart < 0 || bodyEnd <= bodyStart) {
            throw new IllegalArgumentException("Malformed legacy group: " + legacyDrop);
        }

        String header = legacyDrop.substring(0, bodyStart);
        List<String> children = splitTopLevel(legacyDrop.substring(bodyStart + 1, bodyEnd), ';');
        int amount = children.size();
        boolean pickRandom = false;

        if (header.startsWith("group:")) {
            String amountText = header.substring("group:".length(), header.lastIndexOf(':'));
            amount = Math.max(0, (int) Math.round(parseLegacyNumber(amountText, context)));
            pickRandom = true;
        }

        if (pickRandom) {
            List<String> remaining = new ArrayList<>(children);
            for (int i = 0; i < amount && !remaining.isEmpty(); i++) {
                int index = context.random().nextInt(remaining.size());
                executeLegacy(remaining.remove(index), context.child(".group[" + index + "]"));
            }
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            executeLegacy(children.get(i), context.child(".group[" + i + "]"));
        }
    }

    private static void executeLegacyItem(Map<String, String> props, Context context) {
        String id = namespaced(legacyValue(props, "ID", context));
        int amount = Math.max(1, (int) Math.round(parseLegacyNumber(props.getOrDefault("amount", "1"), context)));
        String nbt = props.containsKey("NBTTag") ? "," + legacyNbt("tag", props.get("NBTTag"), context) : "";
        runAtBlock("summon minecraft:item ~ ~ ~ {Item:{id:\"" + id + "\",count:" + amount + nbt + "},Motion:" + randomItemMotion(context) + "}", context);
    }

    private static void executeLegacyBlock(Map<String, String> props, Context context) {
        String id = namespaced(legacyValue(props, "ID", context));
        String pos = legacyRelativePos(props, context);
        String nbt = props.containsKey("NBTTag") || props.containsKey("tileEntity") ? " " + legacyNbtBody(props.getOrDefault("NBTTag", props.get("tileEntity")), context) : "";
        runAtBlock("setblock " + pos + " " + id + nbt + " replace", context);
    }

    private static void executeLegacyEntity(Map<String, String> props, Context context) {
        String id = namespaced(legacyValue(props, "ID", context));
        int amount = Math.max(1, (int) Math.round(parseLegacyNumber(props.getOrDefault("amount", "1"), context)));
        String pos = legacyRelativePos(props, context);
        String nbt = props.containsKey("NBTTag") ? " " + legacyNbtBody(props.get("NBTTag"), context) : "";

        for (int i = 0; i < amount; i++) {
            runAtBlock("summon " + id + " " + pos + nbt, context);
        }
    }

    private static void executeLegacyFill(Map<String, String> props, Context context) {
        String id = namespaced(legacyValue(props, "ID", context));
        int[] size = parseLegacyVec3i(props.getOrDefault("size", "(1,1,1)"), context);
        runAtBlock("fill ~ ~ ~ ~" + (size[0] - 1) + " ~" + (size[1] - 1) + " ~" + (size[2] - 1) + " " + id + " replace", context);
    }

    public static void runAtBlock(String command, Context context) {
        runAtBlock(command, context, context.pos());
    }

    public static void runAtBlock(String command, Context context, BlockPos origin) {
        String dimension = context.level().dimension().location().toString();
        String fullCommand = String.format(Locale.ROOT, "execute in %s positioned %d %d %d run %s", dimension, origin.getX(), origin.getY(), origin.getZ(), command);

        try {
            context.level().getServer().getCommands().performPrefixedCommand(commandSource(context), fullCommand);
        } catch (RuntimeException exception) {
            logFailure("run command '" + fullCommand + "'", context, exception);
        }
    }

    public static void logFailure(String action, Context context, RuntimeException exception) {
        LuckyBlock.LOGGER.warn(
                "Lucky drop {} failed to {} at {} in {} blockPos={} jsonPath={}: {}",
                context.dropId(),
                action,
                context.pos(),
                context.level().dimension().location(),
                context.pos(),
                context.path(),
                exception.getMessage(),
                exception
        );
    }

    private static BlockPos actionOrigin(JsonObject action, Context context) {
        return DropAnchor.resolve(action, context.pos(), context.player(), null, context.structureAnchor());
    }

    private static int structureRotation(Context context) {
        if (context.structureAnchor().isPresent()) {
            return context.structureAnchor().rotation();
        }

        return StructureCoords.playerDirection(context.player());
    }

    private static CommandSourceStack commandSource(Context context) {
        ServerPlayer serverPlayer = context.player() instanceof ServerPlayer player ? player : null;
        return new CommandSourceStack(
                CommandSource.NULL,
                Vec3.atCenterOf(context.pos()),
                Vec2.ZERO,
                context.level(),
                context.level().getServer().getOperatorUserPermissionLevel(),
                "Lucky Block",
                Component.literal("Lucky Block"),
                context.level().getServer(),
                serverPlayer
        );
    }

    private static String relativePos(JsonObject object, RandomSource random) {
        if (object.has("posOffset")) {
            String offset = evaluateString(object.get("posOffset"), random);
            if (offset.startsWith("#circleOffset(") && offset.endsWith(")")) {
                double[] vec = randomCircleOffset(offset, random);
                return formatRelative(vec[0]) + " " + formatRelative(vec[1]) + " " + formatRelative(vec[2]);
            }
        }
        double x = getDouble(object, "x", 0.0, random);
        double y = getDouble(object, "y", 0.0, random);
        double z = getDouble(object, "z", 0.0, random);
        return formatRelative(x) + " " + formatRelative(y) + " " + formatRelative(z);
    }

    private static String relativePos(JsonObject object, Context context) {
        int[] offset = resolvePosOffset(object, context);
        if (!object.has("posOffset")) {
            return formatRelative(offset[0]) + " " + formatRelative(offset[1]) + " " + formatRelative(offset[2]);
        }

        return formatRelative(offset[0] + getDouble(object, "x", 0.0, context)) + " "
                + formatRelative(offset[1] + getDouble(object, "y", 0.0, context)) + " "
                + formatRelative(offset[2] + getDouble(object, "z", 0.0, context));
    }

    private static int[] parsePlayerOffset(String offset, @Nullable Player player) {
        String body = offset.substring("#pOffset(".length(), offset.length() - 1);
        List<String> parts = splitTopLevel(body, ',');
        int localX = parts.isEmpty() ? 0 : (int) Math.round(Double.parseDouble(parts.get(0).trim()));
        int localZ = parts.size() > 1 ? (int) Math.round(Double.parseDouble(parts.get(1).trim())) : 0;
        return rotatePlayerOffset(localX, localZ, StructureCoords.playerDirection(player));
    }

    private static int playerDirection(@Nullable Player player) {
        return StructureCoords.playerDirection(player);
    }

    private static int[] rotatePlayerOffset(int x, int z, int rotation) {
        int modRotation = Math.floorMod(rotation, 4);
        double posX = x;
        double posZ = z;

        for (int index = 0; index < modRotation; index++) {
            double oldX = posX;
            posX = posZ;
            posZ = -oldX;
        }

        return new int[] {(int) Math.round(posX), (int) Math.round(posZ)};
    }

    private static String rawCommandValue(JsonElement element, Context context) {
        return legacyNbtBody(evaluateString(element, context.random()), context);
    }

    private static String rawEntityNbt(String entityType, JsonElement element, Context context) {
        String raw = evaluateString(element, context.random());
        return legacyNbtBody(raw, context);
    }

    private static String toItemComponents(String rawComponents, Context context) {
        rawComponents = rawComponents.trim();
        if (rawComponents.isEmpty() || rawComponents.equals("{}")) {
            return "";
        }
        if (rawComponents.startsWith("[") && rawComponents.endsWith("]")) {
            // 1.21.1: item custom_name must be a stringified JSON, not a raw object.
            // Data on main (26.2) uses [custom_name={"text":"Romantic Rose",...}] which ItemParser
            // rejects as "Not a string" on 1.21.1. Convert to [minecraft:custom_name="{\"text\":...}"]
            // by falling through to the normal parser when needed, or doing a direct
            // stringify of the JSON object.
            if (rawComponents.contains("custom_name={") || rawComponents.contains("custom_name :{") || rawComponents.contains("\"minecraft:custom_name\"={") || rawComponents.contains("minecraft:custom_name={")) {
                // Strip outer brackets and parse as legacy props to get proper stringified form
                String inner = rawComponents.substring(1, rawComponents.length() - 1);
                // Try to handle JSON object for custom_name / item_name
                // Look for custom_name={...} pattern and stringify the JSON object
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(minecraft:)?custom_name\\s*=\\s*(\\{[^\\}]*\\})");
                java.util.regex.Matcher m = p.matcher(inner);
                StringBuffer sb = new StringBuffer();
                boolean found = false;
                while (m.find()) {
                    String jsonObj = m.group(2);
                    String stringified = jsonObj.replace("\"", "\\\"");
                    stringified = "\"" + stringified + "\"";
                    // Ensure minecraft: prefix
                    m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("minecraft:custom_name=" + stringified));
                    found = true;
                }
                m.appendTail(sb);
                if (found) {
                    String resultInner = sb.toString();
                    // Also handle item_name similarly
                    java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("(minecraft:)?item_name\\s*=\\s*(\\{[^\\}]*\\})");
                    java.util.regex.Matcher m2 = p2.matcher(resultInner);
                    StringBuffer sb2 = new StringBuffer();
                    boolean found2 = false;
                    while (m2.find()) {
                        String jsonObj = m2.group(2);
                        String stringified = jsonObj.replace("\"", "\\\"");
                        stringified = "\"" + stringified + "\"";
                        m2.appendReplacement(sb2, java.util.regex.Matcher.quoteReplacement("minecraft:item_name=" + stringified));
                        found2 = true;
                    }
                    m2.appendTail(sb2);
                    if (found2) resultInner = sb2.toString();
                    // Ensure all custom_name without minecraft: gets prefix already handled, return brackets
                    // Also need to handle bare custom_name without minecraft: prefix
                    // The above already prefixes, so just return
                    return "[" + resultInner + "]";
                }
                // Fallback: if regex didn't match (complex nested JSON), try legacy parser path
                rawComponents = inner;
            } else {
                return rawComponents;
            }
        }

        if (rawComponents.startsWith("{") && rawComponents.endsWith("}")) {
            rawComponents = rawComponents.substring(1, rawComponents.length() - 1);
        }
        if (rawComponents.startsWith("(") && rawComponents.endsWith(")")) {
            rawComponents = rawComponents.substring(1, rawComponents.length() - 1);
        }

        List<String> components = new ArrayList<>();
        Map<String, String> props = parseLegacyProps(rawComponents);

        if (props.containsKey("custom_name")) {
            components.add("minecraft:custom_name=" + legacyTextComponent(props.get("custom_name"), context));
        }
        if (props.containsKey("item_name")) {
            components.add("minecraft:item_name=" + legacyTextComponent(props.get("item_name"), context));
        }
        if (props.containsKey("enchantments")) {
            components.add("minecraft:enchantments=" + enchantmentComponent(props.get("enchantments"), context));
        }
        if (props.containsKey("stored_enchantments")) {
            components.add("minecraft:stored_enchantments=" + enchantmentComponent(props.get("stored_enchantments"), context));
        }
        if (props.containsKey("potion_contents")) {
            components.add("minecraft:potion_contents=" + potionContentsComponent(props.get("potion_contents"), context));
        }

        if (components.isEmpty()) {
            LuckyBlock.LOGGER.warn("Lucky drop {} has unsupported item components '{}' at {}; dropping components", context.dropId(), rawComponents, context.path());
            return "";
        }

        return "[" + String.join(",", components) + "]";
    }

    private static String legacyTextComponent(String rawText, Context context) {
        Map<String, String> props = parseLegacyProps(stripParens(rawText));
        String text = unquote(props.getOrDefault("text", rawText));
        if (DropMessages.usesFormattedText(text)) {
            return DropMessages.toJsonString(text, context.level().registryAccess());
        }

        StringBuilder json = new StringBuilder("{\"text\":\"").append(text.replace("\"", "\\\"")).append("\"");

        if (props.containsKey("color")) {
            json.append(",\"color\":\"").append(unquote(props.get("color"))).append("\"");
        }
        if (props.containsKey("bold")) {
            json.append(",\"bold\":").append(props.get("bold"));
        }
        if (props.containsKey("italic")) {
            json.append(",\"italic\":").append(props.get("italic"));
        }

        String rawJson = json.append('}').toString();
        // 1.21.1 requires custom_name as a stringified JSON: "{"text":"..."}"
        return "\"" + rawJson.replace("\"", "\\\"") + "\"";
    }

    private static String enchantmentComponent(String rawEnchantments, Context context) {
        rawEnchantments = rawEnchantments.trim();
        if (rawEnchantments.startsWith("#")) {
            return switch (rawEnchantments) {
                case "#randEnchantment" -> randomEnchantmentComponent(context);
                case "#luckyBowEnchantments" -> "{\"minecraft:power\":5,\"minecraft:punch\":2,\"minecraft:flame\":1,\"minecraft:infinity\":1}";
                case "#luckyCrossbowEnchantments" -> "{\"minecraft:quick_charge\":3,\"minecraft:multishot\":1,\"minecraft:unbreaking\":3}";
                case "#luckyTridentEnchantments" -> "{\"minecraft:loyalty\":3,\"minecraft:impaling\":5,\"minecraft:unbreaking\":3}";
                case "#luckySwordEnchantments" -> "{\"minecraft:sharpness\":5,\"minecraft:looting\":3,\"minecraft:unbreaking\":3}";
                case "#luckyToolEnchantments", "#luckyAxeEnchantments" -> "{\"minecraft:efficiency\":5,\"minecraft:fortune\":3,\"minecraft:unbreaking\":3}";
                case "#luckyFishingRodEnchantments" -> "{\"minecraft:luck_of_the_sea\":3,\"minecraft:lure\":3,\"minecraft:unbreaking\":3}";
                case "#luckyHelmetEnchantments", "#luckyChestplateEnchantments", "#luckyLeggingsEnchantments", "#luckyBootsEnchantments" -> "{\"minecraft:protection\":4,\"minecraft:unbreaking\":3}";
                default -> {
                    LuckyBlock.LOGGER.warn("Lucky drop {} uses unsupported enchantment template '{}' at {}; using empty enchantments", context.dropId(), rawEnchantments, context.path());
                    yield "{}";
                }
            };
        }

        return rawCommandValue(new JsonPrimitive(rawEnchantments), context);
    }

    private static String randomEnchantmentComponent(Context context) {
        RandomEnchantment enchantment = RANDOM_ENCHANTMENTS[context.random().nextInt(RANDOM_ENCHANTMENTS.length)];
        int level = 1 + context.random().nextInt(enchantment.maxLevel());

        return "{\"" + enchantment.id() + "\":" + level + "}";
    }

    private static String stripParens(String value) {
        value = value.trim();
        if (value.startsWith("(") && value.endsWith(")")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static Map<String, String> parseLegacyProps(String legacyDrop) {
        Map<String, String> props = new HashMap<>();
        for (String prop : splitTopLevel(legacyDrop, ',')) {
            int equalsIndex = prop.indexOf('=');
            if (equalsIndex > 0) {
                props.put(prop.substring(0, equalsIndex).trim(), prop.substring(equalsIndex + 1).trim());
            }
        }

        return props;
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean quoted = false;
        int start = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' && (i == 0 || value.charAt(i - 1) != '\\')) {
                quoted = !quoted;
            } else if (!quoted && (c == '(' || c == '[' || c == '{')) {
                depth++;
            } else if (!quoted && (c == ')' || c == ']' || c == '}')) {
                depth--;
            } else if (!quoted && depth == 0 && c == delimiter) {
                parts.add(value.substring(start, i).trim());
                start = i + 1;
            }
        }

        parts.add(value.substring(start).trim());
        parts.removeIf(String::isEmpty);
        return parts;
    }

    private static String legacyValue(Map<String, String> props, String key, Context context) {
        return unquote(replaceLegacyTemplates(props.getOrDefault(key, props.getOrDefault(key.toLowerCase(Locale.ROOT), "")), context));
    }

    private static String legacyRelativePos(Map<String, String> props, Context context) {
        if (props.containsKey("posOffset")) {
            double[] offset = parseLegacyVec3(props.get("posOffset"), context);
            return formatRelative(offset[0]) + " " + formatRelative(offset[1]) + " " + formatRelative(offset[2]);
        }

        double x = props.containsKey("posX") ? parseLegacyNumber(props.get("posX"), context) - context.pos().getX() : 0.0;
        double y = props.containsKey("posY") ? parseLegacyNumber(props.get("posY"), context) - context.pos().getY() : 0.0;
        double z = props.containsKey("posZ") ? parseLegacyNumber(props.get("posZ"), context) - context.pos().getZ() : 0.0;
        return formatRelative(x) + " " + formatRelative(y) + " " + formatRelative(z);
    }

    private static double[] parseLegacyVec3(String value, Context context) {
        value = value.trim();
        if (value.startsWith("#circleOffset(") && value.endsWith(")")) {
            return randomCircleOffset(value, context);
        }
        if (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1);
        }
        List<String> parts = splitTopLevel(value, ',');
        return new double[] {
                parts.size() > 0 ? parseLegacyNumber(parts.get(0), context) : 0.0,
                parts.size() > 1 ? parseLegacyNumber(parts.get(1), context) : 0.0,
                parts.size() > 2 ? parseLegacyNumber(parts.get(2), context) : 0.0
        };
    }

    private static int[] parseLegacyVec3i(String value, Context context) {
        double[] vec = parseLegacyVec3(value, context);
        return new int[] {(int) Math.round(vec[0]), (int) Math.round(vec[1]), (int) Math.round(vec[2])};
    }

    private static double parseLegacyNumber(String value, Context context) {
        return evaluateArithmeticExpression(replaceLegacyTemplates(value, context).trim());
    }

    private static double evaluateArithmeticExpression(String expression) {
        if (expression.isEmpty()) {
            return 0.0;
        }

        return new ArithmeticParser(expression).parse();
    }

    private static String replaceCalc(String value, Context context) {
        int start = value.indexOf("#calc(");
        while (start >= 0) {
            int open = start + "#calc(".length() - 1;
            int close = findMatchingParen(value, open);
            if (close < 0) {
                break;
            }

            String body = value.substring(open + 1, close);
            if (body.contains("#calc(")) {
                start = value.indexOf("#calc(", start + 1);
                continue;
            }

            String resolved = applyTemplatesForCalc(body, context);
            String result;
            if (resolved.contains("[") || resolved.contains("#pLookVector")) {
                result = formatMotionVector(parseVectorExpression(resolved, context));
            } else {
                result = formatNumber(evaluateArithmeticExpression(resolved));
            }
            value = value.substring(0, start) + result + value.substring(close + 1);
            start = value.indexOf("#calc(");
        }

        return value;
    }

    private static String applyTemplatesForCalc(String value, Context context) {
        value = replaceRandom(value, context.random());
        if (context.repeatIndex() >= 0) {
            value = value.replace("#index", Integer.toString(context.repeatIndex()));
        }
        return value.trim();
    }

    private static double[] parseVectorExpression(String expression, Context context) {
        return new VectorExpressionParser(expression, context).parse();
    }

    private static String formatMotionVector(double[] vector) {
        return "[" + formatNumber(vector[0]) + "," + formatNumber(vector[1]) + "," + formatNumber(vector[2]) + "]";
    }

    private static final class VectorExpressionParser {
        private final String input;
        private final Context context;
        private int index;

        private VectorExpressionParser(String input, Context context) { this.input = input; this.context = context; }

        private double[] parse() {
            double[] value = expression();
            whitespace();
            if (index != input.length()) throw new IllegalArgumentException("Unexpected vector input at " + index);
            return value;
        }

        private double[] expression() {
            double[] value = term();
            while (true) {
                whitespace();
                if (consume('+')) value = add(value, term());
                else if (consume('-')) value = subtract(value, term());
                else return value;
            }
        }

        private double[] term() {
            double[] value = primary();
            while (true) {
                whitespace();
                if (!consume('*')) return value;
                value = scaleLookVector(value, scalar());
            }
        }

        private double[] primary() {
            whitespace();
            if (consume('(')) { double[] value = expression(); expect(')'); return value; }
            if (input.startsWith("#pLookVector", index)) {
                index += "#pLookVector".length();
                Vec3 look = context.player() == null ? Vec3.ZERO : context.player().getLookAngle();
                return new double[] {look.x, look.y, look.z};
            }
            expect('[');
            double x = scalar(); expect(',');
            double y = scalar(); expect(',');
            double z = scalar(); expect(']');
            return new double[] {x, y, z};
        }

        private double scalar() {
            whitespace(); int start = index; int depth = 0;
            while (index < input.length()) {
                char c = input.charAt(index);
                if (c == '(') depth++;
                if (c == ')') { if (depth == 0) break; depth--; }
                if (depth == 0 && (c == ',' || c == ']' || c == '+' || c == '*' || (c == '-' && index > start))) break;
                index++;
            }
            String value = input.substring(start, index).trim();
            if (value.isEmpty()) throw new IllegalArgumentException("Expected scalar at " + start);
            return evaluateArithmeticExpression(value);
        }

        private void whitespace() { while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++; }
        private boolean consume(char expected) { whitespace(); if (index < input.length() && input.charAt(index) == expected) { index++; return true; } return false; }
        private void expect(char expected) { if (!consume(expected)) throw new IllegalArgumentException("Expected '" + expected + "' at " + index); }
    }

    private static double[] add(double[] a, double[] b) { return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]}; }
    private static double[] subtract(double[] a, double[] b) { return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
    private static double[] scaleLookVector(double[] v, double scalar) { return new double[] {v[0] * scalar, v[1] * scalar, v[2] * scalar}; }

    private static double[] randomCircleOffset(String value, Context context) {
        return randomCircleOffset(value, context.random());
    }

    private static double[] randomCircleOffset(String value, RandomSource random) {
        String body = value.substring("#circleOffset(".length(), value.length() - 1);
        List<String> parts = splitTopLevel(body, ',');
        double minRadius = 0.0;
        double maxRadius = parts.isEmpty() ? 0.0 : Double.parseDouble(replaceRandom(parts.get(0), random));

        if (parts.size() > 1) {
            minRadius = maxRadius;
            maxRadius = Double.parseDouble(replaceRandom(parts.get(1), random));
        }

        double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
        double angle = random.nextDouble() * Math.PI * 2.0;
        return new double[] {Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius};
    }

    private static String replaceFireworksRocket(String value, RandomSource random) {
        String marker = "#randFireworksRocket";
        if (!value.contains(marker)) {
            return value;
        }

        String[] shapes = {"small_ball", "large_ball", "star", "creeper", "burst"};
        String shape = shapes[random.nextInt(shapes.length)];
        int colors = random.nextInt(0x1000000);
        boolean trail = random.nextBoolean();
        boolean twinkle = random.nextBoolean();
        int flight = 1 + random.nextInt(2);
        String replacement = "{fireworks:{explosions:[{shape:\"" + shape + "\",colors:[" + colors + "],has_trail:" + trail + "b,has_twinkle:" + twinkle + "b}],flight_duration:" + flight + "b}}";

        return value.replace(marker, replacement);
    }

    private static String replaceLegacyTemplates(String value, Context context) {
        value = replaceRandom(value, context.random());
        value = replaceRandomList(value, context);
        if (context.repeatIndex() >= 0) {
            value = value.replace("#index", Integer.toString(context.repeatIndex()));
        }
        value = replaceCalc(value, context);
        value = replaceLaunchMotion(value, context);
        value = replaceFireworksRocket(value, context.random());
        value = replacePotionEffectTemplates(value, context);
        value = value.replace("#bPosX", Integer.toString(context.pos().getX()));
        value = value.replace("#bPosY", Integer.toString(context.pos().getY()));
        value = value.replace("#bPosZ", Integer.toString(context.pos().getZ()));
        if (context.structureAnchor().isPresent()) {
            value = value.replace("#sRotation", Integer.toString(context.structureAnchor().rotation()));
        }
        if (context.player() != null) {
            value = value.replace("#pName", context.player().getName().getString());
            value = value.replace("#pUUID", context.player().getUUID().toString());
            value = value.replace("#pYaw+180f", formatNumber(context.player().getYRot() + 180.0) + "f");
            value = value.replace("#pYaw", formatNumber(context.player().getYRot()) + "f");
            value = value.replace("#pPitch", formatNumber(context.player().getXRot()) + "f");
            value = value.replace("#pSignRotation", Integer.toString(playerSignRotation(context.player())));
            value = value.replace("#pLeverFacing", StructureCoords.leverFacing(context.player()));
        }
        return value;
    }

    private static int playerSignRotation(Player player) {
        return Math.floorMod((int) Math.round((player.getYRot() + 180.0) / 22.5), 16);
    }

    private static String replaceLaunchMotion(String value, Context context) {
        String marker = "#randLaunchMotion";
        int start = value.indexOf(marker);
        while (start >= 0) {
            int end = start + marker.length();
            double power = 0.7;
            double upwardBoost = 0.6;

            if (end < value.length() && value.charAt(end) == '(') {
                int close = value.indexOf(')', end);
                if (close > end) {
                    List<String> args = splitTopLevel(value.substring(end + 1, close), ',');
                    if (!args.isEmpty()) {
                        power = Double.parseDouble(replaceRandom(args.get(0), context.random()));
                    }
                    if (args.size() > 1) {
                        upwardBoost = Double.parseDouble(replaceRandom(args.get(1), context.random())) / 20.0;
                    }
                    end = close + 1;
                }
            }

            String replacement = randomLaunchMotion(context, power, upwardBoost);
            value = value.substring(0, start) + replacement + value.substring(end);
            start = value.indexOf(marker, start + replacement.length());
        }

        return value;
    }

    private static String randomLaunchMotion(Context context, double power, double upwardBoost) {
        RandomSource random = context.random();
        double x;
        double y;
        double z;
        double length;

        do {
            x = random.nextDouble() * 2.0 - 1.0;
            z = random.nextDouble() * 2.0 - 1.0;
            y = random.nextDouble();
            length = Math.sqrt(x * x + y * y + z * z);
        } while (length < 1e-6);

        x /= length;
        y /= length;
        z /= length;

        double magnitude = power * random.nextDouble() * (1.0 + upwardBoost);
        x *= magnitude;
        y *= magnitude;
        z *= magnitude;

        return "[" + formatNumber(x) + "d," + formatNumber(y) + "d," + formatNumber(z) + "d]";
    }

    private static String legacyNbt(String key, String value, Context context) {
        return key + ":" + legacyNbtBody(value, context);
    }

    private static String legacyNbtBody(String value, Context context) {
        value = replaceLegacyTemplates(value, context).trim();
        if (value.startsWith("{") && value.endsWith("}")) {
            value = convertJsonCustomNames(value, context);
            value = convertSignMessages(value, context);
            value = convertEquipmentForPre1_21_2(value);
            value = convertFuseForPre1_21_2(value);
            return value;
        }
        if (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1);
        }
        value = convertLegacyComponents(value, context);
        value = convertLegacyCustomName(value, context);
        value = convertLegacyBlockStateNames(value);
        value = convertLegacyResourceFields(value);
        return "{" + toSnbt(value) + "}";
    }

    private static String convertSignMessages(String value, Context context) {
        // 1.21.1: front_text.messages plain strings must be JSON text component strings: '{"text":"..."}'
        if (!value.contains("front_text") || !value.contains("messages:")) return value;
        // Replace messages: ["","One is lucky.",...] plain strings with ['{"text":""}','{"text":"One is lucky."}',...]
        // Pattern matches messages:[ "","X",...] where entries are quoted strings not already JSON
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("messages:\\s*\\[([^\\]]*)\\]");
        java.util.regex.Matcher m = p.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String inner = m.group(1);
            // Split inner by comma but respecting quotes
            String[] parts = inner.split(",");
            StringBuilder rebuilt = new StringBuilder();
            for (int i=0;i<parts.length;i++) {
                String part = parts[i].trim();
                // part is like "\"One is lucky.\""
                if (part.startsWith("\"") && part.endsWith("\"") && !part.contains("{\"text\"")) {
                    String unq = part.substring(1, part.length()-1);
                    // Empty string stays as '{"text":""}'
                    String json = "{\"text\":\"" + unq.replace("\"", "\\\"") + "\"}";
                    part = "'"+ json.replace("'", "\\'") + "'";
                    // Use single quotes SNBT string: '{"text":"..."}'
                    // Actually SNBT string for JSON text component is single-quoted: '{"text":"One is lucky."}'
                }
                if (i>0) rebuilt.append(",");
                rebuilt.append(part);
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("messages:["+rebuilt.toString()+"]"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String convertEquipmentForPre1_21_2(String value) {
        // 1.21.1 and earlier use ArmorItems/HandItems, not equipment/drop_chances (introduced 1.21.2).
        // Data files on main (26.2) use equipment:{mainhand,feet,legs,chest,head} + drop_chances:{...}.
        // Convert at runtime so 1.21.1 can keep shared data.
        if (!value.contains("equipment:")) return value;
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (true) {
            int equipIdx = value.indexOf("equipment:", cursor);
            if (equipIdx < 0) {
                out.append(value.substring(cursor));
                break;
            }
            out.append(value, cursor, equipIdx);
            int equipBraceOpen = value.indexOf('{', equipIdx);
            if (equipBraceOpen < 0) { out.append(value.substring(equipIdx)); break; }
            int equipBraceClose = findMatchingBrace(value, equipBraceOpen);
            if (equipBraceClose < 0) { out.append(value.substring(equipIdx)); break; }
            String equipInner = value.substring(equipBraceOpen + 1, equipBraceClose);
            java.util.Map<String,String> equipMap = parseEquipmentMap(equipInner);
            // look for drop_chances immediately after
            int dcIdx = -1;
            int dcOpen = -1;
            int dcClose = -1;
            java.util.Map<String,String> dcMap = new java.util.HashMap<>();
            int dcSearch = value.indexOf("drop_chances:", equipBraceClose);
            if (dcSearch >= 0) {
                String between = value.substring(equipBraceClose + 1, dcSearch);
                // only consume if between is just comma/whitespace
                if (between.trim().matches("[,\\s]*")) {
                    dcIdx = dcSearch;
                    dcOpen = value.indexOf('{', dcIdx);
                    if (dcOpen >= 0) dcClose = findMatchingBrace(value, dcOpen);
                    if (dcClose >= 0) {
                        String dcInner = value.substring(dcOpen + 1, dcClose);
                        dcMap = parseEquipmentMap(dcInner);
                    }
                }
            }
            String feet = equipMap.getOrDefault("feet", "{}");
            String legs = equipMap.getOrDefault("legs", "{}");
            String chest = equipMap.getOrDefault("chest", "{}");
            String head = equipMap.getOrDefault("head", "{}");
            String mainhand = equipMap.getOrDefault("mainhand", "{}");
            String offhand = equipMap.getOrDefault("offhand", "{}");
            String armorItems = "ArmorItems:[" + feet + "," + legs + "," + chest + "," + head + "]";
            // Note: ArmorItems order is feet,legs,chest,head in 1.21.1
            String handItems = "HandItems:[" + mainhand + "," + offhand + "]";
            String feetChance = dcMap.getOrDefault("feet", "0.085f");
            String legsChance = dcMap.getOrDefault("legs", "0.085f");
            String chestChance = dcMap.getOrDefault("chest", "0.085f");
            String headChance = dcMap.getOrDefault("head", "0.085f");
            String mainhandChance = dcMap.getOrDefault("mainhand", "0.085f");
            String offhandChance = dcMap.getOrDefault("offhand", "0.085f");
            String armorDrop = "ArmorDropChances:[" + feetChance + "," + legsChance + "," + chestChance + "," + headChance + "]";
            String handDrop = "HandDropChances:[" + mainhandChance + "," + offhandChance + "]";
            String replacement = armorItems + "," + armorDrop + "," + handItems + "," + handDrop;
            out.append(replacement);
            if (dcClose >= 0) {
                cursor = dcClose + 1;
            } else {
                cursor = equipBraceClose + 1;
            }
        }
        return out.toString();
    }

    private static String convertFuseForPre1_21_2(String value) {
        // 1.21.1 PrimedTnt uses lowercase "fuse" (short). Data files from 26.2 use "Fuse:50" / "Fuse:50b".
        // Normalize at runtime so both cases work and int without suffix is coerced via getShort.
        // Also normalize Motion case is already correct (capital M).
        if (value.contains("Fuse:")) {
            // Replace Fuse: with fuse: when used as NBT key (after { or ,)
            // Simple global replace is safe – "Fuse" only appears as NBT key for TNT.
            value = value.replace("Fuse:", "fuse:");
        }
        return value;
    }

    private static java.util.Map<String,String> parseEquipmentMap(String inner) {
        java.util.Map<String,String> map = new java.util.HashMap<>();
        for (String part : splitTopLevel(inner, ',')) {
            int colon = part.indexOf(':');
            if (colon <= 0) continue;
            String key = part.substring(0, colon).trim();
            String val = part.substring(colon + 1).trim();
            // normalize key without quotes
            key = key.replace("\"", "").replace("'", "").trim();
            map.put(key, val);
        }
        return map;
    }

    private static String randomItemMotion(Context context) {
        double[] motion = randomItemMotionValues(context);
        return "[" + formatNumber(motion[0]) + "d," + formatNumber(motion[1]) + "d," + formatNumber(motion[2]) + "d]";
    }

    private static double[] randomItemMotionValues(Context context) {
        double x = (context.random().nextDouble() - context.random().nextDouble()) * 0.1;
        double y = context.random().nextDouble() * 0.05 + 0.2;
        double z = (context.random().nextDouble() - context.random().nextDouble()) * 0.1;
        return new double[] {x, y, z};
    }

    private static String namespaced(String id) {
        if (id == null || id.isBlank()) {
            return "minecraft:air";
        }
        if (id.startsWith("#randList(") && id.endsWith(")")) {
            String body = id.substring("#randList(".length(), id.length() - 1);
            List<String> values = splitTopLevel(body, ',');
            return namespaced(values.get(new java.util.Random().nextInt(values.size())));
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String resolveItemId(String id, Context context) {
        id = resolveTemplate(id, context);
        if (id.equals("#randSpawnEgg")) {
            String[] eggs = {
                    "minecraft:zombie_spawn_egg",
                    "minecraft:skeleton_spawn_egg",
                    "minecraft:creeper_spawn_egg",
                    "minecraft:spider_spawn_egg",
                    "minecraft:cow_spawn_egg",
                    "minecraft:pig_spawn_egg",
                    "minecraft:sheep_spawn_egg",
                    "minecraft:chicken_spawn_egg",
                    "minecraft:villager_spawn_egg",
                    "minecraft:slime_spawn_egg"
            };
            return eggs[context.random().nextInt(eggs.length)];
        }
        if (id.equals("#randColor_wool")) {
            return "minecraft:" + randomColor(context) + "_wool";
        }
        if (id.equals("#randColor_terracotta")) {
            return "minecraft:" + randomColor(context) + "_terracotta";
        }
        if (id.equals("#randColor_dye")) {
            return "minecraft:" + randomColor(context) + "_dye";
        }

        return namespaced(id);
    }

    private static String randomColor(Context context) {
        String[] colors = {
                "white",
                "orange",
                "magenta",
                "light_blue",
                "yellow",
                "lime",
                "pink",
                "gray",
                "light_gray",
                "cyan",
                "purple",
                "blue",
                "brown",
                "green",
                "red",
                "black"
        };
        return colors[context.random().nextInt(colors.length)];
    }

    private static String resolveTemplate(String value, Context context) {
        return replaceRandomList(value, context);
    }

    private static String replaceRandomList(String value, Context context) {
        String marker = "#randList(";
        int start = value.indexOf(marker);
        while (start >= 0) {
            int bodyStart = start + marker.length();
            int depth = 1;
            int end = bodyStart;
            while (end < value.length() && depth > 0) {
                char c = value.charAt(end);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                end++;
            }
            if (depth != 0) {
                return value;
            }

            String body = value.substring(bodyStart, end - 1);
            List<String> values = splitTopLevel(body, ',');
            String replacement = values.isEmpty() ? "" : unquote(values.get(context.random().nextInt(values.size())).trim());
            value = value.substring(0, start) + replacement + value.substring(end);
            start = value.indexOf(marker, start + replacement.length());
        }

        return value;
    }

    private static String convertLegacyCustomName(String value, Context context) {
        Matcher matcher = Pattern.compile("CustomName=\\((text=[^)]*)\\)").matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement("CustomName=" + legacyTextComponent("(" + matcher.group(1) + ")", context)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String convertJsonCustomNames(String value, Context context) {
        value = convertJsonCustomNameField(value, "CustomName", context);
        value = convertJsonCustomNameField(value, "\"minecraft:custom_name\"", context);
        return value;
    }

    private static String convertJsonCustomNameField(String value, String fieldName, Context context) {
        String marker = fieldName + ":";
        int searchFrom = 0;

        while (true) {
            int start = value.indexOf(marker, searchFrom);
            if (start < 0) {
                return value;
            }

            int jsonStart = start + marker.length();
            if (jsonStart >= value.length()) {
                return value;
            }

            char first = value.charAt(jsonStart);
            if (first == '{') {
                int close = findMatchingBrace(value, jsonStart);
                if (close < 0) {
                    return value;
                }

                String jsonObject = value.substring(jsonStart, close + 1);
                String converted = convertTextComponentJson(jsonObject, context);
                if (!converted.equals(jsonObject)) {
                    value = value.substring(0, jsonStart) + converted + value.substring(close + 1);
                }

                searchFrom = jsonStart + converted.length();
                continue;
            }

            searchFrom = start + marker.length();
        }
    }

    private static String convertTextComponentJson(String jsonObject, Context context) {
        Matcher matcher = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(jsonObject);
        if (!matcher.find()) {
            return jsonObject;
        }

        String text = unescapeJsonString(matcher.group(1));
        if (!DropMessages.usesFormattedText(text)) {
            // 1.21.1: entity CustomName and trade custom_name JSON objects must be stringified
            String escaped = jsonObject.replace("\"", "\\\"");
            return "\"" + escaped + "\"";
        }

        return DropMessages.toJsonString(text, context.level().registryAccess());
    }

    private static String unescapeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static int findMatchingBrace(String value, int openIndex) {
        int depth = 0;
        boolean quoted = false;

        for (int index = openIndex; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '"' && (index == 0 || value.charAt(index - 1) != '\\')) {
                quoted = !quoted;
            } else if (!quoted && current == '{') {
                depth++;
            } else if (!quoted && current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    private static String convertLegacyComponents(String value, Context context) {
        String marker = "components=(";
        int start = value.indexOf(marker);
        while (start >= 0) {
            int open = start + "components=".length();
            int close = findMatchingParen(value, open);
            if (close < 0) {
                return value;
            }

            String body = value.substring(open + 1, close);
            String replacement = "components=(" + legacyComponentsBody(body, context) + ")";
            value = value.substring(0, start) + replacement + value.substring(close + 1);
            start = value.indexOf(marker, start + replacement.length());
        }

        return value;
    }

    private static String legacyComponentsBody(String body, Context context) {
        Map<String, String> props = parseLegacyProps(body);
        List<String> components = new ArrayList<>();

        String name = props.getOrDefault("custom_name", props.get("display"));
        if (name != null) {
            components.add("\"minecraft:custom_name\"=" + legacyTextComponent(name, context));
        }
        if (props.containsKey("item_name")) {
            components.add("\"minecraft:item_name\"=" + legacyTextComponent(props.get("item_name"), context));
        }
        if (props.containsKey("enchantments")) {
            components.add("\"minecraft:enchantments\"=" + enchantmentComponent(props.get("enchantments"), context));
        }
        if (props.containsKey("stored_enchantments")) {
            components.add("\"minecraft:stored_enchantments\"=" + enchantmentComponent(props.get("stored_enchantments"), context));
        }
        if (props.containsKey("potion_contents")) {
            components.add("\"minecraft:potion_contents\"=" + potionContentsComponent(props.get("potion_contents"), context));
        }

        return String.join(",", components);
    }

    private static String potionContentsComponent(String rawPotionContents, Context context) {
        String value = replacePotionEffectTemplates(replaceLegacyTemplates(stripParens(rawPotionContents), context), context);
        value = namespacePotionIds(value);
        return "{" + toSnbt(value) + "}";
    }

    private static String replacePotionEffectTemplates(String value, Context context) {
        if (value.contains("#randPotion")) {
            String[] potions = {
                    "minecraft:fire_resistance",
                    "minecraft:harming",
                    "minecraft:healing",
                    "minecraft:invisibility",
                    "minecraft:leaping",
                    "minecraft:night_vision",
                    "minecraft:poison",
                    "minecraft:regeneration",
                    "minecraft:slow_falling",
                    "minecraft:slowness",
                    "minecraft:strength",
                    "minecraft:swiftness",
                    "minecraft:water_breathing",
                    "minecraft:weakness"
            };
            value = value.replace("#randPotion", "\"" + potions[context.random().nextInt(potions.length)] + "\"");
        }

        if (value.contains("#luckyPotionEffects")) {
            value = value.replace("#luckyPotionEffects", randomPotionEffects(true, context.random()));
        }

        if (value.contains("#unluckyPotionEffects")) {
            value = value.replace("#unluckyPotionEffects", randomPotionEffects(false, context.random()));
        }

        return value;
    }

    private static final String[] POSITIVE_POTION_EFFECTS = {
            "minecraft:speed",
            "minecraft:haste",
            "minecraft:strength",
            "minecraft:instant_health",
            "minecraft:jump_boost",
            "minecraft:regeneration",
            "minecraft:resistance",
            "minecraft:fire_resistance",
            "minecraft:water_breathing",
            "minecraft:invisibility",
            "minecraft:night_vision",
            "minecraft:absorption",
            "minecraft:saturation",
            "minecraft:glowing"
    };

    private static final String[] NEGATIVE_POTION_EFFECTS = {
            "minecraft:slowness",
            "minecraft:instant_damage",
            "minecraft:blindness",
            "minecraft:hunger",
            "minecraft:weakness",
            "minecraft:poison",
            "minecraft:wither",
            "minecraft:unluck"
    };

    private static String randomPotionEffects(boolean positive, RandomSource random) {
        String[] pool = positive ? POSITIVE_POTION_EFFECTS : NEGATIVE_POTION_EFFECTS;
        int count = positive ? 7 + random.nextInt(4) : 5 + random.nextInt(3);
        List<Integer> remaining = new ArrayList<>();
        for (int index = 0; index < pool.length; index++) {
            remaining.add(index);
        }

        StringBuilder effects = new StringBuilder("[");
        int chosen = 0;

        while (chosen < count && !remaining.isEmpty()) {
            int pick = random.nextInt(remaining.size());
            String effectId = pool[remaining.remove(pick)];

            if (chosen > 0) {
                effects.append(',');
            }
            effects.append(randomPotionEffectInstance(effectId, random));
            chosen++;
        }

        effects.append(']');
        return effects.toString();
    }

    private static String randomPotionEffectInstance(String effectId, RandomSource random) {
        int amplifier = random.nextInt(4);
        boolean instant = effectId.contains("instant");
        int duration = 0;

        if (!instant) {
            int minDuration = 3200;
            int maxDuration = 9600;
            duration = minDuration + random.nextInt(maxDuration - minDuration + 1);
        }

        return "{id:\"" + effectId + "\",amplifier:" + amplifier + "b,duration:" + duration + ",ambient:0b,show_particles:1b,show_icon:1b}";
    }

    private static String namespacePotionIds(String value) {
        Matcher matcher = Pattern.compile("(?<![A-Za-z0-9_])potion=(?:\"([a-z0-9_./:-]+)\"|([a-z0-9_./:-]+))").matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String id = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement("potion=\"" + id + "\""));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String convertLegacyResourceFields(String value) {
        value = replaceResourceField(value, "id");
        value = replaceResourceField(value, "Name");
        value = replaceResourceField(value, "profession");
        value = replaceResourceField(value, "type");
        return value;
    }

    private static String replaceResourceField(String value, String field) {
        Matcher matcher = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(field) + "=([a-z0-9_./:-]+)").matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String id = matcher.group(1);
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(field + "=\"" + id + "\""));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String convertLegacyBlockStateNames(String value) {
        Matcher matcher = Pattern.compile("BlockState=\\(Name=([a-z0-9_:.\\-/]+)\\)").matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String id = matcher.group(1);
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement("BlockState=(Name=\"" + id + "\")"));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String toSnbt(String value) {
        StringBuilder builder = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' && (i == 0 || value.charAt(i - 1) != '\\')) {
                quoted = !quoted;
                builder.append(c);
            } else if (!quoted && c == '=') {
                builder.append(':');
            } else if (!quoted && c == '(') {
                builder.append('{');
            } else if (!quoted && c == ')') {
                builder.append('}');
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    private static int findMatchingParen(String value, int openIndex) {
        int depth = 0;
        boolean quoted = false;

        for (int i = openIndex; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' && (i == 0 || value.charAt(i - 1) != '\\')) {
                quoted = !quoted;
            } else if (!quoted && c == '(') {
                depth++;
            } else if (!quoted && c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static String unquote(String value) {
        if (value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String formatRelative(double value) {
        if (value == 0.0) {
            return "~";
        }

        return "~" + formatNumber(value);
    }

    private static int getInt(JsonObject object, String key, int fallback, RandomSource random) {
        return object.has(key) ? (int) Math.round(evaluateNumber(object.get(key), random)) : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback, Context context) {
        if (!object.has(key)) {
            return fallback;
        }

        return (int) Math.round(evaluateNumberOrFallback(object.get(key), fallback, context.child("." + key)));
    }

    private static double getDouble(JsonObject object, String key, double fallback, RandomSource random) {
        return object.has(key) ? evaluateNumber(object.get(key), random) : fallback;
    }

    private static double getDouble(JsonObject object, String key, double fallback, Context context) {
        if (!object.has(key)) {
            return fallback;
        }

        return evaluateNumberOrFallback(object.get(key), fallback, context.child("." + key));
    }

    private static String getString(JsonObject object, String key, String fallback, RandomSource random) {
        return object.has(key) ? evaluateString(object.get(key), random) : fallback;
    }

    private static double evaluateNumber(JsonElement element, RandomSource random) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsDouble();
        }

        String value = evaluateString(element, random).trim();
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected number or #random(x, y), got '" + value + "'", exception);
        }
    }

    private static double evaluateNumberOrFallback(JsonElement element, double fallback, Context context) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsDouble();
        }

        String value = evaluateString(element, context).trim();
        if (value.isEmpty()) {
            LuckyBlock.LOGGER.warn("Lucky drop {} had empty number at {}; using fallback {}", context.dropId(), context.path(), fallback);
            return fallback;
        }

        try {
            return evaluateArithmeticExpression(value);
        } catch (NumberFormatException exception) {
            LuckyBlock.LOGGER.warn("Lucky drop {} had invalid number '{}' at {}; using fallback {}", context.dropId(), value, context.path(), fallback);
            return fallback;
        }
    }

    public static String evaluateString(JsonElement element, Context context) {
        return replaceLegacyTemplates(replaceRandom(element.getAsString(), context.random()), context);
    }

    public static String evaluateString(JsonElement element, RandomSource random) {
        return replaceRandom(element.getAsString(), random);
    }

    private static String replaceRandom(String value, RandomSource random) {
        Matcher matcher = RANDOM_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            double min = Double.parseDouble(matcher.group(1));
            double max = Double.parseDouble(matcher.group(2));
            double rolled = min + random.nextDouble() * (max - min);
            matcher.appendReplacement(result, Matcher.quoteReplacement(formatNumber(rolled)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String toCommandValue(JsonElement element, RandomSource random) {
        if (element.isJsonObject()) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;

            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(entry.getKey()).append(':').append(toCommandValue(entry.getValue(), random));
            }

            return builder.append('}').toString();
        }

        if (element.isJsonArray()) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;

            for (JsonElement child : element.getAsJsonArray()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toCommandValue(child, random));
            }

            return builder.append(']').toString();
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber() || primitive.isBoolean()) {
            return replaceRandom(primitive.getAsString(), random);
        }

        String value = replaceRandom(primitive.getAsString(), random);
        if (value.matches("-?\\d+(?:\\.\\d+)?")) {
            return value;
        }

        return quote(value);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }

        return Double.toString(value);
    }

    public static record Context(
            ResourceLocation dropId,
            ServerLevel level,
            BlockPos pos,
            Player player,
            RandomSource random,
            String path,
            StructureAnchor structureAnchor,
            int repeatIndex
    ) {
        private static final int NO_REPEAT_INDEX = -1;

        public Context child(String suffix) {
            return new Context(dropId, level, pos, player, random, path + suffix, structureAnchor, repeatIndex);
        }

        public Context withRepeatIndex(int index) {
            return new Context(dropId, level, pos, player, random, path, structureAnchor, index);
        }
    }

    private static final class ArithmeticParser {
        private final String input;
        private int index;

        private ArithmeticParser(String input) {
            this.input = input.replaceAll("\\s+", "");
            this.index = 0;
        }

        private double parse() {
            double value = parseExpression();
            if (index < input.length()) {
                throw new NumberFormatException("Unexpected trailing input at index " + index);
            }
            return value;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (index < input.length()) {
                char operator = input.charAt(index);
                if (operator == '+') {
                    index++;
                    value += parseTerm();
                } else if (operator == '-') {
                    index++;
                    value -= parseTerm();
                } else {
                    break;
                }
            }
            return value;
        }

        private double parseTerm() {
            double value = parseUnary();
            while (index < input.length()) {
                char operator = input.charAt(index);
                if (operator == '*') {
                    index++;
                    value *= parseUnary();
                } else if (operator == '/') {
                    index++;
                    value /= parseUnary();
                } else {
                    break;
                }
            }
            return value;
        }

        private double parseUnary() {
            if (index < input.length() && input.charAt(index) == '-') {
                index++;
                return -parseUnary();
            }
            if (index < input.length() && input.charAt(index) == '+') {
                index++;
                return parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            if (index < input.length() && input.charAt(index) == '(') {
                index++;
                double value = parseExpression();
                if (index >= input.length() || input.charAt(index) != ')') {
                    throw new NumberFormatException("Missing closing parenthesis");
                }
                index++;
                return value;
            }

            int start = index;
            while (index < input.length()) {
                char character = input.charAt(index);
                if ((character >= '0' && character <= '9') || character == '.') {
                    index++;
                    continue;
                }
                break;
            }

            if (start == index) {
                throw new NumberFormatException("Expected number at index " + index);
            }

            return Double.parseDouble(input.substring(start, index));
        }
    }

    private record RandomEnchantment(String id, int maxLevel) {
    }
}

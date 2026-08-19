package net.zodiuss.luckyblock.drop;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.zodiuss.luckyblock.component.StructureAnchor;
import org.jspecify.annotations.Nullable;

public final class DropAnchor {
    private DropAnchor() {
    }

    public static BlockPos resolve(JsonObject action, BlockPos blockPos, @Nullable Player player, @Nullable String defaultAnchor) {
        return resolve(action, blockPos, player, defaultAnchor, StructureAnchor.EMPTY);
    }

    public static BlockPos resolve(
            JsonObject action,
            BlockPos blockPos,
            @Nullable Player player,
            @Nullable String defaultAnchor,
            StructureAnchor structureAnchor
    ) {
        String anchor = defaultAnchor != null ? defaultAnchor : "block";
        if (action.has("pos")) {
            anchor = action.get("pos").getAsString().trim();
        } else if (action.has("anchor")) {
            anchor = action.get("anchor").getAsString().trim();
        }

        if (StructureCoords.isStructureCenterAnchor(anchor)) {
            if (structureAnchor.isPresent()) {
                return structureAnchor.center();
            }

            if (player != null) {
                int[] local = StructureCoords.parseCoords(anchor);
                return StructureCoords.centerFromInner(
                        blockPos,
                        local[0],
                        local[1],
                        local[2],
                        StructureCoords.playerDirection(player)
                );
            }
        }

        if (isPlayerAnchor(anchor) && player != null) {
            return player.blockPosition();
        }

        return blockPos;
    }

    public static boolean isPlayerAnchor(String anchor) {
        String normalized = normalize(anchor);
        return normalized.equals("player") || normalized.equals("#ppos");
    }

    public static String normalize(String anchor) {
        return anchor.trim().toLowerCase();
    }
}

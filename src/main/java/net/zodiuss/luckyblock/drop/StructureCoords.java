package net.zodiuss.luckyblock.drop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class StructureCoords {
    public static final int GIANT_CENTER_X = 4;
    public static final int GIANT_CENTER_Y = 0;
    public static final int GIANT_CENTER_Z = 7;

    private static final String[] LEVER_FACINGS = {"south", "west", "north", "east"};

    private StructureCoords() {
    }

    public static int[] localToOffset(int localX, int localY, int localZ, int centerX, int centerY, int centerZ, int rotation) {
        double posX = localX - centerX;
        double posY = localY - centerY;
        double posZ = localZ - centerZ;

        double rotX = posX;
        double rotZ = -posZ;
        int modRotation = Math.floorMod(rotation, 4);
        for (int index = 0; index < modRotation; index++) {
            double oldX = rotX;
            rotX = rotZ;
            rotZ = -oldX;
        }

        return new int[] {(int) Math.round(rotX), (int) Math.round(posY), (int) Math.round(-rotZ)};
    }

    public static int[] giantOffset(int localX, int localY, int localZ, int rotation) {
        return localToOffset(localX, localY, localZ, GIANT_CENTER_X, GIANT_CENTER_Y, GIANT_CENTER_Z, rotation);
    }

    public static BlockPos centerFromInner(BlockPos innerPos, int localX, int localY, int localZ, int rotation) {
        int[] offset = giantOffset(localX, localY, localZ, rotation);
        return innerPos.offset(-offset[0], -offset[1], -offset[2]);
    }

    public static BlockPos templateLocalToWorld(BlockPos localPos, BlockPos centerOffset, BlockPos worldCenter, int rotation) {
        double px = (worldCenter.getX() + localPos.getX()) - centerOffset.getX();
        double py = (worldCenter.getY() + localPos.getY()) - centerOffset.getY();
        double pz = (worldCenter.getZ() + localPos.getZ()) - centerOffset.getZ();

        double posX = px - worldCenter.getX();
        double posY = py - worldCenter.getY();
        double posZ = worldCenter.getZ() - pz;

        int modRotation = Math.floorMod(rotation, 4);
        for (int index = 0; index < modRotation; index++) {
            double oldX = posX;
            posX = posZ;
            posZ = -oldX;
        }

        return new BlockPos(
                (int) Math.floor(posX + worldCenter.getX()),
                (int) Math.floor(posY + worldCenter.getY()),
                (int) Math.floor(worldCenter.getZ() - posZ)
        );
    }

    public static int playerDirection(@Nullable Player player) {
        if (player == null) {
            return 0;
        }

        int rotation = (int) Math.round((player.getYRot() + 180.0) / 90.0) % 4;
        return rotation < 0 ? rotation + 4 : rotation;
    }

    public static String leverFacing(@Nullable Player player) {
        return LEVER_FACINGS[playerDirection(player)];
    }

    public static boolean isStructureCenterAnchor(String anchor) {
        return normalizeAnchor(anchor).startsWith("#scenter(");
    }

    public static boolean isStructurePosOffset(String offset) {
        return offset.startsWith("#sPos(") && offset.endsWith(")");
    }

    public static int[] parseCoords(String value) {
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open < 0 || close <= open) {
            return new int[] {0, 0, 0};
        }

        String body = value.substring(open + 1, close);
        List<String> parts = new ArrayList<>();
        for (String part : body.split(",")) {
            parts.add(part.trim());
        }

        int x = parts.isEmpty() ? 0 : (int) Math.round(Double.parseDouble(parts.get(0)));
        int y = parts.size() > 1 ? (int) Math.round(Double.parseDouble(parts.get(1))) : 0;
        int z = parts.size() > 2 ? (int) Math.round(Double.parseDouble(parts.get(2))) : 0;
        return new int[] {x, y, z};
    }

    private static String normalizeAnchor(String anchor) {
        return anchor.trim().toLowerCase();
    }
}

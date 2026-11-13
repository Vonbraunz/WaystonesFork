package net.blay09.mods.waystones.util;

import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.minecraft.entity.player.EntityPlayer;

public class WaystoneXpCost {

    // full exemption for the listed dimensions. costs nothing, no base, no distance, no cross-dim surcharge.
    public static boolean isFreeDimension(int dimensionId) {
        int[] freeDimensions = Waystones.getConfig().xpFreeDimensions;
        if (freeDimensions == null) {
            return false;
        }
        for (int freeDimension : freeDimensions) {
            if (freeDimension == dimensionId) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFreeTeleport(int fromDimensionId, WaystoneEntry to) {
        if (!isFreeDimension(to.getDimensionId())) {
            return false;
        }
        // if the source has to match as well, warping out of a paid dimension into the overworld still costs
        return !Waystones.getConfig().xpFreeDimensionsMatchSource || isFreeDimension(fromDimensionId);
    }

    public static int getXpCost(EntityPlayer player, WaystoneEntry to) {
        if (Waystones.getConfig().xpBaseCost < 0) {
            return -1;
        }
        if (isFreeTeleport(player.worldObj.provider.dimensionId, to)) {
            return 0;
        }
        int base = Waystones.getConfig().xpBaseCost;
        int blocksPerLevel = Waystones.getConfig().xpBlocksPerLevel;

        if (blocksPerLevel <= 0) {
            return base;
        }

        // Cross-dimension cost
        if (player.worldObj.provider.dimensionId != to.getDimensionId()) {
            return base + Waystones.getConfig().xpCrossDimCost;
        }

        double dx = player.posX - to.getPos()
            .getX();
        double dy = player.posY - to.getPos()
            .getY();
        double dz = player.posZ - to.getPos()
            .getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return base + (int) Math.floor(distance / blocksPerLevel);
    }

    public static WaystoneEntry entryFromTile(TileWaystone tile, EntityPlayer player) {
        WaystoneEntry[] entries = WaystoneEntry.getCombinedWaystones(player, false);
        for (WaystoneEntry w : entries) {
            if (WaystoneEntry.tileAndEntryShareCoords(w, tile)) {
                return w;
            }
        }
        return null;
    }

    public static int getXpCost(TileWaystone from, WaystoneEntry to, EntityPlayer player) {
        if (Waystones.getConfig().xpBaseCost < 0) {
            return -1;
        }
        if (from == null) {
            return getXpCost(player, to);
        }
        WaystoneEntry fromEntry = entryFromTile(from, player);
        if (fromEntry != null && isFreeTeleport(fromEntry.getDimensionId(), to)) {
            return 0;
        }
        int base = Waystones.getConfig().xpBaseCost;
        int blocksPerLevel = Waystones.getConfig().xpBlocksPerLevel;

        if (blocksPerLevel <= 0) {
            return base;
        }

        // Cross-dimension cost
        if (fromEntry != null) {
            if (fromEntry.getDimensionId() != to.getDimensionId()) {
                return base + Waystones.getConfig().xpCrossDimCost;
            }
        }

        double dx = from.xCoord - to.getPos()
            .getX();
        double dy = from.yCoord - to.getPos()
            .getY();
        double dz = from.zCoord - to.getPos()
            .getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return base + (int) Math.floor(distance / blocksPerLevel);
    }
}

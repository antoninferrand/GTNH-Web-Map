package org.dynmap.forge;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Main-server-thread adapter; never called from map rendering workers. */
final class VendingTileStates {
    private VendingTileStates() {}

    static Map<VendingRenderUpdates.Position, Integer> read(World world) {
        Map<VendingRenderUpdates.Position, Integer> states = new HashMap<>();
        for (Object object : world.loadedTileEntityList) {
            if (!(object instanceof TileEntity) || !(object instanceof IGregTechTileEntity)) continue;
            TileEntity tile = (TileEntity) object;
            if (tile.isInvalid()) continue;
            IGregTechTileEntity machine = (IGregTechTileEntity) object;
            int id = machine.getMetaTileID();
            if (id != 2741 && id != 2742) continue;
            int state = (id << 4) | (machine.getFrontFacing().ordinal() & 7) | (machine.isActive() ? 8 : 0);
            states.put(new VendingRenderUpdates.Position(tile.xCoord, tile.yCoord, tile.zCoord), state);
        }
        return states;
    }
}

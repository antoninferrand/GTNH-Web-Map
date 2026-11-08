package org.dynmap.modsupport.loaders;

import appeng.items.parts.ItemMultiPart;
import appeng.items.parts.PartType;
import net.minecraft.item.ItemStack;
import org.dynmap.Log;
import org.dynmap.DynmapCore;
import org.dynmap.modsupport.GWM_Util;
import org.dynmap.modsupport.appliedenergistics2.AE2Support;
import org.dynmap.renderer.CustomModSupportLoader;

import java.util.HashMap;

public class AppliedEnergisticsModSupportLoader  extends CustomModSupportLoader {
    HashMap<String, Integer> filetoidx;
    private boolean registryWarningLogged;
    @Override
    public void initializeModSupport(DynmapCore core, HashMap<String, Integer> filetoidx) {
        GWM_Util.initialize(core);
        this.filetoidx = filetoidx;
    }

    @Override
    public void processData(String line, HashMap<String, String> data) {
        String strDamage = data.get("damage");
        String texture = data.get("tex");
        String texture2 = data.get("tex2");
        String texture3 = data.get("tex3");
        String size = data.get("size");

        int damage = Integer.parseInt(strDamage);
        if (damage < 0 || damage >= AE2Support.cableTypes.length) {
            Log.warning("Ignoring AE2 cable mapping with invalid damage " + damage);
            return;
        }
        try {
            ItemMultiPart parts = ItemMultiPart.instance;
            if (parts != null) {
                // Legacy ultra-dense cable metadata is no longer registered by
                // current AE2. Check the actual registry before resolving images:
                // unused legacy images must not become required dynamic tiles.
                if (parts.getTypeByStack(new ItemStack(parts, 1, damage)) == PartType.InvalidType) {
                    return;
                }
            } else {
                warnRegistryUnavailable();
            }
        } catch (LinkageError | RuntimeException unavailableRegistry) {
            // Preserve old-pack support if the optional API changes. A genuine
            // required texture miss must remain visible rather than be hidden.
            warnRegistryUnavailable();
        }
        AE2Support.addCableType(filetoidx, damage, texture, texture2, texture3, Integer.parseInt(size));
    }

    private void warnRegistryUnavailable() {
        if (!registryWarningLogged) {
            Log.warning("AE2 part registry unavailable; retaining declared cable textures");
            registryWarningLogged = true;
        }
    }

}

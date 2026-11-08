package org.dynmap.modsupport.loaders;

import org.dynmap.DynmapCore;
import org.dynmap.Log;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.GWM_Util;
import org.dynmap.modsupport.ic2.CropsSupport;
import org.dynmap.renderer.CustomModSupportLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IC2CropsSupportLoader extends CustomModSupportLoader {
    private HashMap<String, Integer> filetoidx;
    // Null means registry discovery was unavailable, so preserve legacy behavior.
    private Map<String, Set<String>> registeredOwnersByName;
    private int skippedDefinitions;
    private int skippedStageReferences;

    @Override
    public void initializeModSupport(DynmapCore core, HashMap<String, Integer> filetoidx) {
        this.filetoidx = filetoidx;
        registeredOwnersByName = null;
        skippedDefinitions = 0;
        skippedStageReferences = 0;
        if (core.getServer().isModLoaded("IC2")) {
            try {
                registeredOwnersByName = IC2RegistryReader.read();
                if (registeredOwnersByName == null) {
                    Log.warning("IC2 crop registry is not initialized; retaining legacy crop support definitions");
                }
            } catch (LinkageError | RuntimeException e) {
                Log.warning("Unable to inspect IC2 crop registry; retaining legacy crop support definitions", e);
            }
        }
    }

    @Override
    public void processData(String line, HashMap<String, String> data) {
        String crop = data.get("crop");
        String owner = data.get("owner");
        int steps = GWM_Util.objectToInt(data.get("steps"), 1);
        if (registeredOwnersByName != null) {
            Set<String> owners = registeredOwnersByName.get(crop);
            if (owners == null || (owner != null && !owners.contains(owner))) {
                skippedDefinitions++;
                skippedStageReferences += steps;
                Log.info("Skipping unregistered IC2 crop support " + (owner == null ? "" : owner + ":") + crop
                        + " (" + skippedDefinitions + " definitions, " + skippedStageReferences
                        + " stage references skipped by this loader)");
                return;
            }
        }

        int[] arr = new int[steps];
        for (int i = 0; i < steps; i++) {
            arr[i] = TexturePack.parseTextureIndex(filetoidx, data.get("step" + i));
        }
        CropsSupport.registerCrop(crop, arr);
        super.processData(line, data);
    }

    public int getSkippedDefinitionCount() {
        return skippedDefinitions;
    }

    public int getSkippedStageReferenceCount() {
        return skippedStageReferences;
    }

    // Keep optional IC2 classes out of the loader's fields and method signatures.
    // Texture support loads at server start, after mods register their crop cards.
    private static final class IC2RegistryReader {
        private static Map<String, Set<String>> read() {
            if (ic2.api.crops.Crops.instance == null) {
                return null;
            }
            Map<String, Set<String>> result = new HashMap<>();
            for (ic2.api.crops.CropCard card : ic2.api.crops.Crops.instance.getCrops()) {
                Set<String> owners = result.get(card.name());
                if (owners == null) {
                    owners = new HashSet<>();
                    result.put(card.name(), owners);
                }
                owners.add(card.owner());
            }
            return result;
        }
    }
}

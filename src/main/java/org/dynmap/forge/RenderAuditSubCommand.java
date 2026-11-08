package org.dynmap.forge;

import com.google.gson.GsonBuilder;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import org.dynmap.DynmapCore;
import org.dynmap.Log;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.hdmap.TextureResourceResolver;
import org.dynmap.modsupport.gregtech.GregTechSupport;

/** Exports loaded mappings and resource outcomes; never changes world or render state. */
final class RenderAuditSubCommand extends GwmSubCommand {
    private final DynmapPlugin plugin;

    RenderAuditSubCommand(DynmapPlugin plugin) {
        super("renderaudit");
        this.plugin = plugin;
    }

    @Override
    protected void process(ICommandSender sender, String[] args) {
        DynmapCore core = plugin.getAuditCore();
        if (core == null) {
            sender.addChatMessage(new ChatComponentText("[GWM] Map core is not initialized"));
            return;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", 1);
        report.put("scope", "Effective loaded mappings and resource outcomes; visual correctness requires fixture comparison");
        Map<String, String> versions = new LinkedHashMap<>();
        for (String mod : new String[] { "gregtech", "cropsnh", "vendingmachine", "appliedenergistics2", "IC2" }) {
            versions.put(mod, core.getServer().getModVersion(mod));
        }
        report.put("versions", versions);

        List<Map<String, Object>> blocks = new ArrayList<>();
        int blankMetadata = 0;
        for (Map.Entry<Integer, String> entry : new TreeMap<>(core.getServer().getBlockIDMap()).entrySet()) {
            if (entry.getKey() == 0) continue;
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("id", entry.getKey());
            block.put("registryName", entry.getValue());
            List<Integer> blank = new ArrayList<>();
            for (int meta = 0; meta < 16; meta++) {
                if (TexturePack.isBlankBlockMapping(entry.getKey(), meta)) blank.add(meta);
            }
            blankMetadata += blank.size();
            block.put("blankMetadataCandidates", blank);
            blocks.add(block);
        }
        report.put("blocks", blocks);
        report.put("blankMetadataNote", "Candidates include unused metadata; explicit invisible mappings are defined, not blank");

        List<Map<String, Object>> machines = new ArrayList<>();
        int unsupported = 0;
        if (core.getServer().isModLoaded("gregtech")) {
            for (int id = 0; id < GregTechAPI.METATILEENTITIES.length; id++) {
                IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
                if (prototype == null) continue;
                GregTechSupport.MetaTileEntityEntry support = GregTechSupport.INSTANCE.getMTEEntre(id);
                Map<String, Object> machine = new LinkedHashMap<>();
                machine.put("id", id);
                machine.put("name", prototype.getMetaName());
                machine.put("class", prototype.getClass().getName());
                machine.put("mapped", support != null);
                if (support == null) {
                    unsupported++;
                } else {
                    machine.put("type", support.type.toString());
                    machine.put("hasBase", support.baseTexture >= 0 || support.baseTextureSet != null
                            || !support.stateBaseTextures.isEmpty() || !support.casingStateBaseTextures.isEmpty());
                    machine.put("stateField", support.stateField);
                    machine.put("casingStateField", support.casingStateField);
                }
                machines.add(machine);
            }
        }
        report.put("mtes", machines);

        Map<String, Object> resources = new LinkedHashMap<>();
        int requiredMissing = 0, optionalAbsent = 0, fallbacks = 0;
        Map<String, Collection<TextureResourceResolver.Lookup>> loaded = TexturePack.getLoadedResourceLookups();
        report.put("resourceCoverage", loaded.isEmpty() ? "NOT_LOADED_RENDER_FIRST" : "LOADED_PACKS_ONLY");
        for (Map.Entry<String, Collection<TextureResourceResolver.Lookup>> pack : loaded.entrySet()) {
            List<Map<String, Object>> lookups = new ArrayList<>();
            for (TextureResourceResolver.Lookup lookup : pack.getValue()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("resource", lookup.resource);
                row.put("mod", lookup.mod);
                row.put("resolvedResource", lookup.resolvedResource);
                row.put("resolvedMod", lookup.resolvedMod);
                row.put("outcome", lookup.outcome.toString());
                if (lookup.outcome == TextureResourceResolver.Outcome.REQUIRED_MISSING) requiredMissing++;
                if (lookup.outcome == TextureResourceResolver.Outcome.OPTIONAL_ABSENT) optionalAbsent++;
                if (lookup.outcome == TextureResourceResolver.Outcome.FALLBACK) fallbacks++;
                lookups.add(row);
            }
            resources.put(pack.getKey(), lookups);
        }
        report.put("resourcePacks", resources);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("registeredBlocks", blocks.size());
        summary.put("blankMetadataCandidates", blankMetadata);
        summary.put("registeredMtes", machines.size());
        summary.put("unmappedMtes", unsupported);
        summary.put("requiredMissingResources", requiredMissing);
        summary.put("optionalAbsentResources", optionalAbsent);
        summary.put("fallbackResources", fallbacks);
        report.put("summary", summary);

        Path output = core.getDataFolder().toPath().resolve("render-audit.json");
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(report, writer);
            sender.addChatMessage(new ChatComponentText("[GWM] Render audit: " + output
                    + "; unmapped MTEs=" + unsupported + ", required missing resources=" + requiredMissing
                    + (loaded.isEmpty() ? " (resource packs not loaded)" : "")));
        } catch (IOException failure) {
            Log.warning("Unable to write render audit: " + failure.getMessage());
            sender.addChatMessage(new ChatComponentText("[GWM] Unable to write render-audit.json; see server log"));
        }
    }
}

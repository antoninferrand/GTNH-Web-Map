package org.dynmap.forge;

import com.google.gson.GsonBuilder;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import org.dynmap.DynmapCore;
import org.dynmap.Log;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.hdmap.HDBlockModels;
import org.dynmap.hdmap.TextureResourceResolver;
import org.dynmap.modsupport.gregtech.GregTechSupport;

/** Exports loaded mappings and resource outcomes; never changes world or render state. */
final class RenderAuditSubCommand extends GwmSubCommand {
    private final DynmapPlugin plugin;

    RenderAuditSubCommand(DynmapPlugin plugin) {
        super("renderaudit");
        this.plugin = plugin;
    }

    /** Inspect saved registry variation definitions without loading icons or touching worlds. */
    private static Map<String, Object> auditChiselVariants(Map<Integer, String> registered) {
        Map<String, Object> result = new TreeMap<>();
        for (Map.Entry<Integer, String> entry : registered.entrySet()) {
            if (!entry.getValue().startsWith("chisel:")) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            result.put(entry.getValue(), row);
            try {
                Block block = Block.getBlockById(entry.getKey());
                if (block == null) {
                    row.put("status", "UNAVAILABLE_BLOCK");
                    continue;
                }
                row.put("class", block.getClass().getName());
                Field helperField;
                try {
                    helperField = inheritedField(block.getClass(), "carverHelper");
                } catch (NoSuchFieldException absentHelper) {
                    row.put("status", "NO_HELPER");
                    continue;
                }
                Object helper = helperField.get(block);
                if (helper == null) {
                    row.put("status", "NULL_HELPER");
                    continue;
                }
                row.put("helperClass", helper.getClass().getName());
                Object map = inheritedField(helper.getClass(), "infoMap").get(helper);
                if (!(map instanceof Object[])) {
                    row.put("status", "UNAVAILABLE_INFO_MAP");
                    continue;
                }
                Object[] variants = (Object[]) map;
                int mask = 0;
                List<Integer> metadata = new ArrayList<>();
                for (int meta = 0; meta < Math.min(16, variants.length); meta++) {
                    if (variants[meta] == null) continue;
                    mask |= 1 << meta;
                    metadata.add(meta);
                }
                row.put("status", "OK");
                row.put("infoMapLength", variants.length);
                row.put("nonnullMetadataMask", mask);
                row.put("nonnullMetadata", metadata);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                row.put("status", "UNAVAILABLE");
                row.put("diagnostic", unavailable.getClass().getName() + ": " + unavailable.getMessage());
            }
        }
        return result;
    }

    private static Field inheritedField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException notHere) {
                // Some Chisel blocks inherit their helper or helper implementation.
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    @Override
    protected void process(ICommandSender sender, String[] args) {
        DynmapCore core = plugin.getAuditCore();
        if (core == null) {
            sender.addChatMessage(new ChatComponentText("[GWM] Map core is not initialized"));
            return;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", 2);
        report.put("scope", "Effective loaded mappings and resource outcomes; visual correctness requires fixture comparison");
        Map<String, String> versions = new LinkedHashMap<>();
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            versions.put(mod.getModId(), mod.getVersion());
        }
        report.put("versions", versions);

        List<Map<String, Object>> blocks = new ArrayList<>();
        int blankMetadata = 0;
        // EndlessIDs' numeric array includes unassigned slots filled with air.
        // Enumerate actual unique registry names rather than reporting 65,535 blocks.
        Map<Integer, String> registered = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : core.getBlockIDMap().entrySet()) {
            registered.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<Integer, String> entry : registered.entrySet()) {
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
        report.put("chiselVariants", auditChiselVariants(registered));
        report.put("blankMetadataNote", "Candidates include unused metadata; explicit invisible mappings are defined, not blank");
        report.put("failedCustomModels", HDBlockModels.getFailedCustomModels());

        List<Map<String, Object>> machines = new ArrayList<>();
        int unsupported = 0, missingBase = 0, unresolvedIcons = 0;
        if (core.getServer().isModLoaded("gregtech")) {
            for (int id = 0; id < GregTechAPI.METATILEENTITIES.length; id++) {
                IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
                if (prototype == null) continue;
                GregTechSupport.MetaTileEntityEntry support = GregTechSupport.INSTANCE.getMTEEntre(id);
                Map<String, Object> machine = new LinkedHashMap<>();
                machine.put("id", id);
                machine.put("name", prototype.getMetaName());
                machine.put("class", prototype.getClass().getName());
                machine.put("blockMetadata", prototype.getTileEntityBaseType());
                machine.put("mapped", support != null);
                if (support == null) {
                    unsupported++;
                } else {
                    machine.put("type", support.type.toString());
                    machine.put("hasBase", support.hasBaseDefinition());
                    if (!support.hasBaseDefinition()) missingBase++;
                    String[] missingIcons = support.getUnresolvedIconSets();
                    machine.put("unresolvedIconSets", missingIcons);
                    unresolvedIcons += missingIcons.length;
                    machine.put("stateField", support.stateField);
                    machine.put("casingStateField", support.casingStateField);
                }
                machines.add(machine);
            }
        }
        report.put("mtes", machines);

        Map<String, Object> resources = new LinkedHashMap<>();
        int requiredMissing = 0, optionalAbsent = 0, fallbacks = 0, inactiveVariants = 0;
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
                String role = TexturePack.getResourceRole(pack.getKey(), lookup.resource);
                boolean inactiveMissing = lookup.outcome == TextureResourceResolver.Outcome.REQUIRED_MISSING
                        && "INACTIVE_TERRAIN_VARIANT".equals(role);
                row.put("role", role);
                row.put("rawOutcome", lookup.outcome.toString());
                row.put("outcome", inactiveMissing ? role : lookup.outcome.toString());
                if (inactiveMissing) inactiveVariants++;
                else if (lookup.outcome == TextureResourceResolver.Outcome.REQUIRED_MISSING) requiredMissing++;
                if (lookup.outcome == TextureResourceResolver.Outcome.OPTIONAL_ABSENT) optionalAbsent++;
                if (lookup.outcome == TextureResourceResolver.Outcome.FALLBACK) fallbacks++;
                lookups.add(row);
            }
            resources.put(pack.getKey(), lookups);
        }
        report.put("resourcePacks", resources);
        report.put("inactiveResourceDeclarations", TexturePack.getInactiveResourceDeclarations());
        report.put("inactiveTerrainVariants", TexturePack.getInactiveTerrainVariants());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("registeredBlocks", blocks.size());
        summary.put("blankMetadataCandidates", blankMetadata);
        summary.put("registeredMtes", machines.size());
        summary.put("unmappedMtes", unsupported);
        summary.put("mappedMtesWithoutBase", missingBase);
        summary.put("unresolvedMteIconSets", unresolvedIcons);
        summary.put("inactiveMissingTerrainVariants", inactiveVariants);
        summary.put("requiredMissingResources", requiredMissing);
        summary.put("optionalAbsentResources", optionalAbsent);
        summary.put("fallbackResources", fallbacks);
        summary.put("failedCustomModelBlocks", HDBlockModels.getFailedCustomModels().size());
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

package org.dynmap.hdmap.renderer;

import java.util.List;
import java.util.Map;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.renderer.CustomColorMultiplier;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.CustomRendererData;
import org.dynmap.renderer.CustomTextureMapper;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Blood Magic's mimic texture comes from its slot0 ItemBlock, not a standalone PNG. */
public class InventoryMimicRenderer extends CustomRenderer {
    private RenderPatch[] cube;

    @Override
    public boolean initializeRenderer(RenderPatchFactory factory, int id, int mask, Map<String, String> parameters) {
        cube = getBoxFull(factory, new int[] { 1, 4, 0, 3, 2, 5 });
        return super.initializeRenderer(factory, id, mask, parameters);
    }

    @Override
    public int getMaximumTextureCount() {
        return 6;
    }

    @Override
    public String[] getTileEntityFieldsNeeded() {
        return new String[] { "Inventory" };
    }

    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext context) {
        return cube;
    }

    @Override
    public CustomRendererData getRenderData(MapDataContext context) {
        TexturePack.HDTextureMap selected = TexturePack.HDTextureMap.getMap(context.getBlockTypeID(), context.getBlockData(), 0);
        Object inventory = context.getBlockTileEntityField("Inventory");
        if (inventory instanceof List<?>) {
            for (Object entry : (List<?>) inventory) {
                if (!(entry instanceof Map<?, ?>)) continue;
                Map<?, ?> slot = (Map<?, ?>) entry;
                if (number(slot.get("Slot"), -1) != 0) continue;
                int block = blockForItem(slot.get("id"));
                int meta = number(slot.get("Damage"), 0);
                if (block > 0 && block < 65536 && meta >= 0 && meta < 16) {
                    selected = TexturePack.HDTextureMap.getMap(block, meta, 0);
                }
                break;
            }
        }
        final TexturePack.HDTextureMap texture = selected;
        CustomTextureMapper mapper = new CustomTextureMapper() {
            @Override
            public int[] getTextureLayersForPatchId(int patchId) {
                return texture == null ? null : new int[] { texture.getIndexForFace(patchId) };
            }
        };
        CustomColorMultiplier color = texture == null || texture.getColorMult() == 0 ? null : new CustomColorMultiplier() {
            @Override
            public int getColorMultiplier(MapDataContext ignored) {
                return texture.getColorMult();
            }
        };
        return new CustomRendererData(cube, color, mapper);
    }

    protected int blockForItem(Object id) {
        return MinecraftItems.blockFor(id);
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static final class MinecraftItems {
        private static int blockFor(Object id) {
            net.minecraft.item.Item item;
            if (id instanceof String) {
                item = (net.minecraft.item.Item) net.minecraft.item.Item.itemRegistry.getObject((String) id);
            } else if (id instanceof Number) {
                int number = ((Number) id).intValue();
                if (id instanceof Short) number &= 65535;
                item = net.minecraft.item.Item.getItemById(number);
            } else return 0;
            if (!(item instanceof net.minecraft.item.ItemBlock)) return 0;
            return net.minecraft.block.Block.getIdFromBlock(net.minecraft.block.Block.getBlockFromItem(item));
        }
    }
}

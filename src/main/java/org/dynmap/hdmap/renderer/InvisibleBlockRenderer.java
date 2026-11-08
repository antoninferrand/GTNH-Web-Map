package org.dynmap.hdmap.renderer;

import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;

/** For source-defined invisible helper blocks which have no world renderer. */
public class InvisibleBlockRenderer extends CustomRenderer {
    private static final RenderPatch[] EMPTY = new RenderPatch[0];

    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext context) {
        return EMPTY;
    }
}

package org.dynmap.modsupport.loaders;

import java.util.HashMap;
import org.dynmap.DynmapCore;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.ModelAtlasSupport;
import org.dynmap.renderer.CustomModSupportLoader;

public class ModelAtlasSupportLoader extends CustomModSupportLoader {
    private HashMap<String, Integer> files;

    @Override
    public void initializeModSupport(DynmapCore core, HashMap<String, Integer> files) {
        this.files = files;
    }

    @Override
    public void processData(String line, HashMap<String, String> data) {
        int columns = Integer.parseInt(data.get("columns")), rows = Integer.parseInt(data.get("rows"));
        if (columns < 1 || rows < 1 || (long) columns * rows > 4096)
            throw new IllegalArgumentException("Invalid model atlas dimensions");
        int[] cells = new int[columns * rows];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = TexturePack.parseTextureIndex(files, i + ":" + data.get("texture"));
        }
        ModelAtlasSupport.register(data.get("name"), cells);
    }
}

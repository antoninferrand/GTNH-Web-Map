package org.dynmap.hdmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.dynmap.DynmapCore;
import org.dynmap.Log;
import org.dynmap.common.DynmapServerInterface;

public class TexturePackLoader {
    private ZipFile zf;
    private File tpdir;
    private DynmapServerInterface dsi;
    private static final String RESOURCEPATH = "texturepacks/standard";
    
    private static class ModSource {
        ZipFile zf;
        File directory;
    }
    private HashMap<String, ModSource> src_by_mod = new HashMap<String, ModSource>();
    
    private final Map<String, TextureResourceResolver.Lookup> resourceLookups = new LinkedHashMap<String, TextureResourceResolver.Lookup>();

    public TexturePackLoader(File tp, DynmapCore core) {        
        if (tp.isFile() && tp.canRead()) {
            try {
                zf = new ZipFile(tp);
            } catch (IOException e) {
                Log.severe("Error opening texture pack - " + tp.getPath());
            }
        }
        else if (tp.isDirectory() && tp.canRead()) {
            tpdir = tp;
        }
        else {
            Log.info("Texture pack not found - " + tp.getPath());
        }
        dsi = core.getServer();
    }
    public InputStream openTPResource(String rname, String altname) {
        InputStream is = openTPResource(rname);
        if (is == null) {
            if (altname != null) {
                is = openTPResource(altname);
            }
        }
        return is;
    }
    public InputStream openTPResource(String rname) {
        return openModTPResource(rname, null);
    }
    
    public InputStream openModTPResource(String rname, String modname) {
        return openModTPResource(rname, modname, null, null, false);
    }

    public InputStream openModTPResource(String rname, String modname, String fallback, String fallbackmod, boolean optional) {
        TextureResourceResolver.Result result = TextureResourceResolver.open(new TextureResourceResolver.Source() {
            @Override
            public InputStream open(String resource, String mod) {
                return openResourceQuietly(resource, mod);
            }
        }, rname, modname, fallback, fallbackmod, optional);
        resourceLookups.put(String.valueOf(modname) + ":" + rname, result.lookup);
        if (result.lookup.outcome == TextureResourceResolver.Outcome.REQUIRED_MISSING && modname != null) {
            Log.warning("Resource " + rname + " for mod " + modname + " not found"
                    + (fallback == null ? "" : " (fallback " + fallback + " also missing)"));
        }
        return result.stream;
    }

    /** Snapshot for diagnostics; closing the loader releases its retained lookup metadata. */
    public Collection<TextureResourceResolver.Lookup> getResourceLookups() {
        return Collections.unmodifiableList(new ArrayList<TextureResourceResolver.Lookup>(resourceLookups.values()));
    }

    private InputStream openResourceQuietly(String rname, String modname) {
        try {
            if (zf != null) {
                ZipEntry ze = zf.getEntry(rname);
                if ((ze != null) && (!ze.isDirectory())) {
                    return zf.getInputStream(ze);
                }
            }
            else if (tpdir != null) {
                File f = new File(tpdir, rname);
                if (f.isFile() && f.canRead()) {
                    return new FileInputStream(f);
                }
            }
        } catch (IOException iox) {
        }
        // Fall through - load as resource from mod, if possible, or from jar
        InputStream is = dsi.openResource(modname, rname);
        if (is != null) { 
            return is;
        }
        if (modname != null) {
            ModSource ms = src_by_mod.get(modname);
            if (ms == null) {
                File f = dsi.getModContainerFile(modname);
                ms = new ModSource();
                if (f != null) {
                    if (f.isFile()) {
                        try {
                            ms.zf = new ZipFile(f);
                        } catch (IOException e) {
                        }
                    }
                    else {
                        ms.directory = f;
                    }
                }
                src_by_mod.put(modname, ms);
            }
            try {
                if (ms.zf != null) {
                    ZipEntry ze = ms.zf.getEntry(rname);
                    if ((ze != null) && (!ze.isDirectory())) {
                        is = ms.zf.getInputStream(ze);
                    }
                }
                else if (ms.directory != null) {
                    File f = new File(ms.directory, rname);
                    if (f.isFile() && f.canRead()) {
                        is = new FileInputStream(f);
                    }
                }
            } catch (IOException iox) {
            }
        }
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(RESOURCEPATH + "/" + rname);
        }
        return is;
    }
    public void close() {
        if(zf != null) {
            try { zf.close(); } catch (IOException iox) {}
            zf = null;
        }
        for (ModSource ms : src_by_mod.values()) {
            if (ms.zf != null) {
                try { ms.zf.close(); } catch (IOException iox) {}
            }
        }
        src_by_mod.clear();
        resourceLookups.clear();
    }
    public void closeResource(InputStream is) {
        try {
            if (is != null)
                is.close();
        } catch (IOException iox) {
        }
    }
    public Set<String> getEntries() {
        HashSet<String> rslt = new HashSet<String>();
        if (zf != null) {
            Enumeration<? extends ZipEntry> lst = zf.entries();
            while(lst.hasMoreElements()) {
                rslt.add(lst.nextElement().getName());
            }
        }
        if (tpdir != null) {
            addFiles(rslt, tpdir, "");
        }
        return rslt;
    }
    
    private void addFiles(HashSet<String> files, File dir, String path) {
        File[] listfiles = dir.listFiles();
        if(listfiles == null) return;
        for(File f : listfiles) {
            String fn = f.getName();
            if(fn.equals(".") || (fn.equals(".."))) continue;
            if(f.isFile()) {
                files.add(path + "/" + fn);
            }
            else if(f.isDirectory()) {
                addFiles(files, f, path + "/" + f.getName());
            }
        }
    }

}

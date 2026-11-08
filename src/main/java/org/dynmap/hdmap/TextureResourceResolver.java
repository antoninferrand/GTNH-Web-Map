package org.dynmap.hdmap;

import java.io.InputStream;

/** Resolves explicit texture alternatives without importing client-only mod classes. */
public final class TextureResourceResolver {
    private TextureResourceResolver() {}

    public interface Source {
        InputStream open(String resource, String mod);
    }

    public enum Outcome {
        ORIGINAL, FALLBACK, OPTIONAL_ABSENT, REQUIRED_MISSING
    }

    /** Lookup metadata deliberately contains no stream or globally retained state. */
    public static final class Lookup {
        public final String resource, mod, resolvedResource, resolvedMod;
        public final Outcome outcome;

        private Lookup(String resource, String mod, String resolvedResource, String resolvedMod, Outcome outcome) {
            this.resource = resource;
            this.mod = mod;
            this.resolvedResource = resolvedResource;
            this.resolvedMod = resolvedMod;
            this.outcome = outcome;
        }
    }

    public static final class Result {
        public final InputStream stream;
        public final Lookup lookup;

        private Result(InputStream stream, Lookup lookup) {
            this.stream = stream;
            this.lookup = lookup;
        }
    }

    /**
     * The original resource is searched through the complete pack/mod/bundled stack
     * before the explicitly declared fallback. The caller owns the returned stream.
     */
    public static Result open(Source source, String resource, String mod,
                              String fallback, String fallbackMod, boolean optional) {
        InputStream stream = source.open(resource, mod);
        if (stream != null) {
            return new Result(stream, new Lookup(resource, mod, resource, mod, Outcome.ORIGINAL));
        }
        if (fallback != null) {
            String owner = fallbackMod == null ? mod : fallbackMod;
            stream = source.open(fallback, owner);
            if (stream != null) {
                return new Result(stream, new Lookup(resource, mod, fallback, owner, Outcome.FALLBACK));
            }
        }
        return new Result(null, new Lookup(resource, mod, null, null,
                optional ? Outcome.OPTIONAL_ABSENT : Outcome.REQUIRED_MISSING));
    }
}

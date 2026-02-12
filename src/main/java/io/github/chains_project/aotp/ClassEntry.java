package io.github.chains_project.aotp;

/**
 * Base representation of a HotSpot {@code Klass} record in the RW region.
 * https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/oops/klass.hpp#L62
 * 
 * This has fixed size of 200 bytes including padding.
 */
public abstract class ClassEntry {

    public final long vTablePointer;
    public final int layoutHelper;
    public final int kind;
    public final long miscFlags;
    public final int superCheckOffset;
    public final long _name; // symbol pointer (absolute address)
    public final long secondarySuperCache;
    public final long secondarySupers;
    public final long[] primarySupers;
    public final long javaMirror; // this is oopHandle but it basically stores a pointer to oop
    public final long _super;
    public final long subklass;
    public final long nextSibling;
    public final long nextLink;
    public final long classLoaderData;
    public final long prototypeHeader;
    public final long secondarySupersBitmap;
    public final byte hashSlot;
    public final short sharedClassPathIndex;
    public final short aotClassFlags;
    public final int vtableLen;
    public final int archivedMirrorIndex;
    public final long jfrTrace;

    protected ClassEntry(long vTablePointer,
                         int layoutHelper,
                         short kind,
                         byte miscFlags,
                         int superCheckOffset,
                         long _name,
                         long secondarySuperCache,
                         long secondarySupers,
                         long[] primarySupers,
                         long javaMirror,
                         long _super,
                         long subklass,
                         long nextSibling,
                         long nextLink,
                         long classLoaderData,
                         long prototypeHeader,
                         long secondarySupersBitmap,
                         byte hashSlot,
                         short sharedClassPathIndex,
                         short aotClassFlags,
                         int vtableLen,
                         int archivedMirrorIndex,
                         long jfrTrace) {
        this.vTablePointer = vTablePointer;
        this.layoutHelper = layoutHelper;
        this.kind = kind;
        this.miscFlags = miscFlags;
        this.superCheckOffset = superCheckOffset;
        this._name = _name;
        this.secondarySuperCache = secondarySuperCache;
        this.secondarySupers = secondarySupers;
        // _primary_super_limit = 8
        this.primarySupers = new long[8];
        System.arraycopy(primarySupers, 0, this.primarySupers, 0, 8);
        this.javaMirror = javaMirror;
        this._super = _super;
        this.subklass = subklass;
        this.nextSibling = nextSibling;
        this.nextLink = nextLink;
        this.classLoaderData = classLoaderData;
        this.prototypeHeader = prototypeHeader;
        this.secondarySupersBitmap = secondarySupersBitmap;
        this.hashSlot = hashSlot;
        this.sharedClassPathIndex = sharedClassPathIndex;
        this.aotClassFlags = aotClassFlags;
        this.vtableLen = vtableLen;
        this.archivedMirrorIndex = archivedMirrorIndex;
        this.jfrTrace = jfrTrace;
    }

    /**
     * Convenience accessor for the class name symbol pointer.
     */
    public long namePointer() {
        return _name;
    }
}


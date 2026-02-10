package io.github.chains_project.aotp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

record HeapRootSegments(
    long baseOffset,
    long count,
    int rootsCount,
    long maxSizeInBytes,
    int maxSizeInElems
) {}

/**
 * Size = 48 bytes
 */
record ArchiveMappedHeapHeader(
    long ptrmapStartPos,
    long oopmapStartPos,
    HeapRootSegments heapRootSegments
) {}
/**
 * Size = 40 bytes
 */
record ArchiveStreamedHeapHeader(
    long forwardingOffset,
    long rootsOffset,
    long rootHighestObjectIndexTableOffset,
    long numRoots,
    long numArchivedObjects
) {}

// https://github.com/openjdk/jdk/blob/f4607ed0a7ea2504c1d72dd3dab0b21e583fa0e7/src/hotspot/share/cds/filemap.hpp#L102
public class FileMapHeader {
    long coreRegionAlignment;
    // this is not used for alignment, but it is some value that used at runtime for verification check
    // https://github.com/openjdk/jdk/blob/f4607ed0a7ea2504c1d72dd3dab0b21e583fa0e7/src/hotspot/share/cds/filemap.cpp#L1786
    int objAlignment;
    long narrowOopBase;
    int narrowOopShift;
    boolean compactStrings;
    boolean compactHeaders;
    long maxHeapSize;
    int narrowOopMode;
    boolean objectStreamingMode;
    boolean compressedOops;
    boolean compressedClassPointers;
    int narrowKlassPointerBits;
    int narrowKlassShift;
    long clonedVtablesOffset;
    long earlySerializedDataOffset;
    long serializedDataOffset;
    String jvmIdent;
    long classLocationConfigOffset;
    boolean verifyLocal;
    boolean verifyRemote;
    boolean hasPlatformOrAppClasses;
    long requestedBaseAddress;
    long mappedBaseAddress;
    boolean useOptimizedModuleHandling;
    boolean hasAotLinkedClasses;
    boolean hasFullModuleGraph;
    long rwPtrmapStartPos;
    long roPtrmapStartPos;
    ArchiveMappedHeapHeader archiveMappedHeapHeader;
    ArchiveStreamedHeapHeader archiveStreamedHeapHeader;

    // TODO: these don't show up in the aot.map file. Confirm that their values are encoded,
    byte compilerType;
    int typeProfileLevel;
    int typeProfileArgsLimit;
    int typeProfileParmsLimit;
    long typeProfileWidth;
    long bciProfileWidth;
    boolean profileTraps;
    boolean typeProfileCasts;
    int specTrapLimitExtraEntries;

    public FileMapHeader(LittleEndianRandomAccessFile dis) throws IOException {
        coreRegionAlignment = dis.readLong();
        objAlignment = dis.readInt();

        dis.skipBytes(4);

        narrowOopBase = dis.readLong();
        narrowOopShift = dis.readInt();
        compactStrings = dis.readBoolean();
        compactHeaders = dis.readBoolean();
        dis.skipBytes(2);
        maxHeapSize = dis.readLong();
        narrowOopMode = dis.readInt();
        objectStreamingMode = dis.readBoolean();
        compressedOops = dis.readBoolean();
        compressedClassPointers = dis.readBoolean();
        dis.skipBytes(1);
        narrowKlassPointerBits = dis.readInt();
        narrowKlassShift = dis.readInt();
        clonedVtablesOffset = dis.readLong();
        earlySerializedDataOffset = dis.readLong();
        serializedDataOffset = dis.readLong();
        // jvm_ident is 256 bytes (null-terminated string)
        byte[] jvmIdentBytes = new byte[256];
        dis.read(jvmIdentBytes);
        jvmIdent = new String(jvmIdentBytes, StandardCharsets.UTF_8);
        classLocationConfigOffset = dis.readLong();
        verifyLocal = dis.readBoolean();
        verifyRemote = dis.readBoolean();
        hasPlatformOrAppClasses = dis.readBoolean();
        dis.skipBytes(5);
        requestedBaseAddress = dis.readLong();
        mappedBaseAddress = dis.readLong();
        useOptimizedModuleHandling = dis.readBoolean();
        hasAotLinkedClasses = dis.readBoolean();
        hasFullModuleGraph = dis.readBoolean();
        dis.skipBytes(5);
        rwPtrmapStartPos = dis.readLong();
        roPtrmapStartPos = dis.readLong();
        
        long ptrmapStartPos = dis.readLong();
        long oopmapStartPos = dis.readLong();
        long baseOffset = dis.readLong();
        long count = dis.readLong();
        int rootsCount = dis.readInt();
        dis.skipBytes(4);
        long maxSizeInBytes = dis.readLong();
        int maxSizeInElems = dis.readInt();
        dis.skipBytes(4);
        archiveMappedHeapHeader = new ArchiveMappedHeapHeader(ptrmapStartPos, oopmapStartPos, new HeapRootSegments(baseOffset, count, rootsCount, maxSizeInBytes, maxSizeInElems));

        long forwardingOffset = dis.readLong();
        long rootsOffset = dis.readLong();
        long rootHighestObjectIndexTableOffset = dis.readLong();
        long numRoots = dis.readLong();
        long numArchivedObjects = dis.readLong();
        archiveStreamedHeapHeader = new ArchiveStreamedHeapHeader(forwardingOffset, rootsOffset, rootHighestObjectIndexTableOffset, numRoots, numArchivedObjects);

        compilerType = (byte) (dis.readBoolean() ? 1 : 0);
        typeProfileLevel = dis.readInt();
        dis.skipBytes(3);
        typeProfileArgsLimit = dis.readInt();
        typeProfileParmsLimit = dis.readInt();
        typeProfileWidth = dis.readLong();
        bciProfileWidth = dis.readLong();
        profileTraps = dis.readBoolean();
        typeProfileCasts = dis.readBoolean();
        specTrapLimitExtraEntries = dis.readInt();
        dis.skipBytes(2);
    }

    /**
     * Prints the full file map dump in the same format as C++ FileMapHeader::print:
     * generic header, then each region, then "end regions", then file map header fields.
     */
    public static void print(GenericHeader genericHeader, CDSFileMapRegion[] regions,
            FileMapHeader fileMapHeader, Appendable st) throws IOException {
        genericHeader.print(st);
        for (int i = 0; i < regions.length; i++) {
            regions[i].print(st, i);
        }
        st.append("============ end regions ======== \n");
        fileMapHeader.print(st);
    }

    public void print(Appendable st) throws IOException {
        st.append(String.format("- core_region_alignment:                    %d%n", coreRegionAlignment));
        st.append(String.format("- obj_alignment:                            %d%n", objAlignment));
        st.append(String.format("- narrow_oop_base:                          0x%x%n", narrowOopBase));
        st.append(String.format("- narrow_oop_shift:                         %d%n", narrowOopShift));
        st.append(String.format("- compact_strings:                          %d%n", compactStrings ? 1 : 0));
        st.append(String.format("- compact_headers:                          %d%n", compactHeaders ? 1 : 0));
        st.append(String.format("- max_heap_size:                            %d%n", maxHeapSize));
        st.append(String.format("- narrow_oop_mode:                          %d%n", narrowOopMode));
        st.append(String.format("- compressed_oops:                          %d%n", compressedOops ? 1 : 0));
        st.append(String.format("- compressed_class_ptrs:                    %d%n", compressedClassPointers ? 1 : 0));
        st.append(String.format("- narrow_klass_pointer_bits:                %d%n", narrowKlassPointerBits));
        st.append(String.format("- narrow_klass_shift:                       %d%n", narrowKlassShift));
        st.append(String.format("- cloned_vtables_offset:                    0x%x%n", clonedVtablesOffset));
        st.append(String.format("- early_serialized_data_offset:             0x%x%n", earlySerializedDataOffset));
        st.append(String.format("- serialized_data_offset:                   0x%x%n", serializedDataOffset));
        int nullIdx = jvmIdent != null ? jvmIdent.indexOf(0) : -1;
        String jvmIdentTrimmed = jvmIdent == null ? "" : (nullIdx >= 0 ? jvmIdent.substring(0, nullIdx) : jvmIdent).trim();
        st.append(String.format("- jvm_ident:                                %s%n", jvmIdentTrimmed));
        st.append(String.format("- class_location_config_offset:             0x%x%n", classLocationConfigOffset));
        st.append(String.format("- verify_local:                             %d%n", verifyLocal ? 1 : 0));
        st.append(String.format("- verify_remote:                            %d%n", verifyRemote ? 1 : 0));
        st.append(String.format("- has_platform_or_app_classes:              %d%n", hasPlatformOrAppClasses ? 1 : 0));
        st.append(String.format("- requested_base_address:                   0x%x%n", requestedBaseAddress));
        st.append(String.format("- mapped_base_address:                      0x%x%n", mappedBaseAddress));
        st.append(String.format("- object_streaming_mode:                    %d%n", objectStreamingMode ? 1 : 0));
        st.append("- mapped_heap_header\n");
        st.append("  - root_segments\n");
        ArchiveMappedHeapHeader mh = archiveMappedHeapHeader;
        HeapRootSegments rs = mh != null ? mh.heapRootSegments() : null;
        st.append(String.format("    - roots_count:                          %d%n", rs != null ? rs.rootsCount() : 0));
        st.append(String.format("    - base_offset:                          0x%x%n", rs != null ? rs.baseOffset() : 0L));
        st.append(String.format("    - count:                                %d%n", rs != null ? rs.count() : 0L));
        st.append(String.format("    - max_size_elems:                       %d%n", rs != null ? rs.maxSizeInElems() : 0));
        st.append(String.format("    - max_size_bytes:                       %d%n", rs != null ? rs.maxSizeInBytes() : 0L));
        st.append(String.format("  - oopmap_start_pos:                       %d%n", mh != null ? mh.oopmapStartPos() : 0L));
        st.append(String.format("  - oopmap_ptrmap_pos:                      %d%n", mh != null ? mh.ptrmapStartPos() : 0L));
        st.append("- streamed_heap_header\n");
        ArchiveStreamedHeapHeader sh = archiveStreamedHeapHeader;
        st.append(String.format("  - forwarding_offset:                      %d%n", sh != null ? sh.forwardingOffset() : 0L));
        st.append(String.format("  - roots_offset:                           %d%n", sh != null ? sh.rootsOffset() : 0L));
        st.append(String.format("  - num_roots:                              %d%n", sh != null ? sh.numRoots() : 0L));
        st.append(String.format("  - root_highest_object_index_table_offset: %d%n", sh != null ? sh.rootHighestObjectIndexTableOffset() : 0L));
        st.append(String.format("  - num_archived_objects:                   %d%n", sh != null ? sh.numArchivedObjects() : 0L));
        st.append(String.format("- _rw_ptrmap_start_pos:                     %d%n", rwPtrmapStartPos));
        st.append(String.format("- _ro_ptrmap_start_pos:                     %d%n", roPtrmapStartPos));
        st.append(String.format("- use_optimized_module_handling:            %d%n", useOptimizedModuleHandling ? 1 : 0));
        st.append(String.format("- has_full_module_graph:                    %d%n", hasFullModuleGraph ? 1 : 0));
        st.append(String.format("- has_aot_linked_classes:                   %d%n", hasAotLinkedClasses ? 1 : 0));
    }
}

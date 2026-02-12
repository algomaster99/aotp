package io.github.chains_project.aotp.header;

import java.io.IOException;

import io.github.chains_project.aotp.utils.LittleEndianRandomAccessFile;

public class CDSFileMapRegion {

    private static final String[] REGION_NAMES = { "rw", "ro", "bm", "hp", "ac" };

    public static String regionName(int regionIndex) {
        if (regionIndex >= 0 && regionIndex < REGION_NAMES.length) {
            return REGION_NAMES[regionIndex];
        }
        return "?";
    }
    int crc;
    int readOnly;
    int allowExec;
    int isHeapRegion;
    int isBitmapRegion;
    int mappedFromFile;
    long fileOffset;
    long mappingOffset;
    long used;
    long oopmapOffset;
    long oopmapSizeInBits;
    long ptrmapOffset;
    long ptrmapSizeInBits;
    // Note: mappedBase and inReservedSpace are NOT stored in the file
    // They are runtime-only fields, so we skip them

    public CDSFileMapRegion(LittleEndianRandomAccessFile dis) throws IOException {
        crc = dis.readInt();
        readOnly = dis.readInt();
        allowExec = dis.readInt();
        isHeapRegion = dis.readInt();
        isBitmapRegion = dis.readInt();
        mappedFromFile = dis.readInt();
        
        // Read size_t fields (8 bytes on 64-bit, 4 bytes on 32-bit)
        fileOffset = readSizeT(dis);
        mappingOffset = readSizeT(dis);
        used = readSizeT(dis);
        oopmapOffset = readSizeT(dis);
        oopmapSizeInBits = readSizeT(dis);
        ptrmapOffset = readSizeT(dis);
        ptrmapSizeInBits = readSizeT(dis);
        
        // Mapped base is a pointer so size on 64 bit is 8 bytes
        // _in_reserved_space is a boolean so size is 1 byte + 7 bytes for padding
        dis.skipBytes(8 + 1 + 7);
    }
    
    // AOT cache files are 64-bit only (size_t = 8 bytes)
    // TODO: For 32-bit support, modify this method and add a 4 byte version
    private static long readSizeT(LittleEndianRandomAccessFile dis) throws IOException {
        // size_t is unsigned, but Java long is signed
        // readLong() correctly reads the bytes, but values >= 2^63 will appear negative
        // Use Long.toUnsignedString() or Long.compareUnsigned() when working with these values
        return dis.readLong();
    }

    public void print(Appendable st, int regionIndex) throws IOException {
        st.append(String.format("============ region ============= %d \"%s\"%n", regionIndex, regionName(regionIndex)));
        st.append(String.format("- crc:                            0x%08x%n", crc));
        st.append(String.format("- read_only:                      %d%n", readOnly));
        st.append(String.format("- allow_exec:                     %d%n", allowExec));
        st.append(String.format("- is_heap_region:                 %d%n", isHeapRegion));
        st.append(String.format("- is_bitmap_region:               %d%n", isBitmapRegion));
        st.append(String.format("- mapped_from_file:               %d%n", mappedFromFile));
        st.append(String.format("- file_offset:                    0x%x%n", fileOffset));
        st.append(String.format("- mapping_offset:                 0x%x%n", mappingOffset));
        st.append(String.format("- used:                           %d%n", used));
        st.append(String.format("- oopmap_offset:                  0x%x%n", oopmapOffset));
        st.append(String.format("- oopmap_size_in_bits:            %d%n", oopmapSizeInBits));
        st.append(String.format("- ptrmap_offset:                  0x%x%n", ptrmapOffset));
        st.append(String.format("- ptrmap_size_in_bits:            %d%n", ptrmapSizeInBits));
        st.append(String.format("- mapped_base:                    0x%x%n", 0L)); // not stored in file
    }
}

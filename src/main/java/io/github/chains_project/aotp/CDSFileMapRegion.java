package io.github.chains_project.aotp;

import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;

public class CDSFileMapRegion {
    // Platform-dependent: size_t size
    // AOT cache files are currently 64-bit only (size_t = 8 bytes)
    // For 32-bit support, this would be 4 bytes
    private static final int SIZE_T_SIZE = 8;
    
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

    public CDSFileMapRegion(LittleEndianDataInputStream dis) throws IOException {
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
        
        // Skip mapped_base (1 byte), in_reserved_space (1 byte)
        // These are runtime-only fields that aren't stored in the archive
        // Total structure size is 96 bytes: 24 (ints) + 56 (longs) + 16 (skipped) = 96
        dis.skipBytes(8 + 7 + 1);
    }
    
    // AOT cache files are 64-bit only (size_t = 8 bytes)
    // For 32-bit support, change SIZE_T_SIZE to 4 and modify this method
    private static long readSizeT(LittleEndianDataInputStream dis) throws IOException {
        // size_t is unsigned, but Java long is signed
        // readLong() correctly reads the bytes, but values >= 2^63 will appear negative
        // Use Long.toUnsignedString() or Long.compareUnsigned() when working with these values
        return dis.readLong();
    }
}

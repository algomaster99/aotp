package io.github.chains_project.aotp;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.google.common.io.LittleEndianDataInputStream;

public class Main {

    // Magic number for AOTCache files
    // https://github.com/openjdk/jdk/blob/6f6966b28b2c5a18b001be49f5db429c667d7a8f/src/hotspot/share/include/cds.h#L39
    private static final int AOT_MAGIC = 0xf00baba2;
    
    // Pattern to search for: 0x800001080 (64-bit value, little-endian)
    private static final long PATTERN_VALUE = 0x0000000800001080L;
    
    private static void findAndPrintSymbols(String filePath, CDSFileMapRegion rwRegion, CDSFileMapRegion roRegion, long requestedBaseAddress) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             LittleEndianDataInputStream dis = new LittleEndianDataInputStream(fis)) {
            
            // Seek to the RW region start using FileInputStream.skip()
            long bytesToSkip = rwRegion.fileOffset;
            long totalSkipped = 0;
            while (totalSkipped < bytesToSkip) {
                long skipped = fis.skip(bytesToSkip - totalSkipped);
                if (skipped <= 0) break;
                totalSkipped += skipped;
            }
            
            // Read longs sequentially until we find the pattern in RW region
            long regionSize = rwRegion.used;
            long longsToRead = regionSize / 8;
            
            for (long i = 0; i < longsToRead; i++) {
                long value = dis.readLong();
                if (value == PATTERN_VALUE) {
                    // Found the pattern in RW region, read a few more bytes for the symbol pointer
                    // Read 8 bytes (the symbol pointer)
                    dis.skipBytes(16); // _kind, _misc_flags, juint
                    long symbolPointer = dis.readLong(); // Absolute address

                    // Read the symbol name using the absolute address
                    String symbolName = readSymbolName(filePath, roRegion, symbolPointer, requestedBaseAddress);
                    if (symbolName != null) {
                        System.out.println("Found pattern at RW offset " + (i * 8) + 
                            " (file offset: " + (rwRegion.fileOffset + i * 8) + ")" +
                            ", symbol pointer: 0x" + Long.toHexString(symbolPointer) +
                            ", symbol: " + symbolName);
                    }
                }
            }
        }
    }
    
    /**
     * Reads a symbol name from the ro region using an absolute address.
     * Symbol format: hash_and_refcount (4 bytes), length (2 bytes), body[length] (UTF-8)
     */
    private static String readSymbolName(String filePath, CDSFileMapRegion roRegion, long symbolAbsoluteAddress, long requestedBaseAddress) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             LittleEndianDataInputStream dis = new LittleEndianDataInputStream(fis)) {
            
            // The mapping offset in the file is relative, we need to add the base address
            long fullMappingOffset = requestedBaseAddress + roRegion.mappingOffset;
            
            // Convert absolute address to file offset
            // symbolAbsoluteAddress - fullMappingOffset = offset within RO region
            // roRegion.fileOffset + offset = file position
            long symbolOffset = symbolAbsoluteAddress - fullMappingOffset;
            
            // Verify the symbol is within the RO region bounds
            if (symbolOffset < 0 || symbolOffset >= roRegion.used) {
                return null;
            }
            
            // Seek to the symbol location in the file
            // directly seek to the symbol offset in ro region
            dis.skipBytes((int) (roRegion.fileOffset + symbolOffset));
            
            // Skip hash_and_refcount (4 bytes)
            dis.skipBytes(4);
            
            // Read length (2 bytes, little-endian, unsigned)
            int length = dis.readShort() & 0xFFFF;
            
            if (length < 0 || length > 65535) {
                return null;
            }
            
            // Read the symbol body (UTF-8)
            byte[] nameBytes = new byte[length];
            dis.readFully(nameBytes);
            return new String(nameBytes, StandardCharsets.UTF_8);
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Main <path-to-aot-file>");
            System.exit(1);
        }
        
        String filePath = args[0];
        
        try (FileInputStream fis = new FileInputStream(filePath);
             LittleEndianDataInputStream dis = new LittleEndianDataInputStream(fis)) {
            
            // Read the generic header
            // https://github.com/openjdk/jdk/blob/f4607ed0a7ea2504c1d72dd3dab0b21e583fa0e7/src/hotspot/share/include/cds.h#L84
            GenericHeader header = new GenericHeader(dis);

            if (header.magic == AOT_MAGIC) {
                System.out.println("Valid AOTCache file");
            } else {
                String actualMagic = String.format("%08x", header.magic);
                System.out.println("Invalid AOTCache file: magic number mismatch (actual: " + actualMagic + ")");
                System.exit(1);
            }

            System.out.println("Version: " + header.version);

            // read 5 regions
            CDSFileMapRegion[] regions = new CDSFileMapRegion[5];
            for (int i = 0; i < 5; i++) {
                regions[i] = new CDSFileMapRegion(dis);
                System.out.println("Region " + i + ": used=" + regions[i].used + 
                    ", fileOffset=0x" + Long.toHexString(regions[i].fileOffset) +
                    ", mappingOffset=0x" + Long.toHexString(regions[i].mappingOffset));
            }

            // Read the file map header
            FileMapHeader fileMapHeader = new FileMapHeader(dis);

            System.out.println("Requested base address: 0x" + Long.toHexString(fileMapHeader.requestedBaseAddress));
            
            // Find pattern in RW region and resolve symbols from RO region
            CDSFileMapRegion rwRegion = regions[0]; // Region 0 is RW region
            CDSFileMapRegion roRegion = regions[1]; // Region 1 is RO region
            if (rwRegion.used > 0 && roRegion.readOnly != 0 && roRegion.used > 0) {
                findAndPrintSymbols(filePath, rwRegion, roRegion, fileMapHeader.requestedBaseAddress);
            }

            
        } catch (EOFException e) {
            System.out.println("Invalid AOTCache file: file too short");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
}
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
            
            int magic = dis.readInt();

            if (magic == AOT_MAGIC) {
                System.out.println("Valid AOTCache file");
            } else {
                String actualMagic = String.format("%08x", magic);
                System.out.println("Invalid AOTCache file: magic number mismatch (actual: " + actualMagic + ")");
                System.exit(1);
            }

            // crc32 checksum
            dis.readInt();

            // version
            int version = dis.readInt();
            System.out.println("Version: " + version);

            // header size
            dis.skipBytes(4);

            // base archive name offset
            dis.skipBytes(4);

            // base archive name size
            dis.skipBytes(4);

            // read 5 regions
            CDSFileMapRegion[] regions = new CDSFileMapRegion[5];
            for (int i = 0; i < 5; i++) {
                regions[i] = new CDSFileMapRegion(dis);
                System.out.println("Region " + i + ": used=" + regions[i].used + 
                    ", fileOffset=0x" + Long.toHexString(regions[i].fileOffset) +
                    ", mappingOffset=0x" + Long.toHexString(regions[i].mappingOffset));
            }

            // _core_region_alignment
            long coreRegionAlignment = dis.readLong();
            System.out.println("Core region alignment: " + coreRegionAlignment);

            // _obj_alignment
            int objAlignment = dis.readInt();
            System.out.println("Object alignment: " + objAlignment);
            dis.skipBytes(4);

            // _narrow_oop_base
            long narrowOopBase = dis.readLong();
            System.out.println("Narrow oop base: " + Long.toHexString(narrowOopBase));

            // _narrow_oop_shift
            int narrowOopShift = dis.readInt();
            System.out.println("Narrow oop shift: " + narrowOopShift);

            // compact_strings
            boolean compactStrings = dis.readBoolean();
            System.out.println("Compact strings: " + compactStrings);

            // _compact_headers
            boolean compactHeaders = dis.readBoolean();
            System.out.println("Compact headers: " + compactHeaders);

            // 4 + 2 + 2 -> 2 bytes padding required
            dis.skipBytes(2);

            // _max_heap_size
            long maxHeapSize = dis.readLong();
            System.out.println("Max heap size: " + Long.toUnsignedString(maxHeapSize));

            // _narrow_oop_mode
            int narrowOopMode = dis.readInt();
            System.out.println("Narrow oop mode: " + narrowOopMode);

            // _object_streaming_mode
            boolean objectStreamingMode = dis.readBoolean();
            System.out.println("Object streaming mode: " + objectStreamingMode);

            // _compressed_oops
            boolean compressedOops = dis.readBoolean();
            System.out.println("Compressed oops: " + compressedOops);

            // _compressed_class_ptrs
            boolean compressedClassPointers = dis.readBoolean();
            System.out.println("Compressed class pointers: " + compressedClassPointers);

            dis.skipBytes(1);

            // _narrow_klass_pointer_bits
            int narrowKlassPointerBits = dis.readInt();
            System.out.println("Narrow klass pointer bits: " + narrowKlassPointerBits);

            // _narrow_klass_shift
            int narrowKlassShift = dis.readInt();
            System.out.println("Narrow klass shift: " + narrowKlassShift);
            
            // _cloned_vtables_offset
            long clonedVtablesOffset = dis.readLong();
            System.out.println("Cloned vtables offset: " + Long.toHexString(clonedVtablesOffset));

            // _early_serialized_data_offset
            long earlySerializedDataOffset = dis.readLong();
            System.out.println("Early serialized data offset: " + Long.toHexString(earlySerializedDataOffset));

            // _serialized_data_offset
            long serializedDataOffset = dis.readLong();
            System.out.println("Serialized data offset: " + Long.toHexString(serializedDataOffset));

            // _jvm_ident
            byte[] jvmIdentBytes = new byte[256];
            dis.read(jvmIdentBytes);
            String jvmIdent = new String(jvmIdentBytes, StandardCharsets.UTF_8);
            System.out.println("JVM ident: " + jvmIdent);

            // class_location_config_offset
            long classLocationConfigOffset = dis.readLong();
            System.out.println("Class location config offset: " + Long.toHexString(classLocationConfigOffset));

            // verify_local
            boolean verifyLocal = dis.readBoolean();
            System.out.println("Verify local: " + verifyLocal);

            // verify_remote
            boolean verifyRemote = dis.readBoolean();
            System.out.println("Verify remote: " + verifyRemote);

            // _has_platform_or_app_classes
            boolean hasPlatformOrAppClasses = dis.readBoolean();
            System.out.println("Has platform or app classes: " + hasPlatformOrAppClasses);

            // padding
            dis.skipBytes(5);

            // requested_base_address
            long requestedBaseAddress = dis.readLong();
            System.out.println("Requested base address: 0x" + Long.toHexString(requestedBaseAddress));
            
            // Find pattern in RW region and resolve symbols from RO region
            CDSFileMapRegion rwRegion = regions[0]; // Region 0 is RW region
            CDSFileMapRegion roRegion = regions[1]; // Region 1 is RO region
            if (rwRegion.used > 0 && roRegion.readOnly != 0 && roRegion.used > 0) {
                findAndPrintSymbols(filePath, rwRegion, roRegion, requestedBaseAddress);
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
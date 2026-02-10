package io.github.chains_project.aotp;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "aotp", description = "Tool to give insight into AOTCache files.")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the AOT cache file.")
    String filePath;

    @Option(names = "--header", description = "Print the file map header.")
    boolean header;

    @Option(names = "--list-classes", description = "List classes found in the RW region.")
    boolean listClasses;

    // Magic number for AOTCache files
    // https://github.com/openjdk/jdk/blob/6f6966b28b2c5a18b001be49f5db429c667d7a8f/src/hotspot/share/include/cds.h#L39
    private static final int AOT_MAGIC = 0xf00baba2;

    // Pattern to search for: 0x800001080 (64-bit value, little-endian)
    private static List<Long> getPatternsforClasses(long baseAddress) {
        return List.of(
            baseAddress + 0x0000000000001080L, // Instance classes
            baseAddress + 0x00000000000018f0L, // Array classes
            baseAddress + 0x0000000000001a60L, // Array classes with primitive type
            baseAddress + 0x0000000000001350L, // java.lang.Class
            baseAddress + 0x0000000000001620L, // jdk.internal.vm.StackChunk
            baseAddress + 0x00000000000014b8L, // References
            baseAddress + 0x00000000000011e8L // Classloader
        );
    }

    private static void findAndPrintClasses(LittleEndianRandomAccessFile file,
            CDSFileMapRegion rwRegion,
            long requestedBaseAddress) throws IOException {
        // Read longs sequentially until we find the pattern in RW region
        // used does not include padding so this may not be the last long in the region
        // but we have to added padding anywhere to parse the file correctly so we
        // should be fine
        List<Long> patterns = getPatternsforClasses(requestedBaseAddress);
        for (long i = 0; i < rwRegion.used; i += 8) {
            long value = file.readLong();
            if (patterns.contains(value)) {
                // Found the pattern in RW region, read 16 more bytes for the symbol pointer
                // https://github.com/openjdk/jdk/blob/bbae38e510efd8877daca5118f45893bb87f6eaa/src/hotspot/share/oops/klass.hpp#L120-L132
                file.skipBytes(16); // _kind, _misc_flags, _super_check_offset
                long symbolPointer = file.readLong(); // Absolute address

                // Read the symbol name using the absolute address
                String className = readSymbolName(file, symbolPointer, requestedBaseAddress);
                if (className != null) {
                    System.out.println(className);
                }
            }
        }
    }

    /**
     * Reads a symbol name from the ro region using an absolute address.
     * Symbol format: hash_and_refcount (4 bytes), length (2 bytes), body[length]
     * (UTF-8)
     */
    private static String readSymbolName(LittleEndianRandomAccessFile file,
            long symbolAbsoluteAddress, long requestedBaseAddress) throws IOException {
        long symbolOffset = symbolAbsoluteAddress - requestedBaseAddress;

        if (symbolOffset < 0 || symbolOffset >= file.length()) {
            return null;
        }

        long currentPos = file.getFilePointer();
        try {
            file.seek(symbolOffset);

            // Skip hash_and_refcount (4 bytes)
            file.skipBytes(4);

            // Read length (2 bytes, little-endian, unsigned)
            int length = file.readShort() & 0xFFFF;

            if (length < 0 || length > 65535) {
                return null;
            }

            // Read the symbol body (UTF-8)
            byte[] nameBytes = new byte[length];
            file.readFully(nameBytes);
            return new String(nameBytes, StandardCharsets.UTF_8);
        } finally {
            file.seek(currentPos);
        }
    }

    @Override
    public Integer call() {
        boolean showHeader = header;
        boolean showClasses = listClasses;
        if (!showHeader && !showClasses) {
            showHeader = true;
            showClasses = true;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            LittleEndianRandomAccessFile file = new LittleEndianRandomAccessFile(raf);

            // Read the generic header
            // https://github.com/openjdk/jdk/blob/f4607ed0a7ea2504c1d72dd3dab0b21e583fa0e7/src/hotspot/share/include/cds.h#L84
            GenericHeader genericHeader = new GenericHeader(file);

            if (genericHeader.magic != AOT_MAGIC) {
                String actualMagic = String.format("%08x", genericHeader.magic);
                System.out.println("Invalid AOTCache file: magic number mismatch (actual: " + actualMagic + ")");
                return 1;
            }

            // read 5 regions
            CDSFileMapRegion[] regions = new CDSFileMapRegion[5];
            for (int i = 0; i < 5; i++) {
                regions[i] = new CDSFileMapRegion(file);
            }

            // Read the file map header
            FileMapHeader fileMapHeader = new FileMapHeader(file);

            // Snapshot raw bytes for each region so later analyses don't have
            // to seek around in the underlying file.
            RegionData[] regionData = RegionData.loadAll(file, regions);

            if (showHeader) {
                FileMapHeader.print(genericHeader, regions, fileMapHeader, System.out);
            }

            if (showClasses) {
                CDSFileMapRegion rwRegion = regions[0]; // Region 0 is RW region
                if (rwRegion.used > 0) {
                    findAndPrintClasses(file, rwRegion, fileMapHeader.requestedBaseAddress);
                }
            }

            return 0;
        } catch (EOFException e) {
            System.out.println("Invalid AOTCache file: file too short");
            return 1;
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
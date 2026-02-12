package io.github.chains_project.aotp;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.github.chains_project.aotp.header.CDSFileMapRegion;
import io.github.chains_project.aotp.header.FileMapHeader;
import io.github.chains_project.aotp.header.GenericHeader;
import io.github.chains_project.aotp.header.RegionData;
import io.github.chains_project.aotp.oops.klass.InstanceClass;
import io.github.chains_project.aotp.utils.ByteReader;
import io.github.chains_project.aotp.utils.LittleEndianRandomAccessFile;
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
                                            RegionData rwRegionData,
                                            long requestedBaseAddress) throws IOException {
        byte[] bytes = rwRegionData.bytes();
        if (bytes.length == 0) {
            return;
        }

        List<Long> patterns = getPatternsforClasses(requestedBaseAddress);
        List<InstanceClass> entries = new ArrayList<>();
        final int len = bytes.length;

        for (int offset = 0; offset + 8 <= len; offset += 8) {
            long value = ByteReader.readLongLE(bytes, offset);
            if (!patterns.contains(value)) {
                continue;
            }
            int entryStart = offset; // first 4 bytes = layoutHelper, next 4 = kind
            entries.add(InstanceClass.parse(bytes, entryStart));
        }

        for (InstanceClass entry : entries) {
            String className = readSymbolName(file, entry.namePointer(), requestedBaseAddress);
            if (className != null) {
                System.out.println(className);
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

            if (genericHeader.magic() != AOT_MAGIC) {
                String actualMagic = String.format("%08x", genericHeader.magic());
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
                RegionData rwRegionData = regionData[0]; // Region 0 is RW region
                if (rwRegionData.bytes().length > 0) {
                    findAndPrintClasses(file, rwRegionData, fileMapHeader.requestedBaseAddress());
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
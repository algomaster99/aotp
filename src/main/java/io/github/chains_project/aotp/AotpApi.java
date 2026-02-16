package io.github.chains_project.aotp;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.chains_project.aotp.header.CDSFileMapRegion;
import io.github.chains_project.aotp.header.FileMapHeader;
import io.github.chains_project.aotp.header.GenericHeader;
import io.github.chains_project.aotp.header.RegionData;
import io.github.chains_project.aotp.oops.klass.ClassEntry;
import io.github.chains_project.aotp.oops.klass.InstanceClass;
import io.github.chains_project.aotp.utils.ByteReader;
import io.github.chains_project.aotp.utils.LittleEndianRandomAccessFile;

/**
 * Programmatic API for reading AOT cache files. Use this from code or unit tests
 * instead of invoking the CLI.
 */
public final class AotpApi {

    // Magic number for AOTCache files
    // https://github.com/openjdk/jdk/blob/6f6966b28b2c5a18b001be49f5db429c667d7a8f/src/hotspot/share/include/cds.h#L39
    private static final int AOT_MAGIC = 0xf00baba2;

    private AotpApi() {}

    private static List<Long> getPatternsForClasses(long baseAddress) {
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

    private static List<ClassEntry> loadClasses(LittleEndianRandomAccessFile file,
            RegionData rwRegionData,
            long requestedBaseAddress) throws IOException {
        byte[] bytes = rwRegionData.bytes();
        if (bytes.length == 0) {
            return List.of();
        }

        List<Long> patterns = getPatternsForClasses(requestedBaseAddress);
        List<ClassEntry> entries = new ArrayList<>();
        final int len = bytes.length;

        for (int offset = 0; offset + 8 <= len; offset += 8) {
            long value = ByteReader.readLongLE(bytes, offset);
            if (!patterns.contains(value)) {
                continue;
            }
            int entryStart = offset;
            InstanceClass parsed = InstanceClass.parse(bytes, entryStart);
            String className = readSymbolName(file, parsed.namePointer(), requestedBaseAddress);
            if (className != null) {
                parsed.setName(className);
                entries.add(parsed);
            }
        }

        return entries;
    }

    /**
     * Reads a symbol name from the ro region using an absolute address.
     * Symbol format: hash_and_refcount (4 bytes), length (2 bytes), body[length] (UTF-8)
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
            file.skipBytes(4);
            int length = file.readShort() & 0xFFFF;
            if (length < 0 || length > 65535) {
                return null;
            }
            byte[] nameBytes = new byte[length];
            file.readFully(nameBytes);
            return new String(nameBytes, StandardCharsets.UTF_8);
        } finally {
            file.seek(currentPos);
        }
    }

    private static void validateMagic(GenericHeader genericHeader) throws IOException {
        if (genericHeader.magic() != AOT_MAGIC) {
            String actualMagic = String.format("%08x", genericHeader.magic());
            throw new IOException("Invalid AOTCache file: magic number mismatch (actual: " + actualMagic + ")");
        }
    }

    /**
     * Prints the file map header to the given appendable.
     *
     * @param filePath path to the AOT cache file
     * @param out      where to write the header
     * @throws IOException if the file cannot be read or is invalid
     */
    public static void printHeader(String filePath, Appendable out) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            LittleEndianRandomAccessFile file = new LittleEndianRandomAccessFile(raf);
            GenericHeader genericHeader = new GenericHeader(file);
            CDSFileMapRegion[] regions = new CDSFileMapRegion[5];
            for (int i = 0; i < 5; i++) {
                regions[i] = new CDSFileMapRegion(file);
            }
            FileMapHeader fileMapHeader = new FileMapHeader(file);
            validateMagic(genericHeader);
            FileMapHeader.print(genericHeader, regions, fileMapHeader, out);
        } catch (EOFException e) {
            throw new IOException("Invalid AOTCache file: file too short", e);
        }
    }

    /**
     * Returns the list of class names found in the RW region.
     *
     * @param filePath path to the AOT cache file
     * @return list of class names (never null)
     * @throws IOException if the file cannot be read or is invalid
     */
    public static List<String> listClassNames(String filePath) throws IOException {
        return listClasses(filePath).stream().map(ClassEntry::getName).toList();
    }

    /**
     * Returns the list of classes found in the RW region.
     *
     * @param filePath path to the AOT cache file
     * @return list of ClassEntry (never null)
     * @throws IOException if the file cannot be read or is invalid
     */
    public static List<ClassEntry> listClasses(String filePath) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            LittleEndianRandomAccessFile file = new LittleEndianRandomAccessFile(raf);
            GenericHeader genericHeader = new GenericHeader(file);
            CDSFileMapRegion[] regions = new CDSFileMapRegion[5];
            for (int i = 0; i < 5; i++) {
                regions[i] = new CDSFileMapRegion(file);
            }
            FileMapHeader fileMapHeader = new FileMapHeader(file);
            RegionData[] regionData = RegionData.loadAll(file, regions);
            validateMagic(genericHeader);
            RegionData rwRegionData = regionData[0];
            if (rwRegionData.bytes().length == 0) {
                return List.of();
            }
            return loadClasses(file, rwRegionData, fileMapHeader.requestedBaseAddress());
        } catch (EOFException e) {
            throw new IOException("Invalid AOTCache file: file too short", e);
        }
    }

    /**
     * Returns sizes for a batch of classes. Only classes that are present in the
     * AOT cache are included in the result.
     *
     * @param filePath   path to the AOT cache file
     * @param classNames list of fully qualified class names
     * @return map from class name to its size (only for found classes)
     * @throws IOException if the file cannot be read or is invalid
     */
    public static Map<ClassEntry, Integer> getClassSizes(String filePath, List<String> classNames) throws IOException {
        List<ClassEntry> classes = listClasses(filePath);
        Map<ClassEntry, Integer> sizeByClassEntry = new HashMap<>();
        for (ClassEntry entry : classes) {
            sizeByClassEntry.put(entry, entry.getSize());
        }
        Map<ClassEntry, Integer> result = new HashMap<>();
        for (ClassEntry entry : classes) {
            if (classNames.contains(entry.getName())) {
                result.put(entry, sizeByClassEntry.get(entry));
            }
        }
        return result;
    }

    /**
     * Pretty-prints the fields of the specified class to the given stream.
     *
     * @param filePath  path to the AOT cache file
     * @param className fully qualified class name
     * @param out       where to write the output
     * @return true if the class was found and printed, false if not found
     * @throws IOException if the file cannot be read or is invalid
     */
    public static boolean printClass(String filePath, String className, PrintStream out) throws IOException {
        List<ClassEntry> classes = listClasses(filePath);
        for (ClassEntry entry : classes) {
            if (entry.getName().equals(className)) {
                entry.print(out);
                return true;
            }
        }
        return false;
    }
}

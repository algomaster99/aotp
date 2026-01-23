package io.github.chains_project.aotp;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.io.LittleEndianDataInputStream;

public class Main {

    // Magic number for AOTCache files
    // https://github.com/openjdk/jdk/blob/6f6966b28b2c5a18b001be49f5db429c667d7a8f/src/hotspot/share/include/cds.h#L39
    private static final int AOT_MAGIC = 0xf00baba2;
    
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
            }

            // Extract and print class names from RO region
            CDSFileMapRegion roRegion = regions[1]; // RO region
            if (Long.compareUnsigned(roRegion.used, 0) > 0) {
                System.out.println("\nExtracting class names from RO region...");
                List<String> classNames = extractClassNames(filePath, roRegion);
                System.out.println("\nFound " + classNames.size() + " classes:");
                for (String className : classNames) {
                    System.out.println("  " + className);
                }
            } else {
                System.out.println("\nRO region is empty, no classes to extract.");
            }
            
        } catch (EOFException e) {
            System.out.println("Invalid AOTCache file: file too short");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static List<String> extractClassNames(String filePath, CDSFileMapRegion roRegion) throws IOException {
        List<String> classes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // Seek to RO region data
            raf.seek(roRegion.fileOffset);
            
            // Read RO region data (only the used portion)
            long used = roRegion.used;
            if (used > Integer.MAX_VALUE) {
                throw new IOException("RO region too large: " + used);
            }
            
            byte[] roData = new byte[(int)used];
            raf.readFully(roData);
            
            // Create ByteBuffer for easier parsing
            ByteBuffer buffer = ByteBuffer.wrap(roData).order(ByteOrder.LITTLE_ENDIAN);
            
            // Scan for Symbol structures
            int pos = 0;
            int limit = buffer.limit();
            
            while (pos < limit - 6) { // Need at least 6 bytes (4 for hash + 2 for length)
                try {
                    buffer.position(pos);
                    
                    // Skip hash_and_refcount (4 bytes)
                    buffer.getInt();
                    
                    // Read length (u2, little-endian)
                    int length = Short.toUnsignedInt(buffer.getShort());
                    
                    // Validate length
                    if (length > 0 && length < 500 && pos + 6 + length <= limit) {
                        // Read UTF-8 bytes
                        byte[] nameBytes = new byte[length];
                        buffer.get(nameBytes);
                        
                        String name = new String(nameBytes, StandardCharsets.UTF_8);
                        
                        // Check if it's a class name
                        if (isClassName(name)) {
                            String className = name.replace('/', '.');
                            if (!seen.contains(className)) {
                                seen.add(className);
                                classes.add(className);
                            }
                        }
                        
                        // Move to next potential Symbol (align to 8-byte boundary)
                        pos += 6 + length;
                        pos = (pos + 7) & ~7; // Align to 8 bytes
                    } else {
                        pos++;
                    }
                } catch (Exception e) {
                    pos++;
                }
            }
        }
        
        return classes;
    }
    
    private static boolean isClassName(String name) {
        if (name == null || name.isEmpty() || name.length() > 200) {
            return false;
        }
        
        // Class names contain '/' (package separator) or '$' (inner class)
        if (!name.contains("/") && !name.contains("$")) {
            return false;
        }
        
        // Class names don't contain spaces or control characters
        for (char c : name.toCharArray()) {
            if (Character.isISOControl(c) || c == '\0') {
                return false;
            }
        }
        
        // Class names typically contain alphanumeric, '/', '$', '_', '.'
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '/' && c != '$' && c != '_' && c != '.') {
                return false;
            }
        }
        
        // Must have at least one '/' or be a valid Java identifier
        if (name.contains("/")) {
            String[] parts = name.split("/");
            if (parts.length < 2) {
                return false;
            }
            // Last part should be a valid class name (starts with uppercase or $)
            String lastPart = parts[parts.length - 1];
            if (lastPart.isEmpty()) {
                return false;
            }
            char firstChar = lastPart.charAt(0);
            if (!Character.isUpperCase(firstChar) && firstChar != '$') {
                return false;
            }
        }
        
        return true;
    }
}
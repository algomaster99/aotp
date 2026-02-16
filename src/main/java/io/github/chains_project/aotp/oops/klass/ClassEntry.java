package io.github.chains_project.aotp.oops.klass;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

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
    public final long name; // symbol pointer (absolute address)
    public final long secondarySuperCache;
    public final long secondarySupers;
    public final long[] primarySupers;
    public final long javaMirror; // this is oopHandle but it basically stores a pointer to oop
    public final long superKlass;
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
                         long name,
                         long secondarySuperCache,
                         long secondarySupers,
                         long[] primarySupers,
                         long javaMirror,
                         long superKlass,
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
        this.name = name;
        this.secondarySuperCache = secondarySuperCache;
        this.secondarySupers = secondarySupers;
        // _primary_super_limit = 8
        this.primarySupers = new long[8];
        System.arraycopy(primarySupers, 0, this.primarySupers, 0, 8);
        this.javaMirror = javaMirror;
        this.superKlass = superKlass;
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
        return name;
    }

    public int getSize() {
        return 200;
    }

    /**
     * Pretty-print all fields of this class (including subclasses) to the given
     * {@link PrintStream}.
     * <p>
     * Format:
     * <pre>
     * fieldName            value
     * nestedObject:
     *   nestedField        value
     * </pre>
     * <p>
     * All {@code long} values are rendered using {@link Long#toHexString(long)}.
     */
    public void print(PrintStream out) {
        if (out == null) {
            throw new IllegalArgumentException("PrintStream must not be null");
        }
        printObjectFields(this, this.getClass(), out, "");
    }

    private static void printObjectFields(Object obj, Class<?> type, PrintStream out, String indent) {
        if (obj == null || type == null || type == Object.class) {
            return;
        }

        // Print superclass fields first (base class before derived class)
        printObjectFields(obj, type.getSuperclass(), out, indent);

        // Then print current class' fields
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                continue;
            }
            printSingleField(obj, field, out, indent);
        }
    }

    private static void printSingleField(Object owner, Field field, PrintStream out, String indent) {
        field.setAccessible(true);

        final String name = field.getName();
        try {
            Class<?> fieldType = field.getType();

            if (fieldType == long.class) {
                long value = field.getLong(owner);
                printLine(out, indent, name, Long.toHexString(value));
                return;
            }

            if (fieldType == Long.class) {
                Long value = (Long) field.get(owner);
                printLine(out, indent, name, value == null ? "null" : Long.toHexString(value));
                return;
            }

            Object value = field.get(owner);

            if (value == null) {
                printLine(out, indent, name, "null");
                return;
            }

            if (fieldType == long[].class) {
                long[] arr = (long[]) value;
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(Long.toHexString(arr[i]));
                }
                sb.append(']');
                printLine(out, indent, name, sb.toString());
                return;
            }

            if (fieldType.isArray()) {
                int length = java.lang.reflect.Array.getLength(value);
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (int i = 0; i < length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    Object element = java.lang.reflect.Array.get(value, i);
                    if (element instanceof Long l) {
                        sb.append(Long.toHexString(l));
                    } else {
                        sb.append(String.valueOf(element));
                    }
                }
                sb.append(']');
                printLine(out, indent, name, sb.toString());
                return;
            }

            if (value instanceof List<?> list) {
                // Print list summary line
                printLine(out, indent, name + ".size", Integer.toString(list.size()));
                for (int i = 0; i < list.size(); i++) {
                    Object element = list.get(i);
                    String elementName = name + "[" + i + "]";
                    printNestedValue(out, indent + "  ", elementName, element);
                }
                return;
            }

            // Simple scalar types
            if (isSimpleScalar(value)) {
                if (value instanceof Long l) {
                    printLine(out, indent, name, Long.toHexString(l));
                } else {
                    printLine(out, indent, name, String.valueOf(value));
                }
                return;
            }

            // Nested object / record – print its fields with additional indentation.
            printNestedValue(out, indent, name, value);
        } catch (IllegalAccessException e) {
            printLine(out, indent, name, "<inaccessible>");
        }
    }

    private static void printNestedValue(PrintStream out, String indent, String name, Object value) {
        if (value == null) {
            printLine(out, indent, name, "null");
            return;
        }
        if (isSimpleScalar(value)) {
            if (value instanceof Long l) {
                printLine(out, indent, name, Long.toHexString(l));
            } else {
                printLine(out, indent, name, String.valueOf(value));
            }
            return;
        }
        printLine(out, indent, name, "");
        printObjectFields(value, value.getClass(), out, indent + "  ");
    }

    private static boolean isSimpleScalar(Object value) {
        if (value == null) {
            return false;
        }
        return value instanceof String ||
               value instanceof Number ||
               value instanceof Boolean ||
               value instanceof Character ||
               value.getClass().isEnum();
    }

    private static void printLine(PrintStream out, String indent, String fieldName, String value) {
        out.printf("%s%-24s %s%n", indent, fieldName, value);
    }

    public boolean isInterface() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setName(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }
}


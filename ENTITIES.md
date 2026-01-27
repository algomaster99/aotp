# AOT Cache Entities

This document describes the entities stored in AOT (Ahead-of-Time) Cache files. These entities represent internal HotSpot JVM objects that are serialized into the AOT Cache for faster startup and reduced memory footprint through Class Data Sharing (CDS).

## Core Object Types

### Symbol
**Type:** Metadata  
**Purpose:** Represents a canonicalized string stored in the global SymbolTable. All symbols are reference counted and shared across the JVM. Commonly used for class names, method names, signatures, and field names. Symbols are UTF-8 encoded strings with a hash for fast lookup.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/symbol.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/symbol.hpp#L114-L116)
- Key characteristics: Reference counted, canonicalized, stored in SymbolTable
- Structure: `hash_and_refcount` (4 bytes), `length` (2 bytes), `body[length]` (UTF-8 bytes)

### Object
**Type:** Heap Object  
**Purpose:** Base representation of Java objects in the heap. Contains the object header with mark word (for locking, GC, identity hash) and class pointer (klass word).

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/oop.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/oop.hpp)
- Key characteristics: All Java objects derive from this, contains mark word and klass pointer

### Class
**Type:** Heap Object  
**Purpose:** Represents `java.lang.Class` instances in the Java heap. Each Java class has a corresponding mirror object of type `java.lang.Class` used by reflection and the Java language.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/classfile/javaClasses.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/classfile/javaClasses.hpp)
- Related: [`src/hotspot/share/oops/instanceKlass.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/instanceKlass.hpp)
- Key characteristics: Mirror object for Klass metadata, used by reflection API

**Klass Kinds:** The JVM distinguishes different types of classes through the `_layout_helper` field in the Klass structure. This field encodes the type and size information:

| Klass Kind | Layout Helper Pattern | Description | Example in Hexdump |
|------------|----------------------|-------------|-------------------|
| **InstanceKlass** | `0x0000000800001080` | Regular Java classes | Standard classes like `java.lang.String` |
| **ObjArrayKlass** | `0x00000008000018f0` | Object array classes | `[Ljava/lang/Object;` |
| **TypeArrayKlass (primitives)** | `0x0000000800001a60` | Primitive type arrays | `[I`, `[B`, `[C` |
| **InstanceMirrorKlass** | `0x0000000800001350` | `java.lang.Class` mirrors | The `java.lang.Class` class itself |
| **InstanceStackChunkKlass** | `0x0000000800001620` | Stack chunk objects | `jdk.internal.vm.StackChunk` |
| **InstanceRefKlass** | `0x00000008000014b8` | Reference types | Subclasses of `java.lang.ref.Reference` |
| **InstanceClassLoaderKlass** | `0x00000008000011e8` | Class loaders | Subclasses of `java.lang.ClassLoader` |

Source: [`src/hotspot/share/oops/klass.hpp#L68-L77`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/klass.hpp#L68-L77)

**Identifying Classes from Hexdump:** When examining AOT cache hexdumps, the `_layout_helper` field (first 8 bytes after the klass pointer) identifies the class kind:

Example hexdump:
```
0x0000000800233370: @@ Class             512 [Ljdk.internal.vm.FillerElement;
0x0000000800233370:   0000000800001a60 00080005c0100a02 0000000000000038 000000080089c000
                      ^^^^^^^^^^^^^^^^
                      _layout_helper = 0x0000000800001a60 (TypeArrayKlass for primitives)
```

The `_layout_helper` value `0x0000000800001a60` indicates this is a primitive type array class (`TypeArrayKlass`). To parse the structure:
1. First 8 bytes (`0000000800001a60`): `_layout_helper` field identifying the Klass kind
2. Next 8 bytes (`00080005c0100a02`): `_kind` and other metadata fields
3. Following bytes contain additional metadata like `_name` pointer to Symbol, superclass pointers, etc.

**Structure:** Class mirrors contain references back to their corresponding Klass metadata. When accessing a class name from a Class mirror, the following byte-level traversal is performed:
1. **Class mirror** → `_klass` pointer → **InstanceKlass**
2. **InstanceKlass** → `_name` field (offset varies) → **Symbol pointer** (8 bytes on 64-bit)
3. **Symbol** bytes (as defined above in Symbol section): `hash_and_refcount` (4 bytes) + `length` (2 bytes) + `body[length]` UTF-8 bytes
- Source: [`src/hotspot/share/oops/symbol.hpp#L114-L116`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/symbol.hpp#L114-L116)

## Method-Related Entities

### Method
**Type:** Metadata  
**Purpose:** Represents a Java method with its execution state, bytecode pointers, and runtime data. Contains references to ConstMethod (read-only method data), MethodData (profiling info), MethodCounters (invocation counts), and compiled code.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/method.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/method.hpp)
- Key characteristics: Mutable method data, interpreter/compiler entry points, vtable index

**Structure:**
```
Method {
  ConstMethod*      _constMethod;        // Pointer to read-only method data
  MethodData*       _method_data;        // Profiling data (may be null)
  MethodCounters*   _method_counters;    // Invocation/backedge counters
  AdapterHandlerEntry* _adapter;         // i2c/c2i adapters
  int               _vtable_index;       // vtable index or special value
  AccessFlags       _access_flags;       // public/private/static/synchronized/native etc.
  MethodFlags       _flags;              // Additional flags
  u2                _intrinsic_id;       // VM intrinsic ID (0 = none)
  address           _i2i_entry;          // Interpreter-to-interpreter entry
  address           _from_compiled_entry;// Entry from compiled code
  nmethod*          _code;               // Compiled code (may be null)
  address           _from_interpreted_entry; // Entry from interpreter
}
```
- Source: [`src/hotspot/share/oops/method.hpp#L68-L85`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/method.hpp#L68-L85)

### ConstMethod
**Type:** Metadata  
**Purpose:** Represents the immutable portions of a Java method that don't change after the classfile is parsed. Includes bytecodes, line number table, local variable table, exception handlers, and checked exceptions. Shared across processes in read-only CDS regions.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/constMethod.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/constMethod.hpp)
- Key characteristics: Read-only, shareable via CDS, contains bytecode and metadata tables

**Structure:**
```
ConstMethod {
  // Header fields
  uint64_t          _fingerprint;        // Method signature fingerprint
  ConstantPool*     _constants;          // Pointer to constant pool
  Array<u1>*        _stackmap_data;      // Stack map table for verification
  int               _constMethod_size;   // Total size in words
  ConstMethodFlags  _flags;              // Various flags
  u1                _result_type;        // BasicType of return value
  u2                _code_size;          // Bytecode length in bytes
  u2                _name_index;         // CP index of method name
  u2                _signature_index;    // CP index of signature
  u2                _method_idnum;       // Unique method ID
  u2                _max_stack;          // Max expression stack depth
  u2                _max_locals;         // Number of local variables
  u2                _size_of_parameters; // Parameter block size in words
  
  // Followed by variable-length sections:
  // 1. Bytecode array [_code_size bytes]
  // 2. Compressed line number table
  // 3. Local variable table (indexed from end)
  // 4. Exception table (indexed from end)
  // 5. Checked exceptions (indexed from end)
  // 6. Method parameters (indexed from end)
  // 7. Generic signature index
  // 8. Annotation arrays
}
```
- Source: [`src/hotspot/share/oops/constMethod.hpp#L25-L80`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/constMethod.hpp#L25-L80)

### MethodData
**Type:** Metadata  
**Purpose:** Stores profiling information collected during method execution for optimizing compilation. Contains data about branch frequencies, type profiles, call sites, and other runtime statistics used by the JIT compiler.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/methodData.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/methodData.hpp)
- Key characteristics: Profile data for JIT optimization, collects runtime statistics

**Structure:** MethodData consists of a header followed by an array of ProfileData entries. Each entry is structured as a DataLayout:
```
DataLayout {
  union _header {
    u8 _bits;                       // 8 bytes total
    struct {
      u1 _tag;                      // Byte 0: Data type tag (bit_data, counter_data, jump_data, etc.)
      u1 _flags;                    // Byte 1: 8 flag bits
      u2 _bci;                      // Bytes 2-3: Bytecode index
      u4 _traps;                    // Bytes 4-7: Trap state/history
    } _struct;
  } _header;
  intptr_t _cells[];                // Variable-length array of profile data cells
}
```

**Profile Data Types:**
- **BitData**: Single bit flags (null seen, exception seen)
- **CounterData**: Invocation/execution counters
- **ReceiverTypeData**: Type profiles for polymorphic call sites (records seen types)
- **BranchData**: Branch taken/not-taken frequencies
- **JumpData**: Jump target displacement and counts
- **VirtualCallData**: Virtual call site type profiles
- **RetData**: Return bci tracking

Each profile data type inherits from ProfileData and uses the _cells array to store type-specific counters and statistics. Cell size is sizeof(intptr_t), which is 8 bytes on 64-bit platforms.
- Source: [`src/hotspot/share/oops/methodData.hpp#L89-L110`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/methodData.hpp#L89-L110)

### MethodCounters
**Type:** Metadata  
**Purpose:** Tracks invocation and backedge counters for methods to determine when to compile a method. Used for tiered compilation decisions.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/methodCounters.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/methodCounters.hpp)
- Key characteristics: Invocation counts, backedge counts, compilation thresholds

**Structure:**
```
MethodCounters {
  int  _invoke_mask;                // Mask for invocation counter
  int  _backedge_mask;              // Mask for backedge counter
  int  _interpreter_invocation_count; // Number of times interpreted
  u2   _invoke_counter;             // Invocation counter (triggers compilation)
  u2   _backedge_counter;           // Backedge counter (loop iterations)
  u1   _highest_comp_level;         // Highest compilation level achieved
  u1   _highest_osr_comp_level;     // Highest OSR compilation level
  u2   _prev_time;                  // Previous timestamp
  float _rate;                      // Compilation rate
  jlong _prev_event_count;          // Previous event count
}
```
- Source: [`src/hotspot/share/oops/methodCounters.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/methodCounters.hpp)

### MethodTrainingData
**Type:** Metadata  
**Purpose:** Stores training data collected during application training runs for AOT compilation. Helps the AOT compiler make better optimization decisions by recording actual runtime behavior.

**OpenJDK Reference:**
- Usage: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/cds)
- Key characteristics: Training run profiling data, used for AOT optimization decisions

## Constant Pool Entities

### ConstantPool
**Type:** Metadata  
**Purpose:** Represents the runtime constant pool of a class. Contains symbolic references to classes, methods, fields, and literal constants. The constant pool is indexed and resolved lazily as references are used.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/constantPool.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/constantPool.hpp)
- Key characteristics: Symbolic references, lazy resolution, indexed entries

**Structure:**
```
ConstantPool {
  Array<u1>*           _tags;                    // Tag array (entry types)
  ConstantPoolCache*   _cache;                   // Resolved entries cache
  InstanceKlass*       _pool_holder;             // Class owning this pool
  Array<Klass*>*       _resolved_klasses;        // Resolved class references
  u2                   _major_version;           // Class file major version
  u2                   _minor_version;           // Class file minor version
  u2                   _generic_signature_index; // Generic signature CP index
  u2                   _source_file_name_index;  // Source file name CP index
  u2                   _flags;                   // Status flags
  int                  _length;                  // Number of entries
  
  // Followed by variable-length array:
  intptr_t base[];  // CP entry data (symbols, literals, references)
}
```

**Entry types (tag values):**
- JVM_CONSTANT_Utf8 (1): UTF-8 strings (Symbol*)
- JVM_CONSTANT_Integer (3): int literals
- JVM_CONSTANT_Float (4): float literals
- JVM_CONSTANT_Long (5): long literals (takes 2 slots)
- JVM_CONSTANT_Double (6): double literals (takes 2 slots)
- JVM_CONSTANT_Class (7): Resolved class reference (Klass*)
- JVM_CONSTANT_String (8): String constant
- JVM_CONSTANT_Fieldref (9): Field reference
- JVM_CONSTANT_Methodref (10): Method reference
- JVM_CONSTANT_InterfaceMethodref (11): Interface method reference
- JVM_CONSTANT_NameAndType (12): Name and type descriptor
- JVM_CONSTANT_MethodHandle (15): Method handle
- JVM_CONSTANT_MethodType (16): Method type
- JVM_CONSTANT_InvokeDynamic (18): Invokedynamic bootstrap
- Source: [`src/hotspot/share/oops/constantPool.hpp#L118-L145`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/constantPool.hpp#L118-L145)

### ConstantPoolCache
**Type:** Metadata  
**Purpose:** Caches resolved constant pool entries for faster access. Stores resolved method references, field references, and their metadata to avoid repeated resolution.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/cpCache.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/cpCache.hpp)
- Key characteristics: Resolved CP entries cache, improves resolution performance

## Adapter Entities

### AdapterHandlerEntry
**Type:** Runtime Data  
**Purpose:** Manages method call adapters that handle transitions between interpreted and compiled code. Contains entry points for i2c (interpreter-to-compiled), c2i (compiled-to-interpreter), and c2c (compiled-to-compiled) transitions.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/runtime/sharedRuntime.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/runtime/sharedRuntime.hpp)
- Related: [`src/hotspot/share/runtime/sharedRuntime.cpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/runtime/sharedRuntime.cpp)
- Key characteristics: Manages code transition adapters, signature-specific

### AdapterFingerPrint
**Type:** Runtime Data  
**Purpose:** Uniquely identifies method signatures for adapter generation. Used to look up or create appropriate adapters for method calls with specific parameter types.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/runtime/sharedRuntime.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/runtime/sharedRuntime.hpp)
- Key characteristics: Signature identification, adapter lookup key

## Annotation Entities

### Annotations
**Type:** Metadata  
**Purpose:** Stores Java annotations attached to classes, methods, fields, and parameters. Includes runtime-visible and runtime-invisible annotations as defined in the classfile.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/annotations.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/annotations.hpp)
- Key characteristics: Class/method/field/parameter annotations, reflection API support

**Structure:**
```
Annotations {
  Array<u1>* _class_annotations;         // Class-level annotations
  Array<u1>* _fields_annotations;        // Field annotations (array of arrays)
  Array<u1>* _class_type_annotations;    // Type annotations on class
  Array<u1>* _fields_type_annotations;   // Type annotations on fields
}
```
Each annotation is stored as a compact byte array following the classfile annotation format:
- u2 type_index (constant pool index to annotation type)
- u2 num_element_value_pairs
- element_value_pair[num_element_value_pairs]
- Source: [`src/hotspot/share/oops/annotations.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/annotations.hpp)

### RecordComponent
**Type:** Metadata  
**Purpose:** Represents components of Java record classes (introduced in Java 16). Stores metadata about record component names, descriptors, signatures, and annotations.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/recordComponent.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/recordComponent.hpp)
- Key characteristics: Record class metadata, component descriptors

## Training Data Entities

### CompileTrainingData
**Type:** Training Metadata  
**Purpose:** Aggregates training data collected during application profiling for compilation decisions in AOT mode. Helps determine which methods to compile and at what optimization levels.

**OpenJDK Reference:**
- Usage: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/cds)
- Key characteristics: Aggregated compilation statistics, AOT decision data

### KlassTrainingData
**Type:** Training Metadata  
**Purpose:** Stores training data specific to class (Klass) behavior during profiling runs. Captures initialization patterns, inheritance relationships usage, and other class-level runtime characteristics.

**OpenJDK Reference:**
- Usage: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/cds)
- Key characteristics: Class-level profiling data, initialization patterns

## Array Types

### TypeArrayU1
**Type:** Heap Array  
**Purpose:** Array of unsigned 1-byte (8-bit) integers. Used for bytecode arrays, boolean arrays, and byte arrays. Most commonly used for storing method bytecode.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 8-bit unsigned integers, bytecode storage

### TypeArrayU2
**Type:** Heap Array  
**Purpose:** Array of unsigned 2-byte (16-bit) integers. Used for char arrays and UTF-16 string storage.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 16-bit unsigned integers, char arrays

### TypeArrayU4
**Type:** Heap Array  
**Purpose:** Array of unsigned 4-byte (32-bit) integers. Used for int arrays and other 32-bit data structures.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 32-bit unsigned integers, int arrays

### TypeArrayU8
**Type:** Heap Array  
**Purpose:** Array of unsigned 8-byte (64-bit) integers. Used for long arrays, double arrays, and other 64-bit data structures.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 64-bit unsigned integers, long/double arrays

### TypeArrayOther
**Type:** Heap Array  
**Purpose:** Represents type arrays that don't fall into the U1, U2, U4, or U8 categories. Handles other primitive array types and special cases.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: Other primitive array types, special cases

## Miscellaneous

### Misc
**Type:** Metadata  
**Purpose:** Catch-all category for miscellaneous metadata and runtime structures that don't fit into other specific categories. May include JVM-internal data structures, temporary objects, and auxiliary metadata.

**OpenJDK Reference:**
- Various: [`src/hotspot/share/oops/`](https://github.com/openjdk/jdk/tree/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops)
- Key characteristics: Various auxiliary metadata, JVM-internal structures

## Related Resources

- **CDS Architecture**: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/cds)
- **Object System (oops)**: [`src/hotspot/share/oops/`](https://github.com/openjdk/jdk/tree/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/oops)
- **CDS File Format**: [`src/hotspot/share/include/cds.h`](https://github.com/openjdk/jdk/blob/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/include/cds.h)
- **AOT Compilation**: [`src/hotspot/share/aot/`](https://github.com/openjdk/jdk/tree/eb6e74b1fa794bf16f572d5dbce157d1cae4c505/src/hotspot/share/aot)

## Notes

- All metadata entities can be stored in read-only memory regions when appropriate for CDS
- Heap objects are serialized and relocated during AOT cache loading
- Training data entities are specific to AOT compilation and profiling
- Array types correspond to Java primitive arrays and are heavily used in the JVM internals

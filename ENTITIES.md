# AOT Cache Entities

This document describes the entities stored in AOT (Ahead-of-Time) Cache files. These entities represent internal HotSpot JVM objects that are serialized into the AOT Cache for faster startup and reduced memory footprint through Class Data Sharing (CDS).

## Core Object Types

### Symbol
**Type:** Metadata  
**Purpose:** Represents a canonicalized string stored in the global SymbolTable. All symbols are reference counted and shared across the JVM. Commonly used for class names, method names, signatures, and field names. Symbols are UTF-8 encoded strings with a hash for fast lookup.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/symbol.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/symbol.hpp)
- Key characteristics: Reference counted, canonicalized, stored in SymbolTable
- Structure: `hash_and_refcount` (4 bytes), `length` (2 bytes), `body[length]` (UTF-8 bytes)

### Object
**Type:** Heap Object  
**Purpose:** Base representation of Java objects in the heap. Contains the object header with mark word (for locking, GC, identity hash) and class pointer (klass word).

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/oop.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/oop.hpp)
- Key characteristics: All Java objects derive from this, contains mark word and klass pointer

### Class
**Type:** Heap Object  
**Purpose:** Represents `java.lang.Class` instances in the Java heap. Each Java class has a corresponding mirror object of type `java.lang.Class` used by reflection and the Java language.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/classfile/javaClasses.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/classfile/javaClasses.hpp)
- Related: [`src/hotspot/share/oops/instanceKlass.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/instanceKlass.hpp)
- Key characteristics: Mirror object for Klass metadata, used by reflection API

## Method-Related Entities

### Method
**Type:** Metadata  
**Purpose:** Represents a Java method with its execution state, bytecode pointers, and runtime data. Contains references to ConstMethod (read-only method data), MethodData (profiling info), MethodCounters (invocation counts), and compiled code.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/method.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/method.hpp)
- Key characteristics: Mutable method data, interpreter/compiler entry points, vtable index

### ConstMethod
**Type:** Metadata  
**Purpose:** Represents the immutable portions of a Java method that don't change after the classfile is parsed. Includes bytecodes, line number table, local variable table, exception handlers, and checked exceptions. Shared across processes in read-only CDS regions.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/constMethod.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/constMethod.hpp)
- Key characteristics: Read-only, shareable via CDS, contains bytecode and metadata tables

### MethodData
**Type:** Metadata  
**Purpose:** Stores profiling information collected during method execution for optimizing compilation. Contains data about branch frequencies, type profiles, call sites, and other runtime statistics used by the JIT compiler.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/methodData.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/methodData.hpp)
- Key characteristics: Profile data for JIT optimization, collects runtime statistics

### MethodCounters
**Type:** Metadata  
**Purpose:** Tracks invocation and backedge counters for methods to determine when to compile a method. Used for tiered compilation decisions.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/methodCounters.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/methodCounters.hpp)
- Key characteristics: Invocation counts, backedge counts, compilation thresholds

### MethodTrainingData
**Type:** Metadata  
**Purpose:** Stores training data collected during application training runs for AOT compilation. Helps the AOT compiler make better optimization decisions by recording actual runtime behavior.

**OpenJDK Reference:**
- Usage: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/master/src/hotspot/share/cds)
- Key characteristics: Training run profiling data, used for AOT optimization decisions

## Constant Pool Entities

### ConstantPool
**Type:** Metadata  
**Purpose:** Represents the runtime constant pool of a class. Contains symbolic references to classes, methods, fields, and literal constants. The constant pool is indexed and resolved lazily as references are used.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/constantPool.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/constantPool.hpp)
- Key characteristics: Symbolic references, lazy resolution, indexed entries

### ConstantPoolCache
**Type:** Metadata  
**Purpose:** Caches resolved constant pool entries for faster access. Stores resolved method references, field references, and their metadata to avoid repeated resolution.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/cpCache.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/cpCache.hpp)
- Key characteristics: Resolved CP entries cache, improves resolution performance

## Adapter Entities

### AdapterHandlerEntry
**Type:** Runtime Data  
**Purpose:** Manages method call adapters that handle transitions between interpreted and compiled code. Contains entry points for i2c (interpreter-to-compiled), c2i (compiled-to-interpreter), and c2c (compiled-to-compiled) transitions.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/runtime/sharedRuntime.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/sharedRuntime.hpp)
- Related: [`src/hotspot/share/runtime/sharedRuntime.cpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/sharedRuntime.cpp)
- Key characteristics: Manages code transition adapters, signature-specific

### AdapterFingerPrint
**Type:** Runtime Data  
**Purpose:** Uniquely identifies method signatures for adapter generation. Used to look up or create appropriate adapters for method calls with specific parameter types.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/runtime/sharedRuntime.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/sharedRuntime.hpp)
- Key characteristics: Signature identification, adapter lookup key

## Annotation Entities

### Annotations
**Type:** Metadata  
**Purpose:** Stores Java annotations attached to classes, methods, fields, and parameters. Includes runtime-visible and runtime-invisible annotations as defined in the classfile.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/annotations.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/annotations.hpp)
- Key characteristics: Class/method/field/parameter annotations, reflection API support

### RecordComponent
**Type:** Metadata  
**Purpose:** Represents components of Java record classes (introduced in Java 16). Stores metadata about record component names, descriptors, signatures, and annotations.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/recordComponent.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/recordComponent.hpp)
- Key characteristics: Record class metadata, component descriptors

## Training Data Entities

### CompileTrainingData
**Type:** Training Metadata  
**Purpose:** Aggregates training data collected during application profiling for compilation decisions in AOT mode. Helps determine which methods to compile and at what optimization levels.

**OpenJDK Reference:**
- Usage: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/master/src/hotspot/share/cds)
- Key characteristics: Aggregated compilation statistics, AOT decision data

### KlassTrainingData
**Type:** Training Metadata  
**Purpose:** Stores training data specific to class (Klass) behavior during profiling runs. Captures initialization patterns, inheritance relationships usage, and other class-level runtime characteristics.

**OpenJDK Reference:**
- Usage: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/master/src/hotspot/share/cds)
- Key characteristics: Class-level profiling data, initialization patterns

## Array Types

### TypeArrayU1
**Type:** Heap Array  
**Purpose:** Array of unsigned 1-byte (8-bit) integers. Used for bytecode arrays, boolean arrays, and byte arrays. Most commonly used for storing method bytecode.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 8-bit unsigned integers, bytecode storage

### TypeArrayU2
**Type:** Heap Array  
**Purpose:** Array of unsigned 2-byte (16-bit) integers. Used for char arrays and UTF-16 string storage.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 16-bit unsigned integers, char arrays

### TypeArrayU4
**Type:** Heap Array  
**Purpose:** Array of unsigned 4-byte (32-bit) integers. Used for int arrays and other 32-bit data structures.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 32-bit unsigned integers, int arrays

### TypeArrayU8
**Type:** Heap Array  
**Purpose:** Array of unsigned 8-byte (64-bit) integers. Used for long arrays, double arrays, and other 64-bit data structures.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: 64-bit unsigned integers, long/double arrays

### TypeArrayOther
**Type:** Heap Array  
**Purpose:** Represents type arrays that don't fall into the U1, U2, U4, or U8 categories. Handles other primitive array types and special cases.

**OpenJDK Reference:**
- Definition: [`src/hotspot/share/oops/typeArrayOop.hpp`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/oops/typeArrayOop.hpp)
- Key characteristics: Other primitive array types, special cases

## Miscellaneous

### Misc
**Type:** Metadata  
**Purpose:** Catch-all category for miscellaneous metadata and runtime structures that don't fit into other specific categories. May include JVM-internal data structures, temporary objects, and auxiliary metadata.

**OpenJDK Reference:**
- Various: [`src/hotspot/share/oops/`](https://github.com/openjdk/jdk/tree/master/src/hotspot/share/oops)
- Key characteristics: Various auxiliary metadata, JVM-internal structures

## Related Resources

- **CDS Architecture**: [`src/hotspot/share/cds/`](https://github.com/openjdk/jdk/tree/master/src/hotspot/share/cds)
- **Object System (oops)**: [`src/hotspot/share/oops/`](https://github.com/openjdk/jdk/tree/master/src/hotspot/share/oops)
- **CDS File Format**: [`src/hotspot/share/include/cds.h`](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/include/cds.h)
- **AOT Compilation**: [`src/hotspot/share/aot/`](https://github.com/openjdk/jdk/tree/master/src/hotspot/share/aot)

## Notes

- All metadata entities can be stored in read-only memory regions when appropriate for CDS
- Heap objects are serialized and relocated during AOT cache loading
- Training data entities are specific to AOT compilation and profiling
- Array types correspond to Java primitive arrays and are heavily used in the JVM internals

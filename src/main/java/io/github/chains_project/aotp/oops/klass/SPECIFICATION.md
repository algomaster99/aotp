## Overview

This document describes how HotSpot’s C++ metadata structures `Klass` and
`InstanceKlass` (from `src/hotspot/share/oops/klass.hpp` and
`src/hotspot/share/oops/instanceKlass.hpp`) are mapped into the Java-side
abstractions `ClassEntry` and `InstanceClass` under
`io.github.chains_project.aotp.oops.klass`.

Unless noted otherwise, all HotSpot pointer types (e.g. `Klass*`, `Array*`,
`Symbol*`, `OopHandle`, etc.) are represented as 64‑bit absolute addresses
stored in Java `long` fields that point into the AOT cache address space.
All explicit padding rows in the tables are chosen so that every pointer‑sized
field remains **8‑byte aligned** in the serialized layout.

Since AOT Cache is not a stable feature, the current layout is based on
[OpenJDK 27+7](https://github.com/openjdk/jdk/tree/jdk-27%2B7).

---

## `Klass` header → `ClassEntry`

Source: `src/hotspot/share/oops/klass.hpp`

Java type: `io.github.chains_project.aotp.oops.klass.ClassEntry`

| Original HotSpot field (type name)                                      | Java abstraction (type and field)                                   |
|-------------------------------------------------------------------------|---------------------------------------------------------------------|
| C++ vtable pointer (implicit, before `Klass` object)                   | `long vTablePointer` — raw address of the C++ vtable                |
| `jint _layout_helper`                                                  | `int layoutHelper`                                                  |
| `Klass::KlassKind _kind` (`enum KlassKind : u2`)                       | `short kind` — 16‑bit kind (stored in Java `int` field)             |
| `KlassFlags _misc_flags` (backed by `klass_flags_t` / `u1`)            | `byte miscFlags` — 8‑bit flags (stored in Java `long` field)        |
| *(padding after `_kind` / `_misc_flags`)*                               | **1 byte padding** to 8‑byte align `_super_check_offset`           |
| `juint _super_check_offset`                                            | `int superCheckOffset`                                              |
| *(padding after `_super_check_offset`)*                                 | **4 bytes padding** to 8‑byte align `_name` and following pointers |
| `Symbol* _name`                                                        | `long _name` — pointer to `Symbol` (class name)                     |
| `Klass* _secondary_super_cache`                                       | `long secondarySuperCache`                                          |
| `Array<Klass*>* _secondary_supers`                                    | `long secondarySupers`                                              |
| `Klass* _primary_supers[_primary_super_limit]` (array, limit = 8)      | `long[] primarySupers` — fixed‑size array of 8 superclass pointers  |
| `OopHandle _java_mirror`                                               | `long javaMirror` — pointer to `java.lang.Class` mirror             |
| `Klass* _super`                                                        | `long _super`                                                       |
| `Klass* volatile _subklass`                                            | `long subklass`                                                     |
| `Klass* volatile _next_sibling`                                        | `long nextSibling`                                                  |
| `Klass* _next_link`                                                    | `long nextLink`                                                     |
| `ClassLoaderData* _class_loader_data`                                  | `long classLoaderData`                                              |
| `markWord _prototype_header`                                           | `long prototypeHeader` — header word captured as 64‑bit value       |
| `uintx _secondary_supers_bitmap`                                       | `long secondarySupersBitmap`                                        |
| `uint8_t _hash_slot`                                                   | `byte hashSlot`                                                     |
| `s2 _shared_class_path_index`                                          | `short sharedClassPathIndex`                                        |
| `u2 _aot_class_flags` (inside `#if INCLUDE_CDS`)                       | `short aotClassFlags`                                               |
| *(padding before `_vtable_len`)*                                       | **3 bytes padding** so `int vtableLen` is 8‑byte aligned            |
| `int _vtable_len`                                                      | `int vtableLen`                                                     |
| `int _archived_mirror_index` (inside `CDS_JAVA_HEAP_ONLY`)             | `int archivedMirrorIndex`                                           |
| `JFR_ONLY(DEFINE_TRACE_ID_FIELD;)` (JFR trace id field, 64‑bit)        | `long jfrTrace` — trace id captured as 64‑bit value                 |

---

## `InstanceKlass` → `InstanceClass`

Source: `src/hotspot/share/oops/instanceKlass.hpp`

Java type: `io.github.chains_project.aotp.oops.klass.InstanceClass`

All fields below are *in addition to* the `ClassEntry` header fields above,
and follow the same “pointer → `long` address” convention.

| Original HotSpot field (type name)                           | Java abstraction (type and field)                            |
|--------------------------------------------------------------|--------------------------------------------------------------|
| `Annotations* _annotations`                                  | `long annotations`                                           |
| `PackageEntry* _package_entry`                               | `long packageEntry`                                          |
| `ObjArrayKlass* volatile _array_klasses`                     | `long arrayKlasses`                                          |
| `ConstantPool* _constants`                                   | `long constants`                                             |
| `Array<u2>* _inner_classes`                                  | `long innerClasses`                                          |
| `Array<u2>* _nest_members`                                   | `long nestMembers`                                           |
| `InstanceKlass* _nest_host`                                 | `long nestHost`                                              |
| `Array<u2>* _permitted_subclasses`                           | `long permittedSubclasses`                                   |
| `Array* _record_components`                                  | `long recordComponents`                                      |
| `const char* _source_debug_extension`                        | `long sourceDebugExtension` — pointer to UTF‑8 debug string  |
| `int _nonstatic_field_size`                                  | `int nonStaticFieldSize`                                     |
| `int _static_field_size`                                     | `int staticFieldSize`                                        |
| `int _nonstatic_oop_map_size`                                | `int nonStaticOopMapSize`                                    |
| `int _itable_len`                                            | `int itableLen`                                              |
| `u2 _nest_host_index`                                        | `short nestHostIndex`                                        |
| `u2 _this_class_index`                                       | `short thisClassIndex`                                       |
| `u2 _static_oop_field_count`                                 | `short staticOopFieldCount`                                  |
| `volatile u2 _idnum_allocated_count`                         | `short idnumAllocatedCount`                                  |
| `ClassState _init_state` (`enum ClassState : u1`)            | `byte initState`                                             |
| `u1 _reference_type`                                         | `byte referenceType`                                         |
| `AccessFlags _access_flags`                                  | `short accessFlags` — packed access flags                    |
| `InstanceKlassFlags _misc_flags`                             | `InstanceClassFlags miscFlags_fromInstanceKlass` — Java record wrapping the flags (`short flags`, `byte status`) |
| *(padding after `InstanceKlassFlags _misc_flags`)*           | **1 byte padding** before `long initThread` (8‑byte aligned) |
| `JavaThread* volatile _init_thread`                          | `long initThread`                                            |
| `OopMapCache* volatile _oop_map_cache`                       | `long oopMapCache`                                           |
| `JNIid* _jni_ids`                                            | `long jniIds`                                                |
| `jmethodID* volatile _methods_jmethod_ids`                   | `long methodsJmethodIds`                                     |
| `nmethodBucket* volatile _dep_context`                       | `long depContext`                                            |
| `uint64_t volatile _dep_context_last_cleaned`                | `long depContextLastCleaned`                                 |
| `nmethod* _osr_nmethods_head`                                | `long osrNmethodsHead`                                       |
| `BreakpointInfo* _breakpoints` (under `#if INCLUDE_JVMTI`)   | `long breakpoints`                                           |
| `InstanceKlass* _previous_versions` (JVMTI)                  | `long previousVersions`                                      |
| `JvmtiCachedClassFileData* _cached_class_file` (JVMTI)       | `long cachedClassFile`                                       |
| `JvmtiCachedClassFieldMap* _jvmti_cached_class_field_map`    | `long jvmtiCachedClassFieldMap`                              |
| `NOT_PRODUCT(int _verify_count;)`                            | *not modeled* †                                               |
| `NOT_PRODUCT(volatile int _shared_class_load_count;)`        | *not modeled* †                                               |
| `Array* _methods`                                            | `long methods`                                               |
| `Array* _default_methods`                                    | `long defaultMethods`                                        |
| `Array* _local_interfaces`                                   | `long localInterfaces`                                       |
| `Array* _transitive_interfaces`                              | `long transitiveInterfaces`                                  |
| `Array* _method_ordering`                                    | `long methodOrdering`                                        |
| `Array* _default_vtable_indices`                             | `long defaultVtableIndices`                                  |
| `Array* _fieldinfo_stream`                                   | `long fieldInfoStream`                                       |
| `Array* _fieldinfo_search_table`                             | `long fieldInfoSearchTable`                                  |
| `Array* _fields_status`                                      | `long fieldsStatus`                                          |

> † These fields are not modeled in Java because they are only available in DEBUG builds
> and [AOT Cache features do not exist in DEBUG builds](https://bugs.openjdk.org/browse/JDK-8301715).

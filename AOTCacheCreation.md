## This document describes the process of creating an AOTCache (using `-XX:AOTCacheOutput`) file by walking through the JDK codebase.

1. First the JVM calls its [main program](https://github.com/openjdk/jdk/blob/jdk-27%2B7/src/java.base/share/native/launcher/main.c).
2. Then it sets up argc and argv which can be printed in `gdb` using `-exec x/s` or `-exec p`.
3. There is a new thread which is spawned [here](https://github.com/openjdk/jdk/blob/jdk-27%2B7/src/java.base/share/native/libjli/java.c#L2343).
4. The new thread almost begins [here](https://github.com/openjdk/jdk/blob/jdk-27%2B7/src/java.base/unix/native/libjli/java_md.c#L646).
5. The VM creation calls [universe_init](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/runtime/init.cpp#L138) which I think is responsible for setting up AOTCache settings.

### Thread that runs `Hello`
> The goal of this is to know how AOTCache memory is dumped into a file.

> Do not click `this` on frame "filemapcpp:open_as_output()". It hangs the debugger.

```cpp
      rs = MemoryReserver::reserve((char*)base,
                                   size,
                                   Metaspace::reserve_alignment(),
                                   os::vm_page_size(),
                                   mtClass);
```
This can be used for reserving memory.
`_base` is `0x88000000` (unsure how this is computed)
`_size` is `1073741824` (unsure why it decides to save ~1GB)
`Metaspace::reserve_alignment` seems to be `metaspace::Settings::virtual_space_node_reserve_alignment_words()` x `BytesPerWord`
` os::vm_page_size()` seems constant `4096`

[Here](https://github.com/openjdk/jdk/blob/jdk-27%2B7/src/hotspot/share/memory/metaspace.cpp#L581) the space allocation completes.
It has exact same size as `_size`. Size is calculated based on `_class_space_end` - `_class_space_start`

Not sure what [CompressedClassPointers](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/memory/metaspace.cpp#L812) are.

Important to put a breakpoint [here](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/memory/universe.cpp#L903) as it just finishes after `Metaspace::global_initialize();`.

[before_exit](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/runtime/java.cpp#L453)
starts dumping .aot.config. **This is the entry point when dumping aot config archives**

[This function](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/cds/aotMetaspace.cpp#L947)
dumps aot.config.

[`link_all_loaded_classes`](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/cds/aotMetaspace.cpp#L932)
can be used for linking classes during merge.

```java
  if (CDSConfig::is_dumping_preimage_static_archive()) {
    AOTMetaspace::dump_static_archive(thread);
  }
```

The above code block comes
[here](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/cds/aotMetaspace.cpp#L958).

Not sure what [`op`](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/cds/aotMetaspace.cpp#L1184)
stands for but it seems it is responsible for serialization in a separate thread.

This is called in `before_exit` but as you can see that it is only called when we are dumping conf.

[fork_and_dump_final_static_archive](https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/cds/aotMetaspace.cpp#L1205)

- `src/hotspot/share/cds/aotMetaspace.cpp` has lines `1317-1327` which setups the arguments.
For example, it converts `-XX:AOTCacheOutput` to `-XX:AOTMode=create`.
`=record` is done somewhere before.
- `src/hotspot/share/cds/aotMetaspace.cpp:1321` starts the creation of the AOTCache file.
- `src/hotspot/share/cds/aotMetaspace.cpp:345` goes to `src/hotspot/share/cds/filemap.cpp:773` and writes the AOTCache to disk.

> If you want to reconstruct AOTCache contents, you effectively have to follow what ArchiveBuilder and AOTMapLogger do, rather than looking for an extra on‑disk schema beyond the header+region metadata.
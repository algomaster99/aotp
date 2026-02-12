#!/usr/bin/env python3
"""
Merge two AOT cache files into one: single header, merged RW and RO regions.
Uses mmap for efficient copying. Relocates pointers in the second cache's
RW/RO payloads so they refer to the new layout. BM, HP, AC are not merged
(copied from cache1 only or left unused).

Usage:
  python3 merge_aot_cache.py cache1.aot cache2.aot merged.aot

Requirements: Both caches must be from the same JDK build (same alignment,
requested base layout). Merged archive uses cache1's requested_base_address
and cache1's header as template.
"""

import mmap
import os
import struct
import sys

# CDS layout (64-bit, matches cds.h and filemap.hpp)
NUM_REGIONS = 5
GENERIC_HEADER_SIZE = 24
REGION_SIZE = 96  # 6*4 + 7*8 + 8 + 1 + 7 padding
REGIONS_START = 24  # after GenericCDSFileMapHeader
REGION_FILE_OFFSET_OFF = 24   # within each region struct
REGION_MAPPING_OFFSET_OFF = 32
REGION_USED_OFF = 40
CORE_ALIGNMENT_OFF = 504      # first field after 5 regions in FileMapHeader
REQUESTED_BASE_OFF = 864      # requested_base_address in full header

CDS_ARCHIVE_MAGIC = 0xf00baba2
CDS_DYNAMIC_ARCHIVE_MAGIC = 0xf00baba8

# Default shared base when header has requested_base_address == 0 (e.g. some dumps)
DEFAULT_SHARED_BASE = 0x0000_0008_0000_0000


def align_up(value: int, alignment: int) -> int:
    if alignment <= 0:
        return value
    return (value + alignment - 1) // alignment * alignment


def read_u32(buf: memoryview, off: int) -> int:
    return struct.unpack_from("<I", buf, off)[0]


def read_u64(buf: memoryview, off: int) -> int:
    return struct.unpack_from("<Q", buf, off)[0]


def write_u64(buf: memoryview, off: int, value: int) -> None:
    struct.pack_into("<Q", buf, off, value & 0xFFFFFFFF_FFFFFFFF)


def parse_header(data: memoryview):
    """Parse generic header and a few regions. Returns dict."""
    header_size = read_u32(data, 12)
    if header_size > len(data):
        raise ValueError("header_size %u > buffer size" % header_size)
    alignment = read_u64(data, CORE_ALIGNMENT_OFF)
    requested_base = read_u64(data, REQUESTED_BASE_OFF)

    def region(i: int):
        base = REGIONS_START + i * REGION_SIZE
        return {
            "file_offset": read_u64(data, base + REGION_FILE_OFFSET_OFF),
            "mapping_offset": read_u64(data, base + REGION_MAPPING_OFFSET_OFF),
            "used": read_u64(data, base + REGION_USED_OFF),
        }

    return {
        "header_size": header_size,
        "alignment": alignment,
        "requested_base": requested_base,
        "rw": region(0),
        "ro": region(1),
        "bm": region(2),
    }


def shift_pointers(
    blob: bytearray,
    base_old: int,
    size_old: int,
    delta: int,
    also_ro_base: int,
    also_ro_size: int,
    delta_ro: int,
) -> None:
    """
    In-place: add delta to every 8-byte little-endian value in blob that
    lies in [base_old, base_old+size_old) or [also_ro_base, also_ro_base+also_ro_size).
    Used to relocate pointers from cache2's address space to merged layout.
    """
    n = len(blob) // 8
    for i in range(n):
        off = i * 8
        val = read_u64(blob, off)
        if base_old <= val < base_old + size_old:
            write_u64(blob, off, val + delta)
        elif also_ro_base <= val < also_ro_base + also_ro_size:
            write_u64(blob, off, val + delta_ro)


def merge(cache1_path: str, cache2_path: str, out_path: str) -> None:
    with open(cache1_path, "rb") as f1, open(cache2_path, "rb") as f2:
        len1 = os.fstat(f1.fileno()).st_size
        len2 = os.fstat(f2.fileno()).st_size
        m1 = mmap.mmap(f1.fileno(), 0, access=mmap.ACCESS_READ)
        m2 = mmap.mmap(f2.fileno(), 0, access=mmap.ACCESS_READ)

    try:
        h1 = parse_header(m1)
        h2 = parse_header(m2)
    finally:
        m1.close()
        m2.close()

    # Use cache1's alignment and requested base for merged file
    alignment = h1["alignment"]
    requested_base1 = h1["requested_base"] or DEFAULT_SHARED_BASE
    requested_base2 = h2["requested_base"] or DEFAULT_SHARED_BASE
    if (h1["requested_base"] or h2["requested_base"]) == 0:
        print("Warning: requested_base_address is 0 in header; using default 0x%x for pointer relocation"
              % DEFAULT_SHARED_BASE, file=sys.stderr)
    rw1, ro1, bm1 = h1["rw"], h1["ro"], h1["bm"]
    rw2, ro2 = h2["rw"], h2["ro"]

    # Inside a region (rw or ro), HotSpot does NOT insert extra padding – it
    # just dumps a contiguous blob. Padding is only used to align the *start*
    # of each region in the file. Follow that here: concat rw1+rw2 and ro1+ro2
    # without internal padding, and only align between regions.
    rw1_used = rw1["used"]
    rw2_used = rw2["used"]
    ro1_used = ro1["used"]
    ro2_used = ro2["used"]
    bm1_used = bm1["used"]

    merged_rw_used = rw1_used + rw2_used
    merged_ro_used = ro1_used + ro2_used

    # Layout:
    #   [header][pad] [rw (rw1+rw2)] [pad] [ro (ro1+ro2)] [pad] [bm1]
    out_rw_file_off = align_up(h1["header_size"], alignment)
    rw_end = out_rw_file_off + merged_rw_used
    out_ro_file_off = align_up(rw_end, alignment)
    ro_end = out_ro_file_off + merged_ro_used
    out_bm_file_off = align_up(ro_end, alignment)
    bm1_aligned = align_up(bm1_used, alignment)
    total_size = out_bm_file_off + bm1_aligned

    # Deltas for pointer relocation in cache2's payloads.
    # For offline tools like aotp, absolute addresses are treated as
    #   abs = requested_base + file_offset
    # not using mapping_offset. Preserve that model here so that
    #   symbol_abs - requested_base == file_position
    # continues to hold after merging.
    #
    # Old cache2 RW block:  abs_old = requested_base2 + (rw2.file_offset + inner)
    # New merged RW2 block: abs_new = requested_base1 + (out_rw_file_off + rw1_used + inner)
    new_rw2_start = requested_base1 + (out_rw_file_off + rw1_used)
    old_rw2_start = requested_base2 + rw2["file_offset"]
    delta_rw = new_rw2_start - old_rw2_start

    # Old cache2 RO block:  abs_old = requested_base2 + (ro2.file_offset + inner)
    # New merged RO2 block: abs_new = requested_base1 + (out_ro_file_off + ro1_used + inner)
    new_ro2_start = requested_base1 + (out_ro_file_off + ro1_used)
    old_ro2_start = requested_base2 + ro2["file_offset"]
    delta_ro = new_ro2_start - old_ro2_start

    # Build output with one header and merged rw/ro (+ bitmap from cache1)
    with open(out_path, "wb") as out_f:
        out_f.truncate(total_size)

    with open(out_path, "r+b") as out_f:
        out_m = mmap.mmap(out_f.fileno(), total_size, access=mmap.ACCESS_WRITE)

    try:
        # 1) Copy header from cache1 and patch region 0 and 1
        with open(cache1_path, "rb") as f1:
            header_bytes = f1.read(h1["header_size"])
        out_m[:h1["header_size"]] = header_bytes

        # Patch region 0 (rw): file_offset, used
        r0_base = REGIONS_START + 0 * REGION_SIZE
        write_u64(out_m, r0_base + REGION_FILE_OFFSET_OFF, out_rw_file_off)
        write_u64(out_m, r0_base + REGION_USED_OFF, merged_rw_used)

        # Patch region 1 (ro): file_offset, used
        r1_base = REGIONS_START + 1 * REGION_SIZE
        write_u64(out_m, r1_base + REGION_FILE_OFFSET_OFF, out_ro_file_off)
        write_u64(out_m, r1_base + REGION_USED_OFF, merged_ro_used)

        # Patch Bitmap region (2): keep its original contents from cache1 but
        # move it after the merged RO region so the JVM can map region #2.
        bm_base = REGIONS_START + 2 * REGION_SIZE
        write_u64(out_m, bm_base + REGION_FILE_OFFSET_OFF, out_bm_file_off)
        write_u64(out_m, bm_base + REGION_USED_OFF, bm1_used)

        # Zero CRCs so VM doesn't trust stale checksums (optional; VM may still reject)
        struct.pack_into("<I", out_m, 4, 0)
        struct.pack_into("<i", out_m, r0_base, 0)
        struct.pack_into("<i", out_m, r1_base, 0)

        # 2) Copy RW: cache1 rw, then cache2 rw (with pointer shift), no
        # internal padding – just a contiguous blob.
        with open(cache1_path, "rb") as f1:
            f1.seek(rw1["file_offset"])
            rw1_data = f1.read(rw1_used)
        out_m[out_rw_file_off:out_rw_file_off + rw1_used] = rw1_data

        with open(cache2_path, "rb") as f2:
            f2.seek(rw2["file_offset"])
            rw2_data = bytearray(f2.read(rw2["used"]))
        shift_pointers(
            rw2_data,
            old_rw2_start,
            rw2["used"],
            delta_rw,
            old_ro2_start,
            ro2["used"],
            delta_ro,
        )
        rw2_off = out_rw_file_off + rw1_used
        out_m[rw2_off:rw2_off + rw2_used] = rw2_data

        # 3) Copy RO: cache1 ro, then cache2 ro (with pointer shift), again
        # as a contiguous blob.
        with open(cache1_path, "rb") as f1:
            f1.seek(ro1["file_offset"])
            ro1_data = f1.read(ro1_used)
        out_m[out_ro_file_off:out_ro_file_off + ro1_used] = ro1_data

        with open(cache2_path, "rb") as f2:
            f2.seek(ro2["file_offset"])
            ro2_data = bytearray(f2.read(ro2["used"]))
        shift_pointers(
            ro2_data,
            old_rw2_start,
            rw2["used"],
            delta_rw,
            old_ro2_start,
            ro2["used"],
            delta_ro,
        )
        ro2_off = out_ro_file_off + ro1_used
        out_m[ro2_off:ro2_off + ro2_used] = ro2_data

        # 4) Copy Bitmap region (from cache1) after merged RO, including padding
        if bm1_used > 0:
            with open(cache1_path, "rb") as f1:
                f1.seek(bm1["file_offset"])
                bm_data = f1.read(bm1_used)
            out_m[out_bm_file_off:out_bm_file_off + bm1_used] = bm_data
            # zero padding up to bm1_aligned
            pad_bm = bm1_aligned - bm1_used
            if pad_bm > 0:
                out_m[out_bm_file_off + bm1_used:out_bm_file_off + bm1_aligned] = bytes(pad_bm)

        out_m.flush()
    finally:
        out_m.close()

    print("Merged: %s + %s -> %s" % (cache1_path, cache2_path, out_path))
    print("  RW: %u + %u -> %u bytes at file offset 0x%x" % (
        rw1["used"], rw2["used"], merged_rw_used, out_rw_file_off))
    print("  RO: %u + %u -> %u bytes at file offset 0x%x" % (
        ro1["used"], ro2["used"], merged_ro_used, out_ro_file_off))
    print("  Pointer deltas: rw %+d, ro %+d" % (delta_rw, delta_ro))


def main():
    if len(sys.argv) != 4:
        print("Usage: merge_aot_cache.py <cache1.aot> <cache2.aot> <merged.aot>", file=sys.stderr)
        sys.exit(1)
    merge(sys.argv[1], sys.argv[2], sys.argv[3])


if __name__ == "__main__":
    main()

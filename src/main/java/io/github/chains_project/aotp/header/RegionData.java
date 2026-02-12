package io.github.chains_project.aotp.header;

import java.io.IOException;

import io.github.chains_project.aotp.utils.LittleEndianRandomAccessFile;

/**
 * Holds the raw bytes for a single CDS/AOT region together with its
 * {@link CDSFileMapRegion} metadata.
 *
 * This is a tool-side abstraction only – it does not exist in HotSpot.
 * The on-disk format for each region is defined by the VM; here we just
 * snapshot the exact bytes belonging to that region so higher-level code
 * can analyze them without repeatedly seeking in the underlying file.
 */
public final class RegionData {

    // Index of the AOT code region in AOTMetaspace::Region (rw, ro, bm, hp, ac)
    private static final int AC_REGION_INDEX = 4;

    private final int index;
    private final CDSFileMapRegion region;
    private final byte[] bytes;

    public RegionData(int index, CDSFileMapRegion region, byte[] bytes) {
        this.index = index;
        this.region = region;
        this.bytes = bytes;
    }

    public int index() {
        return index;
    }

    public CDSFileMapRegion region() {
        return region;
    }

    /**
     * Raw bytes of the region payload as stored in the file.
     *
     * For non-AC regions, this range starts at {@code region.fileOffset} and
     * ends at the next region's {@code fileOffset}, so it includes any padding
     * that HotSpot inserted between regions for alignment.
     *
     * For the AC region (AOT code), this is currently an empty array; we skip
     * detailed parsing of AOT code blobs for now.
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * Load all regions' raw bytes into memory, based on the {@link CDSFileMapRegion}
     * metadata that has already been parsed.
     *
     * The caller must have already read the generic header and region array;
     * this method will temporarily seek within the underlying file but will
     * restore the original file position before returning.
     */
    public static RegionData[] loadAll(LittleEndianRandomAccessFile file,
                                       CDSFileMapRegion[] regions) throws IOException {
        RegionData[] result = new RegionData[regions.length];

        long originalPos = file.getFilePointer();
        try {
            long fileLength = file.length();

            for (int i = 0; i < regions.length; i++) {
                CDSFileMapRegion r = regions[i];
                long start = r.fileOffset;

                // Skip AC region for now – we don't attempt to decode its contents.
                if (i == AC_REGION_INDEX) {
                    result[i] = new RegionData(i, r, new byte[0]);
                    continue;
                }

                if (start < 0 || start >= fileLength) {
                    result[i] = new RegionData(i, r, new byte[0]);
                    continue;
                }

                // Find the next region that starts after this one; that defines
                // the end of this region's byte range (including padding).
                long end = fileLength;
                for (int j = 0; j < regions.length; j++) {
                    if (j == i) continue;
                    long otherStart = regions[j].fileOffset;
                    if (otherStart > start && otherStart < end) {
                        end = otherStart;
                    }
                }

                long span = end - start;
                if (span <= 0) {
                    result[i] = new RegionData(i, r, new byte[0]);
                    continue;
                }

                // Seek to the start of this region's payload and read until just
                // before the next region's fileOffset. This includes any 0-padding.
                file.seek(start);
                int length = (int) span;
                byte[] data = new byte[length];
                file.readFully(data);

                result[i] = new RegionData(i, r, data);
            }
        } finally {
            // Restore caller's view of the file pointer.
            file.seek(originalPos);
        }

        return result;
    }
}


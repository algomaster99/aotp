package io.github.chains_project.aotp.header;

import java.io.IOException;

import io.github.chains_project.aotp.LittleEndianRandomAccessFile;

public class GenericHeader {
    private int magic;
    int crc;
    int version;
    int headerSize;
    int baseArchivePathOffset;
    int baseArchiveNameSize;

    public GenericHeader(LittleEndianRandomAccessFile dis) throws IOException {
        magic = dis.readInt();
        crc = dis.readInt();
        version = dis.readInt();
        headerSize = dis.readInt();
        baseArchivePathOffset = dis.readInt();
        baseArchiveNameSize = dis.readInt();
    }

    public void print(Appendable st) throws java.io.IOException {
        st.append(String.format("- magic:                          0x%08x%n", magic));
        st.append(String.format("- crc:                            0x%08x%n", crc));
        st.append(String.format("- version:                        0x%x%n", version));
        st.append(String.format("- header_size:                    %d%n", headerSize));
        st.append(String.format("- base_archive_name_offset:       %d%n", baseArchivePathOffset));
        st.append(String.format("- base_archive_name_size:         %d%n", baseArchiveNameSize));
    }

    public int magic() {
        return magic;
    }
}

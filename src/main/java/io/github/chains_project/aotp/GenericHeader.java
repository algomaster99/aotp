package io.github.chains_project.aotp;

import java.io.IOException;

import com.google.common.io.LittleEndianDataInputStream;

public class GenericHeader {
    int magic;
    int crc;
    int version;
    int headerSize;
    int baseArchivePathOffset;
    int baseArchiveNameSize;

    public GenericHeader(LittleEndianDataInputStream dis) throws IOException {
        magic = dis.readInt();
        crc = dis.readInt();
        version = dis.readInt();
        headerSize = dis.readInt();
        baseArchivePathOffset = dis.readInt();
        baseArchiveNameSize = dis.readInt();
    }
}

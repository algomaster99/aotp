package io.github.chains_project.aotp.utils;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Minimal little-endian wrapper around {@link RandomAccessFile}
 * This helps with seeking to absolute positions in the file.
 */
public class LittleEndianRandomAccessFile {

    private final RandomAccessFile raf;

    public LittleEndianRandomAccessFile(RandomAccessFile raf) {
        this.raf = raf;
    }

    public int readInt() throws IOException {
        int ch1 = raf.read();
        int ch2 = raf.read();
        int ch3 = raf.read();
        int ch4 = raf.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            return -1;
        }
        return (ch1 & 0xFF)
            | ((ch2 & 0xFF) << 8)
            | ((ch3 & 0xFF) << 16)
            | ((ch4 & 0xFF) << 24);
    }

    public long readLong() throws IOException {
        long b1 = raf.read();
        long b2 = raf.read();
        long b3 = raf.read();
        long b4 = raf.read();
        long b5 = raf.read();
        long b6 = raf.read();
        long b7 = raf.read();
        long b8 = raf.read();
        if ((b1 | b2 | b3 | b4 | b5 | b6 | b7 | b8) < 0) {
            return -1L;
        }
        return (b1 & 0xFFL)
            | ((b2 & 0xFFL) << 8)
            | ((b3 & 0xFFL) << 16)
            | ((b4 & 0xFFL) << 24)
            | ((b5 & 0xFFL) << 32)
            | ((b6 & 0xFFL) << 40)
            | ((b7 & 0xFFL) << 48)
            | ((b8 & 0xFFL) << 56);
    }

    public short readShort() throws IOException {
        int ch1 = raf.read();
        int ch2 = raf.read();
        if ((ch1 | ch2) < 0) {
            return -1;
        }
        return (short) ((ch1 & 0xFF) | ((ch2 & 0xFF) << 8));
    }

    public boolean readBoolean() throws IOException {
        int ch = raf.read();
        if (ch < 0) {
            return false;
        }
        return ch != 0;
    }

    public void readFully(byte[] b) throws IOException {
        int off = 0;
        int len = b.length;
        while (len > 0) {
            int count = raf.read(b, off, len);
            if (count < 0) {
                return;
            }
            off += count;
            len -= count;
        }
    }

    public int read(byte[] b) throws IOException {
        return raf.read(b);
    }

    public void skipBytes(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        long newPos = raf.getFilePointer() + n;
        raf.seek(newPos);
    }

    public void seek(long pos) throws IOException {
        raf.seek(pos);
    }

    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    public long length() throws IOException {
        return raf.length();
    }

    public void close() throws IOException {
        raf.close();
    }
}


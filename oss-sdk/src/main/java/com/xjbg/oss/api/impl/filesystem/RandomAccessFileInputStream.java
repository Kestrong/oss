package com.xjbg.oss.api.impl.filesystem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * @author kesc
 * @since 2023-07-18 14:40
 */
public class RandomAccessFileInputStream extends InputStream {
    private final RandomAccessFile randomAccessFile;
    private final Long length;
    private Long readBytes = 0L;

    public RandomAccessFileInputStream(RandomAccessFile randomAccessFile, Long offset, Long length) throws IOException {
        this.randomAccessFile = randomAccessFile;
        if (offset == null) {
            offset = 0L;
        }
        if (length == null) {
            length = randomAccessFile.length() - offset;
        }
        this.length = length;
        randomAccessFile.seek(offset);
    }

    @Override
    public int read() throws IOException {
        if (available() <= 0) {
            return -1;
        }
        readBytes += 1;
        return randomAccessFile.read();
    }

    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        if (available() < len) {
            len = available();
        }
        int read = randomAccessFile.read(b, off, len);
        if (read != -1) {
            readBytes += read;
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        return randomAccessFile.skipBytes((int) n);
    }

    @Override
    public int available() throws IOException {
        return length.intValue() - readBytes.intValue();
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }

}

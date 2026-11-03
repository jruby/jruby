package org.jruby.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * A PushbackInputStream that grows its buffer as needed, so any amount of data can be pushed back.
 * Used where Ruby APIs like ungetc and ungetbyte accept arbitrarily large strings.
 */
public class GrowingPushbackInputStream extends PushbackInputStream {
    public GrowingPushbackInputStream(InputStream in, int size) {
        super(in, size);
    }

    @Override
    public synchronized void unread(int b) throws IOException {
        ensurePushbackSpace(1);
        super.unread(b);
    }

    @Override
    public synchronized void unread(byte[] b, int off, int len) throws IOException {
        ensurePushbackSpace(len);
        super.unread(b, off, len);
    }

    private void ensurePushbackSpace(int needed) {
        if (buf == null || needed <= pos) return;

        int used = buf.length - pos;
        byte[] newBuf = new byte[Math.max(buf.length * 2, used + needed)];
        int newPos = newBuf.length - used;
        System.arraycopy(buf, pos, newBuf, newPos, used);
        buf = newBuf;
        pos = newPos;
    }
}

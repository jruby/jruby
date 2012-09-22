package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Use mark to make a safe rewindable cursor.  This assume you know
 * the stream supports mark().
 */
public class InputStreamMarkCursor {
    private InputStream in;
    private int i = 0;
    private int markSize;
    private int actualReadTotal;
    private byte[] buf;
    private int endPoint = 0;

    public InputStreamMarkCursor(InputStream in, int markSize) {
        this.in = in;
        this.markSize = markSize;
        buf = new byte[0];
    }

    public int read() throws IOException {
        if (buf.length == 0 || i > buf.length) { // overflow mark
            if (buf.length != 0) {
                in.reset(); // only reset if we have an active mark
            }
            buf = new byte[buf.length + markSize];
            in.mark(buf.length + markSize);
            actualReadTotal = in.read(buf, 0, buf.length);
        }

        return i >= actualReadTotal ? -1 : buf[i++];
    }

    public void endPoint(int delta) {
        endPoint = i + delta;
    }

    public void rewind() {
        i--;
    }

    /**
     * reset back to mark and then read back to endPoint to repoint stream back
     * to where we want next consumer of stream to start reading from.
     */
    public void finish() throws IOException {
        in.reset();
        buf = new byte[endPoint];
        in.read(buf, 0, endPoint);
    }

    public void reset() throws IOException {
        in.reset();
    }
}

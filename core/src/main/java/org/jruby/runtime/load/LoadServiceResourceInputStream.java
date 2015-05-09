package org.jruby.runtime.load;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author nicksieger
 */
public class LoadServiceResourceInputStream extends ByteArrayInputStream {
    /**
     * Construct a LoadServiceResourceInputStream from the given bytes.
     *
     * @param bytes the bytes to wrap in this stream
     */
    public LoadServiceResourceInputStream(byte[] bytes) {
        super(bytes);
    }

    /**
     * Construct a new LoadServiceInputStream by reading all bytes from the
     * specified stream.
     *
     * You are responsible for the lifecycle of the given stream after this
     * constructor has been called (i.e. it will not be closed for you).
     *
     * @param stream the stream from which to read bytes
     * @throws IOException if the reading causes an IOException
     */
    public LoadServiceResourceInputStream(InputStream stream) throws IOException {
        super(new byte[0]);
        bufferEntireStream(stream);
    }

    public byte[] getBytes() {
        if (buf.length != count) {
            byte[] b = new byte[count];
            System.arraycopy(buf, 0, b, 0, count);
            return b;
        } else {
            return buf;
        }
    }

    private void bufferEntireStream(InputStream stream) throws IOException {
        byte[] b = new byte[16384];
        int bytesRead = 0;
        while ((bytesRead = stream.read(b)) != -1) {
            byte[] newbuf = new byte[buf.length + bytesRead];
            System.arraycopy(buf, 0, newbuf, 0, buf.length);
            System.arraycopy(b, 0, newbuf, buf.length, bytesRead);
            buf = newbuf;
            count = buf.length;
        }
    }
}

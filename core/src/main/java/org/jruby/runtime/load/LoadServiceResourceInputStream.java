package org.jruby.runtime.load;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LoadServiceResourceInputStream extends ByteArrayInputStream {

    private static final byte[] NULL_BYTE_ARRAY = new byte[0];

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
        super(NULL_BYTE_ARRAY);
        bufferEntireStream(stream);
    }

    public byte[] getBytes() {
        if (buf.length != count) {
            byte[] b = new byte[count];
            System.arraycopy(buf, 0, b, 0, count);
            return b;
        }
        return buf;
    }

    private void bufferEntireStream(InputStream stream) throws IOException {
        byte[] all = stream.readAllBytes();
        buf = all;
        count = all.length;
    }
}

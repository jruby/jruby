package org.jruby.util.io;

import java.io.ByteArrayOutputStream;

/**
 * A ByteArrayOutputStream that provides access to the contained byte[] buffer.
 */
public class TransparentByteArrayOutputStream extends ByteArrayOutputStream {
    public byte[] getRawBytes() {
        return buf;
    }
}

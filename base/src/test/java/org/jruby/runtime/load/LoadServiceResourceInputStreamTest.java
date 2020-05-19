package org.jruby.runtime.load;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import junit.framework.TestCase;

/**
 *
 * @author nicksieger
 */
public class LoadServiceResourceInputStreamTest extends TestCase {
    public void testBufferEntireStreamDuplicatesStream() throws IOException {
        assertValidCopy(new byte[10]);
        assertValidCopy(new byte[1050]);
        assertValidCopy(new byte[9000]);
    }

    private void assertValidCopy(byte[] b) throws IOException {
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) (i % 128);
        }
        LoadServiceResourceInputStream lsris = new LoadServiceResourceInputStream(new ByteArrayInputStream(b));
        for (int i = 0; i < b.length; i++) {
            assertEquals(b[i], lsris.read());
            assertEquals(b[i], lsris.getBytes()[i]);
        }
    }
}

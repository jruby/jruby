package org.jruby.util;

import java.io.*;
import java.io.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockingInputStream extends FilterInputStream {
    /**
     * Constructor for BlockingInputStream.
     * @param inStream
     */
    public BlockingInputStream(InputStream inStream) {
        super(inStream);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        while (true) {
            int c = super.read(b, off, len);
            if (c != -1) {
                return c;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException iExcptn) {
            }
        }
    }

    public int read() throws IOException {
        while (true) {
            int c = super.read();
            if (c != -1) {
                return c;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException iExcptn) {
            }
        }
    }
}
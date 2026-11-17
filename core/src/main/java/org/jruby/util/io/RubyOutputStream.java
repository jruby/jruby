package org.jruby.util.io;

import org.jruby.Ruby;
import org.jruby.runtime.Helpers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A wrapper for {@link OutputStream} that throws no IOException, instead raising Ruby Exception (unchecked) via the
 * passed-in runtime.
 */
public class RubyOutputStream {
    private final Ruby runtime;
    private final OutputStream wrap;

    public RubyOutputStream(Ruby runtime, OutputStream wrap) {
        this.runtime = runtime;
        this.wrap = wrap;
    }

    public void write(int b) {
        try {
            wrap.write(b);
        } catch (IOException ioe) {
            handle(ioe);
        }
    }

    public void write(byte[] b, int off, int len) {
        try {
            wrap.write(b, off, len);
        } catch (IOException ioe) {
            handle(ioe);
        }
    }

    public void write(byte[] b) {
        try {
            wrap.write(b);
        } catch (IOException ioe) {
            handle(ioe);
        }
    }

    /**
     * Write an int using marshal logic (masking specific ranges of values to avoid collisions)
     * @param value
     */
    public void writeMarshalInt(int value) {
        if (value == 0) {
            write(0);
        } else if (0 < value && value < 123) {
            write(value + 5);
        } else if (-124 < value && value < 0) {
            write((value - 5) & 0xff);
        } else {
            byte[] buf = new byte[4];
            int i = 0;
            for (; i < buf.length; i++) {
                buf[i] = (byte) (value & 0xff);

                value = value >> 8;
                if (value == 0 || value == -1) {
                    break;
                }
            }
            int len = i + 1;
            int b = value < 0 ? -len : len;
            write(b);
            write(buf, 0, i + 1);
        }
    }

    public void flush() {
        try {
            wrap.flush();
        } catch (IOException ioe) {
            handle(ioe);
        }
    }

    public void close() {
        try {
            wrap.close();
        } catch (IOException ioe) {
            handle(ioe);
        }
    }

    private void handle(IOException ioe) {
        throw Helpers.newIOErrorFromException(runtime, ioe);
    }
}

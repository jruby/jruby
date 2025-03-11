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

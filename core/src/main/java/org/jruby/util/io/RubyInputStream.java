package org.jruby.util.io;

import org.jruby.Ruby;
import org.jruby.runtime.Helpers;

import java.io.IOException;
import java.io.InputStream;

public class RubyInputStream {
    private final Ruby runtime;
    private final InputStream wrap;

    public RubyInputStream(Ruby runtime, InputStream wrap) {
        this.runtime = runtime;
        this.wrap = wrap;
    }

    public int read() {
        try {
            return wrap.read();
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public int read(byte[] b) {
        try {
            return wrap.read(b);
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public int read(byte[] b, int off, int len) {
        try {
            return wrap.read(b, off, len);
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public byte[] readAllBytes() {
        try {
            return wrap.readAllBytes();
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public byte[] readNBytes(int len) {
        try {
            return wrap.readNBytes(len);
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public int readNBytes(byte[] b, int off, int len) {
        try {
            return wrap.readNBytes(b, off, len);
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public long skip(long n) {
        try {
            return wrap.skip(n);
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public void skipNBytes(long n) {
        try {
            wrap.skipNBytes(n);
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public int available() {
        try {
            return wrap.available();
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }

    public void close() {
        try {
            wrap.close();
        } catch (IOException e) {
            throw Helpers.newIOErrorFromException(runtime, e);
        }
    }
}

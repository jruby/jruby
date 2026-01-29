package org.jruby.ext.socket;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Abstract interface for Unix socket channels.
 * Allows both JDK and JNR implementations.
 */
public interface RubyUNIXSocketChannel {
    int read(ByteBuffer buffer) throws IOException;
    int write(ByteBuffer buffer) throws IOException;
    void close() throws IOException;
    boolean isOpen();
    boolean isBlocking() throws IOException;
}

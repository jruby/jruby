package org.jruby.ext.socket;

import java.io.IOException;

/**
 * Abstract interface for Unix server socket channels.
 * Allows both JDK and JNR implementations.
 */
public interface RubyUNIXServerChannel {
    /**
     * Accept an incoming connection
     * @return A client socket channel for the accepted connection
     */
    RubyUNIXSocketChannel accept() throws IOException;

    /**
     * Close the server socket
     */
    void close() throws IOException;

    /**
     * Check if the server socket is open
     */
    boolean isOpen();

    /**
     * Get the path this server is bound to
     */
    String getPath();
}

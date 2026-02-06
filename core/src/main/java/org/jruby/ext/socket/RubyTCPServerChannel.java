package org.jruby.ext.socket;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Interface for TCP server socket channels.
 * Supports both plain TCP and SSL/TLS implementations.
 */
public interface RubyTCPServerChannel {
    /**
     * Bind to address and port
     * @param host Host to bind to (null or "0.0.0.0" for all interfaces)
     * @param port Port to bind to
     * @param backlog Maximum queue length for incoming connections
     * @throws IOException on bind failure
     */
    void bind(String host, int port, int backlog) throws IOException;

    /**
     * Accept an incoming connection
     * @return Client socket channel for the accepted connection
     * @throws IOException on accept error
     */
    RubyTCPSocketChannel accept() throws IOException;

    /**
     * Accept with timeout
     * @param timeout Timeout in milliseconds (0 = no timeout)
     * @return Client socket channel, or null on timeout
     * @throws IOException on accept error
     */
    RubyTCPSocketChannel acceptTimeout(int timeout) throws IOException;

    /**
     * Close the server socket
     * @throws IOException on close error
     */
    void close() throws IOException;

    /**
     * Check if server socket is open
     * @return true if open, false otherwise
     */
    boolean isOpen();

    /**
     * Get local address
     * @return Local socket address
     * @throws IOException on error
     */
    InetSocketAddress getLocalAddress() throws IOException;

    /**
     * Set SO_REUSEADDR option
     * @param enabled true to enable, false to disable
     * @throws IOException on error
     */
    void setReuseAddress(boolean enabled) throws IOException;
}

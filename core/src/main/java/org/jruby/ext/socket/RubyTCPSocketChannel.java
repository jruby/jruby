package org.jruby.ext.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

/**
 * Interface for TCP socket channels.
 * Supports both plain TCP and SSL/TLS implementations.
 */
public interface RubyTCPSocketChannel {
    /**
     * Connect to remote host
     * @param host Remote hostname or IP address
     * @param port Remote port number
     * @param timeout Connection timeout in milliseconds (0 = no timeout)
     * @throws IOException on connection failure
     */
    void connect(String host, int port, int timeout) throws IOException;

    /**
     * Read data from socket
     * @param buffer Buffer to read into
     * @return Number of bytes read, -1 on EOF
     * @throws IOException on read error
     */
    int read(ByteBuffer buffer) throws IOException;

    /**
     * Write data to socket
     * @param buffer Buffer to write from
     * @return Number of bytes written
     * @throws IOException on write error
     */
    int write(ByteBuffer buffer) throws IOException;

    /**
     * Close the socket
     * @throws IOException on close error
     */
    void close() throws IOException;

    /**
     * Check if socket is open
     * @return true if open, false otherwise
     */
    boolean isOpen();

    /**
     * Check if socket is connected
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Get remote address
     * @return Remote socket address
     */
    InetSocketAddress getRemoteAddress() throws IOException;

    /**
     * Get local address
     * @return Local socket address
     */
    InetSocketAddress getLocalAddress() throws IOException;

    /**
     * Set read timeout
     * @param timeout Timeout in milliseconds (0 = no timeout)
     * @throws IOException on error
     */
    void setReadTimeout(int timeout) throws IOException;

    /**
     * Set write timeout
     * @param timeout Timeout in milliseconds (0 = no timeout)
     * @throws IOException on error
     */
    void setWriteTimeout(int timeout) throws IOException;

    /**
     * Enable TCP keepalive
     * @param enabled true to enable, false to disable
     * @throws IOException on error
     */
    void setKeepAlive(boolean enabled) throws IOException;

    /**
     * Enable TCP nodelay (disable Nagle's algorithm)
     * @param enabled true to enable, false to disable
     * @throws IOException on error
     */
    void setTcpNoDelay(boolean enabled) throws IOException;
}

package org.jruby.ext.socket;

import java.io.IOException;
import javax.net.ssl.SSLContext;

/**
 * Factory for creating TCP socket channels
 * Supports both plain TCP and SSL/TLS connections
 */
public class TCPSocketChannelFactory {

    /**
     * Create a plain TCP client socket channel
     * @return New TCP socket channel
     * @throws IOException on creation failure
     */
    public static RubyTCPSocketChannel createPlainSocket() throws IOException {
        return new PlainTCPSocketChannel();
    }

    /**
     * Create a plain TCP server socket channel
     * @return New TCP server channel
     * @throws IOException on creation failure
     */
    public static RubyTCPServerChannel createPlainServer() throws IOException {
        return new PlainTCPServerChannel();
    }

    /**
     * Create an SSL/TLS TCP client socket channel
     * @param sslContext SSL context to use
     * @return New SSL socket channel
     * @throws IOException on creation failure
     */
    public static RubyTCPSocketChannel createSSLSocket(SSLContext sslContext) throws IOException {
        if (sslContext == null) {
            // Use default SSL context
            try {
                sslContext = SSLContext.getDefault();
            } catch (Exception e) {
                throw new IOException("Failed to get default SSL context: " + e.getMessage());
            }
        }
        return new SSLTCPSocketChannel(sslContext);
    }

    /**
     * Create an SSL/TLS TCP server socket channel
     * @param sslContext SSL context to use
     * @return New SSL server channel
     * @throws IOException on creation failure
     */
    public static RubyTCPServerChannel createSSLServer(SSLContext sslContext) throws IOException {
        if (sslContext == null) {
            throw new IOException("SSL context required for SSL server");
        }
        return new SSLTCPServerChannel(sslContext);
    }
}

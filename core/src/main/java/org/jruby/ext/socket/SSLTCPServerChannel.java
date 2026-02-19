package org.jruby.ext.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLContext;

/**
 * SSL/TLS TCP server socket implementation
 * Accepts SSL/TLS encrypted connections
 */
public class SSLTCPServerChannel implements RubyTCPServerChannel {
    private ServerSocketChannel serverChannel;
    private SSLContext sslContext;

    public SSLTCPServerChannel(SSLContext sslContext) throws IOException {
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(true);
        this.sslContext = sslContext;
    }

    @Override
    public void bind(String host, int port, int backlog) throws IOException {
        InetSocketAddress addr;

        // Handle bind address
        if (host == null || host.isEmpty() || host.equals("0.0.0.0") || host.equals("*")) {
            addr = new InetSocketAddress(port);
        } else {
            addr = new InetSocketAddress(host, port);
        }

        // Bind with backlog
        serverChannel.bind(addr, backlog);
    }

    @Override
    public RubyTCPSocketChannel accept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(true);

        // Wrap in SSL channel (server mode)
        SSLTCPSocketChannel sslChannel = new SSLTCPSocketChannel(clientChannel, sslContext, false);

        // Perform handshake
        // Note: Handshake will be done on first read/write

        return sslChannel;
    }

    @Override
    public RubyTCPSocketChannel acceptTimeout(int timeout) throws IOException {
        if (timeout <= 0) {
            return accept();
        }

        // TODO: Implement timeout for accept
        serverChannel.configureBlocking(false);
        long startTime = System.currentTimeMillis();

        while (true) {
            SocketChannel clientChannel = serverChannel.accept();
            if (clientChannel != null) {
                clientChannel.configureBlocking(true);
                serverChannel.configureBlocking(true);

                // Wrap in SSL channel
                return new SSLTCPSocketChannel(clientChannel, sslContext, false);
            }

            if (System.currentTimeMillis() - startTime > timeout) {
                serverChannel.configureBlocking(true);
                return null;
            }

            Thread.yield();
        }
    }

    @Override
    public void close() throws IOException {
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }
    }

    @Override
    public boolean isOpen() {
        return serverChannel != null && serverChannel.isOpen();
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) serverChannel.getLocalAddress();
    }

    @Override
    public void setReuseAddress(boolean enabled) throws IOException {
        serverChannel.socket().setReuseAddress(enabled);
    }

    public ServerSocketChannel getServerChannel() {
        return serverChannel;
    }
}

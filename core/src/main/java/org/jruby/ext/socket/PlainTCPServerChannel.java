package org.jruby.ext.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

/**
 * Plain TCP server socket implementation (non-SSL)
 * Uses Java NIO for efficient I/O operations
 */
public class PlainTCPServerChannel implements RubyTCPServerChannel {
    private ServerSocketChannel serverChannel;

    public PlainTCPServerChannel() throws IOException {
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(true);
    }

    @Override
    public void bind(String host, int port, int backlog) throws IOException {
        InetSocketAddress addr;

        // Handle bind address
        if (host == null || host.isEmpty() || host.equals("0.0.0.0") || host.equals("*")) {
            // Bind to all interfaces
            addr = new InetSocketAddress(port);
        } else {
            // Bind to specific interface
            addr = new InetSocketAddress(host, port);
        }

        // Bind with backlog
        serverChannel.bind(addr, backlog);
    }

    @Override
    public RubyTCPSocketChannel accept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(true);
        return new PlainTCPSocketChannel(clientChannel);
    }

    @Override
    public RubyTCPSocketChannel acceptTimeout(int timeout) throws IOException {
        if (timeout <= 0) {
            // No timeout, just accept
            return accept();
        }

        // Accept with timeout
        serverChannel.configureBlocking(false);
        Selector selector = Selector.open();
        try {
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            if (selector.select(timeout) > 0) {
                SocketChannel clientChannel = serverChannel.accept();
                if (clientChannel != null) {
                    clientChannel.configureBlocking(true);
                    serverChannel.configureBlocking(true);
                    return new PlainTCPSocketChannel(clientChannel);
                }
            }

            // Timeout
            serverChannel.configureBlocking(true);
            return null;
        } finally {
            selector.close();
            serverChannel.configureBlocking(true);
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
        SocketAddress addr = serverChannel.getLocalAddress();
        return (addr instanceof InetSocketAddress) ? (InetSocketAddress) addr : null;
    }

    @Override
    public void setReuseAddress(boolean enabled) throws IOException {
        serverChannel.socket().setReuseAddress(enabled);
    }

    /**
     * Get the underlying ServerSocketChannel
     */
    public ServerSocketChannel getServerChannel() {
        return serverChannel;
    }
}

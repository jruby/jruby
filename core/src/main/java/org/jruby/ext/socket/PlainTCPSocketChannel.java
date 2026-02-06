package org.jruby.ext.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

/**
 * Plain TCP socket implementation (non-SSL)
 * Uses Java NIO for efficient I/O operations
 */
public class PlainTCPSocketChannel implements RubyTCPSocketChannel {
    private SocketChannel channel;
    private int readTimeout = 0;
    private int writeTimeout = 0;

    public PlainTCPSocketChannel() throws IOException {
        this.channel = SocketChannel.open();
        this.channel.configureBlocking(true);
    }

    /**
     * Internal constructor for accepted connections
     */
    protected PlainTCPSocketChannel(SocketChannel channel) throws IOException {
        this.channel = channel;
        this.channel.configureBlocking(true);
    }

    @Override
    public void connect(String host, int port, int timeout) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(host, port);

        if (timeout <= 0) {
            // Blocking connect
            channel.connect(addr);
        } else {
            // Non-blocking connect with timeout
            channel.configureBlocking(false);
            channel.connect(addr);

            Selector selector = Selector.open();
            try {
                channel.register(selector, SelectionKey.OP_CONNECT);

                if (selector.select(timeout) > 0) {
                    if (channel.finishConnect()) {
                        channel.configureBlocking(true);
                        return;
                    }
                }

                // Timeout or connection failed
                throw new IOException("Connection timeout after " + timeout + "ms");
            } finally {
                selector.close();
                channel.configureBlocking(true);
            }
        }
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        if (readTimeout <= 0) {
            // Blocking read
            return channel.read(buffer);
        }

        // Read with timeout
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        try {
            channel.register(selector, SelectionKey.OP_READ);

            if (selector.select(readTimeout) > 0) {
                int bytesRead = channel.read(buffer);
                channel.configureBlocking(true);
                return bytesRead;
            }

            // Timeout
            channel.configureBlocking(true);
            throw new IOException("Read timeout after " + readTimeout + "ms");
        } finally {
            selector.close();
            channel.configureBlocking(true);
        }
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        if (writeTimeout <= 0) {
            // Blocking write
            return channel.write(buffer);
        }

        // Write with timeout
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        try {
            channel.register(selector, SelectionKey.OP_WRITE);

            if (selector.select(writeTimeout) > 0) {
                int bytesWritten = channel.write(buffer);
                channel.configureBlocking(true);
                return bytesWritten;
            }

            // Timeout
            channel.configureBlocking(true);
            throw new IOException("Write timeout after " + writeTimeout + "ms");
        } finally {
            selector.close();
            channel.configureBlocking(true);
        }
    }

    @Override
    public void close() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isConnected();
    }

    @Override
    public InetSocketAddress getRemoteAddress() throws IOException {
        SocketAddress addr = channel.getRemoteAddress();
        return (addr instanceof InetSocketAddress) ? (InetSocketAddress) addr : null;
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        SocketAddress addr = channel.getLocalAddress();
        return (addr instanceof InetSocketAddress) ? (InetSocketAddress) addr : null;
    }

    @Override
    public void setReadTimeout(int timeout) throws IOException {
        this.readTimeout = timeout;
    }

    @Override
    public void setWriteTimeout(int timeout) throws IOException {
        this.writeTimeout = timeout;
    }

    @Override
    public void setKeepAlive(boolean enabled) throws IOException {
        channel.socket().setKeepAlive(enabled);
    }

    @Override
    public void setTcpNoDelay(boolean enabled) throws IOException {
        channel.socket().setTcpNoDelay(enabled);
    }

    /**
     * Get the underlying SocketChannel
     */
    public SocketChannel getChannel() {
        return channel;
    }
}

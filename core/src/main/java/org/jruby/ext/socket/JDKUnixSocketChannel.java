package org.jruby.ext.socket;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * JEP-380 implementation (JDK 16+)
 * Uses native Java Unix Domain Socket support introduced in JEP-380
 */
public class JDKUnixSocketChannel implements RubyUNIXSocketChannel {
    private final SocketChannel channel;

    public JDKUnixSocketChannel(String path) throws IOException {
        UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
        this.channel = SocketChannel.open(addr);
        this.channel.configureBlocking(true);
    }

    protected JDKUnixSocketChannel(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        return channel.read(buffer);
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        return channel.write(buffer);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public boolean isBlocking() throws IOException {
        return channel.isBlocking();
    }

    public SocketChannel getChannel() {
        return channel;
    }
}

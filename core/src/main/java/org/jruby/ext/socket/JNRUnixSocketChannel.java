package org.jruby.ext.socket;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * Legacy JNR implementation (fallback for JDK < 16)
 * Uses JNR-UnixSocket library for Unix Domain Socket support
 */
public class JNRUnixSocketChannel implements RubyUNIXSocketChannel {
    private final UnixSocketChannel channel;

    public JNRUnixSocketChannel(String path) throws IOException {
        UnixSocketAddress addr = new UnixSocketAddress(new java.io.File(path));
        this.channel = UnixSocketChannel.open(addr);
        this.channel.configureBlocking(true);
    }

    protected JNRUnixSocketChannel(UnixSocketChannel channel) {
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
    public boolean isBlocking() {
        return channel.isBlocking();
    }

    public UnixSocketChannel getChannel() {
        return channel;
    }
}

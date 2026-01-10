package org.jruby.ext.socket;

import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import java.io.IOException;

/**
 * Legacy JNR server implementation (fallback for JDK < 16)
 * Uses JNR-UnixSocket library for Unix Domain Server Socket support
 */
public class JNRUnixServerChannel implements RubyUNIXServerChannel {
    private final UnixServerSocketChannel serverChannel;
    private final String path;

    public JNRUnixServerChannel(String path) throws IOException {
        this.path = path;
        UnixSocketAddress addr = new UnixSocketAddress(new java.io.File(path));
        this.serverChannel = UnixServerSocketChannel.open();
        this.serverChannel.socket().bind(addr);
        this.serverChannel.configureBlocking(true);
    }

    @Override
    public RubyUNIXSocketChannel accept() throws IOException {
        UnixSocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(true);
        return new JNRUnixSocketChannel(clientChannel);
    }

    @Override
    public void close() throws IOException {
        serverChannel.close();
    }

    @Override
    public boolean isOpen() {
        return serverChannel.isOpen();
    }

    @Override
    public String getPath() {
        return path;
    }

    public UnixServerSocketChannel getServerChannel() {
        return serverChannel;
    }
}

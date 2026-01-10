package org.jruby.ext.socket;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;

/**
 * JEP-380 server implementation (JDK 16+)
 * Uses native Java Unix Domain Server Socket support
 */
public class JDKUnixServerChannel implements RubyUNIXServerChannel {
    private final ServerSocketChannel serverChannel;
    private final String path;

    public JDKUnixServerChannel(String path) throws IOException {
        this.path = path;
        UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
        this.serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        this.serverChannel.bind(addr);
        this.serverChannel.configureBlocking(true);
    }

    @Override
    public RubyUNIXSocketChannel accept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(true);
        return new JDKUnixSocketChannel(clientChannel);
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

    public ServerSocketChannel getServerChannel() {
        return serverChannel;
    }
}

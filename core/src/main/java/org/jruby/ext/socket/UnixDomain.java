package org.jruby.ext.socket;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unix domain sockets on the JDK's own channels (JEP 380). These come from the default selector provider, so
 * java.nio Selectors (and nio4r) can register them, unlike jnr-unixsocket's channels.
 *
 * @author Troy Mallory (CufeHaco) - from the JEP-380 unix socket work in jruby/jruby#9874
 */
public final class UnixDomain {
    static final ProtocolFamily UNIX = StandardProtocolFamily.UNIX;

    private UnixDomain() {}

    /**
     * Whether this is a JDK unix domain socket channel.
     */
    public static boolean isUnixChannel(Channel channel) {
        try {
            if (channel instanceof SocketChannel sc) {
                SocketAddress local = sc.getLocalAddress();
                SocketAddress remote = sc.getRemoteAddress();
                return local instanceof UnixDomainSocketAddress
                        || remote instanceof UnixDomainSocketAddress;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * A connected stream pair, like socketpair(2): bind a server on a temporary path, connect to it, accept, and
     * remove the path again.
     */
    public static SocketChannel[] pair() throws IOException {
        Path tmp = Files.createTempFile("jruby-unixpair-", ".sock");
        Files.deleteIfExists(tmp);
        ServerSocketChannel server = ServerSocketChannel.open(UNIX);
        try {
            server.bind(UnixDomainSocketAddress.of(tmp));
            SocketChannel a = SocketChannel.open(UNIX);
            a.configureBlocking(true);
            a.connect(UnixDomainSocketAddress.of(tmp));
            SocketChannel b = server.accept();
            b.configureBlocking(true);
            return new SocketChannel[] { a, b };
        } finally {
            try { server.close(); } catch (IOException ignored) {}
            Files.deleteIfExists(tmp);
        }
    }
}

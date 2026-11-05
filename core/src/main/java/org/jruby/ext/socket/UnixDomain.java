package org.jruby.ext.socket;

import jnr.unixsocket.UnixSocketChannel;
import org.jruby.util.io.FilenoUtil;

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
     * A connected stream pair, like socketpair(2). JDK channels when possible, so java.nio Selectors can use them,
     * else jnr's socketpair(2): when the temp dir is missing, read-only or too deep for a socket path, when the
     * thread is interrupted, or when the JDK's fd isn't reachable (send_io and SO_KEEPALIVE need it).
     */
    public static SocketChannel[] pair() throws IOException {
        if (RealFd.AVAILABLE) {
            try {
                return jdkPair();
            } catch (IOException e) {
                // fall back below
            }
        }
        return UnixSocketChannel.pair();
    }

    // Probed on first pair() only, not when TCP sockets load this class through isUnixChannel.
    private static final class RealFd {
        static final boolean AVAILABLE = hasRealFd();
    }

    private static boolean hasRealFd() {
        try (SocketChannel probe = SocketChannel.open(UNIX)) {
            return !FilenoUtil.isFake(FilenoUtil.filenoFrom(probe));
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    // Bind a server in a private temp directory, connect to it, accept, and remove the path and directory again.
    private static SocketChannel[] jdkPair() throws IOException {
        Path dir = Files.createTempDirectory("jruby-unixpair");
        Path path = dir.resolve("s");
        ServerSocketChannel server = null;
        SocketChannel a = null;
        try {
            server = ServerSocketChannel.open(UNIX);
            server.bind(UnixDomainSocketAddress.of(path));
            a = SocketChannel.open(UNIX);
            a.connect(UnixDomainSocketAddress.of(path));
            SocketChannel b = server.accept();
            return new SocketChannel[] { a, b };
        } catch (IOException e) {
            closeQuietly(a);
            throw e;
        } finally {
            closeQuietly(server);
            try {
                Files.deleteIfExists(path);
                Files.deleteIfExists(dir);
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(Channel channel) {
        if (channel != null) try { channel.close(); } catch (IOException ignored) {}
    }
}

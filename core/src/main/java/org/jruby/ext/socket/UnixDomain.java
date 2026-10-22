package org.jruby.ext.socket;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.OpenFile;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JEP-380 Unix-domain backend for JRuby's MRI UNIXSocket/UNIXServer.
 *
 * This is the interpreter path for jruby/jruby#9040. It does not introduce a
 * second IO stack. Callers open a JDK {@link SocketChannel} /
 * {@link ServerSocketChannel} and pass that {@link Channel} to
 * {@code RubyUNIXSocket.init_sock}, which already wraps it in {@code OpenFile}.
 *
 * Capability word (boot-once):
 * <pre>
 *   bit 0  JEP-380 classes present (always on JRuby 10 / JDK 21)
 *   bit 1  channel fileno unwraps via SelChImpl (send_io can stay POSIX)
 *   bit 2  socketpair via temp JEP-380 pair
 * </pre>
 *
 * Ancillary messages ({@code send_io}/{@code recv_io}) stay on the existing
 * POSIX {@code sendmsg} path in {@link RubyUNIXSocket}. JDK Unix channels
 * implement {@code sun.nio.ch.SelChImpl}, so {@link FilenoUtil} already
 * recovers a real fd. FFM is the IPC connection side — see UnixDomainNative.
 *
 * @author Troy Mallory (CufeHaco) - JEP-380/FFM unix socket support (jruby/jruby#9040)
 */
public final class UnixDomain {
    public static final int BIT_JEP380 = 1 << 0;
    public static final int BIT_FILENO = 1 << 1;
    public static final int BIT_PAIR   = 1 << 2;

    static final ProtocolFamily UNIX = StandardProtocolFamily.UNIX;

    private static final int MAX_PATH = 103;
    private static final AtomicInteger PAIR_SEQ = new AtomicInteger();

    private static final int BITS;
    static {
        int bits = BIT_JEP380 | BIT_PAIR;
        try {
            SocketChannel probe = SocketChannel.open(UNIX);
            try {
                if (!FilenoUtil.isFake(FilenoUtil.filenoFrom(probe))) {
                    bits |= BIT_FILENO;
                }
            } finally {
                probe.close();
            }
        } catch (Throwable t) {
            // leave FILENO off
        }
        BITS = bits;
    }

    private UnixDomain() {}

    public static int bits() { return BITS; }
    public static boolean hasJep380() { return (BITS & BIT_JEP380) != 0; }
    public static boolean hasNativeFileno() { return (BITS & BIT_FILENO) != 0; }

    public static boolean isUnixChannel(Channel channel) {
        if (channel instanceof UnixFdChannel) return true;
        try {
            if (channel instanceof SocketChannel sc) {
                SocketAddress local = sc.getLocalAddress();
                SocketAddress remote = sc.getRemoteAddress();
                return local instanceof UnixDomainSocketAddress
                        || remote instanceof UnixDomainSocketAddress;
            }
            if (channel instanceof ServerSocketChannel ssc) {
                return ssc.getLocalAddress() instanceof UnixDomainSocketAddress;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    public static String pathOf(SocketAddress addr) {
        if (addr instanceof UnixDomainSocketAddress u) {
            Path p = u.getPath();
            return p == null ? "" : p.toString();
        }
        if (addr instanceof jnr.unixsocket.UnixSocketAddress jnr) {
            String p = jnr.path();
            return p == null ? "" : p;
        }
        return "";
    }

    /**
     * True for both unix address flavors a socket can still carry: JEP-380's
     * {@link UnixDomainSocketAddress}, and JNR's UnixSocketAddress from the two
     * call sites this patch deliberately leaves on JNR --
     * Socket.new(:UNIX, :STREAM) and Socket.socketpair (RubySocket's
     * UnixSocketChannel.open()/pair()). Dropping the JNR case broke
     * remote_address/getpeername/local_address/getsockname on those sockets.
     */
    public static boolean isUnixAddress(SocketAddress addr) {
        return addr instanceof UnixDomainSocketAddress
                || addr instanceof jnr.unixsocket.UnixSocketAddress;
    }

    // Ends of a temporary-path pair (pairViaTempPath): connected but unnamed to Ruby, like the
    // ends of socketpair(2), so both addresses read as "" (MRI). Weak keys: entries go with
    // the channels.
    private static final java.util.Set<Channel> UNNAMED =
            java.util.Collections.newSetFromMap(new org.jruby.util.collections.ConcurrentWeakHashMap<>());

    public static SocketAddress localAddress(Channel channel) throws IOException {
        if (UNNAMED.contains(channel)) return UnixDomainSocketAddress.of("");
        // UnixFdChannel (UNIXSocket.pair/for_fd) is a connected socket with
        // no path -- MRI reports that as ["AF_UNIX", ""], not ENOTCONN, so
        // this must return a real (if empty) address, not null.
        if (channel instanceof UnixFdChannel) return UnixDomainSocketAddress.of("");
        if (channel instanceof NetworkChannel net) return net.getLocalAddress();
        return null;
    }

    public static SocketAddress remoteAddress(Channel channel) throws IOException {
        if (UNNAMED.contains(channel)) return UnixDomainSocketAddress.of("");
        if (channel instanceof UnixFdChannel ufc) {
            // pair()/socketpair()/for_fd on an already-connected fd are all
            // genuinely connected at the kernel level (just unnamed) --
            // getpeername(2) succeeds for those. A raw Socket.new(:UNIX,
            // :DGRAM) that was only bound, never connect()'d, is not --
            // getpeername(2) is the one kernel-verified way to tell these
            // apart, rather than assuming every UnixFdChannel is connected
            // (true for the first two, wrong for the third). Falls back to
            // the old always-connected assumption if FFM/getpeername isn't
            // available at all (native access disabled).
            if (!UnixDomainNative.hasGetpeername()) return UnixDomainSocketAddress.of("");
            try {
                return UnixDomainSocketAddress.of(UnixDomainNative.peerPathOf(ufc.fd()));
            } catch (UnixDomainNative.NativeException ne) {
                if (ne.errno == UnixDomainNative.ENOTCONN) return null;
                return UnixDomainSocketAddress.of(""); // unexpected failure: don't regress prior behavior
            }
        }
        if (channel instanceof SocketChannel sc) return sc.getRemoteAddress();
        return null;
    }

    public static SocketChannel connect(String path) throws IOException {
        UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
        SocketChannel ch = SocketChannel.open(UNIX);
        ch.configureBlocking(true);
        ch.connect(addr);
        return ch;
    }

    public static ServerSocketChannel bind(String path) throws IOException {
        UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
        ServerSocketChannel ch = ServerSocketChannel.open(UNIX);
        ch.configureBlocking(true);
        ch.bind(addr);
        return ch;
    }

/**
 * Interruptible wait until fd is ready for events (UnixDomainNative.POLLIN/POLLOUT), or timeoutMs
 * passes (negative: no limit). poll(2) runs as a RubyThread task, so the thread shows as "sleep"
 * and Thread#kill/raise/Timeout wake it (executeTask's pollThreadEvents raises them). poll(2)
 * can't be woken from Java, so it waits in slices backing off from 1ms to 100ms, the same
 * governor shape as Kestowv's Wave and UnixFdChannel's SCRATCH: readiness is never delayed, an
 * idle wait costs ~10 wakeups/s, and an interrupt is seen within one slice.
 *
 * @return true when the fd became ready, false on timeout
 */
public static boolean waitFd(ThreadContext context, int fd, short events, long timeoutMs) {
    long deadline = timeoutMs < 0 ? Long.MAX_VALUE : System.nanoTime() + timeoutMs * 1_000_000L;
    try {
        Integer rc = context.getThread().executeTask(context, fd, new RubyThread.Task<Integer, Integer>() {
            private volatile boolean woken;

            @Override
            public Integer run(ThreadContext ctx, Integer f) {
                int slice = 1;
                while (!woken) {
                    long left = deadline == Long.MAX_VALUE ? slice : (deadline - System.nanoTime()) / 1_000_000L;
                    int rc = UnixDomainNative.poll(f, events, (int) Math.max(0, Math.min(left, slice)));
                    if (rc != 0 || (deadline != Long.MAX_VALUE && left <= slice)) return rc;
                    slice = Math.min(slice * 2, 100);
                }
                return 0;
            }

            @Override
            public void wakeup(RubyThread thread, Integer f) {
                woken = true;
            }
        });
        return rc != null && rc > 0;
    } catch (InterruptedException ie) {
        return false; // executeTask's own pollThreadEvents raises any pending kill/raise
    }
}

    public static SocketChannel accept(ServerSocketChannel server) throws IOException {
        SocketChannel ch = server.accept();
        if (ch != null) ch.configureBlocking(true);
        return ch;
    }

    /**
     * Connected unnamed pair for UNIXSocket.pair / the IPC stack.
     * FFM socketpair(2) first (no temp inode). JEP-380 temp-path pair if FFM
     * is off. Channel wrap of a raw fd uses jnr fromFD as an adapter only.
     */
    public static Channel[] pair() throws IOException {
        return pair(UnixDomainNative.SOCK_STREAM);
    }

    /**
     * A connected pair of unix sockets of the given type (SOCK_STREAM or
     * SOCK_DGRAM) -- UNIXSocket.pair and Socket.socketpair. Only a stream
     * pair has the JEP-380 temp-path fallback; JEP-380 has no unix datagrams.
     */
    public static Channel[] pair(int type) throws IOException {
        if (type != UnixDomainNative.SOCK_STREAM && !UnixDomainNative.hasPair()) {
            throw new UnsupportedOperationException("a non-stream socketpair needs native access");
        }
        if (UnixDomainNative.hasPair()) {
            int[] fds = null;
            try {
                fds = UnixDomainNative.pair(type);
                return new Channel[] {
                    UnixDomainNative.channelFromFd(fds[0]),
                    UnixDomainNative.channelFromFd(fds[1])
                };
            } catch (Exception e) {
                if (fds != null) {
                    UnixDomainNative.close(fds[0]);
                    UnixDomainNative.close(fds[1]);
                }
                if (type != UnixDomainNative.SOCK_STREAM) throw new IOException(e.getMessage(), e);
            }
        }
        return pairViaTempPath();
    }

    static SocketChannel[] pairViaTempPath() throws IOException {
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
            UNNAMED.add(a);
            UNNAMED.add(b);
            return new SocketChannel[] { a, b };
        } finally {
            try { server.close(); } catch (IOException ignored) {}
            Files.deleteIfExists(tmp);
        }
    }

    public static void checkPath(ThreadContext context, String path) {
        if (path.length() > MAX_PATH) {
            throw org.jruby.api.Error.argumentError(context,
                    "too long unix socket path (max: " + MAX_PATH + "bytes)");
        }
    }

    public static int fileno(Channel channel) {
        return FilenoUtil.filenoFrom(channel);
    }

    /** One received datagram: payload plus sender path ("" if the kernel gave none). */
    public static final class DgramReceived {
        public final RubyString data;
        public final String srcPath;
        DgramReceived(RubyString data, String srcPath) { this.data = data; this.srcPath = srcPath; }
    }

    /**
     * Blocking recvfrom(2) on a raw UnixFdChannel (Socket.new(:UNIX, :DGRAM),
     * via rawSocket()/for_fd -- see RubySocket_unixDgram.java). Shared by
     * RubyUNIXSocket#recvfrom (a for_fd-wrapped raw dgram fd) and
     * RubyUDPSocket.recvfrom (a Socket object itself, when its channel is a
     * UnixFdChannel) so the EAGAIN/waitReadable retry shape -- same as
     * send_io/recv_io -- isn't duplicated between them.
     */
    public static DgramReceived recvFromBlocking(ThreadContext context, OpenFile fptr, int fd, int len) {
        if (len < 0) throw org.jruby.api.Error.argumentError(context, "negative length " + len + " given");
        Ruby runtime = context.runtime;
        UnixDomainNative.RecvFromResult result;
        while (true) {
            try {
                result = UnixDomainContext.blocking(context, fd, "recvfrom",
                        () -> UnixDomainNative.recvFrom(fd, len));
                break;
            } catch (Exception e) {
                int errno = UnixDomainContext.lastErrno(context);
                if (errno == UnixDomainNative.EAGAIN && fptr.waitReadable(context)) continue;
                throw runtime.newErrnoFromInt(errno, "recvfrom(2)");
            }
        }
        RubyString data = runtime.newString(new ByteList(result.data, false));
        return new DgramReceived(data, result.srcPath);
    }

    /**
     * Single-attempt, non-blocking recvfrom(2) on a raw UnixFdChannel.
     * Returns null if it would block (caller decides :wait_readable vs
     * raising, matching #recv_nonblock/#recvfrom_nonblock's exception:
     * false convention) rather than retrying like recvFromBlocking above.
     *
     * UnixFdChannel isn't a SelectableChannel, so none of the
     * configureBlocking(false)-based nonblock machinery elsewhere in this
     * codebase (DatagramChannel, RubyBasicSocket#doReadNonblock) applies to
     * it -- confirmed by testing: doReadNonblock's `if (!(channel
     * instanceof SelectableChannel))` check unconditionally raises EAGAIN
     * for any UnixFdChannel, even when data is genuinely available. Real
     * fix needs the raw fd toggled into actual OS-level O_NONBLOCK around
     * the call (fcntl F_GETFL/F_SETFL), then restored -- the same shape as
     * DatagramChannel#configureBlocking(false)/(oldBlocking), just done by
     * hand since there's no Channel-level API for a plain fd.
     */
    public static DgramReceived recvFromNonBlocking(ThreadContext context, int fd, int len) {
        if (len < 0) throw org.jruby.api.Error.argumentError(context, "negative length " + len + " given");
        Ruby runtime = context.runtime;
        int oldFlags = getFlagsOrRaise(runtime, fd);
        setFlagsOrRaise(runtime, fd, oldFlags | UnixDomainNative.O_NONBLOCK);
        try {
            UnixDomainNative.RecvFromResult result;
            try {
                result = UnixDomainContext.blocking(context, fd, "recvfrom",
                        () -> UnixDomainNative.recvFrom(fd, len));
            } catch (Exception e) {
                int errno = UnixDomainContext.lastErrno(context);
                if (errno == UnixDomainNative.EAGAIN) return null;
                throw runtime.newErrnoFromInt(errno, "recvfrom(2)");
            }
            return new DgramReceived(runtime.newString(new ByteList(result.data, false)), result.srcPath);
        } finally {
            setFlagsOrRaise(runtime, fd, oldFlags);
        }
    }

    private static int getFlagsOrRaise(Ruby runtime, int fd) {
        try {
            return UnixDomainNative.getFlags(fd);
        } catch (UnixDomainNative.NativeException ne) {
            throw runtime.newErrnoFromInt(ne.errno, "fcntl(2)");
        }
    }

    private static void setFlagsOrRaise(Ruby runtime, int fd, int flags) {
        try {
            UnixDomainNative.setFlags(fd, flags);
        } catch (UnixDomainNative.NativeException ne) {
            throw runtime.newErrnoFromInt(ne.errno, "fcntl(2)");
        }
    }
}

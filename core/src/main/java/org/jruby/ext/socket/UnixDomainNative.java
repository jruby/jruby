package org.jruby.ext.socket;

import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * libc bindings for the Unix IPC ops JEP-380 does not provide.
 *
 * JEP-380 owns the Channel. This class owns the connection primitives:
 * socketpair(2), getsockname(2), sendmsg/recvmsg SCM_RIGHTS, close(2), and
 * read(2)/write(2)/shutdown(2) for UnixFdChannel -- the ByteChannel wrapper
 * for fds JEP-380 has no public API to wrap (socketpair/SCM_RIGHTS ends).
 *
 * WHY a facade over a backend: the bindings are FFM (java.lang.foreign), which
 * is final only from Java 22 and a preview API on Java 21, JRuby's minimum.
 * This class compiles on 21 and holds no FFM types. At class init it detects
 * the running Java version once and routes to a backend, the same
 * detect-then-route shape as Kestowv's runtime Detector/Router: Java 22+
 * loads {@code UnixDomainFFM} (built from src/main/java22, only when the build
 * JDK is 22+); anything else, or a backend that fails to load, gets the base
 * {@link Backend}, whose capability bits are all off. Every caller already
 * checks a bit (hasPair, hasScm, hasDgram, hasPoll, ...) before using native
 * code and keeps its non-native behavior otherwise, so Java 21 fails closed
 * the same way a JVM with native access denied does.
 *
 * Boot is Build-Match-Verify-Execute. Probe fails closed without native
 * access. Layout is Linux LP64.
 *
 * @author Troy Mallory (CufeHaco) - JEP-380/FFM unix socket support (jruby/jruby#9040)
 */
public final class UnixDomainNative {
    private static final Logger LOG = LoggerFactory.getLogger(UnixDomainNative.class);

    public static final int BIT_FFM         = 1 << 0;
    public static final int BIT_PAIR        = 1 << 1;
    public static final int BIT_SCM         = 1 << 2;
    public static final int BIT_GETSOCKNAME = 1 << 3;
    public static final int BIT_LINUX_LP64  = 1 << 4;
    public static final int BIT_DGRAM       = 1 << 5;
    public static final int BIT_GETPEERNAME = 1 << 6;

    static final int AF_UNIX     = 1;
    static final int SOCK_STREAM = 1;
    public static final int SOCK_DGRAM = 2;
    static final int SOL_SOCKET  = 1;
    static final int SCM_RIGHTS  = 1;
    static final int SUN_PATH    = 108;
    static final int SO_TYPE     = 3;
    static final int F_GETFL     = 3;
    static final int F_SETFL     = 4;
    static final int O_NONBLOCK  = 04000; // octal, Linux

    public static final int SHUT_RD   = 0;
    public static final int SHUT_WR   = 1;
    public static final int SHUT_RDWR = 2;

    /** EAGAIN == EWOULDBLOCK on Linux. Matches this file's existing
     * Linux-only scope (see linuxLp64()). */
    public static final int EAGAIN = 11;

    /** Real getpeername(2) failure when a socket was bound but never
     * connect()'d -- see UnixDomain#remoteAddress and peerPathOf. */
    public static final int ENOTCONN = 107;

    /** Linux recv/send flag values (bits/socket.h). */
    public static final int MSG_OOB = 0x01, MSG_PEEK = 0x02, MSG_DONTWAIT = 0x40;
    public static final int MSG_TRUNC = 0x20, MSG_CTRUNC = 0x08;
    /** Linux: fds received over SCM_RIGHTS arrive close-on-exec (MRI asks for this too). */
    public static final int MSG_CMSG_CLOEXEC = 0x40000000;

    public static final short POLLIN = 0x1, POLLOUT = 0x4;

    private static final Backend BACKEND = detect();
    private static final int BITS = BACKEND.bits();
    private static final String ERROR = BACKEND.error();

    private static Backend detect() {
        int feature = Runtime.version().feature();
        if (feature < 22) return new Backend("FFM needs Java 22+, running Java " + feature);
        try {
            return (Backend) Class.forName("org.jruby.ext.socket.UnixDomainFFM").getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            LOG.debug("UnixDomainNative FFM backend not loaded: {}", t);
            return new Backend("FFM backend not loaded: " + t);
        }
    }

    private UnixDomainNative() {}

    public static int bits() { return BITS; }
    public static boolean available() { return (BITS & BIT_FFM) != 0; }
    public static boolean hasPair() { return (BITS & BIT_PAIR) != 0; }
    public static boolean hasScm() { return (BITS & BIT_SCM) != 0; }
    public static boolean hasGetsockname() { return (BITS & BIT_GETSOCKNAME) != 0; }
    public static boolean hasDgram() { return (BITS & BIT_DGRAM) != 0; }
    public static boolean hasGetpeername() { return (BITS & BIT_GETPEERNAME) != 0; }
    public static boolean hasSockopt() { return BACKEND.hasSockopt(); }
    public static boolean hasIoctl() { return BACKEND.hasIoctl(); }
    public static boolean hasPoll() { return BACKEND.hasPoll(); }
    public static String probeError() { return ERROR; }

    public static int[] pair() throws NativeException {
        return pair(SOCK_STREAM);
    }

    /** socketpair(AF_UNIX, type, 0): SOCK_STREAM or SOCK_DGRAM. */
    public static int[] pair(int type) throws NativeException {
        require(BIT_PAIR, "socketpair");
        return BACKEND.pair(type);
    }

    /**
     * Raw socket(2)/bind(2)/sendto(2)/recvfrom(2) for Socket.new(:UNIX,
     * :DGRAM) -- JEP-380 only added SocketChannel/ServerSocketChannel
     * support (stream); DatagramChannel.open(ProtocolFamily) rejects UNIX
     * outright, so there is no JEP-380 API for this at all. The fd these
     * produce is wrapped the same way as pair()/for_fd -- via
     * channelFromFd, i.e. a plain UnixFdChannel -- since bind/send/recv
     * dispatch only need Channel.fd(), not stream-vs-dgram-shaped methods.
     */
    public static int rawSocket(int domain, int type, int protocol) throws NativeException {
        require(BIT_DGRAM, "socket");
        return BACKEND.rawSocket(domain, type, protocol);
    }

    public static void bindPath(int fd, String path) throws NativeException {
        require(BIT_DGRAM, "bind");
        BACKEND.bindPath(fd, path);
    }

    public static long sendTo(int fd, byte[] data, int off, int len, String destPath) throws NativeException {
        require(BIT_DGRAM, "sendto");
        return BACKEND.sendTo(fd, data, off, len, destPath);
    }

    /**
     * recv(2) with flags on any socket fd -- including the real fd of a JDK
     * inet channel, for flags (MSG_PEEK, MSG_OOB, ...) java.nio cannot pass.
     * Returns the bytes received (at most len).
     */
    public static byte[] recv(int fd, int len, int flags) throws NativeException {
        require(BIT_DGRAM, "recv");
        return BACKEND.recv(fd, len, flags);
    }

    /** send(2) with flags on any connected socket fd; see {@link #recv}. */
    public static long send(int fd, byte[] data, int off, int len, int flags) throws NativeException {
        require(BIT_DGRAM, "send");
        return BACKEND.send(fd, data, off, len, flags);
    }

    public static final class RecvFromResult {
        public final byte[] data;
        public final String srcPath; // "" if the kernel gave no/empty address
        public RecvFromResult(byte[] data, String srcPath) { this.data = data; this.srcPath = srcPath; }
    }

    /** recvfrom(2) of at most len bytes, with the sender's unix path. */
    public static RecvFromResult recvFrom(int fd, int len) throws NativeException {
        require(BIT_DGRAM, "recvfrom");
        return BACKEND.recvFrom(fd, len);
    }

    /**
     * getsockopt(SOL_SOCKET, SO_TYPE) -- the one cheap, side-effect-free way
     * to tell a real SOCK_DGRAM fd from a SOCK_STREAM one when a caller only
     * has a bare fd (UNIXSocket.for_fd), used by RubyUNIXSocket#recvfrom to
     * decide whether the kernel's recvfrom(2) source address is meaningful
     * (dgram) or should fall back to the existing connected-peer lookup
     * (stream, where AF_UNIX recvfrom does not reliably populate src_addr).
     * Returns -1 (not SOCK_STREAM/SOCK_DGRAM) on any failure -- callers
     * should treat that as "assume stream, keep existing behavior."
     */
    public static int socketType(int fd) {
        if (fd < 0) return -1;
        return BACKEND.socketType(fd);
    }

    /** getsockopt(2) on any socket fd: the raw option bytes, as the kernel returns them. */
    public static byte[] getSockopt(int fd, int level, int optname) throws NativeException {
        return BACKEND.getSockopt(fd, level, optname);
    }

    /** setsockopt(2) on any socket fd with the raw option bytes. */
    public static void setSockopt(int fd, int level, int optname, byte[] data, int off, int len) throws NativeException {
        BACKEND.setSockopt(fd, level, optname, data, off, len);
    }

    /** gethostname(2), or null if unavailable. */
    public static String hostname() {
        return BACKEND.hostname();
    }

    /** poll(2) on one fd: >0 ready (or error/hangup -- let the next syscall report it), 0 timed out. */
    public static int poll(int fd, short events, int timeoutMs) {
        return BACKEND.poll(fd, events, timeoutMs);
    }

    /**
     * ioctl(2) with a buffer argument: data is copied in, the call made with a
     * pointer to it, and the (possibly updated) bytes copied back into data.
     */
    public static int ioctl(int fd, long request, byte[] data, int off, int len) throws NativeException {
        return BACKEND.ioctl(fd, request, data, off, len);
    }

    public static int close(int fd) {
        if (fd < 0) return -1;
        return BACKEND.close(fd);
    }

    /** A channel for a raw socket fd: UnixFdChannel with the FFM backend, else jnr's, as JRuby has always used. */
    public static java.nio.channels.Channel channelFromFd(int fd) {
        if (!available()) return jnr.unixsocket.UnixSocketChannel.fromFD(fd);
        return new UnixFdChannel(fd);
    }

    /**
     * read(2) into dst's remaining space (heap or direct), advancing its
     * position by the bytes read. Returns the count; 0 at EOF.
     */
    public static long read(int fd, ByteBuffer dst) throws NativeException {
        require(BIT_FFM, "read");
        return BACKEND.read(fd, dst);
    }

    /** write(2) of src's remaining bytes (heap or direct), advancing its position. */
    public static long write(int fd, ByteBuffer src) throws NativeException {
        require(BIT_FFM, "write");
        return BACKEND.write(fd, src);
    }

    /**
     * Best-effort, like close(): used to force a thread blocked in read(2)
     * on this fd to return (see UnixFdChannel), not to report success.
     * shutdown(2) is the safe way to do that -- unlike close(2), it doesn't
     * race with fd reuse in another thread, and reliably unblocks a pending
     * recv/read on this exact socket.
     */
    public static int shutdown(int fd, int how) {
        if (fd < 0) return -1;
        return BACKEND.shutdown(fd, how);
    }

    public static String pathOf(int fd) throws NativeException {
        require(BIT_GETSOCKNAME, "getsockname");
        return BACKEND.pathOf(fd);
    }

    /**
     * getpeername(2) -- unlike pathOf/getsockname (always succeeds for any
     * bound-or-not socket), this genuinely fails with ENOTCONN when there is
     * no peer. That's the one reliable, kernel-verified way to tell a really
     * connected UnixFdChannel (pair()/socketpair()/for_fd on an already-
     * connected fd -- unnamed, but a real peer) from a raw Socket.new(:UNIX,
     * :DGRAM) that was only bound, never connected (see
     * UnixDomain#remoteAddress). The errno is captured so the ENOTCONN case
     * is distinguishable from any other failure.
     */
    public static String peerPathOf(int fd) throws NativeException {
        require(BIT_GETPEERNAME, "getpeername");
        return BACKEND.peerPathOf(fd);
    }

    /** fcntl(fd, F_GETFL) -- current file status flags. */
    public static int getFlags(int fd) throws NativeException {
        require(BIT_DGRAM, "fcntl");
        return BACKEND.getFlags(fd);
    }

    /** fcntl(fd, F_SETFL, flags) -- replace file status flags wholesale
     * (caller is responsible for OR-ing/clearing bits against a prior
     * getFlags() result, same contract as DatagramChannel#configureBlocking
     * needing the old value to restore it afterward). */
    public static void setFlags(int fd, int flags) throws NativeException {
        require(BIT_DGRAM, "fcntl");
        BACKEND.setFlags(fd, flags);
    }

    public static int sendIo(int sockFd, int passFd) throws NativeException {
        require(BIT_SCM, "sendmsg");
        return BACKEND.sendIo(sockFd, passFd);
    }

    public static int recvIo(int sockFd) throws NativeException {
        require(BIT_SCM, "recvmsg");
        return BACKEND.recvIo(sockFd, MSG_CMSG_CLOEXEC);
    }

    /** One control message: cmsg_level, cmsg_type and its data bytes. */
    public record Cmsg(int level, int type, byte[] data) {}

    /** recvmsg(2) result: bytes read, sender sockaddr (may be empty), msg_flags, control messages. */
    public record RecvMsgResult(long n, byte[] data, byte[] name, int flags, java.util.List<Cmsg> controls) {}

    public static long sendMsg(int fd, byte[] data, int off, int len, byte[] dest,
                               java.util.List<Cmsg> controls, int flags) throws NativeException {
        require(BIT_SCM, "sendmsg");
        return BACKEND.sendMsg(fd, data, off, len, dest, controls, flags);
    }

    public static RecvMsgResult recvMsg(int fd, int len, int controlLen, int flags) throws NativeException {
        require(BIT_SCM, "recvmsg");
        return BACKEND.recvMsg(fd, len, controlLen, flags | MSG_CMSG_CLOEXEC);
    }

    /**
     * The calling thread's errno as libc sees it now (__errno_location), or 0
     * without native access. Only for calls that did not capture errno
     * themselves; see NativeException#errno.
     */
    public static int lastErrno() {
        return BACKEND.lastErrno();
    }

    static boolean linuxLp64() {
        String os = System.getProperty("os.name", "");
        String arch = System.getProperty("os.arch", "");
        return os.toLowerCase().contains("linux") && (arch.contains("64") || arch.equals("aarch64") || arch.equals("amd64"));
    }

    private static void require(int bit, String op) throws NativeException {
        if ((BITS & bit) == 0) {
            throw new NativeException(op, "FFM " + op + " not available (" +
                    (ERROR == null ? "bit off" : ERROR) + ")");
        }
    }

    /**
     * The native operations, with no native access: every capability bit off,
     * every op unavailable. The FFM backend (src/main/java22, UnixDomainFFM)
     * overrides all of it.
     */
    static class Backend {
        private final String error;

        protected Backend(String error) { this.error = error; }

        int bits() { return 0; }
        String error() { return error; }
        boolean hasSockopt() { return false; }
        boolean hasIoctl() { return false; }
        boolean hasPoll() { return false; }

        final NativeException unavailable(String op) {
            return new NativeException(op, "FFM " + op + " not available (" + (error == null ? "bit off" : error) + ")");
        }

        int[] pair(int type) throws NativeException { throw unavailable("socketpair"); }
        int rawSocket(int domain, int type, int protocol) throws NativeException { throw unavailable("socket"); }
        void bindPath(int fd, String path) throws NativeException { throw unavailable("bind"); }
        long sendTo(int fd, byte[] data, int off, int len, String destPath) throws NativeException { throw unavailable("sendto"); }
        byte[] recv(int fd, int len, int flags) throws NativeException { throw unavailable("recv"); }
        long send(int fd, byte[] data, int off, int len, int flags) throws NativeException { throw unavailable("send"); }
        RecvFromResult recvFrom(int fd, int len) throws NativeException { throw unavailable("recvfrom"); }
        int socketType(int fd) { return -1; }
        byte[] getSockopt(int fd, int level, int optname) throws NativeException { throw unavailable("getsockopt"); }
        void setSockopt(int fd, int level, int optname, byte[] data, int off, int len) throws NativeException { throw unavailable("setsockopt"); }
        String hostname() { return null; }
        int poll(int fd, short events, int timeoutMs) { return 1; } // callers check hasPoll() first
        int ioctl(int fd, long request, byte[] data, int off, int len) throws NativeException { throw unavailable("ioctl"); }
        int close(int fd) { return -1; }
        long read(int fd, ByteBuffer dst) throws NativeException { throw unavailable("read"); }
        long write(int fd, ByteBuffer src) throws NativeException { throw unavailable("write"); }
        int shutdown(int fd, int how) { return -1; }
        String pathOf(int fd) throws NativeException { throw unavailable("getsockname"); }
        String peerPathOf(int fd) throws NativeException { throw unavailable("getpeername"); }
        int getFlags(int fd) throws NativeException { throw unavailable("fcntl"); }
        void setFlags(int fd, int flags) throws NativeException { throw unavailable("fcntl"); }
        int sendIo(int sockFd, int passFd) throws NativeException { throw unavailable("sendmsg"); }
        int recvIo(int sockFd, int flags) throws NativeException { throw unavailable("recvmsg"); }
        long sendMsg(int fd, byte[] data, int off, int len, byte[] dest,
                     java.util.List<Cmsg> controls, int flags) throws NativeException { throw unavailable("sendmsg"); }
        RecvMsgResult recvMsg(int fd, int len, int controlLen, int flags) throws NativeException { throw unavailable("recvmsg"); }
        int lastErrno() { return 0; }
    }

    public static final class NativeException extends Exception {
        /** Reliably-captured errno (Linker.Option.captureCallState), or 0 if none was captured. */
        public final int errno;

        public NativeException(String op, int rc) { super(op + " failed rc=" + rc); this.errno = 0; }
        public NativeException(String op, int rc, int errno) {
            super(op + " failed rc=" + rc + " errno=" + errno);
            this.errno = errno;
        }
        public NativeException(String op, String msg) { super(op + ": " + msg); this.errno = 0; }
        public NativeException(String op, Throwable cause) { super(op + ": " + cause.getMessage(), cause); this.errno = 0; }
    }
}

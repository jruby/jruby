package org.jruby.ext.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.Map;

/**
 * ByteChannel wrapping a raw fd via FFM read(2)/write(2), for the fds
 * JEP-380 has no public API to wrap directly: the two ends of an FFM
 * socketpair(2) (UnixDomain.pair) and fds received over SCM_RIGHTS. The
 * named bind/connect/accept path stays on real JEP-380
 * SocketChannel/ServerSocketChannel throughout -- this class only exists
 * for that narrower gap, replacing what used to be
 * jnr.unixsocket.UnixSocketChannel.fromFD.
 *
 * Interruptibility: extends AbstractInterruptibleChannel, the same public,
 * stable JDK mechanism FileChannel/DatagramChannel use (begin()/end()
 * around the blocking call), so Thread#kill/interrupt works the normal
 * JRuby way -- no java.nio.Selector/SelChImpl, no JDK-internal reflection.
 * implCloseChannel does shutdown(2) before close(2): a bare close(2) races
 * fd reuse if another thread is still blocked in read(2) on this exact fd
 * (a real POSIX gotcha), while shutdown(2) reliably forces that blocked
 * read to return first.
 *
 * The bytes go through UnixDomainNative.read/write(ByteBuffer); the FFM
 * backend copies heap buffers through a per-thread scratch segment (see
 * UnixDomainFFM).
 *
 * @author Troy Mallory (CufeHaco) - JEP-380/FFM unix socket support (jruby/jruby#9040)
 */
public final class UnixFdChannel extends AbstractInterruptibleChannel implements ByteChannel {
    // org.jruby.runtime.Helpers#errnoFromMessage string-matches IOException
    // messages against exactly these literals (the ones the JDK's own
    // native channels happen to throw) to recover an Errno:: class -- it
    // has no other way to see the real errno. Without this, "rescue
    // Errno::EPIPE" silently fails to catch on a pair()/for_fd socket,
    // since UnixDomainNative's own message ("write failed rc=-1 errno=32")
    // doesn't match anything. Linux x86-64 errno values.
    private static final Map<Integer, String> ERRNO_MESSAGES = Map.ofEntries(
            Map.entry(9,   "Bad file descriptor"),       // EBADF
            Map.entry(13,  "Permission denied"),         // EACCES
            Map.entry(28,  "No space left on device"),   // ENOSPC
            Map.entry(32,  "Broken pipe"),                // EPIPE
            Map.entry(89,  "Destination address required"), // EDESTADDRREQ
            Map.entry(90,  "Message too long"),          // EMSGSIZE
            Map.entry(103, "An established connection was aborted by the software in your host machine"), // ECONNABORTED
            Map.entry(104, "Connection reset by peer"),  // ECONNRESET
            Map.entry(107, "Transport endpoint is not connected"), // ENOTCONN
            Map.entry(110, "Operation timed out")        // ETIMEDOUT
    );

    private final int fd;

    // Lazily-computed, then cached for this instance's lifetime: a socket's
    // SOCK_STREAM/SOCK_DGRAM type is fixed at socket(2) creation time and
    // can never change afterward, so re-querying it via getsockopt(SO_TYPE)
    // on every RubyUNIXSocket#recvfrom call (measured ~1.2us/call in
    // isolation -- not huge per call, but pure waste since the answer is
    // always the same) is a repeated syscall for a value that was already
    // known after the first call. Safe as a plain instance field, not a
    // ThreadLocal/shared map: this fd is immutable per instance (final,
    // set once in the constructor), so there's no reopen/rebind path that
    // could make a cached value stale -- same "one fixed key, own slot,
    // compute once" shape as UnixDomainNative's BITS.
    private int cachedSockType = Integer.MIN_VALUE;

    UnixFdChannel(int fd) {
        this.fd = fd;
    }

    public int fd() { return fd; }

    int sockType() {
        if (cachedSockType == Integer.MIN_VALUE) {
            cachedSockType = UnixDomainNative.socketType(fd);
        }
        return cachedSockType;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        boolean completed = false;
        try {
            begin();
            long n = rawRead(fd, dst);
            completed = true;
            return readResult(n);
        } finally {
            end(completed);
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        boolean completed = false;
        try {
            begin();
            long n = rawWrite(fd, src);
            completed = true;
            return writeResult(n);
        } finally {
            end(completed);
        }
    }

    // WHY: this channel isn't a SelectableChannel, so OpenFile#setNonblock
    // can't flip it non-blocking and PosixShim used to answer every
    // read_nonblock on it with EAGAIN without reading at all -- even with
    // data waiting, and at EOF (where MRI raises EOFError). write_nonblock
    // likewise did a plain blocking write. These do one real read(2)/write(2)
    // with the fd temporarily in O_NONBLOCK, the same fcntl toggle
    // UnixDomain#recvFromNonBlocking already uses for recv_nonblock.
    /** Non-blocking read. Channel contract: bytes read, 0 if none available, -1 at EOF. */
    public int readNonBlocking(ByteBuffer dst) throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        int oldFlags = flags();
        setFlags(oldFlags | UnixDomainNative.O_NONBLOCK);
        try {
            return readResult(rawRead(fd, dst));
        } finally {
            setFlags(oldFlags);
        }
    }

    /** Non-blocking write. Channel contract: bytes written, 0 if the socket buffer is full. */
    public int writeNonBlocking(ByteBuffer src) throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        int oldFlags = flags();
        setFlags(oldFlags | UnixDomainNative.O_NONBLOCK);
        try {
            return writeResult(rawWrite(fd, src));
        } finally {
            setFlags(oldFlags);
        }
    }

    private int flags() throws IOException {
        try {
            return UnixDomainNative.getFlags(fd);
        } catch (UnixDomainNative.NativeException e) {
            throw toIOException(e);
        }
    }

    private void setFlags(int flags) throws IOException {
        try {
            UnixDomainNative.setFlags(fd, flags);
        } catch (UnixDomainNative.NativeException e) {
            throw toIOException(e);
        }
    }

    /**
     * rawRead/rawWrite's result for EAGAIN on a non-blocking fd. It needs its
     * own value: read(2)'s 0 already means EOF.
     */
    static final long WOULD_BLOCK = -2;

    /** rawRead result to Channel contract: -1 at EOF, 0 if nothing available yet. */
    static int readResult(long n) {
        if (n == WOULD_BLOCK) return 0;
        return n == 0 ? -1 : (int) n;
    }

    /** rawWrite result to Channel contract: 0 if the socket buffer is full. */
    static int writeResult(long n) {
        return n == WOULD_BLOCK ? 0 : (int) n;
    }

    // WHY static + fd-parameterized, not instance methods on this class: these
    // are the bare read(2)/write(2) mechanics (errno mapping; the backend does
    // the scratch-buffer copy) with NO begin()/end() bracketing
    // of their own -- callers decide whether to bracket them. This class's own
    // read()/write() above do (see this class's header comment: that's what
    // gives pair()/for_fd SCM_RIGHTS fds real Thread#kill/interrupt support,
    // at the documented cost of the channel closing if a genuine interrupt
    // lands mid-call). RubyUNIXSocket's named-socket read/write path
    // (see RubyUNIXSocket_init.java / ChannelFD_jepUnixNativeChannel.java)
    // deliberately calls these WITHOUT begin()/end() instead: a named JEP-380
    // UNIXSocket keeps its fd genuinely blocking (unlike pair()/for_fd's
    // channel, which stays non-blocking-capable via chSelect), and for that fd
    // these are a real blocking read(2)/write(2) libc call with no JDK-level
    // interrupt hook at all -- Thread#kill/Thread#raise/Timeout.timeout cannot
    // abort it (matching jnr.unixsocket.UnixSocketChannel's long-standing,
    // already-shipped behavior), but a genuine interrupt landing mid-call also
    // can never tear the connection down, which AbstractInterruptibleChannel's
    // own contract cannot offer. Measured: ~30% faster round-trip throughput
    // than the select()-based non-blocking default these replace (this repo's
    // kestowv_tier_one.rb benchmark against Kestowv 0.5.6's ipc/channel.rb) --
    // see INTERPRETER.md Landmine 12/13 for the full trade-off and why the two
    // obvious-looking alternatives (making the real SocketChannel itself
    // blocking; wrapping it in this class's own begin()/end()-bracketed
    // read/write) were tried first and rejected, not overlooked.
    static long rawRead(int fd, ByteBuffer dst) throws IOException {
        if (!dst.hasRemaining()) return 0;
        try {
            return UnixDomainNative.read(fd, dst);
        } catch (UnixDomainNative.NativeException e) {
            if (e.errno == UnixDomainNative.EAGAIN) return WOULD_BLOCK;
            throw toIOException(e);
        }
    }

    static long rawWrite(int fd, ByteBuffer src) throws IOException {
        if (!src.hasRemaining()) return 0;
        try {
            return UnixDomainNative.write(fd, src);
        } catch (UnixDomainNative.NativeException e) {
            if (e.errno == UnixDomainNative.EAGAIN) return WOULD_BLOCK;
            throw toIOException(e);
        }
    }

    static IOException toIOException(UnixDomainNative.NativeException e) {
        String mapped = ERRNO_MESSAGES.get(e.errno);
        return new IOException(mapped != null ? mapped : e.getMessage(), e);
    }

    @Override
    protected void implCloseChannel() throws IOException {
        UnixDomainNative.shutdown(fd, UnixDomainNative.SHUT_RDWR);
        UnixDomainNative.close(fd);
    }
}

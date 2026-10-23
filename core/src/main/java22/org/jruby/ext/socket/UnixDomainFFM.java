package org.jruby.ext.socket;

import org.jruby.ext.socket.UnixDomainNative.Cmsg;
import org.jruby.ext.socket.UnixDomainNative.NativeException;
import org.jruby.ext.socket.UnixDomainNative.RecvFromResult;
import org.jruby.ext.socket.UnixDomainNative.RecvMsgResult;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;

import static org.jruby.ext.socket.UnixDomainNative.AF_UNIX;
import static org.jruby.ext.socket.UnixDomainNative.BIT_DGRAM;
import static org.jruby.ext.socket.UnixDomainNative.BIT_FFM;
import static org.jruby.ext.socket.UnixDomainNative.BIT_GETPEERNAME;
import static org.jruby.ext.socket.UnixDomainNative.BIT_GETSOCKNAME;
import static org.jruby.ext.socket.UnixDomainNative.BIT_LINUX_LP64;
import static org.jruby.ext.socket.UnixDomainNative.BIT_PAIR;
import static org.jruby.ext.socket.UnixDomainNative.BIT_SCM;
import static org.jruby.ext.socket.UnixDomainNative.F_GETFL;
import static org.jruby.ext.socket.UnixDomainNative.F_SETFL;
import static org.jruby.ext.socket.UnixDomainNative.SCM_RIGHTS;
import static org.jruby.ext.socket.UnixDomainNative.SOCK_STREAM;
import static org.jruby.ext.socket.UnixDomainNative.SOL_SOCKET;
import static org.jruby.ext.socket.UnixDomainNative.SO_TYPE;
import static org.jruby.ext.socket.UnixDomainNative.SUN_PATH;

/**
 * FFM (JEP-454) backend for {@link UnixDomainNative}: the libc downcalls.
 *
 * Compiled from src/main/java22 only when the build JDK is 22+ (FFM is final
 * there and a preview API on 21), and loaded by UnixDomainNative only when
 * the running JVM is 22+. Nothing outside this class touches FFM types.
 *
 * Boot is Build-Match-Verify-Execute. Probe fails closed without native
 * access. Layout is Linux LP64.
 *
 * @author Troy Mallory (CufeHaco) - JEP-380/FFM unix socket support (jruby/jruby#9040)
 */
final class UnixDomainFFM extends UnixDomainNative.Backend {
    private static final Logger LOG = LoggerFactory.getLogger(UnixDomainFFM.class);

    private static final ValueLayout.OfInt  C_INT  = ValueLayout.JAVA_INT;
    private static final ValueLayout.OfLong C_LONG = ValueLayout.JAVA_LONG;
    private static final ValueLayout.OfByte C_BYTE = ValueLayout.JAVA_BYTE;
    private static final AddressLayout      C_PTR  = ValueLayout.ADDRESS;

    /**
     * errno must be read via Linker.Option.captureCallState, not a second
     * __errno_location() downcall after the fact (see lastErrno()).
     * The JVM stores it immediately after the raw call returns, before any
     * GC-safepoint or other JVM-internal native call (e.g. the free(3)
     * behind an Arena.close()) gets a chance to clobber it.
     */
    private static final StructLayout  CAPTURE_LAYOUT = Linker.Option.captureStateLayout();
    private static final VarHandle     ERRNO_VH = CAPTURE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("errno"));
    private static final Linker.Option CAPTURE_ERRNO = Linker.Option.captureCallState("errno");

    // Reused per carrier thread like SCRATCH -- read/write is the hot path,
    // so this must not be a fresh Arena.allocate per call. Fixed-size and
    // never resized, so ofAuto (GC-reclaimed, no close needed) is fine here,
    // unlike SCRATCH's growable data buffer.
    private static final ThreadLocal<MemorySegment> ERRNO_STATE =
            ThreadLocal.withInitial(() -> Arena.ofAuto().allocate(CAPTURE_LAYOUT));

    private static int errnoOf(MemorySegment captureState) {
        return (int) ERRNO_VH.get(captureState, 0L);
    }

    // WHY a per-thread scratch segment: RubyBasicSocket#recv/#read hand
    // UnixFdChannel a heap ByteBuffer (plain ByteBuffer.allocate, not
    // allocateDirect), so the heap path in read()/write() below is the one
    // real Ruby code takes, not a fallback. A fresh Arena.ofConfined() per call
    // measured ~35% slower than JNR's channel on UNIXSocket.pair ping-pong --
    // SCRATCH reuses one growable native buffer per carrier thread instead.
    // Safe as a plain ThreadLocal (unlike UnixDomainContext's Fiber-sensitive
    // per-ThreadContext state): this buffer is written and consumed within one
    // synchronous call and never observed across a fiber-switch boundary, so
    // which OS thread backs it doesn't matter -- it's a scratch buffer, not
    // Ruby-visible state.
    private static final int SCRATCH_DEFAULT_SIZE = 8192;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    /**
     * This carrier thread's scratch native buffer, at least minSize bytes,
     * for every call here. Call once per syscall and carve slices from it:
     * a later get() may resize, which frees the segment an earlier call
     * returned.
     */
    private static MemorySegment scratch(int minSize) {
        return SCRATCH.get().get(minSize);
    }

    /**
     * Grow-on-demand, shrink-with-governor. Off-heap, so it never shows up
     * as JVM heap pressure -- the growth problem here is unbounded per-
     * thread native memory, not GC, but the fix is the exact same shape as
     * Kestowv Wave's per-core governor: a rare, decisive writer (shrink)
     * gated by a decaying usage signal, a wide hysteresis dead-band so a
     * single small call right after a burst doesn't immediately snap the
     * buffer back down (which would just force the next big call to
     * regrow it -- thrashing), and a floor so it never shrinks below the
     * size that was already fine for ordinary small messages.
     *
     * Without this, one large one-off read/write (e.g. a single big
     * message on an otherwise small-message connection) permanently
     * inflates this carrier thread's native footprint for the rest of the
     * thread's life, even if every later call only ever needs a few KB.
     */
    private static final class Scratch {
        // Never shrink below this -- matches the original fixed default,
        // so ordinary small messages never pay a regrow cost after a
        // single large outlier is done. Same role as Wave's GOV_MIN floor
        // ("JVM stays warm, cores cool" -- here: "don't shrink into a size
        // that's just going to regrow next call").
        private static final int MIN_SIZE = SCRATCH_DEFAULT_SIZE;

        // How many get() calls form one governor "tick" -- checked
        // periodically, not on every call, same as Wave's GOV_INTERVAL
        // being a real time period rather than a continuous check. Purely
        // call-count-based here since this buffer has no wall-clock tick
        // of its own; higher = slower to react but less governor overhead.
        private static final int TICK_CALLS = 64;

        // Wide dead-band before considering a shrink at all: the current
        // size has to be at least 4x recent actual usage. Mirrors
        // GOV_HYSTERESIS -- without a wide margin here, usage sitting
        // right at a boundary would oscillate grow/shrink every tick.
        private static final int HYSTERESIS_RATIO = 4;

        private Arena arena = Arena.ofConfined();
        private MemorySegment segment = arena.allocate(SCRATCH_DEFAULT_SIZE);
        private int recentMax = SCRATCH_DEFAULT_SIZE;
        private int callsThisTick = 0;

        MemorySegment get(int minSize) {
            if (segment.byteSize() < minSize) {
                // Must grow -- this is a correctness requirement for the
                // current call, not a policy decision, so it happens
                // immediately regardless of tick/hysteresis state.
                resize(minSize);
                recentMax = minSize;
                callsThisTick = 0;
                return segment;
            }

            if (minSize > recentMax) recentMax = minSize;

            if (++callsThisTick >= TICK_CALLS) {
                // Halfway back toward observed usage, not an instant snap
                // to recentMax -- same "back off decisively but don't
                // overshoot" shape as Wave's incremental GOV_STEP, so one
                // noisy tick's recentMax can't yank the buffer straight
                // down to a size the very next call would just regrow.
                if ((long) recentMax * HYSTERESIS_RATIO < segment.byteSize()) {
                    int target = Math.max(MIN_SIZE, (int) ((segment.byteSize() + recentMax) / 2));
                    if (target < segment.byteSize()) resize(target);
                }
                recentMax = minSize;
                callsThisTick = 0;
            }

            return segment;
        }

        private void resize(int size) {
            arena.close();
            arena = Arena.ofConfined();
            segment = arena.allocate(size);
        }
    }

    private final int bits;
    private final MethodHandle mhSocketpair, mhClose, mhGetsockname, mhSendmsg, mhRecvmsg;
    private final MethodHandle mhRead, mhWrite, mhShutdown, mhSocket, mhBind, mhSendto, mhRecvfrom;
    private final MethodHandle mhGetsockopt, mhGetsockoptErrno, mhSetsockopt, mhIoctlPtr, mhPoll;
    private final MethodHandle mhGetpeername, mhFcntlGet, mhFcntlSet, mhGethostname, mhErrnoLocation;

    UnixDomainFFM() {
        this(new Probe());
    }

    private UnixDomainFFM(Probe p) {
        super(p.error);
        bits = p.bits;
        mhSocketpair = p.socketpair; mhClose = p.close; mhGetsockname = p.getsockname;
        mhSendmsg = p.sendmsg; mhRecvmsg = p.recvmsg; mhRead = p.read; mhWrite = p.write;
        mhShutdown = p.shutdown; mhSocket = p.rawSocket; mhBind = p.rawBind; mhSendto = p.sendto;
        mhRecvfrom = p.recvfrom; mhGetsockopt = p.getsockopt; mhGetsockoptErrno = p.getsockoptErrno;
        mhSetsockopt = p.setsockopt; mhIoctlPtr = p.ioctlPtr; mhPoll = p.poll; mhGetpeername = p.getpeername;
        mhFcntlGet = p.fcntlGet; mhFcntlSet = p.fcntlSet; mhGethostname = p.gethostname;
        mhErrnoLocation = p.errnoLocation;
    }

    /** Resolves and verifies every downcall once; anything that fails stays null and its bits off. */
    private static final class Probe {
        int bits;
        String error;
        MethodHandle socketpair, close, getsockname, sendmsg, recvmsg, read, write, shutdown;
        MethodHandle rawSocket, rawBind, sendto, recvfrom, getsockopt, getsockoptErrno, setsockopt;
        MethodHandle ioctlPtr, poll, getpeername, fcntlGet, fcntlSet, gethostname, errnoLocation;

        Probe() {
            try {
                if (!UnixDomainNative.linuxLp64()) {
                    throw new UnsupportedOperationException("FFM Unix IPC is Linux LP64 only");
                }
                bits |= BIT_LINUX_LP64;
                Linker linker = Linker.nativeLinker();
                SymbolLookup libc = linker.defaultLookup();
                socketpair = downcall(linker, libc, "socketpair",
                        FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_PTR));
                close = downcall(linker, libc, "close",
                        FunctionDescriptor.of(C_INT, C_INT));
                getsockname = downcall(linker, libc, "getsockname",
                        FunctionDescriptor.of(C_INT, C_INT, C_PTR, C_PTR));
                getpeername = downcall(linker, libc, "getpeername",
                        FunctionDescriptor.of(C_INT, C_INT, C_PTR, C_PTR), CAPTURE_ERRNO);
                sendmsg = downcall(linker, libc, "sendmsg",
                        FunctionDescriptor.of(C_LONG, C_INT, C_PTR, C_INT), CAPTURE_ERRNO);
                recvmsg = downcall(linker, libc, "recvmsg",
                        FunctionDescriptor.of(C_LONG, C_INT, C_PTR, C_INT), CAPTURE_ERRNO);
                read = downcall(linker, libc, "read",
                        FunctionDescriptor.of(C_LONG, C_INT, C_PTR, C_LONG), CAPTURE_ERRNO);
                write = downcall(linker, libc, "write",
                        FunctionDescriptor.of(C_LONG, C_INT, C_PTR, C_LONG), CAPTURE_ERRNO);
                shutdown = downcall(linker, libc, "shutdown",
                        FunctionDescriptor.of(C_INT, C_INT, C_INT));
                bits |= BIT_FFM;
                if (verifyPair(socketpair, close)) {
                    bits |= BIT_PAIR;
                    bits |= BIT_GETSOCKNAME;
                    bits |= BIT_SCM;
                    bits |= BIT_GETPEERNAME;
                }

                // Resolved separately from the block above: these are only
                // needed for raw Socket.new(:UNIX, :DGRAM) support and the
                // other native socket calls, not the UNIXSocket/UNIXServer/
                // pair() path. A hypothetical resolution failure here (exotic
                // libc) must not take down BIT_PAIR/BIT_SCM/BIT_GETSOCKNAME,
                // which are already verified working above.
                try {
                    rawSocket = downcall(linker, libc, "socket",
                            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT));
                    rawBind = downcall(linker, libc, "bind",
                            FunctionDescriptor.of(C_INT, C_INT, C_PTR, C_INT));
                    sendto = downcall(linker, libc, "sendto",
                            FunctionDescriptor.of(C_LONG, C_INT, C_PTR, C_LONG, C_INT, C_PTR, C_INT), CAPTURE_ERRNO);
                    recvfrom = downcall(linker, libc, "recvfrom",
                            FunctionDescriptor.of(C_LONG, C_INT, C_PTR, C_LONG, C_INT, C_PTR, C_PTR), CAPTURE_ERRNO);
                    getsockopt = downcall(linker, libc, "getsockopt",
                            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_PTR, C_PTR));
                    // errno-capturing getsockopt/setsockopt for BasicSocket#getsockopt/
                    // #setsockopt on any socket fd (see getSockopt/setSockopt)
                    getsockoptErrno = downcall(linker, libc, "getsockopt",
                            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_PTR, C_PTR), CAPTURE_ERRNO);
                    setsockopt = downcall(linker, libc, "setsockopt",
                            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_PTR, C_INT), CAPTURE_ERRNO);
                    poll = downcall(linker, libc, "poll",
                            FunctionDescriptor.of(C_INT, C_PTR, C_LONG, C_INT));
                    // ioctl(int, unsigned long, ...) with one pointer argument
                    ioctlPtr = downcall(linker, libc, "ioctl",
                            FunctionDescriptor.of(C_INT, C_INT, C_LONG, C_PTR),
                            Linker.Option.firstVariadicArg(2), CAPTURE_ERRNO);
                    // fcntl(2) is variadic in C; declaring the two concrete
                    // fixed-arity shapes actually used (F_GETFL takes no extra
                    // arg, F_SETFL takes one int) as separate downcall handles
                    // on the same symbol is the standard, correct way to call a
                    // variadic libc function from FFM when only specific,
                    // known-arity call shapes are needed.
                    fcntlGet = downcall(linker, libc, "fcntl",
                            FunctionDescriptor.of(C_INT, C_INT, C_INT));
                    fcntlSet = downcall(linker, libc, "fcntl",
                            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT));
                    bits |= BIT_DGRAM;
                } catch (Throwable t) {
                    rawSocket = null; rawBind = null; sendto = null; recvfrom = null; getsockopt = null;
                    getsockoptErrno = null; setsockopt = null; ioctlPtr = null; poll = null;
                    fcntlGet = null; fcntlSet = null;
                    LOG.debug("UnixDomainFFM dgram probe failed: {}", t);
                }
                gethostname = optionalDowncall(linker, libc, "gethostname", FunctionDescriptor.of(C_INT, C_PTR, C_LONG));
                errnoLocation = optionalDowncall(linker, libc, "__errno_location", FunctionDescriptor.of(C_PTR));
                if (errnoLocation == null) errnoLocation = optionalDowncall(linker, libc, "__error", FunctionDescriptor.of(C_PTR));
            } catch (Throwable t) {
                error = t.getClass().getSimpleName() + ": " + t.getMessage();
                LOG.debug("UnixDomainFFM probe failed: {}", error);
            }
        }
    }

    @Override int bits() { return bits; }
    @Override boolean hasSockopt() { return mhGetsockoptErrno != null && mhSetsockopt != null; }
    @Override boolean hasIoctl() { return mhIoctlPtr != null; }
    @Override boolean hasPoll() { return mhPoll != null; }

    @Override
    int[] pair(int type) throws NativeException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fds = arena.allocate(C_INT, 2);
            int rc = (int) mhSocketpair.invokeExact(AF_UNIX, type, 0, fds);
            if (rc != 0) throw new NativeException("socketpair", rc);
            return new int[] { fds.getAtIndex(C_INT, 0), fds.getAtIndex(C_INT, 1) };
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("socketpair", t);
        }
    }

    @Override
    int rawSocket(int domain, int type, int protocol) throws NativeException {
        try {
            int rc = (int) mhSocket.invokeExact(domain, type, protocol);
            if (rc < 0) throw new NativeException("socket", rc);
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("socket", t);
        }
    }

    @Override
    void bindPath(int fd, String path) throws NativeException {
        try (Arena arena = Arena.ofConfined()) {
            byte[] pathBytes = path.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (pathBytes.length >= SUN_PATH) throw new NativeException("bind", "path too long");
            MemorySegment sun = sockaddrUn(arena, pathBytes);
            int addrlen = 2 + pathBytes.length + 1;
            int rc = (int) mhBind.invokeExact(fd, sun, addrlen);
            if (rc != 0) throw new NativeException("bind", rc);
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("bind", t);
        }
    }

    @Override
    long sendTo(int fd, byte[] data, int off, int len, String destPath) throws NativeException {
        MemorySegment capState = ERRNO_STATE.get();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(Math.max(len, 1));
            MemorySegment.copy(data, off, buf, C_BYTE, 0, len);
            MemorySegment dest = MemorySegment.NULL;
            int destLen = 0;
            if (destPath != null) {
                byte[] pathBytes = destPath.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                if (pathBytes.length >= SUN_PATH) throw new NativeException("sendto", "path too long");
                dest = sockaddrUn(arena, pathBytes);
                destLen = 2 + pathBytes.length + 1;
            }
            long rc = (long) mhSendto.invokeExact(capState, fd, buf, (long) len, 0, dest, destLen);
            if (rc < 0) throw new NativeException("sendto", (int) rc, errnoOf(capState));
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("sendto", t);
        }
    }

    @Override
    byte[] recv(int fd, int len, int flags) throws NativeException {
        MemorySegment capState = ERRNO_STATE.get();
        try {
            MemorySegment buf = scratch(Math.max(len, 1));
            long rc = (long) mhRecvfrom.invokeExact(capState, fd, buf, (long) len, flags, MemorySegment.NULL, MemorySegment.NULL);
            if (rc < 0) throw new NativeException("recv", (int) rc, errnoOf(capState));
            byte[] bytes = new byte[(int) Math.min(rc, len)];
            MemorySegment.copy(buf, C_BYTE, 0, bytes, 0, bytes.length);
            return bytes;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("recv", t);
        }
    }

    @Override
    long send(int fd, byte[] data, int off, int len, int flags) throws NativeException {
        MemorySegment capState = ERRNO_STATE.get();
        try {
            MemorySegment buf = scratch(Math.max(len, 1));
            MemorySegment.copy(data, off, buf, C_BYTE, 0, len);
            long rc = (long) mhSendto.invokeExact(capState, fd, buf, (long) len, flags, MemorySegment.NULL, 0);
            if (rc < 0) throw new NativeException("send", (int) rc, errnoOf(capState));
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("send", t);
        }
    }

    @Override
    RecvFromResult recvFrom(int fd, int len) throws NativeException {
        MemorySegment capState = ERRNO_STATE.get();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(Math.max(len, 1));
            MemorySegment sun = arena.allocate(2 + SUN_PATH);
            MemorySegment addrlen = arena.allocate(C_INT);
            addrlen.set(C_INT, 0, 2 + SUN_PATH);
            long rc = (long) mhRecvfrom.invokeExact(capState, fd, buf, (long) len, 0, sun, addrlen);
            if (rc < 0) throw new NativeException("recvfrom", (int) rc, errnoOf(capState));
            byte[] bytes = new byte[(int) Math.min(rc, len)];
            MemorySegment.copy(buf, C_BYTE, 0, bytes, 0, bytes.length);
            return new RecvFromResult(bytes, parseSockaddrUn(sun, addrlen.get(C_INT, 0)));
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("recvfrom", t);
        }
    }

    @Override
    int socketType(int fd) {
        if (mhGetsockopt == null) return -1;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment val = arena.allocate(C_INT);
            MemorySegment len = arena.allocate(C_INT);
            len.set(C_INT, 0, 4);
            int rc = (int) mhGetsockopt.invokeExact(fd, SOL_SOCKET, SO_TYPE, val, len);
            if (rc != 0) return -1;
            return val.get(C_INT, 0);
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    byte[] getSockopt(int fd, int level, int optname) throws NativeException {
        if (mhGetsockoptErrno == null) throw new NativeException("getsockopt", "FFM getsockopt not available");
        MemorySegment capState = ERRNO_STATE.get();
        try {
            MemorySegment scratch = scratch(256 + 8);
            MemorySegment val = scratch.asSlice(0, 256);
            MemorySegment len = scratch.asSlice(256, 4);
            len.set(C_INT, 0, 256);
            int rc = (int) mhGetsockoptErrno.invokeExact(capState, fd, level, optname, val, len);
            if (rc != 0) throw new NativeException("getsockopt", rc, errnoOf(capState));
            byte[] out = new byte[len.get(C_INT, 0)];
            MemorySegment.copy(val, C_BYTE, 0, out, 0, out.length);
            return out;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("getsockopt", t);
        }
    }

    @Override
    void setSockopt(int fd, int level, int optname, byte[] data, int off, int len) throws NativeException {
        if (mhSetsockopt == null) throw new NativeException("setsockopt", "FFM setsockopt not available");
        MemorySegment capState = ERRNO_STATE.get();
        try {
            MemorySegment val = scratch(Math.max(len, 1));
            MemorySegment.copy(data, off, val, C_BYTE, 0, len);
            int rc = (int) mhSetsockopt.invokeExact(capState, fd, level, optname, val, len);
            if (rc != 0) throw new NativeException("setsockopt", rc, errnoOf(capState));
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("setsockopt", t);
        }
    }

    @Override
    String hostname() {
        if (mhGethostname == null) return null;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(256);
            int rc = (int) mhGethostname.invokeExact(buf, 256L);
            return rc == 0 ? buf.getString(0) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    int poll(int fd, short events, int timeoutMs) {
        try {
            MemorySegment pfd = scratch(8);
            pfd.set(C_INT, 0, fd);
            pfd.set(ValueLayout.JAVA_SHORT, 4, events);
            pfd.set(ValueLayout.JAVA_SHORT, 6, (short) 0);
            int rc = (int) mhPoll.invokeExact(pfd, 1L, timeoutMs);
            return rc < 0 ? 0 : rc; // EINTR etc.: treat as a timed-out slice, the caller re-checks
        } catch (Throwable t) {
            return 1;
        }
    }

    @Override
    int ioctl(int fd, long request, byte[] data, int off, int len) throws NativeException {
        if (mhIoctlPtr == null) throw new NativeException("ioctl", "FFM ioctl not available");
        MemorySegment capState = ERRNO_STATE.get();
        try {
            MemorySegment buf = scratch(Math.max(len, 1));
            MemorySegment.copy(data, off, buf, C_BYTE, 0, len);
            int rc = (int) mhIoctlPtr.invokeExact(capState, fd, request, buf);
            if (rc < 0) throw new NativeException("ioctl", rc, errnoOf(capState));
            MemorySegment.copy(buf, C_BYTE, 0, data, off, len);
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("ioctl", t);
        }
    }

    @Override
    int close(int fd) {
        if (mhClose == null) return -1;
        try {
            return (int) mhClose.invokeExact(fd);
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    long read(int fd, ByteBuffer dst) throws NativeException {
        int remaining = dst.remaining();
        if (remaining == 0) return 0;

        long n;
        if (dst.isDirect()) {
            n = rawRead(fd, MemorySegment.ofBuffer(dst), remaining);
            if (n > 0) dst.position(dst.position() + (int) n);
        } else {
            MemorySegment tmp = scratch(remaining);
            n = rawRead(fd, tmp, remaining);
            if (n > 0) {
                if (dst.hasArray()) {
                    // Copy straight into dst's backing array -- one copy,
                    // not native->byte[]->dst via an intermediate put().
                    int off = dst.arrayOffset() + dst.position();
                    MemorySegment.copy(tmp, C_BYTE, 0, dst.array(), off, (int) n);
                    dst.position(dst.position() + (int) n);
                } else {
                    byte[] bytes = new byte[(int) n];
                    MemorySegment.copy(tmp, C_BYTE, 0, bytes, 0, (int) n);
                    dst.put(bytes);
                }
            }
        }
        return n;
    }

    @Override
    long write(int fd, ByteBuffer src) throws NativeException {
        int remaining = src.remaining();
        if (remaining == 0) return 0;

        long n;
        if (src.isDirect()) {
            n = rawWrite(fd, MemorySegment.ofBuffer(src), remaining);
        } else {
            MemorySegment tmp = scratch(remaining);
            if (src.hasArray()) {
                // Copy straight from src's backing array -- one copy,
                // not src->byte[]->native via an intermediate get().
                int off = src.arrayOffset() + src.position();
                MemorySegment.copy(src.array(), off, tmp, C_BYTE, 0, remaining);
            } else {
                int startPos = src.position();
                byte[] bytes = new byte[remaining];
                src.get(bytes);
                src.position(startPos);
                MemorySegment.copy(bytes, 0, tmp, C_BYTE, 0, remaining);
            }
            n = rawWrite(fd, tmp, remaining);
        }
        if (n > 0) src.position(src.position() + (int) n);
        return n;
    }

    private long rawRead(int fd, MemorySegment buf, long count) throws NativeException {
        MemorySegment capState = ERRNO_STATE.get();
        try {
            long rc = (long) mhRead.invokeExact(capState, fd, buf, count);
            if (rc < 0) throw new NativeException("read", (int) rc, errnoOf(capState));
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("read", t);
        }
    }

    private long rawWrite(int fd, MemorySegment buf, long count) throws NativeException {
        MemorySegment capState = ERRNO_STATE.get();
        try {
            long rc = (long) mhWrite.invokeExact(capState, fd, buf, count);
            if (rc < 0) throw new NativeException("write", (int) rc, errnoOf(capState));
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("write", t);
        }
    }

    @Override
    int shutdown(int fd, int how) {
        if (mhShutdown == null) return -1;
        try {
            return (int) mhShutdown.invokeExact(fd, how);
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    String pathOf(int fd) throws NativeException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sun = arena.allocate(2 + SUN_PATH);
            MemorySegment len = arena.allocate(C_INT);
            len.set(C_INT, 0, 2 + SUN_PATH);
            int rc = (int) mhGetsockname.invokeExact(fd, sun, len);
            if (rc != 0) throw new NativeException("getsockname", rc);
            int n = len.get(C_INT, 0);
            if (n <= 2) return "";
            byte[] bytes = new byte[n - 2];
            MemorySegment.copy(sun, C_BYTE, 2, bytes, 0, bytes.length);
            if (bytes[0] == 0) {
                int end = 1;
                while (end < bytes.length && bytes[end] != 0) end++;
                return "\0" + new String(bytes, 1, end - 1);
            }
            int end = 0;
            while (end < bytes.length && bytes[end] != 0) end++;
            return new String(bytes, 0, end);
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("getsockname", t);
        }
    }

    @Override
    String peerPathOf(int fd) throws NativeException {
        MemorySegment capState = ERRNO_STATE.get();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sun = arena.allocate(2 + SUN_PATH);
            MemorySegment len = arena.allocate(C_INT);
            len.set(C_INT, 0, 2 + SUN_PATH);
            int rc = (int) mhGetpeername.invokeExact(capState, fd, sun, len);
            if (rc != 0) throw new NativeException("getpeername", rc, errnoOf(capState));
            return parseSockaddrUn(sun, len.get(C_INT, 0));
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("getpeername", t);
        }
    }

    @Override
    int getFlags(int fd) throws NativeException {
        try {
            int rc = (int) mhFcntlGet.invokeExact(fd, F_GETFL);
            if (rc < 0) throw new NativeException("fcntl", rc);
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("fcntl", t);
        }
    }

    @Override
    void setFlags(int fd, int flags) throws NativeException {
        try {
            int rc = (int) mhFcntlSet.invokeExact(fd, F_SETFL, flags);
            if (rc < 0) throw new NativeException("fcntl", rc);
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("fcntl", t);
        }
    }

    @Override
    int sendIo(int sockFd, int passFd) throws NativeException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment payload = arena.allocate(1);
            payload.set(C_BYTE, 0, (byte) 0);
            MemorySegment iov = arena.allocate(16);
            iov.set(C_PTR, 0, payload);
            iov.set(C_LONG, 8, 1L);
            int cmsgLen = 16 + 4;
            MemorySegment cmsg = arena.allocate(24);
            cmsg.set(C_LONG, 0, cmsgLen);
            cmsg.set(C_INT, 8, SOL_SOCKET);
            cmsg.set(C_INT, 12, SCM_RIGHTS);
            cmsg.set(C_INT, 16, passFd);
            MemorySegment msg = arena.allocate(56);
            msg.set(C_PTR, 0, MemorySegment.NULL);
            msg.set(C_INT, 8, 0);
            msg.set(C_PTR, 16, iov);
            msg.set(C_LONG, 24, 1L);
            msg.set(C_PTR, 32, cmsg);
            msg.set(C_LONG, 40, cmsgLen);
            msg.set(C_INT, 48, 0);
            MemorySegment capState = arena.allocate(CAPTURE_LAYOUT);
            long rc = (long) mhSendmsg.invokeExact(capState, sockFd, msg, 0);
            if (rc < 0) throw new NativeException("sendmsg", (int) rc, errnoOf(capState));
            return (int) rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("sendmsg", t);
        }
    }

    @Override
    int recvIo(int sockFd, int flags) throws NativeException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment payload = arena.allocate(1);
            MemorySegment iov = arena.allocate(16);
            iov.set(C_PTR, 0, payload);
            iov.set(C_LONG, 8, 1L);
            MemorySegment cmsg = arena.allocate(24);
            MemorySegment msg = arena.allocate(56);
            msg.set(C_PTR, 0, MemorySegment.NULL);
            msg.set(C_INT, 8, 0);
            msg.set(C_PTR, 16, iov);
            msg.set(C_LONG, 24, 1L);
            msg.set(C_PTR, 32, cmsg);
            msg.set(C_LONG, 40, 24L);
            msg.set(C_INT, 48, 0);
            MemorySegment capState = arena.allocate(CAPTURE_LAYOUT);
            long rc = (long) mhRecvmsg.invokeExact(capState, sockFd, msg, flags);
            if (rc < 0) throw new NativeException("recvmsg", (int) rc, errnoOf(capState));
            return cmsg.get(C_INT, 16);
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("recvmsg", t);
        }
    }

    // -- general sendmsg(2)/recvmsg(2), for BasicSocket#sendmsg/#recvmsg --
    // Linux LP64 layouts: struct msghdr is 56 bytes (msg_name@0,
    // msg_namelen@8, msg_iov@16, msg_iovlen@24, msg_control@32,
    // msg_controllen@40, msg_flags@48); struct iovec is 16; struct cmsghdr is
    // a 16-byte header (cmsg_len@0, cmsg_level@8, cmsg_type@12) padded to 8.
    // Every buffer is carved from the thread's scratch segment, one get() per
    // call (see scratch()).
    private static final int MSGHDR = 56, IOVEC = 16, CMSGHDR = 16, NAME_MAX_LEN = 128;

    private static long align8(long n) { return (n + 7) & ~7L; }

    @Override
    long sendMsg(int fd, byte[] data, int off, int len, byte[] dest,
                 java.util.List<Cmsg> controls, int flags) throws NativeException {
        long controlLen = 0;
        for (Cmsg c : controls) controlLen += CMSGHDR + align8(c.data().length);
        int nameLen = dest == null ? 0 : dest.length;
        long total = MSGHDR + IOVEC + align8(nameLen) + controlLen + Math.max(len, 1);
        MemorySegment capState = ERRNO_STATE.get();
        try {
            MemorySegment all = scratch((int) total);
            all.asSlice(0, total).fill((byte) 0);
            MemorySegment msg = all.asSlice(0, MSGHDR);
            MemorySegment iov = all.asSlice(MSGHDR, IOVEC);
            long at = MSGHDR + IOVEC;
            MemorySegment name = all.asSlice(at, Math.max(nameLen, 1)); at += align8(nameLen);
            MemorySegment control = all.asSlice(at, Math.max(controlLen, 1)); at += controlLen;
            MemorySegment buf = all.asSlice(at, Math.max(len, 1));

            MemorySegment.copy(data, off, buf, C_BYTE, 0, len);
            iov.set(C_PTR, 0, buf);
            iov.set(C_LONG, 8, (long) len);
            if (nameLen > 0) MemorySegment.copy(dest, 0, name, C_BYTE, 0, nameLen);
            long c = 0;
            for (Cmsg cm : controls) {
                control.set(C_LONG, c, (long) (CMSGHDR + cm.data().length));
                control.set(C_INT, c + 8, cm.level());
                control.set(C_INT, c + 12, cm.type());
                MemorySegment.copy(cm.data(), 0, control, C_BYTE, c + CMSGHDR, cm.data().length);
                c += CMSGHDR + align8(cm.data().length);
            }
            msg.set(C_PTR, 0, nameLen > 0 ? name : MemorySegment.NULL);
            msg.set(C_INT, 8, nameLen);
            msg.set(C_PTR, 16, iov);
            msg.set(C_LONG, 24, 1L);
            msg.set(C_PTR, 32, controlLen > 0 ? control : MemorySegment.NULL);
            msg.set(C_LONG, 40, controlLen);
            long rc = (long) mhSendmsg.invokeExact(capState, fd, msg, flags);
            if (rc < 0) throw new NativeException("sendmsg", (int) rc, errnoOf(capState));
            return rc;
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("sendmsg", t);
        }
    }

    @Override
    RecvMsgResult recvMsg(int fd, int len, int controlLen, int flags) throws NativeException {
        long total = MSGHDR + IOVEC + NAME_MAX_LEN + align8(controlLen) + Math.max(len, 1);
        MemorySegment capState = ERRNO_STATE.get();
        try {
            MemorySegment all = scratch((int) total);
            MemorySegment msg = all.asSlice(0, MSGHDR);
            MemorySegment iov = all.asSlice(MSGHDR, IOVEC);
            MemorySegment name = all.asSlice(MSGHDR + IOVEC, NAME_MAX_LEN);
            long at = MSGHDR + IOVEC + NAME_MAX_LEN;
            MemorySegment control = all.asSlice(at, Math.max(controlLen, 1)); at += align8(controlLen);
            MemorySegment buf = all.asSlice(at, Math.max(len, 1));

            iov.set(C_PTR, 0, buf);
            iov.set(C_LONG, 8, (long) len);
            msg.set(C_PTR, 0, name);
            msg.set(C_INT, 8, NAME_MAX_LEN);
            msg.set(C_INT, 12, 0);
            msg.set(C_PTR, 16, iov);
            msg.set(C_LONG, 24, 1L);
            msg.set(C_PTR, 32, controlLen > 0 ? control : MemorySegment.NULL);
            msg.set(C_LONG, 40, (long) controlLen);
            msg.set(C_INT, 48, 0);
            long rc = (long) mhRecvmsg.invokeExact(capState, fd, msg, flags);
            if (rc < 0) throw new NativeException("recvmsg", (int) rc, errnoOf(capState));

            // with MSG_TRUNC in flags, rc is the datagram's full length; only len bytes are in buf
            byte[] data = new byte[(int) Math.min(rc, len)];
            MemorySegment.copy(buf, C_BYTE, 0, data, 0, data.length);
            byte[] sender = new byte[Math.min(msg.get(C_INT, 8), NAME_MAX_LEN)];
            MemorySegment.copy(name, C_BYTE, 0, sender, 0, sender.length);

            java.util.List<Cmsg> controls = new java.util.ArrayList<>();
            long used = Math.min(msg.get(C_LONG, 40), controlLen);
            for (long c = 0; c + CMSGHDR <= used; ) {
                long cmsgLen = control.get(C_LONG, c);
                if (cmsgLen < CMSGHDR || c + cmsgLen > used) break;
                byte[] cdata = new byte[(int) (cmsgLen - CMSGHDR)];
                MemorySegment.copy(control, C_BYTE, c + CMSGHDR, cdata, 0, cdata.length);
                controls.add(new Cmsg(control.get(C_INT, c + 8), control.get(C_INT, c + 12), cdata));
                c += align8(cmsgLen);
            }
            return new RecvMsgResult(rc, data, sender, msg.get(C_INT, 48), controls);
        } catch (NativeException e) {
            throw e;
        } catch (Throwable t) {
            throw new NativeException("recvmsg", t);
        }
    }

    /**
     * __errno_location() read after the fact, for the few calls that don't
     * capture errno themselves (UnixDomainContext's fallback). Prefer
     * NativeException#errno: this runs after other JVM-internal native calls
     * may already have clobbered errno.
     */
    @Override
    int lastErrno() {
        if (mhErrnoLocation == null) return 0;
        try {
            MemorySegment ptr = (MemorySegment) mhErrnoLocation.invokeExact();
            return ptr.reinterpret(4).get(C_INT, 0);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static MemorySegment sockaddrUn(Arena arena, byte[] pathBytes) {
        MemorySegment sun = arena.allocate(2 + SUN_PATH); // zero-initialized
        sun.set(C_BYTE, 0, (byte) AF_UNIX);
        sun.set(C_BYTE, 1, (byte) 0);
        MemorySegment.copy(pathBytes, 0, sun, C_BYTE, 2, pathBytes.length);
        return sun;
    }

    // Same parsing convention as pathOf(int): skip the 2-byte sun_family,
    // handle Linux's abstract-namespace leading NUL, stop at the first NUL
    // (or end of the reported length) for a regular path.
    private static String parseSockaddrUn(MemorySegment sun, int len) {
        if (len <= 2) return "";
        byte[] bytes = new byte[len - 2];
        MemorySegment.copy(sun, C_BYTE, 2, bytes, 0, bytes.length);
        if (bytes[0] == 0) {
            int end = 1;
            while (end < bytes.length && bytes[end] != 0) end++;
            return end <= 1 ? "" : "\0" + new String(bytes, 1, end - 1);
        }
        int end = 0;
        while (end < bytes.length && bytes[end] != 0) end++;
        return new String(bytes, 0, end);
    }

    private static boolean verifyPair(MethodHandle socketpair, MethodHandle close) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fds = arena.allocate(C_INT, 2);
            int rc = (int) socketpair.invokeExact(AF_UNIX, SOCK_STREAM, 0, fds);
            if (rc != 0) return false;
            int a = fds.getAtIndex(C_INT, 0);
            int b = fds.getAtIndex(C_INT, 1);
            try {
                return a >= 0 && b >= 0;
            } finally {
                int ignoredA = (int) close.invokeExact(a);
                int ignoredB = (int) close.invokeExact(b);
            }
        }
    }

    private static MethodHandle downcall(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor desc, Linker.Option... options) {
        MemorySegment sym = lookup.find(name).orElseThrow(() -> new IllegalStateException("missing " + name));
        return linker.downcallHandle(sym, desc, options);
    }

    private static MethodHandle optionalDowncall(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor desc) {
        try {
            return downcall(linker, lookup, name, desc);
        } catch (Throwable t) {
            return null;
        }
    }
}

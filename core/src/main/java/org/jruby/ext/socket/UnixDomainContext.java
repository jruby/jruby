package org.jruby.ext.socket;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.ConcurrentWeakHashMap;


/**
 * CRuby-like per-execution-context frame for Unix-domain IPC.
 *
 * CRuby (thread.c):
 *   rb_thread_io_blocking_region(func, data, fd)
 *     th->waiting_fd = fd
 *     BLOCKING_REGION { val = func(); saved_errno = errno; }
 *     th->waiting_fd = -1
 *     RUBY_VM_CHECK_INTS_BLOCKING(th)
 *     errno = saved_errno
 *
 * JRuby already has the execution context: ThreadContext.
 * Fibers swap it. Do not hang this off java.lang.ThreadLocal.
 * That follows the carrier thread and lies when a fiber migrates.
 *
 * @author Troy Mallory (CufeHaco) - JEP-380/FFM unix socket support (jruby/jruby#9040)
 */
public final class UnixDomainContext {
    public static final Object KEY = new Object();

    public static final class Frame {
        public int waitingFd = -1;
        public int lastErrno;
        public String lastOp;
        public int lastRc;
        public long enteredNanos;
    }

    // ConcurrentWeakHashMap (already in JRuby's util.collections, Doug Lea /
    // JSR-166, public domain), not WeakHashMap+synchronized: get() doesn't
    // lock at all, and put/remove only lock their own segment (16 by
    // default), not the whole table -- no single monitor shared by every
    // thread/fiber doing send_io/recv_io/dgram-recvfrom across the process.
    // Still weak-keyed, so a dead ThreadContext's Frame is still reclaimed
    // automatically (via each segment's own ReferenceQueue, drained
    // opportunistically on put/remove) -- same cleanup guarantee the old
    // WeakHashMap had, just without needing a shared lock to get it.
    private static final ConcurrentWeakHashMap<ThreadContext, Frame> FRAMES = new ConcurrentWeakHashMap<>();

    private UnixDomainContext() {}

    public static Frame current(ThreadContext context) {
        // Not FRAMES.computeIfAbsent: ConcurrentWeakHashMap doesn't override
        // it, so it would fall back to Map's default implementation, which
        // is a plain get()-then-put() -- not atomic, and this map is read
        // and written from multiple threads (one thread's blocking() vs.
        // another thread inspecting its waitingFd). get()+putIfAbsent()
        // below are both real, lock-scoped ConcurrentMap operations, so two
        // threads racing to create the first Frame for the same context
        // can't clobber each other's instance.
        Frame frame = FRAMES.get(context);
        if (frame != null) return frame;
        Frame created = new Frame();
        Frame existing = FRAMES.putIfAbsent(context, created);
        return existing != null ? existing : created;
    }

    public static <T> T blocking(ThreadContext context, int fd, String op, Call<T> body) throws Exception {
        Frame frame = current(context);
        int prevFd = frame.waitingFd;
        String prevOp = frame.lastOp;
        frame.waitingFd = fd;
        frame.lastOp = op;
        frame.enteredNanos = System.nanoTime();
        try {
            T value = body.get();
            frame.lastErrno = 0;
            return value;
        } catch (Exception e) {
            // NativeException already carries a reliably-captured errno (via
            // Linker.Option.captureCallState, read the instant the syscall
            // returned) -- prefer it over a fresh __errno_location() call
            // here, which runs after this frame's Arena.close() and other
            // JVM-internal native calls have had a chance to clobber errno.
            frame.lastErrno = (e instanceof UnixDomainNative.NativeException ne && ne.errno != 0)
                    ? ne.errno : captureErrno();
            frame.lastRc = -1;
            throw e;
        } finally {
            frame.waitingFd = prevFd;
            frame.lastOp = prevOp;
            context.pollThreadEvents();
        }
    }

    public static int lastErrno(ThreadContext context) {
        return current(context).lastErrno;
    }

    public static int waitingFd(ThreadContext context) {
        return current(context).waitingFd;
    }

    public static IRubyObject lastErrnoAsFixnum(ThreadContext context) {
        return context.runtime.newFixnum(current(context).lastErrno);
    }

    public static int captureErrno() {
        return UnixDomainNative.lastErrno();
    }

    public static void record(ThreadContext context, String op, int rc) {
        Frame frame = current(context);
        frame.lastOp = op;
        frame.lastRc = rc;
        frame.lastErrno = rc < 0 ? captureErrno() : 0;
    }

    @FunctionalInterface
    public interface Call<T> {
        T get() throws Exception;
    }
}

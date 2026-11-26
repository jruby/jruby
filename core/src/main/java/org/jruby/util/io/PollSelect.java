package org.jruby.util.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.BitSet;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Platform;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyIO;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;

/**
 * IO.select with a zero timeout, done with a single poll(2) over the descriptors.
 *
 * A zero-timeout select cannot block, so it needs none of the selector registration that makes a blocking one
 * interruptible, and that registration dominates its cost. Event loops such as io-event's select backend make
 * this call on every turn.
 */
final class PollSelect {
    public interface LibC {
        int poll(@In @Out ByteBuffer pollfds, int nfds, int timeout);
    }

    private static final LibC LIBC = load();

    // IO.select still works through the selectors without it
    private static LibC load() {
        Platform platform = Platform.getNativePlatform();

        boolean supported = switch (platform.getOS()) {
            case LINUX, ZLINUX, DARWIN, FREEBSD, OPENBSD, NETBSD, DRAGONFLY, SOLARIS -> true;
            default -> false; // AIX, for one, has other <poll.h> values
        };
        if (!supported) return null;

        try {
            return LibraryLoader.create(LibC.class).load(platform.getStandardCLibraryName());
        } catch (Throwable t) {
            return null;
        }
    }

    // <poll.h> on the systems above
    // struct pollfd { int fd; short events; short revents; }
    private static final int POLLFD_SIZE = 8;
    private static final int FD = 0;
    private static final int EVENTS = 4;
    private static final int REVENTS = 6;
    private static final short POLLIN = 0x1;
    private static final short POLLOUT = 0x4;
    private static final short POLLERR = 0x8;
    private static final short POLLHUP = 0x10;
    private static final short POLLNVAL = 0x20;

    // allocating direct memory costs more than the poll itself, so keep a buffer per thread
    private static final ThreadLocal<ByteBuffer> POLLFDS = new ThreadLocal<>();

    /**
     * @return the select result, or null if some IO cannot be polled this way and the caller must select instead
     */
    static IRubyObject selectNow(ThreadContext context, IRubyObject read, IRubyObject write, IRubyObject except) {
        Ruby runtime = context.runtime;
        if (!runtime.getPosix().isNative()) return null;

        if (LIBC == null) return null;

        // copies, since a #to_io below may change the arrays
        IRubyObject[] reads = read.isNil() ? IRubyObject.NULL_ARRAY : read.convertToArray().toJavaArray(context);
        IRubyObject[] writes = write.isNil() ? IRubyObject.NULL_ARRAY : write.convertToArray().toJavaArray(context);

        int readCount = reads.length;
        int writeCount = writes.length;
        int count = readCount + writeCount;
        if (count == 0) return null;

        int[] fds = new int[count];
        boolean[] pending = new boolean[readCount];

        for (int i = 0; i < readCount; i++) {
            OpenFile fptr = TypeConverter.ioGetIO(runtime, reads[i]).getOpenFileChecked();
            int fd = pollable(fptr.fd());
            if (fd < 0) return null;

            fds[i] = fd;
            pending[i] = fptr.READ_DATA_PENDING() || fptr.READ_CHAR_PENDING();
        }

        for (int i = 0; i < writeCount; i++) {
            RubyIO writeIO = TypeConverter.ioGetIO(runtime, writes[i]).GetWriteIO();
            ChannelFD channelFD = writeIO.getOpenFileChecked().fd();
            int fd = pollable(channelFD);
            if (fd < 0) return null;

            // the selector path finishes a pending connect once it is writable, or hands its error to the socket
            if (channelFD.ch instanceof SocketChannel socketChannel && socketChannel.isConnectionPending()) return null;

            fds[readCount + i] = fd;
        }

        // only exceptional conditions go unreported, as with our selectors
        SelectExecutor.checkExcept(runtime, except);

        // filled only now, since a #to_io above may run another IO.select on this thread and its buffer
        ByteBuffer pollfds = pollfds(count);
        for (int i = 0; i < count; i++) setPollfd(pollfds, i, fds[i], i < readCount ? POLLIN : POLLOUT);

        if (LIBC.poll(pollfds, count, 0) < 0) return null; // let the selector path report the error

        // macOS answers a descriptor given twice in only one of its pollfds, so go by descriptor
        BitSet readable = readyFds(pollfds, 0, readCount, POLLIN);
        BitSet writable = readyFds(pollfds, readCount, count, POLLOUT);

        RubyArray<?> readReady = newEmptyArray(context);
        for (int i = 0; i < readCount; i++) {
            if (pending[i] || readable.get(fds[i])) readReady.append(context, reads[i]);
        }

        RubyArray<?> writeReady = newEmptyArray(context);
        for (int i = 0; i < writeCount; i++) {
            if (writable.get(fds[readCount + i])) writeReady.append(context, writes[i]);
        }

        if (readReady.isEmpty() && writeReady.isEmpty()) return context.nil;

        return newArray(context, readReady, writeReady, newEmptyArray(context));
    }

    private static ByteBuffer pollfds(int count) {
        ByteBuffer pollfds = POLLFDS.get();
        if (pollfds == null || pollfds.capacity() < count * POLLFD_SIZE) {
            pollfds = ByteBuffer.allocateDirect(Math.max(count, 8) * POLLFD_SIZE).order(ByteOrder.nativeOrder());
            POLLFDS.set(pollfds);
        }
        return pollfds;
    }

    private static void setPollfd(ByteBuffer pollfds, int i, int fd, short events) {
        int at = i * POLLFD_SIZE;
        pollfds.putInt(at + FD, fd).putShort(at + EVENTS, events).putShort(at + REVENTS, (short) 0);
    }

    // poll(2) reports hangups and errors whatever was asked for, and select(2) counts those descriptors as ready
    private static BitSet readyFds(ByteBuffer pollfds, int from, int to, short events) {
        BitSet ready = new BitSet();
        for (int i = from; i < to; i++) {
            int at = i * POLLFD_SIZE;
            if ((pollfds.getShort(at + REVENTS) & (events | POLLHUP | POLLERR | POLLNVAL)) != 0) {
                ready.set(pollfds.getInt(at + FD));
            }
        }
        return ready;
    }

    // a descriptor poll can watch the way our selectors would, or -1
    private static int pollable(ChannelFD fd) {
        // unselectable channels, such as files, count as always ready, which the selector path handles
        if (fd == null || fd.chSelect == null) return -1;

        return fd.realFileno;
    }
}

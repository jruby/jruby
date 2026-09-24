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

    private static final class LibCHolder {
        static final LibC LIBC = load();

        // IO.select still works through the selectors without it
        private static LibC load() {
            Platform platform = Platform.getNativePlatform();

            switch (platform.getOS()) {
                case LINUX, ZLINUX, DARWIN, FREEBSD, OPENBSD, NETBSD, DRAGONFLY, SOLARIS: break;
                default: return null; // AIX, for one, has other <poll.h> values
            }

            try {
                return LibraryLoader.create(LibC.class).load(platform.getStandardCLibraryName());
            } catch (Throwable t) {
                return null;
            }
        }
    }

    // <poll.h> on the systems above
    private static final int POLLFD_SIZE = 8; // struct pollfd { int fd; short events; short revents; }
    private static final short POLLIN = 0x1;
    private static final short POLLOUT = 0x4;
    private static final short POLLERR = 0x8;
    private static final short POLLHUP = 0x10;
    private static final short POLLNVAL = 0x20;

    // allocating direct memory costs more than the poll itself, so keep a buffer per thread
    private static final ThreadLocal<ByteBuffer> POLLFDS = new ThreadLocal<>();

    private PollSelect() {}

    /**
     * @return the select result, or null if some IO cannot be polled this way and the caller must select instead
     */
    static IRubyObject selectNow(ThreadContext context, IRubyObject read, IRubyObject write, IRubyObject except) {
        Ruby runtime = context.runtime;
        if (!runtime.getPosix().isNative()) return null;

        LibC libc = LibCHolder.LIBC;
        if (libc == null) return null;

        RubyArray<?> readAry = read.isNil() ? null : read.convertToArray();
        RubyArray<?> writeAry = write.isNil() ? null : write.convertToArray();

        int readCount = readAry == null ? 0 : readAry.size();
        int writeCount = writeAry == null ? 0 : writeAry.size();
        int count = readCount + writeCount;
        if (count == 0) return null;

        // only exceptional conditions go unreported, as with our selectors
        if (!except.isNil()) {
            RubyArray<?> exceptAry = except.convertToArray();
            for (int i = 0; i < exceptAry.size(); i++) TypeConverter.ioGetIO(runtime, exceptAry.eltOk(i)).getOpenFileChecked();
        }

        ByteBuffer pollfds = pollfds(count);
        boolean[] pending = new boolean[readCount];

        for (int i = 0; i < readCount; i++) {
            OpenFile fptr = TypeConverter.ioGetIO(runtime, readAry.eltOk(i)).getOpenFileChecked();
            int fd = pollable(fptr.fd());
            if (fd < 0) return null;

            pending[i] = fptr.READ_DATA_PENDING() || fptr.READ_CHAR_PENDING();
            setPollfd(pollfds, i, fd, POLLIN);
        }

        for (int i = 0; i < writeCount; i++) {
            RubyIO writeIO = TypeConverter.ioGetIO(runtime, writeAry.eltOk(i)).GetWriteIO();
            ChannelFD channelFD = writeIO.getOpenFileChecked().fd();
            int fd = pollable(channelFD);
            if (fd < 0) return null;

            // the selector path finishes a pending connect once it is writable, or hands its error to the socket
            if (channelFD.ch instanceof SocketChannel socketChannel && socketChannel.isConnectionPending()) return null;

            setPollfd(pollfds, readCount + i, fd, POLLOUT);
        }

        if (libc.poll(pollfds, count, 0) < 0) return null; // let the selector path report the error

        // macOS answers a descriptor given twice in only one of its pollfds, so go by descriptor
        BitSet readable = readyFds(pollfds, 0, readCount, POLLIN);
        BitSet writable = readyFds(pollfds, readCount, count, POLLOUT);

        RubyArray<?> readReady = newEmptyArray(context);
        for (int i = 0; i < readCount; i++) {
            if (pending[i] || readable.get(pollfds.getInt(i * POLLFD_SIZE))) readReady.append(context, readAry.eltOk(i));
        }

        RubyArray<?> writeReady = newEmptyArray(context);
        for (int i = 0; i < writeCount; i++) {
            if (writable.get(pollfds.getInt((readCount + i) * POLLFD_SIZE))) writeReady.append(context, writeAry.eltOk(i));
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
        pollfds.putInt(i * POLLFD_SIZE, fd).putShort(i * POLLFD_SIZE + 4, events).putShort(i * POLLFD_SIZE + 6, (short) 0);
    }

    // poll(2) reports hangups and errors whatever was asked for, and select(2) counts those descriptors as ready
    private static BitSet readyFds(ByteBuffer pollfds, int from, int to, short events) {
        BitSet ready = new BitSet();
        for (int i = from; i < to; i++) {
            if ((pollfds.getShort(i * POLLFD_SIZE + 6) & (events | POLLHUP | POLLERR | POLLNVAL)) != 0) {
                ready.set(pollfds.getInt(i * POLLFD_SIZE));
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

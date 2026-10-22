package org.jruby.ext.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * The read/write half of a named JEP-380 unix-domain {@code SocketChannel},
 * routed through FFM's raw read(2)/write(2) ({@link UnixFdChannel#rawRead}/
 * {@link UnixFdChannel#rawWrite}) instead of through
 * {@code sun.nio.ch.SocketChannelImpl.read()}/{@code write()}.
 *
 * WHY this exists, and why it's deliberately NOT an
 * {@code AbstractInterruptibleChannel} the way {@link UnixFdChannel} itself
 * is: {@code SocketChannelImpl} already extends
 * {@code AbstractInterruptibleChannel}, so once
 * {@code RubyUNIXSocket#init_sock} (see RubyUNIXSocket_init.java) leaves a
 * named socket's fd genuinely blocking, a real {@code Thread.interrupt()}
 * landing while truly inside its blocking {@code read()} throws
 * {@code ClosedByInterruptException} and closes the channel as a side
 * effect -- confirmed destroying every connection in a jittered
 * Thread#raise/Timeout.timeout stress loop (1000/1000 trials). Wrapping the
 * same real fd in {@link UnixFdChannel} instead doesn't help either: that
 * class extends {@code AbstractInterruptibleChannel} on purpose (its own
 * header comment) to give pair()/for_fd SCM_RIGHTS fds real
 * Thread#kill/interrupt support, with the exact same close-on-interrupt
 * trade-off, confirmed the same way.
 *
 * This class takes the one remaining option: a plain blocking read(2)/
 * write(2) via FFM with NO begin()/end() bracketing, matching
 * jnr.unixsocket.UnixSocketChannel's long-standing, already-shipped
 * semantics -- Thread#kill/Thread#raise/Timeout.timeout cannot abort a
 * blocked call (the thread simply waits for real I/O activity), but nothing
 * can tear the connection down as a side effect of being interrupted either.
 * That trade released ~30% of this path's round-trip throughput back
 * (this repo's kestowv_tier_one.rb benchmark against Kestowv 0.5.6's
 * ipc/channel.rb) by removing OpenFile#waitReadable's EAGAIN-driven
 * select() from the hot path entirely -- see INTERPRETER.md Landmine 13.
 *
 * {@code owner} is the real {@code SocketChannel}: this class delegates
 * {@code isOpen()}/{@code close()} to it rather than tracking its own
 * state, so closing the Ruby-level IO (which closes {@code ch}, not this
 * wrapper) is immediately visible here too. Only {@code chRead}/
 * {@code chWrite} point at this class (see ChannelFD_jepUnixNativeChannel.java);
 * {@code ch} itself -- used for connect/accept/getLocalAddress/
 * getRemoteAddress/shutdownInput/shutdownOutput/close -- stays the real
 * JEP-380 channel throughout, untouched.
 *
 * @author Troy Mallory (CufeHaco) - JEP-380/FFM unix socket support (jruby/jruby#9040)
 */
public final class UnixSocketNativeChannel implements ReadableByteChannel, WritableByteChannel {
    private final Channel owner;
    private final int fd;

    public UnixSocketNativeChannel(Channel owner, int fd) {
        this.owner = owner;
        this.fd = fd;
    }

    @Override
    public boolean isOpen() {
        return owner.isOpen();
    }

    @Override
    public void close() throws IOException {
        owner.close();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        // EAGAIN is possible: this fd is normally blocking, but recv_nonblock/
        // read_nonblock flip the owning SocketChannel (and so the shared fd)
        // non-blocking around the call. It used to surface as IOError.
        return UnixFdChannel.readResult(UnixFdChannel.rawRead(fd, dst));
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen()) throw new ClosedChannelException();
        return UnixFdChannel.writeResult(UnixFdChannel.rawWrite(fd, src));
    }
}

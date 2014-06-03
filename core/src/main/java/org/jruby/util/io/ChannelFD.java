package org.jruby.util.io;

import jnr.enxio.channels.NativeDeviceChannel;
import jnr.enxio.channels.NativeSelectableChannel;
import jnr.posix.POSIX;
import org.jruby.Ruby;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
* Created by headius on 5/24/14.
*/
public class ChannelFD implements Closeable {
    public ChannelFD(Channel fd) {
        this.ch = fd;

        initFileno();
        initChannelTypes();

        refs = new AtomicInteger(1);

        FilenoUtil.registerWrapper(realFileno, this);
        FilenoUtil.registerWrapper(fakeFileno, this);
    }

    private void initFileno() {
        realFileno = FilenoUtil.filenoFrom(ch);
        if (realFileno == -1) {
            fakeFileno = FilenoUtil.getNewFileno();
        } else {
            fakeFileno = -1;
        }
    }

    public ChannelFD dup(POSIX posix) {
        if (realFileno != -1) {
            // real file descriptors, so we can dup directly
            // TODO: investigate how badly this might damage JVM streams (prediction: not badly)
            return new ChannelFD(new NativeDeviceChannel(posix.dup(realFileno)));
        }

        // TODO: not sure how well this combines native and non-native streams
        // simulate dup by copying our channel into a new ChannelFD and incrementing ref count
        Channel ch = this.ch;
        ChannelFD fd = new ChannelFD(ch);
        fd.refs = refs;
        fd.refs.incrementAndGet();

        return fd;
    }

    public int dup2From(POSIX posix, ChannelFD dup2Source) {
        if (dup2Source.realFileno != -1 && realFileno != -1 && chFile == null) {
            // real file descriptors, so we can dup2 directly
            // ...but FileChannel tracks mode on its own, so we can't dup2 into it
            // TODO: investigate how badly this might damage JVM streams (prediction: not badly)
            return posix.dup2(dup2Source.realFileno, realFileno);
        }

        // TODO: not sure how well this combines native and non-native streams
        // simulate dup2 by forcing filedes's channel into filedes2
        this.ch = dup2Source.ch;
        initFileno();
        initChannelTypes();

        this.refs = dup2Source.refs;
        this.refs.incrementAndGet();

        this.currentLock = dup2Source.currentLock;

        return 0;
    }

    public void close() throws IOException {
        // tidy up
        finish(true);
    }

    public int bestFileno() {
        return realFileno == -1 ? fakeFileno : realFileno;
    }

    private void finish(boolean close) throws IOException {
        synchronized (refs) {
            // if refcount is at or below zero, we're no longer valid
            if (refs.get() <= 0) {
                throw new ClosedChannelException();
            }

            // if channel is already closed, we're no longer valid
            if (!ch.isOpen()) {
                throw new ClosedChannelException();
            }

            // otherwise decrement and possibly close as normal
            if (close) {
                int count = refs.decrementAndGet();

                if (count <= 0) {
                    // if we're the last referrer, close the channel
                    try {
                        ch.close();
                    } finally {
                        FilenoUtil.unregisterWrapper(realFileno);
                        FilenoUtil.unregisterWrapper(fakeFileno);
                    }
                }
            }
        }
    }

    private void initChannelTypes() {
        assert realFileno != -1 || fakeFileno != -1 : "initialize filenos before initChannelTypes";
        if (ch instanceof ReadableByteChannel) chRead = (ReadableByteChannel)ch;
        else chRead = null;
        if (ch instanceof WritableByteChannel) chWrite = (WritableByteChannel)ch;
        else chWrite = null;
        if (ch instanceof SeekableByteChannel) chSeek = (SeekableByteChannel)ch;
        else chSeek = null;
        if (ch instanceof SelectableChannel) chSelect = (SelectableChannel)ch;
        else if (realFileno != -1) chSelect = new NativeDeviceChannel(realFileno);
        else chSelect = null;
        if (ch instanceof FileChannel) chFile = (FileChannel)ch;
        else chFile = null;
        if (ch instanceof SocketChannel) chSock = (SocketChannel)ch;
        else chSock = null;
        if (ch instanceof NativeSelectableChannel) chNative = (NativeSelectableChannel)ch;
        else chNative = null;
    }

    public Channel ch;
    public ReadableByteChannel chRead;
    public WritableByteChannel chWrite;
    public SeekableByteChannel chSeek;
    public SelectableChannel chSelect;
    public FileChannel chFile;
    public SocketChannel chSock;
    public NativeSelectableChannel chNative;
    public int realFileno;
    public int fakeFileno;
    private AtomicInteger refs;
    public FileLock currentLock;

    // FIXME shouldn't use static; would interfere with other runtimes in the same JVM
    public static int FIRST_FAKE_FD = 100000;
    protected static final AtomicInteger internalFilenoIndex = new AtomicInteger(FIRST_FAKE_FD);
}

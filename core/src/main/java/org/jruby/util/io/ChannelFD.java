package org.jruby.util.io;

import jnr.constants.platform.Errno;
import jnr.enxio.channels.NativeDeviceChannel;
import jnr.enxio.channels.NativeSelectableChannel;
import jnr.posix.FileStat;
import jnr.posix.POSIX;
import org.jruby.RubySystemCallError;
import org.jruby.exceptions.SystemCallError;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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

import static org.jruby.api.Convert.toInt;

/**
* Created by headius on 5/24/14.
*/
public class ChannelFD implements Closeable {
    public ChannelFD(Channel fd, POSIX posix, FilenoUtil filenoUtil, int flags) {
        assert fd != null;
        this.ch = fd;
        this.posix = posix;
        this.filenoUtil = filenoUtil;
        this.openflags = flags;

        initFileno(false);
        initChannelTypes();

        refs = new AtomicInteger(1);

        filenoUtil.registerWrapper(realFileno, this);
        filenoUtil.registerWrapper(fakeFileno, this);
    }

    public ChannelFD(Channel fd, POSIX posix, FilenoUtil filenoUtil) {
        this(fd, posix, filenoUtil, -1);
    }

    private void initFileno(boolean allocate) {
        realFileno = FilenoUtil.filenoFrom(ch);
        if (Platform.IS_WINDOWS && realFileno == -1 && openflags >= 0) {
            if (allocate) {
                // TODO: ensure we aren't leaking multiple handles for the same channel/handle
                realFileno = filenoUtil.filenoFromHandleIn(ch, openflags); // TODO: ensure these flags are correct for windows
                needsClosing = realFileno != -1;
                maybeHandle = false;
            } else {
                maybeHandle = true;
            }
        }
        if (realFileno == -1) {
            fakeFileno = filenoUtil.getNewFileno();
        } else {
            fakeFileno = -1;
        }
    }

    public ChannelFD dup() {
        if (realFileno != -1 && !Platform.IS_WINDOWS) {
            // real file descriptors, so we can dup directly
            // TODO: investigate how badly this might damage JVM streams (prediction: not badly)
            return new ChannelFD(new NativeDeviceChannel(posix.dup(realFileno)), posix, filenoUtil);
        }

        // TODO: not sure how well this combines native and non-native streams
        // simulate dup by copying our channel into a new ChannelFD and incrementing ref count
        Channel ch = this.ch;
        ChannelFD fd = new ChannelFD(ch, posix, filenoUtil);
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
        initFileno(false);
        initChannelTypes();

        this.refs = dup2Source.refs;
        this.refs.incrementAndGet();

        this.currentLock = dup2Source.currentLock;

        return fakeFileno;
    }

    public void close() throws IOException {
        // tidy up
        finish();
    }

    public int bestFileno() {
        return realFileno == -1 ? fakeFileno : realFileno;
    }

    // lazily create windows resources
    public int bestFileno(boolean forceFileno) {
        if (maybeHandle && forceFileno) {
            initFileno(true);
            assert maybeHandle == false : "lazy handle creation state changed";
            // Hopefully we don't overwrite existing files
            filenoUtil.registerWrapper(realFileno, this);
            filenoUtil.registerWrapper(fakeFileno, this);
        }
        return bestFileno();
    }

    private void finish() throws IOException {
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
            int count = refs.decrementAndGet();

            if (count <= 0) {
                // if we're the last referrer, close the channel
                try {
                    ch.close();
                    if (needsClosing) {
                        filenoUtil.closeFilenoHandle(realFileno);
                    }
                } finally {
                    filenoUtil.unregisterWrapper(realFileno);
                    filenoUtil.unregisterWrapper(fakeFileno);
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
        else chSelect = null;
        if (ch instanceof FileChannel) chFile = (FileChannel)ch;
        else chFile = null;
        if (ch instanceof SocketChannel) chSock = (SocketChannel)ch;
        else chSock = null;
        if (ch instanceof NativeSelectableChannel) chNative = (NativeSelectableChannel)ch;
        else chNative = null;

        if (chNative != null) {
            // we have an ENXIO channel, but need to know if it's a regular file to skip selection
            boolean isFile = false;
            try {
                isFile = posix.fstat(chNative.getFD()).isFile();
            } catch (SystemCallError e) {
                // We check for EPERM due to GH-6129, and because it has only been seen on WSL inotify file descriptors
                // we assume that it means this is not a normal file.
                IRubyObject errno = ((RubySystemCallError) e.getException()).errno();
                var context = errno.getRuntime().getCurrentContext();
                if (errno.isNil() || toInt(context, errno) != Errno.EPERM.intValue()) {
                    // rethrow anything not EPERM
                    throw e;
                }
            }

            if (isFile) {
                chSelect = null;
                isNativeFile = true;
            }
        }
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
    public ThreadLocal<FileLock> currentLock = new ThreadLocal<>();
    private final POSIX posix;
    public boolean isNativeFile = false;
    private final FilenoUtil filenoUtil;
    private boolean needsClosing = false;
    private boolean maybeHandle = false;
    private final int openflags;
}

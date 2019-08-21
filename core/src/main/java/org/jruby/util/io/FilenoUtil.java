package org.jruby.util.io;

import com.headius.backport9.modules.Modules;
import jnr.enxio.channels.NativeSelectableChannel;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.posix.FileStat;
import jnr.posix.HANDLE;
import jnr.posix.JavaLibCHelper;
import jnr.posix.POSIX;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketChannel;

import org.jruby.javasupport.Java;
import org.jruby.platform.Platform;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilities for working with native fileno and Java structures that wrap them.
 */
public class FilenoUtil {
    public FilenoUtil(POSIX posix) {
        this.posix = posix;
        if (posix.isNative() && Platform.IS_WINDOWS) {
            winc = LibraryLoader.create(WinC.class).load("msvcrt");// TODO: string
        } else {
            winc = null;
        }
    }

    public static FileDescriptor getDescriptorFromChannel(Channel channel) {
        if (ReflectiveAccess.SEL_CH_IMPL_GET_FD != null && ReflectiveAccess.SEL_CH_IMPL.isInstance(channel)) {
            // Pipe Source and Sink, Sockets, and other several other selectable channels
            try {
                return (FileDescriptor) ReflectiveAccess.SEL_CH_IMPL_GET_FD.invoke(channel);
            } catch (Exception e) {
                // return bogus below
            }
        } else if (ReflectiveAccess.FILE_CHANNEL_IMPL_FD != null && ReflectiveAccess.FILE_CHANNEL_IMPL.isInstance(channel)) {
            // FileChannels
            try {
                return (FileDescriptor) ReflectiveAccess.FILE_CHANNEL_IMPL_FD.get(channel);
            } catch (Exception e) {
                // return bogus below
            }
        } else if (ReflectiveAccess.FILE_DESCRIPTOR_FD != null) {
            FileDescriptor unixFD = new FileDescriptor();

            // UNIX sockets, from jnr-unixsocket
            try {
                if (channel instanceof UnixSocketChannel) {
                    ReflectiveAccess.FILE_DESCRIPTOR_FD.set(unixFD, ((UnixSocketChannel)channel).getFD());
                    return unixFD;
                } else if (channel instanceof UnixServerSocketChannel) {
                    ReflectiveAccess.FILE_DESCRIPTOR_FD.set(unixFD, ((UnixServerSocketChannel)channel).getFD());
                    return unixFD;
                }
            } catch (Exception e) {
                // return bogus below
            }
        }
        return new FileDescriptor();
    }

    public ChannelFD getWrapperFromFileno(int fileno) {
        ChannelFD fd = filenoMap.get(fileno);

        // This is a hack to get around stale ChannelFD that are closed when a descriptor is reused.
        // It appears to happen for openpty, and in theory could happen for any IO call that produces
        // a new descriptor.
        if (fd != null && !fd.ch.isOpen() && !isFake(fileno)) {
            FileStat stat = posix.allocateStat();
            if (posix.fstat(fileno, stat) >= 0) {
                // found ChannelFD is closed, but actual fileno is open; clear it.
                filenoMap.remove(fileno);
                fd = null;
            }
        }

        return fd;
    }

    public void registerWrapper(int fileno, ChannelFD wrapper) {
        if (fileno == -1) return;
        filenoMap.put(fileno, wrapper);
    }

    public void unregisterWrapper(int fileno) {
        if (fileno == -1) return;
        filenoMap.remove(fileno);
    }

    // Used by testing. See test/jruby/test_io.rb, test_io_copy_stream_does_not_leak_io_like_objects
    public int getNumberOfWrappers() {
        return filenoMap.size();
    }

    public int getNewFileno() {
        return internalFilenoIndex.getAndIncrement();
    }

    public static boolean isFake(int fileno) {
        return fileno < 0 || fileno >= FIRST_FAKE_FD;
    }

    public static int filenoFrom(Channel channel) {
        if (channel instanceof NativeSelectableChannel) {
            return ((NativeSelectableChannel)channel).getFD();
        }

        return getFilenoUsingReflection(channel);
    }

    private static int getFilenoUsingReflection(Channel channel) {
        if (ReflectiveAccess.FILE_DESCRIPTOR_FD != null) {
            return filenoFrom(getDescriptorFromChannel(channel));
        }
        return -1;
    }

    public static int filenoFrom(FileDescriptor fd) {
        if (fd.valid()) {
            try {
                return (Integer) ReflectiveAccess.FILE_DESCRIPTOR_FD.get(fd);
            } catch (Exception e) {
                // failed to get
            }
        }

        return -1;
    }

    public static HANDLE handleFrom(Channel channel) {
        if (channel instanceof NativeSelectableChannel) {
            return HANDLE.valueOf(((NativeSelectableChannel)channel).getFD()); // TODO: this is an int. Do windows handles ever grow larger?
        }

        return getHandleUsingReflection(channel);
    }

    private static HANDLE getHandleUsingReflection(Channel channel) {
        if (ReflectiveAccess.FILE_DESCRIPTOR_FD != null) {
            return JavaLibCHelper.gethandle(getDescriptorFromChannel(channel));
        }
        return HANDLE.valueOf(-1);
    }

    public int filenoFromHandleIn(Channel channel, int flags) {
        if (winc == null)
            return -1;
        HANDLE hndl = handleFrom(channel);
        if (!hndl.isValid())
            return -1;
        return winc._open_osfhandle(hndl.toPointer(), flags); // TODO: don't re-open this handle ever again, or we start to leak?
    }

    public int closeFilenoHandle(int fd) {
        if (fd != -1)
            return winc._close(fd);// TODO: error handling
        return -1;
    }

    public static interface WinC {
        int _open_osfhandle(Pointer hndl, int flgs);
        int _close(int fd);
    }

    public static final int FIRST_FAKE_FD = 100000;
    protected final AtomicInteger internalFilenoIndex = new AtomicInteger(FIRST_FAKE_FD);
    private final NonBlockingHashMapLong<ChannelFD> filenoMap = new NonBlockingHashMapLong<ChannelFD>();
    private final POSIX posix;
    private final WinC winc;

    static final Logger LOG = LoggerFactory.getLogger(FilenoUtil.class);

    private static class ReflectiveAccess {
        static {
            // open package "sun.nio.ch" (from java.base) for org.jruby.dist module
            try {
                Modules.addOpens(FileDescriptor.class, "sun.nio.ch", ReflectiveAccess.class);
            } catch (RuntimeException re) {
                // Warn users since we don't currently handle half-native process control.
                LOG.warn("Native subprocess control requires open access to sun.nio.ch\n" +
                        "Pass '--add-opens java.base/sun.nio.ch=org.jruby.dist' or '=org.jruby.core' to enable.");
            }

            Method getFD;
            Class selChImpl;
            try {
                selChImpl = Class.forName("sun.nio.ch.SelChImpl");
                try {
                    getFD = selChImpl.getMethod("getFD");
                    if (!Java.trySetAccessible(getFD)) {
                        getFD = null;
                    }
                } catch (Exception e) {
                    getFD = null;
                }
            } catch (Exception e) {
                selChImpl = null;
                getFD = null;
            }
            SEL_CH_IMPL = selChImpl;
            SEL_CH_IMPL_GET_FD = getFD;

            Field fd;
            Class fileChannelImpl;
            try {
                fileChannelImpl = Class.forName("sun.nio.ch.FileChannelImpl");
                try {
                    fd = fileChannelImpl.getDeclaredField("fd");
                    if (!Java.trySetAccessible(fd)) {
                        fd = null;
                    }
                } catch (Exception e) {
                    fd = null;
                }
            } catch (Exception e) {
                fileChannelImpl = null;
                fd = null;
            }
            FILE_CHANNEL_IMPL = fileChannelImpl;
            FILE_CHANNEL_IMPL_FD = fd;

            Field ffd;
            try {
                ffd = FileDescriptor.class.getDeclaredField("fd");
                if (!Java.trySetAccessible(ffd)) {
                    ffd = null;
                }
            } catch (Exception e) {
                ffd = null;
            }
            FILE_DESCRIPTOR_FD = ffd;
        }

        private static final Class SEL_CH_IMPL;
        private static final Method SEL_CH_IMPL_GET_FD;
        private static final Class FILE_CHANNEL_IMPL;
        private static final Field FILE_CHANNEL_IMPL_FD;
        private static final Field FILE_DESCRIPTOR_FD;
    }
}

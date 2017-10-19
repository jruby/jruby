package org.jruby.util.io;

import com.headius.modulator.Modulator;
import jnr.enxio.channels.NativeDeviceChannel;
import jnr.enxio.channels.NativeSocketChannel;
import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketChannel;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilities for working with native fileno and Java structures that wrap them.
 */
public class FilenoUtil {
    public FilenoUtil(POSIX posix) {
        this.posix = posix;
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
        if (channel instanceof NativeDeviceChannel) {
            return ((NativeDeviceChannel)channel).getFD();
        }

        if (channel instanceof NativeSocketChannel) {
            return ((NativeSocketChannel)channel).getFD();
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

    public static final int FIRST_FAKE_FD = 100000;
    protected final AtomicInteger internalFilenoIndex = new AtomicInteger(FIRST_FAKE_FD);
    private final Map<Integer, ChannelFD> filenoMap = new ConcurrentHashMap<Integer, ChannelFD>();
    private final POSIX posix;

    private static class ReflectiveAccess {
        static {
            Method getFD;
            Class selChImpl;
            try {
                selChImpl = Class.forName("sun.nio.ch.SelChImpl");
                try {
                    getFD = selChImpl.getMethod("getFD");
                    if (!Modulator.trySetAccessible(getFD)) {
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
                    if (!Modulator.trySetAccessible(fd)) {
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
                if (!Modulator.trySetAccessible(ffd)) {
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

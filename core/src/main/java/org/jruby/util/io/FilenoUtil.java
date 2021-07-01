package org.jruby.util.io;

import com.headius.backport9.modules.Module;
import com.headius.backport9.modules.Modules;
import jnr.constants.platform.Fcntl;
import jnr.enxio.channels.NativeSelectableChannel;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.posix.FileStat;
import jnr.posix.HANDLE;
import jnr.posix.JavaLibCHelper;
import jnr.posix.POSIX;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketChannel;

import org.jruby.javasupport.JavaUtil;
import org.jruby.platform.Platform;
import org.jruby.runtime.Helpers;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static java.lang.invoke.MethodType.methodType;

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
        if (ReflectiveAccess.SEL_CH_IMPL_GET_FD_HANDLE != null && ReflectiveAccess.SEL_CH_IMPL.test(channel)) {
            // Pipe Source and Sink, Sockets, and other several other selectable channels
            try {
                return ReflectiveAccess.SEL_CH_IMPL_GET_FD.apply(channel);
            } catch (Exception e) {
                // return bogus below
            }
        } else if (ReflectiveAccess.FILE_CHANNEL_IMPL_GET_FD_HANDLE != null && ReflectiveAccess.FILE_CHANNEL_IMPL.test(channel)) {
            // FileChannels
            try {
                return ReflectiveAccess.FILE_CHANNEL_IMPL_GET_FD.apply(channel);
            } catch (Exception e) {
                // return bogus below
            }
        } else if (ReflectiveAccess.FILE_DESCRIPTOR_SET_FILENO_HANDLE != null) {
            FileDescriptor unixFD = new FileDescriptor();

            // UNIX sockets, from jnr-unixsocket
            try {
                if (channel instanceof UnixSocketChannel) {
                    ReflectiveAccess.FILE_DESCRIPTOR_SET_FILENO.accept(unixFD, ((UnixSocketChannel)channel).getFD());
                    return unixFD;
                } else if (channel instanceof UnixServerSocketChannel) {
                    ReflectiveAccess.FILE_DESCRIPTOR_SET_FILENO.accept(unixFD, ((UnixServerSocketChannel)channel).getFD());
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
            int ret;

            if (Platform.IS_WINDOWS) {
                // no fcntl on Windows
                FileStat stat = posix.allocateStat();
                ret = posix.fstat(fileno, stat);
            } else {
                ret = posix.fcntl(fileno, Fcntl.F_GETFD);
            }

            if (ret >= 0) {
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
        if (ReflectiveAccess.FILE_DESCRIPTOR_GET_FILENO_HANDLE != null) {
            return filenoFrom(getDescriptorFromChannel(channel));
        }
        return -1;
    }

    public static int filenoFrom(FileDescriptor fd) {
        if (fd.valid()) {
            try {
                return ReflectiveAccess.FILE_DESCRIPTOR_GET_FILENO.applyAsInt(fd);
            } catch (Exception e) {
                // failed to get
            }
        }

        return -1;
    }

    private static HANDLE handleFrom(Channel channel) {
        if (channel instanceof NativeSelectableChannel) {
            return HANDLE.valueOf(((NativeSelectableChannel)channel).getFD()); // TODO: this is an int. Do windows handles ever grow larger?
        }

        return getHandleUsingReflection(channel);
    }

    private static HANDLE getHandleUsingReflection(Channel channel) {
        if (ReflectiveAccess.FILE_DESCRIPTOR_GET_FILENO_HANDLE != null) {
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
        private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        private static final Predicate<Object> SEL_CH_IMPL;
        private static final MethodHandle SEL_CH_IMPL_GET_FD_HANDLE;
        private static final Function<Object, FileDescriptor> SEL_CH_IMPL_GET_FD = ReflectiveAccess::selChImplGetFD;
        private static final Predicate<Object> FILE_CHANNEL_IMPL;
        private static final MethodHandle FILE_CHANNEL_IMPL_GET_FD_HANDLE;
        private static final Function<Object, FileDescriptor> FILE_CHANNEL_IMPL_GET_FD = ReflectiveAccess::fileChannelImplGetFD;
        private static final MethodHandle FILE_DESCRIPTOR_SET_FILENO_HANDLE;
        private static final ObjIntConsumer<FileDescriptor> FILE_DESCRIPTOR_SET_FILENO = ReflectiveAccess::fileDescriptorSetFileno;
        private static final MethodHandle FILE_DESCRIPTOR_GET_FILENO_HANDLE;
        private static final ToIntFunction<FileDescriptor> FILE_DESCRIPTOR_GET_FILENO = ReflectiveAccess::fileDescriptorGetFileno;

        static {
            MethodHandle selChImplGetFD = null;
            Predicate isSelChImpl = null;

            try {
                Class selChImpl = Class.forName("sun.nio.ch.SelChImpl");

                isSelChImpl = selChImpl::isInstance;

                Method getFD = selChImpl.getDeclaredMethod("getFD");

                selChImplGetFD = JavaUtil.getHandleSafe(getFD, ReflectiveAccess.class, LOOKUP);
            } catch (Throwable e) {
                // leave it null
            }

            SEL_CH_IMPL = isSelChImpl;
            SEL_CH_IMPL_GET_FD_HANDLE = selChImplGetFD;

            Predicate isFileChannelImpl = null;
            MethodHandle fileChannelGetFD = null;

            try {
                Class fileChannelImpl = Class.forName("sun.nio.ch.FileChannelImpl");

                isFileChannelImpl = fileChannelImpl::isInstance;

                Field fd = fileChannelImpl.getDeclaredField("fd");

                fileChannelGetFD = JavaUtil.getGetterSafe(fd, ReflectiveAccess.class, LOOKUP);
            } catch (Throwable e) {
                // leave it null
            }

            FILE_CHANNEL_IMPL = isFileChannelImpl;
            FILE_CHANNEL_IMPL_GET_FD_HANDLE = fileChannelGetFD;

            MethodHandle fdGetFileno = null;
            MethodHandle fdSetFileno = null;

            try {
                Field fd = FileDescriptor.class.getDeclaredField("fd");

                fdGetFileno = JavaUtil.getGetterSafe(fd, ReflectiveAccess.class, LOOKUP);
                fdSetFileno = JavaUtil.getSetterSafe(fd, ReflectiveAccess.class, LOOKUP);
            } catch (Throwable e) {
                // leave it null
            }

            FILE_DESCRIPTOR_GET_FILENO_HANDLE = fdGetFileno;
            FILE_DESCRIPTOR_SET_FILENO_HANDLE = fdSetFileno;

            if (selChImplGetFD == null || fileChannelGetFD == null || fdGetFileno == null) {
                // Warn users since we don't currently handle half-native process control.
                Module module = Modules.getModule(ReflectiveAccess.class);
                String moduleName = module.getName();
                if (moduleName == null) {
                    moduleName = "ALL-UNNAMED";
                }
                LOG.warn("Native subprocess control requires open access to the JDK IO subsystem\n" +
                        "Pass '--add-opens java.base/sun.nio.ch=" + moduleName + " --add-opens java.base/java.io=" + moduleName + "' to enable.");
            }
        }

        private static FileDescriptor fileChannelImplGetFD(Object obj) {
            try {
                return (FileDescriptor) FILE_CHANNEL_IMPL_GET_FD_HANDLE.invoke(obj);
            } catch (Throwable t) {
                Helpers.throwException(t);
                return null; // not reached
            }
        }

        private static int fileDescriptorGetFileno(FileDescriptor obj) {
            try {
                return (int) FILE_DESCRIPTOR_GET_FILENO_HANDLE.invoke(obj);
            } catch (Throwable t) {
                Helpers.throwException(t);
                return -1; // not reached
            }
        }

        private static void fileDescriptorSetFileno(FileDescriptor obj, int i) {
            try {
                FILE_DESCRIPTOR_SET_FILENO_HANDLE.invoke(obj, i);
            } catch (Throwable t) {
                Helpers.throwException(t);
            }
        }

        private static FileDescriptor selChImplGetFD(Object obj) {
            try {
                return (FileDescriptor) SEL_CH_IMPL_GET_FD_HANDLE.invoke(obj);
            } catch (Throwable t) {
                Helpers.throwException(t);
                return null; // not reached
            }
        }
    }
}

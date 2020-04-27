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
import org.jruby.runtime.Helpers;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.FileDescriptor;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
        if (ReflectiveAccess.SEL_CH_IMPL_GET_FD != null && ReflectiveAccess.SEL_CH_IMPL.test(channel)) {
            // Pipe Source and Sink, Sockets, and other several other selectable channels
            try {
                return ReflectiveAccess.SEL_CH_IMPL_GET_FD.apply(channel);
            } catch (Exception e) {
                // return bogus below
            }
        } else if (ReflectiveAccess.FILE_CHANNEL_IMPL_GET_FD != null && ReflectiveAccess.FILE_CHANNEL_IMPL.test(channel)) {
            // FileChannels
            try {
                return ReflectiveAccess.FILE_CHANNEL_IMPL_GET_FD.apply(channel);
            } catch (Exception e) {
                // return bogus below
            }
        } else if (ReflectiveAccess.FILE_DESCRIPTOR_SET_FILENO != null) {
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
        if (ReflectiveAccess.FILE_DESCRIPTOR_GET_FILENO != null) {
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
        if (ReflectiveAccess.FILE_DESCRIPTOR_GET_FILENO != null) {
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
        private static final Function<Object, FileDescriptor> SEL_CH_IMPL_GET_FD;
        private static final Predicate<Object> FILE_CHANNEL_IMPL;
        private static final MethodHandle FILE_CHANNEL_IMPL_GET_FD_HANDLE;
        private static final Function<Object, FileDescriptor> FILE_CHANNEL_IMPL_GET_FD;
        private static final MethodHandle FILE_DESCRIPTOR_SET_FILENO_HANDLE;
        private static final ObjIntConsumer<FileDescriptor> FILE_DESCRIPTOR_SET_FILENO;
        private static final MethodHandle FILE_DESCRIPTOR_GET_FILENO_HANDLE;
        private static final ToIntFunction<FileDescriptor> FILE_DESCRIPTOR_GET_FILENO;

        static {
            MethodHandle selChImplGetFD = null;
            Predicate isSelChImpl = null;

            try {
                Class selChImpl = Class.forName("sun.nio.ch.SelChImpl");

                isSelChImpl = selChImpl::isInstance;

                Method getFD = selChImpl.getDeclaredMethod("getFD");

                selChImplGetFD = getHandleSafe(getFD);
            } catch (Throwable e) {
                // leave it null
            }

            SEL_CH_IMPL = isSelChImpl;
            SEL_CH_IMPL_GET_FD_HANDLE = selChImplGetFD;
            SEL_CH_IMPL_GET_FD = selChImplGetFD == null ? null : (obj) -> {
                try {
                    return (FileDescriptor) SEL_CH_IMPL_GET_FD_HANDLE.invoke(obj);
                } catch (Throwable t) {
                    Helpers.throwException(t);
                    return null; // not reached
                }
            };

            Predicate isFileChannelImpl = null;
            MethodHandle fileChannelGetFD = null;

            try {
                Class fileChannelImpl = Class.forName("sun.nio.ch.FileChannelImpl");

                isFileChannelImpl = fileChannelImpl::isInstance;

                Field fd = fileChannelImpl.getDeclaredField("fd");

                fileChannelGetFD = getGetterSafe(fd);
            } catch (Throwable e) {
                // leave it null
            }

            FILE_CHANNEL_IMPL = isFileChannelImpl;
            FILE_CHANNEL_IMPL_GET_FD_HANDLE = fileChannelGetFD;
            FILE_CHANNEL_IMPL_GET_FD = fileChannelGetFD == null ? null : (obj) -> {
                try {
                    return (FileDescriptor) FILE_CHANNEL_IMPL_GET_FD_HANDLE.invoke(obj);
                } catch (Throwable t) {
                    Helpers.throwException(t);
                    return null; // not reached
                }
            };

            MethodHandle fdGetFileno = null;
            MethodHandle fdSetFileno = null;

            try {
                Field fd = FileDescriptor.class.getDeclaredField("fd");

                fdGetFileno = getGetterSafe(fd);
                fdSetFileno = getSetterSafe(fd);
            } catch (Throwable e) {
                // leave it null
            }
            
            FILE_DESCRIPTOR_GET_FILENO_HANDLE = fdGetFileno;
            FILE_DESCRIPTOR_GET_FILENO = fileChannelGetFD == null ? null : (obj) -> {
                try {
                    return (int) FILE_DESCRIPTOR_GET_FILENO_HANDLE.invoke(obj);
                } catch (Throwable t) {
                    Helpers.throwException(t);
                    return -1; // not reached
                }
            };
            FILE_DESCRIPTOR_SET_FILENO_HANDLE = fdSetFileno;
            FILE_DESCRIPTOR_SET_FILENO = fdSetFileno == null ? null : (obj, i) -> {
                try {
                    FILE_DESCRIPTOR_SET_FILENO_HANDLE.invoke(obj, i);
                } catch (Throwable t) {
                    Helpers.throwException(t);
                }
            };

            if (selChImplGetFD == null || fileChannelGetFD == null || fdGetFileno == null) {
                // Warn users since we don't currently handle half-native process control.
                LOG.warn("Native subprocess control requires open access to sun.nio.ch\n" +
                        "Pass '--add-opens java.base/sun.nio.ch=org.jruby.dist' or '=org.jruby.core' to enable.");
            }
        }

        static MethodHandle getHandleSafe(Method method) {
            try {
                return LOOKUP.unreflect(method);
            } catch (IllegalAccessException iae) {
                // try again with setAccessible
                Class<?> declaringClass = method.getDeclaringClass();
                Modules.addOpens(declaringClass, declaringClass.getPackage().getName(), ReflectiveAccess.class);
                if (Java.trySetAccessible(method)) {
                    try {
                        return LOOKUP.unreflect(method);
                    } catch (IllegalAccessException iae2) {
                        // ignore, return null below
                    }
                }
            }

            return null;
        }

        static MethodHandle getGetterSafe(Field field) {
            try {
                return LOOKUP.unreflectGetter(field);
            } catch (IllegalAccessException iae) {
                // try again with setAccessible
                Class<?> declaringClass = field.getDeclaringClass();
                Modules.addOpens(declaringClass, declaringClass.getPackage().getName(), ReflectiveAccess.class);
                if (Java.trySetAccessible(field)) {
                    try {
                        return LOOKUP.unreflectGetter(field);
                    } catch (IllegalAccessException iae2) {
                        // ignore, return null below
                    }
                }
            }

            return null;
        }

        static MethodHandle getSetterSafe(Field field) {
            try {
                return LOOKUP.unreflectSetter(field);
            } catch (IllegalAccessException iae) {
                // try again with setAccessible
                Class<?> declaringClass = field.getDeclaringClass();
                Modules.addOpens(declaringClass, declaringClass.getPackage().getName(), ReflectiveAccess.class);
                if (Java.trySetAccessible(field)) {
                    try {
                        return LOOKUP.unreflectSetter(field);
                    } catch (IllegalAccessException iae2) {
                        // ignore, return null below
                    }
                }
            }

            return null;
        }
    }
}

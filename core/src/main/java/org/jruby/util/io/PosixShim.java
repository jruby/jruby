package org.jruby.util.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.Pipe;

import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.posix.FileStat;
import jnr.posix.POSIX;

import org.jruby.Ruby;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Helpers;
import org.jruby.util.JRubyFile;
import org.jruby.util.ResourceException;

/**
 * Representations of as many native posix functions as possible applied to an NIO channel
 */
public class PosixShim {
    public static final int LOCK_SH = 1;
    public static final int LOCK_EX = 2;
    public static final int LOCK_NB = 4;
    public static final int LOCK_UN = 8;

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

    public PosixShim(Ruby runtime) {
        this.runtime = runtime;
        this.posix = runtime.getPosix();
    }
    
    // pseudo lseek(2)
    public long lseek(ChannelFD fd, long offset, int type) {
        clear();

        if (fd.chSeek != null) {
            int adj = 0;
            try {
                switch (type) {
                    case SEEK_SET:
                        return fd.chSeek.position(offset).position();
                    case SEEK_CUR:
                        return fd.chSeek.position(fd.chSeek.position() - adj + offset).position();
                    case SEEK_END:
                        return fd.chSeek.position(fd.chSeek.size() + offset).position();
                    default:
                        setErrno(Errno.EINVAL);
                        return -1;
                }
            } catch (IllegalArgumentException e) {
                setErrno(Errno.EINVAL);
                return -1;
            } catch (IOException ioe) {
                setErrno(Helpers.errnoFromException(ioe));
                return -1;
            }
        } else if (fd.chNative != null) {
            // native channel, use native lseek
            long ret = posix.lseekLong(fd.chNative.getFD(), offset, type);
            if (ret == -1) setErrno(Errno.valueOf(posix.errno()));
            return ret;
        }

        if (fd.chSelect != null) {
            // For other channel types, we can't get at a native descriptor to lseek, and we can't use FileChannel
            // .position, so we have to treat them as unseekable and raise EPIPE
            //
            // TODO: It's perhaps just a coincidence that all the channels for
            // which we should raise are instanceof SelectableChannel, since
            // stdio is not...so this bothers me slightly. -CON
            //
            // Original change made in 66b024fedbb2ee32316ccd9de8387931d07993ec
            setErrno(Errno.EPIPE);
            return -1;
        }

        return 0;
    }

    public int write(ChannelFD fd, byte[] bytes, int offset, int length, boolean nonblock) {
        clear();

        // FIXME: don't allocate every time
        ByteBuffer tmp = ByteBuffer.wrap(bytes, offset, length);
        try {
            if (nonblock) {
                // TODO: figure out what nonblocking writes against atypical streams (files?) actually do
                // If we can't set the channel nonblocking, I'm not sure what we can do to
                // pretend the channel is nonblocking.
            }

            if (fd.chWrite == null) {
                setErrno(Errno.EACCES);
                return -1;
            }
            int written = fd.chWrite.write(tmp);

            if (written == 0 && length > 0) {
                // if it's a nonblocking write against a file and we've hit EOF, do EAGAIN
                if (nonblock) {
                    setErrno(Errno.EAGAIN);
                    return -1;
                }
            }

            return written;
        }  catch (Exception e) {
            setErrno(Helpers.errnoFromException(e));
            error = e;
            return -1;
        }
    }

    private static final int NATIVE_EOF = 0;
    private static final int JAVA_EOF = -1;

    public int read(ChannelFD fd, byte[] target, int offset, int length, boolean nonblock) {
        clear();

        try {
            if (nonblock) {
                // need to ensure channels that don't support nonblocking IO at least
                // appear to be nonblocking
                if (fd.chSelect != null) {
                    // ok...we should have set it nonblocking already in setNonblock
                } else {
                    if (fd.chFile != null) {
                        long position = fd.chFile.position();
                        long size = fd.chFile.size();
                        if (position != -1 && size != -1 && position < size) {
                            // there should be bytes available...proceed
                        } else {
                            setErrno(Errno.EAGAIN);
                            return -1;
                        }
                    } else if (fd.chNative != null && fd.isNativeFile) {
                        // it's a native file, so we don't do selection or nonblock
                    } else {
                        setErrno(Errno.EAGAIN);
                        return -1;
                    }
                }
            }

            // FIXME: inefficient to recreate ByteBuffer every time
            ByteBuffer buffer = ByteBuffer.wrap(target, offset, length);
            int read = fd.chRead.read(buffer);

            if (nonblock) {
                if (read == JAVA_EOF) {
                    read = NATIVE_EOF; // still treat EOF as EOF
                } else if (read == 0) {
                    setErrno(Errno.EAGAIN);
                    return -1;
                } else {
                    return read;
                }
            } else {
                // NIO channels will always raise for errors, so -1 only means EOF.
                if (read == JAVA_EOF) read = NATIVE_EOF;
            }

            return read;
        } catch (IOException ioe) {
            setErrno(Helpers.errnoFromException(ioe));
            return -1;
        }
    }

    // rb_thread_flock
    public int flock(ChannelFD fd, int lockMode) {
        // TODO: null channel always succeeds for all locking operations
//        if (descriptor.isNull()) return RubyFixnum.zero(runtime);

        clear();

        int real_fd = fd.realFileno;

        if (posix.isNative() && real_fd != -1 && real_fd < FilenoUtil.FIRST_FAKE_FD && !Platform.IS_SOLARIS) {
            // we have a real fd and not on Solaris...try native flocking
            // see jruby/jruby#3254 and jnr/jnr-posix#60
            int result = posix.flock(real_fd, lockMode);
            if (result < 0) {
                setErrno(Errno.valueOf(posix.errno()));
                return -1;
            }
            return 0;
        }

        if (fd.chFile != null) {
            int ret = checkSharedExclusive(fd, lockMode);
            if (ret < 0) return ret;

            if (!lockStateChanges(fd.currentLock.get(), lockMode)) return 0;

            try {
                synchronized (fd.chFile) {
                    // check again, to avoid unnecessary overhead
                    if (!lockStateChanges(fd.currentLock.get(), lockMode)) return 0;

                    switch (lockMode) {
                        case LOCK_UN:
                        case LOCK_UN | LOCK_NB:
                            return unlock(fd);
                        case LOCK_EX:
                            return lock(fd, true);
                        case LOCK_EX | LOCK_NB:
                            return tryLock(fd, true);
                        case LOCK_SH:
                            return lock(fd, false);
                        case LOCK_SH | LOCK_NB:
                            return tryLock(fd, false);
                    }
                }
            } catch (IOException ioe) {
                setErrno(Helpers.errnoFromException(ioe));
                return -1;
            } catch (OverlappingFileLockException ioe) {
                setErrno(Errno.EINVAL);
                errmsg = "overlapping file locks";
            }
            return lockFailedReturn(lockMode);
        } else {
            // We're not actually a real file, so we can't flock
            // FIXME: This needs to be ENOTSUP
            setErrno(Errno.EINVAL);
            errmsg = "stream is not a file";
            return -1;
        }
    }

    public int dup2(ChannelFD filedes, ChannelFD filedes2) {
        return filedes2.dup2From(posix, filedes);
    }

    public int close(ChannelFD fd) {
        return close((Closeable)fd);
    }

    public int close(Closeable closeable) {
        clear();

        try {
            closeable.close();
            return 0;
        } catch (IOException ioe) {
            Errno errno = Helpers.errnoFromException(ioe);
            if (errno == null) {
                throw new RuntimeException("unknown IOException: " + ioe);
            }
            this.setErrno(errno);
            return -1;
        }
    }

    public Channel[] pipe() {
        clear();
        try {
            Pipe pipe = Pipe.open();
            Channel source = pipe.source(), sink = pipe.sink();

            if (posix.isNative() && !Platform.IS_WINDOWS) {
                // set cloexec if possible
                int read = FilenoUtil.filenoFrom(source);
                int write = FilenoUtil.filenoFrom(sink);
                setCloexec(read, true);
                setCloexec(write, true);
            }

            return new Channel[]{source, sink};
        } catch (IOException ioe) {
            setErrno(Helpers.errnoFromException(ioe));
            return null;
        }
    }

    /**
     * The appropriate errno value for the last thrown error, if any.
     */
    public Errno getErrno() {
        return errno.get();
    }

    public void setErrno(Errno errno) {
        this.errno.set(errno);
    }

    public interface WaitMacros {
        public abstract boolean WIFEXITED(long status);
        public abstract boolean WIFSIGNALED(long status);
        public abstract int WTERMSIG(long status);
        public abstract int WEXITSTATUS(long status);
        public abstract int WSTOPSIG(long status);
        public abstract boolean WIFSTOPPED(long status);
        public abstract boolean WCOREDUMP(long status);
    }

    public static class BSDWaitMacros implements WaitMacros {
        public final long _WSTOPPED = 0177;

        // Only confirmed on Darwin
        public final long WCOREFLAG = 0200;

        public long _WSTATUS(long status) {
            return status & _WSTOPPED;
        }

        public boolean WIFEXITED(long status) {
            return _WSTATUS(status) == 0;
        }

        public boolean WIFSIGNALED(long status) {
            return _WSTATUS(status) != _WSTOPPED && _WSTATUS(status) != 0;
        }

        public int WTERMSIG(long status) {
            return (int)_WSTATUS(status);
        }

        public int WEXITSTATUS(long status) {
            // not confirmed on all platforms
            return (int)((status >>> 8) & 0xFF);
        }

        public int WSTOPSIG(long status) {
            return (int)(status >>> 8);
        }

        public boolean WIFSTOPPED(long status) {
            return _WSTATUS(status) == _WSTOPPED && WSTOPSIG(status) != 0x13;
        }

        public boolean WCOREDUMP(long status) {
            return (status & WCOREFLAG) != 0;
        }
    }

    public static class LinuxWaitMacros implements WaitMacros {
        private int __WAIT_INT(long status) { return (int)status; }

        private int __W_EXITCODE(int ret, int sig) { return (ret << 8) | sig; }
        private int __W_STOPCODE(int sig) { return (sig << 8) | 0x7f; }
        private static int __W_CONTINUED = 0xffff;
        private static final int __WCOREFLAG = 0x80;

        /* If WIFEXITED(STATUS), the low-order 8 bits of the status.  */
        private int __WEXITSTATUS(long status) { return (int)((status & 0xff00) >> 8); }

        /* If WIFSIGNALED(STATUS), the terminating signal.  */
        private int __WTERMSIG(long status) { return (int)(status & 0x7f); }

        /* If WIFSTOPPED(STATUS), the signal that stopped the child.  */
        private int __WSTOPSIG(long status) { return __WEXITSTATUS(status); }

        /* Nonzero if STATUS indicates normal termination.  */
        private boolean __WIFEXITED(long status) { return __WTERMSIG(status) == 0; }

        /* Nonzero if STATUS indicates termination by a signal.  */
        private boolean __WIFSIGNALED(long status) {
            return ((status & 0x7f) + 1) >> 1 > 0;
        }

        /* Nonzero if STATUS indicates the child is stopped.  */
        private boolean __WIFSTOPPED(long status) { return (status & 0xff) == 0x7f; }

        /* Nonzero if STATUS indicates the child dumped core.  */
        private boolean __WCOREDUMP(long status) { return (status & __WCOREFLAG) != 0; }

        /* Macros for constructing status values.  */
        public int WEXITSTATUS(long status) { return __WEXITSTATUS (__WAIT_INT (status)); }
        public int WTERMSIG(long status) { return __WTERMSIG(__WAIT_INT(status)); }
        public int WSTOPSIG(long status) { return __WSTOPSIG(__WAIT_INT(status)); }
        public boolean WIFEXITED(long status) { return __WIFEXITED(__WAIT_INT(status)); }
        public boolean WIFSIGNALED(long status) { return __WIFSIGNALED(__WAIT_INT(status)); }
        public boolean WIFSTOPPED(long status) { return __WIFSTOPPED(__WAIT_INT(status)); }
        public boolean WCOREDUMP(long status) { return __WCOREDUMP(__WAIT_INT(status)); }
    }

    public static final WaitMacros WAIT_MACROS;
    static {
        if (Platform.IS_BSD) {
            WAIT_MACROS = new BSDWaitMacros();
        } else {
            // need other platforms
            WAIT_MACROS = new LinuxWaitMacros();
        }
    }

    public int setCloexec(int fd, boolean cloexec) {
        int ret = posix.fcntl(fd, Fcntl.F_GETFD);
        if (ret == -1) {
            setErrno(Errno.valueOf(posix.errno()));
            return -1;
        }
        if (
                (cloexec && (ret & FcntlLibrary.FD_CLOEXEC) == FcntlLibrary.FD_CLOEXEC)
                || (!cloexec && (ret & FcntlLibrary.FD_CLOEXEC) == 0)) {
            return 0;
        }
        ret = cloexec ?
                ret | FcntlLibrary.FD_CLOEXEC :
                ret & ~FcntlLibrary.FD_CLOEXEC;
        ret = posix.fcntlInt(fd, Fcntl.F_SETFD, ret);
        if (ret == -1) setErrno(Errno.valueOf(posix.errno()));
        return ret;
    }

    public int fcntlSetFD(int fd, int flags) {
        int ret = posix.fcntlInt(fd, Fcntl.F_SETFD, flags);
        if (ret == -1) setErrno(Errno.valueOf(posix.errno()));
        return ret;
    }

    public int fcntlGetFD(int fd) {
        int ret = posix.fcntl(fd, Fcntl.F_GETFD);
        if (ret == -1) {
            setErrno(Errno.valueOf(posix.errno()));
        }
        return ret;
    }

    public Channel open(String cwd, String path, int flags, int perm) {
        if (Platform.IS_WINDOWS && (path.equals("/dev/null") || path.equalsIgnoreCase("nul"))) {
            path = "NUL:";
        }

        try {
            return JRubyFile.createResource(runtime, cwd, path).openChannel(flags, perm);
        } catch (ResourceException.FileExists e) {
            setErrno(Errno.EEXIST);
        } catch (ResourceException.FileIsDirectory e) {
            setErrno(Errno.EISDIR);
        } catch (ResourceException.FileIsNotDirectory e) {
            setErrno(Errno.ENOTDIR);
        } catch (ResourceException.NotFound e) {
            setErrno(Errno.ENOENT);
        } catch (ResourceException.PermissionDenied e) {
            setErrno(Errno.EACCES);
        } catch (ResourceException.TooManySymlinks e) {
            setErrno(Errno.ELOOP);
        } catch (ResourceException.ErrnoException e) {
            setErrno(e.getErrno());
        } catch (ResourceException ex) {
            throw ex.newRaiseException(runtime);
        } catch (Exception ex) {
            Helpers.throwErrorFromException(runtime, ex);
            // not reached
        }
        return null;
    }

    // no longer used
    public Channel open(String cwd, String path, ModeFlags flags, int perm) {
        return open(cwd, path, flags, perm);
    }

    @Deprecated // special case is already handled with JRubyFile.createResource
    public Channel open(String cwd, String path, ModeFlags flags, int perm, ClassLoader classLoader) {
        if (path.startsWith("classpath:/") && classLoader != null) {
            path = path.substring("classpath:/".length());
            return Channels.newChannel(classLoader.getResourceAsStream(path));
        }

        return open(cwd, path, flags, perm);
    }

    /**
     * Joy of POSIX, only way to get the umask is to set the umask,
     * then set it back. That's unsafe in a threaded program. We
     * minimize but may not totally remove this race by caching the
     * obtained or previously set (see umask() above) umask and using
     * that as the initial set value which, cross fingers, is a
     * no-op. The cache access is then synchronized. TODO: Better?
     */
    public static int umask(POSIX posix) {
        synchronized (_umaskLock) {
            final int umask = posix.umask(_cachedUmask);
            if (_cachedUmask != umask ) {
                posix.umask(umask);
                _cachedUmask = umask;
            }
            return umask;
        }
    }

    public static int umask(POSIX posix, int newMask) {
        int oldMask;
        synchronized (_umaskLock) {
            oldMask = posix.umask(newMask);
            _cachedUmask = newMask;
        }
        return oldMask;
    }

    public int ftruncate(ChannelFD fd, long pos) {
        if (fd.chNative != null) {
            int ret = posix.ftruncate(fd.chNative.getFD(), pos);
            if (ret == -1) setErrno(Errno.valueOf(posix.errno()));
            return ret;
        } else if (fd.chFile != null) {
            try {
                fd.chFile.truncate(pos);
            } catch (IOException ioe) {
                setErrno(Helpers.errnoFromException(ioe));
                return -1;
            }
        } else {
            setErrno(Errno.EINVAL);
            return -1;
        }
        return 0;
    }

    public long size(ChannelFD fd) {
        if (fd.chNative != null) { // native fd, use fstat
            FileStat stat = posix.allocateStat();
            int ret = posix.fstat(fd.chNative.getFD(), stat);
            if (ret == -1) {
                setErrno(Errno.valueOf(posix.errno()));
                return -1;
            }
            return stat.st_size();
        } else if (fd.chSeek != null) { // if it is seekable, get size directly
            try {
                return fd.chSeek.size();
            } catch (IOException ioe) {
                setErrno(Helpers.errnoFromException(ioe));
                return -1;
            }
        } else {
            // otherwise just return -1 (should be rare, since size is only defined on File
            setErrno(Errno.EINVAL);
            return -1;
        }
    }

    private void clear() {
        setErrno(null);
        errmsg = null;
    }

    private int checkSharedExclusive(ChannelFD fd, int lockMode) {
        // This logic used to attempt a shared lock instead of an exclusive
        // lock, because LOCK_EX on some systems (as reported in JRUBY-1214)
        // allow exclusively locking a read-only file. However, the JDK
        // APIs do not allow acquiring an exclusive lock on files that are
        // not open for read, and there are other platforms (such as Solaris,
        // see JRUBY-5627) that refuse at an *OS* level to exclusively lock
        // files opened only for read. As a result, this behavior is platform-
        // dependent, and so we will obey the JDK's policy of disallowing
        // exclusive locks on files opened only for read.
        if (fd.chWrite == null && (lockMode & LOCK_EX) > 0) {
            setErrno(Errno.EINVAL);
            errmsg = "cannot acquire exclusive lock on File not opened for write";
            return -1;
        }

        // Likewise, JDK does not allow acquiring a shared lock on files
        // that have not been opened for read. We comply here.
        if (fd.chRead == null && (lockMode & LOCK_SH) > 0) {
            setErrno(Errno.EINVAL);
            errmsg = "cannot acquire shared lock on File not opened for read";
            return -1;
        }

        return 0;
    }

    private static int lockFailedReturn(int lockMode) {
        return (lockMode & LOCK_EX) == 0 ? 0 : -1;
    }

    private static boolean lockStateChanges(FileLock lock, int lockMode) {
        if (lock == null) {
            // no lock, only proceed if we are acquiring
            switch (lockMode & 0xF) {
                case LOCK_UN:
                case LOCK_UN | LOCK_NB:
                    return false;
                default:
                    return true;
            }
        } else {
            // existing lock, only proceed if we are unlocking or changing
            switch (lockMode & 0xF) {
                case LOCK_UN:
                case LOCK_UN | LOCK_NB:
                    return true;
                case LOCK_EX:
                case LOCK_EX | LOCK_NB:
                    return lock.isShared();
                case LOCK_SH:
                case LOCK_SH | LOCK_NB:
                    return !lock.isShared();
                default:
                    return false;
            }
        }
    }

    private int unlock(ChannelFD fd) throws IOException {
        FileLock fileLock = fd.currentLock.get();

        if (fileLock != null) {
            fileLock.release();
            fd.currentLock.remove();

            return 0;
        }
        return -1;
    }

    private int lock(ChannelFD fd, boolean exclusive) throws IOException {
        FileLock fileLock = fd.currentLock.get();

        if (fileLock != null) fileLock.release();

        fileLock = fd.chFile.lock(0L, Long.MAX_VALUE, !exclusive);

        fd.currentLock.set(fileLock);

        if (fileLock != null) {
            return 0;
        }

        return lockFailedReturn(exclusive ? LOCK_EX : LOCK_SH);
    }

    private int tryLock(ChannelFD fd, boolean exclusive) throws IOException {
        FileLock fileLock = fd.currentLock.get();

        if (fileLock != null) fileLock.release();

        fileLock = fd.chFile.tryLock(0L, Long.MAX_VALUE, !exclusive);

        fd.currentLock.set(fileLock);

        if (fileLock != null) {
            return 0;
        }

        return lockFailedReturn(exclusive ? LOCK_EX : LOCK_SH);
    }

    /**
     * The last Throwable exception raised by a call.
     */
    public Throwable error;

    private final ThreadLocal<Errno> errno = new ThreadLocal<>();

    /**
     * The recommended error message, if any.
     */
    public String errmsg;

    /**
     * The POSIX instance to use for native calls
     */
    private final POSIX posix;

    /**
     * The current runtime
     */
    private final Ruby runtime;

    /**
     * An object to synchronize calls to umask
     */
    private static final Object _umaskLock = new Object();

    /**
     * The last umask we set
     */
    private static int _cachedUmask = 0;
}

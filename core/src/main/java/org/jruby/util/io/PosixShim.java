package org.jruby.util.io;

import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import org.jruby.Ruby;
import org.jruby.runtime.Helpers;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

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

    public PosixShim(POSIX posix) {
        this.posix = posix;
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
                        errno = Errno.EINVAL;
                        return -1;
                }
            } catch (IllegalArgumentException e) {
                errno = Errno.EINVAL;
                return -1;
            } catch (IOException ioe) {
                errno = Helpers.errnoFromException(ioe);
                return -1;
            }
        } else if (fd.chSelect != null) {
            // TODO: It's perhaps just a coincidence that all the channels for
            // which we should raise are instanceof SelectableChannel, since
            // stdio is not...so this bothers me slightly. -CON
            errno = Errno.EPIPE;
            return -1;
        } else {
            errno = Errno.EPIPE;
            return -1;
        }
    }

    public int write(ChannelFD fd, byte[] bytes, int offset, int length, boolean nonblock) {
        clear();

        // FIXME: don't allocate every time
        ByteBuffer tmp = ByteBuffer.wrap(bytes, offset, length);
        try {
            return fd.chWrite.write(tmp);
        } catch (IOException ioe) {
            errno = Helpers.errnoFromException(ioe);
            error = ioe;
            return -1;
        }

    }

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
                            errno = Errno.EAGAIN;
                            return -1;
                        }
                    } else {
                        errno = Errno.EAGAIN;
                        return -1;
                    }
                }
            }

            // FIXME: inefficient to recreate ByteBuffer every time
            ByteBuffer buffer = ByteBuffer.wrap(target, offset, length);
            int read = fd.chRead.read(buffer);

            // NIO channels will always raise for errors, so -1 only means EOF.
            if (read == -1) read = 0;

            if (read == 0) {
                // if it's a nonblocking read against a file and we've hit EOF, do EAGAIN
                if (nonblock) {
                    errno = Errno.EAGAIN;
                    return -1;
                }
            }

            return read;
        } catch (IOException ioe) {
            errno = Helpers.errnoFromException(ioe);
            return -1;
        }
    }

    // rb_thread_flock
    public int flock(ChannelFD fd, int lockMode) {
        // TODO: null channel always succeeds for all locking operations
//        if (descriptor.isNull()) return RubyFixnum.zero(runtime);

        Channel channel = fd.ch;
        clear();

        int real_fd = FilenoUtil.getFilenoFromChannel(channel);

        if (real_fd != -1 && real_fd < FilenoUtil.FIRST_FAKE_FD) {
            int result = posix.flock(real_fd, lockMode);
            if (result < 0) {
                errno = Errno.valueOf(posix.errno());
                return -1;
            }
            return 0;
        }

        if (fd.chFile != null) {
            int ret = checkSharedExclusive(fd, lockMode);
            if (ret < 0) return ret;

            if (!lockStateChanges(fd.currentLock, lockMode)) return 0;

            try {
                synchronized (fd.chFile) {
                    // check again, to avoid unnecessary overhead
                    if (!lockStateChanges(fd.currentLock, lockMode)) return 0;

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
                errno = Helpers.errnoFromException(ioe);
                return -1;
            } catch (OverlappingFileLockException ioe) {
                errno = Errno.EINVAL;
                errmsg = "overlapping file locks";
            }
            return lockFailedReturn(lockMode);
        } else {
            // We're not actually a real file, so we can't flock
            // FIXME: This needs to be ENOTSUP
            errno = Errno.EINVAL;
            errmsg = "stream is not a file";
            return -1;
        }
    }

    public int dup2(ChannelFD filedes, ChannelFD filedes2) {
        return filedes2.dup2From(posix, filedes);
    }

    public int close(ChannelFD fd) {
        clear();

        try {
            fd.close();
            return 0;
        } catch (IOException ioe) {
            Errno errno = Helpers.errnoFromException(ioe);
            if (errno == null) {
                throw new RuntimeException("unknown IOException: " + ioe);
            }
            this.errno = errno;
            return -1;
        }
    }

    private void clear() {
        errno = null;
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
            errno = Errno.EINVAL;
            errmsg = "cannot acquire exclusive lock on File not opened for write";
            return -1;
        }

        // Likewise, JDK does not allow acquiring a shared lock on files
        // that have not been opened for read. We comply here.
        if (fd.chRead == null && (lockMode & LOCK_SH) > 0) {
            errno = Errno.EINVAL;
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
        if (fd.currentLock != null) {
            fd.currentLock.release();
            fd.currentLock = null;

            return 0;
        }
        return -1;
    }

    private int lock(ChannelFD fd, boolean exclusive) throws IOException {
        if (fd.currentLock != null) fd.currentLock.release();

        fd.currentLock = fd.chFile.lock(0L, Long.MAX_VALUE, !exclusive);

        if (fd.currentLock != null) {
            return 0;
        }

        return lockFailedReturn(exclusive ? LOCK_EX : LOCK_SH);
    }

    private int tryLock(ChannelFD fd, boolean exclusive) throws IOException {
        if (fd.currentLock != null) fd.currentLock.release();

        fd.currentLock = fd.chFile.tryLock(0L, Long.MAX_VALUE, !exclusive);

        if (fd.currentLock != null) {
            return 0;
        }

        return lockFailedReturn(exclusive ? LOCK_EX : LOCK_SH);
    }

    /**
     * The last Throwable exception raised by a call.
     */
    public Throwable error;

    /**
     * The appropriate errno value for the last thrown error, if any.
     */
    public Errno errno;

    /**
     * The recommended error message, if any.
     */
    public String errmsg;

    /**
     * The POSIX instance to use for native calls
     */
    private final POSIX posix;
}

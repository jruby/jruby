package org.jruby.util.io;

import jnr.constants.platform.Errno;
import jnr.constants.platform.OpenFlags;
import jnr.posix.JavaLibCHelper;
import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jruby.Ruby;
import org.jruby.RubyArgsFile;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBignum;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OpenFile {

    // IO Mode flags
    public static final int READABLE           = 0x00000001;
    public static final int WRITABLE           = 0x00000002;
    public static final int READWRITE          = READABLE | WRITABLE;
    public static final int BINMODE            = 0x00000004;
    public static final int SYNC               = 0x00000008;
    public static final int TTY                = 0x00000010;
    public static final int DUPLEX             = 0x00000020;
    public static final int APPEND             = 0x00000040;
    public static final int CREATE             = 0x00000080;
    public static final int WSPLIT             = 0x00000200;
    public static final int WSPLIT_INITIALIZED = 0x00000400;
    public static final int TRUNC              = 0x00000800;
    public static final int TEXTMODE           = 0x00001000;
    public static final int SETENC_BY_BOM      = 0x00100000;
    public static final int PREP         = (1<<16);

    public static final int LOCK_SH = 1;
    public static final int LOCK_EX = 2;
    public static final int LOCK_NB = 4;
    public static final int LOCK_UN = 8;

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    
    public static final int SYNCWRITE = SYNC | WRITABLE;

    public static final int PIPE_BUF = 512; // value of _POSIX_PIPE_BUF from Mac OS X 10.9
    public static final int BUFSIZ = 1024; // value of BUFSIZ from Mac OS X 10.9 stdio.h

    public static class ChannelFD {
        public ChannelFD(Channel fd) {
            this.ch = fd;

            if (fd instanceof ReadableByteChannel) chRead = (ReadableByteChannel)fd;
            else chRead = null;
            if (fd instanceof WritableByteChannel) chWrite = (WritableByteChannel)fd;
            else chWrite = null;
            if (fd instanceof SeekableByteChannel) chSeek = (SeekableByteChannel)fd;
            else chSeek = null;
            if (fd instanceof SelectableChannel) chSelect = (SelectableChannel)fd;
            else chSelect = null;
            if (fd instanceof FileChannel) chFile = (FileChannel)fd;
            else chFile = null;

            realFileno = ChannelDescriptor.getFilenoFromChannel(fd);
            if (realFileno == -1) {
                fakeFileno = ChannelDescriptor.getNewFileno();
            } else {
                fakeFileno = -1;
            }

            refs = 1;
        }

        public final Channel ch;
        public final ReadableByteChannel chRead;
        public final WritableByteChannel chWrite;
        public final SeekableByteChannel chSeek;
        public final SelectableChannel chSelect;
        public final FileChannel chFile;
        public final int realFileno;
        public final int fakeFileno;
//        private final int fakeFileno;
        private volatile int refs = 0;

        // FIXME shouldn't use static; would interfere with other runtimes in the same JVM
        public static int FIRST_FAKE_FD = 100000;
        protected static final AtomicInteger internalFilenoIndex = new AtomicInteger(FIRST_FAKE_FD);

        public static int getNewFileno() {
            return internalFilenoIndex.getAndIncrement();
        }
    }

    public static interface Finalizer {

        public void finalize(Ruby runtime, boolean raise);
    }

    // RB_IO_FPTR_NEW, minus fields that Java already initializes the same way
    public OpenFile(IRubyObject nil) {
        writeconvAsciicompat = nil;
        writeconvPreEcopts = nil;
        encs.ecopts = nil;
    }
    private ChannelFD fd;
    private int mode;
    public Process process;
    private int lineno;
    private String pathv;
    private Finalizer finalizer;
    public InputStream stdioIn;
    public OutputStream stdioOut;
    public volatile FileLock currentLock;

    public static class Buffer {
        public byte[] ptr;
        public int start;
        public int off;
        public int len;
        public int capa;
    }

    public IOEncodable.ConvConfig encs = new IOEncodable.ConvConfig();

    public EConv readconv;
    public EConv writeconv;
    public IRubyObject writeconvAsciicompat;
    public int writeconvPreEcflags;
    public IRubyObject writeconvPreEcopts;
    public boolean writeconvInitialized;

    public volatile ReentrantReadWriteLock write_lock;

    public final Buffer wbuf = new Buffer(), rbuf = new Buffer(), cbuf = new Buffer();

    public RubyIO tiedIOForWriting;

    private boolean nonblock = false;
    public Errno errno = null;

    public int getFileno() {
        int fileno = fd.realFileno;
        if (fileno != -1) return fileno;
        return fd.fakeFileno;
    }

    // rb_thread_flock
    public int threadFlock(ThreadContext context, int lockMode) {
        Ruby runtime = context.runtime;

        // null channel always succeeds for all locking operations
//        if (descriptor.isNull()) return RubyFixnum.zero(runtime);

        Channel channel = fd.ch;
        errno = null;

        FileDescriptor fdObj = ChannelDescriptor.getDescriptorFromChannel(channel);
        int real_fd = JavaLibCHelper.getfdFromDescriptor(fdObj);

        if (real_fd != -1) {
            // we have a real fd...try native flocking
            IRubyObject oldExc = runtime.getGlobalVariables().get("$!");
            try {
                int result = runtime.getPosix().flock(real_fd, lockMode);
                if (result < 0) {
                    errno = Errno.valueOf(runtime.getPosix().errno());
                    return -1;
                }
                return 0;
            } catch (RaiseException re) {
                if (re.getException().getMetaClass() == runtime.getNotImplementedError()) {
                    // not implemented, probably pure Java; fall through
                    runtime.getGlobalVariables().set("$!", oldExc);
                } else {
                    throw re;
                }
            }
        }

        if (fd.chFile != null) {
            checkSharedExclusive(runtime, lockMode);

            if (!lockStateChanges(currentLock, lockMode)) return 0;

            try {
                synchronized (fd.chFile) {
                    // check again, to avoid unnecessary overhead
                    if (!lockStateChanges(currentLock, lockMode)) return 0;

                    switch (lockMode) {
                        case LOCK_UN:
                        case LOCK_UN | LOCK_NB:
                            return unlock(runtime);
                        case LOCK_EX:
                            return lock(runtime, fd.chFile, true);
                        case LOCK_EX | LOCK_NB:
                            return tryLock(runtime, fd.chFile, true);
                        case LOCK_SH:
                            return lock(runtime, fd.chFile, false);
                        case LOCK_SH | LOCK_NB:
                            return tryLock(runtime, fd.chFile, false);
                    }
                }
            } catch (IOException ioe) {
                if (runtime.getDebug().isTrue()) {
                    ioe.printStackTrace(System.err);
                }
            } catch (OverlappingFileLockException ioe) {
                if (runtime.getDebug().isTrue()) {
                    ioe.printStackTrace(System.err);
                }
            }
            return lockFailedReturn(runtime, lockMode);
        } else {
            // We're not actually a real file, so we can't flock
            // FIXME: This needs to be ENOTSUP
            errno = Errno.ENOENT;
            return -1;
        }
    }

    private void checkSharedExclusive(Ruby runtime, int lockMode) {
        // This logic used to attempt a shared lock instead of an exclusive
        // lock, because LOCK_EX on some systems (as reported in JRUBY-1214)
        // allow exclusively locking a read-only file. However, the JDK
        // APIs do not allow acquiring an exclusive lock on files that are
        // not open for read, and there are other platforms (such as Solaris,
        // see JRUBY-5627) that refuse at an *OS* level to exclusively lock
        // files opened only for read. As a result, this behavior is platform-
        // dependent, and so we will obey the JDK's policy of disallowing
        // exclusive locks on files opened only for read.
        if (!isWritable() && (lockMode & LOCK_EX) > 0) {
            throw runtime.newErrnoEBADFError("cannot acquire exclusive lock on File not opened for write");
        }

        // Likewise, JDK does not allow acquiring a shared lock on files
        // that have not been opened for read. We comply here.
        if (!isReadable() && (lockMode & LOCK_SH) > 0) {
            throw runtime.newErrnoEBADFError("cannot acquire shared lock on File not opened for read");
        }
    }

    private static int lockFailedReturn(Ruby runtime, int lockMode) {
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

    private int unlock(Ruby runtime) throws IOException {
        if (currentLock != null) {
            currentLock.release();
            currentLock = null;

            return 0;
        }
        return -1;
    }

    private int lock(Ruby runtime, FileChannel fileChannel, boolean exclusive) throws IOException {
        if (currentLock != null) currentLock.release();

        currentLock = fileChannel.lock(0L, Long.MAX_VALUE, !exclusive);

        if (currentLock != null) {
            return 0;
        }

        return lockFailedReturn(runtime, exclusive ? LOCK_EX : LOCK_SH);
    }

    private int tryLock(Ruby runtime, FileChannel fileChannel, boolean exclusive) throws IOException {
        if (currentLock != null) currentLock.release();

        currentLock = fileChannel.tryLock(0L, Long.MAX_VALUE, !exclusive);

        if (currentLock != null) {
            return 0;
        }

        return lockFailedReturn(runtime, exclusive ? LOCK_EX : LOCK_SH);
    }

    public void clearStdio() {
        stdioOut = null;
        stdioIn = null;
    }

    public String PREP_STDIO_NAME() {
        return pathv;
    }

    public boolean READ_DATA_PENDING() {return rbuf.len != 0;}
    public int READ_DATA_PENDING_COUNT() {return rbuf.len;}
    // goes with READ_DATA_PENDING_OFF
    public byte[] READ_DATA_PENDING_PTR() {return rbuf.ptr;}
    public int READ_DATA_PENDING_OFF() {return rbuf.off;}
    public int READ_DATA_PENDING_START() {return rbuf.start;}
    public boolean READ_DATA_BUFFERED() {return READ_DATA_PENDING();}

    public boolean READ_CHAR_PENDING() {return cbuf.len != 0;}
    public int READ_CHAR_PENDING_COUNT() {return cbuf.len;}
    // goes with READ_CHAR_PENDING_OFF
    public byte[] READ_CHAR_PENDING_PTR() {return cbuf.ptr;}
    public int READ_CHAR_PENDING_OFF() {return cbuf.off;}
    public int READ_CHAR_PENDING_START() {return cbuf.start;}

    public void READ_CHECK(ThreadContext context) {
        if (!READ_DATA_PENDING()) {
            checkClosed(context.runtime);
        }
    }

    public boolean IS_PREP_STDIO() {
        return (mode & PREP) == PREP;
    }

    public void setFD(Channel fd) {
        this.fd = new ChannelFD(fd);
    }

    public int getMode() {
        return mode;
    }
    
    public String getModeAsString(Ruby runtime) {
        String modeString = getStringFromMode(mode);

        if (modeString == null) {
            throw runtime.newArgumentError("Illegal access modenum " + Integer.toOctalString(mode));
        }

        return modeString;
    }
    
    public static int getModeFlagsAsIntFrom(int fmode) {
        int oflags = 0;
        
        if ((fmode & READABLE) != 0) {
            if ((fmode & WRITABLE) != 0) {
                oflags |= ModeFlags.RDWR;
            } else {
                oflags |= ModeFlags.RDONLY;
            }
        } else if ((fmode & WRITABLE) != 0) {
            oflags |= ModeFlags.WRONLY;
        }
        
        if ((fmode & APPEND) != 0) oflags |= ModeFlags.APPEND;
        if ((fmode & CREATE) != 0) oflags |= ModeFlags.CREAT;
        if ((fmode & BINMODE) != 0) oflags |= ModeFlags.BINARY;
        if ((fmode & TEXTMODE) != 0) oflags |= ModeFlags.TEXT;
        if ((fmode & TRUNC) != 0) oflags |= ModeFlags.TRUNC;
        
        return oflags;
    }

    // MRI: rb_io_oflags_modestr
    public static String ioOflagsModestr(Ruby runtime, int oflags) {
        int accmode = oflags & (OpenFlags.O_RDONLY.intValue()|OpenFlags.O_WRONLY.intValue()|OpenFlags.O_RDWR.intValue());
        if ((oflags & OpenFlags.O_APPEND.intValue()) != 0) {
            if (accmode == OpenFlags.O_WRONLY.intValue()) {
                return MODE_BINARY(oflags, "a", "ab");
            }
            if (accmode == OpenFlags.O_RDWR.intValue()) {
                return MODE_BINARY(oflags, "a+", "ab+");
            }
        }
        switch (OpenFlags.valueOf(oflags & (OpenFlags.O_RDONLY.intValue()|OpenFlags.O_WRONLY.intValue()|OpenFlags.O_RDWR.intValue()))) {
            default:
                throw runtime.newArgumentError("invalid access oflags 0x" + Integer.toHexString(oflags));
            case O_RDONLY:
                return MODE_BINARY(oflags, "r", "rb");
            case O_WRONLY:
                return MODE_BINARY(oflags, "w", "wb");
            case O_RDWR:
                return MODE_BINARY(oflags, "r+", "rb+");
        }
    }

    // MRI: rb_io_modestr_oflags
    public static int ioModestrOflags(Ruby runtime, String modestr) {
        return ioFmodeOflags(ioModestrFmode(runtime, modestr));
    }

    // MRI: rb_io_fmode_oflags
    public static int ioFmodeOflags(int fmode) {
        int oflags = 0;

        switch (fmode & OpenFile.READWRITE) {
            case OpenFile.READABLE:
                oflags |= OpenFlags.O_RDONLY.intValue();
                break;
            case OpenFile.WRITABLE:
                oflags |= OpenFlags.O_WRONLY.intValue();
                break;
            case OpenFile.READWRITE:
                oflags |= OpenFlags.O_RDWR.intValue();
                break;
        }

        if ((fmode & OpenFile.APPEND) != 0) {
            oflags |= OpenFlags.O_APPEND.intValue();
        }
        if ((fmode & OpenFile.TRUNC) != 0) {
            oflags |= OpenFlags.O_TRUNC.intValue();
        }
        if ((fmode & OpenFile.CREATE) != 0) {
            oflags |= OpenFlags.O_CREAT.intValue();
        }
//        #ifdef OpenFlags.O_BINARY
//        if (fmode & OpenFile.BINMODE) {
//            oflags |= OpenFlags.O_BINARY.intValue();
//        }
//        #endif

        return oflags;
    }

    public static int ioModestrFmode(Ruby runtime, String modestr) {
        int fmode = 0;
        char[] mChars = modestr.toCharArray(), pChars = null;
        int m = 0, p = 0;

        switch (mChars[m++]) {
            case 'r':
                fmode |= OpenFile.READABLE;
                break;
            case 'w':
                fmode |= OpenFile.WRITABLE | OpenFile.TRUNC | OpenFile.CREATE;
                break;
            case 'a':
                fmode |= OpenFile.WRITABLE | OpenFile.APPEND | OpenFile.CREATE;
                break;
            default:
                throw runtime.newArgumentError("invalid access mode " + modestr);
        }

        loop: while (m < mChars.length) {
            switch (mChars[m++]) {
                case 'b':
                    fmode |= OpenFile.BINMODE;
                    break;
                case 't':
                    fmode |= OpenFile.TEXTMODE;
                    break;
                case '+':
                    fmode |= OpenFile.READWRITE;
                    break;
                default:
                    throw runtime.newArgumentError("invalid access mode " + modestr);
                case ':':
                    pChars = mChars;
                    p = m;
                    if ((fmode & OpenFile.BINMODE) != 0 && (fmode & OpenFile.TEXTMODE) != 0)
                        throw runtime.newArgumentError("invalid access mode " + modestr);
                    break loop;
            }
        }

        if ((fmode & OpenFile.BINMODE) != 0 && (fmode & OpenFile.TEXTMODE) != 0)
            throw runtime.newArgumentError("invalid access mode " + modestr);
        if (p != 0 && ioEncnameBomP(new String(pChars, p, pChars.length - p), 0))
            fmode |= OpenFile.SETENC_BY_BOM;

        return fmode;
    }

    static boolean ioEncnameBomP(String name, long len) {
        String bom_prefix = "bom|utf-";
        int bom_prefix_len = bom_prefix.length();
        if (len == 0) {
            int p = name.indexOf(':');
            len = p != -1 ? p : name.length();
        }
        return len > bom_prefix_len && name.compareToIgnoreCase(bom_prefix) == 0;
    }

    private static String MODE_BINARY(int oflags, String a, String b) {
        if (OpenFlags.O_BINARY.intValue() != -1 && (oflags & OpenFlags.O_BINARY.intValue()) != 0) {
            return b;
        }
        return a;
    }

    public static int getFModeFromString(String modesString) throws InvalidValueException {
        int fmode = 0;
        int length = modesString.length();

        if (length == 0) {
            throw new InvalidValueException();
        }

        switch (modesString.charAt(0)) {
            case 'r' :
                fmode |= READABLE;
                break;
            case 'w' :
                fmode |= WRITABLE | TRUNC | CREATE;
                break;
            case 'a' :
                fmode |= WRITABLE | APPEND | CREATE;
                break;
            default :
                throw new InvalidValueException();
        }

        ModifierLoop: for (int n = 1; n < length; n++) {
            switch (modesString.charAt(n)) {
                case 'b':
                    fmode |= BINMODE;
                    break;
                case 't' :
                    fmode |= TEXTMODE;
                    break;
                case '+':
                    fmode |= READWRITE;
                    break;
                case ':':
                    break ModifierLoop;
                default:
                    throw new InvalidValueException();
            }
        }

        return fmode;        
    }    

    public static String getStringFromMode(int mode) {
        if ((mode & APPEND) != 0) {
            if ((mode & READWRITE) != 0) {
                return "ab+";
            }
            return "ab";
        }
        switch (mode & READWRITE) {
        case READABLE:
            return "rb";
        case WRITABLE:
            return "wb";
        case READWRITE:
            if ((mode & CREATE) != 0) {
                return "wb+";
            }
            return "rb+";
        }
        return null;
    }

    // rb_io_check_char_readable
    public void checkCharReadable(Ruby runtime) {
        checkClosed(runtime);

        if ((mode & READABLE) == 0) {
            throw runtime.newIOError("not opened for reading");
        }

        if (wbuf.len != 0) {
            if (fflush(runtime) < 0) {
                throw runtime.newErrnoFromErrno(errno, "error flushing");
            }
        }
        if (tiedIOForWriting != null) {
            OpenFile wfptr;
            wfptr = tiedIOForWriting.getOpenFileChecked();
            if (wfptr.fflush(runtime) < 0)
                throw runtime.newErrnoFromErrno(wfptr.errno, wfptr.getPath());
        }
    }

    // rb_io_check_byte_readable
    public void checkByteReadable(Ruby runtime) {
        checkCharReadable(runtime);
        if (READ_CHAR_PENDING()) {
            throw runtime.newIOError("byte oriented read for character buffered IO");
        }
    }

    // rb_io_check_readable
    public void checkReadable(Ruby runtime) {
        checkByteReadable(runtime);
    }

    // io_fflush
    public int fflush(Ruby runtime) {
        checkClosed(runtime);

        if (wbuf.len == 0) return 0;

        checkClosed(runtime);

        while (wbuf.len > 0 && flushBuffer(runtime) != 0) {
            if (!waitWritable(runtime)) {
                return -1;
            }
            checkClosed(runtime);
        }

        return 0;
    }

    // rb_io_wait_writable
    public boolean waitWritable(Ruby runtime) {
        // errno logic checking here appeared to be mostly to release
        // gvl when a read would block or retry if it was interrupted

        // TODO: evaluate whether errno checking here is useful

        return ready(runtime, SelectBlob.WRITE_CONNECT_OPS);
    }

    // rb_io_wait_readable
    public boolean waitReadable(Ruby runtime) {
        // errno logic checking here appeared to be mostly to release
        // gvl when a read would block or retry if it was interrupted

        // TODO: evaluate whether errno checking here is useful

        return ready(runtime, SelectBlob.READ_ACCEPT_OPS);
    }

    public boolean ready(Ruby runtime, int ops) {
        try {
            if (fd.chSelect != null) {
                int ready_stat = 0;
                java.nio.channels.Selector sel = SelectorFactory.openWithRetryFrom(null, fd.chSelect.provider());
                synchronized (fd.chSelect.blockingLock()) {
                    boolean is_block = fd.chSelect.isBlocking();
                    try {
                        fd.chSelect.configureBlocking(false);
                        fd.chSelect.register(sel, ops);
                        ready_stat = sel.selectNow();
                        sel.close();
                    } finally {
                        if (sel != null) {
                            try {
                                sel.close();
                            } catch (Exception e) {
                            }
                        }
                        fd.chSelect.configureBlocking(is_block);
                    }
                }
                return ready_stat == 1;
            } else {
                if (fd.chSeek != null) {
                    return fd.chSeek.position() < fd.chSeek.size();
                }
                return false;
            }
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }

    }

    // io_flush_buffer
    public int flushBuffer(Ruby runtime) {
        if (write_lock != null) {
            write_lock.writeLock().lock();
            try {
                return flushBufferAsync2(runtime);
            } finally {
                write_lock.writeLock().unlock();
            }
        }
        return flushBufferAsync2(runtime);
    }

    // io_flush_buffer_async2
    public int flushBufferAsync2(Ruby runtime)
    {
        int ret;


        // GVL-free call to io_flush_buffer_sync2 here

//        ret = (VALUE)rb_thread_call_without_gvl2(io_flush_buffer_sync2, fptr,
//                RUBY_UBF_IO, NULL);
        ret = flushBufferSync2(runtime);

        if (ret == 0) {
            // TODO
	/* pending async interrupt is there. */
            errno = Errno.EAGAIN;
            return -1;
        } else if (ret == 1) {
            return 0;
        } else
            return ret;
    }

    // io_flush_buffer_sync2
    public int flushBufferSync2(Ruby runtime)
    {
        int result = flushBufferSync(runtime);

    /*
     * rb_thread_call_without_gvl2 uses 0 as interrupted.
     * So, we need to avoid to use 0.
     */
        return result == 0 ? 1 : result;
    }

    // io_flush_buffer_sync
    public int flushBufferSync(Ruby runtime)
    {
        int l = writableLength(wbuf.len);
        ByteBuffer tmp = ByteBuffer.wrap(wbuf.ptr, wbuf.off, l);
        try {
            int r = fd.chWrite.write(tmp);

            if (wbuf.len <= r) {
                wbuf.off = 0;
                wbuf.len = 0;
                return 0;
            }
            if (0 <= r) {
                wbuf.off += (int)r;
                wbuf.len -= (int)r;
                errno = Errno.EAGAIN;
            }
            return -1;
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    // io_writable_length
    public int writableLength(int l)
    {
        // we should always assume other threads, so we don't use rb_thread_alone
        if (PIPE_BUF < l &&
//                !rb_thread_alone() &&
                wsplit()) {
            l = PIPE_BUF;
        }
        return l;
    }

    // wsplit_p
    boolean wsplit()
    {
        int r;

        if ((mode & WSPLIT_INITIALIZED) == 0) {
            // Unsure what wsplit mode is, but only appears to be active for
            // regular files that are doing blocking reads. Since we can't do
            // nonblocking file reads, we just check if the write channel is
            // selectable and set flags accordingly.
//            struct stat buf;
            if (fd.chFile != null
//            if (fstat(fptr->fd, &buf) == 0 &&
//                    !S_ISREG(buf.st_mode)
//                    && (r = fcntl(fptr->fd, F_GETFL)) != -1 &&
//                    !(r & O_NONBLOCK)
            ) {
                mode |= WSPLIT;
            }
            mode |= WSPLIT_INITIALIZED;
        }
        return (mode & WSPLIT) != 0;
    }

    // io_seek
    public long seek(ThreadContext context, long offset, int whence) {
        flushBeforeSeek(context);
        return lseek(context, offset, whence);
    }

    // flush_before_seek
    void flushBeforeSeek(ThreadContext context)
    {
        if (fflush(context.runtime) < 0)
            throw context.runtime.newErrnoFromErrno(errno, "");
        unread(context);
        errno = null;
    }

    // pseudo lseek(2)
    public long lseek(ThreadContext context, long offset, int type) {
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
                        throw context.runtime.newArgumentError("invalid seek whence: " + type);
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

    public void checkWritable(ThreadContext context) {
        checkClosed(context.runtime);
        if ((mode & WRITABLE) == 0) {
            throw context.runtime.newIOError("not opened for writing");
        }
        if (rbuf.len != 0) {
            unread(context);
        }
    }

    public void checkClosed(Ruby runtime) {
        if (fd == null) {
            throw runtime.newIOError("closed stream");
        }
    }

    public boolean isBinmode() {
        return (mode & BINMODE) != 0;
    }

    public boolean isTextMode() {
        return (mode & TEXTMODE) != 0;
    }

    public void setTextMode() {
        mode |= TEXTMODE;
        // FIXME: Make stream(s) know about text mode.
    }

    public void clearTextMode() {
        mode &= ~TEXTMODE;
        // FIXME: Make stream(s) know about text mode.
    }

    public void setBinmode() {
        mode |= BINMODE;
    }

    public boolean isOpen() {
        return fd != null;
    }

    public boolean isReadable() {
        return (mode & READABLE) != 0;
    }

    public boolean isWritable() {
        return (mode & WRITABLE) != 0;
    }

    public boolean isReadBuffered() {
        return READ_DATA_BUFFERED();
    }

    public boolean isWriteBuffered() {
        return false;
    }

    public void setSync(boolean sync) {
        if(sync) {
            mode = mode | SYNC;
        } else {
            mode = mode & ~SYNC;
        }
    }

    public boolean isSync() {
        return (mode & (SYNC | TTY)) != 0;
    }

    public boolean areBothEOF() throws IOException, BadDescriptorException {
        return mainStream.feof() && (pipeStream != null ? pipeStream.feof() : true);
    }

    public void setMode(int modes) {
        this.mode = modes;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public long getPid() {
        return ShellLauncher.getPidFromProcess(process);
    }

    public int getLineNumber() {
        return lineno;
    }

    public void setLineNumber(int lineNumber) {
        this.lineno = lineNumber;
    }

    public String getPath() {
        return pathv;
    }

    public void setPath(String path) {
        this.pathv = path;
    }

    public boolean isAutoclose() {
        boolean autoclose = true;
        Stream myMain, myPipe;
        if ((myMain = mainStream) != null) autoclose &= myMain.isAutoclose();
        if ((myPipe = pipeStream) != null) autoclose &= myPipe.isAutoclose();
        return autoclose;
    }

    public void setAutoclose(boolean autoclose) {
        Stream myMain, myPipe;
        if ((myMain = mainStream) != null) myMain.setAutoclose(autoclose);
        if ((myPipe = pipeStream) != null) myPipe.setAutoclose(autoclose);
    }

    public Finalizer getFinalizer() {
        return finalizer;
    }

    public void setFinalizer(Finalizer finalizer) {
        this.finalizer = finalizer;
    }

    public void cleanup(Ruby runtime, boolean noraise) {
        if (finalizer != null) {
            finalizer.finalize(runtime, noraise);
        } else {
            finalize(runtime, noraise);
        }
    }

    public void finalize(Ruby runtime, boolean noraise) {
        IRubyObject err = runtime.getNil();

        // TODO: ???
//        FILE *stdio_file = fptr->stdio_file;

        if (writeconv != null) {
            if (write_lock != null && !noraise) {
//                struct finish_writeconv_arg arg;
//                arg.fptr = fptr;
//                arg.noalloc = noraise;
//                err = rb_mutex_synchronize(fptr->write_lock, finish_writeconv_sync, (VALUE)&arg);
                // TODO: interruptible version
                finishWriteconv(runtime, noraise);
            }
            else {
                err = finishWriteconv(runtime, noraise);
            }
        }
        if (wbuf.len != 0) {
            if (noraise) {
                if ((int)flushBufferSync(runtime) < 0 && err.isNil())
                    err = runtime.getTrue();
            }
            else {
                if (fflush(runtime) < 0 && err.isNil()) {
                    err = RubyFixnum.newFixnum(runtime, errno == null ? 0 : errno.longValue());
                }
            }
        }

        fd = null;
//        stdio_file = 0;
        mode &= ~(READABLE|WRITABLE);

        if (IS_PREP_STDIO() || isStdio()) {
	/* need to keep FILE objects of stdin, stdout and stderr */
        }
        // TODO: ???
//        else if (stdio_file) {
//	/* stdio_file is deallocated anyway
//         * even if fclose failed.  */
//
//            if ((maygvl_fclose(stdio_file, noraise) < 0) && NIL_P(err))
//                err = noraise ? Qtrue : INT2NUM(errno);
//        }
//        else if (0 <= fd) {
//	/* fptr->fd may be closed even if close fails.
//         * POSIX doesn't specify it.
//         * We assumes it is closed.  */
//            if ((maygvl_close(fd, noraise) < 0) && NIL_P(err))
//                err = noraise ? Qtrue : INT2NUM(errno);
//        }

        if (!err.isNil() && !noraise) {
            if (err instanceof RubyFixnum || err instanceof RubyBignum) {
                errno = Errno.valueOf(RubyNumeric.num2int(err));
                throw runtime.newErrnoFromErrno(errno, pathv);
            } else {
                throw new RaiseException((RubyException)err);
            }
        }
    }

    // MRI: NEED_READCONV (FIXME: Windows has slightly different version)
    public boolean needsReadConversion() {
        return (encs.enc2 != null || (encs.ecflags & ~EConvFlags.CRLF_NEWLINE_DECORATOR) != 0);
    }

    // MRI: NEED_WRITECONV (FIXME: Windows has slightly different version)
    public boolean needsWriteConversion(ThreadContext context) {
        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();

        return (encs.enc != null && encs.enc != ascii8bit) || isTextMode() ||
                (encs.ecflags & ((EConvFlags.DECORATOR_MASK & ~EConvFlags.CRLF_NEWLINE_DECORATOR)| EConvFlags.STATEFUL_DECORATOR_MASK)) != 0;
    }

    // MRI: make_readconv
    public void makeReadConversion(ThreadContext context, int size) {
        if (readconv == null) {
            int ecflags;
            IRubyObject ecopts;
            byte[] sname, dname;
            ecflags = encs.ecflags & ~EConvFlags.NEWLINE_DECORATOR_MASK;
            ecopts = encs.ecopts;
            if (encs.enc2 != null) {
                sname = encs.enc2.getName();
                dname = encs.enc.getName();
            }
            else {
                sname = dname = new byte[0];
            }
            readconv = EncodingUtils.econvOpenOpts(context, sname, dname, ecflags, ecopts);
            if (readconv == null)
                throw EncodingUtils.econvOpenExc(context, sname, dname, ecflags);
            cbuf.off = 0;
            cbuf.len = 0;
            if (size < IO_CBUF_CAPA_MIN) size = IO_CBUF_CAPA_MIN;
            cbuf.capa = size;
            cbuf.ptr = new byte[cbuf.capa];
        }
    }

    public void makeReadConversion(ThreadContext context) {
        makeReadConversion(context, IO_CBUF_CAPA_MIN);
    }

    // MRI: make_writeconv
    public void makeWriteConversion(ThreadContext context) {
        if (writeconvInitialized) return;

        byte[] senc;
        byte[] denc;
        Encoding enc;
        int ecflags;
        IRubyObject ecopts;

        writeconvInitialized = true;

        ecflags = encs.ecflags & ~EConvFlags.NEWLINE_DECORATOR_READ_MASK;
        ecopts = encs.ecopts;

        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();
        if (encs.enc == null || (encs.enc == ascii8bit && encs.enc2 == null)) {
            /* no encoding conversion */
            writeconvPreEcflags = 0;
            writeconvPreEcopts = context.nil;
            writeconv = EncodingUtils.econvOpenOpts(context, EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY, ecflags, ecopts);
            if (writeconv == null) {
                throw EncodingUtils.econvOpenExc(context, EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY, ecflags);
            }
            writeconvAsciicompat = context.nil;
        }
        else {
            enc = encs.enc2 != null ? encs.enc2 : encs.enc;
            Encoding tmpEnc = EncodingUtils.econvAsciicompatEncoding(enc);
            senc = tmpEnc == null ? null : tmpEnc.getName();
            if (senc == null && (encs.ecflags & EConvFlags.STATEFUL_DECORATOR_MASK) == 0) {
                /* single conversion */
                writeconvPreEcflags = ecflags;
                writeconvPreEcopts = ecopts;
                writeconv = null;
                writeconvAsciicompat = context.nil;
            }
            else {
                /* double conversion */
                writeconvPreEcflags = ecflags & ~EConvFlags.STATEFUL_DECORATOR_MASK;
                writeconvPreEcopts = ecopts;
                if (senc != null) {
                    denc = enc.getName();
                    writeconvAsciicompat = RubyString.newString(context.runtime, senc);
                }
                else {
                    senc = denc = EMPTY_BYTE_ARRAY;
                    writeconvAsciicompat = RubyString.newString(context.runtime, enc.getName());
                }
                ecflags = encs.ecflags & (EConvFlags.ERROR_HANDLER_MASK | EConvFlags.STATEFUL_DECORATOR_MASK);
                ecopts = encs.ecopts;
                writeconv = EncodingUtils.econvOpenOpts(context, senc, denc, ecflags, ecopts);
                if (writeconv == null) {
                    throw EncodingUtils.econvOpenExc(context, senc, denc, ecflags);
                }
            }
        }
    }

    public void clearReadConversion() {
        readconv = null;
    }

    public void clearCodeConversion() {
        readconv = null;
        writeconv = null;
    }

    public static final int MORE_CHAR_SUSPENDED = 0;
    public static final int MORE_CHAR_FINISHED = 1;
    public static final int EOF = -1;

    public static final int IO_RBUF_CAPA_MIN = 8192;
    public static final int IO_CBUF_CAPA_MIN = (128*1024);
    public int IO_RBUF_CAPA_FOR() {
        return needsReadConversion() ? IO_CBUF_CAPA_MIN : IO_RBUF_CAPA_MIN;
    }
    public static final int IO_WBUF_CAPA_MIN = 8192;

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public int appendline(ThreadContext context, int delim, ByteList[] strp, int[] lp)
    {
        ByteList str = strp[0];
        int limit = lp[0];

        if (needsReadConversion()) {
            setBinmode();
            makeReadConversion(context);
            do {
                int p, e;
                int searchlen = READ_CHAR_PENDING_COUNT();
                if (searchlen > 0) {
                    byte[] pBytes = READ_CHAR_PENDING_PTR();
                    p = READ_CHAR_PENDING_OFF();
                    if (0 < limit && limit < searchlen)
                        searchlen = limit;
                    e = memchr(pBytes, p, delim, searchlen);
                    if (e != -1) {
                        int len = (int)(e-p+1);
                        if (str == null) {
                            strp[0] = str = new ByteList(pBytes, p, len);
                        } else {
                            str.append(pBytes, p, len);
                        }
                        cbuf.off += len;
                        cbuf.len -= len;
                        limit -= len;
                        lp[0] = limit;
                        return delim;
                    }

                    if (str == null) {
                        strp[0] = str = new ByteList(pBytes, p, searchlen);
                    } else {
                        EncodingUtils.rbStrBufCat(context.runtime, str, pBytes, p, searchlen);
                    }
                    cbuf.off += searchlen;
                    cbuf.len -= searchlen;
                    limit -= searchlen;

                    if (limit == 0) {
                        lp[0] = limit;
                        return str.get(str.getRealSize() - 1) & 0xFF;
                    }
                }
            } while (moreChar(context) != MORE_CHAR_FINISHED);
            clearReadConversion();
            lp[0] = limit;
            return EOF;
        }

        NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
        do {
            int pending = READ_DATA_PENDING_COUNT();
            if (pending > 0) {
                byte[] pBytes = READ_DATA_PENDING_PTR();
                int p = READ_DATA_PENDING_OFF();
                int e = -1;
                int last;

                if (limit > 0 && pending > limit) pending = limit;
                e = memchr(pBytes, p, delim, pending);
                if (e != -1) pending = e - p + 1;
                if (str != null) {
                    last = str.getRealSize();
                    str.length(last + pending);
                }
                else {
                    last = 0;
                    strp[0] = str = new ByteList(pending);
                    str.setRealSize(pending);
                }
                readBufferedData(str.getUnsafeBytes(), str.getBegin() + last, pending); /* must not fail */
                limit -= pending;
                lp[0] = limit;
                if (e != -1) return delim;
                if (limit == 0) {
                    return str.get(str.getRealSize() - 1) & 0xFF;
                }
            }
            READ_CHECK(context);
        } while (fillbuf(context) >= 0);
        lp[0] = limit;
        return EOF;
    }

    private int memchr(byte[] pBytes, int p, int delim, int length) {
        for (int i = p; i < p + length; i++) {
            if ((pBytes[i] & 0xFF) == delim) {
                return i;
            }
        }
        return -1;
    }

    public void NEED_NEWLINE_DECORATOR_ON_READ_CHECK() {
        if (NEED_NEWLINE_DECORATOR_ON_READ()) {
            if ((getMode() & OpenFile.READABLE) != 0 &&
                    (encs.ecflags & EConvFlags.NEWLINE_DECORATOR_MASK) == 0) {
                setBinmode();
            } else {
                setTextMode();
            }
        }
    }

    public boolean NEED_NEWLINE_DECORATOR_ON_READ() {
        return isTextMode();
    }

    public boolean NEED_NEWLINE_DECORATOR_ON_WRITE() {
        return isTextMode();
    }

    public int moreChar(ThreadContext context) {
        Object v;
        v = fillCbuf(context, EConvFlags.AFTER_OUTPUT);
        if (!(v instanceof Integer) ||
                ((Integer)v != MORE_CHAR_SUSPENDED && (Integer)v != MORE_CHAR_FINISHED))
            throw (RaiseException)v;
        return (Integer)v;
    }

    private Object fillCbuf(ThreadContext context, int ec_flags) {
        int ss, se;
        int ds, de;
        EConvResult res;
        int putbackable;
        int cbuf_len0;
        RaiseException exc;

        ec_flags |= EConvFlags.PARTIAL_INPUT;

        if (cbuf.len == cbuf.capa)
            return MORE_CHAR_SUSPENDED; /* cbuf full */
        if (cbuf.len == 0)
            cbuf.off = 0;
        else if (cbuf.off + cbuf.len == cbuf.capa) {
            System.arraycopy(cbuf.ptr, cbuf.off, cbuf.ptr, 0, cbuf.len);
            cbuf.off = 0;
        }

        cbuf_len0 = cbuf.len;

        Ptr spPtr = new Ptr();
        Ptr dpPtr = new Ptr();

        while (true) {
            ss = spPtr.p = rbuf.off;
            se = spPtr.p + rbuf.len;
            ds = dpPtr.p = cbuf.off + cbuf.len;
            de = cbuf.capa;
            res = readconv.convert(rbuf.ptr, spPtr, se, cbuf.ptr, dpPtr, de, ec_flags);
            rbuf.off += (int)(spPtr.p - ss);
            rbuf.len -= (int)(spPtr.p - ss);
            cbuf.len += (int)(dpPtr.p - ds);

            putbackable = readconv.putbackable();
            if (putbackable != 0) {
                readconv.putback(rbuf.ptr, rbuf.off - putbackable, putbackable);
                rbuf.off -= putbackable;
                rbuf.len += putbackable;
            }

            exc = EncodingUtils.makeEconvException(context.runtime, readconv);
            if (exc != null)
                return exc;

            if (cbuf_len0 != cbuf.len)
                return MORE_CHAR_SUSPENDED;

            if (res == EConvResult.Finished) {
                return MORE_CHAR_FINISHED;
            }

            if (res == EConvResult.SourceBufferEmpty) {
                if (rbuf.len == 0) {
                    READ_CHECK(context);
                    if (fillbuf(context) == -1) {
                        if (readconv == null) {
                            return MORE_CHAR_FINISHED;
                        }
                        ds = dpPtr.p = cbuf.off + cbuf.len;
                        de = cbuf.capa;
                        res = readconv.convert(null, null, 0, cbuf.ptr, dpPtr, de, 0);
                        cbuf.len += (int)(dpPtr.p - ds);
                        EncodingUtils.econvCheckError(context, readconv);
                        break;
                    }
                }
            }
        }
        if (cbuf_len0 != cbuf.len)
            return MORE_CHAR_SUSPENDED;

        return MORE_CHAR_FINISHED;
    }

    // read_buffered_data
    public int readBufferedData(byte[] ptrBytes, int ptr, int len) {
        int n = rbuf.len;

        if (n <= 0) return n;
        if (n > len) n = len;
        System.arraycopy(rbuf.ptr, rbuf.start + rbuf.off, ptrBytes, ptr, n);
        rbuf.off += n;
        rbuf.len -= n;
        return n;
    }

    // io_fillbuf
    public int fillbuf(ThreadContext context) {
        int r;

        if (rbuf.ptr == null) {
            rbuf.off = 0;
            rbuf.len = 0;
            rbuf.capa = IO_RBUF_CAPA_FOR();
            rbuf.ptr = new byte[rbuf.capa];
            if (Platform.IS_WINDOWS) {
                rbuf.capa--;
            }
        }
        if (rbuf.len == 0) {
            retry: while (true) {
                r = readInternal(context, this, fd.chRead, rbuf.ptr, 0, rbuf.capa);

                if (r < 0) {
                    if (waitReadable(context, fd)) {
                        continue retry;
                    }
                    // This should be errno
                    throw context.runtime.newErrnoFromErrno(errno, "channel: " + fd + (pathv != null ? " " + pathv : ""));
                }
                break;
            }
            rbuf.off = 0;
            rbuf.len = (int)r; /* r should be <= rbuf_capa */
            if (r == 0)
                return -1; /* EOF */
        }
        return 0;
    }

    public static class InternalReadStruct {
        InternalReadStruct(OpenFile fptr, ReadableByteChannel fd, byte[] bufBytes, int buf, int count) {
            this.fptr = fptr;
            this.fd = fd;
            this.bufBytes = bufBytes;
            this.buf = buf;
            this.capa = count;
        }

        public OpenFile fptr;
        public ReadableByteChannel fd;
        public byte[] bufBytes;
        public int buf;
        public int capa;
    }

    final static RubyThread.Task<InternalReadStruct, Integer> readTask = new RubyThread.Task<InternalReadStruct, Integer>() {
        @Override
        public Integer run(ThreadContext context, InternalReadStruct iis) throws InterruptedException {
            // TODO: make nonblocking (when possible? MRI doesn't seem to...)
            // FIXME: inefficient to recreate ByteBuffer every time
            try {
                iis.fptr.errno = null;
                if (iis.fptr.nonblock) {
                    // need to ensure channels that don't support nonblocking IO at least
                    // appear to be nonblocking
                    if (iis.fd instanceof SelectableChannel) {
                        // ok...we should have set it nonblocking already in setNonblock
                    } else {
                        if (iis.fd instanceof FileChannel) {
                            long position = ((FileChannel)iis.fd).position();
                            long size = ((FileChannel)iis.fd).size();
                            if (position != -1 && size != -1) {
                                // there should be bytes available...proceed
                            } else {
                                iis.fptr.errno = Errno.EAGAIN;
                                return -1;
                            }
                        } else {
                            iis.fptr.errno = Errno.EAGAIN;
                            return -1;
                        }
                    }
                }

                ByteBuffer buffer = ByteBuffer.wrap(iis.bufBytes, iis.buf, iis.capa);
                int read = iis.fd.read(buffer);

                if (read <= 0 && iis.fd instanceof FileChannel) {
                    // if it's a nonblocking read against a file and we've hit EOF, do EAGAIN
                    if (iis.fptr.nonblock) {
                        iis.fptr.errno = Errno.EAGAIN;
                        return -1;
                    }

                    // FileChannel returns -1 upon reading to EOF rather than blocking, so we call that 0 like read(2)
                    read = 0;
                } else if (read <= 0 && iis.fptr.nonblock) {
                    iis.fptr.errno = Errno.EAGAIN;
                    return -1;
                }

                return read;
            } catch (IOException ioe) {
                iis.fptr.errno = Helpers.errnoFromException(ioe);
                return -1;
            }
        }

        @Override
        public void wakeup(RubyThread thread, InternalReadStruct self) {
            // FIXME: NO! This will kill many native channels. Must be nonblocking to interrupt.
            thread.getNativeThread().interrupt();
        }
    };

    final static RubyThread.Task<InternalWriteStruct, Integer> writeTask = new RubyThread.Task<InternalWriteStruct, Integer>() {
        @Override
        public Integer run(ThreadContext context, InternalWriteStruct iis) throws InterruptedException {
            return writeTaskBody(context.runtime, iis);
        }

        @Override
        public void wakeup(RubyThread thread, InternalWriteStruct self) {
            // FIXME: NO! This will kill many native channels. Must be nonblocking to interrupt.
            thread.getNativeThread().interrupt();
        }
    };

    // rb_read_internal
    public static int readInternal(ThreadContext context, OpenFile fptr, ReadableByteChannel fd, byte[] bufBytes, int buf, int count) {
        InternalReadStruct iis = new InternalReadStruct(fptr, fd, bufBytes, buf, count);

        try {
            return context.getThread().executeTask(context, iis, readTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("IO operation interrupted");
        }
    }

    // rb_io_wait_readable
    boolean waitReadable(ThreadContext context, ChannelFD fd)
    {
        if (!fd.ch.isOpen()) {
            throw context.runtime.newIOError("closed stream");
        }

        if (fd.chSelect != null) {
            return context.getThread().select(fd.chSelect, null, SelectionKey.OP_READ);
        }

        // kinda-hacky way to see if there's more data to read from a seekable channel
        if (fd.chSeek != null) {
            SeekableByteChannel fdSeek = fd.chSeek;
            try {
                // not a real file, can't get size...we'll have to just read and block
                if (fdSeek.size() < 0) return true;

                // if current position is less than file size, read should not block
                return fdSeek.position() < fdSeek.size();
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }

        // we can't determine if it is readable
        return false;

        /*
        switch (errno) {
            case EINTR:
                #if defined(ERESTART)
            case ERESTART:
                #endif
                rb_thread_check_ints();
                return TRUE;

            case EAGAIN:
                #if defined(EWOULDBLOCK) && EWOULDBLOCK != EAGAIN
            case EWOULDBLOCK:
                #endif
                rb_thread_wait_fd(f);
                return TRUE;

            default:
                return FALSE;
        }*/
    }

    // io_read_encoding
    public Encoding readEncoding(Ruby runtime) {
        return encs.enc != null ? encs.enc : EncodingUtils.defaultExternalEncoding(runtime);
    }

    // io_input_encoding
    public Encoding inputEncoding(Ruby runtime) {
        return encs.enc2 != null ? encs.enc2 : readEncoding(runtime);
    }

    // swallow
    public boolean swallow(ThreadContext context, int term) {
        Ruby runtime = context.runtime;

        if (needsReadConversion()) {
            Encoding enc = readEncoding(runtime);
            boolean needconv = enc.minLength() != 1;
            setBinmode();
            makeReadConversion(context);
            do {
                int cnt;
                int[] i = {0};
                while ((cnt = READ_CHAR_PENDING_COUNT()) > 0) {
                    byte[] pBytes = READ_CHAR_PENDING_PTR();
                    int p = READ_CHAR_PENDING_OFF();
                    i[0] = 0;
                    if (!needconv) {
                        if (pBytes[p] != term) return true;
                        i[0] = (int)cnt;
                        while ((--i[0] != 0) && pBytes[++p] == term);
                    }
                    else {
                        int e = p + cnt;
                        if (EncodingUtils.encAscget(pBytes, p, e, i, enc) != term) return true;
                        while ((p += i[0]) < e && EncodingUtils.encAscget(pBytes, p, e, i, enc) == term);
                        i[0] = (int)(e - p);
                    }
                    shiftCbuf(context, (int)cnt - i[0], null);
                }
            } while (moreChar(context) != MORE_CHAR_FINISHED);
            return false;
        }

        NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
        do {
            int cnt;
            while ((cnt = READ_DATA_PENDING_COUNT()) > 0) {
                byte[] buf = new byte[1024];
                byte[] pBytes = READ_DATA_PENDING_PTR();
                int p = READ_DATA_PENDING_OFF();
                int i;
                if (cnt > buf.length) cnt = buf.length;
                if ((pBytes[p] &  0xFF) != term) return true;
                i = (int)cnt;
                while (--i != 0 && (pBytes[++p] & 0xFF) == term);
                if (readBufferedData(buf, 0, cnt - i) == 0) /* must not fail */
                    throw context.runtime.newRuntimeError("failure copying buffered IO bytes");
            }
            READ_CHECK(context);
        } while (fillbuf(context) == 0);
        return false;
    }

    // io_shift_cbuf
    public IRubyObject shiftCbuf(ThreadContext context, int len, IRubyObject strp) {
        IRubyObject str = null;
        if (strp != null) {
            str = strp;
            if (str.isNil()) {
                strp = str = RubyString.newString(context.runtime, cbuf.ptr, cbuf.off, len);
            }
            else {
                ((RubyString)str).cat(cbuf.ptr, cbuf.off, len);
            }
            str.setTaint(true);
            ((RubyString)str).setEncoding(encs.enc);
        }
        cbuf.off += len;
        cbuf.len -= len;
    /* xxx: set coderange */
        if (cbuf.len == 0)
            cbuf.off = 0;
        else if (cbuf.capa/2 < cbuf.off) {
            System.arraycopy(cbuf.ptr, cbuf.off, cbuf.ptr, 0, cbuf.len);
            cbuf.off = 0;
        }
        return str;
    }

    // rb_io_getline_fast
    public IRubyObject getlineFast(ThreadContext context, Encoding enc, RubyIO io) {
        Ruby runtime = context.runtime;
        IRubyObject str = null;
        ByteList strByteList;
        int len = 0;
        int pos = 0;
        int cr = 0;

        do {
            int pending = READ_DATA_PENDING_COUNT();

            if (pending > 0) {
                byte[] pBytes = READ_DATA_PENDING_PTR();
                int p = READ_DATA_PENDING_OFF();
                int e;

                e = memchr(pBytes, p, '\n', pending);
                if (e != -1) {
                    pending = (int)(e - p + 1);
                }
                if (str == null) {
                    str = RubyString.newString(runtime, pBytes, p, pending);
                    strByteList = ((RubyString)str).getByteList();
                    rbuf.off += pending;
                    rbuf.len -= pending;
                }
                else {
                    ((RubyString)str).resize(len + pending);
                    strByteList = ((RubyString)str).getByteList();
                    readBufferedData(strByteList.unsafeBytes(), strByteList.begin() + len, pending);
                }
                len += pending;
                if (cr != StringSupport.CR_BROKEN)
                    pos += StringSupport.codeRangeScanRestartable(enc, strByteList.unsafeBytes(), strByteList.begin() + pos, strByteList.begin() + len, cr);
                if (e != -1) break;
            }
            READ_CHECK(context);
        } while (fillbuf(context) >= 0);
        if (str == null) return context.nil;
        str = EncodingUtils.ioEncStr(runtime, str, this);
        ((RubyString)str).setCodeRange(cr);
        incrementLineno(runtime);

        return str;
    }

    public void incrementLineno(Ruby runtime) {
        lineno++;
        runtime.setCurrentLine(lineno);
        RubyArgsFile.setCurrentLineNumber(runtime.getArgsFile(), lineno);
    }

    // read_all, 2014-5-13
    public IRubyObject readAll(ThreadContext context, int siz, IRubyObject str) {
        Ruby runtime = context.runtime;
        int bytes;
        int n;
        int pos;
        Encoding enc;
        int cr;

        if (needsReadConversion()) {
            setBinmode();
            str = EncodingUtils.setStrBuf(runtime, str, 0);
            makeReadConversion(context);
            while (true) {
                Object v;
                if (cbuf.len != 0) {
                    str = shiftCbuf(context, cbuf.len, str);
                }
                v = fillCbuf(context, 0);
                if (!v.equals(MORE_CHAR_SUSPENDED) && !v.equals(MORE_CHAR_FINISHED)) {
                    if (cbuf.len != 0) {
                        str = shiftCbuf(context, cbuf.len, str);
                    }
                    throw (RaiseException)v;
                }
                if (v.equals(MORE_CHAR_FINISHED)) {
                    clearReadConversion();
                    return EncodingUtils.ioEncStr(runtime, str, this);
                }
            }
        }

        NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
        bytes = 0;
        pos = 0;

        enc = readEncoding(runtime);
        cr = 0;

        if (siz == 0) siz = BUFSIZ;
        str = EncodingUtils.setStrBuf(runtime, str, siz);
        for (;;) {
            READ_CHECK(context);
            n = fread(context, str, bytes, siz - bytes);
            if (n == 0 && bytes == 0) {
                ((RubyString)str).resize(0);
                break;
            }
            bytes += n;
            ByteList strByteList = ((RubyString)str).getByteList();
            strByteList.setRealSize(bytes);
            if (cr != StringSupport.CR_BROKEN)
                pos += StringSupport.codeRangeScanRestartable(enc, strByteList.unsafeBytes(), strByteList.begin() + pos, strByteList.begin() + bytes, cr);
            if (bytes < siz) break;
            siz += BUFSIZ;
            ((RubyString)str).modify(BUFSIZ);
        }
        str = EncodingUtils.ioEncStr(runtime, str, this);
        ((RubyString)str).setCodeRange(cr);
        return str;
    }

    // io_bufread
    private int ioBufread(ThreadContext context, byte[] ptrBytes, int ptr, int len) {
        int offset = 0;
        int n = len;
        int c;

        if (!READ_DATA_PENDING()) {
            outer: while (n > 0) {
                again: while (true) {
                    c = readInternal(context, this, fd.chRead, ptrBytes, ptr + offset, n);
                    if (c == 0) break outer;
                    if (c < 0) {
                        if (waitReadable(context, fd))
                            continue again;
                        return -1;
                    }
                    break;
                }
                offset += c;
                if ((n -= c) <= 0) break outer;
            }
            return len - n;
        }

        Ruby runtime = context.runtime;

        while (n > 0) {
            c = readBufferedData(ptrBytes, ptr+offset, n);
            if (c > 0) {
                offset += c;
                if ((n -= c) <= 0) break;
            }
            checkClosed(runtime);
            if (fillbuf(context) < 0) {
                break;
            }
        }
        return len - n;
    }

    private static class BufreadArg {
        byte[] strPtrBytes;
        int strPtr;
        int len;
        OpenFile fptr;
    };

    static IRubyObject bufreadCall(ThreadContext context, BufreadArg p) {
        p.len = p.fptr.ioBufread(context, p.strPtrBytes, p.strPtr, p.len);
        return RubyBasicObject.UNDEF;
    }

    // io_fread
    public int fread(ThreadContext context, IRubyObject str, int offset, int size) {
        int len;
        BufreadArg arg = new BufreadArg();

        str = EncodingUtils.setStrBuf(context.runtime, str, offset + size);
        ByteList strByteList = ((RubyString)str).getByteList();
        arg.strPtrBytes = strByteList.unsafeBytes();
        arg.strPtr = strByteList.begin() + offset;
        arg.len = size;
        arg.fptr = this;
        // we don't support string locking
//        rb_str_locktmp_ensure(str, bufread_call, (VALUE)&arg);
        bufreadCall(context, arg);
        len = arg.len;
        // should be errno
        if (len < 0) throw context.runtime.newErrnoFromErrno(errno, pathv);
        return len;
    }

    public void ungetbyte(ThreadContext context, IRubyObject str) {
        int len = ((RubyString)str).size();

        if (rbuf.ptr == null) {
            int min_capa = IO_RBUF_CAPA_FOR();
            rbuf.off = 0;
            rbuf.len = 0;
//            #if SIZEOF_LONG > SIZEOF_INT
//            if (len > INT_MAX)
//                rb_raise(rb_eIOError, "ungetbyte failed");
//            #endif
            if (len > min_capa)
                rbuf.capa = (int)len;
            else
                rbuf.capa = min_capa;
            rbuf.ptr = new byte[rbuf.capa];
        }
        if (rbuf.capa < len + rbuf.len) {
            throw context.runtime.newIOError("ungetbyte failed");
        }
        if (rbuf.off < len) {
            System.arraycopy(rbuf.ptr, rbuf.off, rbuf.ptr, rbuf.capa - rbuf.len, rbuf.len);
            rbuf.off = rbuf.capa-rbuf.len;
        }
        rbuf.off-=(int)len;
        rbuf.len+=(int)len;
        ByteList strByteList = ((RubyString)str).getByteList();
        System.arraycopy(strByteList.unsafeBytes(), strByteList.begin(), rbuf.ptr, rbuf.off, len);
    }

    // io_getc
    public IRubyObject getc(ThreadContext context, Encoding enc) {
        Ruby runtime = context.runtime;
        int r, n, cr = 0;
        IRubyObject str;

        if (needsReadConversion()) {
            str = context.nil;
            Encoding read_enc = readEncoding(runtime);

            setBinmode();
            makeReadConversion(context, 0);

            while (true) {
                if (cbuf.len != 0) {
                    r = StringSupport.preciseLength(read_enc, cbuf.ptr, cbuf.off, cbuf.off + cbuf.len);
                    if (!StringSupport.MBCLEN_NEEDMORE_P(r))
                        break;
                    if (cbuf.len == cbuf.capa) {
                        throw runtime.newIOError("too long character");
                    }
                }

                if (moreChar(context) == MORE_CHAR_FINISHED) {
                    if (cbuf.len == 0) {
                        clearReadConversion();
                        return context.nil;
                    }
                /* return an unit of an incomplete character just before EOF */
                    str = RubyString.newString(runtime, cbuf.ptr, cbuf.off, 1, read_enc);
                    cbuf.off += 1;
                    cbuf.len -= 1;
                    if (cbuf.len == 0) clearReadConversion();
                    ((RubyString)str).setCodeRange(StringSupport.CR_BROKEN);
                    return str;
                }
            }
            if (StringSupport.MBCLEN_INVALID_P(r)) {
                r = read_enc.length(cbuf.ptr, cbuf.off, cbuf.off + cbuf.len);
                str = shiftCbuf(context, r, str);
                cr = StringSupport.CR_BROKEN;
            }
            else {
                str = shiftCbuf(context, StringSupport.MBCLEN_CHARFOUND_LEN(r), str);
                cr = StringSupport.CR_VALID;
                if (StringSupport.MBCLEN_CHARFOUND_LEN(r) == 1 && read_enc.isAsciiCompatible() &&
                        Encoding.isAscii(((RubyString)str).getByteList().get(0))) {
                    cr = StringSupport.CR_7BIT;
                }
            }
            str = EncodingUtils.ioEncStr(runtime, str, this);
            ((RubyString)str).setCodeRange(cr);
            return str;
        }

        NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
        if (fillbuf(context) < 0) {
            return context.nil;
        }
        if (enc.isAsciiCompatible() && Encoding.isAscii(rbuf.ptr[rbuf.off])) {
            str = RubyString.newString(runtime, rbuf.ptr, rbuf.off, 1);
            rbuf.off += 1;
            rbuf.len -= 1;
            cr = StringSupport.CR_7BIT;
        }
        else {
            r = StringSupport.preciseLength(enc, rbuf.ptr, rbuf.off, rbuf.off + rbuf.len);
            if (StringSupport.MBCLEN_CHARFOUND_P(r) &&
                    (n = StringSupport.MBCLEN_CHARFOUND_LEN(r)) <= rbuf.len) {
                str = RubyString.newString(runtime, rbuf.ptr, rbuf.off, n);
                rbuf.off += n;
                rbuf.len -= n;
                cr = StringSupport.CR_VALID;
            }
            else if (StringSupport.MBCLEN_NEEDMORE_P(r)) {
                str = RubyString.newString(runtime, rbuf.ptr, rbuf.off, rbuf.len);
                rbuf.len = 0;
                getc_needmore: while (true) {
                    if (fillbuf(context) != -1) {
                        ((RubyString)str).cat(rbuf.ptr[rbuf.off]);
                        rbuf.off++;
                        rbuf.len--;
                        ByteList strByteList = ((RubyString)str).getByteList();
                        r = StringSupport.preciseLength(enc, strByteList.unsafeBytes(), strByteList.getBegin(), strByteList.getBegin() + strByteList.length());
                        if (StringSupport.MBCLEN_NEEDMORE_P(r)) {
                            continue getc_needmore;
                        }
                        else if (StringSupport.MBCLEN_CHARFOUND_P(r)) {
                            cr = StringSupport.CR_VALID;
                        }
                    }
                    break;
                }
            }
            else {
                str = RubyString.newString(runtime, rbuf.ptr, rbuf.off, 1);
                rbuf.off++;
                rbuf.len--;
            }
        }
        if (cr == 0) cr = StringSupport.CR_BROKEN;
        str = EncodingUtils.ioEncStr(runtime, str, this);
        ((RubyString)str).setCodeRange(cr);
        return str;
    }

    // io_tell
    public long tell(ThreadContext context) {
        if (fd.chSeek != null) {
            try {
                return fd.chSeek.position();
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }

        return -1;
    }

    // io_unread
    public void unread(ThreadContext context)
    {
        Ruby runtime = context.runtime;
        long r, pos;
        int read_size;
        long i;
        int newlines = 0;
        long extra_max;
        byte[] pBytes;
        int p;
        byte[] bufBytes;
        int buf = 0;

        checkClosed(runtime);
        if (rbuf.len == 0 || (mode & DUPLEX) != 0) {
            return;
        }

        errno = null;
        // TODO...only for win32?
//        if (!rb_w32_fd_is_text(fptr->fd)) {
//            r = lseek(fptr->fd, -fptr->rbuf.len, SEEK_CUR);
//            if (r < 0 && errno) {
//                if (errno == ESPIPE)
//                    fptr->mode |= FMODE_DUPLEX;
//                return;
//            }
//
//            fptr->rbuf.off = 0;
//            fptr->rbuf.len = 0;
//            return;
//        }

        pos = lseek(context, 0, SEEK_CUR);
        if (pos < 0 && errno != null) {
            if (errno == Errno.ESPIPE)
                mode |= DUPLEX;
            return;
        }

    /* add extra offset for removed '\r' in rbuf */
        extra_max = (long)(pos - rbuf.len);
        pBytes = rbuf.ptr;
        p = rbuf.off;

    /* if the end of rbuf is '\r', rbuf doesn't have '\r' within rbuf.len */
        if (rbuf.ptr[rbuf.capa - 1] == '\r') {
        newlines++;
    }

        for (i = 0; i < rbuf.len; i++) {
            if (pBytes[p] == '\n') newlines++;
            if (extra_max == newlines) break;
            p++;
        }

        bufBytes = new byte[rbuf.len + newlines];
        while (newlines >= 0) {
            r = lseek(context, pos - rbuf.len - newlines, SEEK_SET);
            if (newlines == 0) break;
            if (r < 0) {
                newlines--;
                continue;
            }
            read_size = readInternal(context, this, fd.chRead, bufBytes, buf, rbuf.len + newlines);
            if (read_size < 0) {
                throw runtime.newErrnoFromErrno(errno, pathv);
            }
            if (read_size == rbuf.len) {
                lseek(context, r, SEEK_SET);
                break;
            }
            else {
                newlines--;
            }
        }
        rbuf.off = 0;
        rbuf.len = 0;
        return;
    }

    // io_fwrite
    public long fwrite(ThreadContext context, IRubyObject str, boolean nosync) {
//        #ifdef _WIN32
//        if (fptr->mode & FMODE_TTY) {
//            long len = rb_w32_write_console(str, fptr->fd);
//            if (len > 0) return len;
//        }
//        #endif
        str = doWriteconv(context, str);
        ByteList strByteList = ((RubyString)str).getByteList();
        return binwrite(context, str, strByteList.unsafeBytes(), strByteList.begin(), strByteList.length(), nosync);
    }

    // do_writeconv
    public IRubyObject doWriteconv(ThreadContext context, IRubyObject str)
    {
        if (needsWriteConversion(context)) {
            IRubyObject common_encoding = context.nil;
            setBinmode();

            makeWriteConversion(context);

            if (writeconv != null) {
                int fmode = mode;
                if (!writeconvAsciicompat.isNil())
                    common_encoding = writeconvAsciicompat;
                else if (EncodingUtils.MODE_BTMODE(fmode, EncodingUtils.DEFAULT_TEXTMODE,0,1) != 0 && !((RubyString)str).getEncoding().isAsciiCompatible()) {
                    throw context.runtime.newArgumentError("ASCII incompatible string written for text mode IO without encoding conversion: %s" + ((RubyString)str).getEncoding().toString());
                }
            }
            else {
                if (encs.enc2 != null)
                    common_encoding = context.runtime.getEncodingService().convertEncodingToRubyEncoding(encs.enc2);
                else if (encs.enc != EncodingUtils.ascii8bitEncoding(context.runtime))
                    common_encoding = context.runtime.getEncodingService().convertEncodingToRubyEncoding(encs.enc);
            }

            if (!common_encoding.isNil()) {
                str = EncodingUtils.rbStrEncode(context, str, common_encoding, writeconvPreEcflags, writeconvPreEcopts);
            }

            if (writeconv != null) {
                ((RubyString)str).setValue(
                        EncodingUtils.econvStrConvert(context, writeconv, ((RubyString)str).getByteList(), EConvFlags.PARTIAL_INPUT));
            }
        }
//        #if defined(RUBY_TEST_CRLF_ENVIRONMENT) || defined(_WIN32)
//        #define fmode (fptr->mode)
//        else if (MODE_BTMODE(DEFAULT_TEXTMODE,0,1)) {
//        if ((fptr->mode & FMODE_READABLE) &&
//                !(fptr->encs.ecflags & ECONV_NEWLINE_DECORATOR_MASK)) {
//            setmode(fptr->fd, O_BINARY);
//        }
//        else {
//            setmode(fptr->fd, O_TEXT);
//        }
//        if (!rb_enc_asciicompat(rb_enc_get(str))) {
//            rb_raise(rb_eArgError, "ASCII incompatible string written for text mode IO without encoding conversion: %s",
//                    rb_enc_name(rb_enc_get(str)));
//        }
//    }
//        #undef fmode
//        #endif
        return str;
    }

    private static class BinwriteArg {
        OpenFile fptr;
        IRubyObject str;
        byte[] ptrBytes;
        int ptr;
        int length;
    }

    // io_binwrite
    public long binwrite(ThreadContext context, IRubyObject str, byte[] ptrBytes, int ptr, int len, boolean nosync)
    {
        int n, r, offset = 0;
        boolean isSync = !nosync && (mode & (SYNC|TTY)) != 0;

    /* don't write anything if current thread has a pending interrupt. */
        context.pollThreadEvents();

        if ((n = len) <= 0) return n;
        if (wbuf.ptr == null && !isSync) {
            wbuf.off = 0;
            wbuf.len = 0;
            wbuf.capa = IO_WBUF_CAPA_MIN;
            wbuf.ptr = new byte[wbuf.capa];
//            write_lock = new ReentrantReadWriteLock();
            // ???
//            rb_mutex_allow_trap(fptr->write_lock, 1);
        }
        if (isSync ||
                (wbuf.ptr != null && wbuf.capa <= wbuf.len + len)) {
            BinwriteArg arg = new BinwriteArg();

	/*
	 * xxx: use writev to avoid double write if available
	 * writev may help avoid context switch between "a" and "\n" in
	 * STDERR.puts "a" [ruby-dev:25080] (rebroken since native threads
	 * introduced in 1.9)
	 */
            if (wbuf.len != 0 && wbuf.len+len <= wbuf.capa) {
                if (wbuf.capa < wbuf.off+wbuf.len+len) {
                    System.arraycopy(wbuf.ptr, wbuf.off, wbuf.ptr, 0, wbuf.len);
                    wbuf.off = 0;
                }
                System.arraycopy(ptrBytes, ptr + offset, wbuf.ptr, wbuf.off + wbuf.len, len);
                wbuf.len += (int)len;
                n = 0;
            }
            if (fflush(context.runtime) < 0)
                return -1L;
            if (n == 0)
                return len;

            checkClosed(context.runtime);
            arg.fptr = this;
            arg.str = str;
            retry: while (true) {
                arg.ptrBytes = ptrBytes;
                arg.ptr = ptr + offset;
                arg.length = n;
                if (write_lock != null) {
                    // FIXME: not interruptible by Ruby
    //                r = rb_mutex_synchronize(fptr->write_lock, io_binwrite_string, (VALUE)&arg);
                    write_lock.writeLock().lock();
                    try {
                        r = binwriteString(context, arg);
                    } finally {
                        write_lock.writeLock().unlock();
                    }
                }
                else {
                    int l = writableLength(n);
                    r = writeInternal(context, fd.chWrite, ptrBytes, ptr + offset, l);
                }
                /* xxx: other threads may modify given string. */
                if (r == n) return len;
                if (0 <= r) {
                    offset += r;
                    n -= r;
                    errno = Errno.EAGAIN;
                }
                if (waitWritable(context.runtime)) {
                    checkClosed(context.runtime);
                    if (offset < len)
                    continue retry;
                }
                return -1L;
            }
        }

        if (wbuf.off != 0) {
            if (wbuf.len != 0)
                System.arraycopy(wbuf.ptr, wbuf.off, wbuf.ptr, 0, wbuf.len);
            wbuf.off = 0;
        }
        System.arraycopy(ptrBytes, ptr + offset, wbuf.ptr, wbuf.off + wbuf.len, len);
        wbuf.len += (int)len;
        return len;
    }

    // io_binwrite_string
    static int binwriteString(ThreadContext context, BinwriteArg arg) {
        BinwriteArg p = arg;
        int l = p.fptr.writableLength(p.length);
        return p.fptr.writeInternal2(context.runtime, p.fptr.fd.chWrite, p.ptrBytes, p.ptr, l);
    }

    public static class InternalWriteStruct {
        InternalWriteStruct(WritableByteChannel fd, byte[] bufBytes, int buf, int count) {
            this.fd = fd;
            this.bufBytes = bufBytes;
            this.buf = buf;
            this.capa = count;
        }

        public WritableByteChannel fd;
        public final byte[] bufBytes;
        public final int buf;
        public int capa;
    }

    // rb_write_internal
    public static int writeInternal(ThreadContext context, WritableByteChannel fd, byte[] bufBytes, int buf, int count) {
        InternalWriteStruct iis = new InternalWriteStruct(fd, bufBytes, buf, count);

        try {
            return context.getThread().executeTask(context, iis, writeTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("IO operation interrupted");
        }
    }

    // rb_write_internal2 (no GVL version...we just don't use executeTask as above.
    int writeInternal2(Ruby runtime, WritableByteChannel fd, byte[] bufBytes, int buf, int count) {
        InternalWriteStruct iis = new InternalWriteStruct(fd, bufBytes, buf, count);

        return writeTaskBody(runtime, iis);
    }

    public Channel getFd() {
        return fd.ch;
    }

    public ReadableByteChannel getFdRead() {
        return fd.chRead;
    }

    public WritableByteChannel getFdWrite() {
        return fd.chWrite;
    }

    public SeekableByteChannel getFdSeek() {
        return fd.chSeek;
    }

    public SelectableChannel getFdSelect() {
        return fd.chSelect;
    }

    public FileChannel getFdFile() {
        return fd.chFile;
    }

    IRubyObject finishWriteconv(Ruby runtime, boolean noalloc) {
        byte[] dsBytes;
        int ds, de;
        Ptr dpPtr = new Ptr();
        EConvResult res;

        if (wbuf.ptr == null) {
            byte[] buf = new byte[1024];
            long r;

            res = EConvResult.DestinationBufferFull;
            while (res == EConvResult.DestinationBufferFull) {
                dsBytes = buf;
                ds = dpPtr.p = 0;
                de = buf.length;
                dpPtr.p = 0;
                res = writeconv.convert(null, null, 0, dsBytes, dpPtr, de, 0);
                outer: while ((dpPtr.p-ds) != 0) {
                    retry: while (true) {
                        if (write_lock != null && write_lock.isWriteLockedByCurrentThread())
                            r = writeInternal2(runtime, fd.chWrite, dsBytes, ds, dpPtr.p-ds);
                        else
                            r = writeInternal(runtime.getCurrentContext(), fd.chWrite, dsBytes, ds, dpPtr.p - ds);
                        if (r == dpPtr.p-ds)
                            break outer;
                        if (0 <= r) {
                            ds += r;
                        }
                        if (waitWritable(runtime)) {
                            if (fd == null)
                                return noalloc ? runtime.getTrue() : runtime.newIOError("closed stream").getException();
                            continue retry;
                        }
                        break retry;
                    }
                    return noalloc ? runtime.getTrue() : RubyFixnum.newFixnum(runtime, (errno == null) ? 0 : errno.longValue());
                }
                if (res == EConvResult.InvalidByteSequence ||
                        res == EConvResult.IncompleteInput ||
                        res == EConvResult.UndefinedConversion) {
                    return noalloc ? runtime.getTrue() : EncodingUtils.makeEconvException(runtime, writeconv).getException();
                }
            }

            return runtime.getNil();
        }

        res = EConvResult.DestinationBufferFull;
        while (res == EConvResult.DestinationBufferFull) {
            if (wbuf.len == wbuf.capa) {
                if (fflush(runtime) < 0)
                    return noalloc ? runtime.getTrue() : runtime.newFixnum(errno == null ? 0 : errno.longValue());
            }

            dsBytes = wbuf.ptr;
            ds = dpPtr.p = wbuf.off + wbuf.len;
            de = wbuf.capa;
            res = writeconv.convert(null, null, 0, dsBytes, dpPtr, de, 0);
            wbuf.len += (int)(dpPtr.p - ds);
            if (res == EConvResult.InvalidByteSequence ||
                    res == EConvResult.IncompleteInput ||
                    res == EConvResult.UndefinedConversion) {
                return noalloc ? runtime.getTrue() : EncodingUtils.makeEconvException(runtime, writeconv).getException();
            }
        }
        return runtime.getNil();
    }

    private static int writeTaskBody(Ruby runtime, InternalWriteStruct iis) {
        // TODO: make nonblocking (when possible? MRI doesn't seem to...)
        // FIXME: inefficient to recreate ByteBuffer every time
        try {
            ByteBuffer buffer = ByteBuffer.wrap(iis.bufBytes, iis.buf, iis.capa);
            int written = iis.fd.write(buffer);

            return written;
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    // rb_io_set_nonblock
    public void setNonblock(Ruby runtime) {
        // Not all NIO channels are non-blocking, so we need to maintain this flag
        // and make those channels act likenon-blocking
        nonblock = true;

        if (fd.chSelect != null) {
            try {
                fd.chSelect.configureBlocking(false);
                return;
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        }
    }

    public static String oflagsModestr(int oflags) {
        // TODO
        return null;
    }

    // MRI: check_tty
    public void checkTTY() {
        // TODO: native descriptors? Is this only used for stdio?
        if (stdioIn != null || stdioOut != null) {
            mode |= TTY | DUPLEX;
        }
    }

    public boolean isBOM() {
        return (mode & SETENC_BY_BOM) != 0;
    }

    public void setBOM(boolean bom) {
        if (bom) {
            mode |= SETENC_BY_BOM;
        } else {
            mode &= ~SETENC_BY_BOM;
        }
    }

    public boolean isStdio() {
        return stdioIn != null || stdioOut != null;
    }

    @Deprecated
    public Stream getMainStream() {
        return mainStream;
    }

    @Deprecated
    public Stream getMainStreamSafe() throws BadDescriptorException {
        Stream stream = mainStream;
        if (stream == null) throw new BadDescriptorException();
        return stream;
    }

    @Deprecated
    public void setMainStream(Stream mainStream) {
        this.mainStream = mainStream;
        setFD(mainStream.getChannel());
    }

    @Deprecated
    public Stream getPipeStream() {
        return pipeStream;
    }

    @Deprecated
    public Stream getPipeStreamSafe() throws BadDescriptorException {
        Stream stream = pipeStream;
        if (stream == null) throw new BadDescriptorException();
        return stream;
    }

    @Deprecated
    public void setPipeStream(Stream pipeStream) {
        this.pipeStream = pipeStream;
    }

    @Deprecated
    public Stream getWriteStream() {
        return pipeStream == null ? mainStream : pipeStream;
    }

    @Deprecated
    public Stream getWriteStreamSafe() throws BadDescriptorException {
        Stream stream = pipeStream == null ? mainStream : pipeStream;
        if (stream == null) throw new BadDescriptorException();
        return stream;
    }

    @Deprecated
    private Stream mainStream;
    @Deprecated
    private Stream pipeStream;
}

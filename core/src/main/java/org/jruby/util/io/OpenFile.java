package org.jruby.util.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jnr.constants.platform.Errno;
import jnr.constants.platform.OpenFlags;

import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jruby.Finalizable;
import org.jruby.Ruby;
import org.jruby.RubyArgsFile;
import org.jruby.RubyBignum;
import org.jruby.RubyEncoding;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;

import static org.jruby.util.StringSupport.*;

public class OpenFile implements Finalizable {

    // RB_IO_FPTR_NEW, minus fields that Java already initializes the same way
    public OpenFile(IRubyObject nil) {
        runtime = nil.getRuntime();
        writeconvAsciicompat = null;
        writeconvPreEcopts = nil;
        encs.ecopts = nil;
        posix = new PosixShim(runtime);
    }

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
    public static final int EXCLUSIVE          = 0x00000400;
    public static final int TRUNC              = 0x00000800;
    public static final int TEXTMODE           = 0x00001000;
    public static final int SETENC_BY_BOM      = 0x00100000;
    public static final int TMPFILE            = 0x00410000;
    public static final int PREP         = (1<<16);

    public static final int SYNCWRITE = SYNC | WRITABLE;

    public static final int PIPE_BUF = 512; // value of _POSIX_PIPE_BUF from Mac OS X 10.9
    public static final int BUFSIZ = 1024; // value of BUFSIZ from Mac OS X 10.9 stdio.h

    public void ascii8bitBinmode(Ruby runtime) {
        if (readconv != null) {
            readconv.close();
            readconv = null;
        }
        if (writeconv != null) {
            writeconv.close();
            writeconv = null;
        }
        setBinmode();
        clearTextMode();
        // TODO: Windows
        //SET_BINARY_MODE_WITH_SEEK_CUR()
        encs.enc = EncodingUtils.ascii8bitEncoding(runtime);
        encs.enc2 = null;
        encs.ecflags = 0;
        encs.ecopts = runtime.getNil();
        clearCodeConversion();
    }

    public void checkReopenSeek(ThreadContext context, Ruby runtime, long pos) {
        if (seek(context, pos, PosixShim.SEEK_SET) == -1 && errno() != null) {
            throw runtime.newErrnoFromErrno(errno(), getPath());
        }
    }

    public static interface Finalizer {
        public void finalize(Ruby runtime, OpenFile fptr, boolean noraise);
    }

    private ChannelFD fd;
    private int mode;
    private long pid = -1;
    private Process process;
    private int lineno;
    private String pathv;
    private Finalizer finalizer;
    public Closeable stdio_file;
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
    public Encoding writeconvAsciicompat;
    public int writeconvPreEcflags;
    public IRubyObject writeconvPreEcopts;
    public boolean writeconvInitialized;

    public volatile ReentrantReadWriteLock write_lock;
    private final ReentrantLock lock = new ReentrantLock();

    public final Buffer wbuf = new Buffer(), rbuf = new Buffer(), cbuf = new Buffer();

    public RubyIO tiedIOForWriting;

    private boolean nonblock = false;

    public final PosixShim posix;

    private final Ruby runtime;

    protected volatile Set<RubyThread> blockingThreads;

    public void clearStdio() {
        stdio_file = null;
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
            checkClosed();
        }
    }

    public boolean IS_PREP_STDIO() {
        return (mode & PREP) == PREP;
    }

    public void setFD(ChannelFD fd) {
        this.fd = fd;
    }

    public void setChannel(Channel fd) {
        this.fd = new ChannelFD(fd, runtime.getPosix(), runtime.getFilenoUtil());
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

    // MRI: rb_io_fmode_oflags
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
        if ((fmode & EXCLUSIVE) != 0) oflags |= ModeFlags.EXCL;

        return oflags;
    }

    // MRI: rb_io_oflags_modestr
    public static String ioOflagsModestr(Ruby runtime, int oflags) {
        if ((oflags & OpenFlags.O_EXLOCK.intValue()) != 0) {
            throw runtime.newArgumentError("exclusive access mode is not supported");
        }
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
        if ((fmode & OpenFile.EXCLUSIVE) != 0) {
            oflags |= OpenFlags.O_EXCL.intValue();
        }
        if (OpenFlags.O_BINARY.defined()) {
            if ((fmode & OpenFile.BINMODE) != 0) {
                oflags |= OpenFlags.O_BINARY.intValue();
            }
        }

        return oflags;
    }

    public static int ioModestrFmode(Ruby runtime, String modestr) {
        int fmode = 0;
        char[] mChars = modestr.toCharArray(), pChars = null;
        int m = 0, p = 0;

        if (mChars.length == 0) throw runtime.newArgumentError("invalid access mode " + modestr);

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
                case 'x':
                    if (mChars[0] != 'w') {
                        throw runtime.newArgumentError("invalid access mode " + modestr);
                    }
                    fmode |= OpenFile.EXCLUSIVE;
                    break;
                case ':':
                    pChars = mChars;
                    p = m;
                    if ((fmode & OpenFile.BINMODE) != 0 && (fmode & OpenFile.TEXTMODE) != 0)
                        throw runtime.newArgumentError("invalid access mode " + modestr);
                    break loop;
                default:
                    throw runtime.newArgumentError("invalid access mode " + modestr);
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
        if (OpenFlags.O_BINARY.defined() && (oflags & OpenFlags.O_BINARY.intValue()) != 0) {
            return b;
        }
        return a;
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
    public void checkCharReadable(ThreadContext context) {
        checkClosed();

        if ((mode & READABLE) == 0) {
            throw runtime.newIOError("not opened for reading");
        }

        if (wbuf.len != 0) {
            if (io_fflush(context) < 0) {
                throw runtime.newErrnoFromErrno(posix.getErrno(), "error flushing");
            }
        }
        if (tiedIOForWriting != null) {
            OpenFile wfptr;
            wfptr = tiedIOForWriting.getOpenFileChecked();
            if (wfptr.io_fflush(context) < 0) {
                throw runtime.newErrnoFromErrno(wfptr.posix.getErrno(), wfptr.getPath());
            }
        }
    }

    // rb_io_check_byte_readable
    public void checkByteReadable(ThreadContext context) {
        checkCharReadable(context);
        if (READ_CHAR_PENDING()) {
            throw runtime.newIOError("byte oriented read for character buffered IO");
        }
    }

    // rb_io_check_readable
    public void checkReadable(ThreadContext context) {
        checkByteReadable(context);
    }

    // io_fflush
    public int io_fflush(ThreadContext context) {
        boolean locked = lock();
        try {
            checkClosed();

            if (wbuf.len == 0) return 0;

            checkClosed();

            while (wbuf.len > 0 && flushBuffer() != 0) {
                if (!waitWritable(context)) return -1;
                checkClosed();
            }
        } finally {
            if (locked) unlock();
        }

        return 0;
    }

    // rb_io_wait_writable
    public boolean waitWritable(ThreadContext context, long timeout) {
        boolean locked = lock();
        try {
            if (posix.getErrno() == null) return false;

            checkClosed();

            switch (posix.getErrno()) {
                case EINTR:
                    //            case ERESTART: // not defined in jnr-constants
                    runtime.getCurrentContext().pollThreadEvents();
                    return true;
                case EAGAIN:
                case EWOULDBLOCK:
                    ready(runtime, context.getThread(), SelectExecutor.WRITE_CONNECT_OPS, timeout);
                    return true;
                default:
                    return false;
            }
        } finally {
            if (locked) unlock();
        }
    }

    // rb_io_wait_writable
    public boolean waitWritable(ThreadContext context) {
        return waitWritable(context, 0);
    }

    // rb_io_wait_readable
    public boolean waitReadable(ThreadContext context, long timeout) {
        boolean locked = lock();
        try {
            if (posix.getErrno() == null) return false;

            checkClosed();

            switch (posix.getErrno()) {
                case EINTR:
                    //            case ERESTART: // not defined in jnr-constants
                    runtime.getCurrentContext().pollThreadEvents();
                    return true;
                case EAGAIN:
                case EWOULDBLOCK:
                    ready(runtime, context.getThread(), SelectionKey.OP_READ, timeout);
                    return true;
                default:
                    return false;
            }
        } finally {
            if (locked) unlock();
        }
    }

    // rb_io_wait_readable
    public boolean waitReadable(ThreadContext context) {
        return waitReadable(context, -1);
    }

    /**
     * Wait until the channel is available for the given operations or the timeout expires.
     *
     * @see org.jruby.RubyThread#select(java.nio.channels.Channel, OpenFile, int, long)
     *
     * @param runtime
     * @param ops
     * @param timeout
     * @return
     */
    public boolean ready(Ruby runtime, RubyThread thread, int ops, long timeout) {
        boolean locked = lock();
        try {
            if (fd.chSelect != null) {
                int realOps = ops & fd.chSelect.validOps();

                if ((realOps & SelectionKey.OP_WRITE) != (ops & SelectionKey.OP_WRITE)) {
                    // MRI or poll or select appears to return ready for write select on a read-only channel
                    return true;
                }

                return thread.select(fd.chSelect, this, realOps, timeout);

            } else if (fd.chSeek != null) {
                return fd.chSeek.position() != -1
                        && fd.chSeek.size() != -1
                        && fd.chSeek.position() < fd.chSeek.size();
            }

            return false;
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        } finally {
            if (locked) unlock();
        }
    }

    /**
     * Like {@link OpenFile#ready(org.jruby.Ruby, org.jruby.RubyThread, int, long)} but returns a result
     * immediately.
     *
     * @param context
     * @return
     */
    public boolean readyNow(ThreadContext context) {
        return ready(context.runtime, context.getThread(), SelectionKey.OP_READ, 0);
    }

    // io_flush_buffer
    public int flushBuffer() {
        if (write_lock != null) {
            write_lock.writeLock().lock();
            try {
                return flushBufferAsync2();
            } finally {
                write_lock.writeLock().unlock();
            }
        }
        return flushBufferAsync2();
    }

    // io_flush_buffer_async2
    public int flushBufferAsync2() {
        // GVL-free call to io_flush_buffer_sync2 here in MRI

        return flushBufferSync2();

        // logic after here was to interpret the retval of rb_thread_call_without_gvl2
    }

    // io_flush_buffer_sync2
    private int flushBufferSync2() {
        int result = flushBufferSync();

        return result;
//        return result == 0 ? 1 : result;
    }

    // io_flush_buffer_sync
    private int flushBufferSync() {
        int l = writableLength(wbuf.len);
        int r = posix.write(fd, wbuf.ptr, wbuf.off, l, nonblock);

        if (wbuf.len <= r) {
            wbuf.off = 0;
            wbuf.len = 0;
            return 0;
        }
        if (0 <= r) {
            wbuf.off += r;
            wbuf.len -= r;
            posix.setErrno(Errno.EAGAIN);
        }
        return -1;
    }

    // io_writable_length
    private int writableLength(int l) {
        // We don't use wsplit mode, so we just pass length back directly.
//        if (PIPE_BUF < l &&
//                // we should always assume other threads, so we don't use rb_thread_alone
////                !rb_thread_alone() &&
//                wsplit()) {
//            l = PIPE_BUF;
//        }
        return l;
    }

    /**
     * wsplit mode selects a smaller write size based on the internal buffer of
     * things like pipes, in order to help guarantee it will not block when
     * emptying our write buffer. This must be guaranteed to allow MRI to re-try
     * flushing the rest of the buffer with the GVL released, which happens when
     * flushBufferSync above produces EAGAIN.
     *
     * In JRuby, where we don't have to release a lock, we skip this logic and
     * always just let writes do what writes do.
     *
     * MRI: wsplit_p
     * @return
     */
    // wsplit_p
    private boolean wsplit()
    {
        int r;

//        if ((mode & WSPLIT_INITIALIZED) == 0) {
////            struct stat buf;
//            if (fd.chFile == null
////            if (fstat(fptr->fd, &buf) == 0 &&
////                    !S_ISREG(buf.st_mode)
////                    && (r = fcntl(fptr->fd, F_GETFL)) != -1 &&
////                    !(r & O_NONBLOCK)
//            ) {
//                mode |= WSPLIT;
//            }
//            mode |= WSPLIT_INITIALIZED;
//        }
        return (mode & WSPLIT) != 0;
    }

    // io_seek
    public long seek(ThreadContext context, long offset, int whence) {
        boolean locked = lock();
        try {
            flushBeforeSeek(context);
            return posix.lseek(fd, offset, whence);
        } finally {
            if (locked) unlock();
        }
    }

    // flush_before_seek
    private void flushBeforeSeek(ThreadContext context) {
        boolean locked = lock();
        try {
            if (io_fflush(context) < 0)
                throw context.runtime.newErrnoFromErrno(posix.getErrno(), "");
            unread(context);
            posix.setErrno(null);
        } finally {
            if (locked) unlock();
        }
    }

    public void checkWritable(ThreadContext context) {
        boolean locked = lock();
        try {
            checkClosed();
            if ((mode & WRITABLE) == 0) {
                throw context.runtime.newIOError("not opened for writing");
            }
            if (rbuf.len != 0) {
                unread(context);
            }
        } finally {
            if (locked) unlock();
        }
    }

    public void checkClosed() {
        if (fd == null) {
            throw runtime.newIOError(RubyIO.CLOSED_STREAM_MSG);
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
    }

    public void clearTextMode() {
        mode &= ~TEXTMODE;
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

    public boolean isDuplex() {
        return (mode & DUPLEX) != 0;
    }

    public boolean isReadBuffered() {
        return READ_DATA_BUFFERED();
    }

    public boolean isWriteBuffered() {
        return false;
    }

    public void setSync(boolean sync) {
        boolean locked = lock();
        try {
            if (sync) {
                mode = mode | SYNC;
            } else {
                mode = mode & ~SYNC;
            }
        } finally {
            if (locked) unlock();
        }
    }

    public boolean isSync() {
        return (mode & (SYNC | TTY)) != 0;
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
        if (pid != -1) return pid;

        return ShellLauncher.getPidFromProcess(process);
    }

    public void setPid(long pid) {
        this.pid = pid;
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
        return (mode & PREP) == 0;
    }

    public void setAutoclose(boolean autoclose) {
        boolean locked = lock();
        try {
            if (!autoclose)
                mode |= PREP;
            else
                mode &= ~PREP;
        } finally {
            if (locked) unlock();
        }
    }

    public Finalizer getFinalizer() {
        return finalizer;
    }

    public void setFinalizer(Finalizer finalizer) {
        this.finalizer = finalizer;
    }

    public void cleanup(Ruby runtime, boolean noraise) {
        boolean locked = lock();
        try {
            if (finalizer != null) {
                finalizer.finalize(runtime, this, noraise);
            } else {
                finalize(runtime.getCurrentContext(), noraise);
            }
        } finally {
            if (locked) unlock();
        }
    }

    @Deprecated // no longer used
    public static final Finalizer PIPE_FINALIZE = new Finalizer() {
        @Override
        public void finalize(Ruby runtime, OpenFile fptr, boolean noraise) {
            if (!Platform.IS_WINDOWS) { // #if !defined(HAVE_FORK) && !defined(_WIN32)
                int status = 0;
                if (fptr.stdio_file != null) {
                    // unsure how to do this
//                    status = pclose(fptr->stdio_file);
                    fptr.posix.close(fptr.stdio_file);
                }
                fptr.setFD(null);
                fptr.stdio_file = null;
                // no status from above, so can't really do this
//                runtime.getCurrentContext().setLastExitStatus();
            } else {
                fptr.finalize(runtime.getCurrentContext(), noraise);
            }
//            pipe_del_fptr(fptr);
        }
    };

    public void finalize() {
        if (fd != null && isAutoclose()) finalize(runtime.getCurrentContext(), true);
    }

    public void finalize(ThreadContext context, boolean noraise) {
        finalizeFlush(context, noraise);
    }

    public void finalizeFlush(ThreadContext context, boolean noraise) {
        IRubyObject err = context.nil;
        ChannelFD fd = this.fd();
        Closeable stdio_file = this.stdio_file;

        if (writeconv != null) {
            if (write_lock != null && !noraise) {
                // TODO: interruptible version
                write_lock.writeLock().lock();
                try {
                    finishWriteconv(context, noraise);
                } finally {
                    write_lock.writeLock().unlock();
                }
            }
            else {
                err = finishWriteconv(context, noraise);
            }
        }
        if (wbuf.len != 0) {
            if (noraise) {
                if (flushBufferSync() < 0 && err == context.nil)
                    err = context.tru;
            }
            else {
                if (io_fflush(context) < 0 && err == context.nil) {
                    err = RubyFixnum.newFixnum(runtime, posix.getErrno() == null ? 0 : posix.getErrno().longValue());
                }
            }
        }

        this.fd = null;
        this.clearStdio();
        mode &= ~(READABLE|WRITABLE);

        if (IS_PREP_STDIO() || isStdio()) {
	        /* need to keep FILE objects of stdin, stdout and stderr */
        } else if (stdio_file != null) {
	        /* stdio_file is deallocated anyway
             * even if fclose failed.  */
            if (posix.close(stdio_file) < 0 && err.isNil())
                err = noraise ? context.tru : RubyNumeric.int2fix(runtime, posix.getErrno().intValue());
        } else if (fd != null) {
            /* fptr->fd may be closed even if close fails.
             * POSIX doesn't specify it.
             * We assumes it is closed.  */
            if ((posix.close(fd) < 0) && err.isNil())
                err = noraise ? context.tru : runtime.newFixnum(posix.getErrno().intValue());
        }

        if (!err.isNil() && !noraise) {
            if (err instanceof RubyFixnum || err instanceof RubyBignum) {
                posix.setErrno(Errno.valueOf(RubyNumeric.num2int(err)));
                throw runtime.newErrnoFromErrno(posix.getErrno(), pathv);
            } else {
                throw ((RubyException)err).toThrowable();
            }
        }
    }

    // MRI: NEED_READCONV
    public boolean needsReadConversion() {
        return Platform.IS_WINDOWS ?
                (encs.enc2 != null || (encs.ecflags & ~EConvFlags.CRLF_NEWLINE_DECORATOR) != 0) || isTextMode()
                :
                (encs.enc2 != null || NEED_NEWLINE_DECORATOR_ON_READ());
    }

    // MRI: NEED_WRITECONV
    public boolean needsWriteConversion(ThreadContext context) {
        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();

        return Platform.IS_WINDOWS ?
                ((encs.enc != null && encs.enc != ascii8bit) || (encs.ecflags & ((EConvFlags.DECORATOR_MASK & ~EConvFlags.CRLF_NEWLINE_DECORATOR)|EConvFlags.STATEFUL_DECORATOR_MASK)) != 0)
                :
                ((encs.enc != null && encs.enc != ascii8bit) || NEED_NEWLINE_DECORATOR_ON_WRITE() || (encs.ecflags & (EConvFlags.DECORATOR_MASK|EConvFlags.STATEFUL_DECORATOR_MASK)) != 0);
    }

    // MRI: make_readconv
    public void makeReadConversion(ThreadContext context, int size) {
        if (readconv == null) {
            int ecflags;
            IRubyObject ecopts;
            byte[] sname, dname;
            ecflags = encs.ecflags & ~EConvFlags.NEWLINE_DECORATOR_WRITE_MASK;
            ecopts = encs.ecopts;
            if (encs.enc2 != null) {
                sname = encs.enc2.getName();
                dname = encs.enc.getName();
            }
            else {
                sname = dname = EMPTY_BYTE_ARRAY;
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
            writeconvAsciicompat = null;
        }
        else {
            byte[] senc;
            Encoding sEncoding;

            enc = encs.enc2 != null ? encs.enc2 : encs.enc;
            Encoding tmpEnc = EncodingUtils.econvAsciicompatEncoding(enc);
            senc = tmpEnc == null ? null : tmpEnc.getName();
            sEncoding = tmpEnc == null ? null : tmpEnc;
            if (sEncoding == null && (encs.ecflags & EConvFlags.STATEFUL_DECORATOR_MASK) == 0) {
                /* single conversion */
                writeconvPreEcflags = ecflags;
                writeconvPreEcopts = ecopts;
                writeconv = null;
                writeconvAsciicompat = null;
            }
            else {
                /* double conversion */
                writeconvPreEcflags = ecflags & ~EConvFlags.STATEFUL_DECORATOR_MASK;
                writeconvPreEcopts = ecopts;
                if (sEncoding != null) {
                    denc = enc.getName();
                    writeconvAsciicompat = sEncoding;
                }
                else {
                    senc = denc = EMPTY_BYTE_ARRAY;
                    writeconvAsciicompat = enc;
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

    private static final byte[] EMPTY_BYTE_ARRAY = ByteList.NULL_ARRAY;

    // MRI: appendline
    public int appendline(ThreadContext context, int delim, final ByteList[] strp, final int[] lp) {
        ByteList str = strp[0];
        int limit = lp[0];

        if (needsReadConversion()) {
            SET_BINARY_MODE();
            makeReadConversion(context);
            do {
                int p, e;
                int searchlen = READ_CHAR_PENDING_COUNT();
                if (searchlen > 0) {
                    byte[] pBytes = READ_CHAR_PENDING_PTR();
                    p = READ_CHAR_PENDING_OFF();
                    if (0 < limit && limit < searchlen) searchlen = limit;
                    e = memchr(pBytes, p, delim, searchlen);
                    if (e != -1) {
                        int len = e - p + 1;
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
                int last;

                if (limit > 0 && pending > limit) pending = limit;
                int e = memchr(pBytes, p, delim, pending);
                if (e != -1) pending = e - p + 1;
                if (str != null) {
                    last = str.getRealSize();
                    str.ensure(last + pending);
                }
                else {
                    last = 0;
                    strp[0] = str = new ByteList(pending);
                }
                readBufferedData(str.getUnsafeBytes(), last + str.getBegin(), pending); /* must not fail */
                str.setRealSize(last + pending);
                limit -= pending;
                if (e != -1) {
                    lp[0] = limit;
                    return delim;
                }
                if (limit == 0) {
                    lp[0] = limit;
                    return str.get(str.getRealSize() - 1) & 0xFF;
                }
            }
            READ_CHECK(context);
        } while (fillbuf(context) >= 0);
        lp[0] = limit;
        return EOF;
    }

    private static int memchr(byte[] pBytes, int p, int delim, int length) {
        for (int i = p; i < p + length; i++) {
            if ((pBytes[i] & 0xFF) == delim) {
                return i;
            }
        }
        return -1;
    }

    public void NEED_NEWLINE_DECORATOR_ON_READ_CHECK() {
        if (NEED_NEWLINE_DECORATOR_ON_READ()) {
            if (isReadable() && (encs.ecflags & EConvFlags.NEWLINE_DECORATOR_MASK) == 0) {
                SET_BINARY_MODE();
            } else {
                SET_TEXT_MODE();
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
        if (!(v instanceof Integer) || ((Integer)v != MORE_CHAR_SUSPENDED && (Integer)v != MORE_CHAR_FINISHED)) {
            throw (RaiseException) v;
        }
        return (Integer) v;
    }

    private Object fillCbuf(ThreadContext context, int ec_flags) {
        int ss, se;
        int ds, de;
        EConvResult res;
        int putbackable;
        int cbuf_len0;
        RaiseException exc;

        ec_flags |= EConvFlags.PARTIAL_INPUT;

        boolean locked = lock();
        try {
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
                rbuf.off += spPtr.p - ss;
                rbuf.len -= spPtr.p - ss;
                cbuf.len += dpPtr.p - ds;

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
                            cbuf.len += dpPtr.p - ds;
                            EncodingUtils.econvCheckError(context, readconv);
                            break;
                        }
                    }
                }
            }
            if (cbuf_len0 != cbuf.len)
                return MORE_CHAR_SUSPENDED;
        } finally {
            if (locked) unlock();
        }

        return MORE_CHAR_FINISHED;
    }

    // read_buffered_data
    public int readBufferedData(byte[] ptrBytes, int ptr, int len) {
        boolean locked = lock();
        try {
            int n = rbuf.len;

            if (n <= 0) return n;
            if (n > len) n = len;
            System.arraycopy(rbuf.ptr, rbuf.start + rbuf.off, ptrBytes, ptr, n);
            rbuf.off += n;
            rbuf.len -= n;
            return n;
        } finally {
            if (locked) unlock();
        }
    }

    // io_fillbuf
    public int fillbuf(ThreadContext context) {
        int r;

        boolean locked = lock();
        try {
            if (rbuf.ptr == null) {
                rbuf.off = 0;
                rbuf.len = 0;
                rbuf.capa = IO_RBUF_CAPA_FOR();
                rbuf.ptr = new byte[rbuf.capa];
                if (Platform.IS_WINDOWS) rbuf.capa--;
            }
            if (rbuf.len == 0) {
                retry:
                while (true) {
                    r = readInternal(context, this, fd, rbuf.ptr, 0, rbuf.capa);

                    if (r < 0) {
                        if (waitReadable(context, fd)) {
                            continue retry;
                        }
                        throw context.runtime.newErrnoFromErrno(posix.getErrno(), "channel: " + fd + (pathv != null ? " " + pathv : ""));
                    }
                    break;
                }
                if (r > 0) checkClosed();
                rbuf.off = 0;
                rbuf.len = r; /* r should be <= rbuf_capa */
                if (r == 0) return -1; /* EOF */
            }
        } finally {
            if (locked) unlock();
        }
        return 0;
    }

    final static RubyThread.ReadWrite<OpenFile> READ_TASK = new RubyThread.ReadWrite<OpenFile>() {
        @Override
        public int run(ThreadContext context, OpenFile fptr, byte[] buffer, int start, int length) throws InterruptedException {
            ChannelFD fd = fptr.fd;

            assert fptr.lockedByMe();

            fptr.unlock();
            try {
                return fptr.posix.read(fd, buffer, start, length, fptr.nonblock);
            } finally {
                fptr.lock();
            }
        }

        @Override
        public void wakeup(RubyThread thread, OpenFile data) {
            thread.getNativeThread().interrupt();
        }
    };

    final static RubyThread.ReadWrite<OpenFile> WRITE_TASK = new RubyThread.ReadWrite<OpenFile>() {
        @Override
        public int run(ThreadContext context, OpenFile fptr, byte[] bytes, int start, int length) throws InterruptedException {
            assert fptr.lockedByMe();

            fptr.unlock();
            try {
                return fptr.posix.write(fptr.fd, bytes, start, length, fptr.nonblock);
            } finally {
                fptr.lock();
            }
        }

        @Override
        public void wakeup(RubyThread thread, OpenFile data) {
            // FIXME: NO! This will kill many native channels. Must be nonblocking to interrupt.
            thread.getNativeThread().interrupt();
        }
    };

    // rb_read_internal
    public static int readInternal(ThreadContext context, OpenFile fptr, ChannelFD fd, byte[] bufBytes, int buf, int count) {
        // if we can do selection and this is not a non-blocking call, do selection

        /*
            NOTE CON: We only do this selection because on the JDK, blocking calls to NIO channels can't be
            interrupted, and we need to be able to interrupt blocking reads. In MRI, this logic is always just a
            simple read(2) because EINTR does not damage the descriptor.

            Doing selects here on ENXIO native channels caused FIFOs to block for read all the time, even when no
            writers are connected. This may or may not be correct behavior for selects against FIFOs, but in any
            case since MRI does not do a select here at all I believe correct logic is to skip the select when
            working with any native descriptor.
         */

        fptr.unlock();
        try {
            if (fd.chSelect != null
                    && fd.chNative == null // MRI does not select for rb_read_internal on native descriptors
                    && !fptr.nonblock) {
                context.getThread().select(fd.chSelect, fptr, SelectionKey.OP_READ);
            }
        } finally {
            fptr.lock();
        }

        try {
            return context.getThread().executeReadWrite(context, fptr, bufBytes, buf, count, READ_TASK);
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("IO operation interrupted");
        }
    }

    /**
     * Logic to match (as well as possible) rb_io_wait_readable from MRI. We do not
     * have the luxury of treating all file descriptors the same, so there's a bit
     * of special-casing here when the channel is not selectable.
     *
     * Note also the EBADF on closed channels; I believe this is what *would*
     * happen in MRI if we always called the selection logic and were given a
     * closed channel.
     *
     * MRI: rb_io_wait_readable
     */
    boolean waitReadable(ThreadContext context, ChannelFD fd) {
        checkClosed();

        boolean locked = lock();
        try {
            if (!fd.ch.isOpen()) {
                posix.setErrno(Errno.EBADF);
                return false;
            }

            if (posix.getErrno() != null && posix.getErrno() != Errno.EAGAIN
                    && posix.getErrno() != Errno.EWOULDBLOCK && posix.getErrno() != Errno.EINTR) {
                // Encountered a permanent error. Don't read again.
                return false;
            }

            if (fd.chSelect != null) {
                unlock();
                try {
                    return context.getThread().select(fd.chSelect, this, SelectionKey.OP_READ);
                } finally {
                    lock();
                }
            }

            /*
            Seekable channels (usually FileChannel) are treated as ready always. There are
            three kinds we typically see:
            1. files, which always select(2) as ready
            2. stdio, which we can't select and can't check .size for available data
            3. subprocess stdio, which we can't select and can't check .size either
            In all three cases, without native fd logic, we can't do anything to determine
            if the stream is ready, so we just assume it is and hope for the best.
             */
            if (fd.chSeek != null) {
                return true;
            }
        } finally {
            if (locked) unlock();
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

        boolean locked = lock();
        try {
            if (needsReadConversion()) {
                Encoding enc = readEncoding(runtime);
                boolean needconv = enc.minLength() != 1;
                SET_BINARY_MODE();
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
                            i[0] = cnt;
                            while ((--i[0] != 0) && pBytes[++p] == term) ;
                        } else {
                            int e = p + cnt;
                            if (EncodingUtils.encAscget(pBytes, p, e, i, enc) != term) return true;
                            while ((p += i[0]) < e && EncodingUtils.encAscget(pBytes, p, e, i, enc) == term) ;
                            i[0] = (e - p);
                        }
                        shiftCbuf(context, cnt - i[0], null);
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
                    if ((pBytes[p] & 0xFF) != term) return true;
                    i = cnt;
                    while (--i != 0 && (pBytes[++p] & 0xFF) == term) ;
                    if (readBufferedData(buf, 0, cnt - i) == 0) /* must not fail */
                        throw context.runtime.newRuntimeError("failure copying buffered IO bytes");
                }
                READ_CHECK(context);
            } while (fillbuf(context) == 0);
        } finally {
            if (locked) unlock();
        }

        return false;
    }

    // io_shift_cbuf
    public RubyString shiftCbuf(ThreadContext context, final int len, final IRubyObject strp) {
        RubyString str = null;
        if (strp != null) {
            if (strp.isNil()) {
                str = RubyString.newStringLight(context.runtime, len);
            } else {
                str = (RubyString) strp;
            }
            str.setTaint(true);
            EncodingUtils.encAssociateIndex(str, encs.enc);
        }
        return shiftCbuf(len, str);
    }

    // io_shift_cbuf with string or null
    public RubyString shiftCbuf(final int len, final RubyString str) {
        boolean locked = lock();
        try {
            if (str != null) {
                str.cat(cbuf.ptr, cbuf.off, len);
                str.setTaint(true);
                EncodingUtils.encAssociateIndex(str, encs.enc);
            }
            cbuf.off += len;
            cbuf.len -= len;
            /* xxx: set coderange */
            if (cbuf.len == 0)
                cbuf.off = 0;
            else if (cbuf.capa / 2 < cbuf.off) {
                System.arraycopy(cbuf.ptr, cbuf.off, cbuf.ptr, 0, cbuf.len);
                cbuf.off = 0;
            }
            return str;
        } finally {
            if (locked) unlock();
        }
    }

    // rb_io_getline_fast
    public IRubyObject getlineFast(ThreadContext context, Encoding enc, RubyIO io, boolean chomp) {
        Ruby runtime = context.runtime;
        RubyString str = null;
        ByteList strByteList;
        int len = 0;
        int pos = 0;
        int cr = 0;

        boolean locked = lock();
        try {
            do {
                int pending = READ_DATA_PENDING_COUNT();

                if (pending > 0) {
                    byte[] pBytes = READ_DATA_PENDING_PTR();
                    int p = READ_DATA_PENDING_OFF();
                    int e;
                    int chomplen = 0;

                    e = memchr(pBytes, p, '\n', pending);
                    if (e != -1) {
                        pending = e - p + 1;
                        if (chomp) chomplen = ((pending > 1 && pBytes[e - 1] == '\r') ? 1 : 0) + 1;
                    }
                    if (str == null) {
                        str = RubyString.newString(runtime, pBytes, p, pending - chomplen);
                        strByteList = str.getByteList();
                        rbuf.off += pending;
                        rbuf.len -= pending;
                    } else {
                        str.resize(len + pending - chomplen);
                        strByteList = str.getByteList();
                        readBufferedData(strByteList.unsafeBytes(), strByteList.begin() + len, pending - chomplen);
                        rbuf.off += chomplen;
                        rbuf.len -= chomplen;
                    }
                    len += pending - chomplen;
                    //if (cr != StringSupport.CR_BROKEN) {
                        final int beg = strByteList.begin();
                        pos += codeRangeScanRestartable(enc, strByteList.unsafeBytes(), beg + pos, beg + len, cr);
                    //}
                    if (e != -1) break;
                }
                READ_CHECK(context);
            } while (fillbuf(context) >= 0);
            if (str == null) return context.nil;
            str = (RubyString) EncodingUtils.ioEncStr(runtime, str, this);
            str.setCodeRange(cr);
            incrementLineno(runtime, io);
        } finally {
            if (locked) unlock();
        }

        return str;
    }

    public void incrementLineno(Ruby runtime, RubyIO io) {
        boolean locked = lock();
        try {
            lineno++;
            if (RubyArgsFile.ArgsFileData.getArgsFileData(runtime).isCurrentFile(io)) {
                runtime.setCurrentLine(runtime.getCurrentLine() + 1);
            } else {
                runtime.setCurrentLine(lineno);
            }
        } finally {
            if (locked) unlock();
        }
    }

    @Deprecated
    public void incrementLineno(Ruby runtime) {
        boolean locked = lock();
        try {
            lineno++;
        } finally {
            if (locked) unlock();
        }
    }

    // read_all, 2014-5-13
    public IRubyObject readAll(ThreadContext context, int siz, IRubyObject str) {
        Ruby runtime = context.runtime;
        int bytes;
        int n;
        int pos;
        Encoding enc;
        int cr;

        boolean locked = lock();
        try {
            if (needsReadConversion()) {
                SET_BINARY_MODE();
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
                            shiftCbuf(context, cbuf.len, str);
                        }
                        throw (RaiseException) v;
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
            for (; ; ) {
                READ_CHECK(context);
                str = EncodingUtils.setStrBuf(context.runtime, str, siz);
                ByteList strByteList = ((RubyString) str).getByteList();
                n = fread(context, strByteList.unsafeBytes(), strByteList.begin() + bytes, siz - bytes);
                if (n == 0 && bytes == 0) {
                    ((RubyString) str).resize(0);
                    break;
                }
                bytes += n;
                strByteList.setRealSize(bytes);
                //if (cr != StringSupport.CR_BROKEN) {
                    final int beg = strByteList.begin();
                    pos += codeRangeScanRestartable(enc, strByteList.unsafeBytes(), beg + pos, beg + bytes, cr);
                //}
                if (bytes < siz) break;
                siz += BUFSIZ;
                ((RubyString) str).modify(BUFSIZ);
            }
            str = EncodingUtils.ioEncStr(runtime, str, this);
        } finally {
            if (locked) unlock();
        }

        ((RubyString)str).setCodeRange(cr);

        return str;
    }

    // io_bufread
    private int ioBufread(ThreadContext context, byte[] ptrBytes, int ptr, int len) {
        int offset = 0;
        int n = len;
        int c;

        boolean locked = lock();
        try {
            if (!READ_DATA_PENDING()) {
                outer:
                while (n > 0) {
                    again:
                    while (true) {
                        c = readInternal(context, this, fd, ptrBytes, ptr + offset, n);
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

            while (n > 0) {
                c = readBufferedData(ptrBytes, ptr + offset, n);
                if (c > 0) {
                    offset += c;
                    if ((n -= c) <= 0) break;
                }
                checkClosed();
                if (fillbuf(context) < 0) {
                    break;
                }
            }
        } finally {
            if (locked) unlock();
        }
        return len - n;
    }

    // io_fread with target buffer
    public int fread(ThreadContext context, byte[] buffer, int offset, int size) {
        size = ioBufread(context, buffer, offset, size);
        // should be errno
        if (size < 0) throw context.runtime.newErrnoFromErrno(posix.getErrno(), pathv);
        return size;
    }

    public void ungetbyte(ThreadContext context, IRubyObject str) {
        int len = ((RubyString)str).size();

        boolean locked = lock();
        try {
            if (rbuf.ptr == null) {
                int min_capa = IO_RBUF_CAPA_FOR();
                rbuf.off = 0;
                rbuf.len = 0;
                //            #if SIZEOF_LONG > SIZEOF_INT
                //            if (len > INT_MAX)
                //                rb_raise(rb_eIOError, "ungetbyte failed");
                //            #endif
                if (len > min_capa)
                    rbuf.capa = len;
                else
                    rbuf.capa = min_capa;
                rbuf.ptr = new byte[rbuf.capa];
            }
            if (rbuf.capa < len + rbuf.len) {
                throw context.runtime.newIOError("ungetbyte failed");
            }
            if (rbuf.off < len) {
                System.arraycopy(rbuf.ptr, rbuf.off, rbuf.ptr, rbuf.capa - rbuf.len, rbuf.len);
                rbuf.off = rbuf.capa - rbuf.len;
            }
            rbuf.off -= len;
            rbuf.len += len;
            ByteList strByteList = ((RubyString) str).getByteList();
            System.arraycopy(strByteList.unsafeBytes(), strByteList.begin(), rbuf.ptr, rbuf.off, len);
        } finally {
            if (locked) unlock();
        }
    }

    // io_getc
    public IRubyObject getc(ThreadContext context, Encoding enc) {
        Ruby runtime = context.runtime;
        int r, n, cr = 0;
        IRubyObject str;

        boolean locked = lock();
        try {
            if (needsReadConversion()) {
                str = context.nil;
                Encoding read_enc = readEncoding(runtime);

                SET_BINARY_MODE();
                makeReadConversion(context, 0);

                while (true) {
                    if (cbuf.len != 0) {
                        r = preciseLength(read_enc, cbuf.ptr, cbuf.off, cbuf.off + cbuf.len);
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
                        ((RubyString) str).setCodeRange(StringSupport.CR_BROKEN);
                        return str;
                    }
                }
                if (StringSupport.MBCLEN_INVALID_P(r)) {
                    r = read_enc.length(cbuf.ptr, cbuf.off, cbuf.off + cbuf.len);
                    str = shiftCbuf(context, r, str);
                    cr = StringSupport.CR_BROKEN;
                } else {
                    str = shiftCbuf(context, StringSupport.MBCLEN_CHARFOUND_LEN(r), str);
                    cr = StringSupport.CR_VALID;
                    if (StringSupport.MBCLEN_CHARFOUND_LEN(r) == 1 && read_enc.isAsciiCompatible() &&
                            Encoding.isAscii(((RubyString) str).getByteList().get(0))) {
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
                r = preciseLength(enc, rbuf.ptr, rbuf.off, rbuf.off + rbuf.len);
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
                            ((RubyString) str).cat(rbuf.ptr[rbuf.off]);
                            rbuf.off++;
                            rbuf.len--;
                            ByteList strByteList = ((RubyString) str).getByteList();
                            r = preciseLength(enc, strByteList.unsafeBytes(), strByteList.getBegin(), strByteList.getBegin() + strByteList.length());
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
        } finally {
            if (locked) unlock();
        }

        ((RubyString)str).setCodeRange(cr);

        return str;
    }

    // io_tell
    public synchronized long tell(ThreadContext context) {
        flushBeforeSeek(context);
        return posix.lseek(fd, 0, PosixShim.SEEK_CUR);
    }

    public synchronized void unread(ThreadContext context) {
        if (Platform.IS_WINDOWS) {
            unreadWindows(context);
        } else {
            unreadPosix();
        }
    }

    // io_unread, UNIX version
    private void unreadPosix() {
        long r;
        boolean locked = lock();
        try {
            checkClosed();
            if (rbuf.len == 0 || (mode & DUPLEX) != 0)
                return;
            /* xxx: target position may be negative if buffer is filled by ungetc */
            posix.setErrno(null);
            r = posix.lseek(fd, -rbuf.len, PosixShim.SEEK_CUR);
            if (r == -1 && posix.getErrno() != null) {
                if (posix.getErrno() == Errno.ESPIPE)
                    mode |= DUPLEX;
                return;
            }
            rbuf.off = 0;
            rbuf.len = 0;
        } finally {
            if (locked) unlock();
        }
    }

    // io_unread, Windows version
    private void unreadWindows(ThreadContext context) {
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

        boolean locked = lock();
        try {
            checkClosed();
            if (rbuf.len == 0 || (mode & DUPLEX) != 0) {
                return;
            }

            // TODO...
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

            pos = posix.lseek(fd, 0, PosixShim.SEEK_CUR);
            if (pos == -1 && posix.getErrno() != null) {
                if (posix.getErrno() == Errno.ESPIPE)
                    mode |= DUPLEX;
                return;
            }

            /* add extra offset for removed '\r' in rbuf */
            extra_max = (pos - rbuf.len);
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
                r = posix.lseek(fd, pos - rbuf.len - newlines, PosixShim.SEEK_SET);
                if (newlines == 0) break;
                if (r == -1) {
                    newlines--;
                    continue;
                }
                read_size = readInternal(context, this, fd, bufBytes, buf, rbuf.len + newlines);
                if (read_size < 0) {
                    throw runtime.newErrnoFromErrno(posix.getErrno(), pathv);
                }
                if (read_size == rbuf.len) {
                    posix.lseek(fd, r, PosixShim.SEEK_SET);
                    break;
                } else {
                    newlines--;
                }
            }
            rbuf.off = 0;
            rbuf.len = 0;
        } finally {
            if (locked) unlock();
        }
    }

    // MRI: io_fwrite
    public long fwrite(ThreadContext context, RubyString str, boolean nosync) {
        // The System.console null check is our poor-man's isatty for Windows. See jruby/jruby#3292
        if (Platform.IS_WINDOWS && isStdio() && System.console() != null) {
            return rbW32WriteConsole(str);
        }

        str = doWriteconv(context, str);
        ByteList strByteList = str.getByteList();
        return binwriteInt(context, strByteList.unsafeBytes(), strByteList.begin(), strByteList.length(), nosync);
    }

    // MRI: io_fwrite with source bytes
    public int fwrite(ThreadContext context, byte[] bytes, int start, int length, Encoding encoding, boolean nosync) {
        // The System.console null check is our poor-man's isatty for Windows. See jruby/jruby#3292
        if (Platform.IS_WINDOWS && isStdio() && System.console() != null) {
            return rbW32WriteConsole(bytes, start, length, encoding);
        }

        ByteList str = doWriteconv(context, bytes, start, length, encoding);

        if (str != null) {
            bytes = str.unsafeBytes();
            start = str.begin();
            length = str.realSize();
        }

        return binwriteInt(context, bytes, start, length, nosync);
    }

    // MRI: rb_w32_write_console
    public static long rbW32WriteConsole(RubyString buffer) {
        ByteList bl = buffer.getByteList();
        return rbW32WriteConsole(bl.unsafeBytes(), bl.begin(), bl.realSize(), bl.getEncoding());
    }

    // MRI: rb_w32_write_console
    public static int rbW32WriteConsole(byte[] bytes, int start, int length, Encoding encoding) {
        // The actual port in MRI uses win32 APIs, but System.console seems to do what we want. See jruby/jruby#3292.
        // FIXME: This assumes the System.console() is the right one to write to. Can you have multiple active?
        System.console().printf("%s", RubyEncoding.decode(bytes, start, length, encoding.getCharset()));

        return length;
    }

    // do_writeconv
    public RubyString doWriteconv(ThreadContext context, RubyString str) {
        boolean locked = lock();
        try {
            if (needsWriteConversion(context)) {
                SET_BINARY_MODE();

                makeWriteConversion(context);

                Encoding common_encoding = getCommonEncodingForWriteConv(context, str.getEncoding());

                if (common_encoding != null) {
                    str = (RubyString) EncodingUtils.rbStrEncode(context, str, runtime.getEncodingService().convertEncodingToRubyEncoding(common_encoding), writeconvPreEcflags, writeconvPreEcopts);
                }

                if (writeconv != null) {
                    str = context.runtime.newString(EncodingUtils.econvStrConvert(context, writeconv, str.getByteList(), EConvFlags.PARTIAL_INPUT));
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
        } finally {
            if (locked) unlock();
        }
        return str;
    }

    protected Encoding getCommonEncodingForWriteConv(ThreadContext context, Encoding strEncoding) {
        Encoding common_encoding = null;
        if (writeconv != null) {
            int fmode = mode;
            if (writeconvAsciicompat != null)
                common_encoding = writeconvAsciicompat;
            else if (EncodingUtils.MODE_BTMODE(fmode, EncodingUtils.DEFAULT_TEXTMODE, 0, 1) != 0 && !strEncoding.isAsciiCompatible()) {
                throw context.runtime.newArgumentError("ASCII incompatible string written for text mode IO without encoding conversion: %s" + strEncoding.toString());
            }
        } else {
            if (encs.enc2 != null)
                common_encoding = encs.enc2;
            else if (encs.enc != EncodingUtils.ascii8bitEncoding(context.runtime))
                common_encoding = encs.enc;
        }
        return common_encoding;
    }

    // do_writeconv with source bytes
    public ByteList doWriteconv(ThreadContext context, byte[] bytes, int start, int length, Encoding encoding) {
        boolean locked = lock();

        ByteList newBytes = null;
        try {
            if (needsWriteConversion(context)) {
                SET_BINARY_MODE();

                makeWriteConversion(context);

                Encoding common_encoding = getCommonEncodingForWriteConv(context, encoding);

                if (common_encoding != null) {
                    newBytes = EncodingUtils.rbByteEncode(context, bytes, start, length, encoding, CR_UNKNOWN, common_encoding, writeconvPreEcflags, writeconvPreEcopts);
                }

                if (writeconv != null) {
                    if (newBytes != null) {
                        newBytes = EncodingUtils.econvStrConvert(context, writeconv, newBytes, EConvFlags.PARTIAL_INPUT);
                    } else {
                        newBytes = EncodingUtils.econvByteConvert(context, writeconv, bytes, start, length, EConvFlags.PARTIAL_INPUT);
                    }
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
        } finally {
            if (locked) unlock();
        }
        return newBytes;
    }

    // io_binwrite
    public int binwriteInt(ThreadContext context, byte[] ptrBytes, int ptr, int len, boolean nosync) {
        int n, r, offset = 0;

        /* don't write anything if current thread has a pending interrupt. */
        context.blockingThreadPoll();

        boolean locked = lock();
        try {
            if ((n = len) <= 0) return n;
            if (wbuf.ptr == null && (nosync || (mode & SYNC) == 0)) {
                wbuf.off = 0;
                wbuf.len = 0;
                wbuf.capa = IO_WBUF_CAPA_MIN;
                wbuf.ptr = new byte[wbuf.capa];
                //            write_lock = new ReentrantReadWriteLock();
                // ???
                //            rb_mutex_allow_trap(fptr->write_lock, 1);
            }

            // Translation: If we are not nosync (if we can do sync write) and sync or tty mode are set, OR
            //              if the write buffer does not have enough capacity to store all incoming data...unbuffered write
            if ((!nosync && (mode & (SYNC | TTY)) != 0) ||
                    (wbuf.ptr != null && wbuf.capa <= wbuf.len + len)) {
                if (wbuf.len != 0 && wbuf.len + len <= wbuf.capa) {
                    if (wbuf.capa < wbuf.off + wbuf.len + len) {
                        System.arraycopy(wbuf.ptr, wbuf.off, wbuf.ptr, 0, wbuf.len);
                        wbuf.off = 0;
                    }
                    System.arraycopy(ptrBytes, ptr + offset, wbuf.ptr, wbuf.off + wbuf.len, len);
                    wbuf.len += len;
                    n = 0;
                }

                if (io_fflush(context) < 0) return -1;
                if (n == 0) return len;

                checkClosed();
                OpenFile fptr = this;
                retry:
                while (true) {
                    int start = ptr + offset;
                    int length = n;
                    if (write_lock != null) {
                        // FIXME: not interruptible by Ruby
                        //                r = rb_mutex_synchronize(fptr->write_lock, io_binwrite_string, (VALUE)&arg);
                        write_lock.writeLock().lock();
                        try {
                            r = binwriteString(fptr, ptrBytes, start, length);
                        } finally {
                            write_lock.writeLock().unlock();
                        }
                    } else {
                        int l = writableLength(n);
                        r = writeInternal(context, this, ptrBytes, ptr + offset, l);
                    }
                    /* xxx: other threads may modify given string. */
                    if (r == n) return len;
                    if (0 <= r) {
                        offset += r;
                        n -= r;
                        posix.setErrno(Errno.EAGAIN);
                    }
                    if (waitWritable(context)) {
                        checkClosed();
                        if (offset < len)
                            continue retry;
                    }
                    return -1;
                }
            }

            if (wbuf.off != 0) {
                if (wbuf.len != 0)
                    System.arraycopy(wbuf.ptr, wbuf.off, wbuf.ptr, 0, wbuf.len);
                wbuf.off = 0;
            }
            System.arraycopy(ptrBytes, ptr + offset, wbuf.ptr, wbuf.off + wbuf.len, len);
            wbuf.len += len;
        } finally {
            if (locked) unlock();
        }
        return len;
    }

    // io_binwrite_string
    static int binwriteString(OpenFile fptr, byte[] bytes, int start, int length) {
        int l = fptr.writableLength(length);
        return fptr.writeInternal2(fptr.fd, bytes, start, l);
    }

    // rb_write_internal
    public static int writeInternal(ThreadContext context, OpenFile fptr, byte[] bufBytes, int buf, int count) {
        try {
            return context.getThread().executeReadWrite(context, fptr, bufBytes, buf, count, WRITE_TASK);
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("IO operation interrupted");
        }
    }

    // rb_write_internal2 (no GVL version...we just don't use executeTask as above.
    int writeInternal2(ChannelFD fd, byte[] bufBytes, int buf, int count) {
        return posix.write(fd, bufBytes, buf, count, nonblock);
    }

    public ChannelFD fd() {
        return fd;
    }

    public Channel channel() {
        assert(fd != null);
        return fd.ch;
    }

    public ReadableByteChannel readChannel() {
        assert(fd != null);
        return fd.chRead;
    }

    public WritableByteChannel writeChannel() {
        assert(fd != null);
        return fd.chWrite;
    }

    public SeekableByteChannel seekChannel() {
        assert(fd != null);
        return fd.chSeek;
    }

    public SelectableChannel selectChannel() {
        assert(fd != null);
        return fd.chSelect;
    }

    public FileChannel fileChannel() {
        assert(fd != null);
        return fd.chFile;
    }

    public SocketChannel socketChannel() {
        assert(fd != null);
        return fd.chSock;
    }

    IRubyObject finishWriteconv(ThreadContext context, boolean noalloc) {
        byte[] dsBytes;
        int ds, de;
        Ptr dpPtr = new Ptr();
        EConvResult res;

        boolean locked = lock();
        try {
            if (wbuf.ptr == null) {
                byte[] buf = new byte[1024];
                long r;

                res = EConvResult.DestinationBufferFull;
                while (res == EConvResult.DestinationBufferFull) {
                    dsBytes = buf;
                    ds = dpPtr.p = 0;
                    de = buf.length;
                    res = writeconv.convert(null, null, 0, dsBytes, dpPtr, de, 0);
                    outer:
                    while ((dpPtr.p - ds) != 0) {
                        retry:
                        while (true) {
                            if (write_lock != null && write_lock.isWriteLockedByCurrentThread())
                                r = writeInternal2(fd, dsBytes, ds, dpPtr.p - ds);
                            else
                                r = writeInternal(context, this, dsBytes, ds, dpPtr.p - ds);
                            if (r == dpPtr.p - ds)
                                break outer;
                            if (0 <= r) {
                                ds += r;
                            }
                            if (waitWritable(context)) {
                                if (fd == null)
                                    return noalloc ? context.tru : runtime.newIOError(RubyIO.CLOSED_STREAM_MSG).getException();
                                continue retry;
                            }
                            break retry;
                        }
                        return noalloc ? context.tru : RubyFixnum.newFixnum(runtime, (posix.getErrno() == null) ? 0 : posix.getErrno().longValue());
                    }
                    if (res == EConvResult.InvalidByteSequence ||
                            res == EConvResult.IncompleteInput ||
                            res == EConvResult.UndefinedConversion) {
                        return noalloc ? context.tru : EncodingUtils.makeEconvException(runtime, writeconv).getException();
                    }
                }

                return context.nil;
            }

            res = EConvResult.DestinationBufferFull;
            while (res == EConvResult.DestinationBufferFull) {
                if (wbuf.len == wbuf.capa) {
                    if (io_fflush(context) < 0)
                        return noalloc ? context.tru : runtime.newFixnum(posix.getErrno() == null ? 0 : posix.getErrno().longValue());
                }

                dsBytes = wbuf.ptr;
                ds = dpPtr.p = wbuf.off + wbuf.len;
                de = wbuf.capa;
                res = writeconv.convert(null, null, 0, dsBytes, dpPtr, de, 0);
                wbuf.len += (dpPtr.p - ds);
                if (res == EConvResult.InvalidByteSequence ||
                        res == EConvResult.IncompleteInput ||
                        res == EConvResult.UndefinedConversion) {
                    return noalloc ? context.tru : EncodingUtils.makeEconvException(runtime, writeconv).getException();
                }
            }
        } finally {
            if (locked) unlock();
        }
        return context.nil;
    }

    // rb_io_set_nonblock
    public void setNonblock(Ruby runtime) {
        setBlocking(runtime, false);
    }

    public void setBlock(Ruby runtime) {
        setBlocking(runtime, true);
    }

    public void setBlocking(Ruby runtime, boolean blocking) {
        boolean locked = lock();
        try {
            // Not all NIO channels are non-blocking, so we need to maintain this flag
            // and make those channels act like non-blocking
            nonblock = !blocking;

            ChannelFD fd = this.fd;

            checkClosed();

            if (fd.chSelect != null) {
                try {
                    fd.chSelect.configureBlocking(blocking);
                } catch (IOException ioe) {
                    throw runtime.newIOErrorFromException(ioe);
                }
            }
        } finally {
            if (locked) unlock();
        }
    }

    public boolean isBlocking() {
        return !nonblock;
    }

    // MRI: check_tty
    public void checkTTY() {
        if (fd.realFileno != -1 && runtime.getPosix().isatty(fd.realFileno) != 0
            || stdio_file != null) {

            boolean locked = lock();
            try {
                mode |= TTY | DUPLEX;
            } finally {
                if (locked) unlock();
            }
        }

        // Clear errno so ENOTTY does not get picked up elsewhere (jruby/jruby#4527
        runtime.getPosix().errno(0);
    }

    public boolean isBOM() {
        return (mode & SETENC_BY_BOM) != 0;
    }

    public void setBOM(boolean bom) {
        boolean locked = lock();
        try {
            if (bom) {
                mode |= SETENC_BY_BOM;
            } else {
                mode &= ~SETENC_BY_BOM;
            }
        } finally {
            if (locked) unlock();
        }
    }

    public boolean isStdio() {
        return stdio_file != null;
    }

    public int readPending() {
        lock();
        try {
            if (READ_CHAR_PENDING()) return 1;
            return READ_DATA_PENDING_COUNT();
        } finally {
            unlock();
        }
    }

    @Deprecated
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

    public int getFileno() {
        return fd.bestFileno(true);
    }

    // rb_thread_flock
    public int threadFlock(ThreadContext context, final int lockMode) {
//        #ifdef __CYGWIN__
//        int old_errno = errno;
//        #endif
        int ret = 0;
        try {
            ret = context.getThread().executeTaskBlocking(context, this, new RubyThread.Task<OpenFile, Integer>() {
                @Override
                public Integer run(ThreadContext context, OpenFile openFile) throws InterruptedException {
                    return posix.flock(fd, lockMode);
                }

                @Override
                public void wakeup(RubyThread thread, OpenFile openFile) {
                    // unlikely to help a native downcall, but we'll try it
                    thread.getNativeThread().interrupt();
                }
            });
        } catch (InterruptedException ie) {
            // ignore?
        }

//        #ifdef __CYGWIN__
//        if (GetLastError() == ERROR_NOT_LOCKED) {
//            ret = 0;
//            errno = old_errno;
//        }
//        #endif
        return ret;
    }

    public Errno errno() {
        return posix.getErrno();
    }

    public void errno(Errno newErrno) {
        posix.setErrno(newErrno);
    }

    public static int cloexecDup2(PosixShim posix, ChannelFD oldfd, ChannelFD newfd) {
        int ret;
        /* When oldfd == newfd, dup2 succeeds but dup3 fails with EINVAL.
         * rb_cloexec_dup2 succeeds as dup2.  */
        if (oldfd == newfd) {
            ret = 0;
        }
        else {
//            #if defined(HAVE_DUP3) && defined(O_CLOEXEC)
//            static int try_dup3 = 1;
//            if (2 < newfd && try_dup3) {
//                ret = dup3(oldfd, newfd, O_CLOEXEC);
//                if (ret != -1)
//                    return ret;
//            /* dup3 is available since Linux 2.6.27, glibc 2.9. */
//                if (errno == ENOSYS) {
//                    try_dup3 = 0;
//                    ret = dup2(oldfd, newfd);
//                }
//            }
//            else {
//                ret = dup2(oldfd, newfd);
//            }
//            #else
            ret = posix.dup2(oldfd, newfd);
//            #endif
            if (ret == -1) return -1;
        }
        fdFixCloexec(posix, ret);
        return ret;
    }

    // MRI: rb_maygvl_fd_fix_cloexec, without compiler conditions
    public static void fdFixCloexec(PosixShim posix, int fd) {
        if (fd >= 0 && fd < FilenoUtil.FIRST_FAKE_FD) {
            int flags, flags2, ret;
            flags = posix.fcntlGetFD(fd); /* should not fail except EBADF. */
            if (flags == -1) {
                throw new AssertionError(String.format("BUG: rb_maygvl_fd_fix_cloexec: fcntl(%d, F_GETFD) failed: %s", fd, posix.getErrno().description()));
            }
            if (fd <= 2)
                flags2 = flags & ~FcntlLibrary.FD_CLOEXEC; /* Clear CLOEXEC for standard file descriptors: 0, 1, 2. */
            else
                flags2 = flags | FcntlLibrary.FD_CLOEXEC; /* Set CLOEXEC for non-standard file descriptors: 3, 4, 5, ... */
            if (flags != flags2) {
                ret = posix.fcntlSetFD(fd, flags2);
                if (ret == -1) {
                    throw new AssertionError(String.format("BUG: rb_maygvl_fd_fix_cloexec: fcntl(%d, F_SETFD, %d) failed: %s", fd, flags2, posix.getErrno().description()));
                }
            }
        }
        // otherwise JVM sets cloexec
    }

    /**
     * Add a thread to the list of blocking threads for this IO.
     *
     * @param thread A thread blocking on this IO
     */
    public void addBlockingThread(RubyThread thread) {
        Set<RubyThread> blockingThreads = this.blockingThreads;

        if (blockingThreads == null) {
            synchronized (this) {
                blockingThreads = this.blockingThreads;
                if (blockingThreads == null) {
                    this.blockingThreads = blockingThreads = new HashSet<>(1);
                }
            }
        }

        synchronized (blockingThreads) {
            blockingThreads.add(thread);
        }
    }

    /**
     * Remove a thread from the list of blocking threads for this IO.
     *
     * @param thread A thread blocking on this IO
     */
    public void removeBlockingThread(RubyThread thread) {
        Set<RubyThread> blockingThreads = this.blockingThreads;

        if (blockingThreads == null) {
            return;
        }

        synchronized (blockingThreads) {
            blockingThreads.remove(thread);
        }
    }

    /**
     * Fire an IOError in all threads blocking on this IO object
     */
    public void interruptBlockingThreads(ThreadContext context) {
        Set<RubyThread> blockingThreads = this.blockingThreads;

        if (blockingThreads == null) {
            return;
        }

        synchronized (blockingThreads) {
            for (RubyThread thread : blockingThreads) {
                // If it's the current thread, ignore it since we're the one doing the interrupting
                if (thread == context.getThread()) continue;

                // raise will also wake the thread from selection
                RubyException exception = (RubyException) runtime.getIOError().newInstance(context, runtime.newString("stream closed in another thread"), Block.NULL_BLOCK);
                thread.raise(exception);
            }
        }
    }

    /**
     * Wait until all blocking threads have exited their blocking area. Use in combination with
     * interruptBlockingThreads to ensure every blocking thread has moved on before proceding to
     * manipulate the IO.
     */
    public void waitForBlockingThreads(ThreadContext context) {
        Set<RubyThread> blockingThreads = this.blockingThreads;

        if (blockingThreads == null) {
            return;
        }

        while (blockingThreads.size() > 0) {
            try {
                context.getThread().sleep(1);
            } catch (InterruptedException ie) {
                break;
            }
        }
    }

    public void SET_BINARY_MODE() {
        // FIXME: this only does something if we have O_BINARY at open(2) level
    }

    private void SET_TEXT_MODE() {
        // FIXME: this only does something if we have O_TEXT at open(2) level
    }

    public int remainSize() {
        int siz = READ_DATA_PENDING_COUNT();
        long size;
        long pos;

        if ((size = posix.size(fd)) >= 0 &&
                (pos = posix.lseek(fd, 0, PosixShim.SEEK_CUR)) != -1 &&
                size > pos) {
            if (siz + (size - pos) > Integer.MAX_VALUE) {
                throw runtime.newIOError("file too big for single read");
            }
            siz += size - pos;
        } else {
            siz += BUFSIZ;
        }
        return siz;
    }

    public boolean lock() {
        if (lock.isHeldByCurrentThread()) {
            return false;
        } else {
            lock.lock();
            return true;
        }
    }

    public void unlock() {
        assert lock.isHeldByCurrentThread();

        lock.unlock();
    }

    public boolean lockedByMe() {
        return lock.isHeldByCurrentThread();
    }

    @Deprecated
    public long binwrite(ThreadContext context, byte[] ptrBytes, int ptr, int len, boolean nosync) {
        return binwriteInt(context, ptrBytes, ptr, len, nosync);
    }
}

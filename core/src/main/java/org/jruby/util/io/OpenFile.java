package org.jruby.util.io;

import jnr.constants.platform.Errno;
import jnr.constants.platform.OpenFlags;
import jnr.posix.FileStat;
import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jruby.Finalizable;
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
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OpenFile implements Finalizable {

    // RB_IO_FPTR_NEW, minus fields that Java already initializes the same way
    public OpenFile(IRubyObject nil) {
        runtime = nil.getRuntime();
        writeconvAsciicompat = nil;
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
    public static final int WSPLIT_INITIALIZED = 0x00000400;
    public static final int TRUNC              = 0x00000800;
    public static final int TEXTMODE           = 0x00001000;
    public static final int SETENC_BY_BOM      = 0x00100000;
    public static final int PREP         = (1<<16);
    
    public static final int SYNCWRITE = SYNC | WRITABLE;

    public static final int PIPE_BUF = 512; // value of _POSIX_PIPE_BUF from Mac OS X 10.9
    public static final int BUFSIZ = 1024; // value of BUFSIZ from Mac OS X 10.9 stdio.h

    public void ascii8bitBinmode(Ruby runtime) {
        Encoding ascii8bit = runtime.getEncodingService().getAscii8bitEncoding();

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
        if (seek(context, pos, PosixShim.SEEK_SET) < 0 && errno() != null) {
            throw runtime.newErrnoFromErrno(errno(), getPath());
        }
    }

    public static interface Finalizer {
        public void finalize(Ruby runtime, OpenFile fptr, boolean noraise);
    }

    private ChannelFD fd = null;
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
    public IRubyObject writeconvAsciicompat;
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

    protected List<RubyThread> blockingThreads;

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
                throw runtime.newErrnoFromErrno(posix.errno, "error flushing");
            }
        }
        if (tiedIOForWriting != null) {
            OpenFile wfptr;
            wfptr = tiedIOForWriting.getOpenFileChecked();
            if (wfptr.io_fflush(context) < 0)
                throw runtime.newErrnoFromErrno(wfptr.posix.errno, wfptr.getPath());
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
                if (!waitWritable(context)) {
                    return -1;
                }
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
            if (posix.errno == null) return false;

            if (fd == null) throw runtime.newIOError(RubyIO.CLOSED_STREAM_MSG);

            switch (posix.errno) {
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
            if (posix.errno == null) return false;

            if (fd == null) throw runtime.newIOError(RubyIO.CLOSED_STREAM_MSG);

            switch (posix.errno) {
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
        return waitReadable(context, 0);
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
                return thread.select(fd.chSelect, this, ops & fd.chSelect.validOps(), timeout);

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
            wbuf.off += (int)r;
            wbuf.len -= (int)r;
            posix.errno = Errno.EAGAIN;
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
                throw context.runtime.newErrnoFromErrno(posix.errno, "");
            unread(context);
            posix.errno = null;
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
        IRubyObject err = runtime.getNil();
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
                if (flushBufferSync() < 0 && err.isNil())
                    err = runtime.getTrue();
            }
            else {
                if (io_fflush(runtime.getCurrentContext()) < 0 && err.isNil()) {
                    err = RubyFixnum.newFixnum(runtime, posix.errno == null ? 0 :posix.errno.longValue());
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
                err = noraise ? runtime.getTrue() : RubyNumeric.int2fix(runtime, posix.errno.intValue());
        } else if (fd != null) {
            /* fptr->fd may be closed even if close fails.
             * POSIX doesn't specify it.
             * We assumes it is closed.  */
            if ((posix.close(fd) < 0) && err.isNil())
                err = noraise ? runtime.getTrue() : runtime.newFixnum(posix.errno.intValue());
        }

        if (!err.isNil() && !noraise) {
            if (err instanceof RubyFixnum || err instanceof RubyBignum) {
                posix.errno = Errno.valueOf(RubyNumeric.num2int(err));
                throw runtime.newErrnoFromErrno(posix.errno, pathv);
            } else {
                throw new RaiseException((RubyException)err);
            }
        }
    }

    // MRI: NEED_READCONV
    public boolean needsReadConversion() {
        return Platform.IS_WINDOWS ?
                (encs.enc2 != null || (encs.ecflags & ~EConvFlags.CRLF_NEWLINE_DECORATOR) != 0)
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

    // MRI: appendline
    public int appendline(ThreadContext context, int delim, ByteList[] strp, int[] lp) {
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
            if (isReadable() &&
                    (encs.ecflags & EConvFlags.NEWLINE_DECORATOR_MASK) == 0) {
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
                rbuf.off += (int) (spPtr.p - ss);
                rbuf.len -= (int) (spPtr.p - ss);
                cbuf.len += (int) (dpPtr.p - ds);

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
                            cbuf.len += (int) (dpPtr.p - ds);
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
                if (Platform.IS_WINDOWS) {
                    rbuf.capa--;
                }
            }
            if (rbuf.len == 0) {
                retry:
                while (true) {
                    r = readInternal(context, this, fd, rbuf.ptr, 0, rbuf.capa);

                    if (r < 0) {
                        if (waitReadable(context, fd)) {
                            continue retry;
                        }
                        throw context.runtime.newErrnoFromErrno(posix.errno, "channel: " + fd + (pathv != null ? " " + pathv : ""));
                    }
                    break;
                }
                rbuf.off = 0;
                rbuf.len = (int) r; /* r should be <= rbuf_capa */
                if (r == 0)
                    return -1; /* EOF */
            }
        } finally {
            if (locked) unlock();
        }
        return 0;
    }

    public static class InternalReadStruct {
        InternalReadStruct(OpenFile fptr, ChannelFD fd, byte[] bufBytes, int buf, int count) {
            this.fptr = fptr;
            this.fd = fd;
            this.bufBytes = bufBytes;
            this.buf = buf;
            this.capa = count;
        }

        public OpenFile fptr;
        public ChannelFD fd;
        public byte[] bufBytes;
        public int buf;
        public int capa;
        public Selector selector;
    }

    final static RubyThread.Task<InternalReadStruct, Integer> readTask = new RubyThread.Task<InternalReadStruct, Integer>() {
        @Override
        public Integer run(ThreadContext context, InternalReadStruct iis) throws InterruptedException {
            ChannelFD fd = iis.fd;
            OpenFile fptr = iis.fptr;

            assert fptr.lockedByMe();

            fptr.unlock();
            try {
                return fptr.posix.read(fd, iis.bufBytes, iis.buf, iis.capa, fptr.nonblock);
            } finally {
                fptr.lock();
            }
        }

        @Override
        public void wakeup(RubyThread thread, InternalReadStruct data) {
            thread.getNativeThread().interrupt();
        }
    };

    final static RubyThread.Task<InternalWriteStruct, Integer> writeTask = new RubyThread.Task<InternalWriteStruct, Integer>() {
        @Override
        public Integer run(ThreadContext context, InternalWriteStruct iis) throws InterruptedException {
            OpenFile fptr = iis.fptr;

            assert fptr.lockedByMe();

            fptr.unlock();
            try {
                return iis.fptr.posix.write(iis.fd, iis.bufBytes, iis.buf, iis.capa, iis.fptr.nonblock);
            } finally {
                fptr.lock();
            }
        }

        @Override
        public void wakeup(RubyThread thread, InternalWriteStruct data) {
            // FIXME: NO! This will kill many native channels. Must be nonblocking to interrupt.
            thread.getNativeThread().interrupt();
        }
    };

    // rb_read_internal
    public static int readInternal(ThreadContext context, OpenFile fptr, ChannelFD fd, byte[] bufBytes, int buf, int count) {
        InternalReadStruct iis = new InternalReadStruct(fptr, fd, bufBytes, buf, count);

        // if we can do selection and this is not a non-blocking call, do selection
        fptr.unlock();
        try {
            if (fd.chSelect != null && !iis.fptr.nonblock) {
                context.getThread().select(fd.chSelect, fptr, SelectionKey.OP_READ);
            }
        } finally {
            fptr.lock();
        }

        try {
            return context.getThread().executeTask(context, iis, readTask);
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
        if (fd == null) {
            throw context.runtime.newIOError(RubyIO.CLOSED_STREAM_MSG);
        }

        boolean locked = lock();
        try {
            if (!fd.ch.isOpen()) {
                posix.errno = Errno.EBADF;
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

            // kinda-hacky way to see if there's more data to read from a seekable channel
            if (fd.chSeek != null) {
                FileChannel fdSeek = fd.chSeek;
                try {
                    // not a real file, can't get size...we'll have to just read and block
                    if (fdSeek.size() < 0) return true;

                    // if current position is less than file size, read should not block
                    return fdSeek.position() < fdSeek.size();
                } catch (IOException ioe) {
                    throw context.runtime.newIOErrorFromException(ioe);
                }
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
                            i[0] = (int) cnt;
                            while ((--i[0] != 0) && pBytes[++p] == term) ;
                        } else {
                            int e = p + cnt;
                            if (EncodingUtils.encAscget(pBytes, p, e, i, enc) != term) return true;
                            while ((p += i[0]) < e && EncodingUtils.encAscget(pBytes, p, e, i, enc) == term) ;
                            i[0] = (int) (e - p);
                        }
                        shiftCbuf(context, (int) cnt - i[0], null);
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
                    i = (int) cnt;
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
    public IRubyObject shiftCbuf(ThreadContext context, int len, IRubyObject strp) {
        boolean locked = lock();
        try {
            IRubyObject str = null;
            if (strp != null) {
                str = strp;
                if (str.isNil()) {
                    strp = str = RubyString.newString(context.runtime, cbuf.ptr, cbuf.off, len);
                } else {
                    ((RubyString) str).cat(cbuf.ptr, cbuf.off, len);
                }
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
    public IRubyObject getlineFast(ThreadContext context, Encoding enc, RubyIO io) {
        Ruby runtime = context.runtime;
        IRubyObject str = null;
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

                    e = memchr(pBytes, p, '\n', pending);
                    if (e != -1) {
                        pending = (int) (e - p + 1);
                    }
                    if (str == null) {
                        str = RubyString.newString(runtime, pBytes, p, pending);
                        strByteList = ((RubyString) str).getByteList();
                        rbuf.off += pending;
                        rbuf.len -= pending;
                    } else {
                        ((RubyString) str).resize(len + pending);
                        strByteList = ((RubyString) str).getByteList();
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
            ((RubyString) str).setCodeRange(cr);
            incrementLineno(runtime);
        } finally {
            if (locked) unlock();
        }

        return str;
    }

    public void incrementLineno(Ruby runtime) {
        boolean locked = lock();
        try {
            lineno++;
            runtime.setCurrentLine(lineno);
            RubyArgsFile.setCurrentLineNumber(runtime.getArgsFile(), lineno);
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
                            str = shiftCbuf(context, cbuf.len, str);
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
            str = EncodingUtils.setStrBuf(runtime, str, siz);
            for (; ; ) {
                READ_CHECK(context);
                n = fread(context, str, bytes, siz - bytes);
                if (n == 0 && bytes == 0) {
                    ((RubyString) str).resize(0);
                    break;
                }
                bytes += n;
                ByteList strByteList = ((RubyString) str).getByteList();
                strByteList.setRealSize(bytes);
                if (cr != StringSupport.CR_BROKEN)
                    pos += StringSupport.codeRangeScanRestartable(enc, strByteList.unsafeBytes(), strByteList.begin() + pos, strByteList.begin() + bytes, cr);
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
        if (len < 0) throw context.runtime.newErrnoFromErrno(posix.errno, pathv);
        return len;
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
                    rbuf.capa = (int) len;
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
            rbuf.off -= (int) len;
            rbuf.len += (int) len;
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
            unreadUnix(context);
        }
    }

    // io_unread, UNIX version
    private synchronized void unreadUnix(ThreadContext context) {
        long r;
        boolean locked = lock();
        try {
            checkClosed();
            if (rbuf.len == 0 || (mode & DUPLEX) != 0)
                return;
            /* xxx: target position may be negative if buffer is filled by ungetc */
            posix.errno = null;
            r = posix.lseek(fd, -rbuf.len, PosixShim.SEEK_CUR);
            if (r < 0 && posix.errno != null) {
                if (posix.errno == Errno.ESPIPE)
                    mode |= DUPLEX;
                return;
            }
            rbuf.off = 0;
            rbuf.len = 0;
        } finally {
            if (locked) unlock();
        }
        return;
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
            if (pos < 0 && posix.errno != null) {
                if (posix.errno == Errno.ESPIPE)
                    mode |= DUPLEX;
                return;
            }

            /* add extra offset for removed '\r' in rbuf */
            extra_max = (long) (pos - rbuf.len);
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
                if (r < 0) {
                    newlines--;
                    continue;
                }
                read_size = readInternal(context, this, fd, bufBytes, buf, rbuf.len + newlines);
                if (read_size < 0) {
                    throw runtime.newErrnoFromErrno(posix.errno, pathv);
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
        return;
    }

    // io_fwrite
    public long fwrite(ThreadContext context, IRubyObject str, boolean nosync) {
        // TODO: Windows
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
        boolean locked = lock();
        try {
            if (needsWriteConversion(context)) {
                IRubyObject common_encoding = context.nil;
                SET_BINARY_MODE();

                makeWriteConversion(context);

                if (writeconv != null) {
                    int fmode = mode;
                    if (!writeconvAsciicompat.isNil())
                        common_encoding = writeconvAsciicompat;
                    else if (EncodingUtils.MODE_BTMODE(fmode, EncodingUtils.DEFAULT_TEXTMODE, 0, 1) != 0 && !((RubyString) str).getEncoding().isAsciiCompatible()) {
                        throw context.runtime.newArgumentError("ASCII incompatible string written for text mode IO without encoding conversion: %s" + ((RubyString) str).getEncoding().toString());
                    }
                } else {
                    if (encs.enc2 != null)
                        common_encoding = context.runtime.getEncodingService().convertEncodingToRubyEncoding(encs.enc2);
                    else if (encs.enc != EncodingUtils.ascii8bitEncoding(context.runtime))
                        common_encoding = context.runtime.getEncodingService().convertEncodingToRubyEncoding(encs.enc);
                }

                if (!common_encoding.isNil()) {
                    str = EncodingUtils.rbStrEncode(context, str, common_encoding, writeconvPreEcflags, writeconvPreEcopts);
                }

                if (writeconv != null) {
                    ((RubyString) str).setValue(
                            EncodingUtils.econvStrConvert(context, writeconv, ((RubyString) str).getByteList(), EConvFlags.PARTIAL_INPUT));
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

    private static class BinwriteArg {
        OpenFile fptr;
        IRubyObject str;
        byte[] ptrBytes;
        int ptr;
        int length;
    }

    // io_binwrite
    public long binwrite(ThreadContext context, IRubyObject str, byte[] ptrBytes, int ptr, int len, boolean nosync) {
        int n, r, offset = 0;

        /* don't write anything if current thread has a pending interrupt. */
        context.pollThreadEvents();

        boolean locked = lock();
        try {
            if ((n = len) <= 0) return n;
            if (wbuf.ptr == null && !(!nosync && (mode & SYNC) != 0)) {
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
                BinwriteArg arg = new BinwriteArg();

                if (wbuf.len != 0 && wbuf.len + len <= wbuf.capa) {
                    if (wbuf.capa < wbuf.off + wbuf.len + len) {
                        System.arraycopy(wbuf.ptr, wbuf.off, wbuf.ptr, 0, wbuf.len);
                        wbuf.off = 0;
                    }
                    System.arraycopy(ptrBytes, ptr + offset, wbuf.ptr, wbuf.off + wbuf.len, len);
                    wbuf.len += (int) len;
                    n = 0;
                }
                if (io_fflush(context) < 0)
                    return -1L;
                if (n == 0)
                    return len;

                checkClosed();
                arg.fptr = this;
                arg.str = str;
                retry:
                while (true) {
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
                    } else {
                        int l = writableLength(n);
                        r = writeInternal(context, this, fd, ptrBytes, ptr + offset, l);
                    }
                    /* xxx: other threads may modify given string. */
                    if (r == n) return len;
                    if (0 <= r) {
                        offset += r;
                        n -= r;
                        posix.errno = Errno.EAGAIN;
                    }
                    if (waitWritable(context)) {
                        checkClosed();
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
            wbuf.len += (int) len;
        } finally {
            if (locked) unlock();
        }
        return len;
    }

    // io_binwrite_string
    static int binwriteString(ThreadContext context, BinwriteArg arg) {
        BinwriteArg p = arg;
        int l = p.fptr.writableLength(p.length);
        return p.fptr.writeInternal2(p.fptr.fd, p.ptrBytes, p.ptr, l);
    }

    public static class InternalWriteStruct {
        InternalWriteStruct(OpenFile fptr, ChannelFD fd, byte[] bufBytes, int buf, int count) {
            this.fptr = fptr;
            this.fd = fd;
            this.bufBytes = bufBytes;
            this.buf = buf;
            this.capa = count;
        }

        public final OpenFile fptr;
        public final ChannelFD fd;
        public final byte[] bufBytes;
        public final int buf;
        public int capa;
    }

    // rb_write_internal
    public static int writeInternal(ThreadContext context, OpenFile fptr, ChannelFD fd, byte[] bufBytes, int buf, int count) {
        InternalWriteStruct iis = new InternalWriteStruct(fptr, fd, bufBytes, buf, count);

        try {
            return context.getThread().executeTask(context, iis, writeTask);
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
        return fd.ch;
    }

    public ReadableByteChannel readChannel() {
        return fd.chRead;
    }

    public WritableByteChannel writeChannel() {
        return fd.chWrite;
    }

    public FileChannel seekChannel() {
        return fd.chSeek;
    }

    public SelectableChannel selectChannel() {
        return fd.chSelect;
    }

    public FileChannel fileChannel() {
        return fd.chFile;
    }

    public SocketChannel socketChannel() {
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
                    dpPtr.p = 0;
                    res = writeconv.convert(null, null, 0, dsBytes, dpPtr, de, 0);
                    outer:
                    while ((dpPtr.p - ds) != 0) {
                        retry:
                        while (true) {
                            if (write_lock != null && write_lock.isWriteLockedByCurrentThread())
                                r = writeInternal2(fd, dsBytes, ds, dpPtr.p - ds);
                            else
                                r = writeInternal(runtime.getCurrentContext(), this, fd, dsBytes, ds, dpPtr.p - ds);
                            if (r == dpPtr.p - ds)
                                break outer;
                            if (0 <= r) {
                                ds += r;
                            }
                            if (waitWritable(context)) {
                                if (fd == null)
                                    return noalloc ? runtime.getTrue() : runtime.newIOError(RubyIO.CLOSED_STREAM_MSG).getException();
                                continue retry;
                            }
                            break retry;
                        }
                        return noalloc ? runtime.getTrue() : RubyFixnum.newFixnum(runtime, (posix.errno == null) ? 0 : posix.errno.longValue());
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
                    if (io_fflush(context) < 0)
                        return noalloc ? runtime.getTrue() : runtime.newFixnum(posix.errno == null ? 0 : posix.errno.longValue());
                }

                dsBytes = wbuf.ptr;
                ds = dpPtr.p = wbuf.off + wbuf.len;
                de = wbuf.capa;
                res = writeconv.convert(null, null, 0, dsBytes, dpPtr, de, 0);
                wbuf.len += (int) (dpPtr.p - ds);
                if (res == EConvResult.InvalidByteSequence ||
                        res == EConvResult.IncompleteInput ||
                        res == EConvResult.UndefinedConversion) {
                    return noalloc ? runtime.getTrue() : EncodingUtils.makeEconvException(runtime, writeconv).getException();
                }
            }
        } finally {
            if (locked) unlock();
        }
        return runtime.getNil();
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
                    return;
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
        // TODO: native descriptors? Is this only used for stdio?
        if (stdio_file != null) {
            boolean locked = lock();
            try {
                mode |= TTY | DUPLEX;
            } finally {
                if (locked) unlock();
            }
        }
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
        int fileno = fd.realFileno;
        if (fileno != -1) return fileno;
        return fd.fakeFileno;
    }

    // rb_thread_flock
    public int threadFlock(ThreadContext context, final int lockMode) {
//        #ifdef __CYGWIN__
//        int old_errno = errno;
//        #endif
        int ret = 0;
        try {
            ret = context.getThread().executeTask(context, this, new RubyThread.Task<OpenFile, Integer>() {
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
        return posix.errno;
    }

    public void errno(Errno newErrno) {
        posix.errno = newErrno;
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
        // TODO?
//        rb_maygvl_fd_fix_cloexec(ret);
        return ret;
    }

    /**
     * Add a thread to the list of blocking threads for this IO.
     *
     * @param thread A thread blocking on this IO
     */
    public void addBlockingThread(RubyThread thread) {
        boolean locked = lock();
        try {
            if (blockingThreads == null) {
                blockingThreads = new ArrayList<RubyThread>(1);
            }
            blockingThreads.add(thread);
        } finally {
            if (locked) unlock();
        }
    }

    /**
     * Remove a thread from the list of blocking threads for this IO.
     *
     * @param thread A thread blocking on this IO
     */
    public synchronized void removeBlockingThread(RubyThread thread) {
        boolean locked = lock();
        try {
            if (blockingThreads == null) {
                return;
            }
            for (int i = 0; i < blockingThreads.size(); i++) {
                if (blockingThreads.get(i) == thread) {
                    // not using remove(Object) here to avoid the equals() call
                    blockingThreads.remove(i);
                }
            }
        } finally {
            if (locked) unlock();
        }
    }

    /**
     * Fire an IOError in all threads blocking on this IO object
     */
    public void interruptBlockingThreads() {
        boolean locked = lock();
        try {
            if (blockingThreads == null) {
                return;
            }
            for (int i = 0; i < blockingThreads.size(); i++) {
                RubyThread thread = blockingThreads.get(i);

                // raise will also wake the thread from selection
                thread.raise(new IRubyObject[]{runtime.newIOError("stream closed").getException()}, Block.NULL_BLOCK);
            }
        } finally {
            if (locked) unlock();
        }
    }

    public void SET_BINARY_MODE() {
        // FIXME: this only does something if we have O_BINARY at open(2) level
    }

    private void SET_TEXT_MODE() {
        // FIXME: this only does something if we have O_TEXT at open(2) level
    }

    public int remainSize() {
        FileStat st;
        int siz = READ_DATA_PENDING_COUNT();
        long pos;

        // MRI does all this presumably to read more of the file right away, but
        // I believe the logic that uses this is ok with just pending read plus buf size.

//        if (fstat(fptr -> fd, & st)==0 && S_ISREG(st.st_mode))
//        {
//            if (io_fflush(fptr) < 0)
//                rb_sys_fail(0);
//            pos = lseek(fptr -> fd, 0, SEEK_CUR);
//            if (st.st_size >= pos && pos >= 0) {
//                siz += st.st_size - pos;
//                if (siz > LONG_MAX) {
//                    rb_raise(rb_eIOError, "file too big for single read");
//                }
//            }
//        }
//        else {
            siz += BUFSIZ;
//        }
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
}

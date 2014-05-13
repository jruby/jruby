package org.jruby.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jruby.Ruby;
import org.jruby.RubyArgsFile;
import org.jruby.RubyBasicObject;
import org.jruby.RubyIO;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.thread.Mutex;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;

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

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    
    public static final int SYNCWRITE = SYNC | WRITABLE;

    public static final int PIPE_BUF = 512; // value of _POSIX_PIPE_BUF from Mac OS X 10.9
    public static final int BUFSIZ = 1024; // value of BUFSIZ from Mac OS X 10.9 stdio.h

    public static interface Finalizer {

        public void finalize(Ruby runtime, boolean raise);
    }
    private Channel fd;
    private ReadableByteChannel fdRead;
    private WritableByteChannel fdWrite;
    private SeekableByteChannel fdSeek;
    private SelectableChannel fdSelect;
    private Stream mainStream;
    private Stream pipeStream;
    private int mode;
    private Process process;
    private int lineno = 0;
    private String pathv;
    private Finalizer finalizer;
    private boolean stdio;

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

    public Stream getMainStream() {
        return mainStream;
    }

    public Stream getMainStreamSafe() throws BadDescriptorException {
        Stream stream = mainStream;
        if (stream == null) throw new BadDescriptorException();
        return stream;
    }

    public void setMainStream(Stream mainStream) {
        this.mainStream = mainStream;
        this.fd = mainStream.getChannel();
        if (fd instanceof ReadableByteChannel) fdRead = (ReadableByteChannel)fd;
        if (fd instanceof WritableByteChannel) fdWrite = (WritableByteChannel)fd;
        if (fd instanceof SeekableByteChannel) fdSeek = (SeekableByteChannel)fd;
        if (fd instanceof SelectableChannel) fdSelect = (SelectableChannel)fd;
    }

    public Stream getPipeStream() {
        return pipeStream;
    }

    public Stream getPipeStreamSafe() throws BadDescriptorException {
        Stream stream = pipeStream;
        if (stream == null) throw new BadDescriptorException();
        return stream;
    }

    public void setPipeStream(Stream pipeStream) {
        this.pipeStream = pipeStream;
    }

    public Stream getWriteStream() {
        return pipeStream == null ? mainStream : pipeStream;
    }

    public Stream getWriteStreamSafe() throws BadDescriptorException {
        Stream stream = pipeStream == null ? mainStream : pipeStream;
        if (stream == null) throw new BadDescriptorException();
        return stream;
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
    
    // mri: rb_io_modestr_fmode
    public static int ioModestrFmode(Ruby runtime, String modesString) {
        try {
            return getFModeFromString(modesString);
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError(modesString);
        }
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
                throw runtime.newSystemCallError("error flushing");
            }
        }
        // don't know what this is
//        if (fptr->tied_io_for_writing) {
//            rb_io_t *wfptr;
//            GetOpenFile(fptr->tied_io_for_writing, wfptr);
//            if (io_fflush(wfptr) < 0)
//                rb_sys_fail(0);
//        }
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
        // errno logic checking here
        return ready(runtime, SelectionKey.OP_WRITE | SelectionKey.OP_ACCEPT);
    }

    public boolean ready(Ruby runtime, int ops) {
        try {
            if (fdSelect != null) {
                int ready_stat = 0;
                java.nio.channels.Selector sel = SelectorFactory.openWithRetryFrom(null, fdSelect.provider());
                synchronized (fdSelect.blockingLock()) {
                    boolean is_block = fdSelect.isBlocking();
                    try {
                        fdSelect.configureBlocking(false);
                        fdSelect.register(sel, ops);
                        ready_stat = sel.selectNow();
                        sel.close();
                    } finally {
                        if (sel != null) {
                            try {
                                sel.close();
                            } catch (Exception e) {
                            }
                        }
                        fdSelect.configureBlocking(is_block);
                    }
                }
                return ready_stat == 1;
            } else {
                if (fdSeek != null) {
                    return fdSeek.position() < fdSeek.size();
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
//            errno = EAGAIN;
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
            int r = fdWrite.write(tmp);

            if (wbuf.len <= r) {
                wbuf.off = 0;
                wbuf.len = 0;
                return 0;
            }
            if (0 <= r) {
                wbuf.off += (int)r;
                wbuf.len -= (int)r;
//                errno = EAGAIN;
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
            if (fd instanceof FileChannel
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
            throw context.runtime.newSystemCallError("");
        unread(context);
//        errno = 0;
    }

    // pseudo lseek(2)
    private long lseek(ThreadContext context, long offset, int type) {
        if (fdSeek != null) {
            int adj = 0;
            try {
                switch (type) {
                    case SEEK_SET:
                        return fdSeek.position(offset).position();
                    case SEEK_CUR:
                        return fdSeek.position(fdSeek.position() - adj + offset).position();
                    case SEEK_END:
                        return fdSeek.position(fdSeek.size() + offset).position();
                    default:
                        throw context.runtime.newArgumentError("invalid seek whence: " + type);
                }
            } catch (IllegalArgumentException e) {
                throw context.runtime.newErrnoEINVALError();
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        } else if (fdSelect != null) {
            // TODO: It's perhaps just a coincidence that all the channels for
            // which we should raise are instanceof SelectableChannel, since
            // stdio is not...so this bothers me slightly. -CON
            throw context.runtime.newErrnoEPIPEError();
        } else {
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
        if (mainStream == null && pipeStream == null) {
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
        if (mainStream != null) {
            mainStream.setBinmode();
        }
        if (pipeStream != null) {
            pipeStream.setBinmode();
        }
    }

    public boolean isOpen() {
        return mainStream != null || pipeStream != null;
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

    public void setReadBuffered() {
    }

    public boolean isWriteBuffered() {
        return false;
    }

    public void setWriteBuffered() {
    }

    public void setSync(boolean sync) {
        if(sync) {
            mode = mode | SYNC;
        } else {
            mode = mode & ~SYNC;
        }
    }

    public boolean isSync() {
        return (mode & SYNC) != 0;
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
    
    public void setStdio(boolean stdio) {
        this.stdio = true;
    }
    
    public boolean isStdio() {
        return stdio;
    }

    public Finalizer getFinalizer() {
        return finalizer;
    }

    public void setFinalizer(Finalizer finalizer) {
        this.finalizer = finalizer;
    }

    public void cleanup(Ruby runtime, boolean raise) {
        if (finalizer != null) {
            finalizer.finalize(runtime, raise);
        } else {
            finalize(runtime, raise);
        }
    }

    public void finalize(Ruby runtime, boolean raise) {
        try {
            ChannelDescriptor pipe = null;
            
            // TODO: writeconv the remaining bytes in the stream?
            
//            if (isStdio()) return;
            
            // Recent JDKs shut down streams in the parent when child
            // terminates, so we can't trust that they'll be open for our
            // close. Check for that.
            
            boolean isProcess = process != null;

            synchronized (this) {
                Stream ps = pipeStream;
                if (ps != null) {
                    pipe = ps.getDescriptor();

                    try {
                        // Newer JDKs actively close the process streams when
                        // the child exits, so we have to confirm it's still
                        // open to avoid raising an error here when we try to
                        // flush and close the stream.
                        if (isProcess && ps.getChannel().isOpen()
                                || !isProcess) {
                            ps.fflush();
                            ps.fclose();
                        }
                    } finally {
                        // make sure the pipe stream is set to null
                        pipeStream = null;
                    }
                }
                Stream ms = mainStream;
                if (ms != null) {
                    // TODO: Ruby logic is somewhat more complicated here, see comments after
                    ChannelDescriptor main = ms.getDescriptor();
                    runtime.removeFilenoIntMap(main.getFileno());
                    try {
                        // Newer JDKs actively close the process streams when
                        // the child exits, so we have to confirm it's still
                        // open to avoid raising an error here when we try to
                        // flush and close the stream.
                        if (isProcess) {
                            if (ms.getChannel().isOpen()) {
                                if (pipe == null && isWriteBuffered()) {
                                    ms.fflush();
                                }
                                try {
                                    ms.fclose();
                                } catch (IOException ioe) {
                                    // OpenJDK 7 seems to leave the FileChannel in a state where
                                    // the fd is no longer valid, but the channel is not marked
                                    // as open, so we get IOException: Bad file descriptor here.

                                    if (!ioe.getMessage().equals("Bad file descriptor")) throw ioe;

                                    // If the process is still alive, allow the error to propagate

                                    boolean isAlive = false;
                                    try { process.exitValue(); } catch (IllegalThreadStateException itse) { isAlive = true; }
                                    if (isAlive) throw ioe;
                                }
                            }
                        } else {
                            if (pipe == null && isWriteBuffered()) {
                                ms.fflush();
                            }
                            ms.fclose();
                        }
                    } catch (BadDescriptorException bde) {
                        if (main != pipe) throw bde;
                    } finally {
                        // make sure the main stream is set to null
                        mainStream = null;
                    }
                }
            }
        } catch (IOException ex) {
            if (raise) {
                throw runtime.newIOErrorFromException(ex);
            }
        } catch (BadDescriptorException ex) {
            if (raise) {
                throw runtime.newErrnoEBADFError();
            }
        } catch (Throwable t) {
            t.printStackTrace();
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

    private static final int MORE_CHAR_SUSPENDED = 0;
    private static final int MORE_CHAR_FINISHED = 1;
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

    private boolean NEED_NEWLINE_DECORATOR_ON_READ() {
        return isTextMode();
    }

    private int moreChar(ThreadContext context) {
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

            exc = EncodingUtils.makeEconvException(context, readconv);
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
    private int readBufferedData(byte[] ptrBytes, int ptr, int len) {
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
                r = readInternal(context, fdRead, rbuf.ptr, 0, rbuf.capa);

                if (r < 0) {
                    if (waitReadable(context, fdRead)) {
                        continue retry;
                    }
                    // This should be errno
                    throw context.runtime.newSystemCallError("channel: " + fd + (pathv != null ? " " + pathv : ""));
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
        InternalReadStruct(ReadableByteChannel fd, byte[] bufBytes, int buf, int count) {
            this.fd = fd;
            this.bufBytes = bufBytes;
            this.buf = buf;
            this.capa = count;
        }

        public ReadableByteChannel fd;
        public byte[] bufBytes;
        public int buf;
        public int capa;
    }

    final RubyThread.Task<InternalReadStruct, Integer> readTask = new RubyThread.Task<InternalReadStruct, Integer>() {
        @Override
        public Integer run(ThreadContext context, InternalReadStruct iis) throws InterruptedException {
            // TODO: make nonblocking (when possible? MRI doesn't seem to...)
            // FIXME: inefficient to recreate ByteBuffer every time
            try {
                ByteBuffer buffer = ByteBuffer.wrap(iis.bufBytes, iis.buf, iis.capa);
                int read = iis.fd.read(buffer);

                // FileChannel returns -1 upon reading to EOF rather than blocking, so we call that 0 like read(2)
                if (read == -1 && iis.fd instanceof FileChannel) read = 0;

                return read;
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }

        @Override
        public void wakeup(RubyThread thread, InternalReadStruct self) {
            // FIXME: NO! This will kill many native channels. Must be nonblocking to interrupt.
            thread.getNativeThread().interrupt();
        }
    };

    final RubyThread.Task<InternalWriteStruct, Integer> writeTask = new RubyThread.Task<InternalWriteStruct, Integer>() {
        @Override
        public Integer run(ThreadContext context, InternalWriteStruct iis) throws InterruptedException {
            // TODO: make nonblocking (when possible? MRI doesn't seem to...)
            // FIXME: inefficient to recreate ByteBuffer every time
            try {
                ByteBuffer buffer = ByteBuffer.wrap(iis.bufBytes, iis.buf, iis.capa);
                int written = iis.fd.write(buffer);

                return written;
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }

        @Override
        public void wakeup(RubyThread thread, InternalWriteStruct self) {
            // FIXME: NO! This will kill many native channels. Must be nonblocking to interrupt.
            thread.getNativeThread().interrupt();
        }
    };

    // rb_read_internal
    public int readInternal(ThreadContext context, ReadableByteChannel fd, byte[] bufBytes, int buf, int count) {
        InternalReadStruct iis = new InternalReadStruct(fd, bufBytes, buf, count);

        try {
            return context.getThread().executeTask(context, iis, readTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("IO operation interrupted");
        }
    }

    // rb_io_wait_readable
    boolean waitReadable(ThreadContext context, Channel fd)
    {
        if (!fd.isOpen()) {
            throw context.runtime.newIOError("closed stream");
        }

        if (fd instanceof SelectableChannel) {
            return context.getThread().select(fd, null, SelectionKey.OP_READ);
        }

        // kinda-hacky way to see if there's more data to read from a seekable channel
        if (fd instanceof SeekableByteChannel) {
            SeekableByteChannel fdSeek = (SeekableByteChannel)fd;
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

//    static ssize_t
//    rb_write_internal(int fd, const void *buf, size_t count)
//    {
//        struct io_internal_write_struct iis;
//        iis.fd = fd;
//        iis.buf = buf;
//        iis.capa = count;
//
//        return (ssize_t)rb_thread_io_blocking_region(internal_write_func, &iis, fd);
//    }

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
                    c = readInternal(context, fdRead, ptrBytes, ptr + offset, n);
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
        if (len < 0) throw context.runtime.newSystemCallError(pathv);
        return len;
    }

    public void ungetbyte(ThreadContext context, IRubyObject str)
    {
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
        if (fdSeek != null) {
            try {
                return fdSeek.position();
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }

        return -1;
    }

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

//        errno = 0;
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
        // TODO: ???
//        if (pos < 0 && errno) {
//            if (errno == ESPIPE)
//                fptr->mode |= FMODE_DUPLEX;
//            return;
//        }

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
            read_size = readInternal(context, fdRead, bufBytes, buf, rbuf.len + newlines);
            if (read_size < 0) {
                throw runtime.newSystemCallError(pathv);
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

    /* don't write anything if current thread has a pending interrupt. */
        context.pollThreadEvents();

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
        if ((!nosync && (mode & (SYNC|TTY)) != 0) ||
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
                    r = writeInternal(context, fdWrite, ptrBytes, ptr + offset, l);
                }
        /* xxx: other threads may modify given string. */
                if (r == n) return len;
                if (0 <= r) {
                    offset += r;
                    n -= r;
    //                errno = EAGAIN;
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
        return p.fptr.writeInternal2(context, p.fptr.fdWrite, p.ptrBytes, p.ptr, l);
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
    int writeInternal(ThreadContext context, WritableByteChannel fd, byte[] bufBytes, int buf, int count)
    {
        InternalWriteStruct iis = new InternalWriteStruct(fd, bufBytes, buf, count);

        try {
            return context.getThread().executeTask(context, iis, writeTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("IO operation interrupted");
        }
    }

    // rb_write_internal2 (no GVL version...we just don't use executeTask as above.
    int writeInternal2(ThreadContext context, WritableByteChannel fd, byte[] bufBytes, int buf, int count)
    {
        InternalWriteStruct iis = new InternalWriteStruct(fd, bufBytes, buf, count);

        try {
            return writeTask.run(context, iis);
        } catch (InterruptedException ie) {
            throw context.runtime.newConcurrencyError("IO operation interrupted");
        }
    }
}

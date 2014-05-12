package org.jruby.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ShellLauncher;

public class OpenFile {

    // IO Mode flags
    public static final int READABLE           = 0x00000001;
    public static final int WRITABLE           = 0x00000002;
    public static final int READWRITE          = READABLE | WRITABLE;
    public static final int BINMODE            = 0x00000004;
    public static final int SYNC               = 0x00000008;
    public static final int WBUF               = 0x00000010; // TTY
    public static final int RBUF               = 0x00000020; // DUPLEX
    public static final int APPEND             = 0x00000040;
    public static final int CREATE             = 0x00000080;
    public static final int WSPLIT             = 0x00000200;
    public static final int WSPLIT_INITIALIZED = 0x00000400;
    public static final int TRUNC              = 0x00000800;
    public static final int TEXTMODE           = 0x00001000;
    public static final int SETENC_BY_BOM      = 0x00100000;
    
    public static final int SYNCWRITE = SYNC | WRITABLE;

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
    private int lineNumber = 0;
    private String path;
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

    public final ReentrantReadWriteLock write_lock = new ReentrantReadWriteLock();

    public final Buffer wbuf = new Buffer(), rbuf = new Buffer(), cbuf = new Buffer();

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

    public static final int PIPE_BUF = 512; // value of _POSIX_PIPE_BUF from Mac OS X 10.9

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

    public void seek(long offset, int whence) throws IOException, InvalidValueException, PipeException, BadDescriptorException {
        Stream stream = getWriteStreamSafe();

        seekInternal(stream, offset, whence);
    }

    private void seekInternal(Stream stream, long offset, int whence) throws IOException, InvalidValueException, PipeException, BadDescriptorException {
        flushBeforeSeek(stream);

        stream.lseek(offset, whence);
    }

    private void flushBeforeSeek(Stream stream) throws BadDescriptorException, IOException {
        if ((mode & WBUF) != 0) {
            fflush(stream);
        }
    }

    public void fflush(Stream stream) throws IOException, BadDescriptorException {
        while (true) {
            int n = stream.fflush();
            if (n != -1) {
                break;
            }
        }
        mode &= ~WBUF;
    }

    public void checkWritable(Ruby runtime) throws IOException, BadDescriptorException, InvalidValueException {
        checkClosed(runtime);
        if ((mode & WRITABLE) == 0) {
            throw runtime.newIOError("not opened for writing");
        }
        if ((mode & RBUF) != 0 && !mainStream.feof() && pipeStream == null) {
            try {
                // seek to force read buffer to invalidate
                seek(0, Stream.SEEK_CUR);
            } catch (PipeException p) {
                // ignore unseekable streams for purposes of checking readability
            } catch (IOException ioe) {
                // MRI ignores seek errors, presumably for unseekable files like
                // serial ports (JRUBY-2979), so we shall too.
            }
        }
        if (pipeStream == null) {
            mode &= ~RBUF;
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
        return (mode & RBUF) != 0;
    }

    public void setReadBuffered() {
        mode |= RBUF;
    }

    public boolean isWriteBuffered() {
        return (mode & WBUF) != 0;
    }

    public void setWriteBuffered() {
        mode |= WBUF;
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
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
    // Missing flags and doubling readTranscoder as transcoder and whether transcoder has been initializer (ick).
    public void makeReadConversion(ThreadContext context) {
        if (readconv != null) return;

        int ecflags;
        IRubyObject ecopts;
        byte[] sname, dname;
        ecflags = encs.ecflags & ~EConvFlags.NEWLINE_DECORATOR_WRITE_MASK;
        ecopts = encs.ecopts;

        if (encs.enc2 != null) {
            sname = encs.enc2.getName();
            dname = encs.enc.getName();
        } else {
            sname = dname = EMPTY_BYTE_ARRAY;
        }

        readconv = EncodingUtils.econvOpenOpts(context, sname, dname, ecflags, ecopts);

        if (readconv == null) {
            throw EncodingUtils.econvOpenExc(context, sname, dname, ecflags);
        }

        // rest of MRI code sets up read/write buffers
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

    private void NEED_NEWLINE_DECORATOR_ON_READ_CHECK() {
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
    int fillbuf(ThreadContext context) {
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
                    throw context.runtime.newSystemCallError("channel: " + fd + (path != null ? " " + path : ""));
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
            // TODO: make nonblocking (when possible?)
            // FIXME: inefficient to recreate ByteBuffer every time
            ByteBuffer buffer = ByteBuffer.wrap(iis.bufBytes, iis.buf, iis.capa - iis.buf);
            try {
                return iis.fd.read(buffer);
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

//    final RubyThread.Task<IRubyObject, IRubyObject> putTask = new RubyThread.Task<IRubyObject, IRubyObject>() {
//        @Override
//        public IRubyObject run(ThreadContext context, IRubyObject data) throws InterruptedException {
//            final BlockingQueue<IRubyObject> queue = getQueueSafe();
//            queue.put(data);
//            return context.nil;
//        }
//
//        @Override
//        public void wakeup(RubyThread thread, IRubyObject data) {
//            thread.getNativeThread().interrupt();
//        }
//    };

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
    public Object shiftCbuf(ThreadContext context, int len, IRubyObject strp) {
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

}

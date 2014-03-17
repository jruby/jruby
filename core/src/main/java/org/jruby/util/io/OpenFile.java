package org.jruby.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.jcodings.Encoding;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
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
    private Channel mainChannel;
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

    // unnecessary with JVM locking?
    // VALUE write_lock;

    public final Buffer wbuf = new Buffer(), rbuf = new Buffer(), cbuf = new Buffer();

    public boolean READ_DATA_PENDING() {return rbuf.len != 0;}
    public int READ_DATA_PENDING_COUNT() {return rbuf.len;}
    public byte[] READ_DATA_PENDING_PTR() {return rbuf.ptr;}
    public int READ_DATA_PENDING_START() {return rbuf.start;}
    public boolean READ_DATA_BUFFERED() {return READ_DATA_PENDING();}

    public boolean READ_CHAR_PENDING() {return cbuf.len != 0;}
    public int READ_CHAR_PENDING_COUNT() {return cbuf.len;}
    public byte[] READ_CHAR_PENDING_PTR() {return cbuf.ptr;}
    public int READ_CHAR_PENDING_START() {return cbuf.start;}

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
        this.mainChannel = mainStream.getChannel();
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

    public void checkReadable(Ruby runtime) throws IOException, BadDescriptorException, InvalidValueException {
        checkClosed(runtime);

        if ((mode & READABLE) == 0) {
            throw runtime.newIOError("not opened for reading");
        }

        if (((mode & WBUF) != 0 || (mode & (SYNCWRITE | RBUF)) == SYNCWRITE) &&
                !mainStream.feof() && pipeStream == null) {
            try {
                // seek to force underlying buffer to flush
                seek(0, Stream.SEEK_CUR);
            } catch (PipeException p) {
                // ignore unseekable streams for purposes of checking readability
            } catch (IOException ioe) {
                // MRI ignores seek errors, presumably for unseekable files like
                // serial ports (JRUBY-2979), so we shall too.
            }
        }
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
    private boolean needsWriteConversion(ThreadContext context) {
        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();

        return (encs.enc != null && encs.enc != ascii8bit) || isTextMode() ||
                (encs.ecflags & ((EConvFlags.DECORATOR_MASK & ~EConvFlags.CRLF_NEWLINE_DECORATOR)| EConvFlags.STATEFUL_DECORATOR_MASK)) != 0;
    }

    // MRI: make_readconv
    // Missing flags and doubling readTranscoder as transcoder and whether transcoder has been initializer (ick).
    private void makeReadConversion(ThreadContext context) {
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
    private void makeWriteConversion(ThreadContext context) {
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

    private void clearReadConversion() {
        readconv = null;
    }

    private void clearCodeConversion() {
        readconv = null;
        writeconv = null;
    }

    private static final int MORE_CHAR_SUSPENDED = 0;
    private static final int MORE_CHAR_FINISHED = 1;
    public static final int EOF = -1;

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
                int searchlen = cbuf.len;
                if (searchlen > 0) {
                    byte[] pBytes = cbuf.ptr;
                    p = cbuf.start;
                    if (0 < limit && limit < searchlen)
                        searchlen = limit;
                    e = 0;
                    for (int i = p; i < p + searchlen; i++) if ((pBytes[i] & 0xFF) == delim) e = i;
                    if (e != 0) {
                        int len = (int)(e-p+1);
                        if (str != null) {
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
                        str.append(pBytes, p, searchlen);
                    }
                    cbuf.off += searchlen;
                    cbuf.len -= searchlen;
                    limit -= searchlen;

                    if (limit == 0) {
                        lp[0] = limit;
                        return str.get(str.getRealSize() - 1) & 0xFF;
                    }
                }
            } while (moreChar() != MORE_CHAR_FINISHED);
            clearReadConversion();
            lp[0] = limit;
            return EOF;
        }

        NEED_NEWLINE_DECORATOR_ON_READ_CHECK();
        do {
            int pending = rbuf.len;
            if (pending > 0) {
                byte[] pBytes = rbuf.ptr;
                int p = rbuf.start;
                int e;
                int last;

                if (limit > 0 && pending > limit) pending = limit;
                e = -1;
                for (int i = p; i < p + pending; i++) if ((pBytes[i] & 0xFF) == delim) e = i;
                if (e != -1) pending = e - p + 1;
                if (str != null) {
                    last = str.getRealSize();
                    str.ensure(last + pending);
                }
                else {
                    last = 0;
                    strp[0] = str = new ByteList(pending);
                }
                readBufferedData(str.getUnsafeBytes(), str.getBegin() + last, pending); /* must not fail */
                limit -= pending;
                lp[0] = limit;
                if (e != -1) return delim;
                if (limit == 0) {
                    return str.get(str.getRealSize() - 1) & 0xFF;
                }
            }
            checkReadable(runtime);
        } while (fillbuf() >= 0);
        lp[0] = limit;
        return EOF;
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

    private int moreChar() {

    }

    private int fillCbuf() {
        byte[] sBytes, dBytes;
        int ss, sp, se;
        int ds, dp, de;
        EConvResult res;
        int putbackable;
        int cbuf_len0;
        RaiseException exc;

        Stream readStream = getOpenFile().getMainStream();
        ByteBuffer readBuffer = readStream.getBuffer();

        ecflags |= EConvFlags.PARTIAL_INPUT;

        // taken care of by fill below
//        if (fptr->cbuf.len == fptr->cbuf.capa)
//            return MORE_CHAR_SUSPENDED; /* cbuf full */
//        if (fptr->cbuf.len == 0)
//            fptr->cbuf.off = 0;
//        else if (fptr->cbuf.off + fptr->cbuf.len == fptr->cbuf.capa) {
//            memmove(fptr->cbuf.ptr, fptr->cbuf.ptr+fptr->cbuf.off, fptr->cbuf.len);
//            fptr->cbuf.off = 0;
//        }
//
//        cbuf_len0 = fptr->cbuf.len;

        while (true) {
            ss = sp = (const unsigned char *)fptr->rbuf.ptr + fptr->rbuf.off;
            se = sp + fptr->rbuf.len;
            ds = dp = (unsigned char *)fptr->cbuf.ptr + fptr->cbuf.off + fptr->cbuf.len;
            de = (unsigned char *)fptr->cbuf.ptr + fptr->cbuf.capa;
            res = rb_econv_convert(fptr->readconv, &sp, se, &dp, de, ec_flags);
            fptr->rbuf.off += (int)(sp - ss);
            fptr->rbuf.len -= (int)(sp - ss);
            fptr->cbuf.len += (int)(dp - ds);

            putbackable = rb_econv_putbackable(fptr->readconv);
            if (putbackable) {
                rb_econv_putback(fptr->readconv, (unsigned char *)fptr->rbuf.ptr + fptr->rbuf.off - putbackable, putbackable);
                fptr->rbuf.off -= putbackable;
                fptr->rbuf.len += putbackable;
            }

            exc = rb_econv_make_exception(fptr->readconv);
            if (!NIL_P(exc))
                return exc;

            if (cbuf_len0 != fptr->cbuf.len)
                return MORE_CHAR_SUSPENDED;

            if (res == econv_finished) {
                return MORE_CHAR_FINISHED;
            }

            if (res == econv_source_buffer_empty) {
                if (fptr->rbuf.len == 0) {
                    READ_CHECK(fptr);
                    if (io_fillbuf(fptr) == -1) {
                        if (!fptr->readconv) {
                            return MORE_CHAR_FINISHED;
                        }
                        ds = dp = (unsigned char *)fptr->cbuf.ptr + fptr->cbuf.off + fptr->cbuf.len;
                        de = (unsigned char *)fptr->cbuf.ptr + fptr->cbuf.capa;
                        res = rb_econv_convert(fptr->readconv, NULL, NULL, &dp, de, 0);
                        fptr->cbuf.len += (int)(dp - ds);
                        rb_econv_check_error(fptr->readconv);
                        break;
                    }
                }
            }
        }
        if (cbuf_len0 != fptr->cbuf.len)
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
}

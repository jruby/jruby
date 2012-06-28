package org.jruby.util.io;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.util.ShellLauncher;

public class OpenFile {

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
    private Stream mainStream;
    private Stream pipeStream;
    private int mode;
    private Process process;
    private int lineNumber = 0;
    private String path;
    private Finalizer finalizer;

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
            ChannelDescriptor main = null;
            ChannelDescriptor pipe = null;
            
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
                    main = ms.getDescriptor();
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
                        if (main == pipe) {
                        } else {
                            throw bde;
                        }
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
}

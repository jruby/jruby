package org.jruby.util.io;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.util.ShellLauncher;

public class OpenFile {

    public static final int READABLE = 1;
    public static final int WRITABLE = 2;
    public static final int READWRITE = 3;
    public static final int APPEND = 64;
    public static final int CREATE = 128;
    public static final int BINMODE = 4;
    public static final int SYNC = 8;
    public static final int WBUF = 16;
    public static final int RBUF = 32;
    public static final int WSPLIT = 512;
    public static final int WSPLIT_INITIALIZED = 1024;
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

    public synchronized void setMainStream(Stream mainStream) {
        this.mainStream = mainStream;
    }

    public Stream getPipeStream() {
        return pipeStream;
    }

    public synchronized void setPipeStream(Stream pipeStream) {
        this.pipeStream = pipeStream;
    }

    public Stream getWriteStream() {
        return pipeStream == null ? mainStream : pipeStream;
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

    public void checkReadable(Ruby runtime) throws IOException, BadDescriptorException, PipeException, InvalidValueException {
        checkClosed(runtime);

        if ((mode & READABLE) == 0) {
            throw runtime.newIOError("not opened for reading");
        }

        if (((mode & WBUF) != 0 || (mode & (SYNCWRITE | RBUF)) == SYNCWRITE)
                && !mainStream.feof() && pipeStream == null) {
            try {
                // seek to force underlying buffer to flush
                seek(0, Stream.SEEK_CUR);
            } catch (IOException ioe) {
                // MRI ignores seek errors, presumably for unseekable files like
                // serial ports (JRUBY-2979), so we shall too.
            }
        }
    }

    public void seek(long offset, int whence) throws IOException, InvalidValueException, PipeException, BadDescriptorException {
        flushBeforeSeek();

        getWriteStream().lseek(offset, whence);
    }

    private void flushBeforeSeek() throws BadDescriptorException, IOException {
        if ((mode & WBUF) != 0) {
            fflush(getWriteStream());
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

    public void checkWritable(Ruby runtime) throws IOException, BadDescriptorException, InvalidValueException, PipeException {
        checkClosed(runtime);
        if ((mode & WRITABLE) == 0) {
            throw runtime.newIOError("not opened for writing");
        }
        if ((mode & RBUF) != 0 && !mainStream.feof() && pipeStream == null) {
            try {
                // seek to force read buffer to invalidate
                seek(0, Stream.SEEK_CUR);
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

            synchronized (this) {
                Stream ps = pipeStream;
                if (ps != null) {
                    pipe = ps.getDescriptor();

                    // TODO: Ruby logic is somewhat more complicated here, see comments after
                    try {
                        ps.fflush();
                        ps.fclose();
                    } finally {
                        // make sure the pipe stream is set to null
                        pipeStream = null;
                        runtime.unregisterDescriptor(pipe.getFileno());
                    }
                }
                Stream ms = mainStream;
                if (ms != null) {
                    // TODO: Ruby logic is somewhat more complicated here, see comments after
                    main = ms.getDescriptor();
                    try {
                        if (pipe == null && isWriteBuffered()) {
                            ms.fflush();
                        }
                        ms.fclose();
                    } catch (BadDescriptorException bde) {
                        if (main == pipe) {
                        } else {
                            throw bde;
                        }
                    } finally {
                        // make sure the main stream is set to null
                        mainStream = null;
                        runtime.unregisterDescriptor(main.getFileno());
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

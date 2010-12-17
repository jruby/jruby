package org.jruby.util.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import org.jruby.Ruby;
import org.jruby.util.ByteList;

/**
 * Wrapper around Stream that packs and unpacks LF <=> CRLF.
 * @author nicksieger
 */
public class CRLFStreamWrapper implements Stream {
    private final Stream stream;
    private boolean binmode = false;
    private static final int CR = 13;
    private static final int LF = 10;

    public CRLFStreamWrapper(Stream stream) {
        this.stream = stream;
    }

    public ChannelDescriptor getDescriptor() {
        return stream.getDescriptor();
    }

    public void clearerr() {
        stream.clearerr();
    }

    public ModeFlags getModes() {
        return stream.getModes();
    }

    public boolean isSync() {
        return stream.isSync();
    }

    public void setSync(boolean sync) {
        stream.setSync(sync);
    }

    public void setBinmode() {
        binmode = true;
        stream.setBinmode();
    }

    public boolean isAutoclose() {
        return stream.isAutoclose();
    }

    public void setAutoclose(boolean autoclose) {
        stream.setAutoclose(autoclose);
    }

    public ByteList fgets(ByteList separatorString) throws IOException, BadDescriptorException, EOFException {
        return convertCRLFToLF(stream.fgets(separatorString));
    }

    public ByteList readall() throws IOException, BadDescriptorException, EOFException {
        return convertCRLFToLF(stream.readall());
    }

    public int getline(ByteList dst, byte terminator) throws IOException, BadDescriptorException {
        if (binmode) {
            return stream.getline(dst, terminator);
        }

        ByteList intermediate = new ByteList();
        int result = stream.getline(intermediate, terminator);
        convertCRLFToLF(intermediate, dst);
        return result;
    }

    public int getline(ByteList dst, byte terminator, long limit) throws IOException, BadDescriptorException {
        if (binmode) {
            return stream.getline(dst, terminator, limit);
        }

        ByteList intermediate = new ByteList();
        int result = stream.getline(intermediate, terminator, limit);
        convertCRLFToLF(intermediate, dst);
        return result;
    }

    public ByteList fread(int number) throws IOException, BadDescriptorException, EOFException {
        if (number == 0) {
            if (stream.feof()) {
                return null;
            } else {
                return new ByteList(0);
            }
        }
        boolean eof = false;
        ByteList bl = new ByteList(number > ChannelStream.BUFSIZE ? ChannelStream.BUFSIZE : number);
        for (int i = 0; i < number; i++) {
            int c = fgetc();
            if (c == -1) {
                eof = true;
                break;
            }
            bl.append(c);
        }
        if (eof && bl.length() == 0) {
            return null;
        }
        return bl;
    }

    public int fwrite(ByteList string) throws IOException, BadDescriptorException {
        return stream.fwrite(convertLFToCRLF(string));
    }

    public int fgetc() throws IOException, BadDescriptorException, EOFException {
        int c = stream.fgetc();
        if (!binmode && c == CR) {
            c = stream.fgetc();
            if (c != LF) {
                stream.ungetc(c);
                return CR;
            }
        }
        return c;
    }

    public int ungetc(int c) {
        return stream.ungetc(c);
    }

    public void fputc(int c) throws IOException, BadDescriptorException {
        if (!binmode && c == LF) {
            stream.fputc(CR);
        }
        stream.fputc(c);
    }

    public ByteList read(int number) throws IOException, BadDescriptorException, EOFException {
        return convertCRLFToLF(stream.read(number));
    }

    public void fclose() throws IOException, BadDescriptorException {
        stream.fclose();
    }

    public int fflush() throws IOException, BadDescriptorException {
        return stream.fflush();
    }

    public void sync() throws IOException, BadDescriptorException {
        stream.sync();
    }

    public boolean feof() throws IOException, BadDescriptorException {
        return stream.feof();
    }

    public long fgetpos() throws IOException, PipeException, BadDescriptorException, InvalidValueException {
        return stream.fgetpos();
    }

    public void lseek(long offset, int type) throws IOException, InvalidValueException, PipeException, BadDescriptorException {
        stream.lseek(offset, type);
    }

    public void ftruncate(long newLength) throws IOException, PipeException, InvalidValueException, BadDescriptorException {
        stream.ftruncate(newLength);
    }

    public int ready() throws IOException {
        return stream.ready();
    }

    public void waitUntilReady() throws IOException, InterruptedException {
        stream.waitUntilReady();
    }

    public boolean readDataBuffered() {
        return stream.readDataBuffered();
    }

    public boolean writeDataBuffered() {
        return stream.writeDataBuffered();
    }

    public InputStream newInputStream() {
        return stream.newInputStream();
    }

    public OutputStream newOutputStream() {
        return stream.newOutputStream();
    }

    public boolean isBlocking() {
        return stream.isBlocking();
    }

    public void setBlocking(boolean blocking) throws IOException {
        stream.setBlocking(blocking);
    }

    public void freopen(Ruby runtime, String path, ModeFlags modes) throws DirectoryAsFileException, IOException, InvalidValueException, PipeException, BadDescriptorException {
        stream.freopen(runtime, path, modes);
    }

    private ByteList convertCRLFToLF(ByteList input) {
        if (input == null) {
            return null;
        }

        if (binmode) {
            return input;
        }

        ByteList result = new ByteList();
        convertCRLFToLF(input, result);
        return result;
    }

    private void convertCRLFToLF(ByteList src, ByteList dst) {
        for (int i = 0; i < src.length(); i++) {
            int b = src.get(i);
            if (b == CR && i + 1 < src.length() && src.get(i + 1) == LF) {
                continue;
            }
            dst.append(b);
        }
    }

    private ByteList convertLFToCRLF(ByteList input) {
        if (input == null) {
            return null;
        }

        if (binmode) {
            return input;
        }

        ByteList result = new ByteList();
        for (int i = 0; i < input.length(); i++) {
            int b = input.get(i);
            if (b == LF) {
                result.append(CR);
            }
            result.append(b);
        }
        return result;
    }

    public Channel getChannel() {
        return stream.getChannel();
    }
}

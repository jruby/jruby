package org.jruby.util.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.util.ByteList;

/**
 * Wrapper around Stream that packs and unpacks LF <=> CRLF.
 * @author nicksieger
 */
public class CRLFStreamWrapper implements Stream {
    private final Stream stream;
    private final boolean isWindows;
    private boolean binmode = false;
    private static final int CR = 13;
    private static final int LF = 10;

    public CRLFStreamWrapper(Stream stream) {
        this.stream = stream;
        // To differentiate between textmode and windows in how we handle crlf.
        this.isWindows = Platform.IS_WINDOWS;
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
    
    public void setModes(ModeFlags modes) {
        stream.setModes(modes);
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

    public boolean isBinmode() {
        return binmode;
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
        if (isWindows) return stream.fwrite(convertLFToCRLF(string));
        
        return stream.fwrite(convertCRLFToLF(string));
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
        if (input == null || binmode) return input;        

        ByteList result = new ByteList();
        convertCRLFToLF(input, result);
        return result;
    }

    // FIXME: Horrific hack until we properly setup transcoding support of cr/lf logic in 1.9 proper.  This class
    // is going away in 9k and the LE/BE logic is never used by 1.8 support.
    
    // I could not find any way in MRI to exercise this logic....endless needs
    // binmode set (which obviously would not work here).  Leaving it for now
    // since I will likely be either doubling down on new knowledge for 1.7.2
    // or ripping all this out when we have real transcoding logic ported
    // properly
//    private int skipCROfLF(ByteList src, int i, int c) {
//        Encoding encoding = src.getEncoding();
//        int length = src.length();
//        
//        if (encoding == UTF16BEEncoding.INSTANCE) {
//            if (i + 3 < length && c == 0 && src.get(i + 1) == CR && 
//                    src.get(i + 2) == 0 && src.get(i + 3) == LF) {
//                return i + 1;
//            }
//        } else if (encoding == UTF16LEEncoding.INSTANCE) {
//            if (i + 3 < length && c == CR && src.get(i + 1) == 0 && 
//                    src.get(i + 2) == LF && src.get(i + 3) == 0) {
//                return i + 1;
//            }            
//        } else if (c == CR && i + 1 < length && src.get(i + 1) == LF) {
//            return i;
//        }
//        
//        return -1;
//    }
//    
//    private void convertCRLFToLF(ByteList src, ByteList dst) {
//        for (int i = 0; i < src.length(); i++) {
//            int b = src.get(i);
//            int j = skipCROfLF(src, i, b);
//            if (j != -1) i = j;
//
//            dst.append(b);
//        }
//    }
    
    
    private void convertCRLFToLF(ByteList src, ByteList dst) {
        for (int i = 0; i < src.length(); i++) {
            int b = src.get(i);
            if (b == CR && i + 1 < src.length() && src.get(i + 1) == LF) {
                continue;
            }
            dst.append(b);
        }
    }

    final byte[] CRBYTES = new byte[] { CR };
    final byte[] CRLEBYTES = new byte[] { CR, 0};
    final byte[] CRBEBYTES = new byte[] { 0, CR };
    
    private byte[] crBytes(Encoding encoding) {
        if (encoding == UTF16BEEncoding.INSTANCE) return CRBEBYTES;
        if (encoding == UTF16LEEncoding.INSTANCE) return CRLEBYTES;
            
        return CRBYTES;
    }
    
    final byte[] LFBYTES = new byte[] { LF };
    final byte[] LFLEBYTES = new byte[] { LF, 0 };
    final byte[] LFBEBYTES = new byte[] { 0, LF };    

    private byte[] lfBytes(Encoding encoding) {
        if (encoding == UTF16BEEncoding.INSTANCE) return LFBEBYTES;
        if (encoding == UTF16LEEncoding.INSTANCE) return LFLEBYTES;
            
        return LFBYTES;
    }
    
    private ByteList convertLFToCRLF(ByteList bs) {
        if (bs == null || binmode) return bs;

        byte[] crBytes = crBytes(bs.getEncoding());
        byte[] lfBytes = lfBytes(bs.getEncoding());
        
        int p = bs.getBegin();
        int end = p + bs.getRealSize();
        byte[]bytes = bs.getUnsafeBytes();
        Encoding enc = bs.getEncoding();

        ByteList result = new ByteList();
        int lastWrittenIndex = p;
        while (p < end) {
            int c = enc.mbcToCode(bytes, p, end);
            int cLength = enc.codeToMbcLength(c);

            if (c == LF) {
                result.append(bytes, lastWrittenIndex, p - lastWrittenIndex);
                result.append(crBytes);
                result.append(lfBytes);
                lastWrittenIndex = p + cLength;
            }

            p += cLength;
        }
        
        if (lastWrittenIndex < end) {
            result.append(bytes, lastWrittenIndex, end - lastWrittenIndex);
        }

        return result;
    }

    public Channel getChannel() {
        return stream.getChannel();
    }
}

/*
 */
package org.jruby.ext.zlib;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import static org.jruby.CompatVersion.RUBY1_8;
import static org.jruby.CompatVersion.RUBY1_9;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.ByteList;
import org.jruby.util.IOInputStream;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingOption;
import org.jruby.util.io.Stream;

/**
 *
 * @author enebo
 */
@JRubyClass(name = "Zlib::GzipReader", parent = "Zlib::GzipFile", include = "Enumerable")
public class JZlibRubyGzipReader extends RubyGzipFile {
    @JRubyClass(name = "Zlib::GzipReader::Error", parent = "Zlib::GzipReader")
    public static class Error {}
    
    protected static final ObjectAllocator GZIPREADER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new JZlibRubyGzipReader(runtime, klass);
        }
    };

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static JZlibRubyGzipReader newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass) recv;
        JZlibRubyGzipReader result = (JZlibRubyGzipReader) klass.allocate();
        result.callInit(args, block);
        return result;
    }

    @JRubyMethod(name = "open", required = 1, optional = 1, meta = true, compat = RUBY1_8)
    public static IRubyObject open18(final ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("rb"));
        JZlibRubyGzipReader gzio = newInstance(recv, new IRubyObject[]{io}, block);
        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    @JRubyMethod(name = "open", required = 1, optional = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject open19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("rb"));
        JZlibRubyGzipReader gzio = newInstance(recv, argsWithIo(io, args), block);
        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    public JZlibRubyGzipReader(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    private int line;
    private long position;
    private com.jcraft.jzlib.GZIPInputStream io;
    private InputStream bufferedStream;

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(IRubyObject stream) {
        realIo = stream;
        line = 0;
        position = 0;
        try {
            io = new com.jcraft.jzlib.GZIPInputStream(new IOInputStream(realIo),
                    512,
                    false); // don't close realIO
            // JRUBY-4502
            // CRuby expects to parse gzip header in 'new'.
            io.readHeader();
        } catch (IOException e) {
            RaiseException re = RubyZlib.newGzipFileError(getRuntime(),
                    "not in gzip format");
            if (getRuntime().is1_9()) {
                byte[] input = io.getAvailIn();
                if (input != null && input.length > 0) {
                    ByteList i = new ByteList(input, 0, input.length);
                    RubyException rubye = re.getException();
                    rubye.setInstanceVariable("@input",
                            RubyString.newString(getRuntime(), i));
                }
            }
            throw re;
        }
        bufferedStream = new BufferedInputStream(io);
        return this;
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(IRubyObject[] args) {
        IRubyObject obj = initialize(args[0]);
        if (args.length > 1) {
            IRubyObject opt = TypeConverter.checkHashType(getRuntime(), args[args.length - 1]);
            if (!opt.isNil()) {
                EncodingOption enc = EncodingOption.getEncodingOptionFromObject(opt);
                if (enc != null) {
                    readEncoding = enc.getExternalEncoding();
                    writeEncoding = enc.getInternalEncoding();
                }
            }
        }
        if (realIo.respondsTo("path")) {
            obj.getSingletonClass().defineMethod("path", new Callback() {

                public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                    return ((JZlibRubyGzipReader) recv).realIo.callMethod(recv.getRuntime().getCurrentContext(), "path");
                }

                public Arity getArity() {
                    return Arity.NO_ARGUMENTS;
                }
            });
        }
        return obj;
    }

    /**
     * Get position within this stream including that has been read by users
     * calling read + what jzlib may have speculatively read in because of
     * buffering.
     *
     * @return number of bytes
     */
    private long internalPosition() {
        com.jcraft.jzlib.Inflater inflater = io.getInflater();
        return inflater.getTotalIn() + inflater.getAvailIn();
    }

    @JRubyMethod
    public IRubyObject rewind() {
        Ruby rt = getRuntime();
        // should invoke seek on realIo...
        realIo.callMethod(rt.getCurrentContext(), "seek",
                new IRubyObject[]{rt.newFixnum(-internalPosition()), rt.newFixnum(Stream.SEEK_CUR)});
        // ... and then reinitialize
        initialize(realIo);
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno() {
        return getRuntime().newFixnum(line);
    }

    @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
    public IRubyObject readline(ThreadContext context) {
        IRubyObject dst = gets_18(context, new IRubyObject[0]);
        if (dst.isNil()) {
            throw getRuntime().newEOFError();
        }
        return dst;
    }

    private IRubyObject internalGets(IRubyObject[] args) throws IOException {
        ByteList sep = ((RubyString) getRuntime().getGlobalVariables().get("$/")).getByteList();
        int limit = -1;
        switch (args.length) {
            case 0:
                break;
            case 1:
                if (args[0].isNil()) {
                    return readAll();
                }
                IRubyObject tmp = args[0].checkStringType();
                if (tmp.isNil()) {
                    limit = RubyNumeric.fix2int(args[0]);
                } else {
                    sep = tmp.convertToString().getByteList();
                }
                break;
            case 2:
            default:
                limit = RubyNumeric.fix2int(args[1]);
                if (args[0].isNil()) {
                    return readAll(limit);
                }
                sep = args[0].convertToString().getByteList();
                break;
        }
        return internalSepGets(sep, limit);
    }

    private IRubyObject internalSepGets(ByteList sep) throws IOException {
        return internalSepGets(sep, -1);
    }

    private ByteList newReadByteList() {
        ByteList byteList = new ByteList();

        if (readEncoding != null) byteList.setEncoding(readEncoding);

        return byteList;
    }

    private ByteList newReadByteList(int size) {
        ByteList byteList = new ByteList(size);

        if (readEncoding != null) byteList.setEncoding(readEncoding);

        return byteList;
    }

    private IRubyObject internalSepGets(ByteList sep, int limit) throws IOException {
        ByteList result = newReadByteList();

        if (sep.getRealSize() == 0) sep = Stream.PARAGRAPH_SEPARATOR;

        int ce = -1;
        // TODO: CRuby does encoding aware 'gets'. Not yet implemented.
        // StringIO.new("あいう").gets(0) => ""
        // StringIO.new("あいう").gets(1) => "あ"
        // StringIO.new("あいう").gets(2) => "あ"
        // StringIO.new("あいう").gets(3) => "あ"
        // StringIO.new("あいう").gets(4) => "あい"
        // StringIO.new("あいう").gets(5) => "あい"
        // StringIO.new("あいう").gets(6) => "あい"
        // StringIO.new("あいう").gets(7) => "あいう"
        while (result.indexOf(sep) == -1) {
            ce = bufferedStream.read();

            if (ce == -1) break;

            result.append(ce);
            
            if (limit > 0 && result.length() >= limit) break;
        }
        
        // io.available() only returns 0 after EOF is encountered
        // so we need to differentiate between the empty string and EOF
        if (0 == result.length() && -1 == ce) return getRuntime().getNil();

        line++;
        this.position = result.length();
        return newStr(getRuntime(), result);
    }

    @JRubyMethod(name = "gets", optional = 1, writes = FrameField.LASTLINE, compat = RUBY1_8)
    public IRubyObject gets_18(ThreadContext context, IRubyObject[] args) {
        return gets(context, args);
    }

    @JRubyMethod(name = "gets", optional = 2, writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        try {
            IRubyObject result = internalGets(args);

            if (!result.isNil()) context.getCurrentScope().setLastLine(result);
            
            return result;
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }
    private final static int BUFF_SIZE = 4096;

    @JRubyMethod(name = "read", optional = 1)
    public IRubyObject read(IRubyObject[] args) {
        try {
            if (args.length == 0 || args[0].isNil()) return readAll();

            int len = RubyNumeric.fix2int(args[0]);
            
            if (len < 0) throw getRuntime().newArgumentError("negative length " + len + " given");
            if (len > 0) {
                ByteList buf = readSize(len);
                
                if (buf == null) return getRuntime().getNil();
                
                return newStr(getRuntime(), buf);
            }

            return RubyString.newEmptyString(getRuntime());
        } catch (IOException ioe) {
            String m = ioe.getMessage();
            if (m.startsWith("Unexpected end of ZLIB input stream")) {
                throw RubyZlib.newGzipFileError(getRuntime(), ioe.getMessage());
            } else if (m.startsWith("footer is not found")) {
                throw RubyZlib.newNoFooter(getRuntime(), "footer is not found");
            } else if (m.startsWith("incorrect data check")) {
                throw RubyZlib.newCRCError(getRuntime(), "invalid compressed data -- crc error");
            } else if (m.startsWith("incorrect length check")) {
                throw RubyZlib.newLengthError(getRuntime(), "invalid compressed data -- length error");
            } else {
                throw RubyZlib.newDataError(getRuntime(), ioe.getMessage());
            }
        }
    }

    @JRubyMethod(name = "readpartial", required = 1, optional = 1)
    public IRubyObject readpartial(IRubyObject[] args) {
        try {
            int len = RubyNumeric.fix2int(args[0]);
            if (len < 0) {
                throw getRuntime().newArgumentError("negative length " + len + " given");
            }
            if (args.length > 1) {
                if (!(args[1] instanceof RubyString)) {
                    throw getRuntime().newTypeError(
                            "wrong argument type " + args[1].getMetaClass().getName()
                            + " (expected String)");
                }
                return readPartial(len, (RubyString) args[1]);
            }
            return readPartial(len, null);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    private IRubyObject readPartial(int len, RubyString outbuf) throws IOException {
        ByteList val = newReadByteList(10);
        byte[] buffer = new byte[len];
        int read = bufferedStream.read(buffer, 0, len);
        if (read == -1) {
            return getRuntime().getNil();
        }
        val.append(buffer, 0, read);
        this.position += val.length();
        if (outbuf != null) {
            outbuf.view(val);
        }
        return newStr(getRuntime(), val);
    }

    private IRubyObject readAll() throws IOException {
        return readAll(-1);
    }

    private IRubyObject readAll(int limit) throws IOException {
        ByteList val = newReadByteList(10);
        int rest = limit == -1 ? BUFF_SIZE : limit;
        byte[] buffer = new byte[rest];
        
        while (rest > 0) {
            int read = bufferedStream.read(buffer, 0, rest);
            if (read == -1) break;

            val.append(buffer, 0, read);
            if (limit != -1) rest -= read;
        }
        
        this.position += val.length();
        return newStr(getRuntime(), val);
    }


    // FIXME: I think offset == 0 should return empty bytelist and not null
    // mri: gzfile_read
    // This returns a bucket of bytes trying to read length bytes.
    private ByteList readSize(int length) throws IOException {
        byte[] buffer = new byte[length];
        int toRead = length;
        int offset = 0;
        while (toRead > 0) {
            int read = bufferedStream.read(buffer, offset, toRead);
            
            if (read == -1) {
                if (offset == 0) return null; // we're at EOF right away
                break;
            }
            
            toRead -= read;
            offset += read;
        } // hmm...
        this.position += buffer.length;

        // Like MRI we do not set encoding here.  All callers are responsible
        // for that.  We are still just working on blobs of bytes here.
        return new ByteList(buffer, 0, length - toRead, false);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public IRubyObject set_lineno(IRubyObject lineArg) {
        line = RubyNumeric.fix2int(lineArg);
        return lineArg;
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos() {
        return RubyNumeric.int2fix(getRuntime(), position);
    }

    @JRubyMethod(name = "readchar")
    public IRubyObject readchar() {
        try {
            int value = bufferedStream.read();
            if (value == -1) throw getRuntime().newEOFError();

            position++;
            
            return getRuntime().newFixnum(value);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod(name = {"getc", "getbyte"}, compat = RUBY1_8)
    public IRubyObject getc() {
        try {
            int value = bufferedStream.read();
            if (value == -1) return getRuntime().getNil();

            position++;
            
            return getRuntime().newFixnum(value);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod(name = "getbyte", compat = RUBY1_9)
    public IRubyObject getbyte() {
        return getc();
    }

    @JRubyMethod(name = "getc", compat = RUBY1_9)
    public IRubyObject getc_19() {
        try {
            int value = bufferedStream.read();
            if (value == -1) return getRuntime().getNil();

            position++;
            // TODO: must handle encoding. Move encoding handling methods to util class from RubyIO and use it.
            // TODO: StringIO needs a love, too.
            return getRuntime().newString("" + (char) (value & 0xFF));
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    private boolean isEof() throws IOException {
        if (bufferedStream.available() == 0) return true;

        // Java's GZIPInputStream behavior is such
        // that it says that more bytes available even
        // when we are right before the EOF, but not yet
        // encountered the actual EOF during the reading.
        // So, we compensate for that to provide MRI
        // compatible behavior.
        bufferedStream.mark(16);
        bufferedStream.read();
        bufferedStream.reset();

        return bufferedStream.available() == 0;
    }

    @Override
    @JRubyMethod(name = "close")
    public IRubyObject close() {
        if (!closed) {
            try {
                /**
                 * We call internal IO#close directly, not via
                 * IOInputStream#close. IOInputStream#close directly invoke
                 * IO.getOutputStream().close() for IO object instead of just
                 * calling IO#cloase of Ruby. It causes EBADF at
                 * OpenFile#finalize.
                 *
                 * CAUTION: bufferedStream.close() will not cause
                 * 'IO.getOutputStream().close()', becase 'false' has been given
                 * as third augument in constructing GZIPInputStream.
                 *
                 * TODO: implement this without IOInputStream? Not so hard.
                 */
                bufferedStream.close();
                if (realIo.respondsTo("close")) {
                    realIo.callMethod(realIo.getRuntime().getCurrentContext(), "close");
                }
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }
        this.closed = true;
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "eof")
    public IRubyObject eof() {
        try {
            return isEof() ? getRuntime().getTrue() : getRuntime().getFalse();
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod(name = "eof?")
    public IRubyObject eof_p() {
        return eof();
    }

    @JRubyMethod
    public IRubyObject unused() {
        byte[] tmp = io.getAvailIn();
        
        if (tmp == null) return getRuntime().getNil();

        return RubyString.newString(getRuntime(), tmp);
    }

    @Override
    @JRubyMethod
    public IRubyObject crc() {
        long crc = 0;
        try {
            crc = io.getCRC();
        } catch (com.jcraft.jzlib.GZIPException e) {
        }
        return getRuntime().newFixnum(crc);
    }

    @Override
    @JRubyMethod
    public IRubyObject os_code() {
        int os = io.getOS();
        
        if (os == 255) os = (byte) 0x0b; // NTFS filesystem (NT), because CRuby's test_zlib expect it.
        
        return getRuntime().newFixnum(os & 0xff);
    }

    @Override
    @JRubyMethod
    public IRubyObject orig_name() {
        String name = io.getName();
        nullFreeOrigName = getRuntime().newString(name);
        return super.orig_name();
    }

    @Override
    @JRubyMethod
    public IRubyObject comment() {
        String comment = io.getComment();
        nullFreeComment = getRuntime().newString(comment);
        return super.comment();
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        ByteList sep = ((RubyString) getRuntime().getGlobalVariables().get("$/")).getByteList();
        if (args.length > 0 && !args[0].isNil()) {
            sep = args[0].convertToString().getByteList();
        }
        try {
            for (IRubyObject result = internalSepGets(sep); !result.isNil(); result = internalSepGets(sep)) {
                block.yield(context, result);
            }
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        return each(context, args, block);
    }

    @JRubyMethod
    public IRubyObject ungetc(IRubyObject arg) {
        return getRuntime().getNil();
    }

    @JRubyMethod(optional = 1)
    public IRubyObject readlines(IRubyObject[] args) {
        List<IRubyObject> array = new ArrayList<IRubyObject>();
        if (args.length != 0 && args[0].isNil()) {
            array.add(read(new IRubyObject[0]));
        } else {
            ByteList sep = ((RubyString) getRuntime().getGlobalVariables().get("$/")).getByteList();
            if (args.length > 0) sep = args[0].convertToString().getByteList();

            try {
                for (IRubyObject result = internalSepGets(sep); !result.isNil(); result = internalSepGets(sep)) {
                    array.add(result);
                }
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }
        return getRuntime().newArray(array);
    }

    @JRubyMethod
    public IRubyObject each_byte(ThreadContext context, Block block) {
        try {
            int value = bufferedStream.read();
            while (value != -1) {
                position++;
                block.yield(context, getRuntime().newFixnum(value));
                value = bufferedStream.read();
            }
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
        return getRuntime().getNil();
    }
}

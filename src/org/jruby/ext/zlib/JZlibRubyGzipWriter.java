/*
 */
package org.jruby.ext.zlib;

import java.io.IOException;
import org.joda.time.DateTime;
import static org.jruby.CompatVersion.RUBY1_8;
import static org.jruby.CompatVersion.RUBY1_9;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyKernel;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.stringio.RubyStringIO;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.ByteList;
import org.jruby.util.CharsetTranscoder;
import org.jruby.util.IOOutputStream;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingOption;

/**
 *
 */
@JRubyClass(name = "Zlib::GzipWriter", parent = "Zlib::GzipFile")
public class JZlibRubyGzipWriter extends RubyGzipFile {
    protected static final ObjectAllocator GZIPWRITER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new JZlibRubyGzipWriter(runtime, klass);
        }
    };

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static JZlibRubyGzipWriter newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass) recv;
        JZlibRubyGzipWriter result = (JZlibRubyGzipWriter) klass.allocate();
        result.callInit(args, block);
        
        return result;
    }

    @JRubyMethod(name = "open", required = 1, optional = 2, meta = true, compat = RUBY1_8)
    public static IRubyObject open18(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("wb"));
        JZlibRubyGzipWriter gzio = newInstance(recv, argsWithIo(io, args), block);
        
        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    @JRubyMethod(name = "open", required = 1, optional = 3, meta = true, compat = RUBY1_9)
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("wb"));
        JZlibRubyGzipWriter gzio = newInstance(recv, argsWithIo(io, args), block);
        
        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    public JZlibRubyGzipWriter(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    private com.jcraft.jzlib.GZIPOutputStream io;

    @JRubyMethod(name = "initialize", required = 1, rest = true, visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(IRubyObject[] args) {
        // args: recv, path, opts = {}
        if (args.length > 2) {
            checkLevel(getRuntime(), RubyNumeric.fix2int(args[2]));
        }
        return initializeCommon(args[0]);
    }

    private IRubyObject initializeCommon(IRubyObject stream) {
        realIo = (RubyObject) stream;
        try {
            io = new com.jcraft.jzlib.GZIPOutputStream(new IOOutputStream(realIo, false, false), 512, false);
            return this;
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(IRubyObject[] args, Block unused) {
        // args: recv, path, level = nil, strategy = nil, opts = {}
        IRubyObject obj = initializeCommon(args[0]);
        if (args.length > 2) {
            IRubyObject opt = TypeConverter.checkHashType(getRuntime(), args[args.length - 1]);
            if (!opt.isNil()) {
                EncodingOption enc = EncodingOption.getEncodingOptionFromObject(opt);
                if (enc != null) {
                    readEncoding = enc.getExternalEncoding();
                    writeEncoding = enc.getInternalEncoding();
                }
                IRubyObject[] newArgs = new IRubyObject[args.length - 1];
                System.arraycopy(args, 0, newArgs, 0, args.length - 1);
                args = newArgs;
            }
        }
        if (args.length > 2) {
            checkLevel(getRuntime(), RubyNumeric.fix2int(args[2]));
        }
        if (realIo.respondsTo("path")) {
            obj.getSingletonClass().defineMethod("path", new Callback() {

                public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                    return ((JZlibRubyGzipWriter) recv).realIo.callMethod(recv.getRuntime().getCurrentContext(), "path");
                }

                public Arity getArity() {
                    return Arity.NO_ARGUMENTS;
                }
            });
        }
        return obj;
    }

    private static void checkLevel(Ruby runtime, int level) {
        if (level < 0 || level > 9) {
            throw RubyZlib.newStreamError(runtime, "stream error: invalid level");
        }
    }

    @Override
    @JRubyMethod(name = "close")
    public IRubyObject close() {
        if (!closed) {
            try {
                io.close();
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

    @JRubyMethod(name = {"append", "<<"}, required = 1)
    public IRubyObject append(IRubyObject p1) {
        this.write(p1);
        return this;
    }

    @JRubyMethod(name = "printf", required = 1, rest = true)
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        write(RubyKernel.sprintf(context, this, args));
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "print", rest = true)
    public IRubyObject print(IRubyObject[] args) {
        if (args.length != 0) {
            for (int i = 0, j = args.length; i < j; i++) {
                write(args[i]);
            }
        }

        IRubyObject sep = getRuntime().getGlobalVariables().get("$\\");
        if (!sep.isNil()) write(sep);

        return getRuntime().getNil();
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos() {
        return RubyNumeric.int2fix(getRuntime(), io.getTotalIn());
    }

    @JRubyMethod(name = "orig_name=", required = 1)
    public IRubyObject set_orig_name(IRubyObject obj) {
        nullFreeOrigName = obj.convertToString();
        ensureNonNull(nullFreeOrigName);
        try {
            io.setName(nullFreeOrigName.toString());
        } catch (com.jcraft.jzlib.GZIPException e) {
            throw RubyZlib.newGzipFileError(getRuntime(), "header is already written");
        }
        return obj;
    }

    @JRubyMethod(name = "comment=", required = 1)
    public IRubyObject set_comment(IRubyObject obj) {
        nullFreeComment = obj.convertToString();
        ensureNonNull(nullFreeComment);
        try {
            io.setComment(nullFreeComment.toString());
        } catch (com.jcraft.jzlib.GZIPException e) {
            throw RubyZlib.newGzipFileError(getRuntime(), "header is already written");
        }
        return obj;
    }

    private void ensureNonNull(RubyString obj) {
        String str = obj.toString();
        if (str.indexOf('\0') >= 0) {
            String trim = str.substring(0, str.toString().indexOf('\0'));
            obj.setValue(new ByteList(trim.getBytes()));
        }
    }

    @JRubyMethod(name = "putc", required = 1)
    public IRubyObject putc(IRubyObject p1) {
        try {
            io.write(RubyNumeric.num2chr(p1));
            return p1;
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod(name = "puts", rest = true)
    public IRubyObject puts(ThreadContext context, IRubyObject[] args) {
        RubyStringIO sio = (RubyStringIO) getRuntime().getClass("StringIO").newInstance(context, new IRubyObject[0], Block.NULL_BLOCK);
        sio.puts(context, args);
        write(sio.string());

        return getRuntime().getNil();
    }

    @Override
    public IRubyObject finish() {
        if (!finished) {
            try {
                io.finish();
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }
        finished = true;
        return realIo;
    }

    @JRubyMethod(name = "flush", optional = 1)
    public IRubyObject flush(IRubyObject[] args) {
        int flush = com.jcraft.jzlib.JZlib.Z_SYNC_FLUSH;
        if (args.length > 0 && !args[0].isNil()) {
            flush = RubyNumeric.fix2int(args[0]);
        }
        boolean tmp = io.getSyncFlush();
        try {
            if (flush != 0 /*
                     * NO_FLUSH
                     */) {
                io.setSyncFlush(true);
            }
            io.flush();
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        } finally {
            io.setSyncFlush(tmp);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "mtime=", required = 1)
    public IRubyObject set_mtime(IRubyObject arg) {
        if (arg instanceof RubyTime) {
            this.mtime = ((RubyTime) arg);
        } else if (arg.isNil()) {
            // ...nothing
        } else {
            this.mtime.setDateTime(new DateTime(RubyNumeric.fix2long(arg) * 1000));
        }
        try {
            io.setModifiedTime(this.mtime.to_i().getLongValue());
        } catch (com.jcraft.jzlib.GZIPException e) {
            throw RubyZlib.newGzipFileError(getRuntime(), "header is already written");
        }
        return getRuntime().getNil();
    }

    @Override
    @JRubyMethod(name = "crc")
    public IRubyObject crc() {
        long crc = 0L;
        try {
            crc = io.getCRC();
        } catch (com.jcraft.jzlib.GZIPException e) {
            // not calculated yet
        }
        return getRuntime().newFixnum(crc);
    }

    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(IRubyObject p1) {
        ByteList bytes = p1.asString().getByteList();
        Ruby runtime = getRuntime();
        if (runtime.is1_9()) {
            if (writeEncoding != null
                    && writeEncoding != runtime.getEncodingService().getAscii8bitEncoding()) {
                bytes = CharsetTranscoder.transcode(runtime.getCurrentContext(), bytes, null,
                        writeEncoding, runtime.getNil());
            }
        }
        try {
            // TODO: jzlib-1.1.0.jar throws IndexOutOfBoundException for zero length buffer.
            if (bytes.length() > 0) {
                io.write(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
            }
            return getRuntime().newFixnum(bytes.length());
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    @Override
    @JRubyMethod
    public IRubyObject set_sync(IRubyObject arg) {
        IRubyObject s = super.set_sync(arg);
        io.setSyncFlush(sync);
        return s;
    }
}

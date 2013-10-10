/*
 */
package org.jruby.ext.zlib;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.JZlib;
import java.io.IOException;
import org.jcodings.specific.ASCIIEncoding;
import org.joda.time.DateTime;
import static org.jruby.CompatVersion.RUBY1_8;
import static org.jruby.CompatVersion.RUBY1_9;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.stringio.RubyStringIO;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.encoding.Transcoder;
import org.jruby.util.IOOutputStream;
import org.jruby.util.TypeConverter;

/**
 *
 */
@JRubyClass(name = "Zlib::GzipWriter", parent = "Zlib::GzipFile")
public class JZlibRubyGzipWriter extends RubyGzipFile {
    protected static final ObjectAllocator GZIPWRITER_ALLOCATOR = new ObjectAllocator() {
        @Override
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
        args[0] = Helpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("wb"));
        JZlibRubyGzipWriter gzio = newInstance(recv, args, block);
        
        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    @JRubyMethod(name = "open", required = 1, optional = 3, meta = true, compat = RUBY1_9)
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        args[0] = Helpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("wb"));
        JZlibRubyGzipWriter gzio = newInstance(recv, args, block);
        
        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    public JZlibRubyGzipWriter(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    private com.jcraft.jzlib.GZIPOutputStream io;

    @JRubyMethod(name = "initialize", required = 1, rest = true, visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(IRubyObject[] args) {
        Ruby runtime = getRuntime();
        
        level = args.length < 2 ? JZlib.Z_DEFAULT_COMPRESSION : RubyZlib.FIXNUMARG(args[1], JZlib.Z_DEFAULT_COMPRESSION);
        checkLevel(runtime, level);
        // unused; could not figure out how to get JZlib to take these right
        int strategy = args.length < 3 ? JZlib.Z_DEFAULT_STRATEGY : RubyZlib.FIXNUMARG(args[2], JZlib.Z_DEFAULT_STRATEGY);
        return initializeCommon(args[0], level);
    }

    private IRubyObject initializeCommon(IRubyObject stream, int level) {
        realIo = (RubyObject) stream;
        try {
            // the 15+16 here is copied from a Deflater default constructor
            Deflater deflater = new Deflater(level, 15+16, false);
            io = new com.jcraft.jzlib.GZIPOutputStream(new IOOutputStream(realIo, false, false), deflater, 512, false);
            return this;
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject[] args, Block unused) {
        Ruby runtime = context.getRuntime();
        IRubyObject opt = context.nil;
        
        int argc = args.length;
        if (argc > 1) {
            opt = TypeConverter.checkHashType(runtime, opt);
            if (!opt.isNil()) argc--;
        }
        
        level = argc < 2 ? JZlib.Z_DEFAULT_COMPRESSION : RubyZlib.FIXNUMARG(args[1], JZlib.Z_DEFAULT_COMPRESSION);
        checkLevel(runtime, level);
        // unused; could not figure out how to get JZlib to take these right
        int strategy = argc < 3 ? JZlib.Z_DEFAULT_STRATEGY : RubyZlib.FIXNUMARG(args[2], JZlib.Z_DEFAULT_STRATEGY);
        
        IRubyObject obj = initializeCommon(args[0], level);
        
        ecopts(context, opt);
        
        // FIXME: don't singletonize!
        if (realIo.respondsTo("path")) {
            getSingletonClass().addMethod("path", new JavaMethod.JavaMethodZero(this.getSingletonClass(), Visibility.PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                    return ((JZlibRubyGzipWriter) self).realIo.callMethod(context, "path");
                }
            });
        }
        return this;
    }
    
    private static void checkLevel(Ruby runtime, int level) {
        if (level != JZlib.Z_DEFAULT_COMPRESSION && (level < JZlib.Z_NO_COMPRESSION || level > JZlib.Z_BEST_COMPRESSION)) {
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
        if (enc2 != null
                && enc2 != ASCIIEncoding.INSTANCE) {
            bytes = Transcoder.strConvEncOpts(runtime.getCurrentContext(), bytes, bytes.getEncoding(),
                    enc2, 0, runtime.getNil());
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
    
    private void blah() {
        
    }

    @Override
    @JRubyMethod
    public IRubyObject set_sync(IRubyObject arg) {
        IRubyObject s = super.set_sync(arg);
        io.setSyncFlush(sync);
        return s;
    }
}

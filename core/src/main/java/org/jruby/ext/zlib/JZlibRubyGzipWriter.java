/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.zlib;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.GZIPOutputStream;
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
import org.jruby.ext.stringio.StringIO;
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

    @JRubyMethod(name = "initialize", required = 1, rest = true, visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(IRubyObject[] args) {
        Ruby runtime = getRuntime();
        
        level = processLevel(args.length, args, runtime);
        
        // unused; could not figure out how to get JZlib to take this right
        /*int strategy = */processStrategy(args.length, args);
        
        return initializeCommon(args[0], level);
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
        
        level = processLevel(argc, args, runtime);
        
        // unused; could not figure out how to get JZlib to take this right
        /*int strategy = */processStrategy(argc, args);
        
        initializeCommon(args[0], level);
        
        ecopts(context, opt);
        
        return this;
    }

    // These methods are here to avoid defining a singleton #path on every instance, as in MRI

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject path(ThreadContext context) {
        return this.realIo.callMethod(context, "path");
    }
    
    @JRubyMethod(name = "respond_to?", frame = true, compat = RUBY1_9)
    public IRubyObject respond_to(ThreadContext context, IRubyObject name) {
        if (name.asJavaString().equals("path")) {
            return this.realIo.callMethod(context, "respond_to?", name);
        }
        
        return Helpers.invokeSuper(context, this, name, Block.NULL_BLOCK);
    }

    private int processStrategy(int argc, IRubyObject[] args) {
        return argc < 3 ? JZlib.Z_DEFAULT_STRATEGY : RubyZlib.FIXNUMARG(args[2], JZlib.Z_DEFAULT_STRATEGY);
    }

    private int processLevel(int argc, IRubyObject[] args, Ruby runtime) {
        int level = argc < 2 ? JZlib.Z_DEFAULT_COMPRESSION : RubyZlib.FIXNUMARG(args[1], JZlib.Z_DEFAULT_COMPRESSION);

        checkLevel(runtime, level);

        return level;
    }

    private IRubyObject initializeCommon(IRubyObject stream, int level) {
        realIo = (RubyObject) stream;
        try {
            // the 15+16 here is copied from a Deflater default constructor
            Deflater deflater = new Deflater(level, 15+16, false);
            io = new GZIPOutputStream(new IOOutputStream(realIo, false, false), deflater, 512, false);
            return this;
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
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
        } catch (GZIPException e) {
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
        } catch (GZIPException e) {
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
        IRubyObject _sio = getRuntime().getClass("StringIO").newInstance(context, new IRubyObject[0], Block.NULL_BLOCK);

        if (_sio instanceof RubyStringIO) {
            RubyStringIO sio = (RubyStringIO)_sio;
            sio.puts(context, args);
            write(sio.string());
        } else {
            StringIO sio = (StringIO)_sio;
            sio.puts(context, args);
            write(sio.string(context));
        }

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
        int flush = JZlib.Z_SYNC_FLUSH;
        
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
        } catch (GZIPException e) {
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
        } catch (GZIPException e) {
            // not calculated yet
        }
        
        return getRuntime().newFixnum(crc);
    }

    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(IRubyObject p1) {
        Ruby runtime = getRuntime();
        ByteList bytes = p1.asString().getByteList();
        
        if (runtime.is1_9()) {
            if (enc2 != null
                    && enc2 != ASCIIEncoding.INSTANCE) {
                bytes = Transcoder.strConvEncOpts(runtime.getCurrentContext(), bytes, bytes.getEncoding(),
                        enc2, 0, runtime.getNil());
            }
        }
        
        try {
            // TODO: jzlib-1.1.0.jar throws IndexOutOfBoundException for zero length buffer.
            if (bytes.length() > 0) {
                io.write(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
            }
            
            return runtime.newFixnum(bytes.length());
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    @Override
    @JRubyMethod
    public IRubyObject set_sync(IRubyObject arg) {
        IRubyObject s = super.set_sync(arg);
        
        io.setSyncFlush(sync);
        
        return s;
    }
    
    private GZIPOutputStream io;
}

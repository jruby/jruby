/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyKernel;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.IOOutputStream;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;

import java.io.IOException;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;
import static org.jruby.runtime.Visibility.PRIVATE;

/**
 *
 */
@JRubyClass(name = "Zlib::GzipWriter", parent = "Zlib::GzipFile")
public class JZlibRubyGzipWriter extends RubyGzipFile {
    @JRubyMethod(name = "new", rest = true, meta = true, keywords = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        JZlibRubyGzipWriter result = newInstance(recv, args);

        return RubyGzipFile.wrapBlock(context, result, block);
    }

    public static JZlibRubyGzipWriter newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyClass klass = (RubyClass) recv;
        JZlibRubyGzipWriter result = (JZlibRubyGzipWriter) klass.allocate();

        result.callInit(args, Block.NULL_BLOCK);

        return result;
    }

    @JRubyMethod(name = "open", required = 1, optional = 3, checkArity = false, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 1, 4);
        args[0] = Helpers.invoke(context, context.runtime.getFile(), "open", args[0], newString(context, "wb"));
        
        JZlibRubyGzipWriter gzio = newInstance(recv, args);
        
        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    public JZlibRubyGzipWriter(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public IRubyObject initialize(IRubyObject[] args) {
        return initialize(getRuntime().getCurrentContext(), args, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
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
        
        initializeCommon(context, args[0], level);
        
        ecopts(context, opt);
        
        return this;
    }

    private int processStrategy(int argc, IRubyObject[] args) {
        return argc < 3 ? JZlib.Z_DEFAULT_STRATEGY : RubyZlib.FIXNUMARG(args[2], JZlib.Z_DEFAULT_STRATEGY);
    }

    private int processLevel(int argc, IRubyObject[] args, Ruby runtime) {
        int level = argc < 2 ? JZlib.Z_DEFAULT_COMPRESSION : RubyZlib.FIXNUMARG(args[1], JZlib.Z_DEFAULT_COMPRESSION);

        checkLevel(runtime, level);

        return level;
    }

    private IRubyObject initializeCommon(ThreadContext context, IRubyObject stream, int level) {
        Ruby runtime = context.runtime;
        realIo = stream;
        try {
            // the 15+16 here is copied from a Deflater default constructor
            Deflater deflater = new Deflater(level, 15+16, false);
            final IOOutputStream ioOutputStream = new IOOutputStream(realIo, false, false) {
                /**
                 * Customize IOOutputStream#write(byte[], int, int) to create a defensive copy of the byte array
                 * that GZIPOutputStream hands us.
                 *
                 * That byte array is a reference to one of GZIPOutputStream's internal byte buffers.
                 * The base IOOutputStream#write(byte[], int, int) uses the bytes it is handed to back a
                 * copy-on-write ByteList.  So, without this defensive copy, those two classes overwrite each
                 * other's bytes, corrupting our output.
                 */
                @Override
                public void write(byte[] bytes, int off, int len) throws IOException {
                    byte[] bytesCopy = new byte[len];
                    System.arraycopy(bytes, off, bytesCopy, 0, len);
                    super.write(bytesCopy, 0, len);
                }
            };

            io = new GZIPOutputStream(ioOutputStream, deflater, 512, false);

            // set mtime to current time in case it is never updated
            long now = System.currentTimeMillis();
            this.mtime = RubyTime.newTime(runtime, now);
            io.setModifiedTime(now / 1000);

            return this;
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
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
        
        return realIo;
    }

    @JRubyMethod(name = {"append", "<<"})
    public IRubyObject append(IRubyObject p1) {
        this.write(p1);
        
        return this;
    }

    @JRubyMethod(name = "printf", required = 1, rest = true, checkArity = false)
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

    @JRubyMethod(name = "orig_name=")
    public IRubyObject set_orig_name(IRubyObject obj) {
        nullFreeOrigName = obj.convertToString().strDup(getRuntime());
        ensureNonNull(nullFreeOrigName);
        
        try {
            io.setName(nullFreeOrigName.toString());
        } catch (GZIPException e) {
            throw RubyZlib.newGzipFileError(getRuntime(), "header is already written");
        }
        
        return obj;
    }

    @JRubyMethod(name = "comment=")
    public IRubyObject set_comment(IRubyObject obj) {
        nullFreeComment = obj.convertToString().strDup(getRuntime());
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
            String trim = str.substring(0, str.indexOf('\0'));
            obj.setValue(new ByteList(trim.getBytes()));
        }
    }

    @JRubyMethod(name = "putc")
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
        return RubyIO.puts(context, this, args);
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

    @Deprecated
    public IRubyObject flush(IRubyObject[] args) {
        return flush(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(name = "flush", optional = 1, checkArity = false)
    public IRubyObject flush(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        int flush = JZlib.Z_SYNC_FLUSH;
        
        if (argc > 0 && !args[0].isNil()) {
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
            throw context.runtime.newIOErrorFromException(ioe);
        } finally {
            io.setSyncFlush(tmp);
        }
        
        return context.nil;
    }

    @JRubyMethod(name = "mtime=")
    public IRubyObject set_mtime(IRubyObject arg) {
        Ruby runtime = getRuntime();

        if (arg instanceof RubyTime) {
            this.mtime = ((RubyTime) arg);
        } else if (arg.isNil()) {
            // ...nothing
        } else {
            this.mtime = RubyTime.newTime(runtime, RubyNumeric.fix2long(arg) * 1000);
        }
        try {
            io.setModifiedTime(this.mtime.to_i(runtime.getCurrentContext()).getLongValue());
        } catch (GZIPException e) {
            throw RubyZlib.newGzipFileError(runtime, "header is already written");
        }
        
        return runtime.getNil();
    }

    @Override
    @JRubyMethod(name = "crc")
    public IRubyObject crc(ThreadContext context) {
        long crc = 0L;
        
        try {
            crc = io.getCRC();
        } catch (GZIPException e) {
            // not calculated yet
        }
        
        return getRuntime().newFixnum(crc);
    }

    @Deprecated
    public IRubyObject write(IRubyObject p1) {
        return write(getCurrentContext(), p1);
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject p1) {
        RubyString str = p1.asString();

        if (enc2 != null && enc2 != ASCIIEncoding.INSTANCE) {
            str = EncodingUtils.strConvEncOpts(context, str, str.getEncoding(), enc2, 0, context.nil);
        }
        
        try {
            // TODO: jzlib-1.1.0.jar throws IndexOutOfBoundException for zero length buffer.
            if (!str.isEmpty()) {
                io.write(str.getByteList().getUnsafeBytes(), str.getByteList().begin(), str.getByteList().length());
            }
            
            return asFixnum(context, str.getByteList().length());
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
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

/*
 */
package org.jruby.ext.zlib;

import java.util.ArrayList;
import java.util.List;
import org.jcodings.Encoding;
import org.joda.time.DateTime;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CharsetTranscoder;

/**
 *
 */
@JRubyClass(name = "Zlib::GzipFile")
public class RubyGzipFile extends RubyObject {
    @JRubyClass(name = "Zlib::GzipFile::Error", parent = "Zlib::Error")
    public static class Error {}

    @JRubyClass(name = "Zlib::GzipFile::CRCError", parent = "Zlib::GzipFile::Error")
    public static class CRCError extends Error {}

    @JRubyClass(name = "Zlib::GzipFile::NoFooter", parent = "Zlib::GzipFile::Error")
    public static class NoFooter extends Error {}

    @JRubyClass(name = "Zlib::GzipFile::LengthError", parent = "Zlib::GzipFile::Error")
    public static class LengthError extends Error {}

    static IRubyObject wrapBlock(ThreadContext context, RubyGzipFile instance, Block block) {
        if (block.isGiven()) {
            try {
                return block.yield(context, instance);
            } finally {
                if (!instance.isClosed()) {
                    instance.close();
                }
            }
        }
        return instance;
    }

    static IRubyObject[] argsWithIo(IRubyObject io, IRubyObject[] args) {
        List<IRubyObject> newArgs = new ArrayList<IRubyObject>();
        newArgs.add(io);
        for (IRubyObject arg : args) {
            if (arg == null) {
                break;
            }
            newArgs.add(arg);
        }
        return newArgs.toArray(new IRubyObject[0]);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject wrap(ThreadContext context, IRubyObject recv, IRubyObject io, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyGzipFile instance;

        // TODO: People extending GzipWriter/reader will break.  Find better way here.
        if (recv == runtime.getModule("Zlib").getClass("GzipWriter")) {
            instance = JZlibRubyGzipWriter.newInstance(recv, new IRubyObject[]{io}, block);
        } else {
            instance = JZlibRubyGzipReader.newInstance(recv, new IRubyObject[]{io}, block);
        }

        return wrapBlock(context, instance, block);
    }
    protected static final ObjectAllocator GZIPFILE_ALLOCATOR = new ObjectAllocator() {

        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyGzipFile(runtime, klass);
        }
    };

    @JRubyMethod(name = "new", meta = true)
    public static RubyGzipFile newInstance(IRubyObject recv, Block block) {
        RubyClass klass = (RubyClass) recv;

        RubyGzipFile result = (RubyGzipFile) klass.allocate();

        result.callInit(new IRubyObject[0], block);

        return result;
    }
    protected boolean closed = false;
    protected boolean finished = false;
    protected byte osCode = Zlib.OS_UNKNOWN;
    protected int level = -1;
    protected RubyString nullFreeOrigName;
    protected RubyString nullFreeComment;
    protected IRubyObject realIo;
    protected RubyTime mtime;
    protected Encoding readEncoding;    // enc
    protected Encoding writeEncoding;   // enc2
    protected boolean sync = false;

    public RubyGzipFile(Ruby runtime, RubyClass type) {
        super(runtime, type);
        mtime = RubyTime.newTime(runtime, new DateTime());
        readEncoding = null;
        writeEncoding = null;
    }

    // c: gzfile_newstr
    protected RubyString newStr(Ruby runtime, ByteList value) {
        if (runtime.is1_9()) {
            if (writeEncoding == null) {
                // FIXME: MRI does initialize readEncoding to def external, but we are missing something in some
                // initialization cases where that would make this go bad.  Bandage until then.
                Encoding encoding = readEncoding == null ? runtime.getEncodingService().getAscii8bitEncoding() : readEncoding;

                return RubyString.newString(runtime, value, encoding);
            }

            return RubyString.newStringNoCopy(runtime, CharsetTranscoder.transcode(
                    runtime.getCurrentContext(), value, readEncoding, writeEncoding,
                    runtime.getNil()));
        } 

        return RubyString.newString(runtime, value);
    }

    @JRubyMethod(name = "os_code")
    public IRubyObject os_code() {
        return getRuntime().newFixnum(osCode & 0xff);
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        return closed ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    protected boolean isClosed() {
        return closed;
    }

    @JRubyMethod(name = "orig_name")
    public IRubyObject orig_name() {
        if (closed) {
            throw RubyZlib.newGzipFileError(getRuntime(), "closed gzip stream");
        }
        return nullFreeOrigName == null ? getRuntime().getNil() : nullFreeOrigName;
    }

    @JRubyMethod(name = "to_io")
    public IRubyObject to_io() {
        return realIo;
    }

    @JRubyMethod(name = "comment")
    public IRubyObject comment() {
        if (closed) {
            throw RubyZlib.newGzipFileError(getRuntime(), "closed gzip stream");
        }
        return nullFreeComment == null ? getRuntime().getNil() : nullFreeComment;
    }

    @JRubyMethod(name = "crc")
    public IRubyObject crc() {
        return getRuntime().newFixnum(0);
    }

    @JRubyMethod(name = "mtime")
    public IRubyObject mtime() {
        return mtime;
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync() {
        return sync ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "finish")
    public IRubyObject finish() {
        if (!finished) {
            //io.finish();
        }
        finished = true;
        return realIo;
    }

    @JRubyMethod(name = "close")
    public IRubyObject close() {
        return null;
    }

    @JRubyMethod(name = "level")
    public IRubyObject level() {
        return getRuntime().newFixnum(level);
    }

    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject set_sync(IRubyObject arg) {
        sync = ((RubyBoolean) arg).isTrue();
        return sync ? getRuntime().getTrue() : getRuntime().getFalse();
    }
}
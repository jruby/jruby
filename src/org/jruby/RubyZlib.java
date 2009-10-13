/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2009 Aurelian Oancea <aurelian@locknet.ro>
 * Copyright (C) 2009 Vladimir Sizikov <vsizikov@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import org.joda.time.DateTime;

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.Adler32Ext;
import org.jruby.util.ByteList;
import org.jruby.util.CRC32Ext;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.ZlibDeflate;

@JRubyModule(name="Zlib")
public class RubyZlib {
    private final static int MAX_WBITS = 15;

    /** Create the Zlib module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createZlibModule(Ruby runtime) {
        RubyModule result = runtime.defineModule("Zlib");

        RubyClass gzfile = result.defineClassUnder("GzipFile", runtime.getObject(), RubyGzipFile.GZIPFILE_ALLOCATOR);
        gzfile.defineAnnotatedMethods(RubyGzipFile.class);
        
        RubyClass gzreader = result.defineClassUnder("GzipReader", gzfile, RubyGzipReader.GZIPREADER_ALLOCATOR);
        gzreader.includeModule(runtime.getEnumerable());
        gzreader.defineAnnotatedMethods(RubyGzipReader.class);
        
        RubyClass standardError = runtime.getStandardError();
        RubyClass zlibError = result.defineClassUnder("Error", standardError, standardError.getAllocator());
        gzreader.defineClassUnder("Error", zlibError, zlibError.getAllocator());

        RubyClass gzwriter = result.defineClassUnder("GzipWriter", gzfile, RubyGzipWriter.GZIPWRITER_ALLOCATOR);
        gzwriter.defineAnnotatedMethods(RubyGzipWriter.class);

        result.defineConstant("ZLIB_VERSION",runtime.newString("1.2.1"));
        result.defineConstant("VERSION",runtime.newString("0.6.0"));

        result.defineConstant("BINARY",runtime.newFixnum(0));
        result.defineConstant("ASCII",runtime.newFixnum(1));
        result.defineConstant("UNKNOWN",runtime.newFixnum(2));

        result.defineConstant("DEF_MEM_LEVEL",runtime.newFixnum(8));
        result.defineConstant("MAX_MEM_LEVEL",runtime.newFixnum(9));

        result.defineConstant("OS_UNIX",runtime.newFixnum(3));
        result.defineConstant("OS_UNKNOWN",runtime.newFixnum(255));
        result.defineConstant("OS_CODE",runtime.newFixnum(11));
        result.defineConstant("OS_ZSYSTEM",runtime.newFixnum(8));
        result.defineConstant("OS_VMCMS",runtime.newFixnum(4));
        result.defineConstant("OS_VMS",runtime.newFixnum(2));
        result.defineConstant("OS_RISCOS",runtime.newFixnum(13));
        result.defineConstant("OS_MACOS",runtime.newFixnum(7));
        result.defineConstant("OS_OS2",runtime.newFixnum(6));
        result.defineConstant("OS_AMIGA",runtime.newFixnum(1));
        result.defineConstant("OS_QDOS",runtime.newFixnum(12));
        result.defineConstant("OS_WIN32",runtime.newFixnum(11));
        result.defineConstant("OS_ATARI",runtime.newFixnum(5));
        result.defineConstant("OS_MSDOS",runtime.newFixnum(0));
        result.defineConstant("OS_CPM",runtime.newFixnum(9));
        result.defineConstant("OS_TOPS20",runtime.newFixnum(10));

        result.defineConstant("DEFAULT_STRATEGY",runtime.newFixnum(0));
        result.defineConstant("FILTERED",runtime.newFixnum(1));
        result.defineConstant("HUFFMAN_ONLY",runtime.newFixnum(2));

        result.defineConstant("NO_FLUSH",runtime.newFixnum(0));
        result.defineConstant("SYNC_FLUSH",runtime.newFixnum(2));
        result.defineConstant("FULL_FLUSH",runtime.newFixnum(3));
        result.defineConstant("FINISH",runtime.newFixnum(4));

        result.defineConstant("NO_COMPRESSION",runtime.newFixnum(0));
        result.defineConstant("BEST_SPEED",runtime.newFixnum(1));
        result.defineConstant("DEFAULT_COMPRESSION",runtime.newFixnum(-1));
        result.defineConstant("BEST_COMPRESSION",runtime.newFixnum(9));

        result.defineConstant("MAX_WBITS",runtime.newFixnum(MAX_WBITS));

        result.defineAnnotatedMethods(RubyZlib.class);

        result.defineClassUnder("StreamEnd",zlibError, zlibError.getAllocator());
        result.defineClassUnder("StreamError",zlibError, zlibError.getAllocator());
        result.defineClassUnder("BufError",zlibError, zlibError.getAllocator());
        result.defineClassUnder("NeedDict",zlibError, zlibError.getAllocator());
        result.defineClassUnder("MemError",zlibError, zlibError.getAllocator());
        result.defineClassUnder("VersionError",zlibError, zlibError.getAllocator());
        result.defineClassUnder("DataError",zlibError, zlibError.getAllocator());

        RubyClass gzError = gzfile.defineClassUnder("Error",zlibError, zlibError.getAllocator());
        gzfile.defineClassUnder("CRCError",gzError, gzError.getAllocator());
        gzfile.defineClassUnder("NoFooter",gzError, gzError.getAllocator());
        gzfile.defineClassUnder("LengthError",gzError, gzError.getAllocator());

        // ZStream actually *isn't* allocatable
        RubyClass zstream = result.defineClassUnder("ZStream", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        zstream.defineAnnotatedMethods(ZStream.class);
        zstream.undefineMethod("new");

        RubyClass infl = result.defineClassUnder("Inflate", zstream, Inflate.INFLATE_ALLOCATOR);
        infl.defineAnnotatedMethods(Inflate.class);

        RubyClass defl = result.defineClassUnder("Deflate", zstream, Deflate.DEFLATE_ALLOCATOR);
        defl.defineAnnotatedMethods(Deflate.class);

        runtime.getKernel().callMethod(runtime.getCurrentContext(),"require",runtime.newString("stringio"));

        return result;
    }

    @JRubyClass(name="Zlib::Error", parent="StandardError")
    public static class Error {}
    @JRubyClass(name="Zlib::StreamEnd", parent="Zlib::Error")
    public static class StreamEnd extends Error {}
    @JRubyClass(name="Zlib::StreamError", parent="Zlib::Error")
    public static class StreamError extends Error {}
    @JRubyClass(name="Zlib::BufError", parent="Zlib::Error")
    public static class BufError extends Error {}
    @JRubyClass(name="Zlib::NeedDict", parent="Zlib::Error")
    public static class NeedDict extends Error {}
    @JRubyClass(name="Zlib::MemError", parent="Zlib::Error")
    public static class MemError extends Error {}
    @JRubyClass(name="Zlib::VersionError", parent="Zlib::Error")
    public static class VersionError extends Error {}
    @JRubyClass(name="Zlib::DataError", parent="Zlib::Error")
    public static class DataError extends Error {}

    @JRubyMethod(name = "zlib_version", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject zlib_version(IRubyObject recv) {
        RubyBasicObject res = (RubyBasicObject) ((RubyModule)recv).fastGetConstant("ZLIB_VERSION");
        // MRI behavior, enforced by tests
        res.taint(recv.getRuntime());
        return res;
    }

    @JRubyMethod(name = "crc32", optional = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject crc32(IRubyObject recv, IRubyObject[] args) throws Exception {
        args = Arity.scanArgs(recv.getRuntime(),args,0,2);
        long crc = 0;
        ByteList bytes = null;
        
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) crc = RubyNumeric.num2long(args[1]);

        CRC32Ext ext = new CRC32Ext((int)crc);
        if (bytes != null) {
            ext.update(bytes.unsafeBytes(), bytes.begin(), bytes.length());
        }
        
        return recv.getRuntime().newFixnum(ext.getValue());
    }

    @JRubyMethod(name = "adler32", optional = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject adler32(IRubyObject recv, IRubyObject[] args) throws Exception {
        args = Arity.scanArgs(recv.getRuntime(),args,0,2);
        int adler = 1;
        ByteList bytes = null;
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) adler = RubyNumeric.fix2int(args[1]);

        Adler32Ext ext = new Adler32Ext(adler);
        if (bytes != null) {
            ext.update(bytes.unsafeBytes(), bytes.begin(), bytes.length()); // it's safe since adler.update doesn't modify the array
        }
        return recv.getRuntime().newFixnum(ext.getValue());
    }

    private final static long[] crctab = new long[]{
        0L, 1996959894L, 3993919788L, 2567524794L, 124634137L, 1886057615L, 3915621685L, 2657392035L, 249268274L, 2044508324L, 3772115230L, 2547177864L, 162941995L, 
        2125561021L, 3887607047L, 2428444049L, 498536548L, 1789927666L, 4089016648L, 2227061214L, 450548861L, 1843258603L, 4107580753L, 2211677639L, 325883990L, 
        1684777152L, 4251122042L, 2321926636L, 335633487L, 1661365465L, 4195302755L, 2366115317L, 997073096L, 1281953886L, 3579855332L, 2724688242L, 1006888145L, 
        1258607687L, 3524101629L, 2768942443L, 901097722L, 1119000684L, 3686517206L, 2898065728L, 853044451L, 1172266101L, 3705015759L, 2882616665L, 651767980L, 
        1373503546L, 3369554304L, 3218104598L, 565507253L, 1454621731L, 3485111705L, 3099436303L, 671266974L, 1594198024L, 3322730930L, 2970347812L, 795835527L, 
        1483230225L, 3244367275L, 3060149565L, 1994146192L, 31158534L, 2563907772L, 4023717930L, 1907459465L, 112637215L, 2680153253L, 3904427059L, 2013776290L, 
        251722036L, 2517215374L, 3775830040L, 2137656763L, 141376813L, 2439277719L, 3865271297L, 1802195444L, 476864866L, 2238001368L, 4066508878L, 1812370925L, 
        453092731L, 2181625025L, 4111451223L, 1706088902L, 314042704L, 2344532202L, 4240017532L, 1658658271L, 366619977L, 2362670323L, 4224994405L, 1303535960L, 
        984961486L, 2747007092L, 3569037538L, 1256170817L, 1037604311L, 2765210733L, 3554079995L, 1131014506L, 879679996L, 2909243462L, 3663771856L, 1141124467L, 
        855842277L, 2852801631L, 3708648649L, 1342533948L, 654459306L, 3188396048L, 3373015174L, 1466479909L, 544179635L, 3110523913L, 3462522015L, 1591671054L, 
        702138776L, 2966460450L, 3352799412L, 1504918807L, 783551873L, 3082640443L, 3233442989L, 3988292384L, 2596254646L, 62317068L, 1957810842L, 3939845945L, 
        2647816111L, 81470997L, 1943803523L, 3814918930L, 2489596804L, 225274430L, 2053790376L, 3826175755L, 2466906013L, 167816743L, 2097651377L, 4027552580L, 
        2265490386L, 503444072L, 1762050814L, 4150417245L, 2154129355L, 426522225L, 1852507879L, 4275313526L, 2312317920L, 282753626L, 1742555852L, 4189708143L, 
        2394877945L, 397917763L, 1622183637L, 3604390888L, 2714866558L, 953729732L, 1340076626L, 3518719985L, 2797360999L, 1068828381L, 1219638859L, 3624741850L, 
        2936675148L, 906185462L, 1090812512L, 3747672003L, 2825379669L, 829329135L, 1181335161L, 3412177804L, 3160834842L, 628085408L, 1382605366L, 3423369109L, 
        3138078467L, 570562233L, 1426400815L, 3317316542L, 2998733608L, 733239954L, 1555261956L, 3268935591L, 3050360625L, 752459403L, 1541320221L, 2607071920L, 
        3965973030L, 1969922972L, 40735498L, 2617837225L, 3943577151L, 1913087877L, 83908371L, 2512341634L, 3803740692L, 2075208622L, 213261112L, 2463272603L, 
        3855990285L, 2094854071L, 198958881L, 2262029012L, 4057260610L, 1759359992L, 534414190L, 2176718541L, 4139329115L, 1873836001L, 414664567L, 2282248934L, 
        4279200368L, 1711684554L, 285281116L, 2405801727L, 4167216745L, 1634467795L, 376229701L, 2685067896L, 3608007406L, 1308918612L, 956543938L, 2808555105L, 
        3495958263L, 1231636301L, 1047427035L, 2932959818L, 3654703836L, 1088359270L, 936918000L, 2847714899L, 3736837829L, 1202900863L, 817233897L, 3183342108L, 
        3401237130L, 1404277552L, 615818150L, 3134207493L, 3453421203L, 1423857449L, 601450431L, 3009837614L, 3294710456L, 1567103746L, 711928724L, 3020668471L, 
        3272380065L, 1510334235L, 755167117};

    @JRubyMethod(name = "crc_table", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject crc_table(IRubyObject recv) {
        List<IRubyObject> ll = new ArrayList<IRubyObject>(crctab.length);
        for(int i=0;i<crctab.length;i++) {
            ll.add(recv.getRuntime().newFixnum(crctab[i]));
        }
        return recv.getRuntime().newArray(ll);
    }


    @JRubyClass(name="Zlib::ZStream")
    public static abstract class ZStream extends RubyObject {
        protected boolean closed = false;

        protected abstract int internalTotalIn();
        protected abstract int internalTotalOut();
        protected abstract boolean internalStreamEndP();
        protected abstract void internalReset();
        protected abstract boolean internalFinished();
        protected abstract int internalAdler();

        // TODO: eliminate?
        protected abstract IRubyObject internalFinish();
        protected abstract void internalClose();

        public ZStream(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(Block unusedBlock) {
            return this;
        }

        @JRubyMethod(name = "flush_next_out")
        public IRubyObject flush_next_out(ThreadContext context) {
            return RubyString.newEmptyString(context.getRuntime());
        }

        @JRubyMethod(name = "total_out")
        public IRubyObject total_out() {
            return getRuntime().newFixnum(internalTotalOut());
        }

        @JRubyMethod(name = "stream_end?")
        public IRubyObject stream_end_p() {
            return internalStreamEndP() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod(name = "data_type")
        public IRubyObject data_type() {
            return getRuntime().fastGetModule("Zlib").fastGetConstant("UNKNOWN");
        }

        @JRubyMethod(name = { "closed?", "ended?"})
        public IRubyObject closed_p() {
            return closed ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod(name = "reset")
        public IRubyObject reset() {
            internalReset();
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "avail_out")
        public IRubyObject avail_out() {
            return RubyFixnum.zero(getRuntime());
        }

        @JRubyMethod(name = "avail_out=", required = 1)
        public IRubyObject set_avail_out(IRubyObject p1) {
            return p1;
        }

        @JRubyMethod(name = "adler")
        public IRubyObject adler() {
            return getRuntime().newFixnum(internalAdler());
        }

        @JRubyMethod(name = "finish", backtrace = true)
        public IRubyObject finish(ThreadContext context) {
            if (closed) {
                Ruby runtime = context.getRuntime();
                RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("Error");
                throw new RaiseException(RubyException.newException(
                        runtime, errorClass, "stream is not ready"), true);
            }

            IRubyObject result = internalFinish();

            return result;
        }

        @JRubyMethod(name = "avail_in")
        public IRubyObject avail_in() {
            return RubyFixnum.zero(getRuntime());
        }

        @JRubyMethod(name = "flush_next_in")
        public IRubyObject flush_next_in(ThreadContext context) {
            return RubyString.newEmptyString(context.getRuntime());
        }

        @JRubyMethod(name = "total_in")
        public IRubyObject total_in() {
            return getRuntime().newFixnum(internalTotalIn());
        }

        @JRubyMethod(name = "finished?")
        public IRubyObject finished_p(ThreadContext context) {
            Ruby runtime = context.getRuntime();
            if (closed) {
                RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("Error");
                throw new RaiseException(RubyException.newException(
                        runtime, errorClass, "stream is not ready"), true);
            }

            return internalFinished() ? runtime.getTrue() : runtime.getFalse();
        }

        @JRubyMethod(name = {"close", "end"})
        public IRubyObject close() {
            if (!closed) {
                internalClose();
                closed = true;
            }
            return getRuntime().getNil();
        }
    }

    @JRubyClass(name="Zlib::Inflate", parent="Zlib::ZStream")
    public static class Inflate extends ZStream {
        public static final int BASE_SIZE = 100;

        protected static final ObjectAllocator INFLATE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Inflate(runtime, klass);
            }
        };

        public Inflate(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(name = "inflate", required = 1, meta = true, backtrace = true)
        public static IRubyObject s_inflate(ThreadContext context, IRubyObject recv, IRubyObject string) {
            RubyClass klass = (RubyClass) recv;
            Inflate inflate = (Inflate) klass.allocate();
            inflate.init(MAX_WBITS);

            IRubyObject result;
            try {
                inflate.append(string.convertToString().getByteList());
            } finally {
                result = inflate.finish(context);
                inflate.close();
            }
            return result;
        }

        @JRubyMethod(name = "initialize", optional = 1, visibility = Visibility.PRIVATE)
        public IRubyObject _initialize(IRubyObject[] args) throws Exception {
            int window_bits = MAX_WBITS;

            if(args.length > 0 && !args[0].isNil()) {
                window_bits = RubyNumeric.fix2int(args[0]);
            }

            init(window_bits);
            return this;
        }

        private void init(int window_bits) {
            flater = new Inflater(window_bits < 0);
            collected = new ByteList(BASE_SIZE);
            input = new ByteList();
        }

        @Override
        @JRubyMethod(name = "flush_next_out")
        public IRubyObject flush_next_out(ThreadContext context) {
            return flushOutput(context.getRuntime());
        }

        private IRubyObject flushOutput(Ruby runtime) {
            if (collected.realSize > 0) {
                IRubyObject res = RubyString.newString(runtime,
                        collected.bytes, collected.begin, collected.realSize);
                resetBuffer(collected);
                return res;
            }
            return RubyString.newEmptyString(runtime);
        }

        @JRubyMethod(name = "<<", required = 1)
        public IRubyObject append(ThreadContext context, IRubyObject arg) {
            if (arg.isNil()) {
                run(true);
            } else {
                append(arg.convertToString().getByteList());
            }
            return this;
        }

        public void append(ByteList obj) {
            if (!internalFinished()) {
                byte[] bytes = obj.bytes();
                flater.setInput(bytes);
                input = new ByteList(bytes, false);
                run(false);
            } else {
                input.append(obj);
            }
        }

        @JRubyMethod(name = "sync_point?")
        public IRubyObject sync_point_p() {
            return sync_point();
        }

        public IRubyObject sync_point() {
            return getRuntime().getFalse();
        }

        @JRubyMethod(name = "set_dictionary", required = 1, backtrace = true)
        public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) throws Exception {
            try {
                return set_dictionary(arg);
            } catch (IllegalArgumentException iae) {
                Ruby runtime = context.getRuntime();
                RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("StreamError");
                throw new RaiseException(RubyException.newException(runtime, errorClass, iae.getMessage()), true);
            }
        }

        public IRubyObject set_dictionary(IRubyObject str) throws UnsupportedEncodingException {
            flater.setDictionary(str.convertToString().getBytes());
            run(false);
            return str;
        }

        @JRubyMethod(name = "inflate", required = 1, backtrace = true)
        public IRubyObject inflate(ThreadContext context, IRubyObject string) {
            ByteList data = null;
            if (!string.isNil()) {
                data = string.convertToString().getByteList();
            }
            return inflate(context, data);
        }

        public IRubyObject inflate(ThreadContext context, ByteList str) {
            if (null == str) {
                return internalFinish();
            } else {
                append(str);
                return flushOutput(context.getRuntime());
            }
        }

        @JRubyMethod(name = "sync", required = 1)
        public IRubyObject sync(ThreadContext context, IRubyObject string) {
            append(context, string);
            return context.getRuntime().getFalse();
        }

        private void run(boolean finish) {
            byte[] outp = new byte[1024];
            int resultLength = -1;

            while (!internalFinished() && resultLength != 0) {
                Ruby runtime = getRuntime();

                // MRI behavior
                if (finish && flater.needsInput()) {
                    RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("BufError");
                    throw new RaiseException(RubyException.newException(
                            runtime, errorClass, "buffer error"), true);
                }

                try {
                    resultLength = flater.inflate(outp);
                    if (flater.needsDictionary()) {
                        RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("NeedDict");
                        throw new RaiseException(RubyException.newException(
                                runtime, errorClass, "need dictionary"));
                    } else {
                        if (input.realSize > 0) {
                            int remaining = flater.getRemaining();
                            if (remaining > 0) {
                                input.view(input.realSize - remaining, remaining);
                            } else {
                                resetBuffer(input);
                            }
                        }
                    }
                } catch (DataFormatException ex) {
                    RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("DataError");
                    throw new RaiseException(RubyException.newException(
                            runtime, errorClass, ex.getMessage()), true);
                }

                collected.append(outp, 0, resultLength);
                if (resultLength == outp.length) {
                    outp = new byte[outp.length * 2];
                }
            }

            // MRI behavior: in finished mode, we work as pass-through
            if (internalFinished() && finish) {
                if (input.realSize > 0) {
                    collected.append(input);
                    resetBuffer(input);
                }
            }

            if (finish) {
                flater.end();
            }
        }

        protected int internalTotalOut() {
            return flater.getTotalOut();
        }

        protected boolean internalStreamEndP() {
            return flater.finished();
        }

        protected int internalAdler() {
            return flater.getAdler();
        }

        protected int internalTotalIn() {
            return flater.getTotalIn();
        }

        @Override
        protected boolean internalFinished() {
            return flater.finished();
        }

        @Override
        protected IRubyObject internalFinish() {
            run(true);
            flater.end();
            resetBuffer(input);
            return flushOutput(getRuntime());
        }

        protected void internalClose() {
            flater.end();
        }

        protected void internalEnd() {
            flater.end();
        }

        protected void internalReset() {
            flater.reset();
        }

        private Inflater flater;
        private ByteList collected;
        private ByteList input;
    }

    static void checkLevel(Ruby runtime, int level) {
        if ((level < 0 || level > 9) && level != Deflater.DEFAULT_COMPRESSION) {
            RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("StreamError");
            throw new RaiseException(RubyException.newException(
                    runtime, errorClass, "stream error: invalid level"), true);
        }
    }

    static void checkStrategy(Ruby runtime, int strategy) {
        switch (strategy) {
            case Deflater.DEFAULT_STRATEGY:
            case Deflater.FILTERED:
            case Deflater.HUFFMAN_ONLY:
                break;
            default:
                RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("StreamError");
                throw new RaiseException(RubyException.newException(
                        runtime, errorClass, "stream error: invalid strategy"), true);
        }
    }

    @JRubyClass(name="Zlib::Deflate", parent="Zlib::ZStream")
    public static class Deflate extends ZStream {
        protected static final ObjectAllocator DEFLATE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Deflate(runtime, klass);
            }
        };

        @JRubyMethod(name = "deflate", required = 1, optional = 1, meta = true, backtrace = true)
        public static IRubyObject s_deflate(IRubyObject recv, IRubyObject[] args) throws Exception {
            Ruby runtime = recv.getRuntime();
            args = Arity.scanArgs(runtime,args,1,1);
            int level = Deflater.DEFAULT_COMPRESSION;
            if(!args[1].isNil()) {
                level = RubyNumeric.fix2int(args[1]);
                checkLevel(runtime, level);
            }
            return ZlibDeflate.s_deflate(recv,args[0].convertToString().getByteList(),level);
        }

        public Deflate(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        private ZlibDeflate defl;

        @JRubyMethod(name = "dup")
        public IRubyObject op_dup() {
            throw getRuntime().newNotImplementedError("Zlib::Deflate#dup is not yet implemented");
        }

        @JRubyMethod(name = "initialize", optional = 4, visibility = Visibility.PRIVATE, backtrace = true)
        public IRubyObject _initialize(IRubyObject[] args) throws Exception {
            args = Arity.scanArgs(getRuntime(),args,0,4);
            int level = -1;
            int window_bits = MAX_WBITS;
            int memlevel = 8;
            int strategy = 0;
            if(!args[0].isNil()) {
                level = RubyNumeric.fix2int(args[0]);
                checkLevel(getRuntime(), level);
            }
            if(!args[1].isNil()) {
                window_bits = RubyNumeric.fix2int(args[1]);
            }
            if(!args[2].isNil()) {
                memlevel = RubyNumeric.fix2int(args[2]);
            }
            if(!args[3].isNil()) {
                strategy = RubyNumeric.fix2int(args[3]);
            }
            defl = new ZlibDeflate(this,level,window_bits,memlevel,strategy);
            return this;
        }

        @JRubyMethod(name = "<<", required = 1)
        public IRubyObject append(IRubyObject arg) throws Exception {
            defl.append(arg);
            return this;
        }

        @JRubyMethod(name = "params", required = 2)
        public IRubyObject params(ThreadContext context, IRubyObject level, IRubyObject strategy) {
            Ruby runtime = context.getRuntime();

            int l = RubyNumeric.fix2int(level);
            checkLevel(runtime, l);

            int s = RubyNumeric.fix2int(strategy);
            checkStrategy(runtime, s);

            defl.params(l, s);
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "set_dictionary", required = 1, backtrace = true)
        public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) throws Exception {
            try {
                return defl.set_dictionary(arg);
            } catch (IllegalArgumentException iae) {
                Ruby runtime = context.getRuntime();
                RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("StreamError");
                throw new RaiseException(RubyException.newException(runtime, errorClass, iae.getMessage()), true);
            }
        }
        
        @JRubyMethod(name = "flush", optional = 1)
        public IRubyObject flush(IRubyObject[] args) throws Exception {
            int flush = 2; // SYNC_FLUSH
            if(args.length == 1) {
                if(!args[0].isNil()) {
                    flush = RubyNumeric.fix2int(args[0]);
                }
            }
            return defl.flush(flush);
        }

        @JRubyMethod(name = "deflate", required = 1, optional = 1)
        public IRubyObject deflate(IRubyObject[] args) throws Exception {
            args = Arity.scanArgs(getRuntime(),args,1,1);

            ByteList data = null;
            if (!args[0].isNil()) {
                data = args[0].convertToString().getByteList();
            }

            int flush = 0; // By default, NO_FLUSH
            if(!args[1].isNil()) {
                flush = RubyNumeric.fix2int(args[1]);
            }
            return defl.deflate(data, flush);
        }

        protected int internalTotalOut() {
            return defl.getDeflater().getTotalOut();
        }

        protected boolean internalStreamEndP() {
            return defl.getDeflater().finished();
        }

        protected void internalReset() {
            defl.getDeflater().reset();
        }

        protected int internalAdler() {
            return defl.getDeflater().getAdler();
        }

        @Override
        public boolean internalFinished() {
            return defl.getDeflater().finished();
        }

        protected IRubyObject internalFinish() {
            return defl.finish();
        }

        protected int internalTotalIn() {
            return defl.getDeflater().getTotalIn();
        }

        protected void internalClose() {
            defl.close();
        }
    }

    @JRubyClass(name="Zlib::GzipFile")
    public static class RubyGzipFile extends RubyObject {
        @JRubyClass(name="Zlib::GzipFile::Error", parent="Zlib::Error")
        public static class Error {}
        @JRubyClass(name="Zlib::GzipFile::CRCError", parent="Zlib::GzipFile::Error")
        public static class CRCError extends Error {}
        @JRubyClass(name="Zlib::GzipFile::NoFooter", parent="Zlib::GzipFile::Error")
        public static class NoFooter extends Error {}
        @JRubyClass(name="Zlib::GzipFile::LengthError", parent="Zlib::GzipFile::Error")
        public static class LengthError extends Error {}

        private static IRubyObject wrapBlock(ThreadContext context, RubyGzipFile instance, 
                Block block) throws IOException {
            if (block.isGiven()) {
                try {
                    return block.yield(context, instance);
                } finally {
                    if (!instance.isClosed()) instance.close();
                }
            }
            
            return instance;
        }
        
        @JRubyMethod(name = "wrap", required = 1, frame = true, meta = true)
        public static IRubyObject wrap(ThreadContext context, IRubyObject recv, IRubyObject io, Block block) throws IOException {
            Ruby runtime = recv.getRuntime();
            RubyGzipFile instance;
            
            // TODO: People extending GzipWriter/reader will break.  Find better way here.
            if (recv == runtime.getModule("Zlib").getClass("GzipWriter")) {
                instance = RubyGzipWriter.newGzipWriter(recv, new IRubyObject[] { io }, block);
            } else {
                instance = RubyGzipReader.newInstance(recv, new IRubyObject[] { io }, block);
            }

            return wrapBlock(context, instance, block);
        }
        
        protected static final ObjectAllocator GZIPFILE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyGzipFile(runtime, klass);
            }
        };

        @JRubyMethod(name = "new", frame = true, meta = true)
        public static RubyGzipFile newInstance(IRubyObject recv, Block block) {
            RubyClass klass = (RubyClass)recv;
            
            RubyGzipFile result = (RubyGzipFile) klass.allocate();
            
            result.callInit(new IRubyObject[0], block);
            
            return result;
        }

        protected boolean closed = false;
        protected boolean finished = false;
        private int os_code = 255;
        private int level = -1;
        protected String orig_name;
        protected String comment;
        protected IRubyObject realIo;
        protected RubyTime mtime;

        public RubyGzipFile(Ruby runtime, RubyClass type) {
            super(runtime, type);
            mtime = RubyTime.newTime(runtime, new DateTime());
        }
        
        @JRubyMethod(name = "os_code")
        public IRubyObject os_code() {
            return getRuntime().newFixnum(os_code);
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
            if(closed) {
                RubyClass errorClass = getRuntime().fastGetModule("Zlib").fastGetClass("GzipFile").fastGetClass("Error");
                throw new RaiseException(RubyException.newException(getRuntime(), errorClass, "closed gzip stream"));
            }
            return orig_name == null ? getRuntime().getNil() : getRuntime().newString(orig_name);
        }
        
        @JRubyMethod(name = "to_io")
        public IRubyObject to_io() {
            return realIo;
        }

        @JRubyMethod(name = "comment")
        public IRubyObject comment() {
            if(closed) {
                RubyClass errorClass = getRuntime().fastGetModule("Zlib").fastGetClass("GzipFile").fastGetClass("Error");
                throw new RaiseException(RubyException.newException(getRuntime(), errorClass, "closed gzip stream"));
            }
            return comment == null ? getRuntime().getNil() : RubyString.newString(getRuntime(), comment);
        }

        @JRubyMethod(name = "crc")
        public IRubyObject crc() {
            return RubyFixnum.zero(getRuntime());
        }
        
        @JRubyMethod(name = "mtime")
        public IRubyObject mtime() {
            return mtime;
        }
        
        @JRubyMethod(name = "sync")
        public IRubyObject sync() {
            return getRuntime().getNil();
        }
        
        @JRubyMethod(name = "finish")
        public IRubyObject finish() throws IOException {
            if (!finished) {
                //io.finish();
            }
            finished = true;
            return realIo;
        }

        @JRubyMethod(name = "close")
        public IRubyObject close() throws IOException {
            return null;
        }
        
        @JRubyMethod(name = "level")
        public IRubyObject level() {
            return getRuntime().newFixnum(level);
        }
        
        @JRubyMethod(name = "sync=", required = 1)
        public IRubyObject set_sync(IRubyObject ignored) {
            return getRuntime().getNil();
        }

    }

    @JRubyClass(name="Zlib::GzipReader", parent="Zlib::GzipFile", include="Enumerable")
    public static class RubyGzipReader extends RubyGzipFile {
        @JRubyClass(name="Zlib::GzipReader::Error", parent="Zlib::GzipReader")
        public static class Error {}
        protected static final ObjectAllocator GZIPREADER_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyGzipReader(runtime, klass);
            }
        };
        
        @JRubyMethod(name = "new", rest = true, frame = true, meta = true)
        public static RubyGzipReader newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            RubyClass klass = (RubyClass)recv;
            RubyGzipReader result = (RubyGzipReader)klass.allocate();
            result.callInit(args, block);
            return result;
        }

        @JRubyMethod(name = "open", required = 1, frame = true, meta = true)
        public static IRubyObject open(final ThreadContext context, IRubyObject recv, IRubyObject filename, Block block) throws IOException {
            Ruby runtime = recv.getRuntime();
            IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", filename, runtime.newString("rb"));
            RubyGzipReader gzio = newInstance(recv, new IRubyObject[]{io}, block);
            return RubyGzipFile.wrapBlock(context, gzio, block);
        }

        public RubyGzipReader(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }
        
        private int line;
        private int position;
        private InputStream io;
        
        @JRubyMethod(name = "initialize", required = 1, frame = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(IRubyObject io, Block unusedBlock) {
            realIo = io;
            try {
                this.io = new GZIPInputStream(new IOInputStream(io));
            } catch (IOException e) {
                Ruby runtime = io.getRuntime();
                RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("GzipReader").fastGetClass("Error");
                throw new RaiseException(RubyException.newException(runtime, errorClass, e.getMessage()));
            }

            line = 1;
            position= 0;
            
            return this;
        }
        
        @JRubyMethod(name = "rewind")
        public IRubyObject rewind() {
            this.position= 0;
            // should invoke seek on realIo
            realIo.callMethod(getRuntime().getCurrentContext(), "seek", getRuntime().newFixnum(0));
            return getRuntime().getNil();
        }
        
        @JRubyMethod(name = "lineno")
        public IRubyObject lineno() {
            return getRuntime().newFixnum(line);
        }

        @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
        public IRubyObject readline(ThreadContext context) throws IOException {
            IRubyObject dst = gets(context, new IRubyObject[0]);
            if (dst.isNil()) {
                throw getRuntime().newEOFError();
            }
            return dst;
        }

        public IRubyObject internalGets(IRubyObject[] args) throws IOException {
            ByteList sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getByteList();
            if (args.length > 0) {
                sep = args[0].convertToString().getByteList();
            }
            return internalSepGets(sep);
        }

        private IRubyObject internalSepGets(ByteList sep) throws IOException {
            ByteList result = new ByteList();
            int ce = io.read();
            while (ce != -1 && sep.indexOf(ce) == -1) {
                result.append((byte)ce);
                ce = io.read();
            }
            // io.available() only returns 0 after EOF is encountered
            // so we need to differentiate between the empty string and EOF
            if (0 == result.length() && -1 == ce) {
              return getRuntime().getNil();
            }
            line++;
            this.position= result.length();
            result.append(sep);
            return RubyString.newString(getRuntime(),result);
        }

        @JRubyMethod(name = "gets", optional = 1, writes = FrameField.LASTLINE)
        public IRubyObject gets(ThreadContext context, IRubyObject[] args) throws IOException {
            IRubyObject result = internalGets(args);
            if (!result.isNil()) {
                context.getCurrentScope().setLastLine(result);
            }
            return result;
        }

        private final static int BUFF_SIZE = 4096;
        
        @JRubyMethod(name = "read", optional = 1)
        public IRubyObject read(IRubyObject[] args) throws IOException {
            if (args.length == 0 || args[0].isNil()) {
                ByteList val = new ByteList(10);
                byte[] buffer = new byte[BUFF_SIZE];
                int read = io.read(buffer);
                while (read != -1) {
                    val.append(buffer,0,read);
                    read = io.read(buffer);
                }
                this.position += val.length();
                return RubyString.newString(getRuntime(),val);
            } 

            int len = RubyNumeric.fix2int(args[0]);
            if (len < 0) {
            	throw getRuntime().newArgumentError("negative length " + len + " given");
            } else if (len > 0) {
            	byte[] buffer = new byte[len];
            	int toRead = len;
            	int offset = 0;
            	int read = 0;
            	while (toRead > 0) {
            		read = io.read(buffer,offset,toRead);
            		if (read == -1) {
            			break;
            		}
            		toRead -= read;
            		offset += read;
            	} // hmm...
                this.position += buffer.length;
            	return RubyString.newString(getRuntime(),new ByteList(buffer,0,len-toRead,false));
            }
                
            return RubyString.newEmptyString(getRuntime());
        }

        @JRubyMethod(name = "lineno=", required = 1)
        public IRubyObject set_lineno(IRubyObject lineArg) {
            line = RubyNumeric.fix2int(lineArg);
            return lineArg;
        }

        @JRubyMethod(name = "pos")
        public IRubyObject pos() {
            return RubyFixnum.int2fix(getRuntime(), position);
        }
        
        @JRubyMethod(name = "readchar")
        public IRubyObject readchar() throws IOException {
            int value = io.read();
            if (value == -1) {
                throw getRuntime().newEOFError();
            }
            position++;
            return getRuntime().newFixnum(value);
        }

        @JRubyMethod(name = "getc")
        public IRubyObject getc() throws IOException {
            int value = io.read();
            return value == -1 ? getRuntime().getNil() : getRuntime().newFixnum(value);
        }

        private boolean isEof() throws IOException {
            return ((GZIPInputStream)io).available() == 0;
        }

        @Override
        @JRubyMethod(name = "close")
        public IRubyObject close() throws IOException {
            if (!closed) {
                io.close();
            }
            this.closed = true;
            return getRuntime().getNil();
        }
        
        @JRubyMethod(name = "eof")
        public IRubyObject eof() throws IOException {
            return isEof() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod(name = "eof?")
        public IRubyObject eof_p() throws IOException {
            return eof();
        }

        @JRubyMethod(name = "unused")
        public IRubyObject unused() {
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "tell")
        public IRubyObject tell() {
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "each", optional = 1, frame = true)
        public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) throws IOException {
            ByteList sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getByteList();
            
            if (args.length > 0 && !args[0].isNil()) {
                sep = args[0].convertToString().getByteList();
            }
            for (IRubyObject result = internalSepGets(sep); !result.isNil(); result = internalSepGets(sep)) {
                block.yield(context, result);
            }
            
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "each_line", optional = 1, frame = true)
        public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) throws IOException {
          return each(context, args, block);
        }
    
        @JRubyMethod(name = "ungetc", required = 1)
        public IRubyObject ungetc(IRubyObject arg) {
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "readlines", optional = 1)
        public IRubyObject readlines(IRubyObject[] args) throws IOException {
            List<IRubyObject> array = new ArrayList<IRubyObject>();
            
            if (args.length != 0 && args[0].isNil()) {
                array.add(read(new IRubyObject[0]));
            } else {
                ByteList sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getByteList();
                if (args.length > 0) {
                    sep = args[0].convertToString().getByteList();
                }
                for (IRubyObject result = internalSepGets(sep); !result.isNil(); result = internalSepGets(sep)) {
                    array.add(result);
                }
            }
            return getRuntime().newArray(array);
        }

        @JRubyMethod(name = "each_byte", frame = true)
        public IRubyObject each_byte(ThreadContext context, Block block) throws IOException {
            int value = io.read();

            while (value != -1) {
                block.yield(context, getRuntime().newFixnum(value));
                value = io.read();
            }
            
            return getRuntime().getNil();
        }
    }

    @JRubyClass(name="Zlib::GzipWriter", parent="Zlib::GzipFile")
    public static class RubyGzipWriter extends RubyGzipFile {
        protected static final ObjectAllocator GZIPWRITER_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyGzipWriter(runtime, klass);
            }
        };
        
        @JRubyMethod(name = "new", rest = true, frame = true, meta = true)
        public static RubyGzipWriter newGzipWriter(IRubyObject recv, IRubyObject[] args, Block block) {
            RubyClass klass = (RubyClass)recv;
            
            RubyGzipWriter result = (RubyGzipWriter)klass.allocate();
            result.callInit(args, block);
            return result;
        }

        @JRubyMethod(name = "open", required = 1, optional = 2, frame = true, meta = true)
        public static IRubyObject open(final ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) throws IOException {
            Ruby runtime = recv.getRuntime();
            IRubyObject level = runtime.getNil();
            IRubyObject strategy = runtime.getNil();

            if (args.length > 1) {
                level = args[1];
                if (args.length > 2) strategy = args[2];
            }

            IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("wb"));
            RubyGzipWriter gzio = newGzipWriter(recv, new IRubyObject[]{io, level, strategy}, block);
            return RubyGzipFile.wrapBlock(context, gzio, block);
        }

        public RubyGzipWriter(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        public class HeaderModifyableGZIPOutputStream extends DeflaterOutputStream {
            public HeaderModifyableGZIPOutputStream(OutputStream outputStream) throws IOException {
                super(outputStream, new Deflater(Deflater.DEFAULT_COMPRESSION, true), DEFAULT_BUFFER_SIZE);
            }

            @Override
            public synchronized void write(byte bytes[], int offset, int length) throws IOException {
                writeHeaderIfNeeded();

                super.write(bytes, offset, length);
                checksum.update(bytes, offset, length);
            }

            @Override
            public void finish() throws IOException {
                writeHeaderIfNeeded();

                super.finish();

                writeTrailer();
            }

            public void setModifiedTime(long newModifiedTime) {
                if (!headerIsWritten()) modifiedTime = newModifiedTime;
            }

            public boolean headerIsWritten() {
                return headerIsWritten;
            }

            // Called before any write to make sure the
            // header is always written before the first bytes
            private void writeHeaderIfNeeded() throws IOException {
                if (headerIsWritten == false) {
                    writeHeader();
                    headerIsWritten = true;
                }
            }

            private void writeHeader() throws IOException {
                //  See http://www.gzip.org/zlib/rfc-gzip.html
                final byte header[] = { GZIP_MAGIC_ID_1, GZIP_MAGIC_ID_2,
                    COMPRESSION_TYPE_DEFLATED,
                    0, // flags
                    (byte) (modifiedTime), (byte)(modifiedTime >> 8), // 4 bytes of modified time
                    (byte) (modifiedTime >> 16),(byte)(modifiedTime >> 24),
                    0, // extras flag
                    GZIP_OS_TYPE_UNKNOWN
                };

                out.write(header);
            }

            private void writeTrailer() throws IOException {
                final int originalDataSize = def.getTotalIn();
                final int checksumInt = (int) checksum.getValue();

                final byte[] trailer = {
                    (byte) (checksumInt), (byte)(checksumInt >> 8),
                    (byte) (checksumInt >> 16), (byte)(checksumInt >> 24),

                    (byte) (originalDataSize), (byte)(originalDataSize >> 8),
                    (byte) (originalDataSize >> 16), (byte)(originalDataSize >> 24)
                };

                out.write(trailer);
            }

            private CRC32   checksum        = new CRC32();
            private boolean headerIsWritten = false;
            private long    modifiedTime    = System.currentTimeMillis();

            private final static int  DEFAULT_BUFFER_SIZE       = 512;
            private final static byte COMPRESSION_TYPE_DEFLATED = (byte) 8;
            private final static byte GZIP_MAGIC_ID_1           = (byte) 0x1f;
            private final static byte GZIP_MAGIC_ID_2           = (byte) 0x8b;
            private final static byte GZIP_OS_TYPE_UNKNOWN      = (byte) 255;
        }

        private HeaderModifyableGZIPOutputStream io;
        
        @JRubyMethod(name = "initialize", required = 1, rest = true, frame = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize2(IRubyObject[] args, Block unusedBlock) throws IOException {
            realIo = (RubyObject) args[0];
            io = new HeaderModifyableGZIPOutputStream(new IOOutputStream(realIo, false, false));
            return this;
        }

        @Override
        @JRubyMethod(name = "close")
        public IRubyObject close() throws IOException {
            if (!closed) io.close();

            this.closed = true;
            
            return getRuntime().getNil();
        }

        @JRubyMethod(name = {"append", "<<"}, required = 1)
        public IRubyObject append(IRubyObject p1) throws IOException {
            this.write(p1);
            return this;
        }

        @JRubyMethod(name = "printf", required = 1, rest = true)
        public IRubyObject printf(ThreadContext context, IRubyObject[] args) throws IOException {
            write(RubyKernel.sprintf(context, this, args));
            return context.getRuntime().getNil();
        }

        @JRubyMethod(name = "print", rest = true)
        public IRubyObject print(IRubyObject[] args) throws IOException {
            if (args.length != 0) {
                for (int i = 0, j = args.length; i < j; i++) {
                    write(args[i]);
                }
            }
            
            IRubyObject sep = getRuntime().getGlobalVariables().get("$\\");
            if (!sep.isNil()) {
                write(sep);
            }
            
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "pos")
        public IRubyObject pos() {
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "orig_name=", required = 1)
        public IRubyObject set_orig_name(IRubyObject str) {
            this.orig_name= str.convertToString().toString();
            return str;
        }

        @JRubyMethod(name = "comment=", required = 1)
        public IRubyObject set_comment(IRubyObject str) {
            this.comment= str.convertToString().toString();
            return str;
        }

        @JRubyMethod(name = "putc", required = 1)
        public IRubyObject putc(IRubyObject p1) throws IOException {
            io.write(RubyNumeric.fix2int(p1));
            return p1;
        }
        
        @JRubyMethod(name = "puts", rest = true)
        public IRubyObject puts(ThreadContext context, IRubyObject[] args) throws IOException {
            RubyStringIO sio = (RubyStringIO)getRuntime().fastGetClass("StringIO").newInstance(context, new IRubyObject[0], Block.NULL_BLOCK);
            sio.puts(context, args);
            write(sio.string());
            
            return getRuntime().getNil();
        }

        public IRubyObject finish() throws IOException {
            if (!finished) io.finish();

            finished = true;
            return realIo;
        }

        @JRubyMethod(name = "flush", optional = 1)
        public IRubyObject flush(IRubyObject[] args) throws IOException {
            if (args.length == 0 || args[0].isNil() || RubyNumeric.fix2int(args[0]) != 0) { // Zlib::NO_FLUSH
                io.flush();
            }
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "mtime=", required = 1)
        public IRubyObject set_mtime(IRubyObject arg) {
            if( io.headerIsWritten() ) {
                RubyClass errorClass = getRuntime().fastGetModule("Zlib").fastGetClass("GzipFile").fastGetClass("Error");
                throw new RaiseException(RubyException.newException(getRuntime(), errorClass, "header is already written"));
            }
            if (arg instanceof RubyTime) {
                this.mtime = ((RubyTime) arg);
            } else if(arg.isNil()) {
                // ...nothing
            } else {
                this.mtime.setDateTime(new DateTime( RubyNumeric.fix2long(arg)*1000 ));
            }
            
            io.setModifiedTime( this.mtime.to_i().getLongValue() );
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "tell")
        public IRubyObject tell() {
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "write", required = 1)
        public IRubyObject write(IRubyObject p1) throws IOException {
            ByteList bytes = p1.asString().getByteList();
            io.write(bytes.unsafeBytes(), bytes.begin(), bytes.length());
            return getRuntime().newFixnum(bytes.length());
        }
    }

    // utility method
    static void resetBuffer(ByteList l) {
        l.begin = 0;
        l.realSize = 0;
        l.invalidate();
    }
}

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

import java.io.InputStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.jruby.exceptions.RaiseException;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.CRC32Ext;
import org.jruby.util.Adler32Ext;
import org.jruby.util.ZlibInflate;
import org.jruby.util.ZlibDeflate;

import org.jruby.util.ByteList;

public class RubyZlib {
    /** Create the Zlib module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createZlibModule(IRuby runtime) {
        RubyModule result = runtime.defineModule("Zlib");

        RubyClass gzfile = result.defineClassUnder("GzipFile", runtime.getObject(), RubyGzipFile.GZIPFILE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyGzipFile.class);
        gzfile.getMetaClass().defineMethod("wrap", callbackFactory.getSingletonMethod("wrap", RubyGzipFile.class, IRubyObject.class));
        gzfile.getMetaClass().defineMethod("new", callbackFactory.getSingletonMethod("newInstance"));
        gzfile.defineFastMethod("os_code", callbackFactory.getFastMethod("os_code"));
        gzfile.defineFastMethod("closed?", callbackFactory.getFastMethod("closed_p"));
        gzfile.defineFastMethod("orig_name", callbackFactory.getFastMethod("orig_name"));
        gzfile.defineFastMethod("to_io", callbackFactory.getFastMethod("to_io"));
        gzfile.defineFastMethod("finish", callbackFactory.getFastMethod("finish"));
        gzfile.defineFastMethod("comment", callbackFactory.getFastMethod("comment"));
        gzfile.defineFastMethod("crc", callbackFactory.getFastMethod("crc"));
        gzfile.defineFastMethod("mtime", callbackFactory.getFastMethod("mtime"));
        gzfile.defineFastMethod("sync", callbackFactory.getFastMethod("sync"));
        gzfile.defineFastMethod("close", callbackFactory.getFastMethod("close"));
        gzfile.defineFastMethod("level", callbackFactory.getFastMethod("level"));
        gzfile.defineFastMethod("sync=", callbackFactory.getFastMethod("set_sync", IRubyObject.class));
        
        CallbackFactory classCB = runtime.callbackFactory(RubyClass.class);
        RubyClass gzreader = result.defineClassUnder("GzipReader", gzfile, RubyGzipReader.GZIPREADER_ALLOCATOR);
        gzreader.includeModule(runtime.getModule("Enumerable"));
        CallbackFactory callbackFactory2 = runtime.callbackFactory(RubyGzipReader.class);
        gzreader.getMetaClass().defineMethod("open", callbackFactory2.getSingletonMethod("open", RubyString.class));
        gzreader.getMetaClass().defineMethod("new", classCB.getOptMethod("newInstance"));
        gzreader.defineMethod("initialize", callbackFactory2.getMethod("initialize", IRubyObject.class));
        gzreader.defineFastMethod("rewind", callbackFactory2.getFastMethod("rewind"));
        gzreader.defineFastMethod("lineno", callbackFactory2.getFastMethod("lineno"));
        gzreader.defineFastMethod("readline", callbackFactory2.getFastMethod("readline"));
        gzreader.defineFastMethod("read", callbackFactory2.getFastOptMethod("read"));
        gzreader.defineFastMethod("lineno=", callbackFactory2.getFastMethod("set_lineno", RubyNumeric.class));
        gzreader.defineFastMethod("pos", callbackFactory2.getFastMethod("pos"));
        gzreader.defineFastMethod("readchar", callbackFactory2.getFastMethod("readchar"));
        gzreader.defineFastMethod("readlines", callbackFactory2.getFastOptMethod("readlines"));
        gzreader.defineMethod("each_byte", callbackFactory2.getMethod("each_byte"));
        gzreader.defineFastMethod("getc", callbackFactory2.getFastMethod("getc"));
        gzreader.defineFastMethod("eof", callbackFactory2.getFastMethod("eof"));
        gzreader.defineFastMethod("ungetc", callbackFactory2.getFastMethod("ungetc", RubyNumeric.class));
        gzreader.defineMethod("each", callbackFactory2.getOptMethod("each"));
        gzreader.defineFastMethod("unused", callbackFactory2.getFastMethod("unused"));
        gzreader.defineFastMethod("eof?", callbackFactory2.getFastMethod("eof_p"));
        gzreader.defineFastMethod("gets", callbackFactory2.getFastOptMethod("gets"));
        gzreader.defineFastMethod("tell", callbackFactory2.getFastMethod("tell"));
        
        RubyClass standardError = runtime.getClass("StandardError");
        RubyClass zlibError = result.defineClassUnder("Error", standardError, standardError.getAllocator());
        gzreader.defineClassUnder("Error", zlibError, zlibError.getAllocator());

        RubyClass gzwriter = result.defineClassUnder("GzipWriter", gzfile, RubyGzipWriter.GZIPWRITER_ALLOCATOR);
        CallbackFactory callbackFactory3 = runtime.callbackFactory(RubyGzipWriter.class);
        gzwriter.getMetaClass().defineMethod("open", callbackFactory3.getOptSingletonMethod("open"));
        gzwriter.getMetaClass().defineMethod("new", classCB.getOptMethod("newInstance"));
        gzwriter.defineMethod("initialize", callbackFactory3.getOptMethod("initialize2"));
        gzwriter.defineFastMethod("<<", callbackFactory3.getFastMethod("append", IRubyObject.class));
        gzwriter.defineFastMethod("printf", callbackFactory3.getFastOptMethod("printf"));
        gzwriter.defineFastMethod("pos", callbackFactory3.getFastMethod("pos"));
        gzwriter.defineFastMethod("orig_name=", callbackFactory3.getFastMethod("set_orig_name", RubyString.class));
        gzwriter.defineFastMethod("putc", callbackFactory3.getFastMethod("putc", RubyNumeric.class));
        gzwriter.defineFastMethod("comment=", callbackFactory3.getFastMethod("set_comment", RubyString.class));
        gzwriter.defineFastMethod("puts", callbackFactory3.getFastOptMethod("puts"));
        gzwriter.defineFastMethod("flush", callbackFactory3.getFastOptMethod("flush"));
        gzwriter.defineFastMethod("mtime=", callbackFactory3.getFastMethod("set_mtime", IRubyObject.class));
        gzwriter.defineFastMethod("tell", callbackFactory3.getFastMethod("tell"));
        gzwriter.defineFastMethod("write", callbackFactory3.getFastMethod("write", IRubyObject.class));

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

        result.defineConstant("MAX_WBITS",runtime.newFixnum(15));

        CallbackFactory cf = runtime.callbackFactory(RubyZlib.class);
        result.defineFastModuleFunction("zlib_version",cf.getFastSingletonMethod("zlib_version"));
        result.defineFastModuleFunction("version",cf.getFastSingletonMethod("version"));
        result.defineFastModuleFunction("adler32",cf.getFastOptSingletonMethod("adler32"));
        result.defineFastModuleFunction("crc32",cf.getFastOptSingletonMethod("crc32"));
        result.defineFastModuleFunction("crc_table",cf.getFastSingletonMethod("crc_table"));

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
        CallbackFactory zstreamcb = runtime.callbackFactory(ZStream.class);
        zstream.defineMethod("initialize",zstreamcb.getMethod("initialize"));
        zstream.defineFastMethod("flush_next_out",zstreamcb.getFastMethod("flush_next_out"));
        zstream.defineFastMethod("total_out",zstreamcb.getFastMethod("total_out"));
        zstream.defineFastMethod("stream_end?",zstreamcb.getFastMethod("stream_end_p"));
        zstream.defineFastMethod("data_type",zstreamcb.getFastMethod("data_type"));
        zstream.defineFastMethod("closed?",zstreamcb.getFastMethod("closed_p"));
        zstream.defineFastMethod("ended?",zstreamcb.getFastMethod("ended_p"));
        zstream.defineFastMethod("end",zstreamcb.getFastMethod("end"));
        zstream.defineFastMethod("reset",zstreamcb.getFastMethod("reset"));
        zstream.defineFastMethod("avail_out",zstreamcb.getFastMethod("avail_out"));
        zstream.defineFastMethod("avail_out=",zstreamcb.getFastMethod("set_avail_out",IRubyObject.class));
        zstream.defineFastMethod("adler",zstreamcb.getFastMethod("adler"));
        zstream.defineFastMethod("finish",zstreamcb.getFastMethod("finish"));
        zstream.defineFastMethod("avail_in",zstreamcb.getFastMethod("avail_in"));
        zstream.defineFastMethod("flush_next_in",zstreamcb.getFastMethod("flush_next_in"));
        zstream.defineFastMethod("total_in",zstreamcb.getFastMethod("total_in"));
        zstream.defineFastMethod("finished?",zstreamcb.getFastMethod("finished_p"));
        zstream.defineFastMethod("close",zstreamcb.getFastMethod("close"));
        zstream.undefineMethod("new");

        RubyClass infl = result.defineClassUnder("Inflate", zstream, Inflate.INFLATE_ALLOCATOR);
        CallbackFactory inflcb = runtime.callbackFactory(Inflate.class);
        infl.getMetaClass().defineFastMethod("inflate",inflcb.getFastSingletonMethod("s_inflate",IRubyObject.class));
        infl.defineFastMethod("initialize",inflcb.getFastOptMethod("_initialize"));
        infl.defineFastMethod("<<",inflcb.getFastMethod("append",IRubyObject.class));
        infl.defineFastMethod("sync_point?",inflcb.getFastMethod("sync_point_p"));
        infl.defineFastMethod("set_dictionary",inflcb.getFastMethod("set_dictionary",IRubyObject.class));
        infl.defineFastMethod("inflate",inflcb.getFastMethod("inflate",IRubyObject.class));
        infl.defineFastMethod("sync",inflcb.getFastMethod("sync",IRubyObject.class));

        RubyClass defl = result.defineClassUnder("Deflate", zstream, Deflate.DEFLATE_ALLOCATOR);
        CallbackFactory deflcb = runtime.callbackFactory(Deflate.class);
        defl.getMetaClass().defineFastMethod("deflate",deflcb.getFastOptSingletonMethod("s_deflate"));
        defl.defineFastMethod("initialize",deflcb.getFastOptMethod("_initialize"));
        defl.defineFastMethod("<<",deflcb.getFastMethod("append",IRubyObject.class));
        defl.defineFastMethod("params",deflcb.getFastMethod("params",IRubyObject.class,IRubyObject.class));
        defl.defineFastMethod("set_dictionary",deflcb.getFastMethod("set_dictionary",IRubyObject.class));
        defl.defineFastMethod("flush",deflcb.getFastOptMethod("flush"));
        defl.defineFastMethod("deflate",deflcb.getFastOptMethod("deflate"));

        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(),"require",runtime.newString("stringio"));

        return result;
    }

    public static IRubyObject zlib_version(IRubyObject recv) {
        return ((RubyModule)recv).getConstant("ZLIB_VERSION");
    }

    public static IRubyObject version(IRubyObject recv) {
        return ((RubyModule)recv).getConstant("VERSION");
    }

    public static IRubyObject crc32(IRubyObject recv, IRubyObject[] args) throws Exception {
        args = recv.scanArgs(args,0,2);
        int crc = 0;
        byte[] bytes = null;
        if(!args[0].isNil()) {
            bytes = args[0].convertToString().getBytes();
        }
        if(!args[1].isNil()) {
            crc = RubyNumeric.fix2int(args[1]);
        }
        CRC32Ext ext = new CRC32Ext(crc);
        if(bytes != null) {
            ext.update(bytes);
        }
        return recv.getRuntime().newFixnum(ext.getValue());
    }

    public static IRubyObject adler32(IRubyObject recv, IRubyObject[] args) throws Exception {
        args = recv.scanArgs(args,0,2);
        int adler = 1;
        byte[] bytes = null;
        if(!args[0].isNil()) {
            bytes = args[0].convertToString().getBytes();
        }
        if(!args[1].isNil()) {
            adler = RubyNumeric.fix2int(args[1]);
        }
        Adler32Ext ext = new Adler32Ext(adler);
        if(bytes != null) {
            ext.update(bytes);
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

    public static IRubyObject crc_table(IRubyObject recv) {
        List ll = new ArrayList(crctab.length);
        for(int i=0;i<crctab.length;i++) {
            ll.add(recv.getRuntime().newFixnum(crctab[i]));
        }
        return recv.getRuntime().newArray(ll);
    }


    public static abstract class ZStream extends RubyObject {
        protected boolean closed = false;
        protected boolean ended = false;
        protected boolean finished = false;

        protected abstract int internalTotalOut();
        protected abstract boolean internalStreamEndP();
        protected abstract void internalEnd();
        protected abstract void internalReset();
        protected abstract int internalAdler();
        protected abstract IRubyObject internalFinish() throws Exception;
        protected abstract int internalTotalIn();
        protected abstract void internalClose();

        public ZStream(IRuby runtime, RubyClass type) {
            super(runtime, type);
        }

        public IRubyObject initialize(Block unusedBlock) {
            return this;
        }

        public IRubyObject flush_next_out() {
            return getRuntime().getNil();
        }

        public IRubyObject total_out() {
            return getRuntime().newFixnum(internalTotalOut());
        }

        public IRubyObject stream_end_p() {
            return internalStreamEndP() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject data_type() {
            return getRuntime().getModule("Zlib").getConstant("UNKNOWN");
        }

        public IRubyObject closed_p() {
            return closed ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject ended_p() {
            return ended ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject end() {
            if(!ended) {
                internalEnd();
                ended = true;
            }
            return getRuntime().getNil();
        }

        public IRubyObject reset() {
            internalReset();
            return getRuntime().getNil();
        }

        public IRubyObject avail_out() {
            return RubyFixnum.zero(getRuntime());
        }

        public IRubyObject set_avail_out(IRubyObject p1) {
            return p1;
        }

        public IRubyObject adler() {
            return getRuntime().newFixnum(internalAdler());
        }

        public IRubyObject finish() throws Exception {
            if(!finished) {
                finished = true;
                return internalFinish();
            }
            return getRuntime().newString("");
        }

        public IRubyObject avail_in() {
            return RubyFixnum.zero(getRuntime());
        }

        public IRubyObject flush_next_in() {
            return getRuntime().getNil();
        }

        public IRubyObject total_in() {
            return getRuntime().newFixnum(internalTotalIn());
        }

        public IRubyObject finished_p() {
            return finished ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject close() {
            if(!closed) {
                internalClose();
                closed = true;
            }
            return getRuntime().getNil();
        }
    }

    public static class Inflate extends ZStream {
        protected static ObjectAllocator INFLATE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(IRuby runtime, RubyClass klass) {
                return new Inflate(runtime, klass);
            }
        };

        public static IRubyObject s_inflate(IRubyObject recv, IRubyObject string) throws Exception {
            return ZlibInflate.s_inflate(recv,string.convertToString().getByteList());
        }

        public Inflate(IRuby runtime, RubyClass type) {
            super(runtime, type);
        }

        private ZlibInflate infl;

        public IRubyObject _initialize(IRubyObject[] args) throws Exception {
            infl = new ZlibInflate(this);
            return this;
        }

        public IRubyObject append(IRubyObject arg) {
            infl.append(arg);
            return this;
        }

        public IRubyObject sync_point_p() {
            return infl.sync_point();
        }

        public IRubyObject set_dictionary(IRubyObject arg) throws Exception {
            return infl.set_dictionary(arg);
        }

        public IRubyObject inflate(IRubyObject string) throws Exception {
            return infl.inflate(string.convertToString().getByteList());
        }

        public IRubyObject sync(IRubyObject string) {
            return infl.sync(string);
        }

        protected int internalTotalOut() {
            return infl.getInflater().getTotalOut();
        }

        protected boolean internalStreamEndP() {
            return infl.getInflater().finished();
        }

        protected void internalEnd() {
            infl.getInflater().end();
        }

        protected void internalReset() {
            infl.getInflater().reset();
        }

        protected int internalAdler() {
            return infl.getInflater().getAdler();
        }

        protected IRubyObject internalFinish() throws Exception {
            infl.finish();
            return getRuntime().getNil();
        }

        public IRubyObject finished_p() {
            return infl.getInflater().finished() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        protected int internalTotalIn() {
            return infl.getInflater().getTotalIn();
        }

        protected void internalClose() {
            infl.close();
        }
    }

    public static class Deflate extends ZStream {
        protected static ObjectAllocator DEFLATE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(IRuby runtime, RubyClass klass) {
                return new Deflate(runtime, klass);
            }
        };

        public static IRubyObject s_deflate(IRubyObject recv, IRubyObject[] args) throws Exception {
            args = recv.scanArgs(args,1,1);
            int level = -1;
            if(!args[1].isNil()) {
                level = RubyNumeric.fix2int(args[1]);
            }
            return ZlibDeflate.s_deflate(recv,args[0].convertToString().getByteList(),level);
        }

        public Deflate(IRuby runtime, RubyClass type) {
            super(runtime, type);
        }

        private ZlibDeflate defl;

        public IRubyObject _initialize(IRubyObject[] args) throws Exception {
            args = scanArgs(args,0,4);
            int level = -1;
            int window_bits = 15;
            int memlevel = 8;
            int strategy = 0;
            if(!args[0].isNil()) {
                level = RubyNumeric.fix2int(args[0]);
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

        public IRubyObject append(IRubyObject arg) throws Exception {
            defl.append(arg);
            return this;
        }

        public IRubyObject params(IRubyObject level, IRubyObject strategy) {
            defl.params(RubyNumeric.fix2int(level),RubyNumeric.fix2int(strategy));
            return getRuntime().getNil();
        }

        public IRubyObject set_dictionary(IRubyObject arg) throws Exception {
            return defl.set_dictionary(arg);
        }
        
        public IRubyObject flush(IRubyObject[] args) throws Exception {
            int flush = 2; // SYNC_FLUSH
            if(checkArgumentCount(args,0,1) == 1) {
                if(!args[0].isNil()) {
                    flush = RubyNumeric.fix2int(args[0]);
                }
            }
            return defl.flush(flush);
        }

        public IRubyObject deflate(IRubyObject[] args) throws Exception {
            args = scanArgs(args,1,1);
            int flush = 0; // NO_FLUSH
            if(!args[1].isNil()) {
                flush = RubyNumeric.fix2int(args[1]);
            }
            return defl.deflate(args[0].convertToString().getByteList(),flush);
        }

        protected int internalTotalOut() {
            return defl.getDeflater().getTotalOut();
        }

        protected boolean internalStreamEndP() {
            return defl.getDeflater().finished();
        }

        protected void internalEnd() {
            defl.getDeflater().end();
        }

        protected void internalReset() {
            defl.getDeflater().reset();
        }

        protected int internalAdler() {
            return defl.getDeflater().getAdler();
        }

        protected IRubyObject internalFinish() throws Exception {
            return defl.finish();
        }

        protected int internalTotalIn() {
            return defl.getDeflater().getTotalIn();
        }

        protected void internalClose() {
            defl.close();
        }
    }

    public static class RubyGzipFile extends RubyObject {
        public static IRubyObject wrap(IRubyObject recv, RubyGzipFile io, IRubyObject proc, Block unusedBlock) throws IOException {
            if (!proc.isNil()) {
                try {
                    ((RubyProc)proc).call(new IRubyObject[]{io});
                } finally {
                    if (!io.isClosed()) {
                        io.close();
                    }
                }
                return recv.getRuntime().getNil();
            }

            return io;
        }
        
        protected static ObjectAllocator GZIPFILE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(IRuby runtime, RubyClass klass) {
                return new RubyGzipFile(runtime, klass);
            }
        };

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
        private String orig_name;
        private String comment;
        protected IRubyObject realIo;
        private IRubyObject mtime;

        public RubyGzipFile(IRuby runtime, RubyClass type) {
            super(runtime, type);
            mtime = runtime.getNil();
        }
        
        public IRubyObject os_code() {
            return getRuntime().newFixnum(os_code);
        }
        
        public IRubyObject closed_p() {
            return closed ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        
        protected boolean isClosed() {
            return closed;
        }
        
        public IRubyObject orig_name() {
            return orig_name == null ? getRuntime().getNil() : getRuntime().newString(orig_name);
        }
        
        public Object to_io() {
            return realIo;
        }
        
        public IRubyObject comment() {
            return comment == null ? getRuntime().getNil() : getRuntime().newString(comment);
        }
        
        public IRubyObject crc() {
            return RubyFixnum.zero(getRuntime());
        }
        
        public IRubyObject mtime() {
            return mtime;
        }
        
        public IRubyObject sync() {
            return getRuntime().getNil();
        }
        
        public IRubyObject finish() throws IOException {
            if (!finished) {
                //io.finish();
            }
            finished = true;
            return realIo;
        }

        public IRubyObject close() throws IOException {
            return null;
        }
        
        public IRubyObject level() {
            return getRuntime().newFixnum(level);
        }
        
        public IRubyObject set_sync(IRubyObject ignored) {
            return getRuntime().getNil();
        }
    }

    public static class RubyGzipReader extends RubyGzipFile {
        protected static ObjectAllocator GZIPREADER_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(IRuby runtime, RubyClass klass) {
                return new RubyGzipReader(runtime, klass);
            }
        };
        
        private static RubyGzipReader newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            RubyClass klass = (RubyClass)recv;
            RubyGzipReader result = (RubyGzipReader)klass.allocate();
            result.callInit(args, block);
            return result;
        }

        public static IRubyObject open(IRubyObject recv, RubyString filename, Block block) throws IOException {
            IRuby runtime = recv.getRuntime();
            IRubyObject proc = block.isGiven() ? runtime.newProc(false, block) : runtime.getNil();
            RubyGzipReader io = newInstance(
                    recv,
                    new IRubyObject[]{ runtime.getClass("File").callMethod(
                            runtime.getCurrentContext(),
                            "open",
                            new IRubyObject[]{filename, runtime.newString("rb")})},
                            block);
            
            return RubyGzipFile.wrap(recv, io, proc, null);
        }

        public RubyGzipReader(IRuby runtime, RubyClass type) {
            super(runtime, type);
        }
        
        private int line;
        private InputStream io;
        
        public IRubyObject initialize(IRubyObject io, Block unusedBlock) {
            realIo = io;
            try {
                this.io = new GZIPInputStream(new IOInputStream(io));
            } catch (IOException e) {
                IRuby runtime = io.getRuntime();
                RubyClass errorClass = runtime.getModule("Zlib").getClass("GzipReader").getClass("Error");
                throw new RaiseException(RubyException.newException(runtime, errorClass, e.getMessage()));
            }

            line = 1;
            
            return this;
        }
        
        public IRubyObject rewind() {
            return getRuntime().getNil();
        }
        
        public IRubyObject lineno() {
            return getRuntime().newFixnum(line);
        }

        public IRubyObject readline() throws IOException {
            IRubyObject dst = gets(new IRubyObject[0]);
            if (dst.isNil()) {
                throw getRuntime().newEOFError();
            }
            return dst;
        }

        public IRubyObject internalGets(IRubyObject[] args) throws IOException {
            String sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
            if (args.length > 0) {
                sep = args[0].toString();
            }
            return internalSepGets(sep);
        }

        private IRubyObject internalSepGets(String sep) throws IOException {
            StringBuffer result = new StringBuffer();
            char ce = (char) io.read();
            while (ce != -1 && sep.indexOf(ce) == -1) {
                result.append((char) ce);
                ce = (char) io.read();
            }
            line++;
            return getRuntime().newString(result.append(sep).toString());
        }

        public IRubyObject gets(IRubyObject[] args) throws IOException {
            IRubyObject result = internalGets(args);
            if (!result.isNil()) {
                getRuntime().getCurrentContext().setLastline(result);
            }
            return result;
        }

        private final static int BUFF_SIZE = 4096;
        public IRubyObject read(IRubyObject[] args) throws IOException {
            if (args.length == 0 || args[0].isNil()) {
                ByteList val = new ByteList(10);
                byte[] buffer = new byte[BUFF_SIZE];
                int read = io.read(buffer);
                while (read != -1) {
                    val.append(buffer,0,read);
                    read = io.read(buffer);
                }
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
            	return RubyString.newString(getRuntime(),new ByteList(buffer,0,len-toRead,false));
            }
                
            return getRuntime().newString("");
        }

        public IRubyObject set_lineno(RubyNumeric lineArg) {
            line = RubyNumeric.fix2int(lineArg);
            return lineArg;
        }

        public IRubyObject pos() {
            return RubyFixnum.zero(getRuntime());
        }
        
        public IRubyObject readchar() throws IOException {
            int value = io.read();
            if (value == -1) {
                throw getRuntime().newEOFError();
            }
            return getRuntime().newFixnum(value);
        }

        public IRubyObject getc() throws IOException {
            int value = io.read();
            return value == -1 ? getRuntime().getNil() : getRuntime().newFixnum(value);
        }

        private boolean isEof() throws IOException {
            return ((GZIPInputStream)io).available() != 1;
        }

        public IRubyObject close() throws IOException {
            if (!closed) {
                io.close();
            }
            this.closed = true;
            return getRuntime().getNil();
        }
        
        public IRubyObject eof() throws IOException {
            return isEof() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject eof_p() throws IOException {
            return eof();
        }

        public IRubyObject unused() {
            return getRuntime().getNil();
        }

        public IRubyObject tell() {
            return getRuntime().getNil();
        }

        public IRubyObject each(IRubyObject[] args, Block block) throws IOException {
            String sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
            
            if (args.length > 0 && !args[0].isNil()) {
                sep = args[0].toString();
            }

            ThreadContext context = getRuntime().getCurrentContext();
            while (!isEof()) {
                context.yield(internalSepGets(sep), block);
            }
            
            return getRuntime().getNil();
        }
    
        public IRubyObject ungetc(RubyNumeric arg) {
            return getRuntime().getNil();
        }

        public IRubyObject readlines(IRubyObject[] args) throws IOException {
            List array = new ArrayList();
            
            if (args.length != 0 && args[0].isNil()) {
                array.add(read(new IRubyObject[0]));
            } else {
                String seperator = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
                if (args.length > 0) {
                    seperator = args[0].toString();
                }
                while (!isEof()) {
                    array.add(internalSepGets(seperator));
                }
            }
            return getRuntime().newArray(array);
        }

        public IRubyObject each_byte(Block block) throws IOException {
            int value = io.read();

            ThreadContext context = getRuntime().getCurrentContext();
            while (value != -1) {
                context.yield(getRuntime().newFixnum(value), block);
                value = io.read();
            }
            
            return getRuntime().getNil();
        }
    }

    public static class RubyGzipWriter extends RubyGzipFile {
        protected static ObjectAllocator GZIPWRITER_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(IRuby runtime, RubyClass klass) {
                return new RubyGzipWriter(runtime, klass);
            }
        };
        
        private static RubyGzipWriter newGzipWriter(IRubyObject recv, IRubyObject[] args, Block block) {
            RubyClass klass = (RubyClass)recv;
            
            RubyGzipWriter result = (RubyGzipWriter)klass.allocate();
            result.callInit(args, block);
            return result;
        }

        public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) throws IOException {
            IRuby runtime = recv.getRuntime();
            IRubyObject level = runtime.getNil();
            IRubyObject strategy = runtime.getNil();

            if (args.length > 1) {
                level = args[1];
                if (args.length > 2) strategy = args[2];
            }

            IRubyObject proc = block.isGiven() ? runtime.newProc(false, block) : runtime.getNil();
            RubyGzipWriter io = newGzipWriter(
                    recv,
                    new IRubyObject[]{ runtime.getClass("File").callMethod(
                            runtime.getCurrentContext(),
                            "open",
                            new IRubyObject[]{args[0],runtime.newString("wb")}),level,strategy},block);
            
            return RubyGzipFile.wrap(recv, io, proc, null);
        }

        public RubyGzipWriter(IRuby runtime, RubyClass type) {
            super(runtime, type);
        }

        private GZIPOutputStream io;
        public IRubyObject initialize2(IRubyObject[] args, Block unusedBlock) throws IOException {
            realIo = (RubyObject)args[0];
            this.io = new GZIPOutputStream(new IOOutputStream(args[0]));
            
            return this;
        }

        public IRubyObject close() throws IOException {
            if (!closed) {
                io.close();
            }
            this.closed = true;
            
            return getRuntime().getNil();
        }

        public IRubyObject append(IRubyObject p1) throws IOException {
            this.write(p1);
            return this;
        }

        public IRubyObject printf(IRubyObject[] args) throws IOException {
            write(RubyKernel.sprintf(this, args));
            return getRuntime().getNil();
        }

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

        public IRubyObject pos() {
            return getRuntime().getNil();
        }

        public IRubyObject set_orig_name(RubyString ignored) {
            return getRuntime().getNil();
        }

        public IRubyObject set_comment(RubyString ignored) {
            return getRuntime().getNil();
        }

        public IRubyObject putc(RubyNumeric p1) throws IOException {
            io.write(RubyNumeric.fix2int(p1));
            return p1;
        }
        
        public IRubyObject puts(IRubyObject[] args) throws IOException {
            RubyStringIO sio = (RubyStringIO)getRuntime().getClass("StringIO").newInstance(new IRubyObject[0], Block.NULL_BLOCK);
            sio.puts(args);
            write(sio.string());
            
            return getRuntime().getNil();
        }

        public IRubyObject finish() throws IOException {
            if (!finished) {
                io.finish();
            }
            finished = true;
            return realIo;
        }

        public IRubyObject flush(IRubyObject[] args) throws IOException {
            if (args.length == 0 || args[0].isNil() || RubyNumeric.fix2int(args[0]) != 0) { // Zlib::NO_FLUSH
                io.flush();
            }
            return getRuntime().getNil();
        }

        public IRubyObject set_mtime(IRubyObject ignored) {
            return getRuntime().getNil();
        }

        public IRubyObject tell() {
            return getRuntime().getNil();
        }

        public IRubyObject write(IRubyObject p1) throws IOException {
            byte[] bs = p1.convertToString().getBytes();
            io.write(bs);
            return getRuntime().newFixnum(bs.length);
        }
    }
}

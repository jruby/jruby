/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Dave Brosius <dbrosius@mebigfatguy.com>
 * Copyright (C) 2006 Peter K Chan <peter@oaktop.com>
 * Copyright (C) 2009 Aurelian Oancea <aurelian@locknet.ro>
 * Copyright (C) 2009 Vladimir Sizikov <vsizikov@gmail.com>
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


import java.util.ArrayList;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyException;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.exceptions.RaiseException;

import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ext.zlib.Zlib.*;

import org.jruby.util.Adler32Ext;
import org.jruby.util.ByteList;
import org.jruby.util.CRC32Ext;

import com.jcraft.jzlib.JZlib;

import static org.jruby.CompatVersion.*;

@JRubyModule(name="Zlib")
public class RubyZlib {
    // version
    public final static String ZLIB_VERSION = "1.2.3.3";
    public final static String VERSION = "0.6.0";

    
    /** Create the Zlib module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createZlibModule(Ruby runtime) {
        RubyModule mZlib = runtime.defineModule("Zlib");
        mZlib.defineAnnotatedMethods(RubyZlib.class);

        RubyClass cStandardError = runtime.getStandardError();
        RubyClass cZlibError = mZlib.defineClassUnder("Error", cStandardError, cStandardError.getAllocator());
        mZlib.defineClassUnder("StreamEnd", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("StreamError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("BufError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("NeedDict", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("MemError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("VersionError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("DataError", cZlibError, cZlibError.getAllocator());

        RubyClass cGzFile = mZlib.defineClassUnder("GzipFile", runtime.getObject(), RubyGzipFile.GZIPFILE_ALLOCATOR);
        cGzFile.defineAnnotatedMethods(RubyGzipFile.class);

        cGzFile.defineClassUnder("Error", cZlibError, cZlibError.getAllocator());
        RubyClass cGzError = cGzFile.defineClassUnder("Error", cZlibError, cZlibError.getAllocator());
        if (runtime.is1_9()) {
            cGzError.addReadAttribute(runtime.getCurrentContext(), "input");
        }
        cGzError.defineAnnotatedMethods(RubyGzipFile.Error.class);
        cGzFile.defineClassUnder("CRCError", cGzError, cGzError.getAllocator());
        cGzFile.defineClassUnder("NoFooter", cGzError, cGzError.getAllocator());
        cGzFile.defineClassUnder("LengthError", cGzError, cGzError.getAllocator());

        RubyClass cGzReader = mZlib.defineClassUnder("GzipReader", cGzFile, JZlibRubyGzipReader.GZIPREADER_ALLOCATOR);
        cGzReader.includeModule(runtime.getEnumerable());
        cGzReader.defineAnnotatedMethods(JZlibRubyGzipReader.class);

        RubyClass cGzWriter = mZlib.defineClassUnder("GzipWriter", cGzFile, JZlibRubyGzipWriter.GZIPWRITER_ALLOCATOR);
        cGzWriter.defineAnnotatedMethods(JZlibRubyGzipWriter.class);

        mZlib.defineConstant("ZLIB_VERSION", runtime.newString(ZLIB_VERSION));
        mZlib.defineConstant("VERSION", runtime.newString(VERSION));

        mZlib.defineConstant("BINARY", runtime.newFixnum(Z_BINARY));
        mZlib.defineConstant("ASCII", runtime.newFixnum(Z_ASCII));
        mZlib.defineConstant("UNKNOWN", runtime.newFixnum(Z_UNKNOWN));

        mZlib.defineConstant("DEF_MEM_LEVEL", runtime.newFixnum(8));
        mZlib.defineConstant("MAX_MEM_LEVEL", runtime.newFixnum(9));

        mZlib.defineConstant("OS_UNIX", runtime.newFixnum(OS_UNIX));
        mZlib.defineConstant("OS_UNKNOWN", runtime.newFixnum(OS_UNKNOWN));
        mZlib.defineConstant("OS_CODE", runtime.newFixnum(OS_CODE));
        mZlib.defineConstant("OS_ZSYSTEM", runtime.newFixnum(OS_ZSYSTEM));
        mZlib.defineConstant("OS_VMCMS", runtime.newFixnum(OS_VMCMS));
        mZlib.defineConstant("OS_VMS", runtime.newFixnum(OS_VMS));
        mZlib.defineConstant("OS_RISCOS", runtime.newFixnum(OS_RISCOS));
        mZlib.defineConstant("OS_MACOS", runtime.newFixnum(OS_MACOS));
        mZlib.defineConstant("OS_OS2", runtime.newFixnum(OS_OS2));
        mZlib.defineConstant("OS_AMIGA", runtime.newFixnum(OS_AMIGA));
        mZlib.defineConstant("OS_QDOS", runtime.newFixnum(OS_QDOS));
        mZlib.defineConstant("OS_WIN32", runtime.newFixnum(OS_WIN32));
        mZlib.defineConstant("OS_ATARI", runtime.newFixnum(OS_ATARI));
        mZlib.defineConstant("OS_MSDOS", runtime.newFixnum(OS_MSDOS));
        mZlib.defineConstant("OS_CPM", runtime.newFixnum(OS_CPM));
        mZlib.defineConstant("OS_TOPS20", runtime.newFixnum(OS_TOPS20));

        mZlib.defineConstant("DEFAULT_STRATEGY", runtime.newFixnum(JZlib.Z_DEFAULT_STRATEGY));
        mZlib.defineConstant("FILTERED", runtime.newFixnum(JZlib.Z_FILTERED));
        mZlib.defineConstant("HUFFMAN_ONLY", runtime.newFixnum(JZlib.Z_HUFFMAN_ONLY));

        mZlib.defineConstant("NO_FLUSH", runtime.newFixnum(JZlib.Z_NO_FLUSH));
        mZlib.defineConstant("SYNC_FLUSH", runtime.newFixnum(JZlib.Z_SYNC_FLUSH));
        mZlib.defineConstant("FULL_FLUSH", runtime.newFixnum(JZlib.Z_FULL_FLUSH));
        mZlib.defineConstant("FINISH", runtime.newFixnum(JZlib.Z_FINISH));

        mZlib.defineConstant("NO_COMPRESSION", runtime.newFixnum(JZlib.Z_NO_COMPRESSION));
        mZlib.defineConstant("BEST_SPEED", runtime.newFixnum(JZlib.Z_BEST_SPEED));
        mZlib.defineConstant("DEFAULT_COMPRESSION", runtime.newFixnum(JZlib.Z_DEFAULT_COMPRESSION));
        mZlib.defineConstant("BEST_COMPRESSION", runtime.newFixnum(JZlib.Z_BEST_COMPRESSION));

        mZlib.defineConstant("MAX_WBITS", runtime.newFixnum(JZlib.MAX_WBITS));

        // ZStream actually *isn't* allocatable
        RubyClass cZStream = mZlib.defineClassUnder("ZStream", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        cZStream.defineAnnotatedMethods(ZStream.class);
        cZStream.undefineMethod("new");

        RubyClass cInflate = mZlib.defineClassUnder("Inflate", cZStream, JZlibInflate.INFLATE_ALLOCATOR);
        cInflate.defineAnnotatedMethods(JZlibInflate.class);

        RubyClass cDeflate = mZlib.defineClassUnder("Deflate", cZStream, JZlibDeflate.DEFLATE_ALLOCATOR);
        cDeflate.defineAnnotatedMethods(JZlibDeflate.class);

        runtime.getKernel().callMethod(runtime.getCurrentContext(), "require", runtime.newString("stringio"));

        return mZlib;
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

    @JRubyMethod(name = "zlib_version", module = true, visibility = PRIVATE)
    public static IRubyObject zlib_version(IRubyObject recv) {
        RubyBasicObject res = (RubyBasicObject) ((RubyModule)recv).getConstant("ZLIB_VERSION");
        // MRI behavior, enforced by tests
        res.setTaint(true);
        return res;
    }

    @JRubyMethod(name = "crc32", optional = 2, module = true, visibility = PRIVATE)
    public static IRubyObject crc32(IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(recv.getRuntime(),args,0,2);
        long crc = 0;
        ByteList bytes = null;
        
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) crc = RubyNumeric.num2long(args[1]);

        CRC32Ext ext = new CRC32Ext((int)crc);
        if (bytes != null) {
            ext.update(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        }
        
        return recv.getRuntime().newFixnum(ext.getValue());
    }

    @JRubyMethod(name = "adler32", optional = 2, module = true, visibility = PRIVATE)
    public static IRubyObject adler32(IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(recv.getRuntime(),args,0,2);
        int adler = 1;
        ByteList bytes = null;
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) adler = RubyNumeric.fix2int(args[1]);

        Adler32Ext ext = new Adler32Ext(adler);
        if (bytes != null) {
            ext.update(bytes.getUnsafeBytes(), bytes.begin(), bytes.length()); // it's safe since adler.update doesn't modify the array
        }
        return recv.getRuntime().newFixnum(ext.getValue());
    }

    @JRubyMethod(compat = RUBY1_9)
    public static IRubyObject inflate(ThreadContext context, IRubyObject recv, IRubyObject string) {
        return JZlibInflate.s_inflate(context, recv, string);
    }

    @JRubyMethod(required = 1, optional = 1, compat = RUBY1_9)
    public static IRubyObject deflate(IRubyObject recv, IRubyObject[] args) {
        return JZlibDeflate.s_deflate(recv, args);
    }

    // TODO: com.jcraft.jzlib.CRC32 has this table...
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

    @JRubyMethod(name = "crc_table", module = true, visibility = PRIVATE)
    public static IRubyObject crc_table(IRubyObject recv) {
        List<IRubyObject> ll = new ArrayList<IRubyObject>(crctab.length);
        for(int i=0;i<crctab.length;i++) {
            ll.add(recv.getRuntime().newFixnum(crctab[i]));
        }
        return recv.getRuntime().newArray(ll);
    }

    @JRubyMethod(name = "crc32_combine", required = 3, module = true, visibility = PRIVATE)
    public static IRubyObject crc32_combine(IRubyObject recv,
                                            IRubyObject arg0,
                                            IRubyObject arg1,
                                            IRubyObject arg2) {
        long crc1 = RubyNumeric.num2long(arg0);
        long crc2 = RubyNumeric.num2long(arg1);
        long len2 = RubyNumeric.num2long(arg2);

        long crc3 = com.jcraft.jzlib.JZlib.crc32_combine(crc1, crc2, len2);
        return recv.getRuntime().newFixnum(crc3);
    }

    @JRubyMethod(name = "adler32_combine", required = 3, module = true, visibility = PRIVATE)
    public static IRubyObject adler32_combine(IRubyObject recv,
                                            IRubyObject arg0,
                                            IRubyObject arg1,
                                            IRubyObject arg2) {
        long adler1 = RubyNumeric.num2long(arg0);
        long adler2 = RubyNumeric.num2long(arg1);
        long len2 = RubyNumeric.num2long(arg2);

        long adler3 = com.jcraft.jzlib.JZlib.adler32_combine(adler1, adler2, len2);
        return recv.getRuntime().newFixnum(adler3);
    }

    static RaiseException newZlibError(Ruby runtime, String message) {
        return newZlibError(runtime, "Error", message);
    }

    static RaiseException newBufError(Ruby runtime, String message) {
        return newZlibError(runtime, "BufError", message);
    }

    static RaiseException newDictError(Ruby runtime, String message) {
        return newZlibError(runtime, "NeedDict", message);
    }

    static RaiseException newStreamError(Ruby runtime, String message) {
        return newZlibError(runtime, "StreamError", message);
    }

    static RaiseException newDataError(Ruby runtime, String message) {
        return newZlibError(runtime, "DataError", message);
    }

    static RaiseException newZlibError(Ruby runtime, String klass, String message) {
        RubyClass errorClass = runtime.getModule("Zlib").getClass(klass);
        return new RaiseException(RubyException.newException(runtime, errorClass, message), true);
    }

    static RaiseException newGzipFileError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "Error", message);
    }

    static RaiseException newCRCError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "CRCError", message);
    }

    static RaiseException newNoFooter(Ruby runtime, String message) {
        return newGzipFileError(runtime, "NoFooter", message);
    }

    static RaiseException newLengthError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "LengthError", message);
    }

    static RaiseException newGzipFileError(Ruby runtime, String klass, String message) {
        RubyClass errorClass = runtime.getModule("Zlib").getClass("GzipFile").getClass(klass);
        RubyException excn = RubyException.newException(runtime, errorClass, message);
        if (runtime.is1_9()) {
            // TODO: not yet supported. rewrite GzipReader/Writer with Inflate/Deflate?
            excn.setInstanceVariable("@input", runtime.getNil());
        }
        return new RaiseException(excn, true);
    }
    
    static int FIXNUMARG(IRubyObject obj, int ifnil) {
        if (obj.isNil()) return ifnil;
        
        return RubyNumeric.fix2int(obj);
    }
}

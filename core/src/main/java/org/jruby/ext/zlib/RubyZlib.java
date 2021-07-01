/***** BEGIN LICENSE BLOCK *****
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

import java.util.zip.CRC32;
import java.util.zip.Adler32;

import org.jruby.Ruby;
import org.jruby.RubyArray;
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

import org.jruby.util.ByteList;

import com.jcraft.jzlib.JZlib;

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
        RubyClass cZlibError = mZlib.defineOrGetClassUnder("Error", cStandardError, cStandardError.getAllocator());
        mZlib.defineOrGetClassUnder("StreamEnd", cZlibError, cZlibError.getAllocator());
        mZlib.defineOrGetClassUnder("StreamError", cZlibError, cZlibError.getAllocator());
        mZlib.defineOrGetClassUnder("BufError", cZlibError, cZlibError.getAllocator());
        mZlib.defineOrGetClassUnder("NeedDict", cZlibError, cZlibError.getAllocator());
        mZlib.defineOrGetClassUnder("MemError", cZlibError, cZlibError.getAllocator());
        mZlib.defineOrGetClassUnder("VersionError", cZlibError, cZlibError.getAllocator());
        mZlib.defineOrGetClassUnder("DataError", cZlibError, cZlibError.getAllocator());

        RubyClass cGzFile = mZlib.defineOrGetClassUnder("GzipFile", runtime.getObject(), RubyGzipFile::new);
        cGzFile.defineAnnotatedMethods(RubyGzipFile.class);

        cGzFile.defineOrGetClassUnder("Error", cZlibError, cZlibError.getAllocator());
        RubyClass cGzError = cGzFile.defineOrGetClassUnder("Error", cZlibError, cZlibError.getAllocator());
        cGzError.addReadAttribute(runtime.getCurrentContext(), "input");
        cGzFile.defineOrGetClassUnder("CRCError", cGzError, cGzError.getAllocator());
        cGzFile.defineOrGetClassUnder("NoFooter", cGzError, cGzError.getAllocator());
        cGzFile.defineOrGetClassUnder("LengthError", cGzError, cGzError.getAllocator());

        RubyClass cGzReader = mZlib.defineOrGetClassUnder("GzipReader", cGzFile, JZlibRubyGzipReader::new);
        cGzReader.includeModule(runtime.getEnumerable());
        cGzReader.defineAnnotatedMethods(JZlibRubyGzipReader.class);

        RubyClass cGzWriter = mZlib.defineOrGetClassUnder("GzipWriter", cGzFile, JZlibRubyGzipWriter::new);
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
        RubyClass cZStream = mZlib.defineOrGetClassUnder("ZStream", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        cZStream.defineAnnotatedMethods(ZStream.class);
        cZStream.undefineMethod("new");

        RubyClass cInflate = mZlib.defineOrGetClassUnder("Inflate", cZStream, JZlibInflate::new);
        cInflate.defineAnnotatedMethods(JZlibInflate.class);

        RubyClass cDeflate = mZlib.defineOrGetClassUnder("Deflate", cZStream, JZlibDeflate::new);
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
        long start = 0;
        ByteList bytes = null;
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) start = RubyNumeric.num2long(args[1]);
        start &= 0xFFFFFFFFL;

        final boolean slowPath = start != 0;
        final int bytesLength = bytes == null ? 0 : bytes.length();
        long result = 0;
        if (bytes != null) {
            CRC32 checksum = new CRC32();
            checksum.update(bytes.getUnsafeBytes(), bytes.begin(), bytesLength);
            result = checksum.getValue();
        }
        if (slowPath) {
            result = JZlib.crc32_combine(start, result, bytesLength);
        }
        return recv.getRuntime().newFixnum(result);
    }

    @JRubyMethod(name = "adler32", optional = 2, module = true, visibility = PRIVATE)
    public static IRubyObject adler32(IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(recv.getRuntime(),args,0,2);
        int start = 1;
        ByteList bytes = null;
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) start = (int)RubyNumeric.num2long(args[1]);

        Adler32 checksum = new Adler32();
        if (bytes != null) {
            checksum.update(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        }
        long result = checksum.getValue();
        if (start != 1) {
            result = JZlib.adler32_combine(start, result, bytes.length());
        }
        return recv.getRuntime().newFixnum(result);
    }

    @JRubyMethod(module = true)
    public static IRubyObject inflate(ThreadContext context, IRubyObject recv, IRubyObject string) {
        return JZlibInflate.s_inflate(context, recv, string);
    }

    @JRubyMethod(required = 1, optional = 1, module = true)
    public static IRubyObject deflate(IRubyObject recv, IRubyObject[] args) {
        return JZlibDeflate.s_deflate(recv, args);
    }

    @JRubyMethod(name = "crc_table", module = true, visibility = PRIVATE)
    public static IRubyObject crc_table(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        int[] table = com.jcraft.jzlib.CRC32.getCRC32Table();
        RubyArray array = runtime.newArray(table.length);
        for (int i = 0; i < table.length; i++) {
            array.append(runtime.newFixnum(table[i] & 0xffffffffL));
        }
        return array;
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
        return RaiseException.from(runtime, errorClass, message);
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
        // TODO: not yet supported. rewrite GzipReader/Writer with Inflate/Deflate?
        excn.setInstanceVariable("@input", runtime.getNil());
        return excn.toThrowable();
    }

    static int FIXNUMARG(IRubyObject obj, int ifnil) {
        if (obj.isNil()) return ifnil;

        return RubyNumeric.fix2int(obj);
    }
}

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

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyException;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.api.Access;
import org.jruby.api.Create;
import org.jruby.exceptions.RaiseException;

import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Access.enumerableModule;
import static org.jruby.api.Access.kernelModule;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Access.standardErrorClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineModule;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
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
    public static RubyModule createZlibModule(ThreadContext context) {
        var Object = objectClass(context);
        var StandardError = standardErrorClass(context);
        var Zlib = defineModule(context, "Zlib").
                defineMethods(context, RubyZlib.class);
        var ZlibError = Zlib.defineClassUnder(context, "Error", StandardError, StandardError.getAllocator());
        var errorAllocator = ZlibError.getAllocator();
        Zlib.defineClassUnder(context, "StreamEnd", ZlibError, errorAllocator);
        Zlib.defineClassUnder(context, "StreamError", ZlibError, errorAllocator);
        Zlib.defineClassUnder(context, "BufError", ZlibError, errorAllocator);
        Zlib.defineClassUnder(context, "NeedDict", ZlibError, errorAllocator);
        Zlib.defineClassUnder(context, "MemError", ZlibError, errorAllocator);
        Zlib.defineClassUnder(context, "VersionError", ZlibError, errorAllocator);
        Zlib.defineClassUnder(context, "DataError", ZlibError, errorAllocator);

        RubyClass GzipFile = Zlib.defineClassUnder(context, "GzipFile", Object, RubyGzipFile::new).
                defineMethods(context, RubyGzipFile.class);

        GzipFile.defineClassUnder(context, "Error", ZlibError, errorAllocator);
        var GZipFileError = GzipFile.defineClassUnder(context, "Error", ZlibError, errorAllocator);
        var fileErrorAllocator = ZlibError.getAllocator();
        GZipFileError.addReadAttribute(context, "input");
        GzipFile.defineClassUnder(context, "CRCError", GZipFileError, fileErrorAllocator);
        GzipFile.defineClassUnder(context, "NoFooter", GZipFileError, fileErrorAllocator);
        GzipFile.defineClassUnder(context, "LengthError", GZipFileError, fileErrorAllocator);

        Zlib.defineClassUnder(context, "GzipReader", GzipFile, JZlibRubyGzipReader::new).
                include(context, enumerableModule(context)).
                defineMethods(context, JZlibRubyGzipReader.class);

        Zlib.defineClassUnder(context, "GzipWriter", GzipFile, JZlibRubyGzipWriter::new).
                defineMethods(context, JZlibRubyGzipWriter.class);

        Zlib.defineConstant(context, "ZLIB_VERSION", newString(context, ZLIB_VERSION)).
                defineConstant(context, "VERSION", newString(context, VERSION)).

                defineConstant(context, "BINARY", asFixnum(context, Z_BINARY)).
                defineConstant(context, "ASCII", asFixnum(context, Z_ASCII)).
                defineConstant(context, "UNKNOWN", asFixnum(context, Z_UNKNOWN)).

                defineConstant(context, "DEF_MEM_LEVEL", asFixnum(context, 8)).
                defineConstant(context, "MAX_MEM_LEVEL", asFixnum(context, 9)).

                defineConstant(context, "OS_UNIX", asFixnum(context, OS_UNIX)).
                defineConstant(context, "OS_UNKNOWN", asFixnum(context, OS_UNKNOWN)).
                defineConstant(context, "OS_CODE", asFixnum(context, OS_CODE)).
                defineConstant(context, "OS_ZSYSTEM", asFixnum(context, OS_ZSYSTEM)).
                defineConstant(context, "OS_VMCMS", asFixnum(context, OS_VMCMS)).
                defineConstant(context, "OS_VMS", asFixnum(context, OS_VMS)).
                defineConstant(context, "OS_RISCOS", asFixnum(context, OS_RISCOS)).
                defineConstant(context, "OS_MACOS", asFixnum(context, OS_MACOS)).
                defineConstant(context, "OS_OS2", asFixnum(context, OS_OS2)).
                defineConstant(context, "OS_AMIGA", asFixnum(context, OS_AMIGA)).
                defineConstant(context, "OS_QDOS", asFixnum(context, OS_QDOS)).
                defineConstant(context, "OS_WIN32", asFixnum(context, OS_WIN32)).
                defineConstant(context, "OS_ATARI", asFixnum(context, OS_ATARI)).
                defineConstant(context, "OS_MSDOS", asFixnum(context, OS_MSDOS)).
                defineConstant(context, "OS_CPM", asFixnum(context, OS_CPM)).
                defineConstant(context, "OS_TOPS20", asFixnum(context, OS_TOPS20)).

                defineConstant(context, "DEFAULT_STRATEGY", asFixnum(context, JZlib.Z_DEFAULT_STRATEGY)).
                defineConstant(context, "FILTERED", asFixnum(context, JZlib.Z_FILTERED)).
                defineConstant(context, "HUFFMAN_ONLY", asFixnum(context, JZlib.Z_HUFFMAN_ONLY)).

                defineConstant(context, "NO_FLUSH", asFixnum(context, JZlib.Z_NO_FLUSH)).
                defineConstant(context, "SYNC_FLUSH", asFixnum(context, JZlib.Z_SYNC_FLUSH)).
                defineConstant(context, "FULL_FLUSH", asFixnum(context, JZlib.Z_FULL_FLUSH)).
                defineConstant(context, "FINISH", asFixnum(context, JZlib.Z_FINISH)).

                defineConstant(context, "NO_COMPRESSION", asFixnum(context, JZlib.Z_NO_COMPRESSION)).
                defineConstant(context, "BEST_SPEED", asFixnum(context, JZlib.Z_BEST_SPEED)).
                defineConstant(context, "DEFAULT_COMPRESSION", asFixnum(context, JZlib.Z_DEFAULT_COMPRESSION)).
                defineConstant(context, "BEST_COMPRESSION", asFixnum(context, JZlib.Z_BEST_COMPRESSION)).

                defineConstant(context, "MAX_WBITS", asFixnum(context, JZlib.MAX_WBITS));

        // ZStream actually *isn't* allocatable
        RubyClass ZStream = Zlib.defineClassUnder(context, "ZStream", Object, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, ZStream.class).
                undefMethods(context, "new");
        Zlib.defineClassUnder(context, "Inflate", ZStream, JZlibInflate::new).
                defineMethods(context, JZlibInflate.class);
        Zlib.defineClassUnder(context, "Deflate", ZStream, JZlibDeflate::new).
                defineMethods(context, JZlibDeflate.class);

        kernelModule(context).callMethod(context, "require", newString(context, "stringio"));

        return Zlib;
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

    @Deprecated(since = "10.0")
    public static IRubyObject zlib_version(IRubyObject recv) {
        return zlib_version(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(name = "zlib_version", module = true, visibility = PRIVATE)
    public static IRubyObject zlib_version(ThreadContext context, IRubyObject recv) {
        return ((RubyModule) recv).getConstant(context, "ZLIB_VERSION");
    }

    @JRubyMethod(name = "crc32", optional = 2, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject crc32(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(context, args, 0, 2);
        ByteList bytes = !args[0].isNil() ? args[0].convertToString().getByteList() : null;
        long start = !args[1].isNil() ? toLong(context, args[1]) : 0;
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
        return asFixnum(context, result);
    }

    @Deprecated
    public static IRubyObject crc32(IRubyObject recv, IRubyObject[] args) {
        return crc32(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "adler32", optional = 2, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject adler32(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(context, args, 0, 2);
        ByteList bytes = !args[0].isNil() ? args[0].convertToString().getByteList() : null;
        int start = !args[1].isNil() ? (int) toLong(context, args[1]) : 1;

        Adler32 checksum = new Adler32();
        if (bytes != null) checksum.update(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());

        long result = checksum.getValue();
        if (start != 1) result = JZlib.adler32_combine(start, result, bytes.length());

        return asFixnum(context, result);
    }

    @Deprecated
    public static IRubyObject adler32(IRubyObject recv, IRubyObject[] args) {
        return adler32(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(module = true)
    public static IRubyObject inflate(ThreadContext context, IRubyObject recv, IRubyObject string) {
        return JZlibInflate.s_inflate(context, recv, string);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject deflate(IRubyObject recv, IRubyObject[] args) {
        return deflate(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false, module = true)
    public static IRubyObject deflate(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return JZlibDeflate.s_deflate(context, recv, args);
    }

    @JRubyMethod(name = "crc_table", module = true, visibility = PRIVATE)
    public static IRubyObject crc_table(ThreadContext context, IRubyObject recv) {
        int[] table = com.jcraft.jzlib.CRC32.getCRC32Table();
        var result = Create.allocArray(context, table.length);
        for (int j: table) result.append(context, asFixnum(context, j & 0xffffffffL));
        return result;
    }

    @Deprecated
    public static IRubyObject crc_table(IRubyObject recv) {
        return crc_table(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(name = "crc32_combine", module = true, visibility = PRIVATE)
    public static IRubyObject crc32_combine(ThreadContext context, IRubyObject recv,
                                            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        long crc1 = toLong(context, arg0);
        long crc2 = toLong(context, arg1);
        long len2 = toLong(context, arg2);

        return asFixnum(context, com.jcraft.jzlib.JZlib.crc32_combine(crc1, crc2, len2));
    }

    @Deprecated
    public static IRubyObject crc32_combine(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return crc32_combine(((RubyBasicObject) recv).getCurrentContext(), recv, arg0, arg1, arg2);
    }

    @JRubyMethod(name = "adler32_combine", module = true, visibility = PRIVATE)
    public static IRubyObject adler32_combine(ThreadContext context, IRubyObject recv,
                                              IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        long adler1 = toLong(context, arg0);
        long adler2 = toLong(context, arg1);
        long len2 = toLong(context, arg2);

        return asFixnum(context, com.jcraft.jzlib.JZlib.adler32_combine(adler1, adler2, len2));
    }

    @Deprecated
    public static IRubyObject adler32_combine(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return adler32_combine(((RubyBasicObject) recv).getCurrentContext(), recv, arg0, arg1, arg2);
    }

    static RaiseException newZlibError(ThreadContext context, String message) {
        return newZlibError(context, "Error", message);
    }

    static RaiseException newBufError(ThreadContext context, String message) {
        return newZlibError(context, "BufError", message);
    }

    static RaiseException newDictError(ThreadContext context, String message) {
        return newZlibError(context, "NeedDict", message);
    }

    static RaiseException newStreamError(ThreadContext context, String message) {
        return newZlibError(context, "StreamError", message);
    }

    static RaiseException newDataError(ThreadContext context, String message) {
        return newZlibError(context, "DataError", message);
    }

    static RaiseException newZlibError(ThreadContext context, String klass, String message) {
        return RaiseException.from(context.runtime, Access.getClass(context, "Zlib", klass), message);
    }

    static RaiseException newGzipFileError(ThreadContext context, String message) {
        return newGzipFileError(context, "Error", message);
    }

    static RaiseException newCRCError(ThreadContext context, String message) {
        return newGzipFileError(context, "CRCError", message);
    }

    static RaiseException newNoFooter(ThreadContext context, String message) {
        return newGzipFileError(context, "NoFooter", message);
    }

    static RaiseException newLengthError(ThreadContext context, String message) {
        return newGzipFileError(context, "LengthError", message);
    }

    static RaiseException newGzipFileError(ThreadContext context, String klass, String message) {
        RubyClass errorClass = Access.getClass(context, "Zlib", "GzipFile", klass);
        RubyException excn = RubyException.newException(context.runtime, errorClass, message);
        // TODO: not yet supported. rewrite GzipReader/Writer with Inflate/Deflate?
        excn.setInstanceVariable("@input", context.nil);
        return excn.toThrowable();
    }

    static int FIXNUMARG(ThreadContext context, IRubyObject obj, int ifnil) {
        return obj.isNil() ? ifnil : toInt(context, obj);
    }
}

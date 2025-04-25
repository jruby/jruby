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

import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.GZIPInputStream;
import com.jcraft.jzlib.Inflater;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyEnumerator;
import org.jruby.RubyException;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.IOInputStream;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.PosixShim;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.jruby.RubyIO.PARAGRAPH_SEPARATOR;
import static org.jruby.api.Access.fileClass;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.castAsString;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.runtime.Visibility.PRIVATE;

@JRubyClass(name = "Zlib::GzipReader", parent = "Zlib::GzipFile", include = "Enumerable")
public class JZlibRubyGzipReader extends RubyGzipFile {
    @JRubyClass(name = "Zlib::GzipReader::Error", parent = "Zlib::GzipReader")
    public static class Error {}

    @JRubyMethod(name = "new", rest = true, meta = true, keywords = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        JZlibRubyGzipReader result = newInstance(context, (RubyClass) recv, args);

        return RubyGzipFile.wrapBlock(context, result, block);
    }

    @Deprecated(since = "10.0")
    public static JZlibRubyGzipReader newInstance(IRubyObject recv, IRubyObject[] args) {
        return newInstance(((RubyBasicObject) recv).getCurrentContext(), (RubyClass) recv, args);
    }

    public static JZlibRubyGzipReader newInstance(ThreadContext context, RubyClass klass, IRubyObject[] args) {
        JZlibRubyGzipReader result = (JZlibRubyGzipReader) klass.allocate(context);

        result.callInit(args, Block.NULL_BLOCK);

        return result;
    }

    @JRubyMethod(name = "open", required = 1, optional = 1, checkArity = false, meta = true)
    public static IRubyObject open(final ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 1, 2);

        args[0] = Helpers.invoke(context, fileClass(context), "open", args[0], newString(context, "rb"));

        JZlibRubyGzipReader gzio = newInstance(context, (RubyClass) recv, args);

        return RubyGzipFile.wrapBlock(context, gzio, block);
    }

    public JZlibRubyGzipReader(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject stream) {
        realIo = stream;

        try {
            // don't close realIO
            ioInputStream = new IOInputStream(realIo);
            io = new GZIPInputStream(ioInputStream, 512, false);

            // JRUBY-4502
            // CRuby expects to parse gzip header in 'new'.
            io.readHeader();

        } catch (IOException e) {
            RaiseException re = RubyZlib.newGzipFileError(context, "not in gzip format");

            byte[] input = io.getAvailIn();
            if (input != null && input.length > 0) {
                RubyException rubye = re.getException();
                rubye.setInstanceVariable("@input", newString(context, new ByteList(input, 0, input.length)));
            }

            throw re;
        }

        position = 0;
        line = 0;
        bufferedStream = new PushbackInputStream(new BufferedInputStream(io), 512);
        mtime = org.jruby.RubyTime.newTime(context.runtime, io.getModifiedTime() * 1000);

        return this;
    }

    @JRubyMethod(name = "initialize", required = 1, optional = 1, checkArity = false, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);
        IRubyObject obj = initialize(context, args[0]);
        IRubyObject opt = context.nil;
        
        if (argc == 2) {
            opt = args[1];
            if (TypeConverter.checkHashType(context.runtime, opt).isNil()) throw argumentError(context, 2, 1);
        }
        
        ecopts(context, opt);

        return obj;
    }

    /**
     * Get position within this stream including that has been read by users
     * calling read + what jzlib may have speculatively read in because of
     * buffering.
     *
     * @return number of bytes
     */
    private long internalPosition() {
        Inflater inflater = io.getInflater();

        return inflater.getTotalIn() + inflater.getAvailIn();
    }

    @JRubyMethod
    public IRubyObject rewind(ThreadContext context) {
        // should invoke seek on realIo...
        realIo.callMethod(context, "seek",
                new IRubyObject[]{asFixnum(context, -internalPosition()), asFixnum(context, PosixShim.SEEK_CUR)});

        // ... and then reinitialize
        initialize(context, realIo);

        return context.nil;
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno(ThreadContext context) {
        return asFixnum(context, line);
    }

    @Deprecated
    public IRubyObject lineno() {
        return lineno(getCurrentContext());
    }

    @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
    public IRubyObject readline(ThreadContext context) {
        IRubyObject dst = gets(context, IRubyObject.NULL_ARRAY);

        if (dst.isNil()) throw context.runtime.newEOFError();

        return dst;
    }

    private IRubyObject internalGets(ThreadContext context, IRubyObject[] args) throws IOException {
        ByteList sep = ((RubyString) globalVariables(context).get("$/")).getByteList();
        int limit = -1;

        switch (args.length) {
            case 0:
                break;
            case 1:
                if (args[0].isNil()) return readAll(context);

                IRubyObject tmp = args[0].checkStringType();
                if (tmp.isNil()) {
                    limit = toInt(context, args[0]);
                } else {
                    sep = tmp.convertToString().getByteList();
                }
                break;
            case 2:
            default:
                limit = toInt(context, args[1]);
                if (args[0].isNil()) return readAll(context, limit);

                sep = args[0].convertToString().getByteList();
                break;
        }

        return internalSepGets(context, sep, limit);
    }

    private IRubyObject internalSepGets(ThreadContext context, ByteList sep) throws IOException {
        return internalSepGets(context, sep, -1);
    }

    private ByteList newReadByteList() {
        ByteList byteList = new ByteList();

        return byteList;
    }

    private ByteList newReadByteList(int size) {
        ByteList byteList = new ByteList(size);

        return byteList;
    }

    private IRubyObject internalSepGets(ThreadContext context, ByteList sep, int limit) throws IOException {
        ByteList result = newReadByteList();
        boolean stripNewlines = false;

        if (sep.getRealSize() == 0) {
            sep = PARAGRAPH_SEPARATOR;
            stripNewlines = true;
        }

        if (stripNewlines) skipNewlines();

        int ce = -1;
        
        while (limit <= 0 || result.length() < limit) {
            int sepOffset = result.length() - sep.getRealSize();
            if (sepOffset >= 0 && result.startsWith(sep, sepOffset)) break;

            ce = bufferedStream.read();

            if (ce == -1) break;

            result.append(ce);
        }
        
        fixBrokenTrailingCharacter(context, result);

        if (stripNewlines) skipNewlines();

        // io.available() only returns 0 after EOF is encountered
        // so we need to differentiate between the empty string and EOF
        if (0 == result.length() && -1 == ce) return context.nil;

        line++;
        position += result.length();

        return newStr(context, result);
    }

    private static final int NEWLINE = '\n';

    private void skipNewlines() throws IOException {
        while (true) {
            int b = bufferedStream.read();
            if (b == -1) break;

            if (b != NEWLINE) {
                bufferedStream.unread(b);
                break;
            }
        }
    }

    @JRubyMethod(name = "gets", optional = 2, checkArity = false, writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        try {
            IRubyObject result = internalGets(context, args);

            if (!result.isNil()) context.setLastLine(result);
            
            return result;
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }
    private final static int BUFF_SIZE = 4096;

    @JRubyMethod(name = "read", optional = 1, checkArity = false)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        try {
            if (argc == 0 || args[0].isNil()) return readAll(context);

            int len = toInt(context, args[0]);
            if (len < 0) throw argumentError(context, "negative length " + len + " given");
            if (len > 0) { // rb_gzfile_read
                ByteList buf = readSize(len);
                return buf == null ? context.nil : newString(context, buf);
            }

            return RubyString.newEmptyBinaryString(context.runtime);
        } catch (IOException ioe) {
            String m = ioe.getMessage();

            if (m.startsWith("Unexpected end of ZLIB input stream")) {
                throw RubyZlib.newGzipFileError(context, ioe.getMessage());
            } else if (m.startsWith("footer is not found")) {
                throw RubyZlib.newNoFooter(context, "footer is not found");
            } else if (m.startsWith("incorrect data check")) {
                throw RubyZlib.newCRCError(context, "invalid compressed data -- crc error");
            } else if (m.startsWith("incorrect length check")) {
                throw RubyZlib.newLengthError(context, "invalid compressed data -- length error");
            } else {
                throw RubyZlib.newDataError(context, ioe.getMessage());
            }
        }
    }

    @Deprecated
    public IRubyObject readpartial(IRubyObject[] args) {
        return readpartial(getCurrentContext(), args);
    }

    @JRubyMethod(name = "readpartial", required = 1, optional = 1, checkArity = false)
    public IRubyObject readpartial(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);

        try {
            int len = toInt(context, args[0]);
            if (len < 0) throw argumentError(context, "negative length " + len + " given");

            return argc > 1 && !args[1].isNil() ?
                    readPartial(context, len, castAsString(context, args[1])) :
                    readPartial(context, len, null);
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    private IRubyObject readPartial(ThreadContext context, int len, RubyString outbuf) throws IOException {
        ByteList val = newReadByteList(10);
        byte[] buffer = new byte[len];
        int read = bufferedStream.read(buffer, 0, len);

        if (read == -1) return context.nil;

        val.append(buffer, 0, read);
        this.position += val.length();

        if (outbuf != null) outbuf.view(val);

        return newStr(context, val);
    }

    private RubyString readAll(ThreadContext context) throws IOException {
        return readAll(context, -1);
    }

    private RubyString readAll(ThreadContext context, int limit) throws IOException {
        ByteList val = newReadByteList(10);
        int rest = limit == -1 ? BUFF_SIZE : limit;
        byte[] buffer = new byte[rest];
        
        while (rest > 0) {
            int read = bufferedStream.read(buffer, 0, rest);
            if (read == -1) break;

            val.append(buffer, 0, read);
            if (limit != -1) rest -= read;
        }
        
        fixBrokenTrailingCharacter(context, val);
        
        this.position += val.length();
        return newStr(context, val);
    }


    // FIXME: I think offset == 0 should return empty bytelist and not null
    // mri: gzfile_read
    // This returns a bucket of bytes trying to read length bytes.
    private ByteList readSize(int length) throws IOException {
        byte[] buffer = new byte[length];
        int toRead = length;
        int offset = 0;

        while (toRead > 0) {
            int read = bufferedStream.read(buffer, offset, toRead);
            
            if (read == -1) {
                if (offset == 0) return null; // we're at EOF right away
                break;
            }
            
            toRead -= read;
            offset += read;
        } // hmm...

        this.position += length - toRead;

        return new ByteList(buffer, 0, length - toRead, false);
    }

    @Deprecated(since = "10.0")
    public IRubyObject set_lineno(IRubyObject lineArg) {
        return set_lineno(getCurrentContext(), lineArg);
    }

    @JRubyMethod(name = "lineno=")
    public IRubyObject set_lineno(ThreadContext context, IRubyObject lineArg) {
        line = toInt(context, lineArg);

        return lineArg;
    }

    @Deprecated(since = "10.0")
    public IRubyObject pos() {
        return pos(getCurrentContext());
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos(ThreadContext context) {
        return asFixnum(context, position);
    }

    @Deprecated
    public IRubyObject readchar() {
        return readchar(getCurrentContext());
    }

    @JRubyMethod(name = "readchar")
    public IRubyObject readchar(ThreadContext context) {
        try {
            int value = bufferedStream.read();
            if (value == -1) throw context.runtime.newEOFError();

            position++;
            
            return asFixnum(context, value);
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    @Deprecated
    public IRubyObject getc() {
        return getbyte(getCurrentContext());
    }

    @JRubyMethod(name = "getbyte")
    public IRubyObject getbyte(ThreadContext context) {
        try {
            int value = bufferedStream.read();
            if (value == -1) return context.nil;

            position++;

            return asFixnum(context, value);
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    @Deprecated
    public IRubyObject getbyte() {
        return getbyte(getCurrentContext());
    }

    @Deprecated
    public IRubyObject readbyte() {
        return readbyte(getCurrentContext());
    }

    @JRubyMethod(name = "readbyte")
    public IRubyObject readbyte(ThreadContext context) {
        IRubyObject dst = getbyte(context);
        if (dst.isNil()) throw context.runtime.newEOFError();
        return dst;
    }

    @JRubyMethod(name = "getc")
    public IRubyObject getc(ThreadContext context) {
        try {
            int value = bufferedStream.read();
            if (value == -1) return context.nil;

            position++;
            // TODO: must handle encoding. Move encoding handling methods to util class from RubyIO and use it.
            // TODO: StringIO needs a love, too.
            return newString(context, String.valueOf((char) (value & 0xFF)));
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    private boolean isEof() throws IOException {
        if (bufferedStream.available() == 0) return true;

        // Java's GZIPInputStream behavior is such
        // that it says that more bytes available even
        // when we are right before the EOF, but not yet
        // encountered the actual EOF during the reading.
        // So, we compensate for that to provide MRI
        // compatible behavior.
        byte[] bytes = new byte[16];
        int read = bufferedStream.read(bytes, 0, bytes.length);

        // We are already at EOF.
        if (read == -1) return true;

        bufferedStream.unread(bytes, 0, read);

        return bufferedStream.available() == 0;
    }

    @Override
    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        if (!closed) {
            try {
                /**
                 * We call internal IO#close directly, not via
                 * IOInputStream#close. IOInputStream#close directly invoke
                 * IO.getOutputStream().close() for IO object instead of just
                 * calling IO#cloase of Ruby. It causes EBADF at
                 * OpenFile#finalize.
                 *
                 * CAUTION: bufferedStream.close() will not cause
                 * 'IO.getOutputStream().close()', becase 'false' has been given
                 * as third augument in constructing GZIPInputStream.
                 *
                 * TODO: implement this without IOInputStream? Not so hard.
                 */
                bufferedStream.close();
                if (realIo.respondsTo("close")) realIo.callMethod(context, "close");
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }
        this.closed = true;
        return realIo;
    }

    @Deprecated(since = "10.0")
    public IRubyObject eof() {
        return eof(getCurrentContext());
    }

    @JRubyMethod(name = "eof")
    public IRubyObject eof(ThreadContext context) {
        try {
            return isEof() ? context.tru : context.fals;
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    @Deprecated(since = "10.0")
    public IRubyObject eof_p() {
        return eof_p(getCurrentContext());
    }

    @JRubyMethod(name = "eof?")
    public IRubyObject eof_p(ThreadContext context) {
        return eof(context);
    }

    @Deprecated(since = "10.0")
    public IRubyObject unused() {
        return unused(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject unused(ThreadContext context) {
        byte[] tmp = io.getAvailIn();
        
        return tmp == null ? context.nil : newString(context, new ByteList(tmp));
    }

    @Override
    @JRubyMethod
    public IRubyObject crc(ThreadContext context) {
        long crc = 0;

        try {
            crc = io.getCRC();
        } catch (GZIPException e) {
        }

        return asFixnum(context, crc);
    }

    @Override
    @JRubyMethod
    public IRubyObject os_code(ThreadContext context) {
        int os = io.getOS();
        
        if (os == 255) os = (byte) 0x0b; // NTFS filesystem (NT), because CRuby's test_zlib expect it.
        
        return asFixnum(context, os & 0xff);
    }

    @Override
    @JRubyMethod
    public IRubyObject orig_name(ThreadContext context) {
        String name = io.getName();

        nullFreeOrigName = newString(context, name);

        return super.orig_name(context);
    }

    @Override
    @JRubyMethod
    public IRubyObject comment(ThreadContext context) {
        String comment = io.getComment();

        nullFreeComment = newString(context, comment);

        return super.comment(context);
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each", args);

        ByteList sep = ((RubyString) globalVariables(context).get("$/")).getByteList();

        if (argc > 0 && !args[0].isNil()) {
            sep = args[0].convertToString().getByteList();
        }

        try {
            for (IRubyObject result = internalSepGets(context, sep); !result.isNil(); result = internalSepGets(context, sep)) {
                block.yield(context, result);
            }
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }

        return context.nil;
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each_line", args);

        return each(context, args, block);
    }

    @JRubyMethod
    public IRubyObject ungetc(ThreadContext context, IRubyObject cArg) {
        if (cArg.isNil()) return context.nil;

        RubyString c = cArg instanceof RubyInteger cint ?
                EncodingUtils.encUintChr(context, cint.asInt(context), getReadEncoding(context)) :
                cArg.convertToString();

        try {
            byte[] bytes = c.getBytes();
            bufferedStream.unread(bytes);
            position -= bytes.length;
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }

        return context.nil;
    }

    @Deprecated(since = "10.0")
    public IRubyObject ungetbyte(IRubyObject b) {
        return ungetbyte(getCurrentContext(), b);
    }

    @JRubyMethod
    public IRubyObject ungetbyte(ThreadContext context, IRubyObject b) {
        if (b.isNil()) return b;

        try {
            bufferedStream.unread(toInt(context, b));
            position--;
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }

        return context.nil;
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public IRubyObject readlines(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        List<IRubyObject> array = new ArrayList<>();

        if (argc != 0 && args[0].isNil()) {
            array.add(read(context, IRubyObject.NULL_ARRAY));
        } else {
            ByteList sep = ((RubyString) globalVariables(context).get("$/")).getByteList();

            if (argc > 0) sep = args[0].convertToString().getByteList();

            try {
                for (IRubyObject result = internalSepGets(context, sep); !result.isNil(); result = internalSepGets(context, sep)) {
                    array.add(result);
                }
            } catch (IOException ioe) {
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }

        return newArray(context, array);
    }

    @JRubyMethod
    public IRubyObject each_byte(ThreadContext context, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, this, "each_byte");

        try {
            int value = bufferedStream.read();

            while (value != -1) {
                position++;
                block.yield(context, asFixnum(context, value));
                value = bufferedStream.read();
            }
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject each_char(ThreadContext context, Block block) {
        final Ruby runtime = context.runtime;
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(runtime, this, "each_char");

        try {
            int value = bufferedStream.read();
            while(value != -1) {
                position++;
                // TODO: must handle encoding. Move encoding handling methods to util class from RubyIO and use it.
                // TODO: StringIO needs a love, too.
                block.yield(context, runtime.newString(String.valueOf((char) (value & 0xFF))));
                value = bufferedStream.read();
            }
        } catch(IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }

        return context.nil;
    }

    /**
     * Document-method: Zlib::GzipReader.zcat
     *
     * call-seq:
     *   Zlib::GzipReader.zcat(io, options = {}, &amp;block) =&gt; nil
     *   Zlib::GzipReader.zcat(io, options = {}) =&gt; string
     *
     * Decompresses all gzip data in the +io+, handling multiple gzip
     * streams until the end of the +io+.  There should not be any non-gzip
     * data after the gzip streams.
     *
     * If a block is given, it is yielded strings of uncompressed data,
     * and the method returns +nil+.
     * If a block is not given, the method returns the concatenation of
     * all uncompressed data in all gzip streams.
     */
    @JRubyMethod(required = 1, optional = 1, checkArity = false, meta = true)
    public static IRubyObject zcat(ThreadContext context, IRubyObject klass, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 1, 2);

        IRubyObject io, unused;
        JZlibRubyGzipReader obj;
        RubyString buf = null, tmpbuf;
        long pos;

        io = args[0];

        try {
            do {
                obj = (JZlibRubyGzipReader) ((RubyClass) klass).newInstance(context, args, block);
                if (block.isGiven()) {
                    obj.each(context, IRubyObject.NULL_ARRAY, block);
                }
                else {
                    if (buf == null) buf = newEmptyString(context);
                    tmpbuf = obj.readAll(context);
                    buf.cat(tmpbuf);
                }

                obj.read(context, IRubyObject.NULL_ARRAY);
                pos = toLong(context, io.callMethod(context, "pos"));
                unused = obj.unused();
                obj.finish(context);
                if (!unused.isNil()) {
                    pos -= toLong(context, unused.callMethod(context, "length"));
                    io.callMethod(context, "pos=", asFixnum(context, pos));
                }
            } while (pos < toLong(context, io.callMethod(context, "size")));
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }

        return block.isGiven() ? context.nil : buf;
    }

    private void fixBrokenTrailingCharacter(ThreadContext context, ByteList result) throws IOException {
        // fix broken trailing character
        int extraBytes = StringSupport.bytesToFixBrokenTrailingCharacter(result.getUnsafeBytes(), result.getBegin(),
                result.getRealSize(), getReadEncoding(context), result.length());

        for (int i = 0; i < extraBytes; i++) {
            int read = bufferedStream.read();
            if (read == -1) break;
            
            result.append(read);
        }
    }

    private int line = 0;
    private long position = 0;
    private IOInputStream ioInputStream;
    private GZIPInputStream io;
    private PushbackInputStream bufferedStream;
}

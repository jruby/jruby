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

package org.jruby;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash.HashEntryIterator;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.Constantizable;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

import static com.headius.backport9.buffer.Buffers.clearBuffer;
import static com.headius.backport9.buffer.Buffers.flipBuffer;
import static org.jruby.api.Access.encodingService;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

@JRubyClass(name="Encoding")
public class RubyEncoding extends RubyObject implements Constantizable {
    public static final Charset UTF8 = StandardCharsets.UTF_8;
    public static final Charset UTF16 = StandardCharsets.UTF_16;
    public static final Charset ISO = StandardCharsets.ISO_8859_1;

    public static final ByteList LOCALE = new ByteList(encodeISO("locale"), false);
    public static final ByteList EXTERNAL = new ByteList(encodeISO("external"), false);
    public static final ByteList FILESYSTEM = new ByteList(encodeISO("filesystem"), false);
    public static final ByteList INTERNAL = new ByteList(encodeISO("internal"), false);
    public static final ByteList BINARY_ASCII_NAME = new ByteList(encodeISO("BINARY (ASCII-8BIT)"), false);

    public static RubyClass createEncodingClass(ThreadContext context, RubyClass Object) {
        return defineClass(context, "Encoding", Object, NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyEncoding.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyEncoding.class)).
                classIndex(ClassIndex.ENCODING).
                defineMethods(context, RubyEncoding.class).
                tap(c -> c.singletonClass(context).undefMethods(context, "allocate"));
    }

    private Encoding encoding;
    private final ByteList name;
    private final boolean isDummy;
    private final transient Object constant;

    private RubyEncoding(Ruby runtime, byte[] name, int p, int end, boolean isDummy) {
        this(runtime, new ByteList(name, p, end), null, isDummy);
    }

    private RubyEncoding(Ruby runtime, byte[] name, Encoding encoding, boolean isDummy) {
        this(runtime, new ByteList(name), encoding, isDummy);
    }

    private RubyEncoding(Ruby runtime, ByteList name, Encoding encoding, boolean isDummy) {
        super(runtime, runtime.getEncoding());
        this.name = name;
        this.isDummy = isDummy;
        this.encoding = encoding;

        this.constant = OptoFactory.newConstantWrapper(RubyEncoding.class, this);
    }

    /**
     * @see org.jruby.compiler.Constantizable
     */
    @Override
    public Object constant() {
        return constant;
    }

    public static RubyEncoding newEncoding(Ruby runtime, byte[] name, int p, int end, boolean isDummy) {
        return new RubyEncoding(runtime, name, p, end, isDummy);
    }

    public final Encoding getEncoding() {
        if (encoding == null) encoding = getRuntime().getEncodingService().loadEncoding(name);
        return encoding;
    }

    private static Encoding extractEncodingFromObject(IRubyObject obj) {
        if (obj instanceof RubyEncoding) return ((RubyEncoding) obj).getEncoding();
        if (obj instanceof EncodingCapable) return ((EncodingCapable) obj).getEncoding();

        return null;
    }

    public static Encoding areCompatible(IRubyObject obj1, IRubyObject obj2) {
        Encoding enc1 = extractEncodingFromObject(obj1);
        Encoding enc2 = extractEncodingFromObject(obj2);

        if (enc1 == null || enc2 == null) return null;
        if (enc1 == enc2) return enc1;

        if (obj2 instanceof RubyString && ((RubyString) obj2).getByteList().getRealSize() == 0) return enc1;
        if (obj1 instanceof RubyString && ((RubyString) obj1).getByteList().getRealSize() == 0) {
            return enc1.isAsciiCompatible() && obj2 instanceof RubyString &&
                    ((RubyString) obj2).isAsciiOnly() ? enc1 : enc2;
        }

        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;

        if (!(obj2 instanceof RubyString) && enc2 instanceof USASCIIEncoding) return enc1;
        if (!(obj1 instanceof RubyString) && enc1 instanceof USASCIIEncoding) return enc2;

        if (!(obj1 instanceof RubyString)) {
            IRubyObject objTmp = obj1; // swap1 obj1 & obj2
            obj1 = obj2;
            obj2 = objTmp;

            Encoding encTmp = enc1;  // swap their encodings
            enc1 = enc2;
            enc2 = encTmp;
        }

        if (obj1 instanceof RubyString) {
            int cr1 = ((RubyString)obj1).scanForCodeRange();
            if (obj2 instanceof RubyString) {
                int cr2 = ((RubyString)obj2).scanForCodeRange();
                return areCompatible(enc1, cr1, enc2, cr2);
            }
            if (cr1 == StringSupport.CR_7BIT) return enc2;
        }

        return null;
    }

    // last block in rb_enc_compatible
    public static Encoding areCompatible(Encoding enc1, int cr1, Encoding enc2, int cr2) {
        if (cr1 != cr2) {
            /* may need to handle ENC_CODERANGE_BROKEN */
            if (cr1 == StringSupport.CR_7BIT) return enc2;
            if (cr2 == StringSupport.CR_7BIT) return enc1;
        }
        if (cr2 == StringSupport.CR_7BIT) return enc1;
        if (cr1 == StringSupport.CR_7BIT) return enc2;
        return null;
    }

    public static byte[] encodeUTF8(String str) {
        return encodeUTF8((CharSequence) str);
    }

    public static byte[] encodeUTF8(CharSequence str) {
        if (str.length() > CHAR_THRESHOLD) {
            return getBytes(UTF8.encode(toCharBuffer(str)));
        }
        return getBytes(getUTF8Coder().encode(str));
    }

    public static byte[] encodeISO(CharSequence str) {
        return encodeISOLoop(str);
    }

    public static byte[] encodeISO(char[] str) {
        return encodeISOLoop(str);
    }

    private static byte[] encodeISOLoop(char[] s) {
        int length = s.length;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) s[i];
        }
        return bytes;
    }

    private static byte[] encodeISOLoop(CharSequence s) {
        int length = s.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) s.charAt(i);
        }
        return bytes;
    }

    static ByteList doEncodeUTF8(String str) {
        if (str.length() > CHAR_THRESHOLD) {
            return getByteList(UTF8.encode(CharBuffer.wrap(str)), UTF8Encoding.INSTANCE, false);
        }
        return getByteList(getUTF8Coder().encode(str), UTF8Encoding.INSTANCE, true);
    }

    static ByteList doEncodeUTF8(CharSequence str) {
        if (str.length() > CHAR_THRESHOLD) {
            return getByteList(UTF8.encode(toCharBuffer(str)), UTF8Encoding.INSTANCE, false);
        }
        return getByteList(getUTF8Coder().encode(str), UTF8Encoding.INSTANCE, true);
    }

    private static CharBuffer toCharBuffer(CharSequence str) {
        return str instanceof CharBuffer ? (CharBuffer) str : CharBuffer.wrap(str);
    }

    private static byte[] getBytes(final ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    private static ByteList getByteList(final ByteBuffer buffer, final Encoding enc, final boolean shared) {
        byte[] bytes; int off;
        if (!shared && buffer.hasArray()) { // HeapByteBuffer
            bytes = buffer.array();
            off = buffer.arrayOffset();
        } else {
            bytes = getBytes(buffer);
            off = 0;
        }
        return new ByteList(bytes, off, off + buffer.limit(), enc, false);
    }

    public static byte[] encodeUTF16(String str) {
        return encode(str, UTF16);
    }

    public static byte[] encodeUTF16(CharSequence str) {
        return encode(str, UTF16);
    }

    static ByteList doEncodeUTF16(String str) {
        return doEncode(str, UTF16, UTF16BEEncoding.INSTANCE);
    }

    static ByteList doEncodeUTF16(CharSequence str) {
        return doEncode(str, UTF16, UTF16BEEncoding.INSTANCE);
    }

    public static byte[] encode(CharSequence cs, Charset charset) {
        ByteBuffer buffer = charset.encode(CharBuffer.wrap(cs));
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    static ByteList doEncode(String cs, Charset charset, Encoding enc) {
        return getByteList(charset.encode(CharBuffer.wrap(cs)), enc, false);
    }

    static ByteList doEncode(CharSequence cs, Charset charset, Encoding enc) {
        return getByteList(charset.encode(toCharBuffer(cs)), enc, false);
    }

    public static byte[] encode(String str, Charset charset) {
        ByteBuffer buffer = charset.encode(str);
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * Decode the range of bytes specified as UTF-8 content.
     *
     * This will attempt to use a zero-allocation decoder if the content length is shorter than {@link #CHAR_THRESHOLD}
     * bytes, and otherwise will used cached decoders to avoid reallocating Charset-related objects.
     *
     * @param bytes the byte array
     * @param start start of content
     * @param length length of content
     * @return a decoded String based on UTF-8 bytes
     */
    public static String decodeUTF8(byte[] bytes, int start, int length) {
        if (length > CHAR_THRESHOLD) {
            return UTF8.decode(ByteBuffer.wrap(bytes, start, length)).toString();
        }
        return getUTF8Coder().decode(bytes, start, length).toString();
    }

    /**
     * Decode the range of bytes specified as "raw" binary content, as in ISO-8859-1 or ASCII-8BIT encodings.
     *
     * This will attempt to use a zero-allocation decoder if the content length is shorter than {@link #CHAR_THRESHOLD}
     * bytes, and otherwise will used cached decoders to avoid reallocating Charset-related objects.
     *
     * @param bytes the byte array
     * @param start start of content
     * @param length length of content
     * @return a decoded String based on raw bytes
     */
    public static String decodeRaw(byte[] bytes, int start, int length) {
        if (length > CHAR_THRESHOLD) {
            return new String(decodeRawLoop(bytes, start, length));
        }
        return getRawCoder().decode(bytes, start, length);
    }

    /**
     * Decode the specified bytelist as "raw" binary content.
     *
     * This is the same as calling {@link #decodeRaw(byte[], int, int)} with the contents of byteList.
     *
     * @param byteList
     * @return a decoded string based on raw bytes
     */
    public static String decodeRaw(ByteList byteList) {
        return decodeRaw(byteList.unsafeBytes(), byteList.begin(), byteList.realSize());
    }

    private static char[] decodeRawLoop(byte[] s, int start, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (s[i + start] & 0xFF);
        }
        return chars;
    }

    public static String decodeUTF8(byte[] bytes) {
        return decodeUTF8(bytes, 0, bytes.length);
    }

    public static String decode(byte[] bytes, int start, int length, Charset charset) {
        return charset.decode(ByteBuffer.wrap(bytes, start, length)).toString();
    }

    public static String decode(byte[] bytes, Charset charset) {
        return charset.decode(ByteBuffer.wrap(bytes)).toString();
    }

    /** The maximum number of characters we can encode/decode in our cached buffers */
    private static final int CHAR_THRESHOLD = 1024;

    /**
     * A cached decoder for UTF-8 bytes.
     */
    private static class UTF8Coder {
        private final CharsetEncoder encoder = UTF8.newEncoder();
        private final CharsetDecoder decoder = UTF8.newDecoder();
        /** The resulting encode/decode buffer sized by the max number of
         * characters (using 4 bytes per char possible for utf-8) */
        private static final int BUF_SIZE = CHAR_THRESHOLD * 4;
        private final ByteBuffer byteBuffer = ByteBuffer.allocate(BUF_SIZE);
        private final CharBuffer charBuffer = CharBuffer.allocate(BUF_SIZE);

        UTF8Coder() {
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        public final ByteBuffer encode(String str) {
            ByteBuffer buf = byteBuffer;
            CharBuffer cbuf = charBuffer;
            clearBuffer(buf);
            clearBuffer(cbuf);
            cbuf.put(str);
            flipBuffer(cbuf);
            encoder.encode(cbuf, buf, true);
            flipBuffer(buf);

            return buf;
        }

        public final ByteBuffer encode(CharSequence str) {
            ByteBuffer buf = byteBuffer;
            CharBuffer cbuf = charBuffer;
            clearBuffer(buf);
            clearBuffer(cbuf);
            // NOTE: doesn't matter is we toString here in terms of speed
            // ... so we "safe" some space at least by not copy-ing char[]
            for (int i = 0; i < str.length(); i++) cbuf.put(str.charAt(i));
            flipBuffer(cbuf);
            encoder.encode(cbuf, buf, true);
            flipBuffer(buf);

            return buf;
        }

        public final CharBuffer decode(byte[] bytes, int start, int length) {
            CharBuffer cbuf = charBuffer;
            ByteBuffer buf = byteBuffer;
            clearBuffer(cbuf);
            clearBuffer(buf);
            buf.put(bytes, start, length);
            flipBuffer(buf);
            decoder.decode(buf, cbuf, true);
            flipBuffer(cbuf);

            return cbuf;
        }

    }

    /**
     * A cached decoder object to decode bytes as raw binary (ISO-8859-1 or ASCII-8BIT) content.
     */
    private static class RawCoder {
        private final char[] charBuffer = new char[CHAR_THRESHOLD];

        public final String decode(byte[] bytes, int start, int length) {
            char[] charBuffer = this.charBuffer;
            for (int i = 0; i < length; i++) {
                charBuffer[i] = (char) (bytes[i + start] & 0xFF);
            }

            return new String(charBuffer, 0, length);
        }
    }

    /**
     * Thread-local UTF8Coder wrapped in a SoftReference to avoid possible ClassLoader leak.
     * See JRUBY-6522
     */
    private static final ThreadLocal<SoftReference<UTF8Coder>> UTF8_CODER = new ThreadLocal<>();

    /**
     * Thread-local RawCoder wrapped in a SoftReference to avoid possible ClassLoader leak.
     * See JRUBY-6522
     */
    private static final ThreadLocal<SoftReference<RawCoder>> RAW_CODER = new ThreadLocal<>();

    private static UTF8Coder getUTF8Coder() {
        UTF8Coder coder;
        SoftReference<UTF8Coder> ref = UTF8_CODER.get();
        if (ref == null || (coder = ref.get()) == null) {
            coder = new UTF8Coder();
            UTF8_CODER.set(new SoftReference<>(coder));
        }

        return coder;
    }

    private static RawCoder getRawCoder() {
        RawCoder coder;
        SoftReference<RawCoder> ref = RAW_CODER.get();
        if (ref == null || (coder = ref.get()) == null) {
            coder = new RawCoder();
            RAW_CODER.set(new SoftReference<>(coder));
        }

        return coder;
    }

    /**
     * Check whether the given encoding and the encoding from the given {@link CodeRangeable} are compatible.
     *
     * This version differs from {@link RubyString#checkEncoding(CodeRangeable)} in that the contents of the first
     * encoding's string are not taken into consideration (e.g. blank first string does not skip compatibility check
     * with second string's encoding).
     *
     * See rb_enc_check in CRuby and use from stringio that passes encoding instead of string for the first argument.
     *
     * See https://github.com/ruby/stringio/pull/116 for some discussion
     *
     * MRI: rb_enc_check with encoding for first parameter
     *
     * @param context the current thread context
     * @param encoding the first encoding
     * @param other the {@link CodeRangeable} from which to get bytes and the second encoding
     * @return a negotiated encoding, if compatible; null otherwise
     */
    public static Encoding checkEncoding(ThreadContext context, Encoding encoding, CodeRangeable other) {
        Encoding enc = StringSupport.areCompatible(encoding, other);
        if (enc == null) throw context.runtime.newEncodingCompatibilityError("incompatible character encodings: " +
                encoding + " and " + other.getByteList().getEncoding());
        return enc;
    }

    @JRubyMethod(name = "list", meta = true)
    public static IRubyObject list(ThreadContext context, IRubyObject recv) {
        return RubyArray.newArrayMayCopy(context.runtime, encodingService(context).getEncodingList());
    }

    @JRubyMethod(name = "locale_charmap", meta = true)
    public static IRubyObject locale_charmap(ThreadContext context, IRubyObject recv) {
        ByteList name = new ByteList(encodingService(context).getLocaleEncoding().getName());

        return RubyString.newUsAsciiStringNoCopy(context.runtime, name);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "name_list", meta = true)
    public static IRubyObject name_list(ThreadContext context, IRubyObject recv) {
        EncodingService service = encodingService(context);

        var result = allocArray(context, service.getEncodings().size() + service.getAliases().size() + 4);
        var i = service.getEncodings().entryIterator();
        while (i.hasNext()) {
            var e = i.next();
            result.append(context, RubyString.newUsAsciiStringShared(context.runtime, e.bytes, e.p, e.end - e.p).freeze(context));
        }
        i = service.getAliases().entryIterator();
        while (i.hasNext()) {
            var e = i.next();
            result.append(context, RubyString.newUsAsciiStringShared(context.runtime, e.bytes, e.p, e.end - e.p).freeze(context));
        }

        result.append(context, newString(context, EXTERNAL));
        result.append(context, newString(context, FILESYSTEM));
        result.append(context, newString(context, INTERNAL));
        result.append(context, newString(context, LOCALE));

        return result;
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "aliases", meta = true)
    public static IRubyObject aliases(ThreadContext context, IRubyObject recv) {
        EncodingService service = encodingService(context);
        IRubyObject list[] = service.getEncodingList();
        HashEntryIterator i = service.getAliases().entryIterator();
        RubyHash result = newHash(context);

        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            IRubyObject alias = RubyString.newUsAsciiStringShared(context.runtime, e.bytes, e.p, e.end - e.p).freeze(context);
            IRubyObject name = RubyString.newUsAsciiStringShared(context.runtime,
                                ((RubyEncoding)list[e.value.getIndex()]).name).freeze(context);
            result.fastASet(alias, name);
        }

        result.fastASet(newString(context, EXTERNAL), newString(context, new ByteList(context.runtime.getDefaultExternalEncoding().getName())));
        result.fastASet(newString(context, LOCALE), newString(context, new ByteList(service.getLocaleEncoding().getName())));

        return result;
    }

    @JRubyMethod(name = "find", meta = true)
    public static IRubyObject find(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return str instanceof RubyEncoding ? str : encodingService(context).rubyEncodingFromObject(str);
    }

    @Deprecated(since = "10.0")
    public IRubyObject replicate(ThreadContext context, IRubyObject arg) {
        return new RubyEncoding(context.runtime, arg.convertToString().getBytes(), getEncoding(), isDummy);
    }

    @JRubyMethod(name = "_dump")
    public IRubyObject _dump(ThreadContext context, IRubyObject arg) {
        return to_s(context);
    }

    @JRubyMethod(name = "_load", meta = true)
    public static IRubyObject _load(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return find(context, recv, str);
    }

    @JRubyMethod(name = "ascii_compatible?")
    public IRubyObject asciiCompatible_p(ThreadContext context) {
        return asBoolean(context, getEncoding().isAsciiCompatible());
    }

    @JRubyMethod(name = {"to_s", "name"})
    public IRubyObject to_s(ThreadContext context) {
        // TODO: rb_usascii_str_new2
        return RubyString.newUsAsciiStringShared(context.runtime, name);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        ByteList bytes = new ByteList();
        bytes.append("#<Encoding:".getBytes());
        bytes.append(inspectName());
        if (isDummy) bytes.append(" (dummy)".getBytes());
        bytes.append('>');
        return RubyString.newUsAsciiStringNoCopy(context.runtime, bytes);
    }

    private ByteList inspectName() {
        if (encoding == ASCIIEncoding.INSTANCE) {
            return BINARY_ASCII_NAME;
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "names")
    public IRubyObject names(ThreadContext context) {
        EncodingService service = encodingService(context);
        Entry entry = service.findEncodingOrAliasEntry(name);
        var result = newArray(context);
        HashEntryIterator i;
        i = service.getEncodings().entryIterator();
        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            if (e.value == entry) {
                result.append(context, RubyString.newUsAsciiStringShared(context.runtime, e.bytes, e.p, e.end - e.p).freeze(context));
            }
        }
        i = service.getAliases().entryIterator();
        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e =
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            if (e.value == entry) {
                result.append(context, RubyString.newUsAsciiStringShared(context.runtime, e.bytes, e.p, e.end - e.p).freeze(context));
            }
        }
        result.append(context, newString(context, EXTERNAL));
        result.append(context, newString(context, LOCALE));

        return result;
    }

    @JRubyMethod(name = "dummy?")
    public IRubyObject dummy_p(ThreadContext context) {
        return asBoolean(context, isDummy);
    }

    @JRubyMethod(name = "compatible?", meta = true)
    public static IRubyObject compatible_p(ThreadContext context, IRubyObject self, IRubyObject first, IRubyObject second) {
        Encoding enc = areCompatible(first, second);

        return enc == null ? context.nil : encodingService(context).getEncoding(enc);
    }

    @JRubyMethod(name = "default_external", meta = true)
    public static IRubyObject getDefaultExternal(ThreadContext context, IRubyObject recv) {
        return encodingService(context).getDefaultExternal();
    }

    @JRubyMethod(name = "default_external=", meta = true)
    public static IRubyObject setDefaultExternal(ThreadContext context, IRubyObject recv, IRubyObject encoding) {
        if (context.runtime.isVerbose()) context.runtime.getWarnings().warning("setting Encoding.default_external");
        EncodingUtils.rbEncSetDefaultExternal(context, encoding);
        return encoding;
    }

    @JRubyMethod(name = "default_internal", meta = true)
    public static IRubyObject getDefaultInternal(ThreadContext context, IRubyObject recv) {
        return encodingService(context).getDefaultInternal();
    }

    @Deprecated
    public static IRubyObject getDefaultInternal(IRubyObject recv) {
        return getDefaultInternal(recv.getRuntime().getCurrentContext(), recv);
    }

    @JRubyMethod(name = "default_internal=", meta = true)
    public static IRubyObject setDefaultInternal(ThreadContext context, IRubyObject recv, IRubyObject encoding) {
        if (context.runtime.isVerbose()) context.runtime.getWarnings().warning("setting Encoding.default_internal");
        EncodingUtils.rbEncSetDefaultInternal(context, encoding);
        return encoding;
    }

    /**
     * @deprecated use {@link #decodeRaw(byte[], int, int)}
     */
    @Deprecated
    public static String decodeISO(byte[] bytes, int start, int length) {
        return decodeRaw(bytes, start, length);
    }

    /**
     * @deprecated use {@link #decodeRaw(ByteList)}
     */
    @Deprecated
    public static String decodeISO(ByteList byteList) {
        return decodeRaw(byteList);
    }
}

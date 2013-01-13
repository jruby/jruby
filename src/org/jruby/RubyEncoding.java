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

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash.HashEntryIterator;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import static org.jruby.CompatVersion.*;

@JRubyClass(name="Encoding")
public class RubyEncoding extends RubyObject {
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final Charset ISO = Charset.forName("ISO-8859-1");
    public static final ByteList LOCALE = ByteList.create("locale");
    public static final ByteList EXTERNAL = ByteList.create("external");

    public static RubyClass createEncodingClass(Ruby runtime) {
        RubyClass encodingc = runtime.defineClass("Encoding", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setEncoding(encodingc);
        encodingc.index = ClassIndex.ENCODING;
        encodingc.setReifiedClass(RubyEncoding.class);
        encodingc.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyEncoding;
            }
        };

        encodingc.getSingletonClass().undefineMethod("allocate");
        encodingc.defineAnnotatedMethods(RubyEncoding.class);

        return encodingc;
    }

    private Encoding encoding;
    private final ByteList name;
    private final boolean isDummy;

    private RubyEncoding(Ruby runtime, byte[]name, int p, int end, boolean isDummy) {
        super(runtime, runtime.getEncoding());
        this.name = new ByteList(name, p, end);
        this.isDummy = isDummy;
    }
    
    private RubyEncoding(Ruby runtime, byte[]name, boolean isDummy) {
        this(runtime, name, 0, name.length, isDummy);
    }

    private RubyEncoding(Ruby runtime, Encoding encoding) {
        super(runtime, runtime.getEncoding());
        this.name = new ByteList(encoding.getName());
        this.isDummy = false;
        this.encoding = encoding;
    }

    public static RubyEncoding newEncoding(Ruby runtime, byte[]name, int p, int end, boolean isDummy) {
        return new RubyEncoding(runtime, name, p, end, isDummy);
    }

    public static RubyEncoding newEncoding(Ruby runtime, byte[]name, boolean isDummy) {
        return new RubyEncoding(runtime, name, isDummy);
    }

    public static RubyEncoding newEncoding(Ruby runtime, Encoding encoding) {
        return new RubyEncoding(runtime, encoding);
    }

    public final Encoding getEncoding() {
        // TODO: make threadsafe
        if (encoding == null) encoding = getRuntime().getEncodingService().loadEncoding(name);
        return encoding;
    }

    public static Encoding areCompatible(IRubyObject obj1, IRubyObject obj2) {
        Encoding enc1 = null;
        Encoding enc2 = null;

        if (obj1 instanceof RubyEncoding) {
          enc1 = ((RubyEncoding)obj1).getEncoding();
        } else if (obj1 instanceof EncodingCapable) {
          enc1 = ((EncodingCapable)obj1).getEncoding();
        }

        if (obj2 instanceof RubyEncoding) {
          enc2 = ((RubyEncoding)obj2).getEncoding();
        } else if (obj2 instanceof EncodingCapable) {
          enc2 = ((EncodingCapable)obj2).getEncoding();
        }

        if (enc1 != null && enc2 != null) {
            if (enc1 == enc2) return enc1;

            if (obj2 instanceof RubyString && ((RubyString) obj2).getByteList().getRealSize() == 0) return enc1;
            if (obj1 instanceof RubyString && ((RubyString) obj1).getByteList().getRealSize() == 0) return enc2;

            if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;

            if (!(obj2 instanceof RubyString) && enc2 instanceof USASCIIEncoding) return enc1;
            if (!(obj1 instanceof RubyString) && enc1 instanceof USASCIIEncoding) return enc2;

            if(!(obj1 instanceof RubyString)) {
                IRubyObject objTmp = obj1;
                obj1 = obj2;
                obj1 = objTmp;

                Encoding encTmp = enc1;
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
        }
        return null;
    }

    static Encoding areCompatible(Encoding enc1, int cr1, Encoding enc2, int cr2) {
        if (cr1 != cr2) {
            /* may need to handle ENC_CODERANGE_BROKEN */
            if (cr1 == StringSupport.CR_7BIT) return enc2;
            if (cr2 == StringSupport.CR_7BIT) return enc1;
        }
        if (cr2 == StringSupport.CR_7BIT) {
            if (enc1 instanceof ASCIIEncoding) return enc2;
            return enc1;
        }
        if (cr1 == StringSupport.CR_7BIT) return enc2;
        return null;
    }

    public static byte[] encodeUTF8(CharSequence cs) {
        return getUTF8Coder().encode(cs);
    }

    public static byte[] encodeUTF8(String str) {
        return getUTF8Coder().encode(str);
    }

    public static byte[] encode(CharSequence cs, Charset charset) {
        ByteBuffer buffer = charset.encode(cs.toString());
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    public static byte[] encode(String str, Charset charset) {
        ByteBuffer buffer = charset.encode(str);
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    public static String decodeUTF8(byte[] bytes, int start, int length) {
        return getUTF8Coder().decode(bytes, start, length);
    }

    public static String decodeUTF8(byte[] bytes) {
        return getUTF8Coder().decode(bytes);
    }

    public static String decode(byte[] bytes, int start, int length, Charset charset) {
        return charset.decode(ByteBuffer.wrap(bytes, start, length)).toString();
    }

    public static String decode(byte[] bytes, Charset charset) {
        return charset.decode(ByteBuffer.wrap(bytes)).toString();
    }
    
    private static class UTF8Coder {
        private final CharsetEncoder encoder = UTF8.newEncoder();
        private final CharsetDecoder decoder = UTF8.newDecoder();
        /** The maximum number of characters we can encode/decode in our cached buffers */
        private static final int CHAR_THRESHOLD = 1024;
        /** The resulting encode/decode buffer sized by the max number of
         * characters (using 4 bytes per char possible for utf-8) */
        private static final int BUF_SIZE = CHAR_THRESHOLD * 4;
        private final ByteBuffer byteBuffer = ByteBuffer.allocate(BUF_SIZE);
        private final CharBuffer charBuffer = CharBuffer.allocate(BUF_SIZE);

        public UTF8Coder() {
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        public byte[] encode(CharSequence cs) {
            ByteBuffer buffer;
            if (cs.length() > CHAR_THRESHOLD) {
                buffer = UTF8.encode(cs.toString());
            } else {
                buffer = byteBuffer;
                CharBuffer cbuffer = charBuffer;
                buffer.clear();
                cbuffer.clear();
                cbuffer.put(cs.toString());
                cbuffer.flip();
                encoder.encode(cbuffer, buffer, true);
                buffer.flip();
            }
            
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            return bytes;
        }
        
        public String decode(byte[] bytes, int start, int length) {
            CharBuffer cbuffer;
            if (length > CHAR_THRESHOLD) {
                cbuffer = UTF8.decode(ByteBuffer.wrap(bytes, start, length));
            } else {
                cbuffer = charBuffer;
                ByteBuffer buffer = byteBuffer;
                cbuffer.clear();
                buffer.clear();
                buffer.put(bytes, start, length);
                buffer.flip();
                decoder.decode(buffer, cbuffer, true);
                cbuffer.flip();
            }
            
            return cbuffer.toString();
        }
        
        public String decode(byte[] bytes) {
            return decode(bytes, 0, bytes.length);
        }
    }

    /**
     * UTF8Coder wrapped in a SoftReference to avoid possible ClassLoader leak.
     * See JRUBY-6522
     */
    private static final ThreadLocal<SoftReference<UTF8Coder>> UTF8_CODER =
        new ThreadLocal<SoftReference<UTF8Coder>>();

    private static UTF8Coder getUTF8Coder() {
        UTF8Coder coder;
        SoftReference<UTF8Coder> ref = UTF8_CODER.get();
        if (ref == null || (coder = ref.get()) == null) {
            coder = new UTF8Coder();
            ref = new SoftReference<UTF8Coder>(coder);
            UTF8_CODER.set(ref);
        }
        
        return coder;
    }

    @JRubyMethod(name = "list", meta = true)
    public static IRubyObject list(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        return RubyArray.newArrayNoCopy(runtime, runtime.getEncodingService().getEncodingList(), 0);
    }

    @JRubyMethod(name = "locale_charmap", meta = true)
    public static IRubyObject locale_charmap(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();
        ByteList name = new ByteList(service.getLocaleEncoding().getName());
        
        return RubyString.newUsAsciiStringNoCopy(runtime, name);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "name_list", meta = true)
    public static IRubyObject name_list(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();
        
        RubyArray result = runtime.newArray(service.getEncodings().size() + service.getAliases().size());
        HashEntryIterator i;
        i = service.getEncodings().entryIterator();
        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            result.append(RubyString.newUsAsciiStringShared(runtime, e.bytes, e.p, e.end - e.p).freeze(context));
        }
        i = service.getAliases().entryIterator();        
        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            result.append(RubyString.newUsAsciiStringShared(runtime, e.bytes, e.p, e.end - e.p).freeze(context));
        }

        result.append(runtime.newString(EXTERNAL));
        result.append(runtime.newString(LOCALE));
        
        return result;
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "aliases", meta = true)
    public static IRubyObject aliases(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();

        IRubyObject list[] = service.getEncodingList();
        HashEntryIterator i = service.getAliases().entryIterator();
        RubyHash result = RubyHash.newHash(runtime);

        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            IRubyObject alias = RubyString.newUsAsciiStringShared(runtime, e.bytes, e.p, e.end - e.p).freeze(context);
            IRubyObject name = RubyString.newUsAsciiStringShared(runtime, 
                                ((RubyEncoding)list[e.value.getIndex()]).name).freeze(context);
            result.fastASet(alias, name);
        }

        result.fastASet(runtime.newString(EXTERNAL),
                runtime.newString(new ByteList(runtime.getDefaultExternalEncoding().getName())));
        result.fastASet(runtime.newString(LOCALE),
                runtime.newString(new ByteList(service.getLocaleEncoding().getName())));

        return result;
    }

    @JRubyMethod(name = "find", meta = true)
    public static IRubyObject find(ThreadContext context, IRubyObject recv, IRubyObject str) {
        Ruby runtime = context.runtime;

        // Wacky but true...return arg if it is an encoding looking for itself
        if (str instanceof RubyEncoding) return str;

        return runtime.getEncodingService().rubyEncodingFromObject(str);
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
        return context.runtime.newBoolean(getEncoding().isAsciiCompatible());
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
        bytes.append(name);
        if (isDummy) bytes.append(" (dummy)".getBytes());
        bytes.append('>');
        return RubyString.newUsAsciiStringNoCopy(context.runtime, bytes);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "names")
    public IRubyObject names(ThreadContext context) {
        Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();
        Entry entry = service.findEncodingOrAliasEntry(name);

        RubyArray result = runtime.newArray();
        HashEntryIterator i;
        i = service.getEncodings().entryIterator();
        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            if (e.value == entry) {
                result.append(RubyString.newUsAsciiStringShared(runtime, e.bytes, e.p, e.end - e.p).freeze(context));
            }
        }
        i = service.getAliases().entryIterator();        
        while (i.hasNext()) {
            CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry> e = 
                ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<Entry>)i.next());
            if (e.value == entry) {
                result.append(RubyString.newUsAsciiStringShared(runtime, e.bytes, e.p, e.end - e.p).freeze(context));
            }
        }
        result.append(runtime.newString(EXTERNAL));
        result.append(runtime.newString(LOCALE));
        
        return result;
    }

    @JRubyMethod(name = "dummy?")
    public IRubyObject dummy_p(ThreadContext context) {
        return context.runtime.newBoolean(isDummy);
    }

    @JRubyMethod(name = "compatible?", meta = true)
    public static IRubyObject compatible_p(ThreadContext context, IRubyObject self, IRubyObject first, IRubyObject second) {
        Ruby runtime = context.runtime;
        Encoding enc = areCompatible(first, second);

        return enc == null ? runtime.getNil() : runtime.getEncodingService().getEncoding(enc);
    }

    @JRubyMethod(name = "default_external", meta = true, compat = RUBY1_9)
    public static IRubyObject getDefaultExternal(IRubyObject recv) {
        return recv.getRuntime().getEncodingService().getDefaultExternal();
    }

    @JRubyMethod(name = "default_external=", meta = true, compat = RUBY1_9)
    public static IRubyObject setDefaultExternal(IRubyObject recv, IRubyObject encoding) {
        Ruby runtime = recv.getRuntime();
        EncodingService service = runtime.getEncodingService();
        if (encoding.isNil()) {
            throw recv.getRuntime().newArgumentError("default_external can not be nil");
        }
        runtime.setDefaultExternalEncoding(service.getEncodingFromObject(encoding));
        return encoding;
    }

    @JRubyMethod(name = "default_internal", meta = true, compat = RUBY1_9)
    public static IRubyObject getDefaultInternal(IRubyObject recv) {
        return recv.getRuntime().getEncodingService().getDefaultInternal();
    }

    @JRubyMethod(name = "default_internal=", required = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject setDefaultInternal(IRubyObject recv, IRubyObject encoding) {
        Ruby runtime = recv.getRuntime();
        EncodingService service = runtime.getEncodingService();
        if (encoding.isNil()) {
            throw recv.getRuntime().newArgumentError("default_internal can not be nil");
        }
        recv.getRuntime().setDefaultInternalEncoding(service.getEncodingFromObject(encoding));
        return encoding;
    }

    @Deprecated
    public static IRubyObject getDefaultExternal(Ruby runtime) {
        return runtime.getEncodingService().getDefaultExternal();
    }

    @Deprecated
    public static IRubyObject getDefaultInternal(Ruby runtime) {
        return runtime.getEncodingService().getDefaultInternal();
    }

    @Deprecated
    public static IRubyObject convertEncodingToRubyEncoding(Ruby runtime, Encoding defaultEncoding) {
        return runtime.getEncodingService().convertEncodingToRubyEncoding(defaultEncoding);
    }

    @Deprecated
    public static Encoding getEncodingFromObject(Ruby runtime, IRubyObject arg) {
        return runtime.getEncodingService().getEncodingFromObject(arg);
    }
}

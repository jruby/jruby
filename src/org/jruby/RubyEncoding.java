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

import java.nio.charset.Charset;
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

@JRubyClass(name="Encoding")
public class RubyEncoding extends RubyObject {

    public static RubyClass createEncodingClass(Ruby runtime) {
        RubyClass encodingc = runtime.defineClass("Encoding", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setEncoding(encodingc);
        encodingc.index = ClassIndex.ENCODING;
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

    public final Encoding getEncoding() {
        // TODO: make threadsafe
        if (encoding == null) encoding = getRuntime().getEncodingService().loadEncoding(name);
        return encoding;
    }

    public static final Encoding areCompatible(IRubyObject obj1, IRubyObject obj2) {
        if (obj1 instanceof EncodingCapable && obj2 instanceof EncodingCapable) {
            Encoding enc1 = ((EncodingCapable)obj1).getEncoding();
            Encoding enc2 = ((EncodingCapable)obj2).getEncoding();
            if (enc1 == enc2) return enc1;

            if (obj2 instanceof RubyString && ((RubyString)obj2).getByteList().realSize == 0) return enc1; 
            if (obj1 instanceof RubyString && ((RubyString)obj1).getByteList().realSize == 0) return enc2;

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

    @JRubyMethod(name = "list", meta = true)
    public static IRubyObject list(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        return RubyArray.newArrayNoCopy(runtime, runtime.getEncodingService().getEncodingList(), 0);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "name_list", meta = true)
    public static IRubyObject name_list(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
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
        return result;
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "aliases", meta = true)
    public static IRubyObject aliases(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
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
        return result;
    }

    @JRubyMethod(name = "find", meta = true)
    public static IRubyObject find(ThreadContext context, IRubyObject recv, IRubyObject str) {
        Ruby runtime = context.getRuntime();
        EncodingService service = runtime.getEncodingService();
        // TODO: check for ascii string
        ByteList name = str.convertToString().getByteList();
        Entry e = service.findEncodingOrAliasEntry(name);

        if (e == null) throw context.getRuntime().newArgumentError("unknown encoding name - " + name);

        return service.getEncodingList()[e.getIndex()];
    }

    @JRubyMethod(name = "_dump")
    public IRubyObject _dump(ThreadContext context) {
        return to_s(context);
    }

    @JRubyMethod(name = "_load", meta = true)
    public static IRubyObject _load(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return find(context, recv, str);
    }

    @JRubyMethod(name = {"to_s", "name"})
    public IRubyObject to_s(ThreadContext context) {
        // TODO: rb_usascii_str_new2
        return RubyString.newUsAsciiStringShared(context.getRuntime(), name);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        ByteList bytes = new ByteList();
        bytes.append("#<Encoding:".getBytes());
        bytes.append(name);
        if (isDummy) bytes.append(" (dummy)".getBytes());
        bytes.append('>');
        return RubyString.newUsAsciiStringNoCopy(context.getRuntime(), bytes);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "names")
    public IRubyObject names(ThreadContext context) {
        Ruby runtime = context.getRuntime();
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
        return result;
    }

    @JRubyMethod(name = "dummy?")
    public IRubyObject dummy_p(ThreadContext context) {
        return context.getRuntime().newBoolean(isDummy);
    }

    @JRubyMethod(name = "compatible?", meta = true)
    public static IRubyObject compatible_p(ThreadContext context, IRubyObject self, IRubyObject first, IRubyObject second) {
        Ruby runtime = context.getRuntime();
        Encoding enc = areCompatible(first, second);

        return enc == null ? runtime.getNil() : new RubyEncoding(runtime, enc);
    }

    @JRubyMethod(name = "default_external", meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject getDefaultExternal(IRubyObject recv) {
        return getDefaultExternal(recv.getRuntime());
    }

    public static IRubyObject getDefaultExternal(Ruby runtime) {
        IRubyObject defaultExternal = convertEncodingToRubyEncoding(runtime, runtime.getDefaultExternalEncoding());

        if (defaultExternal.isNil()) {
            ByteList encodingName = ByteList.create(Charset.defaultCharset().name());
            Encoding encoding = runtime.getEncodingService().loadEncoding(encodingName);

            runtime.setDefaultExternalEncoding(encoding);
            defaultExternal = convertEncodingToRubyEncoding(runtime, encoding);
        }

        return defaultExternal;
    }

    @JRubyMethod(name = "default_external=", required = 1, frame = true, meta = true, compat = CompatVersion.RUBY1_9)
    public static void setDefaultExternal(IRubyObject recv, IRubyObject encoding) {
        if (encoding.isNil()) {
            recv.getRuntime().newArgumentError("default_external can not be nil");
        }
        recv.getRuntime().setDefaultExternalEncoding(getEncodingFromObject(recv.getRuntime(), encoding));
    }

    @JRubyMethod(name = "default_internal", meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject getDefaultInternal(IRubyObject recv) {
        return getDefaultInternal(recv.getRuntime());
    }

    public static IRubyObject getDefaultInternal(Ruby runtime) {
        return convertEncodingToRubyEncoding(runtime, runtime.getDefaultInternalEncoding());
    }

    @JRubyMethod(name = "default_internal=", required = 1, frame = true, meta = true, compat = CompatVersion.RUBY1_9)
    public static void setDefaultInternal(IRubyObject recv, IRubyObject encoding) {
        if (encoding.isNil()) {
            recv.getRuntime().newArgumentError("default_internal can not be nil");
        }
        recv.getRuntime().setDefaultInternalEncoding(getEncodingFromObject(recv.getRuntime(), encoding));
    }

    public static IRubyObject convertEncodingToRubyEncoding(Ruby runtime, Encoding defaultEncoding) {
        if (defaultEncoding != null) {
            return new RubyEncoding(runtime, defaultEncoding);
        }
        return runtime.getNil();
    }

    public static Encoding getEncodingFromObject(Ruby runtime, IRubyObject arg) {
        Encoding encoding = null;
        if (arg instanceof RubyEncoding) {
            encoding = ((RubyEncoding) arg).getEncoding();
        } else if (!arg.isNil()) {
            encoding = arg.convertToString().toEncoding(runtime);
        }
        return encoding;
    }
}

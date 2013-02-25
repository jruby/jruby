/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import static org.jruby.CompatVersion.*;
import static org.jruby.runtime.Visibility.*;

@JRubyClass(name="Converter")
public class RubyConverter extends RubyObject {
    private RubyEncoding srcEncoding;
    private RubyEncoding destEncoding;
    private CharsetDecoder srcDecoder;
    private CharsetEncoder destEncoder;

    public static RubyClass createConverterClass(Ruby runtime) {
        RubyClass converterc = runtime.defineClassUnder("Converter", runtime.getClass("Data"), CONVERTER_ALLOCATOR, runtime.getEncoding());
        runtime.setConverter(converterc);
        converterc.index = ClassIndex.CONVERTER;
        converterc.setReifiedClass(RubyConverter.class);
        converterc.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyConverter;
            }
        };

        converterc.defineAnnotatedMethods(RubyConverter.class);
        return converterc;
    }

    private static ObjectAllocator CONVERTER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyConverter(runtime, klass);
        }
    };

    private static final Encoding UTF16 = UTF16BEEncoding.INSTANCE;

    public RubyConverter(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyConverter(Ruby runtime) {
        super(runtime, runtime.getConverter());
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject convpath) {
        return context.runtime.getNil();
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject dest) {
        srcEncoding = (RubyEncoding)context.runtime.getEncodingService().rubyEncodingFromObject(src);
        destEncoding = (RubyEncoding)context.runtime.getEncodingService().rubyEncodingFromObject(dest);

        srcDecoder = context.runtime.getEncodingService().charsetForEncoding(srcEncoding.getEncoding()).newDecoder();
        destEncoder = context.runtime.getEncodingService().charsetForEncoding(destEncoding.getEncoding()).newEncoder();

        return context.runtime.getNil();
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject dest, IRubyObject opt) {
        // TODO: opt
        initialize(context, src, dest);
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.runtime, "#<Encoding::Converter: " + srcDecoder.charset().name() + " to " + destEncoder.charset().name());
    }

    @JRubyMethod
    public IRubyObject convpath(ThreadContext context) {
        // we always pass through UTF-16
        IRubyObject utf16Encoding = context.runtime.getEncodingService().getEncodingList()[UTF16.getIndex()];
        return RubyArray.newArray(
                context.runtime,
                RubyArray.newArray(context.runtime, srcEncoding, utf16Encoding),
                RubyArray.newArray(context.runtime, utf16Encoding, destEncoding)
        );
    }

    @JRubyMethod
    public IRubyObject source_encoding() {
        return srcEncoding;
    }

    @JRubyMethod
    public IRubyObject destination_encoding() {
        return destEncoding;
    }

    @JRubyMethod
    public IRubyObject primitive_convert(ThreadContext context, IRubyObject src, IRubyObject dest) {
        RubyString result = (RubyString)convert(context, src);
        dest.convertToString().replace19(result);

        return context.runtime.newSymbol("finished");
    }

    @JRubyMethod
    public IRubyObject convert(ThreadContext context, IRubyObject srcBuffer) {
        if (!(srcBuffer instanceof RubyString)) {
            throw context.runtime.newTypeError(srcBuffer, context.runtime.getString());
        }

        RubyString srcString = (RubyString)srcBuffer;

        ByteList srcBL = srcString.getByteList();

        if (srcBL.getRealSize() == 0) return context.runtime.newSymbol("source_buffer_empty");

        ByteBuffer srcBB = ByteBuffer.wrap(srcBL.getUnsafeBytes(), srcBL.begin(), srcBL.getRealSize());
        try {
            CharBuffer srcCB = CharBuffer.allocate((int) (srcDecoder.maxCharsPerByte() * srcBL.getRealSize()) + 1);
            CoderResult decodeResult = srcDecoder.decode(srcBB, srcCB, true);
            srcCB.flip();

            ByteBuffer destBB = ByteBuffer.allocate((int) (destEncoder.maxBytesPerChar() * srcCB.limit()) + 1);
            CoderResult encodeResult = destEncoder.encode(srcCB, destBB, true);
            destBB.flip();

            byte[] destBytes = new byte[destBB.limit()];
            destBB.get(destBytes);

            srcDecoder.reset();
            destEncoder.reset();
            
            return context.runtime.newString(new ByteList(destBytes, destEncoding.getEncoding(), false));
        } catch (Exception e) {
            throw context.runtime.newRuntimeError(e.getLocalizedMessage());
        }
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject replacement(ThreadContext context) {
        return RubyString.newString(context.runtime, srcDecoder.replacement());
    }


    @JRubyMethod(name = "replacement=", compat = RUBY1_9)
    public IRubyObject replacement_set(ThreadContext context, IRubyObject replacement) {
        srcDecoder.replaceWith(replacement.convertToString().asJavaString());

        return replacement;
    }}

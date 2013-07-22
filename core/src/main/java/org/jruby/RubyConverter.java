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

import java.util.HashMap;
import java.util.Map;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CharsetTranscoder;

import static org.jruby.CompatVersion.*;
import org.jruby.anno.JRubyConstant;
import org.jruby.exceptions.RaiseException;

import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.encoding.EncodingService;

@JRubyClass(name="Converter")
public class RubyConverter extends RubyObject {
    private CharsetTranscoder transcoder;
    
    @JRubyConstant
    public static final int INVALID_MASK = 15;
    @JRubyConstant
    public static final int INVALID_REPLACE = 2;
    @JRubyConstant
    public static final int UNDEF_MASK = 240;
    @JRubyConstant
    public static final int UNDEF_REPLACE = 32;
    @JRubyConstant
    public static final int UNDEF_HEX_CHARREF = 48;
    @JRubyConstant
    public static final int PARTIAL_INPUT = 65536;
    @JRubyConstant
    public static final int AFTER_OUTPUT = 131072;
    @JRubyConstant
    public static final int UNIVERSAL_NEWLINE_DECORATOR = 256;
    @JRubyConstant
    public static final int CRLF_NEWLINE_DECORATOR = 4096;
    @JRubyConstant
    public static final int CR_NEWLINE_DECORATOR = 8192;
    @JRubyConstant
    public static final int XML_TEXT_DECORATOR = 16384;
    @JRubyConstant
    public static final int XML_ATTR_CONTENT_DECORATOR = 32768;
    @JRubyConstant
    public static final int XML_ATTR_QUOTE_DECORATOR = 1048576;
    
    // TODO: This is a little ugly...we should have a table of these in jcodings.
    private static final Map<Encoding, Encoding> NONASCII_TO_ASCII = new HashMap<Encoding, Encoding>();
    static {
        NONASCII_TO_ASCII.put(UTF16BEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(UTF16LEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(UTF32BEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(UTF32LEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(
                EncodingDB.getEncodings().get("ISO-2022-JP".getBytes()).getEncoding(),
                EncodingDB.getEncodings().get("stateless-ISO-2022-JP".getBytes()).getEncoding());
    }

    public static RubyClass createConverterClass(Ruby runtime) {
        RubyClass converterc = runtime.defineClassUnder("Converter", runtime.getClass("Data"), CONVERTER_ALLOCATOR, runtime.getEncoding());
        runtime.setConverter(converterc);
        converterc.index = ClassIndex.CONVERTER;
        converterc.setReifiedClass(RubyConverter.class);
        converterc.kindOf = new RubyModule.JavaClassKindOf(RubyConverter.class);

        converterc.defineAnnotatedMethods(RubyConverter.class);
        converterc.defineAnnotatedConstants(RubyConverter.class);
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
        return initialize(context, src, dest, context.nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject dest, IRubyObject opt) {
        Ruby runtime = context.runtime;
        EncodingService encodingService = runtime.getEncodingService();
        
        RubyEncoding srcEncoding = (RubyEncoding)encodingService.rubyEncodingFromObject(src);
        RubyEncoding destEncoding = (RubyEncoding)encodingService.rubyEncodingFromObject(dest);

        if (srcEncoding.eql(destEncoding)) {
            throw runtime.newConverterNotFoundError("code converter not found (" + srcEncoding + " to " + destEncoding + ")");
        }

        // Ensure we'll be able to get charsets fo these encodings
        try {
            encodingService.charsetForEncoding(srcEncoding.getEncoding());
            encodingService.charsetForEncoding(destEncoding.getEncoding());
        } catch (RaiseException e) {
            if (e.getException().getMetaClass().getBaseName().equals("CompatibilityError")) {
                throw runtime.newConverterNotFoundError("code converter not found (" + srcEncoding + " to " + destEncoding + ")");
            } else {
                throw e;
            }
        }
        
        transcoder = new CharsetTranscoder(context, destEncoding.getEncoding(), srcEncoding.getEncoding(), opt);

        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.runtime, "#<Encoding::Converter: " + transcoder.forceEncoding.getName() + " to " + transcoder.toEncoding.getName());
    }

    @JRubyMethod
    public IRubyObject convpath(ThreadContext context) {
        Ruby runtime = context.runtime;
        EncodingService encodingService = runtime.getEncodingService();
        // we always pass through UTF-16
        IRubyObject utf16Encoding = encodingService.getEncodingList()[UTF16.getIndex()];
        return RubyArray.newArray(
                runtime,
                RubyArray.newArray(runtime, source_encoding(context), utf16Encoding),
                RubyArray.newArray(runtime, utf16Encoding, destination_encoding(context))
        );
    }

    @JRubyMethod
    public IRubyObject source_encoding(ThreadContext context) {
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(transcoder.forceEncoding);
    }

    @JRubyMethod
    public IRubyObject destination_encoding(ThreadContext context) {
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(transcoder.toEncoding);
    }

    @JRubyMethod(required = 2, optional = 4)
    public IRubyObject primitive_convert(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        RubyString input;
        RubyString output;
        int outputByteoffset;
        int outputBytesize;
        int flags = 0;
        
        if (args.length <= 2 || args[2].isNil()) {
            outputByteoffset = -1;
        } else {
            outputByteoffset = (int)args[2].convertToInteger().getLongValue();
            if (outputByteoffset < 0) throw runtime.newArgumentError("negative offset");
        }
        
        if (args.length <= 3 || args[3].isNil()) {
            outputBytesize = 0;
        } else {
            outputBytesize = (int)args[3].convertToInteger().getLongValue();
            if (outputBytesize < 0) throw runtime.newArgumentError("negative offset");
        }
        
        if (args.length > 4 && !args[4].isNil()) {
            if (args.length > 5 && !args[5].isNil()) {
                throw runtime.newArgumentError(args.length, 5);
            }
            if (args[4] instanceof RubyHash) {
                RubyHash opt = (RubyHash)args[4];
                
                IRubyObject partialInput = opt.fastARef(runtime.newSymbol("partial_input"));
                if (partialInput != null && partialInput.isTrue()) {
                    flags |= PARTIAL_INPUT;
                }
                
                IRubyObject afterOutput = opt.fastARef(runtime.newSymbol("after_output"));
                if (afterOutput != null && afterOutput.isTrue()) {
                    flags |= AFTER_OUTPUT;
                }
            } else {
                flags = (int)args[4].convertToInteger().getLongValue();
            }
        }
        
        ByteList inBytes;
        ByteList outBytes;
        
        if (args[0].isNil()) {
            inBytes = new ByteList();
        } else {
            input = args[0].convertToString();
            input.modify19();
            inBytes = input.getByteList();
        }
        
        output = args[1].convertToString();
        output.modify19();
        outBytes = output.getByteList();
        
        if (outputByteoffset == -1) {
            outputByteoffset = outBytes.getRealSize();
        } else if (outputByteoffset > outBytes.getRealSize()) {
            throw runtime.newArgumentError("offset too big");
        }
        
        int outputByteEnd = outputByteoffset + outputBytesize;
        
        if (outputByteEnd > outBytes.getRealSize()) {
            outBytes.ensure(outputByteEnd);
        }

        if (inBytes.getRealSize() == 0 && (flags & PARTIAL_INPUT) == 0) return context.runtime.newSymbol("source_buffer_empty");

        CharsetTranscoder.RubyCoderResult result = transcoder.primitiveConvert(
                runtime,
                inBytes,
                output.getByteList(),
                outputByteoffset,
                outputBytesize,
                inBytes.getEncoding(),
                inBytes.getEncoding().isAsciiCompatible(),
                flags);
        
        outBytes.setEncoding(transcoder.toEncoding);

        if (result != null) {
            if (result.coderResult.isError()) {
                if (result.coderResult.isMalformed()) {
                    return runtime.newSymbol("invalid_byte_sequence");
                } else if (result.coderResult.isUnmappable()) {
                    return runtime.newSymbol("undefined_conversion");
                }
            } else {
                if (result.coderResult.isUnderflow()) {
                    if ((flags & PARTIAL_INPUT) == 0) {
                        return runtime.newSymbol("incomplete_input");
                    } else {
                        return runtime.newSymbol("finished");
                    }
                } else if (result.coderResult.isOverflow()) {
                    return runtime.newSymbol("destination_buffer_full");
                }
            }
        }

        return context.runtime.newSymbol("finished");
    }

    @JRubyMethod
    public IRubyObject convert(ThreadContext context, IRubyObject srcBuffer) {
        RubyString srcString = srcBuffer.convertToString();
        boolean is7BitAscii = srcString.isCodeRangeAsciiOnly();
        ByteList srcBL = srcString.getByteList();

        ByteList bytes = transcoder.transcode(context, srcBL, is7BitAscii);

        return context.runtime.newString(bytes);
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject replacement(ThreadContext context) {
        String replacement = transcoder.getCodingErrorActions().getReplaceWith();
        
        if (replacement == null) {
            return context.nil;
        }
        
        return context.runtime.newString(replacement);
    }


    @JRubyMethod(name = "replacement=", compat = RUBY1_9)
    public IRubyObject replacement_set(ThreadContext context, IRubyObject replacement) {
        transcoder.getCodingErrorActions().setReplaceWith(replacement.convertToString().asJavaString());

        return replacement;
    }
    
    @JRubyMethod(compat = RUBY1_9, meta = true)
    public static IRubyObject asciicompat_encoding(ThreadContext context, IRubyObject self, IRubyObject strOrEnc) {
        Ruby runtime = context.runtime;
        EncodingService encodingService = runtime.getEncodingService();
        
        Encoding encoding = encodingService.getEncodingFromObjectNoError(strOrEnc);
        
        if (encoding == null) {
            return context.nil;
        }
        
        if (encoding.isAsciiCompatible()) {
            return context.nil;
        }
        
        Encoding asciiCompat = NONASCII_TO_ASCII.get(encoding);
        
        if (asciiCompat == null) {
            throw runtime.newConverterNotFoundError("no ASCII compatible encoding found for " + strOrEnc);
        }
        
        return encodingService.convertEncodingToRubyEncoding(asciiCompat);
    }
}

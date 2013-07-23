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
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.transcode.Transcoder;
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
        throw context.runtime.newNotImplementedError("custom convpath not supported");
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject dest) {
        return initialize(context, src, dest, context.nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject dest, IRubyObject _opt) {
        Ruby runtime = context.runtime;
        EncodingService encodingService = runtime.getEncodingService();
        
        // both may be null
        Encoding srcEncoding = encodingService.getEncodingFromObjectNoError(src);
        Encoding destEncoding = encodingService.getEncodingFromObjectNoError(dest);
        
        int flags = 0;
        IRubyObject replace = context.nil;

        if (srcEncoding == destEncoding && srcEncoding != null) {
            throw runtime.newConverterNotFoundError("code converter not found (" + srcEncoding + " to " + destEncoding + ")");
        }

        // Ensure we'll be able to get charsets fo these encodings
        try {
            if (srcEncoding != destEncoding) {
                if (srcEncoding != null) encodingService.charsetForEncoding(srcEncoding);
                if (destEncoding != null) encodingService.charsetForEncoding(destEncoding);
            }
        } catch (RaiseException e) {
            if (e.getException().getMetaClass().getBaseName().equals("CompatibilityError")) {
                throw runtime.newConverterNotFoundError("code converter not found (" + srcEncoding + " to " + destEncoding + ")");
            } else {
                throw e;
            }
        }
        
        if (!_opt.isNil()) {
            if (_opt instanceof RubyHash) {
                RubyHash opt = (RubyHash)_opt;
                flags |= optHashToFlags(opt, runtime, flags);
                
                IRubyObject value = opt.fastARef(runtime.newSymbol("replace"));
                if (value != null) {
                    replace = value;
                }
            } else {
                flags = (int)_opt.convertToInteger().getLongValue();
                replace = context.nil;
            }
        }
        
        transcoder = new CharsetTranscoder(context, destEncoding, srcEncoding, flags, replace);

        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.runtime, "#<Encoding::Converter: " + transcoder.fromEncoding + " to " + transcoder.toEncoding);
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
        if (transcoder.fromEncoding == null) return context.nil;
        
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(transcoder.fromEncoding);
    }

    @JRubyMethod
    public IRubyObject destination_encoding(ThreadContext context) {
        if (transcoder.toEncoding == null) return context.nil;
        
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(transcoder.toEncoding);
    }

    @JRubyMethod(required = 2, optional = 4)
    public IRubyObject primitive_convert(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        RubyString input;
        RubyString output;
        int outputByteoffset = -1;
        int outputBytesize = 0;
        int flags = transcoder.getCodingErrorActions().flags;
        
        int hashArg = -1;
        
        if (args.length > 2 && !args[2].isNil()) {
            if (args.length == 3 && args[2] instanceof RubyHash) {
                hashArg = 2;
            } else {
                outputByteoffset = (int)args[2].convertToInteger().getLongValue();
                if (outputByteoffset < 0) throw runtime.newArgumentError("negative offset");
            }
        }
        
        if (args.length > 3 && !args[3].isNil()) {
            if (args.length == 4 && args[3] instanceof RubyHash) {
                hashArg = 3;
            } else {
                outputBytesize = (int)args[3].convertToInteger().getLongValue();
                if (outputBytesize < 0) throw runtime.newArgumentError("negative bytesize");
            }
        }
        
        if (args.length > 4 && !args[4].isNil()) {
            if (args.length > 5 && !args[5].isNil()) {
                throw runtime.newArgumentError(args.length, 5);
            }
            
            if (args[4] instanceof RubyHash) {
                hashArg = 4;
            } else {
                flags = (int)args[4].convertToInteger().getLongValue();
            }
        }
        
        if (hashArg != -1) {
            RubyHash opt = (RubyHash)args[hashArg];
            flags |= optHashToFlags(opt, runtime, flags);
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

        return symbolFromResult(result, runtime, flags, context);
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
    
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject last_error(ThreadContext context) {
        RaiseException lastError = transcoder.getLastError();
        
        if (lastError != null) return lastError.getException();
        
        return context.nil;
    }
    
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject primitive_errinfo(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        CharsetTranscoder.RubyCoderResult lastResult = transcoder.getLastResult();
        
        // if we have not done anything, produce an empty errinfo
        if (lastResult == null) {
            return runtime.newArray(new IRubyObject[] {
               runtime.newSymbol("source_buffer_empty"),
               context.nil,
               context.nil,
               context.nil,
               context.nil
            });
        }
        
        RubyArray errinfo = RubyArray.newArray(context.runtime);
        
        errinfo.append(runtime.newSymbol(lastResult.stringResult));
        
        // FIXME: gross
        errinfo.append(runtime.newString(lastResult.inCharset.name()));
        errinfo.append(runtime.newString(lastResult.outCharset.name()));
        
        if (lastResult.isError() && lastResult.errorBytes != null) {
            // FIXME: do this elsewhere and cache it
            ByteList errorBytes = new ByteList(lastResult.errorBytes, runtime.getEncodingService().getEncodingFromString(lastResult.inCharset.name()), true);
            errinfo.append(RubyString.newString(runtime, errorBytes));
        } else {
            errinfo.append(RubyString.newEmptyString(runtime));
        }

        if (lastResult.readagainBytes != null) {
            // FIXME: do this elsewhere and cache it
            ByteList readagainBytes = new ByteList(lastResult.readagainBytes, runtime.getEncodingService().getEncodingFromString(lastResult.inCharset.name()), true);
            errinfo.append(RubyString.newString(runtime, readagainBytes));
        } else {
            errinfo.append(RubyString.newEmptyString(runtime));
        }
        
        return errinfo;
    }

    private IRubyObject symbolFromResult(CharsetTranscoder.RubyCoderResult result, Ruby runtime, int flags, ThreadContext context) {
        if (result != null) {
            return runtime.newSymbol(result.stringResult);
        }

        if ((flags & PARTIAL_INPUT) == 0) {
            return context.runtime.newSymbol("finished");
        } else {
            return context.runtime.newSymbol("source_buffer_empty");
        }
    }

    public static int optHashToFlags(RubyHash opt, Ruby runtime, int flags) {
        IRubyObject value = opt.fastARef(runtime.newSymbol("partial_input"));
        if (value != null) {
            if (value.isTrue()) {
                flags |= PARTIAL_INPUT;
            } else {
                flags &= ~PARTIAL_INPUT;
            }
        }
        value = opt.fastARef(runtime.newSymbol("after_output"));
        if (value != null) {
            if (value.isTrue()) {
                flags |= AFTER_OUTPUT;
            } else {
                flags &= ~AFTER_OUTPUT;
            }
        }
        
        value = opt.fastARef(runtime.newSymbol("invalid"));
        if (value != null) {
            if (value.isNil()) {
                flags &= ~INVALID_REPLACE;
            } else {
                if (value.equals(runtime.newSymbol("replace"))) {
                    flags |= INVALID_REPLACE;
                } else {
                    throw runtime.newArgumentError("unknown value for invalid character option");
                }
            }
        }
        value = opt.fastARef(runtime.newSymbol("undef"));
        if (value != null) {
            if (value.isNil()) {
                flags &= ~UNDEF_REPLACE;
            } else {
                if (value.equals(runtime.newSymbol("replace"))) {
                    flags |= UNDEF_REPLACE;
                } else {
                    throw runtime.newArgumentError("unknown value for undefined character option");
                }
            }
        }
        value = opt.fastARef(runtime.newSymbol("universal_newline"));
        if (value != null) {
            if (value.isTrue()) {
                flags |= UNIVERSAL_NEWLINE_DECORATOR;
            } else {
                flags &= ~UNIVERSAL_NEWLINE_DECORATOR;
            }
        }
        value = opt.fastARef(runtime.newSymbol("crlf_newline"));
        if (value != null) {
            if (value.isTrue()) {
                flags |= CRLF_NEWLINE_DECORATOR;
            } else {
                flags &= ~CRLF_NEWLINE_DECORATOR;
            }
        }
        value = opt.fastARef(runtime.newSymbol("cr_newline"));
        if (value != null) {
            if (value.isTrue()) {
                flags |= CR_NEWLINE_DECORATOR;
            } else {
                flags &= ~CR_NEWLINE_DECORATOR;
            }
        }
        value = opt.fastARef(runtime.newSymbol("xml"));
        if (value != null) {
            String valueString = value.toString();
            if (value.equals(runtime.newSymbol("text"))) {
                flags |= XML_TEXT_DECORATOR;
            } else if (value.equals(runtime.newSymbol("attr"))) {
                flags |= XML_ATTR_CONTENT_DECORATOR;
            } else {
                throw runtime.newArgumentError("unexpected value for xml option: " + value.toString());
            }
        }
        
        return flags;
    }
    
    public static class EncodingErrorMethods {
        @JRubyMethod
        public static IRubyObject error_char(ThreadContext context, IRubyObject self) {
            CharsetTranscoder.RubyCoderResult result = (CharsetTranscoder.RubyCoderResult)self.dataGetStruct();
            
            if (result != null && result.isError() && result.errorBytes != null) {
                // FIXME: do this elsewhere and cache it
                ByteList errorBytes = new ByteList(result.errorBytes, context.runtime.getEncodingService().getEncodingFromString(result.inCharset.name()), true);
                return RubyString.newString(context.runtime, errorBytes);
            }
        
            return context.nil;
        }
        
        @JRubyMethod
        public static IRubyObject readagain_bytes(ThreadContext context, IRubyObject self) {
            CharsetTranscoder.RubyCoderResult result = (CharsetTranscoder.RubyCoderResult)self.dataGetStruct();
            
            if (result != null && result.isError() && result.readagainBytes != null) {
                // FIXME: do this elsewhere and cache it
                ByteList errorBytes = new ByteList(result.readagainBytes, ASCIIEncoding.INSTANCE, true);
                return RubyString.newString(context.runtime, errorBytes);
            }
        
            return context.nil;
        }
        
        @JRubyMethod(name = "incomplete_input?")
        public static IRubyObject incomplete_input_p(ThreadContext context, IRubyObject self) {
            CharsetTranscoder.RubyCoderResult result = (CharsetTranscoder.RubyCoderResult)self.dataGetStruct();
            
            if (result != null && result.isInvalid()) {
                // FIXME: do this elsewhere and cache it
                ByteList errorBytes = new ByteList(result.readagainBytes, ASCIIEncoding.INSTANCE, true);
                return RubyString.newString(context.runtime, errorBytes);
            }
        
            return context.nil;
        }
        
        @JRubyMethod
        public static IRubyObject source_encoding(ThreadContext context, IRubyObject self) {
            CharsetTranscoder.RubyCoderResult result = (CharsetTranscoder.RubyCoderResult)self.dataGetStruct();
            
            return context.runtime.getEncodingService().convertEncodingToRubyEncoding(context.runtime.getEncodingService().getEncodingFromString(result.inCharset.name()));
        }
        
        @JRubyMethod
        public static IRubyObject source_encoding_name(ThreadContext context, IRubyObject self) {
            CharsetTranscoder.RubyCoderResult result = (CharsetTranscoder.RubyCoderResult)self.dataGetStruct();
            
            return context.runtime.newString(result.inCharset.name());
        }
        
        @JRubyMethod
        public static IRubyObject destination_encoding(ThreadContext context, IRubyObject self) {
            CharsetTranscoder.RubyCoderResult result = (CharsetTranscoder.RubyCoderResult)self.dataGetStruct();
            
            return context.runtime.getEncodingService().convertEncodingToRubyEncoding(context.runtime.getEncodingService().getEncodingFromString(result.outCharset.name()));
        }
        
        @JRubyMethod
        public static IRubyObject destination_encoding_name(ThreadContext context, IRubyObject self) {
            CharsetTranscoder.RubyCoderResult result = (CharsetTranscoder.RubyCoderResult)self.dataGetStruct();
            
            return context.runtime.newString(result.outCharset.name());
        }
    }
}

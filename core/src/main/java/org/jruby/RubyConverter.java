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

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.Ptr;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jcodings.transcode.Transcoder;
import org.jcodings.transcode.TranscoderDB;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;

import java.util.HashMap;
import java.util.Map;

import static org.jruby.runtime.Visibility.PRIVATE;

@JRubyClass(name="Converter")
public class RubyConverter extends RubyObject {
    private EConv ec;
    
    @JRubyConstant
    public static final int INVALID_MASK = EConvFlags.INVALID_MASK;
    @JRubyConstant
    public static final int INVALID_REPLACE = EConvFlags.INVALID_REPLACE;
    @JRubyConstant
    public static final int UNDEF_MASK = EConvFlags.UNDEF_MASK;
    @JRubyConstant
    public static final int UNDEF_REPLACE = EConvFlags.UNDEF_REPLACE;
    @JRubyConstant
    public static final int UNDEF_HEX_CHARREF = EConvFlags.UNDEF_HEX_CHARREF;
    @JRubyConstant
    public static final int PARTIAL_INPUT = EConvFlags.PARTIAL_INPUT;
    @JRubyConstant
    public static final int AFTER_OUTPUT = EConvFlags.AFTER_OUTPUT;
    @JRubyConstant
    public static final int UNIVERSAL_NEWLINE_DECORATOR = EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
    @JRubyConstant
    public static final int CRLF_NEWLINE_DECORATOR = EConvFlags.CRLF_NEWLINE_DECORATOR;
    @JRubyConstant
    public static final int CR_NEWLINE_DECORATOR = EConvFlags.CR_NEWLINE_DECORATOR;
    @JRubyConstant
    public static final int XML_TEXT_DECORATOR = EConvFlags.XML_TEXT_DECORATOR;
    @JRubyConstant
    public static final int XML_ATTR_CONTENT_DECORATOR = EConvFlags.XML_ATTR_CONTENT_DECORATOR;
    @JRubyConstant
    public static final int XML_ATTR_QUOTE_DECORATOR = EConvFlags.XML_ATTR_QUOTE_DECORATOR;
    
    // TODO: This is a little ugly...we should have a table of these in jcodings.
    public static final Map<Encoding, Encoding> NONASCII_TO_ASCII = new HashMap<Encoding, Encoding>();
    static {
        NONASCII_TO_ASCII.put(UTF16BEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(UTF16LEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(UTF32BEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(UTF32LEEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(
                EncodingDB.getEncodings().get("CP50220".getBytes()).getEncoding(),
                EncodingDB.getEncodings().get("CP51932".getBytes()).getEncoding());
        NONASCII_TO_ASCII.put(
                EncodingDB.getEncodings().get("CP50221".getBytes()).getEncoding(),
                EncodingDB.getEncodings().get("CP51932".getBytes()).getEncoding());
        NONASCII_TO_ASCII.put(EncodingDB.getEncodings().get("IBM037".getBytes()).getEncoding(), ISO8859_1Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(EncodingDB.getEncodings().get("UTF-16".getBytes()).getEncoding(), UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(EncodingDB.getEncodings().get("UTF-32".getBytes()).getEncoding(), UTF8Encoding.INSTANCE);
        NONASCII_TO_ASCII.put(
                EncodingDB.getEncodings().get("ISO-2022-JP".getBytes()).getEncoding(),
                EncodingDB.getEncodings().get("stateless-ISO-2022-JP".getBytes()).getEncoding());
        NONASCII_TO_ASCII.put(
                EncodingDB.getEncodings().get("ISO-2022-JP-KDDI".getBytes()).getEncoding(),
                EncodingDB.getEncodings().get("stateless-ISO-2022-JP-KDDI".getBytes()).getEncoding());
    }

    public static RubyClass createConverterClass(Ruby runtime) {
        RubyClass converterc = runtime.defineClassUnder("Converter", runtime.getData(), RubyConverter::new, runtime.getEncoding());

        converterc.setClassIndex(ClassIndex.CONVERTER);
        converterc.setReifiedClass(RubyConverter.class);
        converterc.kindOf = new RubyModule.JavaClassKindOf(RubyConverter.class);

        converterc.defineAnnotatedMethods(RubyConverter.class);
        converterc.defineAnnotatedConstants(RubyConverter.class);

        return converterc;
    }

    public RubyConverter(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyConverter(Ruby runtime) {
        super(runtime, runtime.getConverter());
    }

    @JRubyMethod(visibility = PRIVATE, required = 1, optional = 2)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        Encoding[] encs = {null, null};
        byte[][] encNames = {null, null};
        int[] ecflags = {0};
        IRubyObject[] ecopts = {context.nil};

        IRubyObject convpath;

        if (ec != null) {
            throw runtime.newTypeError("already initialized");
        }

        if (args.length == 1 && !(convpath = args[0].checkArrayType()).isNil()) {
            ec = EncodingUtils.econvInitByConvpath(context, convpath, encNames, encs);
            ecflags[0] = 0;
            ecopts[0] = context.nil;
        } else {
            EncodingUtils.econvArgs(context, args, encNames, encs, ecflags, ecopts);
            ec = EncodingUtils.econvOpenOpts(context, encNames[0], encNames[1], ecflags[0], ecopts[0]);
        }

        if (ec == null) {
            throw EncodingUtils.econvOpenExc(context, encNames[0], encNames[1], ecflags[0]);
        }

        if (!EncodingUtils.DECORATOR_P(encNames[0], encNames[1])) {
            if (encs[0] == null) {
                encs[0] = EncodingDB.dummy(encNames[0]).getEncoding();
            }
            if (encs[1] == null) {
                encs[1] = EncodingDB.dummy(encNames[1]).getEncoding();
            }
        }

        ec.sourceEncoding = encs[0];
        ec.destinationEncoding = encs[1];

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.runtime, "#<Encoding::Converter: " + ec.sourceEncoding + " to " + ec.destinationEncoding + ">");
    }

    @JRubyMethod
    public IRubyObject convpath(ThreadContext context) {
        Ruby runtime = context.runtime;

        RubyArray result = runtime.newArray();

        for (int i = 0; i < ec.numTranscoders; i++) {
            Transcoder tr = ec.elements[i].transcoding.transcoder;
            IRubyObject v;
            if (EncodingUtils.DECORATOR_P(tr.getSource(), tr.getDestination())) {
                v = RubyString.newString(runtime, tr.getDestination());
            } else {
                v = runtime.newArray(
                        runtime.getEncodingService().convertEncodingToRubyEncoding(runtime.getEncodingService().findEncodingOrAliasEntry(tr.getSource()).getEncoding()),
                        runtime.getEncodingService().convertEncodingToRubyEncoding(runtime.getEncodingService().findEncodingOrAliasEntry(tr.getDestination()).getEncoding()));
            }
            result.push(v);
        }

        return result;
    }

    @JRubyMethod
    public IRubyObject source_encoding(ThreadContext context) {
        if (ec.sourceEncoding == null) return context.nil;
        
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(ec.sourceEncoding);
    }

    @JRubyMethod
    public IRubyObject destination_encoding(ThreadContext context) {
        if (ec.destinationEncoding == null) return context.nil;
        
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(ec.destinationEncoding);
    }

    // econv_primitive_convert
    @JRubyMethod(required = 2, optional = 4)
    public IRubyObject primitive_convert(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        RubyString input = null;
        RubyString output;
        IRubyObject outputByteOffsetObj = context.nil;
        IRubyObject outputBytesizeObj = context.nil;
        int outputByteoffset = -1;
        int outputBytesize = 0;
        int flags = 0;
        
        int hashArg = -1;
        
        if (args.length > 2 && !args[2].isNil()) {
            if (args.length == 3 && args[2] instanceof RubyHash) {
                hashArg = 2;
            } else {
                outputByteOffsetObj = args[2];
                outputByteoffset = (int)args[2].convertToInteger().getLongValue();
            }
        }
        
        if (args.length > 3 && !args[3].isNil()) {
            if (args.length == 4 && args[3] instanceof RubyHash) {
                hashArg = 3;
            } else {
                outputBytesizeObj = args[3];
                outputBytesize = (int)args[3].convertToInteger().getLongValue();
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
        
        IRubyObject opt;
        if (hashArg != -1 &&
                !(opt = TypeConverter.checkHashType(runtime, args[hashArg])).isNil()) {
            IRubyObject v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("partial_input"));
            if (v.isTrue()) {
                flags |= EConvFlags.PARTIAL_INPUT;
            }
            v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("after_output"));
            if (v.isTrue()) {
                flags |= EConvFlags.AFTER_OUTPUT;
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

        Ptr inPtr = new Ptr();
        Ptr outPtr = new Ptr();

        if (outputBytesizeObj.isNil()) {
            outputBytesize = 16; // in MRI, this is RSTRING_EMBED_LEN_MAX
            if (input != null && outputBytesize < input.getByteList().getRealSize()) {
                outputBytesize = input.getByteList().getRealSize();
            }
        }

        while (true) {
            if (outputByteOffsetObj.isNil()) {
                outputByteoffset = outBytes.getRealSize();
            }

            if (outputByteoffset < 0) {
                throw runtime.newArgumentError("negative output offset");
            }

            if (outBytes.getRealSize() < outputByteoffset) {
                throw runtime.newArgumentError("output offset too big");
            }

            if (outputBytesize < 0) {
                throw runtime.newArgumentError("negative bytesize");
            }

            long outputByteEnd = outputByteoffset + outputBytesize;

            if (outputByteEnd > Integer.MAX_VALUE) {
                // overflow check
                throw runtime.newArgumentError("output offset + bytesize too big");
            }

            outBytes.ensure((int)outputByteEnd);

            inPtr.p = inBytes.getBegin();
            outPtr.p = outBytes.getBegin() + outputByteoffset;
            int os = outPtr.p + outputBytesize;
            EConvResult res = ec.convert(inBytes.getUnsafeBytes(), inPtr, inBytes.getRealSize() + inPtr.p, outBytes.getUnsafeBytes(), outPtr, os, flags);

            outBytes.setRealSize(outPtr.p - outBytes.begin());

            if (input != null) {
                input.getByteList().setRealSize(inBytes.getRealSize() - (inPtr.p - inBytes.getBegin()));
                input.getByteList().setBegin(inPtr.p);
            }

            if (outputBytesizeObj.isNil() && res == EConvResult.DestinationBufferFull) {
                if (Integer.MAX_VALUE / 2 < outputBytesize) {
                    throw runtime.newArgumentError("too long conversion result");
                }
                outputBytesize *= 2;
                outputByteOffsetObj = context.nil;
                continue;
            }

            if (ec.destinationEncoding != null) {
                outBytes.setEncoding(ec.destinationEncoding);
            }

            return runtime.newSymbol(res.symbolicName());
        }
    }

    @JRubyMethod
    public IRubyObject convert(ThreadContext context, IRubyObject srcBuffer) {
        Ruby runtime = context.runtime;
        RubyString orig = srcBuffer.convertToString();
        IRubyObject dest;

        IRubyObject[] newArgs = {
                orig.dup(),
                dest = runtime.newString(),
                context.nil,
                context.nil,
                runtime.newFixnum(EConvFlags.PARTIAL_INPUT)
        };

        IRubyObject ret = primitive_convert(context, newArgs);

        if (ret instanceof RubySymbol) {
            RubySymbol retSym = (RubySymbol)ret;
            String retStr = retSym.asJavaString(); // 7bit comparison

            if (retStr.equals(EConvResult.InvalidByteSequence.symbolicName()) ||
                    retStr.equals(EConvResult.UndefinedConversion.symbolicName()) ||
                    retStr.equals(EConvResult.IncompleteInput.symbolicName())) {
                throw EncodingUtils.makeEconvException(runtime, ec);
            }

            if (retStr.equals(EConvResult.Finished.symbolicName())) {
                throw runtime.newArgumentError("converter already finished");
            }

            if (!retStr.equals(EConvResult.SourceBufferEmpty.symbolicName())) {
                throw runtime.newRuntimeError("bug: unexpected result of primitive_convert: " + retSym);
            }
        }

        dest.infectBy(orig);

        return dest;
    }
    
    @JRubyMethod
    public IRubyObject finish(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject dest;

        IRubyObject[] newArgs = {
                context.nil,
                dest = runtime.newString(),
                context.nil,
                context.nil,
                runtime.newFixnum(0)
        };

        IRubyObject ret = primitive_convert(context, newArgs);

        if (ret instanceof RubySymbol) {
            RubySymbol retSym = (RubySymbol)ret;
            String retStr = retSym.asJavaString(); // 7 bit comparison

            if (retStr.equals(EConvResult.InvalidByteSequence.symbolicName()) ||
                    retStr.equals(EConvResult.UndefinedConversion.symbolicName()) ||
                    retStr.equals(EConvResult.IncompleteInput.symbolicName())) {
                throw EncodingUtils.makeEconvException(runtime, ec);
            }

            if (!retStr.equals(EConvResult.Finished.symbolicName())) {
                throw runtime.newRuntimeError("bug: unexpected result of primitive_convert");
            }
        }

        return dest;
    }

    @JRubyMethod
    public IRubyObject replacement(ThreadContext context) {
        int ret = ec.makeReplacement();

        if (ret == -1) {
            throw context.runtime.newUndefinedConversionError("replacement character setup failed");
        }

        return context.runtime.newString(new ByteList(
                ec.replacementString,
                0,
                ec.replacementLength,
                context.runtime.getEncodingService().findEncodingOrAliasEntry(ec.replacementEncoding).getEncoding(), true));
    }

    @JRubyMethod(name = "replacement=")
    public IRubyObject replacement_set(ThreadContext context, IRubyObject arg) {
        RubyString string = arg.convertToString();
        ByteList stringBytes = string.getByteList();
        Encoding enc = string.getEncoding();

        int ret = ec.setReplacement(stringBytes.getUnsafeBytes(), stringBytes.getBegin(), stringBytes.getRealSize(), enc.getName());

        if (ret == -1) {
            throw context.runtime.newUndefinedConversionError("replacement character setup failed");
        }

        return arg;
    }
    
    @JRubyMethod(meta = true)
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
    
    @JRubyMethod
    public IRubyObject last_error(ThreadContext context) {
        RaiseException re = EncodingUtils.makeEconvException(context.runtime, ec);

        if (re != null) return re.getException();
        
        return context.nil;
    }
    
    @JRubyMethod
    public IRubyObject primitive_errinfo(ThreadContext context) {
        Ruby runtime = context.runtime;

        IRubyObject[] values = {
                runtime.newSymbol(ec.lastError.getResult().symbolicName()),
                context.nil,
                context.nil,
                context.nil,
                context.nil
        };

        if (ec.lastError.getSource() != null) {
            values[1] = RubyString.newString(runtime, ec.lastError.getSource());
        }

        if (ec.lastError.getDestination() != null) {
            values[2] = RubyString.newString(runtime, ec.lastError.getDestination());
        }

        if (ec.lastError.getErrorBytes() != null) {
            values[3] = RubyString.newString(runtime, ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP(), ec.lastError.getErrorBytesLength());
            values[4] = RubyString.newString(runtime, ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP() + ec.lastError.getErrorBytesLength(), ec.lastError.getReadAgainLength());
        }
        
        RubyArray ary = RubyArray.newArrayMayCopy(context.runtime, values);

        return ary;
    }

    @JRubyMethod(meta = true, required = 2, optional = 1)
    public static IRubyObject search_convpath(ThreadContext context, IRubyObject self, IRubyObject[] argv) {
        final Ruby runtime = context.runtime;
        final IRubyObject nil = context.nil;
        final byte[][] encNames = {null, null};
        final Encoding[] encs = {null, null};
        final int[] ecflags_p = {0};
        final IRubyObject[] ecopts_p = {context.nil};
        final IRubyObject[] convpath = {nil};

        EncodingUtils.econvArgs(context, argv, encNames, encs, ecflags_p, ecopts_p);

        TranscoderDB.searchPath(encNames[0], encNames[1], new TranscoderDB.SearchPathCallback() {
            final EncodingService es = runtime.getEncodingService();

            public void call(byte[] source, byte[] destination, int depth) {
                IRubyObject v;

                if (convpath[0] == nil) {
                    convpath[0] = runtime.newArray();
                }

                if (EncodingUtils.DECORATOR_P(encNames[0], encNames[1])) {
                    v = RubyString.newString(runtime, encNames[2]);
                } else {
                    v = runtime.newArray(
                            es.convertEncodingToRubyEncoding(es.findEncodingOrAliasEntry(source).getEncoding()),
                            es.convertEncodingToRubyEncoding(es.findEncodingOrAliasEntry(destination).getEncoding()));
                }

                ((RubyArray)convpath[0]).store(depth, v);
            }
        });

        if (convpath[0].isNil()) {
            throw EncodingUtils.econvOpenExc(context, encNames[0], encNames[1], 0);
        }

        if (EncodingUtils.decorateConvpath(context, convpath[0], ecflags_p[0]) == -1) {
            throw EncodingUtils.econvOpenExc(context, encNames[0], encNames[1], ecflags_p[0]);
        }

        return convpath[0];
    }

    // econv_insert_output
    @JRubyMethod
    public IRubyObject insert_output(ThreadContext context, IRubyObject string) {
        Ruby runtime = context.runtime;
        byte[] insertEnc;

        int ret;

        string = string.convertToString();
        insertEnc = ec.encodingToInsertOutput();
        string = EncodingUtils.rbStrEncode(
                context,
                string,
                runtime.getEncodingService().findEncodingObject(insertEnc),
                0,
                context.nil);

        ByteList stringBL = ((RubyString)string).getByteList();
        ret = ec.insertOutput(stringBL.getUnsafeBytes(), stringBL.getBegin(), stringBL.getRealSize(), insertEnc);
        if (ret == -1) {
            throw runtime.newArgumentError("too big string");
        }

        return context.nil;
    }

    // econv_putback
    @JRubyMethod(required = 0, optional = 1)
    public IRubyObject putback(ThreadContext context, IRubyObject[] argv)
    {
        Ruby runtime = context.runtime;
        int n;
        int putbackable;
        IRubyObject str, max;

        if (argv.length == 0) {
            max = context.nil;
        } else {
            max = argv[0];
        }

        if (max.isNil()) {
            n = ec.putbackable();
        } else {
            n = (int)max.convertToInteger().getLongValue();
            putbackable = ec.putbackable();
            if (putbackable < n) {
                n = putbackable;
            }
        }

        str = RubyString.newStringLight(runtime, n);
        ByteList strBL = ((RubyString)str).getByteList();
        ec.putback(strBL.getUnsafeBytes(), strBL.getBegin(), n);
        strBL.setRealSize(n);

        if (ec.sourceEncoding != null) {
            ((RubyString)str).setEncoding(ec.sourceEncoding);
        }

        return str;
    }

    // econv_equal
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        EConv ec1 = ec;
        EConv ec2;
        int i;

        if (!(other instanceof RubyConverter)) return context.nil;

        ec2 = ((RubyConverter)other).ec;

        return RubyBoolean.newBoolean(context, ec1.equals(ec2));
    }
    
    public static class EncodingErrorMethods {
        @JRubyMethod
        public static IRubyObject source_encoding(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "source_encoding");
        }
        
        @JRubyMethod
        public static IRubyObject source_encoding_name(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "source_encoding_name");
        }
        
        @JRubyMethod
        public static IRubyObject destination_encoding(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "destination_encoding");
        }
        
        @JRubyMethod
        public static IRubyObject destination_encoding_name(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "destination_encoding_name");
        }
    }

    public static class UndefinedConversionErrorMethods {
        @JRubyMethod
        public static IRubyObject error_char(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "error_char");
        }
    }

    public static class InvalidByteSequenceErrorMethods {
        @JRubyMethod
        public static IRubyObject readagain_bytes(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "readagain_bytes");
        }

        @JRubyMethod(name = "incomplete_input?")
        public static IRubyObject incomplete_input_p(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "incomplete_input");
        }

        @JRubyMethod
        public static IRubyObject error_bytes(ThreadContext context, IRubyObject self) {
            return Helpers.getInstanceVariableNoWarn(self, context, "error_bytes");
        }
    }
}

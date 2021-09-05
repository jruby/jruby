package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.Ptr;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.exception.EncodingError;
import org.jcodings.exception.EncodingException;
import org.jcodings.exception.ErrorCodes;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
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
import org.jcodings.transcode.Transcoding;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyConverter;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyMethod;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.ByteList;
import org.jruby.util.ByteListHolder;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jruby.RubyString.*;
import static org.jruby.util.StringSupport.CR_UNKNOWN;
import static org.jruby.util.StringSupport.searchNonAscii;

public class EncodingUtils {
    public static final int ECONV_DEFAULT_NEWLINE_DECORATOR = Platform.IS_WINDOWS ? EConvFlags.CRLF_NEWLINE_DECORATOR : 0;
    public static final int DEFAULT_TEXTMODE = Platform.IS_WINDOWS ? OpenFile.TEXTMODE : 0;
    public static final int TEXTMODE_NEWLINE_DECORATOR_ON_WRITE = Platform.IS_WINDOWS ? EConvFlags.CRLF_NEWLINE_DECORATOR : 0;

    private static final byte[] NULL_BYTE_ARRAY = ByteList.NULL_ARRAY;

    // rb_to_encoding
    public static Encoding rbToEncoding(ThreadContext context, IRubyObject enc) {
        if (enc instanceof RubyEncoding) return ((RubyEncoding) enc).getEncoding();

        return toEncoding(context, enc);
    }

    // to_encoding
    public static Encoding toEncoding(ThreadContext context, IRubyObject enc) {
        RubyString encStr = enc.convertToString();
        if (!encStr.getEncoding().isAsciiCompatible()) {
            throw context.runtime.newArgumentError("invalid name encoding (non ASCII)");
        }
        Encoding idx = context.runtime.getEncodingService().getEncodingFromObject(encStr);
        // check for missing encoding is in getEncodingFromObject
        return idx;
    }

    public static IRubyObject[] openArgsToArgs(Ruby runtime, IRubyObject firstElement, RubyHash options) {
        IRubyObject value = hashARef(runtime, options, "open_args");

        if (value.isNil()) return new IRubyObject[] { firstElement, options };

        RubyArray array = value.convertToArray();

        IRubyObject[] openArgs = new IRubyObject[array.size()];
        value.convertToArray().toArray(openArgs);
        IRubyObject[] args = new IRubyObject[openArgs.length + 1];

        args[0] = firstElement;

        System.arraycopy(openArgs, 0, args, 1, openArgs.length);

        return args;
    }

    // FIXME: This could be smarter amount determining whether optionsArg is a RubyHash and !null (invariant)
    // mri: extract_binmode
    public static void extractBinmode(Ruby runtime, IRubyObject optionsArg, int[] fmode_p) {
        int fmodeMask = 0;

        IRubyObject v = hashARef(runtime, optionsArg, "textmode");
        if (!v.isNil() && v.isTrue()) fmodeMask |= OpenFile.TEXTMODE;

        v = hashARef(runtime, optionsArg, "binmode");
        if (!v.isNil() && v.isTrue()) fmodeMask |= OpenFile.BINMODE;

        if ((fmodeMask & OpenFile.BINMODE) != 0 && (fmodeMask & OpenFile.TEXTMODE) != 0) {
            throw runtime.newArgumentError("both textmode and binmode specified");
        }

        fmode_p[0] |= fmodeMask;
    }

    private static IRubyObject hashARef(Ruby runtime, IRubyObject hash, String symbol) {
        if (hash == null || !(hash instanceof RubyHash)) return runtime.getNil();

        IRubyObject value = ((RubyHash) hash).fastARef(runtime.newSymbol(symbol));

        return value == null ? runtime.getNil() : value;
    }

    public static Encoding ascii8bitEncoding(Ruby runtime) {
        return runtime.getEncodingService().getAscii8bitEncoding();
    }

    static final int VMODE = 0;
    static final int PERM = 1;

    public static Object vmodeVperm(IRubyObject vmode, IRubyObject vperm) {
        return new IRubyObject[] {vmode, vperm};
    }

    public static IRubyObject vmode(Object vmodeVperm) {
        return ((IRubyObject[])vmodeVperm)[VMODE];
    }

    public static void vmode(Object vmodeVperm, IRubyObject vmode) {
        ((IRubyObject[])vmodeVperm)[VMODE] = vmode;
    }

    public static IRubyObject vperm(Object vmodeVperm) {
        return ((IRubyObject[])vmodeVperm)[PERM];
    }

    public static void vperm(Object vmodeVperm, IRubyObject vperm) {
        ((IRubyObject[])vmodeVperm)[PERM] = vperm;
    }

    public static final int MODE_BTMODE(int fmode, int a, int b, int c) {
        if ((fmode & OpenFile.BINMODE) != 0) {
            return b;
        } else if ((fmode & OpenFile.TEXTMODE) != 0) {
            return c;
        }
        return a;
    }

    public static int SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(Encoding enc2, int ecflags) {
        if (enc2 != null && (ecflags & ECONV_DEFAULT_NEWLINE_DECORATOR) != 0) {
            return ecflags | EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
        }
        return ecflags;
    }

    /*
     * This is a wacky method which is a very near port from MRI.  pm passes in
     * a permissions value and a mode value.  As a side-effect mode will get set
     * if this found any 'mode'-like stuff so the caller can know whether mode
     * has been handled yet.   The same story for permission value.  If it has
     * not been set then we know it needs to default permissions from the caller.
     */
    // mri: rb_io_extract_modeenc
    public static void extractModeEncoding(ThreadContext context,
            IOEncodable ioEncodable, Object vmodeAndVperm_p, IRubyObject options, int[] oflags_p, int[] fmode_p) {
        Ruby runtime = context.runtime;
        int ecflags;
        IRubyObject[] ecopts_p = {context.nil};
        boolean hasEnc = false, hasVmode = false;
        IRubyObject intmode;

        // Give default encodings
        ioExtIntToEncs(context, ioEncodable, null, null, 0);

        vmode_handle: do {
            if (vmode(vmodeAndVperm_p) == null || vmode(vmodeAndVperm_p).isNil()) {
                fmode_p[0] = OpenFile.READABLE;
                oflags_p[0] = ModeFlags.RDONLY;
            } else {
                intmode = TypeConverter.checkIntegerType(context, vmode(vmodeAndVperm_p));

                if (!intmode.isNil()) {
                    vmode(vmodeAndVperm_p, intmode);
                    oflags_p[0] = RubyNumeric.num2int(intmode);
                    fmode_p[0] = ModeFlags.getOpenFileFlagsFor(oflags_p[0]);
                } else {
                    String p = vmode(vmodeAndVperm_p).convertToString().asJavaString();
                    fmode_p[0] = OpenFile.ioModestrFmode(runtime, p);
                    oflags_p[0] = OpenFile.ioFmodeOflags(fmode_p[0]);
                    int colonSplit = p.indexOf(":");

                    if (colonSplit != -1) {
                        hasEnc = true;
                        parseModeEncoding(context, ioEncodable, p.substring(colonSplit + 1), fmode_p);
                    } else {
                        Encoding e = (fmode_p[0] & OpenFile.BINMODE) != 0 ? ascii8bitEncoding(runtime) : null;
                        ioExtIntToEncs(context, ioEncodable, e, null, fmode_p[0]);
                    }
                }
            }

            if (options == null || options.isNil()) {
                ecflags = (fmode_p[0] & OpenFile.READABLE) != 0
                        ? MODE_BTMODE(fmode_p[0], ECONV_DEFAULT_NEWLINE_DECORATOR, 0, EConvFlags.UNIVERSAL_NEWLINE_DECORATOR)
                        : 0;
                if (TEXTMODE_NEWLINE_DECORATOR_ON_WRITE != 0) {
                    ecflags |= (fmode_p[0] & OpenFile.WRITABLE) != 0
                            ? MODE_BTMODE(fmode_p[0], TEXTMODE_NEWLINE_DECORATOR_ON_WRITE, 0, TEXTMODE_NEWLINE_DECORATOR_ON_WRITE)
                            : 0;
                }
                ecflags = SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(ioEncodable.getEnc2(), ecflags);
                ecopts_p[0] = context.nil;
            } else {
                if (!hasVmode) {
                    IRubyObject v = ((RubyHash) options).op_aref(context, runtime.newSymbol("mode"));
                    if (!v.isNil()) {
                        if (vmode(vmodeAndVperm_p) != null && !vmode(vmodeAndVperm_p).isNil()) {
                            throw runtime.newArgumentError("mode specified twice");
                        }
                        hasVmode = true;
                        vmode(vmodeAndVperm_p, v);
                        continue vmode_handle;
                    }
                }

                IRubyObject v = ((RubyHash) options).op_aref(context, runtime.newSymbol("flags"));
                if (!v.isNil()) {
                    v = v.convertToInteger();
                    oflags_p[0] |= RubyNumeric.num2int(v);
                    vmode(vmodeAndVperm_p, runtime.newFixnum(oflags_p[0]));
                    fmode_p[0] = ModeFlags.getOpenFileFlagsFor(oflags_p[0]);
                }

                extractBinmode(runtime, options, fmode_p);
                // Differs from MRI but we open with ModeFlags
                if ((fmode_p[0] & OpenFile.BINMODE) != 0) {
                    oflags_p[0] |= ModeFlags.BINARY;

                    if (!hasEnc) {
                        ioExtIntToEncs(context, ioEncodable, ascii8bitEncoding(runtime), null, fmode_p[0]);
                    }
                } else if (DEFAULT_TEXTMODE != 0 && (vmode(vmodeAndVperm_p) == null || vmode(vmodeAndVperm_p).isNil())) {
                    fmode_p[0] |= DEFAULT_TEXTMODE;
                }
                
                v = hashARef(runtime, options, "perm");
                if (!v.isNil()) {
                    if (vperm(vmodeAndVperm_p) != null) {
                        if (!vperm(vmodeAndVperm_p).isNil()) throw runtime.newArgumentError("perm specified twice");

                        vperm(vmodeAndVperm_p, v);
                    }
                }

                IRubyObject extraFlags = hashARef(runtime, options, "flags");
                if (!extraFlags.isNil()) {
                    oflags_p[0] |= extraFlags.convertToInteger().getIntValue();
                }

                ecflags = (fmode_p[0] & OpenFile.READABLE) != 0 ?
                        MODE_BTMODE(fmode_p[0], ECONV_DEFAULT_NEWLINE_DECORATOR, 0, EConvFlags.UNIVERSAL_NEWLINE_DECORATOR) : 0;
                if (TEXTMODE_NEWLINE_DECORATOR_ON_WRITE != -1) {
                    ecflags |= (fmode_p[0] & OpenFile.WRITABLE) != 0 ?
                            MODE_BTMODE(fmode_p[0], TEXTMODE_NEWLINE_DECORATOR_ON_WRITE, 0, TEXTMODE_NEWLINE_DECORATOR_ON_WRITE) : 0;
                }

                if (ioExtractEncodingOption(context, ioEncodable, options, fmode_p)) {
                    if (hasEnc) throw runtime.newArgumentError("encoding specified twice");
                }

                ecflags = SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(ioEncodable.getEnc2(), ecflags);
                ecflags = econvPrepareOptions(context, options, ecopts_p, ecflags);
            }

            EncodingUtils.validateEncodingBinmode(context, fmode_p, ecflags, ioEncodable);

            ioEncodable.setEcflags(ecflags);
            ioEncodable.setEcopts(ecopts_p[0]);
            return;
        } while (true);
    }

    // mri: rb_io_extract_encoding_option
    public static boolean ioExtractEncodingOption(ThreadContext context, IOEncodable ioEncodable, IRubyObject options, int[] fmode_p) {
        Ruby runtime = context.runtime;

        IRubyObject encoding = context.nil;
        IRubyObject extenc = null;
        IRubyObject intenc = null;
        IRubyObject tmp;
        boolean extracted = false;
        Encoding extencoding = null;
        Encoding intencoding = null;

        if (options != null && !options.isNil()) {
            RubyHash opts = (RubyHash) options;

            IRubyObject encodingOpt = opts.op_aref(context, runtime.newSymbol("encoding"));
            if (!encodingOpt.isNil()) encoding = encodingOpt;
            IRubyObject externalOpt = opts.op_aref(context, runtime.newSymbol("external_encoding"));
            if (!externalOpt.isNil()) extenc = externalOpt;
            IRubyObject internalOpt = opts.op_aref(context, runtime.newSymbol("internal_encoding"));
            if (!internalOpt.isNil()) intenc = internalOpt;
        }

        if ((extenc != null || intenc != null) && !encoding.isNil()) {
            if (runtime.isVerbose()) {
                    runtime.getWarnings().warn("Ignoring encoding parameter '" + encoding + "': " +
                            (extenc == null ? "internal" : "external") + "_encoding is used");
            }
            encoding = context.nil;
        }

        if (extenc != null && !extenc.isNil()) {
            extencoding = rbToEncoding(context, extenc);
        }

        if (intenc != null) {
            if (intenc.isNil()) {
                intencoding = null;
            } else if (!(tmp = intenc.checkStringType()).isNil()) {
                String p = tmp.toString();
                if (p.equals("-")) {
                    intencoding = null;
                } else {
                    intencoding = rbToEncoding(context, intenc);
                }
            } else {
                intencoding = rbToEncoding(context, intenc);
            }
            if (extencoding == intencoding) {
                intencoding = null;
            }
        }

        if (!encoding.isNil()) {
            extracted = true;

            if (!(tmp = encoding.checkStringType()).isNil()) {
                parseModeEncoding(context, ioEncodable, tmp.asJavaString(), fmode_p);
            } else {
                ioExtIntToEncs(context, ioEncodable, rbToEncoding(context, encoding), null, 0);
            }
        } else if (extenc != null || intenc != null) {
            extracted = true;
            ioExtIntToEncs(context, ioEncodable, extencoding, intencoding, 0);
        }

        return extracted;
    }

    // MRI: rb_external_str_new_with_enc
    public static RubyString newExternalStringWithEncoding(Ruby runtime, String string, Encoding encoding) {
        if (string == null) return newEmptyString(runtime, encoding);

        /* ASCII-8BIT case, no conversion */
        if ((encoding == ASCIIEncoding.INSTANCE) ||
                (encoding == USASCIIEncoding.INSTANCE && searchNonAscii(string) != -1)) {
            return newBinaryString(runtime, string);
        }

        /* no default_internal or same encoding, no conversion */
        Encoding internalEncoding = runtime.getDefaultInternalEncoding();
        if (internalEncoding == null || encoding == internalEncoding) return newString(runtime, string, encoding);

        /* ASCII compatible, and ASCII only string, no conversion in
         * default_internal */
        if ((encoding == ASCIIEncoding.INSTANCE) ||
                (encoding == USASCIIEncoding.INSTANCE) ||
                (encoding.isAsciiCompatible() && searchNonAscii(string) == -1)) {
            return newString(runtime, string, internalEncoding);
        }

        /* convert from the given encoding to default_internal */
        RubyString convertedString = newEmptyString(runtime, internalEncoding);
        /* when the conversion failed for some reason, just ignore the
         * default_internal and result in the given encoding as-is. */
        try {
            convertedString.cat19(encodeBytelist(string, encoding), CR_UNKNOWN);
        } catch (org.jruby.exceptions.EncodingError.CompatibilityError ce) {
            return newString(runtime, string, encoding);
        }

        return convertedString;
    }

    // MRI: rb_external_str_new_with_enc
    public static RubyString newExternalStringWithEncoding(Ruby runtime, ByteList bytelist, Encoding encoding) {
        if (bytelist == null) return newEmptyString(runtime, encoding);

        /* ASCII-8BIT case, no conversion */
        if ((encoding == ASCIIEncoding.INSTANCE) ||
                (encoding == USASCIIEncoding.INSTANCE && searchNonAscii(bytelist) != -1)) {
            return newBinaryString(runtime, bytelist);
        }

        /* no default_internal or same encoding, no conversion */
        Encoding internalEncoding = runtime.getDefaultInternalEncoding();
        if (internalEncoding == null || encoding == internalEncoding) return newString(runtime, bytelist, encoding);

        /* ASCII compatible, and ASCII only string, no conversion in
         * default_internal */
        if ((encoding == ASCIIEncoding.INSTANCE) ||
                (encoding == USASCIIEncoding.INSTANCE) ||
                (encoding.isAsciiCompatible() && searchNonAscii(bytelist) == -1)) {
            return newString(runtime, bytelist, internalEncoding);
        }

        /* convert from the given encoding to default_internal */
        RubyString convertedString = newEmptyString(runtime, internalEncoding);
        /* when the conversion failed for some reason, just ignore the
         * default_internal and result in the given encoding as-is. */
        try {
            convertedString.cat19(encodeBytelist(bytelist, encoding), CR_UNKNOWN);
        } catch (org.jruby.exceptions.EncodingError.CompatibilityError ce) {
            return newString(runtime, bytelist, encoding);
        }

        return convertedString;
    }

    // mri: rb_io_ext_int_to_encs
    public static void ioExtIntToEncs(ThreadContext context, IOEncodable encodable, Encoding external, Encoding internal, int fmode) {
        boolean defaultExternal = false;

        if (external == null) {
            external = context.runtime.getDefaultExternalEncoding();
            defaultExternal = true;
        }

        if (external == ascii8bitEncoding(context.runtime)) {
            internal = null;
        } else if (internal == null) {
            internal = context.runtime.getDefaultInternalEncoding();
        }

        if (internal == null ||
                ((fmode & OpenFile.SETENC_BY_BOM) == 0 && internal == external)) {
            encodable.setEnc((defaultExternal && internal != external) ? null : external);
            encodable.setEnc2(null);
        } else {
            encodable.setEnc(internal);
            encodable.setEnc2(external);
        }
    }

    // mri: parse_mode_enc
    public static void parseModeEncoding(ThreadContext context, IOEncodable ioEncodable, String option, int[] fmode_p) {
        final Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();
        Encoding intEnc, extEnc;
        if (fmode_p == null) fmode_p = new int[]{0};

        List<String> encs = StringSupport.split(option, ':', 2);

        String estr = encs.size() == 2 ? encs.get(0) : option;

        if (estr.toLowerCase().startsWith("bom|")) {
            estr = estr.substring(4);
            if (estr.toLowerCase().startsWith("utf-")) {
                fmode_p[0] |= OpenFile.SETENC_BY_BOM;
                ioEncodable.setBOM(true);
            } else {
                runtime.getWarnings().warn("BOM with non-UTF encoding " + estr + " is nonsense");
                fmode_p[0] &= ~OpenFile.SETENC_BY_BOM;
            }
        }

        Encoding idx = service.findEncodingNoError(new ByteList(estr.getBytes(), false));

        if (idx == null) {
            runtime.getWarnings().warn("Unsupported encoding " + estr + " ignored");
            extEnc = null;
        } else {
            extEnc = idx;
        }

        intEnc = null;
        if (encs.size() == 2) {
            String istr = encs.get(1);
            if (istr.equals("-")) {
                intEnc = null;
            } else {
                idx = service.getEncodingFromString(istr);
                if (idx == null) {
                    runtime.getWarnings().warn("ignoring internal encoding " + idx + ": it is identical to external encoding " + idx);
                    intEnc = null;
                } else {
                    intEnc = idx;
                }
            }
        }

        ioExtIntToEncs(context, ioEncodable, extEnc, intEnc, fmode_p[0]);
    }

    // rb_econv_str_convert
    public static ByteList econvStrConvert(ThreadContext context, EConv ec, ByteList src, int flags) {
        return econvSubstrAppend(context, ec, src, null, flags);
    }

    // rb_econv_str_convert with source bytes
    public static ByteList econvByteConvert(ThreadContext context, EConv ec, byte[] bytes, int start, int length, int flags) {
        return econvAppend(context, ec, bytes, start, length, new ByteList(length, ec.destinationEncoding), flags);
    }

    // rb_econv_substr_append
    public static ByteList econvSubstrAppend(ThreadContext context, EConv ec, ByteList src, ByteList dst, int flags) {
        return econvAppend(context, ec, src, dst, flags);
    }

    // rb_econv_append
    public static ByteList econvAppend(ThreadContext context, EConv ec, ByteList sByteList, ByteList dst, int flags) {
        int len = sByteList.getRealSize();

        if (dst == null) {
            dst = new ByteList(len, ec.destinationEncoding);
        }

        return econvAppend(context, ec, sByteList.unsafeBytes(), sByteList.begin(), len, dst, flags);
    }

    // rb_econv_append with source bytes
    public static ByteList econvAppend(ThreadContext context, EConv ec, byte[] bytes, int start, int length, ByteList dst, int flags) {
        Ptr sp = new Ptr(0);
        int se;
        int ds;
        int ss = start;
        byte[] dBytes;
        Ptr dp = new Ptr(0);
        int de;
        EConvResult res;
        int maxOutput;

        if (ec.lastTranscoding != null) {
            maxOutput = ec.lastTranscoding.transcoder.maxOutput;
        } else {
            maxOutput = 1;
        }

        do {
            int dlen = dst.getRealSize();
            if ((dst.getUnsafeBytes().length - dst.getBegin()) - dlen < length + maxOutput) {
                long newCapa = dlen + length + maxOutput;
                if (Integer.MAX_VALUE < newCapa) {
                    throw context.runtime.newArgumentError("too long string");
                }
                dst.ensure((int)newCapa);
                dst.setRealSize(dlen);
            }
            sp.p = ss;
            se = sp.p + length;
            dBytes = dst.getUnsafeBytes();
            ds = dst.getBegin();
            de = dBytes.length;
            dp.p = ds += dlen;
            res = ec.convert(bytes, sp, se, dBytes, dp, de, flags);
            length -= sp.p - ss;
            ss = sp.p;
            dst.setRealSize(dlen + (dp.p - ds));
            EncodingUtils.econvCheckError(context, ec);
        } while (res == EConvResult.DestinationBufferFull);

        return dst;
    }

    // rb_econv_check_error
    public static void econvCheckError(ThreadContext context, EConv ec) {
        RaiseException re = makeEconvException(context.runtime, ec);
        if (re != null) throw re;
    }

    // rb_econv_prepare_opts
    public static int econvPrepareOpts(ThreadContext context, IRubyObject opthash, IRubyObject[] opts) {
        return econvPrepareOptions(context, opthash, opts, 0);
    }

    // rb_econv_prepare_options
    public static int econvPrepareOptions(ThreadContext context, IRubyObject opthash, IRubyObject[] opts, int ecflags) {
        IRubyObject newhash = context.nil;
        IRubyObject v;

        if (opthash.isNil()) {
            opts[0] = context.nil;
            return ecflags;
        }

        RubyHash optHash2 = (RubyHash)opthash;
        ecflags = econvOpts(context, opthash, ecflags);

        v = optHash2.op_aref(context, context.runtime.newSymbol("replace"));
        if (!v.isNil()) {
            RubyString v_str = v.convertToString();
            if (v_str.scanForCodeRange() == StringSupport.CR_BROKEN) {
                throw context.runtime.newArgumentError("replacement string is broken: " + v_str);
            }
            v = v_str.freeze(context);
            newhash = RubyHash.newHash(context.runtime);
            ((RubyHash)newhash).op_aset(context, context.runtime.newSymbol("replace"), v);
        }

        v = optHash2.op_aref(context, context.runtime.newSymbol("fallback"));
        if (!v.isNil()) {
            IRubyObject h = TypeConverter.checkHashType(context.runtime, v);
            boolean condition;
            if (h.isNil()) {
                condition = (v instanceof RubyProc || v instanceof RubyMethod || v.respondsTo("[]"));
            } else {
                v = h;
                condition = true;
            }

            if (condition) {
                if (newhash.isNil()) {
                    newhash = RubyHash.newHash(context.runtime);
                }
                ((RubyHash)newhash).op_aset(context, context.runtime.newSymbol("fallback"), v);
            }
        }

        if (!newhash.isNil()) {
            newhash.setFrozen(true);
        }
        opts[0] = newhash;

        return ecflags;
    }

    // econv_opts
    public static int econvOpts(ThreadContext context, IRubyObject opt, int ecflags) {
        Ruby runtime = context.runtime;
        IRubyObject v;

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("invalid"));
        if (v.isNil()) {
        } else if (v.toString().equals("replace")) {
            ecflags |= EConvFlags.INVALID_REPLACE;
        } else {
            throw runtime.newArgumentError("unknown value for invalid character option");
        }

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("undef"));
        if (v.isNil()) {
        } else if (v.toString().equals("replace")) {
            ecflags |= EConvFlags.UNDEF_REPLACE;
        } else {
            throw runtime.newArgumentError("unknown value for undefined character option");
        }

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("replace"));
        if (!v.isNil() && (ecflags & EConvFlags.INVALID_REPLACE) != 0) {
            ecflags |= EConvFlags.UNDEF_REPLACE;
        }

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("xml"));
        if (!v.isNil()) {
            if (v.toString().equals("text")) {
                ecflags |= EConvFlags.XML_TEXT_DECORATOR | EConvFlags.UNDEF_HEX_CHARREF;
            } else if (v.toString().equals("attr")) {
                ecflags |= EConvFlags.XML_ATTR_CONTENT_DECORATOR | EConvFlags.XML_ATTR_QUOTE_DECORATOR | EConvFlags.UNDEF_HEX_CHARREF;
            } else {
                throw runtime.newArgumentError("unexpected value for xml option: " + v);
            }
        }

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("newline"));
        if (!v.isNil()) {
            ecflags &= ~EConvFlags.NEWLINE_DECORATOR_MASK;
            if (v.toString().equals("universal")) {
                ecflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
            } else if (v.toString().equals("crlf")) {
                ecflags |= EConvFlags.CRLF_NEWLINE_DECORATOR;
            } else if (v.toString().equals("cr")) {
                ecflags |= EConvFlags.CR_NEWLINE_DECORATOR;
            } else if (v.toString().equals("lf")) {
//                ecflags |= ECONV_LF_NEWLINE_DECORATOR;
            } else if (v instanceof RubySymbol) {
                throw runtime.newArgumentError("unexpected value for newline option: " + ((RubySymbol) v).to_s(context).toString());
            } else {
                throw runtime.newArgumentError("unexpected value for newline option");
            }
        }

        int setflags = 0;
        boolean newlineflag = false;

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("universal_newline"));
        if (v.isTrue()) {
            setflags |= EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
        }
        newlineflag |= !v.isNil();

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("crlf_newline"));
        if (v.isTrue()) {
            setflags |= EConvFlags.CRLF_NEWLINE_DECORATOR;
        }
        newlineflag |= !v.isNil();

        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("cr_newline"));
        if (v.isTrue()) {
            setflags |= EConvFlags.CR_NEWLINE_DECORATOR;
        }
        newlineflag |= !v.isNil();

        if (newlineflag) {
            ecflags &= ~EConvFlags.NEWLINE_DECORATOR_MASK;
            ecflags |= setflags;
        }

        return ecflags;
    }

    // rb_econv_open_opts
    public static EConv econvOpenOpts(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, IRubyObject opthash) {
        Ruby runtime = context.runtime;
        IRubyObject replacement;

        if (opthash == null || opthash.isNil()) {
            replacement = context.nil;
        } else {
            if (!(opthash instanceof RubyHash) || !opthash.isFrozen()) {
                throw runtime.newRuntimeError("bug: EncodingUtils.econvOpenOpts called with invalid opthash");
            }
            replacement = ((RubyHash)opthash).op_aref(context, runtime.newSymbol("replace"));
        }

        EConv ec = TranscoderDB.open(sourceEncoding, destinationEncoding, ecflags);
        if (ec == null) return ec;

        if (!replacement.isNil()) {
            int ret;
            RubyString replStr = (RubyString)replacement;
            ByteList replBL = replStr.getByteList();
            ec.makeReplacement();

            ret = ec.setReplacement(replBL.getUnsafeBytes(), replBL.getBegin(), replBL.getRealSize(), replBL.getEncoding().getName());

            if (ret == -1) {
                ec.close();
                return null;
            }
        }

        return ec;
    }

    // rb_econv_open_exc
    public static RaiseException econvOpenExc(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags) {
        String message = econvDescription(context, sourceEncoding, destinationEncoding, ecflags, "code converter not found (") + ")";
        return context.runtime.newConverterNotFoundError(message);
    }

    // rb_econv_description
    public static String econvDescription(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, String message) {
        // limited port for now
        return message + new String(sourceEncoding) + " to " + new String(destinationEncoding);
    }

    // rb_econv_asciicompat_encoding
    // Missing proper logic from transcoding subsystem
    public static Encoding econvAsciicompatEncoding(Encoding enc) {
        return RubyConverter.NONASCII_TO_ASCII.get(enc);
    }

    // rb_enc_asciicompat
    public static boolean encAsciicompat(Encoding enc) {
        return encMbminlen(enc) == 1 && !encDummy(enc);
    }

    // rb_enc_ascget
    public static int encAscget(byte[] pBytes, int p, int e, int[] len, Encoding enc) {
        int c;
        int l;

        if (e <= p) {
            return -1;
        }

        if (encAsciicompat(enc)) {
            c = pBytes[p] & 0xFF;
            if (!Encoding.isAscii((byte)c)) {
                return -1;
            }
            if (len != null) len[0] = 1;
            return c;
        }
        l = StringSupport.preciseLength(enc, pBytes, p, e);
        if (!StringSupport.MBCLEN_CHARFOUND_P(l)) {
            return -1;
        }
        c = enc.mbcToCode(pBytes, p, e);
        if (!Encoding.isAscii(c)) {
            return -1;
        }
        if (len != null) len[0] = l;
        return c;
    }

    // rb_enc_mbminlen
    public static int encMbminlen(Encoding encoding) {
        return encoding.minLength();
    }

    // rb_enc_dummy_p
    public static boolean encDummy(Encoding enc) {
        return enc.isDummy();
    }

    // rb_enc_get
    public static Encoding encGet(ThreadContext context, IRubyObject obj) {
        if (obj instanceof EncodingCapable) {
            return ((EncodingCapable)obj).getEncoding();
        }

        return context.runtime.getDefaultInternalEncoding();
    }

    // encoding_equal
    public static boolean encodingEqual(byte[] enc1, byte[] enc2) {
        return ByteList.memcmp(enc1, 0, enc1.length, enc2, 0, enc2.length) == 0;
    }

    // enc_arg
    public static Encoding encArg(ThreadContext context, IRubyObject encval, byte[][] name_p, Encoding[] enc_p) {
        Encoding enc;
        if ((enc = toEncodingIndex(context, encval)) == null) {
            name_p[0] = encval.convertToString().getBytes();
        } else {
            name_p[0] = enc.getName();
        }

        return enc_p[0] = enc;
    }

    // rb_to_encoding_index
    public static Encoding toEncodingIndex(ThreadContext context, IRubyObject enc) {
        if (enc instanceof RubyEncoding) {
            return ((RubyEncoding)enc).getEncoding();
        } else if ((enc = enc.checkStringType()).isNil()) {
            return null;
        }
        if (!((RubyString)enc).getEncoding().isAsciiCompatible()) {
            return null;
        }
        return context.runtime.getEncodingService().getEncodingFromObjectNoError(enc);
    }

    // encoded_dup
    public static IRubyObject encodedDup(ThreadContext context, IRubyObject newstr, IRubyObject str, Encoding encindex) {
        if (encindex == null) return str.dup();
        if (newstr == str) {
            newstr = str.dup();
        } else {
            // set to same superclass
            ((RubyBasicObject)newstr).setMetaClass(str.getMetaClass());
        }
        ((RubyString)newstr).modify19();
        return strEncodeAssociate(context, newstr, encindex);
    }

    // str_encode_associate
    public static IRubyObject strEncodeAssociate(ThreadContext context, IRubyObject str, Encoding encidx) {
        encAssociateIndex(str, encidx);

        if (encAsciicompat(encidx)) {
            ((RubyString)str).scanForCodeRange();
        } else {
            ((RubyString)str).setCodeRange(StringSupport.CR_VALID);
        }

        return str;
    }

    // rb_enc_associate_index
    public static IRubyObject encAssociateIndex(IRubyObject obj, Encoding encidx) {
        ((RubyBasicObject)obj).checkFrozen();
        if (encidx == null) encidx = ASCIIEncoding.INSTANCE;
        if (((EncodingCapable)obj).getEncoding() == encidx) {
            return obj;
        }
        if (obj instanceof RubyString && !CodeRangeSupport.isCodeRangeAsciiOnly((RubyString) obj) ||
                encAsciicompat(encidx)) {
            ((RubyString)obj).clearCodeRange();
        }
        ((EncodingCapable)obj).setEncoding(encidx);
        return obj;
    }

    // str_encode
    public static IRubyObject strEncode(ThreadContext context, IRubyObject str, IRubyObject... args) {
        IRubyObject[] newstr_p = {str};

        Encoding dencindex = strTranscode(context, args, newstr_p);

        return encodedDup(context, newstr_p[0], str, dencindex);
    }

    // rb_str_encode
    public static IRubyObject rbStrEncode(ThreadContext context, IRubyObject str, IRubyObject to, int ecflags, IRubyObject ecopt) {
        IRubyObject[] newstr_p = {str};

        Encoding dencindex = strTranscode0(context, 1, new IRubyObject[]{to}, newstr_p, ecflags, ecopt);

        return encodedDup(context, newstr_p[0], str, dencindex);
    }

    // rb_str_encode
    public static ByteList rbByteEncode(ThreadContext context, byte[] bytes, int start, int length, Encoding encoding, int cr, Encoding to, int ecflags, IRubyObject ecopt) {
        byte[] sname, dname;

        sname = encoding.getName();
        dname = to.getName();

        if (noDecorators(ecflags)) {
            if (encoding.isAsciiCompatible() && to.isAsciiCompatible()) {
                if (cr == StringSupport.CR_7BIT) {
                    return null;
                }
            } else if (encodingEqual(sname, dname)) {
                return null;
            }
        } else if (encodingEqual(sname, dname)) {
            sname = NULL_BYTE_ARRAY;
            dname = NULL_BYTE_ARRAY;
        }

        int slen = length;
        int blen = slen + 30;
        ByteList dest = new ByteList(blen, to);

        Ptr fromPos = new Ptr(start);
        int destBegin = dest.getBegin();
        transcodeLoop(context, bytes, fromPos, dest.unsafeBytes(), new Ptr(destBegin), start + slen, destBegin + blen, dest, strTranscodingResize, sname, dname, ecflags, ecopt);

        if (fromPos.p != start + slen) {
            throw context.runtime.newArgumentError("not fully converted, " + (slen - fromPos.p) + " bytes left");
        }

        dest.setEncoding(to);

        return dest;
    }

    protected static boolean noDecorators(int ecflags) {
        return (ecflags & (EConvFlags.NEWLINE_DECORATOR_MASK
                | EConvFlags.XML_TEXT_DECORATOR
                | EConvFlags.XML_ATTR_CONTENT_DECORATOR
                | EConvFlags.XML_ATTR_QUOTE_DECORATOR)) == 0;
    }

    // str_transcode
    public static Encoding strTranscode(ThreadContext context, IRubyObject[] args, IRubyObject[] self_p) {
        int ecflags = 0;
        int argc = args.length;
        IRubyObject[] ecopts_p = {context.nil};

        if (args.length >= 1) {
            IRubyObject tmp = TypeConverter.checkHashType(context.runtime, args[args.length - 1]);
            if (!tmp.isNil()) {
                argc--;
                ecflags = econvPrepareOpts(context, tmp, ecopts_p);
            }
        }

        return strTranscode0(context, argc, args, self_p, ecflags, ecopts_p[0]);
    }

    // str_transcode0
    public static Encoding strTranscode0(ThreadContext context, int argc, IRubyObject[] args, IRubyObject[] self_p, int ecflags, IRubyObject ecopts) {
        Ruby runtime = context.runtime;

        IRubyObject str = self_p[0];
        IRubyObject arg1, arg2;
        Encoding[] senc_p = {null}, denc_p = {null};
        byte[][] sname_p = {null}, dname_p = {null};
        Encoding dencindex;
        boolean explicitlyInvalidReplace = true;

        if (argc > 2) {
            throw context.runtime.newArgumentError(args.length, 2);
        }

        if (argc == 0) {
            arg1 = runtime.getEncodingService().getDefaultInternal();
            if (arg1 == null || arg1.isNil()) {
                if (ecflags == 0) return null;
                arg1 = objEncoding(context, str);
            }
            if ((ecflags & EConvFlags.INVALID_MASK) == 0) {
                explicitlyInvalidReplace = false;
            }
            ecflags |= EConvFlags.INVALID_REPLACE | EConvFlags.UNDEF_REPLACE;
        } else {
            arg1 = args[0];
        }

        arg2 = argc <= 1 ? context.nil : args[1];
        dencindex = strTranscodeEncArgs(context, str, arg1, arg2, sname_p, senc_p, dname_p, denc_p);

        IRubyObject dest;

        if (noDecorators(ecflags)) {
            if (senc_p[0] != null && senc_p[0] == denc_p[0]) {
                if ((ecflags & EConvFlags.INVALID_MASK) != 0 && explicitlyInvalidReplace) {
                    IRubyObject rep = context.nil;
                    if (!ecopts.isNil()) {
                        rep = ((RubyHash)ecopts).op_aref(context, runtime.newSymbol("replace"));
                    }
                    dest = ((RubyString)str).encStrScrub(context, senc_p[0], rep, Block.NULL_BLOCK);
                    if (dest.isNil()) dest = str;
                    self_p[0] = dest;
                    return dencindex;
                }
                return arg2.isNil() ? null : dencindex;
            } else if (senc_p[0] != null && denc_p[0] != null && senc_p[0].isAsciiCompatible() && denc_p[0].isAsciiCompatible()) {
                if (((RubyString)str).scanForCodeRange() == StringSupport.CR_7BIT) {
                    return dencindex;
                }
            }
            if (encodingEqual(sname_p[0], dname_p[0])) {
                return arg2.isNil() ? null : dencindex;
            }
        } else {
            if (encodingEqual(sname_p[0], dname_p[0])) {
                sname_p[0] = NULL_BYTE_ARRAY;
                dname_p[0] = NULL_BYTE_ARRAY;
            }
        }

        ByteList sp = ((RubyString)str).getByteList();
        ByteList fromp = sp;
        int slen = ((RubyString)str).size();
        int blen = slen + 30;
        dest = RubyString.newStringLight(runtime, blen);
        ByteList destp = ((RubyString)dest).getByteList();

        byte[] frompBytes = fromp.unsafeBytes();
        byte[] destpBytes = destp.unsafeBytes();
        Ptr frompPos = new Ptr(fromp.getBegin());
        Ptr destpPos = new Ptr(destp.getBegin());
        transcodeLoop(context, frompBytes, frompPos, destpBytes, destpPos, frompPos.p + slen, destpPos.p + blen, destp, strTranscodingResize, sname_p[0], dname_p[0], ecflags, ecopts);

        if (frompPos.p != sp.begin() + slen) {
            throw runtime.newArgumentError("not fully converted, " + (slen - frompPos.p) + " bytes left");
        }

        // MRI sets length of dest here, but we've already done it in the inner transcodeLoop

        if (denc_p[0] == null) {
            dencindex = defineDummyEncoding(context, dname_p[0]);
        }

        self_p[0] = dest;

        return dencindex;
    }

    // rb_obj_encoding
    public static IRubyObject objEncoding(ThreadContext context, IRubyObject obj) {
        Encoding enc = encGet(context, obj);
        if (enc == null) {
            throw context.runtime.newTypeError("unknown encoding");
        }
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(enc);
    }

    public static Encoding strTranscodeEncArgs(ThreadContext context, IRubyObject str, IRubyObject arg1, IRubyObject arg2, byte[][] sname_p, Encoding[] senc_p, byte[][] dname_p, Encoding[] denc_p) {
        Encoding dencindex;

        dencindex = encArg(context, arg1, dname_p, denc_p);

        if (arg2.isNil()) {
            senc_p[0] = encGet(context, str);
            sname_p[0] = senc_p[0].getName();
        } else {
            encArg(context, arg2, sname_p, senc_p);
        }

        return dencindex;
    }

    public static boolean encRegistered(byte[] name) {
        return EncodingDB.getEncodings().get(name) != null;
    }

    // enc_check_duplication
    public static void encCheckDuplication(ThreadContext context, byte[] name) {
        if (encRegistered(name)) {
            throw context.runtime.newArgumentError("encoding " + new String(name) + " is already registered");
        }
    }

    // rb_enc_replicate
    public static Encoding encReplicate(ThreadContext context, byte[] name, Encoding encoding) {
        encCheckDuplication(context, name);
        EncodingDB.replicate(new String(name), new String(encoding.getName()));
        return EncodingDB.getEncodings().get(name).getEncoding();
    }

    // rb_define_dummy_encoding
    public static Encoding defineDummyEncoding(ThreadContext context, byte[] name) {
        Encoding dummy = encReplicate(context, name, ascii8bitEncoding(context.runtime));
        // TODO: set dummy on encoding; this probably should live in jcodings
        return dummy;
    }

    public static boolean DECORATOR_P(byte[] sname, byte[] dname) {
        return sname == null || sname.length == 0 || sname[0] == 0;
    }

    // TODO: Get rid of this and get consumers calling with existing RubyString
    public static ByteList strConvEncOpts(ThreadContext context, ByteList str, Encoding fromEncoding,
                                            Encoding toEncoding, int ecflags, IRubyObject ecopts) {
        return strConvEncOpts(
                context,
                newString(context.runtime, str),
                fromEncoding, toEncoding, ecflags, ecopts).getByteList();
    }

    /**
     * This will try and transcode the supplied ByteList to the supplied toEncoding.  It will use
     * forceEncoding as its encoding if it is supplied; otherwise it will use the encoding it has
     * tucked away in the bytelist.  This will return a new copy of a ByteList in the request
     * encoding or die trying (ConverterNotFound).
     *
     * c: rb_str_conv_enc_opts
     */
    public static RubyString strConvEncOpts(ThreadContext context, RubyString str, Encoding fromEncoding,
            Encoding toEncoding, int ecflags, IRubyObject ecopts) {

        if (toEncoding == null) return str;
        if (fromEncoding == null) fromEncoding = str.getEncoding();
        if (fromEncoding == toEncoding) return str;
        if ((toEncoding.isAsciiCompatible() && str.isAsciiOnly()) ||
                toEncoding == ASCIIEncoding.INSTANCE) {
            if (str.getEncoding() != toEncoding) {
                str = (RubyString)str.dup();
                str.setEncoding(toEncoding);
            }
            return str;
        }

        ByteList strByteList = str.getByteList();
        int len = strByteList.getRealSize();
        ByteList newStr = new ByteList(len);
        int olen = len;

        EConv ec = econvOpenOpts(context, fromEncoding.getName(), toEncoding.getName(), ecflags, ecopts);
        if (ec == null) return str;

        byte[] sbytes = strByteList.getUnsafeBytes();
        Ptr sp = new Ptr(strByteList.getBegin());
        int start = sp.p;

        byte[] destbytes;
        Ptr dp = new Ptr(0);
        EConvResult ret;
        int convertedOutput = 0;

        // these are in the while clause in MRI
        destbytes = newStr.getUnsafeBytes();
        int dest = newStr.begin();
        dp.p = dest + convertedOutput;
        ret = ec.convert(sbytes, sp, start + len, destbytes, dp, dest + olen, 0);

        while (ret == EConvResult.DestinationBufferFull) {
            int convertedInput = sp.p - start;
            int rest = len - convertedInput;
            convertedOutput = dp.p - dest;
            newStr.setRealSize(convertedOutput);
            if (convertedInput != 0 && convertedOutput != 0 &&
                    rest < (Integer.MAX_VALUE / convertedOutput)) {
                rest = (rest * convertedOutput) / convertedInput;
            } else {
                rest = olen;
            }
            olen += rest < 2 ? 2 : rest;
            newStr.ensure(olen);

            // these are the while clause in MRI
            destbytes = newStr.getUnsafeBytes();
            dest = newStr.begin();
            dp.p = dest + convertedOutput;
            ret = ec.convert(sbytes, sp, start + len, destbytes, dp, dest + olen, 0);
        }
        ec.close();

        switch (ret) {
            case Finished:
                len = dp.p;
                newStr.setRealSize(len);
                newStr.setEncoding(toEncoding);
                return newString(context.runtime, newStr);

            default:
                // some error, return original
                return str;
        }
    }

    // rb_str_conv_enc
    public static RubyString strConvEnc(ThreadContext context, RubyString value, Encoding fromEncoding, Encoding toEncoding) {
        return strConvEncOpts(context, value, fromEncoding, toEncoding, 0, context.nil);
    }

    public static ByteList strConvEnc(ThreadContext context, ByteList value, Encoding fromEncoding, Encoding toEncoding) {
        return strConvEncOpts(context, value, fromEncoding, toEncoding, 0, context.nil);
    }

    public static RubyString setStrBuf(Ruby runtime, final IRubyObject obj, final int len) {
        final RubyString str;
        if (obj == null || obj.isNil()) {
            str = RubyString.newStringLight(runtime, len);
        }
        else {
            str = obj.convertToString();
            int clen = str.size();
            if (clen >= len) {
                str.modify();
                return str;
            }
            str.modifyExpand(len);
        }
        return str;
    }

    public static List<String> encodingNames(byte[] name, int p, int end) {
        final List<String> names = new ArrayList<String>();

        Encoding enc = ASCIIEncoding.INSTANCE;
        int s = p;

        int code = name[s] & 0xff;
        if (enc.isDigit(code)) return names;

        boolean hasUpper = false;
        boolean hasLower = false;
        if (enc.isUpper(code)) {
            hasUpper = true;
            while (++s < end && (enc.isAlnum(name[s] & 0xff) || name[s] == (byte)'_')) {
                if (enc.isLower(name[s] & 0xff)) hasLower = true;
            }
        }

        boolean isValid = false;
        if (s >= end) {
            isValid = true;
            names.add(new String(name, p, end));
        }

        if (!isValid || hasLower) {
            if (!hasLower || !hasUpper) {
                do {
                    code = name[s] & 0xff;
                    if (enc.isLower(code)) hasLower = true;
                    if (enc.isUpper(code)) hasUpper = true;
                } while (++s < end && (!hasLower || !hasUpper));
            }

            byte[]constName = new byte[end - p];
            System.arraycopy(name, p, constName, 0, end - p);
            s = 0;
            code = constName[s] & 0xff;

            if (!isValid) {
                if (enc.isLower(code)) constName[s] = AsciiTables.ToUpperCaseTable[code];
                for (; s < constName.length; ++s) {
                    if (!enc.isAlnum(constName[s] & 0xff)) constName[s] = (byte)'_';
                }
                if (hasUpper) {
                    names.add(new String(constName, 0, constName.length));
                }
            }
            if (hasLower) {
                for (s = 0; s < constName.length; ++s) {
                    code = constName[s] & 0xff;
                    if (enc.isLower(code)) constName[s] = AsciiTables.ToUpperCaseTable[code];
                }
                names.add(new String(constName, 0, constName.length));
            }
        }

        return names;
    }

    public interface ResizeFunction {
        /**
         * Resize the destination, returning the new begin offset.
         *
         * @param destination
         * @param len
         * @param new_len
         * @return
         */
        int resize(ByteList destination, int len, int new_len);
    }

    public static final ResizeFunction strTranscodingResize = new ResizeFunction() {
        @Override
        public int resize(ByteList destination, int len, int new_len) {
            destination.setRealSize(len);
            destination.ensure(new_len);
            return destination.getBegin();
        }
    };

    /**
     * Fallback function to provide replacements for characters that fail to transcode.
     *
     * @param <Data> Data needed for the function to execute
     */
    public interface TranscodeFallback<Data> {
        /**
         * Return a replacement character for the given byte range and encoding.
         *
         * @param context  runtime state for the function
         * @param fallback data for the function
         * @param ec the transcoder that stumbled over the character
         * @return true if the character was successfully replaced; false otherwise
         */
        boolean call(ThreadContext context, Data fallback, EConv ec);
    }

    private static abstract class AbstractTranscodeFallback implements TranscodeFallback<IRubyObject> {
        @Override
        public boolean call(ThreadContext context, IRubyObject fallback, EConv ec) {
            Ruby runtime = context.runtime;
            IRubyObject rep = RubyString.newStringNoCopy(
                    runtime,
                    new ByteList(
                            ec.lastError.getErrorBytes(),
                            ec.lastError.getErrorBytesP(),
                            ec.lastError.getErrorBytesLength(),
                            runtime.getEncodingService().findEncodingOrAliasEntry(ec.lastError.getSource()).getEncoding(),
                            false)
            );
            rep = innerCall(context, fallback, rep);
            if (!rep.isNil()) {
                rep = rep.convertToString();
                Encoding repEnc = ((RubyString) rep).getEncoding();
                ByteList repByteList = ((RubyString) rep).getByteList();
                ec.insertOutput(repByteList.getUnsafeBytes(), repByteList.begin(), repByteList.getRealSize(), repEnc.getName());

                // TODO: check for too-large replacement
                return true;
            }
            return false;
        }

        protected abstract IRubyObject innerCall(ThreadContext context, IRubyObject fallback, IRubyObject c);
    }

    private static final AbstractTranscodeFallback HASH_FALLBACK = new AbstractTranscodeFallback() {
        @Override
        protected IRubyObject innerCall(ThreadContext context, IRubyObject fallback, IRubyObject c) {
            return ((RubyHash)fallback).op_aref(context, c);
        }
    };

    private static final AbstractTranscodeFallback PROC_FALLBACK = new AbstractTranscodeFallback() {
        @Override
        protected IRubyObject innerCall(ThreadContext context, IRubyObject fallback, IRubyObject c) {
            return ((RubyProc)fallback).call(context, c);
        }
    };

    private static final AbstractTranscodeFallback METHOD_FALLBACK = new AbstractTranscodeFallback() {
        @Override
        protected IRubyObject innerCall(ThreadContext context, IRubyObject fallback, IRubyObject c) {
            return fallback.callMethod(context, "call", c);
        }
    };

    private static final AbstractTranscodeFallback AREF_FALLBACK = new AbstractTranscodeFallback() {
        @Override
        protected IRubyObject innerCall(ThreadContext context, IRubyObject fallback, IRubyObject c) {
            return fallback.callMethod(context, "[]", c);
        }
    };

    /**
     * Perform the inner transcoding loop.
     *
     * @see #transcodeLoop(EConv, TranscodeFallback, Object, Object, byte[], Ptr, byte[], Ptr, int, int, ByteList, ResizeFunction)
     *
     * This version will determine fallback function and encoding options from the given options object.
     *
     * MRI: transcode_loop Ruby-related bits
     */
    public static void transcodeLoop(ThreadContext context, byte[] inBytes, Ptr inPos, byte[] outBytes, Ptr outPos, int inStop, int _outStop, ByteList destination, ResizeFunction resizeFunction, byte[] sname, byte[] dname, int ecflags, IRubyObject ecopts) {
        Ruby runtime = context.runtime;
        EConv ec;
        IRubyObject fallback = context.nil;
        TranscodeFallback fallbackFunc = null;

        ec = econvOpenOpts(context, sname, dname, ecflags, ecopts);

        if (ec == null) {
            throw econvOpenExc(context, sname, dname, ecflags);
        }

        if (!ecopts.isNil() && ecopts instanceof RubyHash) {
            fallback = ((RubyHash)ecopts).op_aref(context, runtime.newSymbol("fallback"));
            if (fallback instanceof RubyHash) {
                fallbackFunc = HASH_FALLBACK;
            } else if (fallback instanceof RubyProc) { // not quite same check as MRI
                fallbackFunc = PROC_FALLBACK;
            } else if (fallback instanceof RubyMethod) { // not quite same check as MRI
                fallbackFunc = METHOD_FALLBACK;
            } else {
                fallbackFunc = AREF_FALLBACK;
            }
        }

        boolean success = transcodeLoop(ec, fallbackFunc, context, fallback, inBytes, inPos, outBytes, outPos, inStop, _outStop, destination, resizeFunction);

        if (!success) {
            RaiseException re = makeEconvException(runtime, ec);
            ec.close();
            throw re;
        }
    }

    /**
     * A version of transcodeLoop for working without any Ruby runtime available.
     *
     * MRI: transcode_loop with no fallback and java.lang.String input
     */
    public static ByteList transcodeString(String string, Encoding toEncoding, int ecflags) {
        Encoding encoding;

        encoding = getUTF16ForPlatform();

        EConv ec = TranscoderDB.open(encoding.getName(), toEncoding.getName(), ecflags);

        byte[] inBytes = string.getBytes(EncodingUtils.charsetForEncoding(encoding));
        Ptr inPos = new Ptr(0);

        int inStop = inBytes.length;
        // most encodings will be shorter than UTF-16 for typical input
        int outStop = (int)((double) inBytes.length / 1.5 + 1);

        byte[] outBytes = new byte[outStop];
        Ptr outPos = new Ptr(0);

        ByteList destination = new ByteList(outBytes, toEncoding, false);

        boolean success = transcodeLoop(ec, null, null, null, inBytes, inPos, outBytes, outPos, inStop, outStop, destination, strTranscodingResize);

        if (!success) {
            // TODO: anything?
        }

        return destination;
    }

    public static Encoding getUTF16ForPlatform() {
        Encoding encoding;// This may be inefficient if we aren't matching endianness right
        if (Platform.BYTE_ORDER == Platform.LITTLE_ENDIAN) {
            encoding = UTF16LEEncoding.INSTANCE;
        } else {
            encoding = UTF16BEEncoding.INSTANCE;
        }
        return encoding;
    }

    /**
     * Perform the inner transcoding loop.
     *
     * The data in inBytes will be transcoded from the source encoding to the destination, eventually
     * replacing the contents of the given ByteList. Along the way, invalid characters may be handled by
     * calling the fallback function (if non-null) with the given state and data. If the destination
     * needs to be resized, use the given function to do so. Upon completion, destination will
     * contain the resulting transcoded bytes.
     *
     * MRI: transcode_loop generified with EConv and fallback function provided
     *
     * @param ec the encoding converter
     * @param fallbackFunc the fallback function for non-transcodable characters, or null if none
     * @param context runtime state to pass into the fallback
     * @param fallbackData call state to pass into the fallback
     * @param inBytes the incoming byte array
     * @param inPos the position from which to start in the incoming bytearray
     * @param outBytes the initial output byte array
     * @param outPos the position from which to start in the initial output byte array
     * @param inStop the position at which to stop in the input
     * @param outStop the number of bytes at which to stop in the output
     * @param destination the ByteList to hold the eventual output
     * @param resizeFunction a function to use to grow the destination
     * @param <Data> type of data for the fallback function
     * @return
     */
    public static <Data> boolean transcodeLoop(EConv ec, TranscodeFallback<Data> fallbackFunc, ThreadContext context, Data fallbackData, byte[] inBytes, Ptr inPos, byte[] outBytes, Ptr outPos, int inStop, int outStop, ByteList destination, ResizeFunction resizeFunction) {
        Ptr outstopPos = new Ptr(outStop);
        Transcoding lastTC = ec.lastTranscoding;
        int maxOutput = lastTC != null ? lastTC.transcoder.maxOutput : 1;

        Ptr outStart = new Ptr(outPos.p);

        // resume:
        while (true) {
            EConvResult ret = ec.convert(inBytes, inPos, inStop, outBytes, outPos, outstopPos.p, 0);

            if (fallbackFunc != null && ret == EConvResult.UndefinedConversion) {
                if (fallbackFunc.call(context, fallbackData, ec)) {
                    continue;
                }
            }

            if (ret == EConvResult.InvalidByteSequence ||
                    ret == EConvResult.IncompleteInput ||
                    ret == EConvResult.UndefinedConversion) {

                RaiseException exc = makeEconvException(context.runtime, ec);

                ec.close();

                destination.setRealSize(outPos.p);

                throw exc;
            }

            if (ret == EConvResult.DestinationBufferFull) {
                moreOutputBuffer(destination, resizeFunction, maxOutput, outStart, outPos, outstopPos);
                outBytes = destination.getUnsafeBytes();
                continue;
            }

            ec.close();

            destination.setRealSize(outPos.p);

            return true;
        }
    }

    // make_econv_exception
    public static RaiseException makeEconvException(Ruby runtime, EConv ec) {
        final StringBuilder mesg = new StringBuilder(); RaiseException exc;

        final EConvResult result = ec.lastError.getResult();
        if (result == EConvResult.InvalidByteSequence || result == EConvResult.IncompleteInput) {
            byte[] errBytes = ec.lastError.getErrorBytes();
            int errBytesP = ec.lastError.getErrorBytesP();
            int errorLen = ec.lastError.getErrorBytesLength();
            ByteList _bytes = new ByteList(errBytes, errBytesP, errorLen - errBytesP);
            RubyString bytes = newString(runtime, _bytes);
            RubyString dumped = (RubyString)bytes.dump();
            int readagainLen = ec.lastError.getReadAgainLength();
            IRubyObject bytes2 = runtime.getNil();
            if (result == EConvResult.IncompleteInput) {
                mesg.append("incomplete ").append(dumped).append(" on ").append(new String(ec.lastError.getSource()));
            } else if (readagainLen != 0) {
                bytes2 = newString(runtime, new ByteList(errBytes, errorLen + errBytesP, ec.lastError.getReadAgainLength()));
                IRubyObject dumped2 = ((RubyString) bytes2).dump();
                mesg.append(dumped).append(" followed by ").append(dumped2).append(" on ").append( new String(ec.lastError.getSource()) );
            } else {
                mesg.append(dumped).append(" on ").append( new String(ec.lastError.getSource()) );
            }

            exc = runtime.newInvalidByteSequenceError(mesg.toString());
            exc.getException().setInternalVariable("error_bytes", bytes);
            exc.getException().setInternalVariable("readagain_bytes", bytes2);
            exc.getException().setInternalVariable("incomplete_input", result == EConvResult.IncompleteInput ? runtime.getTrue() : runtime.getFalse());

            return makeEConvExceptionSetEncs(exc, runtime, ec);
        }
        else if (result == EConvResult.UndefinedConversion) {
            byte[] errBytes = ec.lastError.getErrorBytes();
            int errBytesP = ec.lastError.getErrorBytesP();
            int errorLen = ec.lastError.getErrorBytesLength();
            final byte[] errSource = ec.lastError.getSource();
            if (Arrays.equals(errSource, "UTF-8".getBytes())) {
                // prepare dumped form
            }

            RubyString bytes = newString(runtime, new ByteList(errBytes, errBytesP, errorLen - errBytesP));
            RubyString dumped = (RubyString) bytes.dump();

            if (Arrays.equals(errSource, ec.source) &&  Arrays.equals(ec.lastError.getDestination(), ec.destination)) {
                mesg.append(dumped).append(" from ").append( new String(errSource) ).append(" to ").append( new String(ec.lastError.getDestination()) );
            } else {
                mesg.append(dumped).append(" to ").append( new String(ec.lastError.getDestination()) ).append(" in conversion from ").append( new String(ec.source) );
                for (int i = 0; i < ec.numTranscoders; i++) {
                    mesg.append(" to ").append( new String(ec.elements[i].transcoding.transcoder.getDestination()) );
                }
            }

            exc = runtime.newUndefinedConversionError(mesg.toString());

            EncodingDB.Entry entry = runtime.getEncodingService().findEncodingOrAliasEntry(errSource);
            if (entry != null) {
                bytes.setEncoding(entry.getEncoding());
                exc.getException().setInternalVariable("error_char", bytes);
            }

            return makeEConvExceptionSetEncs(exc, runtime, ec);
        }
        return null;
    }

    private static RaiseException makeEConvExceptionSetEncs(RaiseException exc, Ruby runtime, EConv ec) {
        exc.getException().setInternalVariable("source_encoding_name", newString(runtime, ec.lastError.getSource()));
        exc.getException().setInternalVariable("destination_encoding_name", newString(runtime, ec.lastError.getDestination()));

        EncodingDB.Entry entry = runtime.getEncodingService().findEncodingOrAliasEntry(ec.lastError.getSource());
        if (entry != null) {
            exc.getException().setInternalVariable("source_encoding", runtime.getEncodingService().convertEncodingToRubyEncoding(entry.getEncoding()));
        }
        entry = runtime.getEncodingService().findEncodingOrAliasEntry(ec.lastError.getDestination());
        if (entry != null) {
            exc.getException().setInternalVariable("destination_encoding", runtime.getEncodingService().convertEncodingToRubyEncoding(entry.getEncoding()));
        }

        return exc;
    }

    // more_output_buffer
    static void moreOutputBuffer(ByteList destination, ResizeFunction resizeDestination, int maxOutput, Ptr outStart, Ptr outPos, Ptr outStop) {
        int len = outPos.p - outStart.p;
        int newLen = (len + maxOutput) * 2;
        outStart.p = resizeDestination.resize(destination, len, newLen);
        outPos.p = outStart.p + len;
        outStop.p = outStart.p + newLen;
    }

    // MRI: io_set_encoding_by_bom
    public static void ioSetEncodingByBOM(ThreadContext context, RubyIO io) {
        Ruby runtime = context.runtime;
        Encoding bomEncoding = ioStripBOM(context, io);

        if (bomEncoding != null) {
            // FIXME: Wonky that we acquire RubyEncoding to pass these encodings through
            IRubyObject theBom = runtime.getEncodingService().getEncoding(bomEncoding);
            IRubyObject theInternal = io.internal_encoding(context);

            io.setEncoding(runtime.getCurrentContext(), theBom, theInternal, context.nil);
        } else {
            io.setEnc2(null);
        }
    }

    // MRI: io_strip_bom
    public static Encoding ioStripBOM(ThreadContext context, RubyIO io) {
        IRubyObject b1, b2, b3, b4;

        if ((io.getOpenFile().getMode() & OpenFile.READABLE) == 0) return null;
        if ((b1 = io.getbyte(context)).isNil()) return null;

        switch ((int)((RubyFixnum)b1).getLongValue()) {
            case 0xEF:
                if ((b2 = io.getbyte(context)).isNil()) break;
                if (b2 instanceof RubyFixnum && ((RubyFixnum)b2).getLongValue() == 0xBB && !(b3 = io.getbyte(context)).isNil()) {
                    if (((RubyFixnum)b3).getLongValue() == 0xBF) {
                        return UTF8Encoding.INSTANCE;
                    }
                    io.ungetbyte(context, b3);
                }
                io.ungetbyte(context, b2);
                break;
            case 0xFE:
                if ((b2 = io.getbyte(context)).isNil()) break;
                if (b2 instanceof RubyFixnum && ((RubyFixnum)b2).getLongValue() == 0xFF) {
                    return UTF16BEEncoding.INSTANCE;
                }
                io.ungetbyte(context, b2);
                break;
            case 0xFF:
                if ((b2 = io.getbyte(context)).isNil()) break;
                if (b2 instanceof RubyFixnum && ((RubyFixnum)b2).getLongValue() == 0xFE) {
                    b3 = io.getbyte(context);
                    if (b3 instanceof RubyFixnum && ((RubyFixnum)b3).getLongValue() == 0 && !(b4 = io.getbyte(context)).isNil()) {
                        if (((RubyFixnum)b4).getLongValue() == 0) {
                            return UTF32LEEncoding.INSTANCE;
                        }
                        io.ungetbyte(context, b4);
                    } else {
                        io.ungetbyte(context, b3);
                        return UTF16LEEncoding.INSTANCE;
                    }
                    io.ungetbyte(context, b3);
                }
                io.ungetbyte(context, b2);
                break;
            case 0:
                if ((b2 = io.getbyte(context)).isNil()) break;
                if (b2 instanceof RubyFixnum && ((RubyFixnum)b2).getLongValue() == 0 && !(b3 = io.getbyte(context)).isNil()) {
                    if (b3 instanceof RubyFixnum && ((RubyFixnum)b3).getLongValue() == 0xFE && !(b4 = io.getbyte(context)).isNil()) {
                        if (b4 instanceof RubyFixnum && ((RubyFixnum)b4).getLongValue() == 0xFF) {
                            return UTF32BEEncoding.INSTANCE;
                        }
                        io.ungetbyte(context, b4);
                    }
                    io.ungetbyte(context, b3);
                }
                io.ungetbyte(context, b2);
                break;
        }
        io.ungetbyte(context, b1);
        return null;
    }

    // validate_enc_binmode
    public static void validateEncodingBinmode(ThreadContext context, int[] fmode_p, int ecflags, IOEncodable ioEncodable) {
        Ruby runtime = context.runtime;
        int fmode = fmode_p[0];

        if ((fmode & OpenFile.READABLE) != 0 &&
                ioEncodable.getEnc2() == null &&
                (fmode & OpenFile.BINMODE) == 0 &&
                !(ioEncodable.getEnc() != null ? ioEncodable.getEnc() : runtime.getDefaultExternalEncoding()).isAsciiCompatible()) {
            throw runtime.newArgumentError("ASCII incompatible encoding needs binmode");
        }

        if ((fmode & OpenFile.BINMODE) != 0 && (ecflags & EConvFlags.NEWLINE_DECORATOR_MASK) != 0) {
            throw runtime.newArgumentError("newline decorator with binary mode");
        }

        if ((fmode & OpenFile.BINMODE) == 0 && (EncodingUtils.DEFAULT_TEXTMODE != 0 || (ecflags & EConvFlags.NEWLINE_DECORATOR_MASK) != 0)) {
            fmode |= OpenFile.TEXTMODE;
            fmode_p[0] = fmode;
        } else if (EncodingUtils.DEFAULT_TEXTMODE == 0 && (ecflags & EConvFlags.NEWLINE_DECORATOR_MASK) == 0) {
            fmode &= ~OpenFile.TEXTMODE;
            fmode_p[0] = fmode;
        }
    }

    // rb_enc_set_default_external
    public static void rbEncSetDefaultExternal(ThreadContext context, IRubyObject encoding) {
        if (encoding.isNil()) {
            throw context.runtime.newArgumentError("default external can not be nil");
        }

        Encoding[] enc_p = {context.runtime.getDefaultExternalEncoding()};
        encSetDefaultEncoding(context, enc_p, encoding, "external");
        context.runtime.setDefaultExternalEncoding(enc_p[0]);
    }

    // rb_enc_set_default_internal
    public static void rbEncSetDefaultInternal(ThreadContext context, IRubyObject encoding) {
        Encoding[] enc_p = {context.runtime.getDefaultInternalEncoding()};
        encSetDefaultEncoding(context, enc_p, encoding, "internal");
        context.runtime.setDefaultInternalEncoding(enc_p[0]);
    }

    // enc_set_default_encoding
    public static boolean encSetDefaultEncoding(ThreadContext context, Encoding[] def_p, IRubyObject encoding, String name) {
        boolean overridden = false;

        if (def_p != null) {
            overridden = true;
        }

        if (encoding.isNil()) {
            def_p[0] = null;
            // don't set back into encoding table since it defers to us
        } else {
            def_p[0] = rbToEncoding(context, encoding);
            // don't set back into encoding table since it defers to us
        }

        if (name.equals("external")) {
            // TODO: set filesystem encoding
        }

        return overridden;
    }

    // rb_default_external_encoding
    public static Encoding defaultExternalEncoding(Ruby runtime) {
        if (runtime.getDefaultExternalEncoding() != null) return runtime.getDefaultExternalEncoding();

        return runtime.getEncodingService().getLocaleEncoding();
    }

    // rb_str_buf_cat
    public static void  rbStrBufCat(Ruby runtime, RubyString str, ByteList ptr) {
        if (ptr.length() == 0) return;
        // negative length check here, we shouldn't need
        strBufCat(runtime, str, ptr);
    }
    public static void  rbStrBufCat(Ruby runtime, ByteListHolder str, byte[] ptrBytes, int ptr, int len) {
        if (len == 0) return;
        // negative length check here, we shouldn't need
        strBufCat(runtime, str, ptrBytes, ptr, len);
    }
    public static void  rbStrBufCat(Ruby runtime, ByteList str, byte[] ptrBytes, int ptr, int len) {
        if (len == 0) return;
        // negative length check here, we shouldn't need
        strBufCat(str, ptrBytes, ptr, len);
    }

    // str_buf_cat
    public static void strBufCat(Ruby runtime, RubyString str, ByteList ptr) {
        strBufCat(runtime, str, ptr.getUnsafeBytes(), ptr.getBegin(), ptr.getRealSize());
    }
    public static void strBufCat(Ruby runtime, ByteListHolder str, byte[] ptrBytes, int ptr, int len) {
        str.modify();
        strBufCat(str.getByteList(), ptrBytes, ptr, len);
    }
    public static void strBufCat(ByteList str, byte[] ptrBytes, int ptr, int len) {
        int total, off = -1;

        // termlen is not relevant since we have no termination sequence

        // missing: if ptr string is inside str, off = ptr start minus str start

//        str.modify();
        if (len == 0) return;

        // much logic is missing here, since we don't manually manage the ByteList buffer

        total = str.getRealSize() + len;
        str.ensure(total);
        str.append(ptrBytes, ptr, len);
    }

    // rb_enc_str_buf_cat
    public static void encStrBufCat(Ruby runtime, RubyString str, ByteList ptr, Encoding enc) {
        encCrStrBufCat(runtime, str, ptr.getUnsafeBytes(), ptr.getBegin(), ptr.getRealSize(),
                enc, StringSupport.CR_UNKNOWN);
    }

    public static void encStrBufCat(Ruby runtime, RubyString str, ByteList ptr) {
        encCrStrBufCat(runtime, str, ptr.getUnsafeBytes(), ptr.getBegin(), ptr.getRealSize(),
                ptr.getEncoding(), StringSupport.CR_UNKNOWN);
    }

    public static void encStrBufCat(Ruby runtime, RubyString str, byte[] ptrBytes) {
        encCrStrBufCat(runtime, str, ptrBytes, 0, ptrBytes.length, USASCIIEncoding.INSTANCE, StringSupport.CR_UNKNOWN);
    }

    public static void encStrBufCat(Ruby runtime, RubyString str, byte[] ptrBytes, Encoding enc) {
        encCrStrBufCat(runtime, str, ptrBytes, 0, ptrBytes.length, enc, StringSupport.CR_UNKNOWN);
    }

    public static void encStrBufCat(Ruby runtime, RubyString str, byte[] ptrBytes, int ptr, int len, Encoding enc) {
        encCrStrBufCat(runtime, str, ptrBytes, ptr, len,
                enc, StringSupport.CR_UNKNOWN);
    }

    public static void encStrBufCat(Ruby runtime, RubyString str, CharSequence cseq) {
        byte[] utf8 = RubyEncoding.encodeUTF8(cseq.toString());
        encCrStrBufCat(runtime, str, utf8, 0, utf8.length, UTF8Encoding.INSTANCE, StringSupport.CR_UNKNOWN);
    }

    // rb_enc_cr_str_buf_cat
    public static int encCrStrBufCat(Ruby runtime, CodeRangeable str, ByteList ptr, Encoding ptrEnc, int ptr_cr) {
        return encCrStrBufCat(runtime, str, ptr.getUnsafeBytes(), ptr.getBegin(), ptr.getRealSize(), ptrEnc, ptr_cr);
    }

    public static int encCrStrBufCat(Ruby runtime, CodeRangeable str, byte[] ptrBytes, int ptr, int len, Encoding ptrEnc, int ptr_cr) {
        Encoding strEnc = str.getByteList().getEncoding();
        Encoding resEnc;
        int str_cr, res_cr;
        boolean incompatible = false;

        str_cr = str.getByteList().getRealSize() > 0 ? str.getCodeRange() : StringSupport.CR_7BIT;

        if (strEnc == ptrEnc) {
            if (str_cr == StringSupport.CR_UNKNOWN) {
                ptr_cr = StringSupport.CR_UNKNOWN;
            } else if (ptr_cr == StringSupport.CR_UNKNOWN) {
                ptr_cr = StringSupport.codeRangeScan(ptrEnc, ptrBytes, ptr, len);
            }
        } else {
            if (!EncodingUtils.encAsciicompat(strEnc) || !EncodingUtils.encAsciicompat(ptrEnc)) {
                if (len == 0) return ptr_cr;
                if (str.getByteList().getRealSize() == 0) {
                    strBufCat(runtime, str, ptrBytes, ptr, len);
                    str.getByteList().setEncoding(ptrEnc);
                    str.setCodeRange(ptr_cr);
                    return ptr_cr;
                }
                incompatible = true;
            }
            if (!incompatible) {
                if (ptr_cr == StringSupport.CR_UNKNOWN) {
                    ptr_cr = StringSupport.codeRangeScan(ptrEnc, ptrBytes, ptr, len);
                }
                if (str_cr == StringSupport.CR_UNKNOWN) {
                    if (strEnc == ASCIIEncoding.INSTANCE || ptr_cr != StringSupport.CR_7BIT) {
                        str_cr = str.scanForCodeRange();
                    }
                }
            }
        }

        if (incompatible ||
                (strEnc != ptrEnc &&
                str_cr != StringSupport.CR_7BIT &&
                ptr_cr != StringSupport.CR_7BIT)) {
            throw runtime.newEncodingCompatibilityError("incompatible encodings: " + strEnc + " and " + ptrEnc);
        }

        if (str_cr == StringSupport.CR_UNKNOWN) {
            resEnc = strEnc;
            res_cr = StringSupport.CR_UNKNOWN;
        } else if (str_cr == StringSupport.CR_7BIT) {
            if (ptr_cr == StringSupport.CR_7BIT) {
                resEnc = strEnc;
                res_cr = StringSupport.CR_7BIT;
            } else {
                resEnc = ptrEnc;
                res_cr = ptr_cr;
            }
        } else if (str_cr == StringSupport.CR_VALID) {
            resEnc = strEnc;
            if (ptr_cr == StringSupport.CR_7BIT || ptr_cr == StringSupport.CR_VALID) {
                res_cr = str_cr;
            } else {
                res_cr = ptr_cr;
            }
        } else { // str_cr must be BROKEN at this point
            resEnc = strEnc;
            res_cr = str_cr;
            if (0 < len) res_cr = StringSupport.CR_UNKNOWN;
        }

        // MRI checks for len < 0 here, but I don't think that's possible for us

        strBufCat(runtime, str, ptrBytes, ptr, len);
        str.getByteList().setEncoding(resEnc);
        str.setCodeRange(res_cr);

        return ptr_cr;
    }

    // econv_args
    public static void econvArgs(ThreadContext context, IRubyObject[] args, byte[][] encNames, Encoding[] encs, int[] ecflags_p, IRubyObject[] ecopts_p) {
        Ruby runtime = context.runtime;
        IRubyObject snamev = context.nil;
        IRubyObject dnamev = context.nil;
        IRubyObject flags = context.nil;
        IRubyObject opt = context.nil;

        // scan args logic
        {
            switch (args.length) {
                case 3:
                    flags = args[2];
                case 2:
                    dnamev = args[1];
                case 1:
                    snamev = args[0];
            }

            IRubyObject tmp;
            if (!(tmp = TypeConverter.checkHashType(runtime, flags)).isNil()) {
                opt = tmp;
                flags = context.nil;
            }
        }

        if (!flags.isNil()) {
            if (!opt.isNil()) {
                throw runtime.newArgumentError(args.length, 3);
            }
            ecflags_p[0] = (int)flags.convertToInteger().getLongValue();
            ecopts_p[0] = context.nil;
        } else if (!opt.isNil()) {
            ecflags_p[0] = EncodingUtils.econvPrepareOpts(context, opt, ecopts_p);
        } else {
            ecflags_p[0] = 0;
            ecopts_p[0] = context.nil;
        }

        encs[0] = runtime.getEncodingService().getEncodingFromObjectNoError(snamev);
        if (encs[0] == null) {
            snamev = snamev.convertToString();
        }
        encs[1] = runtime.getEncodingService().getEncodingFromObjectNoError(dnamev);
        if (encs[1] == null) {
            dnamev = dnamev.convertToString();
        }

        encNames[0] = encs[0] != null ? encs[0].getName() : ((RubyString)snamev).getBytes();
        encNames[1] = encs[1] != null ? encs[1].getName() : ((RubyString)dnamev).getBytes();

        return;
    }

    // rb_econv_init_by_convpath
    public static EConv econvInitByConvpath(ThreadContext context, IRubyObject convpath, byte[][] encNames, Encoding[] encs) {
        final Ruby runtime = context.runtime;
        final EConv ec = TranscoderDB.alloc(convpath.convertToArray().size());

        IRubyObject[] sname_v = {context.nil};
        IRubyObject[] dname_v = {context.nil};
        byte[][] sname = {null};
        byte[][] dname = {null};
        Encoding[] senc = {null};
        Encoding[] denc = {null};

        boolean first = true;

        for (int i = 0; i < ((RubyArray)convpath).size(); i++) {
            IRubyObject elt = ((RubyArray)convpath).eltOk(i);
            IRubyObject pair;
            if (!(pair = elt.checkArrayType()).isNil()) {
                if (((RubyArray)pair).size() != 2) {
                    throw context.runtime.newArgumentError("not a 2-element array in convpath");
                }
                sname_v[0] = ((RubyArray)pair).eltOk(0);
                encArg(context, sname_v[0], sname, senc);
                dname_v[0] = ((RubyArray)pair).eltOk(1);
                encArg(context, dname_v[0], dname, denc);
            } else {
                sname[0] = NULL_BYTE_ARRAY;
                dname[0] = elt.convertToString().getBytes();
            }
            if (DECORATOR_P(sname[0], dname[0])) {
                boolean ret = ec.addConverter(sname[0], dname[0], ec.numTranscoders);
                if (!ret) {
                    throw runtime.newArgumentError("decoration failed: " + new String(dname[0]));
                }
            } else {
                int j = ec.numTranscoders;
                final int[] arg = {j,0};
                int ret = TranscoderDB.searchPath(sname[0], dname[0], new TranscoderDB.SearchPathCallback() {
                    @Override
                    public void call(byte[] source, byte[] destination, int depth) {
                        if (arg[1] == -1) return;

                        arg[1] = ec.addConverter(source, destination, arg[0]) ? 0 : -1;
                    }
                });
                if (ret == -1 || arg[1] == -1) {
                    throw runtime.newArgumentError("adding conversion failed: " + new String(sname[0]) + " to " + new String(dname[0]));
                }
                if (first) {
                    first = false;
                    encs[0] = senc[0];
                    encNames[0] = ec.elements[j].transcoding.transcoder.getSource();
                }
                encs[1] = denc[0];
                encNames[1] = ec.elements[ec.numTranscoders - 1].transcoding.transcoder.getDestination();
            }
        }

        if (first) {
            encs[0] = null;
            encs[1] = null;
            encNames[0] = NULL_BYTE_ARRAY;
            encNames[1] = NULL_BYTE_ARRAY;
        }

        ec.source = encNames[0];
        ec.destination = encNames[0];

        return ec;
    }

    // decorate_convpath
    public static int decorateConvpath(ThreadContext context, IRubyObject convpath, int ecflags) {
        Ruby runtime = context.runtime;
        int num_decorators;
        byte[][] decorators = new byte[EConvFlags.MAX_ECFLAGS_DECORATORS][];
        int i;
        int n, len;

        num_decorators = TranscoderDB.decoratorNames(ecflags, decorators);
        if (num_decorators == -1)
            return -1;

        len = n = ((RubyArray)convpath).size();
        if (n != 0) {
            IRubyObject pair = ((RubyArray)convpath).eltOk(n - 1);
            if (pair instanceof RubyArray) {
                byte[] sname = runtime.getEncodingService().getEncodingFromObject(((RubyArray)pair).eltOk(0)).getName();
                byte[] dname = runtime.getEncodingService().getEncodingFromObject(((RubyArray)pair).eltOk(1)).getName();
                TranscoderDB.Entry entry = TranscoderDB.getEntry(sname, dname);
                Transcoder tr = entry.getTranscoder();
                if (tr == null)
                    return -1;
                if (!DECORATOR_P(tr.getSource(), tr.getDestination()) &&
                        tr.compatibility.isEncoder()) {
                    n--;
                    ((RubyArray)convpath).store(len + num_decorators - 1, pair);
                }
            } else {
                ((RubyArray)convpath).store(len + num_decorators - 1, pair);
            }
        }

        for (i = 0; i < num_decorators; i++)
            ((RubyArray)convpath).store(n + i, newString(runtime, decorators[i]));

        return 0;
    }

    // io_enc_str
    public static IRubyObject ioEncStr(Ruby runtime, IRubyObject str, OpenFile fptr)
    {
        str.setTaint(true);
        ((RubyString)str).setEncoding(fptr.readEncoding(runtime));
        return str;
    }

    // rb_enc_uint_chr
    public static RubyString encUintChr(ThreadContext context, int code, Encoding enc) {
        Ruby runtime = context.runtime;

        long i = code & 0xFFFFFFFFL;

        int n;
        switch (n = EncodingUtils.encCodelen(context, code, enc)) {
            case ErrorCodes.ERR_INVALID_CODE_POINT_VALUE:
                throw runtime.newRangeError("invalid codepoint " + Long.toHexString(i) + " in " + enc);
            case ErrorCodes.ERR_TOO_BIG_WIDE_CHAR_VALUE:
            case 0:
                throw runtime.newRangeError(i + " out of char range");
        }

        ByteList strBytes = new ByteList(n);
        strBytes.setEncoding(enc);
        strBytes.length(n);
        byte[] bytes = strBytes.unsafeBytes();
        int begin = strBytes.begin();
        int end = strBytes.realSize();

        encMbcput(context, code, bytes, begin, enc);
        if (StringSupport.preciseLength(enc, bytes, begin, end) != n) {
            throw runtime.newRangeError("invalid codepoint " + Long.toHexString(i) + " in " + enc);
        }

        return newString(runtime, strBytes);

    }

    // rb_enc_mbcput with Java exception
    public static int encMbcput(int c, byte[] buf, int p, Encoding enc) {
        int len = enc.codeToMbc(c, buf, p);
        if (len < 0) {
            throw new EncodingException(EncodingError.fromCode(len));
        }

        return len;
    }

    // rb_enc_mbcput with Ruby exception
    public static int encMbcput(ThreadContext context, int c, byte[] buf, int p, Encoding enc) {
        int len = enc.codeToMbc(c, buf, p);

        // in MRI, this check occurs within some of the individual encoding functions, such as the
        // US-ASCII check for values >= 0x80. In MRI, unlike in JRuby, we can't throw Ruby errors
        // from within encoding logic, so we try to reproduce the expected results via normal
        // error codes here.
        // See MRI's rb_enc_mbcput and related downstream encoding functions.
        if (len < 0) {
            switch (len) {
                case ErrorCodes.ERR_INVALID_CODE_POINT_VALUE:
                    throw context.runtime.newRangeError("invalid codepoint " + Long.toHexString(c & 0xFFFFFFFFL) + " in " + enc);
                case ErrorCodes.ERR_TOO_BIG_WIDE_CHAR_VALUE:
                    throw context.runtime.newRangeError("" + (c & 0xFFFFFFFFL) + " out of char range");
            }
            throw context.runtime.newEncodingError(EncodingError.fromCode(len).getMessage());
        }

        return len;
    }

    // rb_enc_codepoint_len
    public static int encCodepointLength(byte[] pBytes, int p, int e, int[] len_p, Encoding enc) {
        int r;
        if (e <= p)
            throw new IllegalArgumentException("empty string");
        r = StringSupport.preciseLength(enc, pBytes, p, e);
        if (!StringSupport.MBCLEN_CHARFOUND_P(r)) {
            throw new IllegalArgumentException("invalid byte sequence in " + enc);
        }
        if (len_p != null) len_p[0] = StringSupport.MBCLEN_CHARFOUND_LEN(r);
        return StringSupport.codePoint(enc, pBytes, p, e);
    }

    public static int encCodepointLength(Ruby runtime, byte[] pBytes, int p, int e, int[] len_p, Encoding enc) {
        try {
            return encCodepointLength(pBytes, p, e, len_p, enc);
        } catch (IllegalArgumentException ex) {
            throw runtime.newArgumentError(ex.getMessage());
        }
    }

    // MRI: str_compat_and_valid
    public static IRubyObject strCompatAndValid(ThreadContext context, IRubyObject _str, Encoding enc) {
        int cr;
        RubyString str = _str.convertToString();
        cr = str.scanForCodeRange();
        if (cr == StringSupport.CR_BROKEN) {
            throw context.runtime.newArgumentError("replacement must be valid byte sequence '" + str + "'");
        }
        else {
            Encoding e = STR_ENC_GET(str);
            if (cr == StringSupport.CR_7BIT ? enc.minLength() != 1 : enc != e) {
                throw context.runtime.newEncodingCompatibilityError("incompatible character encodings: " + enc + " and " + e);
            }
        }
        return str;
    }

    // MRI: get_encoding
    public static Encoding getEncoding(ByteList str) {
        return getActualEncoding(str.getEncoding(), str);
    }

    private static final Encoding UTF16Dummy = EncodingDB.getEncodings().get("UTF-16".getBytes()).getEncoding();
    private static final Encoding UTF32Dummy = EncodingDB.getEncodings().get("UTF-32".getBytes()).getEncoding();

    // MRI: get_actual_encoding
    public static Encoding getActualEncoding(Encoding enc, ByteList byteList) {
        return getActualEncoding(enc, byteList.getUnsafeBytes(), byteList.begin(), byteList.begin() + byteList.realSize());
    }

    public static Encoding getActualEncoding(Encoding enc, byte[] bytes, int p, int end) {
        if (enc.isDummy() && enc instanceof UnicodeEncoding) {
            // handle dummy UTF-16 and UTF-32 by scanning for BOM, as in MRI
            if (enc == UTF16Dummy && end - p >= 2) {
                int c0 = bytes[p] & 0xff;
                int c1 = bytes[p + 1] & 0xff;

                if (c0 == 0xFE && c1 == 0xFF) {
                    return UTF16BEEncoding.INSTANCE;
                } else if (c0 == 0xFF && c1 == 0xFE) {
                    return UTF16LEEncoding.INSTANCE;
                }
                return ASCIIEncoding.INSTANCE;
            } else if (enc == UTF32Dummy && end - p >= 4) {
                int c0 = bytes[p] & 0xff;
                int c1 = bytes[p + 1] & 0xff;
                int c2 = bytes[p + 2] & 0xff;
                int c3 = bytes[p + 3] & 0xff;

                if (c0 == 0 && c1 == 0 && c2 == 0xFE && c3 == 0xFF) {
                    return UTF32BEEncoding.INSTANCE;
                } else if (c3 == 0 && c2 == 0 && c1 == 0xFE && c0 == 0xFF) {
                    return UTF32LEEncoding.INSTANCE;
                }
                return ASCIIEncoding.INSTANCE;
            }
        }
        return enc;
    }

    public static Encoding STR_ENC_GET(ByteListHolder str) {
        return getEncoding(str.getByteList());
    }

    public static RubyString rbStrEscape(Ruby runtime, RubyString str) {
        return (RubyString) RubyString.rbStrEscape(runtime.getCurrentContext(), str);
    }

    public static int rbStrBufCatEscapedChar(RubyString result, long c, boolean unicode_p) {
        // FIXME: inefficient
        byte[] buf;
        int l;

        c &= 0xffffffff;

        if (unicode_p) {
            if (c < 0x7F && c > 31 /*ISPRINT(c)*/) {
                buf = String.format("%c", (char)c).getBytes();
            }
            else if (c < 0x10000) {
                buf = String.format("\\u%04X", c).getBytes();
            }
            else {
                buf = String.format("\\u{%X}", c).getBytes();
            }
        }
        else {
            if (c < 0x100) {
                buf = String.format("\\x{%02X}", c).getBytes();
            }
            else {
                buf = String.format("\\x{%X}", c).getBytes();
            }
        }
        result.cat(buf);
        return buf.length;
    }

    /**
     * Get an appropriate Java Charset for the given Encoding.
     *
     * This works around a bug in jcodings where it would return null as the charset for encodings that should have
     * a match, like Windows-1252. This method is equivalent to enc.getCharset in jcodings 1.0.25 and higher.
     *
     * See https://github.com/jruby/jruby/issues/4716 for more information.
     *
     * @param enc the encoding for which to get a matching charset
     * @return the matching charset
     */
    public static Charset charsetForEncoding(Encoding enc) {
        Charset charset = enc.getCharset();

        if (charset == null) {
            charset = Charset.forName(enc.toString());
        }

        return charset;
    }

    public static int encCodelen(ThreadContext context, int c, Encoding enc) {
        int n = enc.codeToMbcLength(c);
        if (n == 0) {
            throw context.runtime.newArgumentError("invalid codepoint " + Long.toHexString(c & 0xFFFFFFFFL) + " in " + enc);
        }
        return n;
    }

    @Deprecated
    public static Encoding ioStripBOM(RubyIO io) {
        return ioStripBOM(io.getRuntime().getCurrentContext(), io);
    }

}

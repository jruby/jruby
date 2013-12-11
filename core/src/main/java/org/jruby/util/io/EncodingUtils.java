package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyConverter;
import org.jruby.RubyEncoding;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyMethod;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.ByteList;
import org.jruby.util.encoding.Transcoder;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.encoding.RubyCoderResult;

public class EncodingUtils {    
    public static final int ECONV_ERROR_HANDLER_MASK               = 0x000000ff;

    public static final int ECONV_INVALID_MASK                     = 0x0000000f;
    public static final int ECONV_INVALID_REPLACE                  = 0x00000002;

    public static final int ECONV_UNDEF_MASK                       = 0x000000f0;
    public static final int ECONV_UNDEF_REPLACE                    = 0x00000020;
    public static final int ECONV_UNDEF_HEX_CHARREF                = 0x00000030;

    public static final int ECONV_DECORATOR_MASK                   = 0x0000ff00;
    public static final int ECONV_NEWLINE_DECORATOR_MASK           = 0x00003f00;
    public static final int ECONV_NEWLINE_DECORATOR_READ_MASK      = 0x00000f00;
    public static final int ECONV_NEWLINE_DECORATOR_WRITE_MASK     = 0x00003000;

    public static final int ECONV_UNIVERSAL_NEWLINE_DECORATOR      = 0x00000100;
    public static final int ECONV_CRLF_NEWLINE_DECORATOR           = 0x00001000;
    public static final int ECONV_CR_NEWLINE_DECORATOR             = 0x00002000;
    public static final int ECONV_XML_TEXT_DECORATOR               = 0x00004000;
    public static final int ECONV_XML_ATTR_CONTENT_DECORATOR       = 0x00008000;

    public static final int ECONV_STATEFUL_DECORATOR_MASK          = 0x00f00000;
    public static final int ECONV_XML_ATTR_QUOTE_DECORATOR         = 0x00100000;
    
    public static final int ECONV_PARTIAL_INPUT                    = 0x00010000;
    public static final int ECONV_AFTER_OUTPUT                     = 0x00020000;
    
    public static final int ECONV_DEFAULT_NEWLINE_DECORATOR = Platform.IS_WINDOWS ? ECONV_CRLF_NEWLINE_DECORATOR : 0;
    public static final int DEFAULT_TEXTMODE = Platform.IS_WINDOWS ? OpenFile.TEXTMODE : 0;
    public static final int TEXTMODE_NEWLINE_DECORATOR_ON_WRITE = Platform.IS_WINDOWS ? ECONV_CRLF_NEWLINE_DECORATOR : -1;
    
    private static final byte[] NULL_BYTE_ARRAY = new byte[0];

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
        Encoding idx = context.runtime.getEncodingService().getEncodingFromObject(enc);
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
    
    public static final int PERM = 0;
    public static final int VMODE = 1;
    
    public static final int MODE_BTMODE(int fmode, int a, int b, int c) {
        return (fmode & OpenFile.BINMODE) != 0 ? b :
                (fmode & OpenFile.TEXTMODE) != 0 ? c : a;
    }
    
    public static int SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(Encoding enc2, int ecflags) {
        if (enc2 != null && (ecflags & ECONV_DEFAULT_NEWLINE_DECORATOR) != 0) {
            return ecflags | ECONV_UNIVERSAL_NEWLINE_DECORATOR;
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
            IOEncodable ioEncodable, IRubyObject[] vmodeAndVperm_p, IRubyObject options, int[] oflags_p, int[] fmode_p) {
        IRubyObject vmode;
        int ecflags;
        IRubyObject[] ecopts_p = {context.nil};
        boolean hasEnc = false, hasVmode = false;
        IRubyObject intmode;
        
        vmode = vmodeAndVperm_p[VMODE];
        
        // Give default encodings
        ioExtIntToEncs(context, ioEncodable, null, null, 0);

        vmode_handle: do {
            if (vmodeAndVperm_p[VMODE] == null || vmodeAndVperm_p[VMODE].isNil()) {
                fmode_p[0] = OpenFile.READABLE;
                oflags_p[0] = ModeFlags.RDONLY;
            } else {
                intmode = TypeConverter.checkIntegerType(context.runtime, vmodeAndVperm_p[VMODE], "to_int");

                if (!intmode.isNil()) {
                    vmodeAndVperm_p[VMODE] = intmode;
                    oflags_p[0] = RubyNumeric.num2int(intmode);
                    fmode_p[0] = ModeFlags.getOpenFileFlagsFor(oflags_p[0]);
                } else {
                    String p = vmodeAndVperm_p[VMODE].convertToString().asJavaString();
                    int colonSplit = p.indexOf(":");
                    String mode = colonSplit == -1 ? p : p.substring(0, colonSplit);
                    try {
                        fmode_p[0] = OpenFile.getFModeFromString(mode);
                        oflags_p[0] = OpenFile.getModeFlagsAsIntFrom(fmode_p[0]);
                    } catch (InvalidValueException e) {
                        throw context.runtime.newArgumentError("illegal access mode " + vmodeAndVperm_p[VMODE]);
                    }

                    if (colonSplit != -1) {
                        hasEnc = true;
                        parseModeEncoding(context, ioEncodable, p.substring(colonSplit + 1), fmode_p);
                    } else {
                        Encoding e = (fmode_p[0] & OpenFile.BINMODE) != 0 ? ascii8bitEncoding(context.runtime) : null;
                        ioExtIntToEncs(context, ioEncodable, e, null, fmode_p[0]);
                    }
                }
            }

            if (options == null || options.isNil()) {
                ecflags = (fmode_p[0] & OpenFile.READABLE) != 0
                        ? MODE_BTMODE(fmode_p[0], ECONV_DEFAULT_NEWLINE_DECORATOR, 0, ECONV_UNIVERSAL_NEWLINE_DECORATOR)
                        : 0;
                if (TEXTMODE_NEWLINE_DECORATOR_ON_WRITE != -1) {
                    ecflags |= (fmode_p[0] & OpenFile.WRITABLE) != 0
                            ? MODE_BTMODE(fmode_p[0], TEXTMODE_NEWLINE_DECORATOR_ON_WRITE, 0, TEXTMODE_NEWLINE_DECORATOR_ON_WRITE)
                            : 0;
                }
                ecflags = SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(ioEncodable.getEnc2(), ecflags);
                ecopts_p[0] = context.nil;
            } else {
                extractBinmode(context.runtime, options, fmode_p);
                // Differs from MRI but we open with ModeFlags
                if ((fmode_p[0] & OpenFile.BINMODE) != 0) {
                    oflags_p[0] |= ModeFlags.BINARY;
                
                    if (!hasEnc) {
                        ioExtIntToEncs(context, ioEncodable, ascii8bitEncoding(context.runtime), null, fmode_p[0]);
                    }
                } else if (DEFAULT_TEXTMODE != 0 && (vmode == null || vmode.isNil())) {
                    fmode_p[0] |= DEFAULT_TEXTMODE;
                }

                if (!hasVmode) {
                    IRubyObject v = hashARef(context.runtime, options, "mode");

                    if (!v.isNil()) {
                        if (vmodeAndVperm_p[VMODE] != null && !vmodeAndVperm_p[VMODE].isNil()) {
                            throw context.runtime.newArgumentError("mode specified twice");
                        }
                        hasVmode = true;
                        vmodeAndVperm_p[VMODE] = v;

                        continue vmode_handle;
                    }
                }
                IRubyObject v = hashARef(context.runtime, options, "perm");
                if (!v.isNil()) {
                    if (vmodeAndVperm_p[PERM] != null) {
                        if (!vmodeAndVperm_p[PERM].isNil()) throw context.runtime.newArgumentError("perm specified twice");

                        vmodeAndVperm_p[PERM] = v;
                    }
                }
                
                ecflags = (fmode_p[0] & OpenFile.READABLE) != 0 ?
                        MODE_BTMODE(fmode_p[0], ECONV_DEFAULT_NEWLINE_DECORATOR, 0, ECONV_UNIVERSAL_NEWLINE_DECORATOR) : 0;
                if (TEXTMODE_NEWLINE_DECORATOR_ON_WRITE != -1) {
                    ecflags |= (fmode_p[0] & OpenFile.WRITABLE) != 0 ?
                            MODE_BTMODE(fmode_p[0], TEXTMODE_NEWLINE_DECORATOR_ON_WRITE, 0, TEXTMODE_NEWLINE_DECORATOR_ON_WRITE) : 0;
                }

                if (ioExtractEncodingOption(context, ioEncodable, options, fmode_p)) {
                    if (hasEnc) throw context.runtime.newArgumentError("encoding specified twice");
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
        
        if (options != null || !options.isNil()) {
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
            } else if (!(tmp = intenc.checkStringType19()).isNil()) {
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
            
            if (!(tmp = encoding.checkStringType19()).isNil()) {
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
                ((fmode & OpenFile.SETENC_BY_BOM) == 0) && internal == external) {
            encodable.setEnc((defaultExternal && internal != external) ? null : external);
            encodable.setEnc2(null);
        } else {
            encodable.setEnc(internal);
            encodable.setEnc2(external);
        }
    }    

    // mri: parse_mode_enc
    public static void parseModeEncoding(ThreadContext context, IOEncodable ioEncodable, String option, int[] fmode_p) {
        Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();
        Encoding idx, idx2 = null;
        Encoding intEnc, extEnc;
        if (fmode_p == null) fmode_p = new int[]{0};
        String estr;

        String[] encs = option.split(":", 2);

        if (encs.length == 2) {
            estr = encs[0];
            if (estr.toLowerCase().startsWith("bom|utf-")) {
                fmode_p[0] |= OpenFile.SETENC_BY_BOM;
                ioEncodable.setBOM(true);
                estr = estr.substring(4);
            }
            idx = context.runtime.getEncodingService().getEncodingFromString(estr);
        } else {
            estr = option;
            if (estr.toLowerCase().startsWith("bom|utf-")) {
                fmode_p[0] |= OpenFile.SETENC_BY_BOM;
                ioEncodable.setBOM(true);
                estr = estr.substring(4);
            }
            idx = context.runtime.getEncodingService().getEncodingFromString(estr);
        }

        extEnc = idx;
        
        intEnc = null;
        if (encs.length == 2) {
            if (encs[1].equals("-")) {
                intEnc = null;
            } else {
                idx2 = context.runtime.getEncodingService().getEncodingFromString(encs[1]);
                if (idx2 == idx) {
                    context.runtime.getWarnings().warn("ignoring internal encoding " + idx2 + ": it is identical to external encoding " + idx);
                    intEnc = null;
                } else {
                    intEnc = idx2;
                }
            }
        }

        ioExtIntToEncs(context, ioEncodable, extEnc, intEnc, fmode_p[0]);
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
        ecflags = econvOpts(context, opthash, ecflags);
        
        v = ((RubyHash)opthash).op_aref(context, context.runtime.newSymbol("replace"));
        if (!v.isNil()) {
            RubyString v_str = v.convertToString();
            if (v_str.scanForCodeRange() == StringSupport.CR_BROKEN) {
                throw context.runtime.newArgumentError("replacement string is broken: " + v_str);
            }
            v = v_str.freeze(context);
            newhash = RubyHash.newHash(context.runtime);
            ((RubyHash)newhash).op_aset(context, context.runtime.newSymbol("replace"), v);
        }
        
        v = ((RubyHash)opthash).op_aref(context, context.runtime.newSymbol("fallback"));
        if (!v.isNil()) {
            IRubyObject h = TypeConverter.checkHashType(context.runtime, v);
            boolean condition;
            if (h.isNil()) {
                condition = (h instanceof RubyProc || h instanceof RubyMethod || h.respondsTo("[]"));
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
    
    // rb_econv_opts
    public static int econvOpts(ThreadContext context, IRubyObject opt, int ecflags) {
        Ruby runtime = context.runtime;
        IRubyObject v;
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("invalid"));
        if (v.isNil()) {
        } else if (v == runtime.newSymbol("replace")) {
            ecflags |= ECONV_INVALID_REPLACE;
        } else {
            throw runtime.newArgumentError("unknown value for invalid character option");
        }
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("undef"));
        if (v.isNil()) {
        } else if (v == runtime.newSymbol("replace")) {
            ecflags |= ECONV_UNDEF_REPLACE;
        } else {
            throw runtime.newArgumentError("unknown value for undefined character option");
        }
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("replace"));
        if (!v.isNil() && (ecflags & ECONV_INVALID_REPLACE) != 0) {
            ecflags |= ECONV_UNDEF_REPLACE;
        }
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("xml"));
        if (!v.isNil()) {
            if (v == runtime.newSymbol("text")) {
                ecflags |= ECONV_XML_TEXT_DECORATOR|ECONV_UNDEF_HEX_CHARREF;
            } else if (v == runtime.newSymbol("attr")) {
                ecflags |= ECONV_XML_ATTR_CONTENT_DECORATOR|ECONV_XML_ATTR_QUOTE_DECORATOR|ECONV_UNDEF_HEX_CHARREF;
            } else {
                throw runtime.newArgumentError("unexpected value for xml option: " + v);
            }
        }
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("newline"));
        if (!v.isNil()) {
            ecflags &= ~ECONV_NEWLINE_DECORATOR_MASK;
            if (v == runtime.newSymbol("universal")) {
                ecflags |= ECONV_UNIVERSAL_NEWLINE_DECORATOR;
            } else if (v == runtime.newSymbol("crlf")) {
                ecflags |= ECONV_CRLF_NEWLINE_DECORATOR;
            } else if (v == runtime.newSymbol("cr")) {
                ecflags |= ECONV_CR_NEWLINE_DECORATOR;
            } else if (v == runtime.newSymbol("lf")) {
//                ecflags |= ECONV_LF_NEWLINE_DECORATOR;
            } else {
                throw runtime.newArgumentError("unexpected value for newline option");
            }
        }
        
        int setflags = 0;
        boolean newlineflag = false;
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("universal_newline"));
        if (v.isTrue()) {
            setflags |= ECONV_UNIVERSAL_NEWLINE_DECORATOR;
        }
        newlineflag |= !v.isNil();
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("crlf_newline"));
        if (v.isTrue()) {
            setflags |= ECONV_CRLF_NEWLINE_DECORATOR;
        }
        newlineflag |= !v.isNil();
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("cr_newline"));
        if (v.isTrue()) {
            setflags |= ECONV_CR_NEWLINE_DECORATOR;
        }
        newlineflag |= !v.isNil();
        
        if (newlineflag) {
            ecflags &= ~ECONV_NEWLINE_DECORATOR_MASK;
            ecflags |= setflags;
        }
        
        return ecflags;
    }
    
    // rb_econv_open_opts
    public static Transcoder econvOpenOpts(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, IRubyObject opthash) {
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
        
        return Transcoder.open(context, sourceEncoding, destinationEncoding, ecflags, replacement);
        // missing logic for checking replacement encoding, may live in CharsetTranscoder
        // already...
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
    public static int encAscget(byte[] bytes, int offset, int end, int[] chlen, Encoding enc) {
        int c;
        int l;
        
        if (enc.isAsciiCompatible()) {
            c = bytes[offset];
            if (!Encoding.isAscii((byte)c)) {
                return -1;
            }
            if (chlen != null) chlen[0] = 1;
            return c;
        }
        l = StringSupport.preciseLength(enc, bytes, offset, end);
        if (StringSupport.MBCLEN_CHARFOUND_LEN(l) == 0) {
            return -1;
        }
        c = enc.mbcToCode(bytes, offset, end);
        if (!Encoding.isAscii(c)) {
            return -1;
        }
        if (chlen != null) chlen[0] = 1;
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
        return new String(enc1).equalsIgnoreCase(new String(enc2));
    }
    
    // enc_arg
    public static Encoding encArg(ThreadContext context, IRubyObject encval, byte[][] name_p, Encoding[] enc_p) {
        if ((enc_p[0] = toEncodingIndex(context, encval)) == null) {
            name_p[0] = ((RubyString)encval.anyToString()).getBytes();
        } else {
            name_p[0] = enc_p[0].getName();
        }
        
        return enc_p[0];
    }
    
    // rb_to_encoding_index
    public static Encoding toEncodingIndex(ThreadContext context, IRubyObject enc) {
        if (enc instanceof RubyEncoding) {
            return ((RubyEncoding)enc).getEncoding();
        } else if ((enc = enc.checkStringType19()).isNil()) {
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
        if (((EncodingCapable)obj).getEncoding() == encidx) {
            return obj;
        }
        if (!((RubyString)obj).isCodeRangeAsciiOnly() ||
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
    
    // str_transcode
    public static Encoding strTranscode(ThreadContext context, IRubyObject[] args, IRubyObject[] self_p) {
        int ecflags = 0;
        int argc = args.length;
        IRubyObject[] ecopts_p = {context.nil};
        
        if (args.length >= 1) {
            IRubyObject tmp = TypeConverter.checkHashType(context.runtime, args[args.length -1]);
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
        
        if (argc > 2) {
            throw context.runtime.newArgumentError(args.length, 2);
        }
        
        if (argc == 0) {
            arg1 = runtime.getEncodingService().getDefaultInternal();
            if (arg1 == null || arg1.isNil()) {
                if (ecflags == 0) return null;
                arg1 = objEncoding(context, str);
            }
            ecflags |= EncodingUtils.ECONV_INVALID_REPLACE | EncodingUtils.ECONV_UNDEF_REPLACE;
        } else {
            arg1 = args[0];
        }
        
        arg2 = argc <= 1 ? context.nil : args[1];
        dencindex = strTranscodeEncArgs(context, str, arg1, arg2, sname_p, senc_p, dname_p, denc_p);
        
        if ((ecflags & (EncodingUtils.ECONV_NEWLINE_DECORATOR_MASK
                | EncodingUtils.ECONV_XML_TEXT_DECORATOR
                | EncodingUtils.ECONV_XML_ATTR_CONTENT_DECORATOR
                | EncodingUtils.ECONV_XML_ATTR_QUOTE_DECORATOR)) == 0) {
            if (senc_p[0] != null && senc_p[0] == denc_p[0]) {                
                // TODO: Ruby 2.0 or 2.1 use String#scrub here
                if ((ecflags & EncodingUtils.ECONV_INVALID_MASK) != 0) {
                    // TODO: scrub with replacement
                    str = str.dup();
                } else {
                    // TODO: scrub without replacement
                    str = str.dup();
                }
                self_p[0] = str;
                return dencindex;
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
        
        ByteList fromp = ((RubyString)str).getByteList().shallowDup();
        RubyString dest = runtime.newString();
        ByteList destp = dest.getByteList();
        
        transcodeLoop(context, fromp, destp, sname_p[0], dname_p[0], ecflags, ecopts);
        
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
    
    // transcode_loop
    public static void transcodeLoop(ThreadContext context, ByteList fromp, ByteList dest, byte[] sname, byte[] dname, int ecflags, IRubyObject ecopts) {
        Transcoder ec;
        
        ec = econvOpenOpts(context, sname, dname, ecflags, ecopts);
        
        if (ec == null) {
            throw econvOpenExc(context, sname, dname, ecflags);
        }
        
        // TODO: fallback function
        
        RubyCoderResult result = ec.transcode(context, fromp, dest);
    }
    
    // io_set_encoding_by_bom
    public static void ioSetEncodingByBOM(ThreadContext context, RubyIO io) {
        Ruby runtime = context.runtime;
        Encoding bomEncoding = ioStripBOM(io);
        
        if (bomEncoding != null) {
            // FIXME: Wonky that we acquire RubyEncoding to pass these encodings through
            IRubyObject theBom = runtime.getEncodingService().getEncoding(bomEncoding);
            IRubyObject theInternal = io.internal_encoding(context);

            io.setEncoding(runtime.getCurrentContext(), theBom, theInternal, context.nil);
        }
    }
    
    // mri: io_strip_bom
    public static Encoding ioStripBOM(RubyIO io) {
        int b1, b2, b3, b4;

        switch (b1 = io.getcCommon()) {
            case 0xEF:
                b2 = io.getcCommon();
                if (b2 == 0xBB) {
                    b3 = io.getcCommon();
                    if (b3 == 0xBF) {
                        return UTF8Encoding.INSTANCE;
                    }
                    io.ungetcCommon(b3);
                }
                io.ungetcCommon(b2);
                break;
            case 0xFE:
                b2 = io.getcCommon();
                if (b2 == 0xFF) {
                    return UTF16BEEncoding.INSTANCE;
                }
                io.ungetcCommon(b2);
                break;
            case 0xFF:
                b2 = io.getcCommon();
                if (b2 == 0xFE) {
                    b3 = io.getcCommon();
                    if (b3 == 0) {
                        b4 = io.getcCommon();
                        if (b4 == 0) {
                            return UTF32LEEncoding.INSTANCE;
                        }
                        io.ungetcCommon(b4);
                    } else {
                        io.ungetcCommon(b3);
                        return UTF16LEEncoding.INSTANCE;
                    }
                    io.ungetcCommon(b3);
                }
                io.ungetcCommon(b2);
                break;
            case 0:
                b2 = io.getcCommon();
                if (b2 == 0) {
                    b3 = io.getcCommon();
                    if (b3 == 0xFE) {
                        b4 = io.getcCommon();
                        if (b4 == 0xFF) {
                            return UTF32BEEncoding.INSTANCE;
                        }
                        io.ungetcCommon(b4);
                    }
                    io.ungetcCommon(b3);
                }
                io.ungetcCommon(b2);
                break;
        }
        io.ungetcCommon(b1);
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
        
        if ((fmode & OpenFile.BINMODE) == 0 && (EncodingUtils.DEFAULT_TEXTMODE != 0 || (ecflags & EncodingUtils.ECONV_DECORATOR_MASK) != 0)) {
            fmode |= EncodingUtils.DEFAULT_TEXTMODE;
            fmode_p[0] = fmode;
        } else if (EncodingUtils.DEFAULT_TEXTMODE == 0 && (ecflags & ECONV_NEWLINE_DECORATOR_MASK) == 0) {
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

    // str_buf_cat
    public static void strBufCat(Ruby runtime, RubyString str, ByteList ptr) {
        int total, off = -1;

        // termlen is not relevant since we have no termination sequence

        // missing: if ptr string is inside str, off = ptr start minus str start

        str.modify();
        if (ptr.length() == 0) return;

        // much logic is missing here, since we don't manually manage the ByteList buffer

        total = str.size() + ptr.length();
        str.getByteList().ensure(total);
        str.getByteList().append(ptr);
    }

    // rb_enc_str_buf_cat
    public static void encStrBufCat(Ruby runtime, RubyString str, ByteList ptr, Encoding enc) {
        encCrStrBufCat(runtime, str, ptr,
                enc, StringSupport.CR_UNKNOWN, null);
    }

    // rb_enc_cr_str_buf_cat
    public static void encCrStrBufCat(Ruby runtime, RubyString str, ByteList ptr, Encoding ptrEnc, int ptr_cr, int[] ptr_cr_ret) {
        Encoding strEnc = str.getEncoding();
        Encoding resEnc;
        int str_cr, res_cr;
        boolean incompatible = false;

        str_cr = str.size() > 0 ? str.getCodeRange() : StringSupport.CR_7BIT;

        if (strEnc == ptrEnc) {
            if (str_cr == StringSupport.CR_UNKNOWN) {
                ptr_cr = StringSupport.CR_UNKNOWN;
            } else if (ptr_cr == StringSupport.CR_UNKNOWN) {
                ptr_cr = StringSupport.codeRangeScan(ptrEnc, ptr);
            }
        } else {
            if (!strEnc.isAsciiCompatible() || !ptrEnc.isAsciiCompatible()) {
                if (ptr.getRealSize() == 0) {
                    return;
                }
                if (str.size() == 0) {
                    rbStrBufCat(runtime, str, ptr);
                    str.setEncodingAndCodeRange(ptrEnc, ptr_cr);
                    return;
                }
                incompatible = true;
            }
            if (!incompatible) {
                if (ptr_cr == StringSupport.CR_UNKNOWN) {
                    ptr_cr = StringSupport.codeRangeScan(ptrEnc, ptr);
                }
                if (str_cr == StringSupport.CR_UNKNOWN) {
                    if (strEnc == ASCIIEncoding.INSTANCE || ptr_cr != StringSupport.CR_7BIT) {
                        str_cr = str.scanForCodeRange();
                    }
                }
            }
        }
        if (ptr_cr_ret != null) {
            ptr_cr_ret[0] = ptr_cr;
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
            if (0 < ptr.getRealSize()) res_cr = StringSupport.CR_UNKNOWN;
        }

        // MRI checks for len < 0 here, but I don't think that's possible for us

        strBufCat(runtime, str, ptr);
        str.setEncodingAndCodeRange(resEnc, res_cr);
    }
}
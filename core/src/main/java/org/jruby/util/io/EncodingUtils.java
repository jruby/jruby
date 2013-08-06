package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyEncoding;
import org.jruby.RubyHash;
import org.jruby.RubyMethod;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.CharsetTranscoder;
import org.jruby.util.TypeConverter;

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

    public static Encoding toEncoding(ThreadContext context, IRubyObject object) {
        if (object instanceof RubyEncoding) return ((RubyEncoding) object).getEncoding();
        
        return context.runtime.getEncodingService().getEncodingFromObject(object);
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
    public static int extractBinmode(Ruby runtime, IRubyObject optionsArg, int fmode) {
        IRubyObject v = hashARef(runtime, optionsArg, "textmode");
        if (!v.isNil() && v.isTrue()) fmode |= OpenFile.TEXTMODE;
        
        v = hashARef(runtime, optionsArg, "binmode");
        if (!v.isNil() && v.isTrue()) fmode |= OpenFile.BINMODE;

        if ((fmode & OpenFile.BINMODE) != 0 && (fmode & OpenFile.TEXTMODE) != 0) {
            throw runtime.newArgumentError("both textmode and binmode specified");
        }
        
        return fmode;
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
    
    /*
     * This is a wacky method which is a very near port from MRI.  pm passes in 
     * a permissions value and a mode value.  As a side-effect mode will get set
     * if this found any 'mode'-like stuff so the caller can know whether mode 
     * has been handled yet.   The same story for permission value.  If it has
     * not been set then we know it needs to default permissions from the caller.
     */
    // mri: rb_io_extract_modeenc
    public static int extractModeEncoding(ThreadContext context, 
            IOEncodable ioEncodable, IRubyObject[] pm, IRubyObject options, boolean secondTime) {
        int fmode; // OpenFile
        boolean hasEncoding = false;
        int oflags = 0; // ModeFlags
        
        // Give default encodings
        setupReadWriteEncodings(context, ioEncodable, null, null);

        if (pm[VMODE] == null || pm[VMODE].isNil()) {
            fmode = OpenFile.READABLE;
            oflags = ModeFlags.RDONLY;
        } else {
            IRubyObject intMode = TypeConverter.checkIntegerType(context.runtime, pm[VMODE], "to_int");
            
            if (!intMode.isNil()) {
                pm[VMODE] = intMode;
                oflags = RubyNumeric.num2int(intMode);
                fmode = ModeFlags.getOpenFileFlagsFor(oflags);
            } else {
                String p = pm[VMODE].convertToString().asJavaString();
                int colonSplit = p.indexOf(":");
                String mode = colonSplit == -1 ? p : p.substring(0, colonSplit);
                try {
                    fmode = OpenFile.getFModeFromString(mode);
                    oflags = OpenFile.getModeFlagsAsIntFrom(fmode);
                } catch (InvalidValueException e) {
                    throw context.runtime.newArgumentError("illegal access mode " + pm[VMODE]);
                }
                
                if (colonSplit != -1) {
                    hasEncoding = true;
                    parseModeEncoding(context, ioEncodable, p.substring(colonSplit + 1));
                } else {
                    Encoding e = (fmode & OpenFile.BINMODE) != 0 ? ascii8bitEncoding(context.runtime) : null;
                    setupReadWriteEncodings(context, ioEncodable, null, e);
                }
            }
        }
        
        if (options == null || options.isNil()) {
            // FIXME: Set up ecflags
        } else {
            fmode = extractBinmode(context.runtime, options, fmode);
            // Differs from MRI but we open with ModeFlags
            oflags |= OpenFile.getModeFlagsAsIntFrom(fmode);

            // FIXME: What is DEFAULT_TEXTMODE
            
            if (!secondTime) {
                IRubyObject v = hashARef(context.runtime, options, "mode");
                
                if (!v.isNil()) {
                    if (pm[VMODE] != null && !pm[VMODE].isNil()) {
                        throw context.runtime.newArgumentError("mode specified twice");
                    }
                    secondTime = true;
                    pm[VMODE] = v;
                  
                    return extractModeEncoding(context, ioEncodable, pm, options, true);
                }
            } 
            IRubyObject v = hashARef(context.runtime, options, "perm");
            if (!v.isNil()) {
                if (pm[PERM] != null) {
                    if (!pm[PERM].isNil()) throw context.runtime.newArgumentError("perm specified twice");
                    
                    pm[PERM] = v;
                }
            }
        
            if (getEncodingOptionFromObject(context, ioEncodable, options)) {
                if (hasEncoding) throw context.runtime.newArgumentError("encoding specified twice");
            }
            
            
        }
        
        return oflags;
    }

    // mri: rb_io_extract_encoding_option
    public static boolean getEncodingOptionFromObject(ThreadContext context, IOEncodable ioEncodable, IRubyObject options) {
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
            extencoding = toEncoding(context, extenc);
        }

        if (intenc != null) {
            if (intenc.isNil()) {
                intencoding = null;
            } else if (!(tmp = intenc.checkStringType19()).isNil()) {
                String p = tmp.toString();
                if (p.equals("-")) {
                    intencoding = null;
                } else {
                    intencoding = toEncoding(context, intenc);
                }
            } else {
                intencoding = toEncoding(context, intenc);
            }
            if (extencoding == intencoding) {
                intencoding = null;
            }
        }
        
        if (!encoding.isNil()) {
            extracted = true;
            
            if (!(tmp = encoding.checkStringType19()).isNil()) {
                parseModeEncoding(context, ioEncodable, tmp.convertToString().toString());
            } else {
                setupReadWriteEncodings(context, ioEncodable, null, toEncoding(context, encoding));
            }
        } else if (extenc != null || intenc != null) {
            extracted = true;
            setupReadWriteEncodings(context, ioEncodable, intencoding, extencoding);
        }
        
        return extracted;
    }
    
    // mri: rb_io_ext_int_to_encs
    public static void setupReadWriteEncodings(ThreadContext context, IOEncodable encodable, 
            Encoding internal, Encoding external) {
        boolean defaultExternal = false;
        
        if (external == null) {
            external = context.runtime.getDefaultExternalEncoding();
            defaultExternal = true;
        }
        
        if (internal == null && external != ascii8bitEncoding(context.runtime)) {
            internal = context.runtime.getDefaultInternalEncoding();
        }
        
        if (internal == null || internal == external) { // missing internal == nil?
            encodable.setEnc((defaultExternal && internal != external) ? null : external);
            encodable.setEnc2(null);
        } else {
            encodable.setEnc(internal);
            encodable.setEnc2(external);
        }
    }    

    // mri: parse_mode_enc
    public static void parseModeEncoding(ThreadContext context, IOEncodable ioEncodable, String option) {
        Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();
        Encoding intEncoding = null;

        String[] encs = option.split(":", 2);

        if (encs[0].toLowerCase().startsWith("bom|utf-")) {
            ioEncodable.setBOM(true);
            encs[0] = encs[0].substring(4);
        }

        Encoding extEncoding = service.getEncodingFromString(encs[0]);

        if (encs.length > 1) {
            if (encs[1].equals("-")) {
                // null;
            } else {
                intEncoding = service.getEncodingFromString(encs[1]);
            }
        }

        setupReadWriteEncodings(context, ioEncodable, intEncoding, extEncoding);
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
            if (v_str.isCodeRangeBroken()) {
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
            } else if (v == runtime.newSymbol("text")) {
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
        
        v = ((RubyHash)opt).op_aref(context, runtime.newSymbol("newline"));
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
    public static CharsetTranscoder econvOpenOpts(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, IRubyObject opthash) {
        Ruby runtime = context.runtime;
        CharsetTranscoder ec;
        IRubyObject replacement;
        
        if (opthash.isNil()) {
            replacement = context.nil;
        } else {
            if (!(opthash instanceof RubyHash) || !opthash.isFrozen()) {
                throw runtime.newRuntimeError("bug: EncodingUtils.econvOpenOpts called with invalid opthash");
            }
            replacement = ((RubyHash)opthash).op_aref(context, runtime.newSymbol("replace"));
        }
        
        return CharsetTranscoder.open(context, sourceEncoding, destinationEncoding, ecflags, replacement);
        // missing logic for checking replacement encoding, may live in CharsetTranscoder
        // already...
    }
}

    
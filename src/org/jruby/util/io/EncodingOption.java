package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;

public class EncodingOption {
    private Encoding externalEncoding;
    private Encoding internalEncoding;
    private boolean bom;

    public EncodingOption(Encoding externalEncoding, Encoding internalEncoding, boolean bom) {
        this.externalEncoding = externalEncoding;
        this.internalEncoding = internalEncoding;
        this.bom = bom;
    }

    public Encoding getExternalEncoding() {
        return externalEncoding;
    }

    public Encoding getInternalEncoding() {
        return internalEncoding;
    }

    public boolean hasBom() {
        return bom;
    }
    
    //FIXME: Temporary...
    public static Encoding toEncoding(ThreadContext context, IRubyObject object) {
        if (object instanceof RubyEncoding) return ((RubyEncoding) object).getEncoding();
        
        return context.runtime.getEncodingService().getEncodingFromObject(object);
    }

    // c: rb_io_extract_encoding_option
    public static boolean getEncodingOptionFromObject(ThreadContext context, IOEncodable ioEncodable, IRubyObject options) {
        if (options == null || options.isNil() || !(options instanceof RubyHash)) return false;

        RubyHash opts = (RubyHash) options;        
        boolean extracted = false;
        Encoding externalEncoding = null;
        
        Ruby runtime = options.getRuntime();
        IRubyObject encodingOpt = opts.fastARef(runtime.newSymbol("encoding"));
        IRubyObject externalOpt = opts.fastARef(runtime.newSymbol("external_encoding"));
        IRubyObject internalOpt = opts.fastARef(runtime.newSymbol("internal_encoding"));
        
        if ((externalOpt != null || internalOpt != null) && encodingOpt != null && !encodingOpt.isNil()) {
                runtime.getWarnings().warn("Ignoring encoding parameter '" + encodingOpt + "': " + 
                        (externalOpt == null ? "internal" : "external") + "_encoding is used");
                encodingOpt = null;
        }
        
        if (externalOpt != null && !externalOpt.isNil()) externalEncoding = toEncoding(context, externalOpt);

        Encoding internalEncoding = null;

        if (internalOpt != null) {
            if (internalOpt.isNil() || internalOpt.asString().toString().equals("-")) {
                internalEncoding = null;
            } else {
                internalEncoding = toEncoding(context, options);
            }
            
            if (externalEncoding == internalEncoding) internalEncoding = null;
        }
        
        if (encodingOpt != null && !encodingOpt.isNil()) {
            extracted = true;
            
            IRubyObject tmp = encodingOpt.checkStringType19();
            if (!tmp.isNil()) {
                parseModeEncoding(context, ioEncodable, tmp.convertToString().toString());
            } else {
                setupReadWriteEncodings(context, ioEncodable, toEncoding(context, tmp), null);
            }
        } else if (externalOpt != null || internalEncoding != null) {
            extracted = true;
            setupReadWriteEncodings(context, ioEncodable, externalEncoding, internalEncoding);
        }
        
        return extracted;
    }
    
    // MRI: rb_io_ext_int_to_encs
    public static void setupReadWriteEncodings(ThreadContext context, IOEncodable encodable, 
            Encoding internal, Encoding external) {
        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();
        boolean defaultExternal = false;
        
        if (external == null) {
            external = context.runtime.getDefaultExternalEncoding();
            defaultExternal = true;
        }
        
        if (internal == null && external != ascii8bit) {
            internal = context.runtime.getDefaultInternalEncoding();
        }
        
        if (internal == null || internal == external) { // missing internal == nil?
            encodable.setReadEncoding((defaultExternal && internal != external) ? null : external);
            encodable.setWriteEncoding(null);
        } else {
            encodable.setReadEncoding(internal);
            encodable.setWriteEncoding(external);
        }
    }    

    // c: parse_mode_enc
    public static void parseModeEncoding(ThreadContext context, IOEncodable ioEncodable, String option) {
        Ruby runtime = context.runtime;
        EncodingService service = runtime.getEncodingService();
        Encoding intEncoding = null;
        boolean isBom = false;
        
        // FIXME: Did not mark BOM

        String[] encs = option.split(":", 2);

        if (encs[0].toLowerCase().startsWith("bom|utf-")) {
            isBom = true;
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

    // c: parse_mode_enc
    public static EncodingOption getEncodingNoOption(Ruby runtime, ModeFlags modeFlags) {
        if (modeFlags.isBinary()) {
            return new EncodingOption(ASCIIEncoding.INSTANCE, null, false);
        }

        return new EncodingOption(null, null, false);
    }

    @Override
    public String toString() {
        return "EncodingOption(int:" + internalEncoding + ", ext:" + externalEncoding + ", bom:" + bom + ")";
    }
}

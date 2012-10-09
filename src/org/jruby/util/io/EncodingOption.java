package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyHash;
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

    // c: rb_io_extract_encoding_option
    public static EncodingOption getEncodingOptionFromObject(IRubyObject options) {
        if (options == null || options.isNil() || !(options instanceof RubyHash)) return null;

        RubyHash opts = (RubyHash) options;

        Ruby runtime = options.getRuntime();
        IRubyObject encOption = opts.fastARef(runtime.newSymbol("encoding"));
        IRubyObject extOption = opts.fastARef(runtime.newSymbol("external_encoding"));
        IRubyObject intOption = opts.fastARef(runtime.newSymbol("internal_encoding"));
        if (encOption != null && !encOption.isNil()) {
            if (extOption != null) {
                runtime.getWarnings().warn(
                        "Ignoring encoding parameter '" + encOption
                                + "': external_encoding is used");
                encOption = runtime.getNil();
            } else if (intOption != null) {
                runtime.getWarnings().warn(
                        "Ignoring encoding parameter '" + encOption
                                + "': internal_encoding is used");
                encOption = runtime.getNil();
            } else {
                IRubyObject tmp = encOption.checkStringType19();
                if (!tmp.isNil()) {
                    return getEncodingOptionFromString(runtime, tmp.convertToString().toString());
                }
                return createEncodingOption(runtime, runtime.getEncodingService()
                        .getEncodingFromObject(encOption), null, false);
            }
        }
        boolean set = false;
        Encoding extEncoding = null;
        Encoding intEncoding = null;

        if (extOption != null) {
            set = true;
            if (!extOption.isNil()) {
                extEncoding = runtime.getEncodingService().getEncodingFromObject(extOption);
            }
        }
        if (intOption != null) {
            set = true;
            if (intOption.isNil()) {
                // null;
            } else if (intOption.asString().toString().equals("-")) {
                // null;
            } else {
                intEncoding = runtime.getEncodingService().getEncodingFromObject(intOption);
            }
        }
        if (!set)
            return null;

        return createEncodingOption(runtime, extEncoding, intEncoding, false);
    }

    // c: rb_io_ext_int_to_encs
    private static EncodingOption createEncodingOption(Ruby runtime, Encoding extEncoding,
                                                       Encoding intEncoding, boolean isBom) {
        boolean defaultExt = false;
        if (extEncoding == null) {
            extEncoding = runtime.getDefaultExternalEncoding();
            defaultExt = true;
        }
        if (intEncoding == null && extEncoding != ASCIIEncoding.INSTANCE) {
            /* If external is ASCII-8BIT, no default transcoding */
            intEncoding = runtime.getDefaultInternalEncoding();
        }
        if (intEncoding == null || intEncoding == extEncoding) {
            /* No internal encoding => use external + no transcoding */
            // JRuby passes extEncoding instead of null for external, since we use this for final encoding of strings
            return new EncodingOption(
                    extEncoding,
                    (defaultExt && intEncoding != extEncoding) ? null : extEncoding,
                    isBom);
        } else {
            return new EncodingOption(
                    extEncoding,
                    intEncoding,
                    isBom);
        }
    }

    // c: parse_mode_enc
    public static EncodingOption getEncodingOptionFromString(Ruby runtime, String option) {
        EncodingService service = runtime.getEncodingService();
        Encoding ascii8bit = service.getAscii8bitEncoding();
        Encoding extEncoding = null;
        Encoding intEncoding = null;
        boolean isBom = false;

        String[] encs = option.split(":", 2);

        if (encs[0].toLowerCase().startsWith("bom|utf-")) {
            isBom = true;
            encs[0] = encs[0].substring(4);
        }

        extEncoding = service.getEncodingFromString(encs[0]);

        if (encs.length > 1) {
            if (encs[1].equals("-")) {
                // null;
            } else {
                intEncoding = service.getEncodingFromString(encs[1]);
            }
        }

        // Duplicating some rb_io_ext_int_to_encs logic here, which is also in RubyIO#setupReadWriteEncodings.

        if (extEncoding == null) {
            extEncoding = runtime.getDefaultExternalEncoding();
        }

        if (intEncoding == null && extEncoding != ASCIIEncoding.INSTANCE) {
            intEncoding = runtime.getDefaultInternalEncoding();
        }

        return new EncodingOption(extEncoding, intEncoding, isBom);
    }

    // c: parse_mode_enc
    public static EncodingOption getEncodingNoOption(Ruby runtime, ModeFlags modeFlags) {
        if (modeFlags.isBinary()) {
            return new EncodingOption(ASCIIEncoding.INSTANCE, null, false);
        }

        return new EncodingOption(null, null, false);
    }

    public String toString() {
        return "EncodingOption(int:" + internalEncoding + ", ext:" + externalEncoding + ", bom:" + bom + ")";
    }
}

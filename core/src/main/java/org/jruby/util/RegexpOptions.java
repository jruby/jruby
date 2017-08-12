/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.EUCJPEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import static org.jruby.parser.ReOptions.*;

public class RegexpOptions implements Cloneable {
    private static ByteList WINDOWS31J = new ByteList(new byte[] {'W', 'i', 'n', 'd', 'o', 'w', 's', '-', '3', '1', 'J'});
    public static final RegexpOptions NULL_OPTIONS = new RegexpOptions();
    
    public RegexpOptions() {
        this.embedded = newEmbeddedOptions();
    }
    
    public RegexpOptions(KCode kcode, boolean isKCodeDefault) {
        assert kcode != null : "kcode must always be set to something";

        this.embedded = newEmbeddedOptions(kcode, isKCodeDefault);
    }

    public RegexpOptions(int embedded) {
        assert getKCode(embedded) != null : "kcode must always be set to something";

        this.embedded = embedded;
    }

    public static int newEmbeddedOptions() {
        return newEmbeddedOptions(KCode.NONE, true);
    }

    public static int newEmbeddedOptions(KCode kcode, boolean isKCodeDefault) {
        return setKCode(setKCodeDefault(0, isKCodeDefault), kcode);
    }

    public boolean isExtended() {
        return isExtended(embedded);
    }

    public static boolean isExtended(int embedded) {
        return (embedded & RE_OPTION_EXTENDED) == RE_OPTION_EXTENDED;
    }

    public void setExtended(boolean extended) {
        embedded = setExtended(embedded, extended);
    }

    public static int setExtended(int embedded, boolean extended) {
        return setFlag(embedded, RE_OPTION_EXTENDED, extended);
    }

    public boolean isIgnorecase() {
        return isIgnorecase(embedded);
    }

    public static boolean isIgnorecase(int embedded) {
        return (embedded & RE_OPTION_IGNORECASE) == RE_OPTION_IGNORECASE;
    }

    public void setIgnorecase(boolean ignorecase) {
        embedded = setIgnorecase(embedded, ignorecase);
    }

    public static int setIgnorecase(int embedded, boolean ignorecase) {
        return setFlag(embedded, RE_OPTION_IGNORECASE, ignorecase);
    }

    public boolean isFixed() {
        return isFixed(embedded);
    }

    public static boolean isFixed(int embedded) {
        return (embedded & RE_FIXED) == RE_FIXED;
    }

    public void setFixed(boolean fixed) {
        embedded = setFixed(embedded, fixed);
    }

    public static int setFixed(int embedded, boolean fixed) {
        return setFlag(embedded, RE_FIXED, fixed);
    }

    public boolean isMultiline() {
        return isMultiline(embedded);
    }

    public static boolean isMultiline(int embedded) {
        return (embedded & RE_OPTION_MULTILINE) == RE_OPTION_MULTILINE;
    }

    public void setMultiline(boolean multiline) {
        embedded = setMultiline(embedded, multiline);
    }

    public static int setMultiline(int embedded, boolean multiline) {
        return setFlag(embedded, RE_OPTION_MULTILINE, multiline);
    }

    public boolean isOnce() {
        return isOnce(embedded);
    }

    public static boolean isOnce(int embedded) {
        return (embedded & RE_OPTION_ONCE) == RE_OPTION_ONCE;
    }

    public void setOnce(boolean once) {
        embedded = setFlag(embedded, RE_OPTION_ONCE, once);
    }

    public boolean isJava() {
        return isJava(embedded);
    }

    public static boolean isJava(int embedded) {
        return (embedded & RE_JAVA) == RE_JAVA;
    }

    public void setJava(boolean java) {
        embedded = setFlag(embedded, RE_JAVA, java);
    }

    public boolean isEncodingNone() {
        return isEncodingNone(embedded);
    }

    public static boolean isEncodingNone(int embedded) {
        return (embedded & RE_NONE) == RE_NONE;
    }

    public void setEncodingNone(boolean encodingNone) {
        embedded = setEncodingNone(embedded, encodingNone);
    }

    public static int setEncodingNone(int embedded, boolean encodingNone) {
        return setFlag(embedded, RE_NONE, encodingNone);
    }

    public boolean isLiteral() {
        return isLiteral(embedded);
    }

    public static boolean isLiteral(int embedded) {
        return (embedded & RE_LITERAL) == RE_LITERAL;
    }

    public void setLiteral(boolean literal) {
        embedded = setLiteral(embedded, literal);
    }

    public static int setLiteral(int embedded, boolean literal) {
        return setFlag(embedded, RE_LITERAL, literal);
    }

    public boolean isEmbeddable() {
        return isMultiline() && isIgnorecase() && isExtended();
    }

    public static boolean isEmbeddable(int embedded) {
        return isMultiline(embedded) && isIgnorecase(embedded) && isExtended(embedded);
    }

    public void setKCodeDefault(boolean kcodeDefault) {
        embedded = setKCodeDefault(embedded, kcodeDefault);
    }

    public static int setKCodeDefault(int embedded, boolean kcodeDefault) {
        return setFlag(embedded, RE_DEFAULT, kcodeDefault);
    }

    /**
     * Whether the kcode associated with this regexp is implicit (aka
     * default) or is specified explicitly (via 'nesu' syntax postscript or
     * flags to Regexp.new.
     */
    public boolean isKCodeDefault() {
        return isKCodeDefault(embedded);
    }

    public static boolean isKCodeDefault(int embedded) {
        return (embedded & RE_DEFAULT) == RE_DEFAULT;
    }

    public KCode getKCode() {
        return getKCode(embedded);
    }

    public static KCode getKCode(int embedded) {
        return KCode.values()[embedded >>> 16];
    }

    public void setKCode(KCode kcode) {
        embedded = setKCode(embedded, kcode);
    }

    public static int setKCode(int embedded, KCode kcode) {
        return embedded & 0xFFFF | kcode.ordinal() << 16;
    }

    /**
     * This regexp has an explicit encoding flag or 'nesu' letter associated
     * with it.
     *
     * @param kcode to be set
     */
    public void setExplicitKCode(KCode kcode) {
        this.setKCode(kcode);
        setKCodeDefault(false);
    }

    private KCode getExplicitKCode() {
        if (isKCodeDefault() == true) return null;

        return getKCode();
    }

    /**
     * Set the given flag bit to the given boolean value.
     *
     * The top two bytes of the embedded flags are preserved for KCode.
     *
     * @param flag flag with appropriate bit set
     * @param set whether to set or clear in the embedded
     */
    private final static int setFlag(int embedded, int flag, boolean set) {
        if (set) {
            embedded |= flag;
        } else {
            embedded &= ~flag | 0xFFFF0000;
        }
        return embedded;
    }
    
    /**
     * Calculate the encoding based on kcode option set via 'nesu'.  Also as
     * side-effects:
     * 1.set whether this marks the soon to be made regexp as  'fixed'. 
     * 2.kcode.none will set 'none' option
     * @return null if no explicit encoding is specified.
     */
    public Encoding setup(Ruby runtime) {
        KCode explicitKCode = getExplicitKCode();
        
        // None will not set fixed
        if (explicitKCode == KCode.NONE) {
            setEncodingNone((boolean) true);
            return ASCIIEncoding.INSTANCE;
        }
        
        if (explicitKCode == KCode.EUC) {
            setFixed(true);
            return EUCJPEncoding.INSTANCE;
        } else if (explicitKCode == KCode.SJIS) {
            setFixed(true);
            return runtime.getEncodingService().loadEncoding(WINDOWS31J);
        } else if (explicitKCode == KCode.UTF8) {
            setFixed(true);
            return UTF8Encoding.INSTANCE;
        }
        
        return null;
    }
    
    /**
     * This int value can be used by compiler or any place where we want
     * an integer representation of the state of this object.
     * 
     * Note: This is for full representation of state in the JIT.  It is not
     * to be confused with state of marshalled regexp data.
     */
    public int toEmbeddedOptions() {
        int options = toJoniOptions();

        if (isOnce()) options |= RE_OPTION_ONCE;
        if (isLiteral()) options |= RE_LITERAL;
        if (isKCodeDefault()) options |= RE_DEFAULT;
        if (isFixed()) options |= RE_FIXED;
        if (isEncodingNone()) options |= RE_NONE;
        if (isJava()) options |= RE_JAVA;

        return options;
    }

    // Note: once is not an option that is pertinent to Joni so we exclude it.
    private static final int EMBEDDED_TO_JONI_MASK = RE_OPTION_MULTILINE | RE_OPTION_IGNORECASE | RE_OPTION_EXTENDED;

    /**
     * This int value is meant to only be used when dealing directly with
     * the joni regular expression library.  It differs from embeddedOptions
     * in that it only contains bit values which Joni cares about.
     */
    public int toJoniOptions() {
        return fromEmbeddedToJoniOptions(embedded);
    }

    public static int fromEmbeddedToJoniOptions(int embedded) {
        return embedded & EMBEDDED_TO_JONI_MASK;
    }
    
    private static final int EMBEDDED_TO_RUBY_MASK = RE_OPTION_MULTILINE | RE_OPTION_IGNORECASE | RE_OPTION_EXTENDED | RE_FIXED | RE_NONE;
    
    /**
     * This int value is used by Regex#options
     */
    public int toOptions() {
        return fromEmbeddedToOptions(embedded);
    }

    public static int fromEmbeddedToOptions(int embedded) {
        return embedded & EMBEDDED_TO_RUBY_MASK;
    }

    public static RegexpOptions fromEmbeddedOptions(int embeddedOptions) {
        return new RegexpOptions(embeddedOptions);
    }

    public static RegexpOptions fromJoniOptions(int joniOptions) {
        RegexpOptions options = new RegexpOptions();
        options.setMultiline((joniOptions & RE_OPTION_MULTILINE) != 0);
        options.setIgnorecase((joniOptions & RE_OPTION_IGNORECASE) != 0);
        options.setExtended((joniOptions & RE_OPTION_EXTENDED) != 0);
        options.setFixed((joniOptions & RE_FIXED) != 0);
        options.setOnce((joniOptions & RE_OPTION_ONCE) != 0);

        return options;
    }

    public static int embeddedFromJoniOptions(int joniOptions) {
        return newEmbeddedOptions() | (joniOptions & EMBEDDED_TO_JONI_MASK);
    }

    public RegexpOptions withoutOnce() {
        RegexpOptions options = (RegexpOptions)clone();
        options.setOnce(false);
        return options;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (getKCode() != null ? getKCode().hashCode() : 0);
        hash = 11 * hash + (this.isFixed() ? 1 : 0);
        hash = 11 * hash + (this.isOnce() ? 1 : 0);
        hash = 11 * hash + (this.isExtended() ? 1 : 0);
        hash = 11 * hash + (this.isMultiline() ? 1 : 0);
        hash = 11 * hash + (this.isIgnorecase() ? 1 : 0);
        hash = 11 * hash + (this.isJava() ? 1 : 0);
        hash = 11 * hash + (this.isEncodingNone() ? 1 : 0);
        hash = 11 * hash + (this.isKCodeDefault() ? 1 : 0);
        hash = 11 * hash + (this.isLiteral() ? 1 : 0);
        return hash;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {throw new RuntimeException(cnse);}
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RegexpOptions)) return false;

        return equals(embedded, ((RegexpOptions) other).embedded);
    }

    public static boolean equals(int embedded1, int embedded2) {
        // Note: literal and once can be different in this object but for the
        // sake of equality we ignore those two fields since those flags do
        // not affect Ruby equality.
        boolean equality =
                isExtended(embedded1)   == isExtended(embedded2) &&
                isFixed(embedded1)      == isFixed(embedded2) &&
                isIgnorecase(embedded1) == isIgnorecase(embedded2) &&
                isJava(embedded1)       == isJava(embedded2) &&
                isMultiline(embedded1)  == isMultiline(embedded2);

        if (isEncodingNone(embedded1) || isEncodingNone(embedded2)) {
            return equality && getKCode(embedded1) == getKCode(embedded2);
        } else {
            return equality &&
                    isEncodingNone(embedded1) == isEncodingNone(embedded2) &&
                    getKCode(embedded1)       == getKCode(embedded2) &&
                    isKCodeDefault(embedded1) == isKCodeDefault(embedded2);
        }
    }

    @Override
    public String toString() {
        return toString(embedded);
    }
    
    public static String toString(int embedded) {
        return "RegexpOptions(kcode: " + getKCode(embedded) +
                (isEncodingNone(embedded) == true ? ", encodingNone" : "") +
                (isExtended(embedded) == true ? ", extended" : "") +
                (isFixed(embedded) == true ? ", fixed" : "") +
                (isIgnorecase(embedded) == true ? ", ignorecase" : "") +
                (isJava(embedded) == true ? ", java" : "") +
                (isKCodeDefault(embedded) == true ? ", kcodeDefault" : "") +
                (isLiteral(embedded) == true ? ", literal" : "") +
                (isMultiline(embedded) == true ? ", multiline" : "") +
                (isOnce(embedded) == true ? ", once" : "") +
                ")";
    }

    private int embedded = 0;

    @Deprecated
    public KCode getKcode() {
        return getKCode();
    }

    @Deprecated
    public String getKCodeName() {
        return isKCodeDefault() ? null : getKCode().name().toLowerCase();
    }
}

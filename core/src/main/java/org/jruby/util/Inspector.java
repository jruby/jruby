package org.jruby.util;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;

import static org.jruby.util.io.EncodingUtils.encStrBufCat;

public class Inspector {

    public static final ByteList EMPTY_ARRAY_BL = new ByteList(new byte[] { '[',']' }, USASCIIEncoding.INSTANCE, false);
    public static final ByteList RECURSIVE_ARRAY_BL = new ByteList(new byte[] { '[','.','.','.',']' }, USASCIIEncoding.INSTANCE, false);

    public static final ByteList EMPTY_HASH_BL = new ByteList(new byte[] { '{','}' }, USASCIIEncoding.INSTANCE, false);
    public static final ByteList RECURSIVE_HASH_BL = new ByteList(new byte[] { '{','.','.','.','}' }, USASCIIEncoding.INSTANCE, false);

    private static final byte[] COLON_ZERO_X = new byte[] { ':', '0', 'x' };
    public static final byte[] COLON = new byte[] { ':' };
    public static final byte[] SPACE = new byte[] { ' ' };
    public static final byte[] SLASH = new byte[] { '/' };
    public static final byte[] COLON_SPACE = new byte[] { ':', ' ' };
    public static final byte[] GT = new byte[] { '>' };
    public static final byte[] COMMA = new byte[] { ',' };
    public static final byte[] COMMA_SPACE = new byte[] { ',', ' ' };
    public static final byte[] BEG_BRACKET = new byte[] { '[' };
    public static final byte[] END_BRACKET = new byte[] { ']' };
    public static final byte[] END_BRACKET_GT = new byte[] { ']', '>' };

    // e.g.: #<Object:0x5a1c0542
    public static RubyString inspectPrefix(final ThreadContext context, final RubyModule type, final int hash) {
        final Ruby runtime = context.runtime;
        RubyString buf = inspectPrefixTypeOnly(context, type);
        encStrBufCat(runtime, buf, COLON_ZERO_X); // :0x
        encStrBufCat(runtime, buf, ConvertBytes.longToHexBytes(hash));
        return buf;
    }

    // e.g. #<Java::JavaUtil::Vector:
    public static RubyString inspectPrefix(final ThreadContext context, final RubyModule type) {
        final Ruby runtime = context.runtime;
        RubyString buf = inspectPrefixTypeOnly(context, type);
        encStrBufCat(runtime, buf, COLON); // :
        return buf;
    }

    public static RubyString inspectPrefixTypeOnly(final ThreadContext context, final RubyModule type) {
        RubyString name = RubyStringBuilder.types(context, type);
        // minimal: "#<Object:0x5a1c0542>"
        RubyString buf = RubyString.newStringLight(context.runtime, 2 + name.length() + 3 + 8 + 1, USASCIIEncoding.INSTANCE);
        buf.cat('#').cat('<').cat19(name);
        return buf;
    }

}

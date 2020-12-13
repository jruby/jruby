package org.jruby.util;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.unicode.UnicodeEncoding;

import org.jruby.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.util.StringSupport.MBCLEN_CHARFOUND_LEN;
import static org.jruby.util.StringSupport.MBCLEN_CHARFOUND_P;
import static org.jruby.util.StringSupport.codePoint;

/**
 * Helper methods to make Ruby Strings without the ceremony of manually building the string up.
  */
public class RubyStringBuilder {

    public static RubyString types(ThreadContext context, RubyModule type) {
        return inspectIdentifierByteList(context.runtime, type.toRubyString(context).getByteList());
    }

    public static RubyString types(Ruby runtime, RubyModule type) {
        ThreadContext context = runtime.getCurrentContext();

        return inspectIdentifierByteList(runtime, type.toRubyString(context).getByteList());
    }

    public static RubyString types(Ruby runtime, RubyModule type1, RubyModule type2) {
        ThreadContext context = runtime.getCurrentContext();
        RubyString fullTypeName = type1.toRubyString(context).catString("::").cat(type2.toRubyString(context));

        return inspectIdentifierByteList(runtime, fullTypeName.getByteList());
    }


    public static RubyString ids(Ruby runtime, String id) {
        ByteList identifier = runtime.newSymbol(id).getBytes();

        return inspectIdentifierByteList(runtime, identifier);
    }

    /**
     * Convert object to proper Ruby String representation of an id.  This will not work for any string.
     * It must represent an identifier the systems knows about.
     *
     * @param runtime
     * @param object
     */
    public static RubyString ids(Ruby runtime, IRubyObject object) {
        ByteList identifier;

        if (object instanceof RubyString) {
            identifier = ((RubyString) object).getByteList();
        } else if (object instanceof RubySymbol) {
            identifier = ((RubySymbol) object).getBytes();
        } else {
            identifier = object.convertToString().getByteList();
        }

        return inspectIdentifierByteList(runtime, identifier);
    }

    public static RubyString inspectIdentifierByteList(final Ruby runtime, ByteList byteList) {
        Encoding enc = byteList.getEncoding();
        byte bytes[] = byteList.getUnsafeBytes();
        int p = byteList.getBegin();
        int end = p + byteList.getRealSize();
        RubyString result = RubyString.newStringLight(runtime, new ByteList(end - p + 2));
        result.getByteList().setBegin(1); // just in-case we need to prepend a '"'

        Encoding resultEnc = runtime.getDefaultInternalEncoding();
        boolean isUnicode = enc.isUnicode();
        boolean asciiCompat = enc.isAsciiCompatible();
        boolean needsQuotes = false;

        if (resultEnc == null) resultEnc = runtime.getDefaultExternalEncoding();
        if (!resultEnc.isAsciiCompatible()) resultEnc = USASCIIEncoding.INSTANCE;
        result.associateEncoding(resultEnc);

        int prev = p;
        Encoding actEnc = EncodingUtils.getActualEncoding(enc, byteList);
        if (actEnc != enc) {
            enc = actEnc;
            if (isUnicode) isUnicode = enc instanceof UnicodeEncoding;
        }

        while (p < end) {
            int n = StringSupport.preciseLength(enc, bytes, p, end);
            if (!MBCLEN_CHARFOUND_P(n)) {
                if (p > prev) result.cat(bytes, prev, p - prev);
                n = enc.minLength();
                if (end < p + n) n = end - p;
                while (n-- > 0) {
                    result.modifyExpand(result.size() + 4);
                    Sprintf.sprintf(runtime, result.getByteList() ,"\\x%02X", bytes[p] & 0377);
                    prev = ++p;
                }
                continue;
            }
            n = MBCLEN_CHARFOUND_LEN(n);
            final int c = enc.mbcToCode(bytes, p, end); int cc = 0;

            p += n;
            if ((asciiCompat || isUnicode) &&
                    (c == '"' || c == '\\' ||
                            (c == '#' &&
                                    p < end &&
                                    MBCLEN_CHARFOUND_P(StringSupport.preciseLength(enc, bytes, p, end)) &&
                                    ((cc = codePoint(runtime, enc, bytes, p, end)) == '$' ||
                                            cc == '@' || cc == '{')
                            )
                    )) {
                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                result.cat('\\');
                if (asciiCompat || enc == resultEnc) {
                    prev = p - n;
                    continue;
                }
            }

            switch (c) {
                case '\n': cc = 'n'; break;
                case '\r': cc = 'r'; break;
                case '\t': cc = 't'; break;
                case '\f': cc = 'f'; break;
                case '\013': cc = 'v'; break;
                case '\010': cc = 'b'; break;
                case '\007': cc = 'a'; break;
                case 033: cc = 'e'; break;
                default: cc = 0; break;
            }

            if (cc != 0) {
                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                result.cat('\\');
                result.cat(cc);
                prev = p;
                continue;
            }

            // FIXME: Can't use Encoding.isAscii because it does not treat int as unsigned 32-bit
            if ((enc == resultEnc && enc.isPrint(c)) || (asciiCompat && (c < 128 && c > 0) && enc.isPrint(c))) {
                continue;
            } else {

                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                String format =  StringSupport.escapedCharFormat(c, isUnicode);
                if (!format.equals("%c")) needsQuotes = true;
                Sprintf.sprintf(runtime, result.getByteList() ,format, (c & 0xFFFFFFFFL));
                prev = p;
                continue;
            }
        }

        if (p > prev) result.cat(bytes, prev, p - prev);

        if (needsQuotes) {
            result.prepend('"');
            result.cat('"');
        }
        return result;
    }
    
    public static String str(Ruby runtime, IRubyObject value, String message) {
        RubyString buf = (RubyString) value.asString().dup();

        cat(runtime, buf, message);

        return buf.toString();
    }

    public static String str(Ruby runtime, String message, IRubyObject value) {
        RubyString buf = runtime.newString(message);

        buf.cat19(value.asString());

        return buf.toString();
    }

    public static String str(Ruby runtime, String messageBegin, IRubyObject value, String messageEnd) {
        RubyString buf = runtime.newString(messageBegin);

        buf.cat19(value.asString());
        cat(runtime, buf, messageEnd);

        return buf.toString();
    }

    public static String str(Ruby runtime, IRubyObject value, String message, IRubyObject value2) {
        RubyString buf = (RubyString) value.asString().dup();

        cat(runtime, buf, message);
        buf.cat19(value2.asString());

        return buf.toString();
    }

    public static String str(Ruby runtime, IRubyObject value, String message, IRubyObject value2, String message2) {
        RubyString buf = (RubyString) value.asString().dup();

        cat(runtime, buf, message);
        buf.cat19(value2.asString());
        cat(runtime, buf, message2);

        return buf.toString();
    }

    public static String str(Ruby runtime, String messageBegin, IRubyObject value, String messageMiddle, IRubyObject value2) {
        RubyString buf = runtime.newString(messageBegin);

        buf.cat19(value.asString());
        cat(runtime, buf, messageMiddle);
        buf.cat19(value2.asString());

        return buf.toString();
    }

    public static String str(Ruby runtime, String messageBegin, IRubyObject value, String messageMiddle, IRubyObject value2, String messageEnd) {
        RubyString buf = runtime.newString(messageBegin);

        buf.cat19(value.asString());
        cat(runtime, buf, messageMiddle);
        buf.cat19(value2.asString());
        cat(runtime, buf, messageEnd);

        return buf.toString();
    }


    public static String str(Ruby runtime, String messageBegin, IRubyObject value, String messageMiddle, RubyString value2,
                              String messageMiddle2, IRubyObject value3, String messageMiddle3, RubyString value4, String messageEnd) {
        RubyString buf = runtime.newString(messageBegin);

        buf.cat19(value.asString());
        cat(runtime, buf, messageMiddle);
        buf.cat19(value2);
        cat(runtime, buf, messageMiddle2);
        buf.cat19(value3.asString());
        cat(runtime, buf, messageMiddle3);
        buf.cat19(value4);
        cat(runtime, buf, messageEnd);

        return buf.toString();
    }

    public static RubyString cat(final Ruby runtime, final RubyString str, final String value) {
        EncodingUtils.encStrBufCat(runtime, str, value);
        return str;
    }

    public static RubyString cat(final Ruby runtime, RubyString buf, int b) {
        EncodingUtils.encStrBufCat(runtime, buf, new byte[] { (byte) b });
        return buf;
    }

    public static RubyString cat(final Ruby runtime, RubyString buf, byte[] bytes) {
        EncodingUtils.encStrBufCat(runtime, buf, bytes);
        return buf;
    }

    public static RubyString cat(final Ruby runtime, RubyString buf, ByteList bytes) {
        EncodingUtils.encStrBufCat(runtime, buf, bytes);
        return buf;
    }

}

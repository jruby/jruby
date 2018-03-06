package org.jruby.util;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.util.StringSupport.MBCLEN_CHARFOUND_LEN;
import static org.jruby.util.StringSupport.MBCLEN_CHARFOUND_P;
import static org.jruby.util.StringSupport.codePoint;

/**
 * Helper methods to make Ruby Strings without the ceremony of manually building the string up.
  */
public class RubyStringBuilder {
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
        // FIXME: bytelist_love - should this be convertToString()?
        ByteList identifier;

        // FIXME: bytelist_love - Want something which gets bytes without any dyncalls I think? [needs test]
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
        RubyString result = new RubyString(runtime, runtime.getString(), new ByteList(end - p));
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

        // FIXME: bytelist_love: This is weird and should be done differently
        if (needsQuotes) {
            RubyString newResult = runtime.newString("\"");
            newResult.cat(result);
            newResult.cat('"');
            result = newResult;
        }
        return result;
    }

    public static RubyString dumpedValue(Ruby runtime, IRubyObject value) {
        RubyString string = value.asString();

        return runtime.newString(StringSupport.dumpCommon(runtime, string.getByteList(), true));
    }
    
    public static String buildString(Ruby runtime, IRubyObject value, String message) {
        RubyString buf = (RubyString) value.asString().dup();

        buf.cat19(runtime.newString(message));

        return buf.toString();
    }

    public static String buildString(Ruby runtime, String message, IRubyObject value) {
        RubyString buf = runtime.newString(message);

        buf.cat19(value.asString());

        return buf.toString();
    }

    public static String buildString(Ruby runtime, String messageBegin, IRubyObject value, String messageEnd) {
        RubyString buf = runtime.newString(messageBegin);

        buf.cat19(value.asString());
        buf.cat19(runtime.newString(messageEnd));

        return buf.toString();
    }

    public static String buildString(Ruby runtime, IRubyObject value, String message, IRubyObject value2) {
        RubyString buf = (RubyString) value.asString().dup();

        buf.cat19(runtime.newString(message));
        buf.cat19(value2.asString());

        return buf.toString();
    }

    public static String buildString(Ruby runtime, IRubyObject value, String message, IRubyObject value2, String message2) {
        RubyString buf = (RubyString) value.asString().dup();

        buf.cat19(runtime.newString(message));
        buf.cat19(value2.asString());
        buf.cat19(runtime.newString(message2));

        return buf.toString();
    }

    public static String buildString(Ruby runtime, String messageBegin, IRubyObject value, String messageMiddle, IRubyObject value2) {
        RubyString buf = runtime.newString(messageBegin);

        buf.cat19(value.asString());
        buf.cat19(runtime.newString(messageMiddle));
        buf.cat19(value2.asString());

        return buf.toString();
    }

    public static String buildString(Ruby runtime, String messageBegin, IRubyObject value, String messageMiddle, IRubyObject value2, String messageEnd) {
        RubyString buf = runtime.newString(messageBegin);

        buf.cat19(value.asString());
        buf.cat19(runtime.newString(messageMiddle));
        buf.cat19(value2.asString());
        buf.cat19(runtime.newString(messageEnd));

        return buf.toString();
    }
}

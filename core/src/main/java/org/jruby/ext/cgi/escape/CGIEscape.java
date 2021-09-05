package org.jruby.ext.cgi.escape;

import org.jcodings.Encoding;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

/**
 * Created by headius on 12/8/16.
 */
public class CGIEscape implements Library {
    public static final String ACCEPT_CHARSET = "@@accept_charset";
    public static final byte[] NO39 = "&#39;".getBytes(RubyEncoding.UTF8);
    public static final byte[] AMP = "&amp;".getBytes(RubyEncoding.UTF8);
    public static final byte[] QUOT = "&quot;".getBytes(RubyEncoding.UTF8);
    public static final byte[] LT = "&lt;".getBytes(RubyEncoding.UTF8);
    public static final byte[] GT = "&gt;".getBytes(RubyEncoding.UTF8);
    public static final int UNICODE_MAX = 0x10ffff;
    public static final byte[] TSEMI = "t;".getBytes(RubyEncoding.UTF8);
    public static final byte[] UOTSEMI = "uot;".getBytes(RubyEncoding.UTF8);
    public static final byte[] MPSEMI = "mp;".getBytes(RubyEncoding.UTF8);
    public static final byte[] POSSEMI = "pos;".getBytes(RubyEncoding.UTF8);

    static void html_escaped_cat(RubyString str, byte c) {
        switch (c) {
            case '\'':
                str.cat(NO39);
                break;
            case '&':
                str.cat(AMP);
                break;
            case '"':
                str.cat(QUOT);
                break;
            case '<':
                str.cat(LT);
                break;
            case '>':
                str.cat(GT);
                break;
        }
    }

    static void preserve_original_state(RubyString orig, RubyString dest) {
        dest.setEncoding(orig.getEncoding());

        dest.infectBy(orig);
    }

    static IRubyObject
    optimized_escape_html(Ruby runtime, RubyString str) {
        int i, len, beg = 0;
        RubyString dest = null;
        byte[] cstrBytes;

        len = str.size();
        ByteList byteList = str.getByteList();
        cstrBytes = byteList.unsafeBytes();
        int cstr = byteList.begin();

        for (i = 0; i < len; i++) {
            switch (cstrBytes[cstr + i]) {
                case '\'':
                case '&':
                case '"':
                case '<':
                case '>':
                    if (dest == null) {
                        dest = RubyString.newStringLight(runtime, len);
                    }

                    dest.cat(cstrBytes, cstr + beg, i - beg);
                    beg = i + 1;

                    html_escaped_cat(dest, cstrBytes[cstr + i]);
                    break;
            }
        }

        if (dest != null) {
            dest.cat(cstrBytes, cstr + beg, len - beg);
            preserve_original_state(str, dest);
            return dest;
        } else {
            return str.strDup(runtime);
        }
    }

    // Set of i has to happen outside this; see MATCH macro in MRI ext/cgi/escape/escape.c
    static boolean MATCH(byte[] s, int len, int i, byte[] cstrBytes, int cstr) {
        if (len - i >= s.length && ByteList.memcmp(cstrBytes, cstr + i, s, 0, s.length) == 0) {
            return true;
        } else {
            return false;
        }
    }

    static IRubyObject
    optimized_unescape_html(Ruby runtime, RubyString str) {
        Encoding enc = str.getEncoding();
        int charlimit = (enc instanceof UTF8Encoding) ? UNICODE_MAX :
                (enc instanceof ISO8859_1Encoding) ? 256 :
                        128;
        int i, len, beg = 0;
        int clen = 0, plen;
        boolean overflow = false;
        byte[] cstrBytes;
        int cstr;
        byte[] buf = new byte[6];
        RubyString dest = null;

        len = str.size();
        ByteList byteList = str.getByteList();
        cstrBytes = byteList.getUnsafeBytes();
        cstr = byteList.begin();

        for (i = 0; i < len; i++) {
            int cc;
            int c = cstrBytes[cstr + i];
            if (c != '&') continue;
            plen = i - beg;
            if (++i >= len) break;
            c = cstrBytes[cstr + i] & 0xFF;
            switch (c) {
                case 'a':
                    ++i;
                    if (MATCH(POSSEMI, len, i, cstrBytes, cstr)) {
                        i += POSSEMI.length - 1;
                        c = '\'';
                    } else if (MATCH(MPSEMI, len, i, cstrBytes, cstr)) {
                        i += MPSEMI.length - 1;
                        c = '&';
                    } else continue;
                    break;
                case 'q':
                    ++i;
                    if (MATCH(UOTSEMI, len, i, cstrBytes, cstr)) {
                        i += UOTSEMI.length - 1;
                        c = '"';
                    } else continue;
                    break;
                case 'g':
                    ++i;
                    if (MATCH(TSEMI, len, i, cstrBytes, cstr)) {
                        i += TSEMI.length - 1;
                        c = '>';
                    } else continue;
                    break;
                case 'l':
                    ++i;
                    if (MATCH(TSEMI, len, i, cstrBytes, cstr)) {
                        i += TSEMI.length - 1;
                        c = '<';
                    } else continue;
                    break;
                case '#':
                    if (len - ++i >= 2 && Character.isDigit(cstrBytes[cstr + i])) {
                        int[] clenOverflow = {clen, overflow ? 1 : 0};
                        cc = ruby_scan_digits(cstrBytes, cstr + i, len - i, 10, clenOverflow);
                        clen = clenOverflow[0];
                        overflow = clenOverflow[1] == 1;
                    } else if (i < len && (cstrBytes[cstr + i] == 'x' || cstrBytes[cstr + i] == 'X') && len - ++i >= 2 && ISXDIGIT(cstrBytes, cstr + i)) {
                        int[] clenOverflow = {clen, overflow ? 1 : 0};
                        cc = ruby_scan_digits(cstrBytes, cstr + i, len - i, 16, clenOverflow);
                        clen = clenOverflow[0];
                        overflow = clenOverflow[1] == 1;
                    } else continue;
                    i += clen;
                    if (overflow || cc >= charlimit || i >= len || cstrBytes[cstr + i] != ';') continue;
                    if (dest == null) {
                        dest = RubyString.newStringLight(runtime, len);
                    }
                    dest.cat(cstrBytes, cstr + beg, plen);
                    if (charlimit > 256) {
                        dest.cat(buf, 0, enc.codeToMbc(cc, buf, 0));
                    } else {
                        c = cc;
                        dest.cat(c);
                    }
                    beg = i + 1;
                    continue;
                default:
                    --i;
                    continue;
            }
            if (dest == null) {
                dest = RubyString.newStringLight(runtime, len);
            }
            dest.cat(cstrBytes, cstr + beg, plen);
            dest.cat(c);
            beg = i + 1;
        }

        if (dest != null) {
            dest.cat(cstrBytes, cstr + beg, len - beg);
            preserve_original_state(str, dest);
            return dest;
        } else {
            return str.strDup(runtime);
        }
    }

    static boolean ISXDIGIT(byte[] cstrBytes, int i) {
        byte cstrByte = cstrBytes[i];
        return (cstrByte >= '0' && cstrByte <= '9') || (cstrByte >= 'a' && cstrByte <= 'f') || (cstrByte >= 'A' && cstrByte <= 'F');
    }

    static boolean url_unreserved_char(int c) {
        switch (c) {
            case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
            case '-': case '.': case '_': case '~':
                return true;
            default:
                break;
        }
        return false;
    }

    static final byte[] upper_hexdigits = "0123456789ABCDEF".getBytes(RubyEncoding.UTF8);

    static IRubyObject optimized_escape(Ruby runtime, RubyString str) {
        int i, len, beg = 0;
        RubyString dest = null;
        byte[] cstrBytes;
        int cstr;
        byte[] buf = {'%', 0, 0};

        len = str.size();
        ByteList byteList = str.getByteList();
        cstrBytes = byteList.unsafeBytes();
        cstr = byteList.begin();

        for (i = 0; i < len; ++i) {
            int c = cstrBytes[cstr + i] & 0xFF;
            if (!url_unreserved_char(c)) {
                if (dest == null) {
                    dest = RubyString.newStringLight(runtime, len);
                }

                dest.cat(cstrBytes, cstr + beg, i - beg);
                beg = i + 1;

                if (c == ' ') {
                    dest.cat('+');
                } else {
                    buf[1] = upper_hexdigits[(c >> 4) & 0xf];
                    buf[2] = upper_hexdigits[c & 0xf];
                    dest.cat(buf, 0, 3);
                }
            }
        }

        if (dest != null) {
            dest.cat(cstrBytes, cstr + beg, len - beg);
            preserve_original_state(str, dest);
            return dest;
        } else {
            return str.strDup(runtime);
        }
    }

    static IRubyObject
    optimized_unescape(ThreadContext context, RubyString str, IRubyObject encoding) {
        int i, len, beg = 0;
        RubyString dest = null;
        byte[] cstrBytes;
        int cstr;
        int cr;
        Encoding origenc, encidx = EncodingUtils.rbToEncoding(context, encoding);

        len = str.size();
        ByteList byteList = str.getByteList();
        cstrBytes = byteList.unsafeBytes();
        cstr = byteList.begin();

        int buf = 0;
        Ruby runtime = context.runtime;
        
        for (i = 0; i < len; ++i) {
            int c = cstrBytes[cstr + i] & 0xFF;
            int clen = 0;
            if (c == '%') {
                if (i + 3 > len) break;
                if (!ISXDIGIT(cstrBytes, cstr + i + 1)) continue;
                if (!ISXDIGIT(cstrBytes, cstr + i + 2)) continue;
                buf = ((char_to_number(cstrBytes[cstr + i + 1]) << 4)
                        | char_to_number(cstrBytes[cstr + i + 2]));
                clen = 2;
            } else if (c == '+') {
                buf = ' ';
            } else {
                continue;
            }

            if (dest == null) {
                dest = RubyString.newStringLight(runtime, len);
            }

            dest.cat(cstrBytes, cstr + beg, i - beg);
            i += clen;
            beg = i + 1;

            dest.cat(buf);
        }

        if (dest != null) {
            dest.cat(cstrBytes, cstr + beg, len - beg);
            preserve_original_state(str, dest);
            cr = StringSupport.CR_UNKNOWN;
        } else {
            dest = str.strDup(runtime);
            cr = str.getCodeRange();
        }
        origenc = str.getEncoding();
        if (origenc != encidx) {
            dest.setEncoding(encidx);
            if (StringSupport.encCoderangeClean(dest.getCodeRange()) == 0) {
                dest.setEncoding(origenc);
                if (cr != StringSupport.CR_UNKNOWN)
                    dest.setCodeRange(cr);
            }
        }
        return dest;
    }

    /*
     *  call-seq:
     *     CGI.escapeHTML(string) -> string
     *
     *  Returns HTML-escaped string.
     *
     */
    @JRubyMethod(name = "escapeHTML", module = true, frame = true)
    public static IRubyObject cgiesc_escape_html(ThreadContext context, IRubyObject self, IRubyObject _str) {
        RubyString str = _str.convertToString();

        if (str.getEncoding().isAsciiCompatible()) {
            return optimized_escape_html(context.runtime, str);
        } else {
            return Helpers.invokeSuper(context, self, _str, Block.NULL_BLOCK);
        }
    }

    /*
     *  call-seq:
     *     CGI.unescapeHTML(string) -> string
     *
     *  Returns HTML-unescaped string.
     *
     */
    @JRubyMethod(name = "unescapeHTML", module = true, frame = true)
    public static IRubyObject cgiesc_unescape_html(ThreadContext context, IRubyObject self, IRubyObject _str) {
        RubyString str = _str.convertToString();

        if (str.getEncoding().isAsciiCompatible()) {
            return optimized_unescape_html(context.runtime, str);
        } else {
            return Helpers.invokeSuper(context, self, _str, Block.NULL_BLOCK);
        }
    }

    /*
     *  call-seq:
     *     CGI.escape(string) -> string
     *
     *  Returns URL-escaped string.
     *
     */
    @JRubyMethod(name = "escape", module = true, frame = true)
    public static IRubyObject cgiesc_escape(ThreadContext context, IRubyObject self, IRubyObject _str) {
        RubyString str = _str.convertToString();

        if (str.getEncoding().isAsciiCompatible()) {
            return optimized_escape(context.runtime, str);
        } else {
            return Helpers.invokeSuper(context, self, _str, Block.NULL_BLOCK);
        }
    }

    static IRubyObject accept_charset(IRubyObject[] args, int argc, int argv, IRubyObject self) {
        if (argc > 0)
            return args[argv];
        return self.getMetaClass().getClassVar(ACCEPT_CHARSET);
    }

    /*
     *  call-seq:
     *     CGI.unescape(string, encoding=@@accept_charset) -> string
     *
     *  Returns URL-unescaped string.
     *
     */
    @JRubyMethod(name = "unescape", required = 1, optional = 1, module = true, frame = true)
    public static IRubyObject cgiesc_unescape(ThreadContext context, IRubyObject self, IRubyObject[] argv) {
        IRubyObject _str = argv[0];

        RubyString str = _str.convertToString();

        if (str.getEncoding().isAsciiCompatible()) {
            IRubyObject enc = accept_charset(argv, argv.length - 1, 1, self);
            return optimized_unescape(context, str, enc);
        } else {
            return Helpers.invokeSuper(context, self, argv, Block.NULL_BLOCK);
        }
    }

    public void load(Ruby runtime, boolean wrap) {
        RubyClass rb_cCGI = runtime.defineClass("CGI", runtime.getObject(), runtime.getObject().getAllocator());
        RubyModule rb_mEscape = rb_cCGI.defineModuleUnder("Escape");
        RubyModule rb_mUtil = rb_cCGI.defineModuleUnder("Util");
        rb_mEscape.defineAnnotatedMethods(CGIEscape.class);
        rb_mUtil.prependModule(rb_mEscape);
        rb_mEscape.extend_object(rb_cCGI);
    }

    // PORTED FROM OTHER FILES IN MRI

    static int ruby_scan_digits(byte[] strBytes, int str, int len, int base, int[] retlenOverflow) {
        int start = str;
        int ret = 0, x;
        int mul_overflow = Integer.MAX_VALUE / base;

        retlenOverflow[1] = 0;

        if (len == 0) {
            retlenOverflow[0] = 0;
            return 0;
        }

        do {
            int d = ruby_digit36_to_number_table[strBytes[str++]];
            if (d == -1 || base <= d) {
                --str;
                break;
            }
            if (mul_overflow < ret)
                retlenOverflow[1] = 1;
            ret *= base;
            x = ret;
            ret += d;
            if (ret < x)
                retlenOverflow[1] = 1;
        } while (len < 0 || (--len != 0));
        retlenOverflow[0] = str - start;
        return ret;
    }

    static int char_to_number(int c) {
        return ruby_digit36_to_number_table[c];
    }

    private static final int ruby_digit36_to_number_table[] = {
            /*     0   1   2   3   4   5   6   7   8   9   a   b   c   d   e   f */
            /*0*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*1*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*2*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*3*/  0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1,
            /*4*/ -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            /*5*/ 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,
            /*6*/ -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            /*7*/ 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,
            /*8*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*9*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*a*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*b*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*c*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*d*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*e*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*f*/ -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };
}

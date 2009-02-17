/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007-2009 Koichiro Ohba <koichiro@meadowy.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.Pack;

@JRubyModule(name="NKF")
public class RubyNKF {
    public static final NKFCharset AUTO = new NKFCharset(0, "x-JISAutoDetect");
    public static final NKFCharset JIS = new NKFCharset(1, "iso-2022-jp");
    public static final NKFCharset EUC = new NKFCharset(2, "EUC-JP");
    public static final NKFCharset SJIS = new NKFCharset(3, "Windows-31J");
    public static final NKFCharset BINARY = new NKFCharset(4, null);
    public static final NKFCharset NOCONV = new NKFCharset(4, null);
    public static final NKFCharset UNKNOWN = new NKFCharset(0, null);
    public static final NKFCharset ASCII = new NKFCharset(5, "iso-8859-1");
    public static final NKFCharset UTF8 = new NKFCharset(6, "UTF-8");
    public static final NKFCharset UTF16 = new NKFCharset(8, "UTF-16");
    public static final NKFCharset UTF32 = new NKFCharset(12, "UTF-32");
    public static final NKFCharset OTHER = new NKFCharset(16, null);
    public static final NKFCharset BASE64 = new NKFCharset(20, "base64");
    public static final NKFCharset QENCODE = new NKFCharset(21, "qencode");
    public static final NKFCharset MIME_DETECT = new NKFCharset(22, "MimeAutoDetect");

    private static final ByteList BEGIN_MIME_STRING = new ByteList(ByteList.plain("=?"));
    private static final ByteList END_MIME_STRING = new ByteList(ByteList.plain("?="));
    private static final ByteList MIME_ASCII = new ByteList(ByteList.plain(ASCII.getCharset()));
    private static final ByteList MIME_UTF8 = new ByteList(ByteList.plain(UTF8.getCharset()));
    private static final ByteList MIME_JIS = new ByteList(ByteList.plain(JIS.getCharset()));
    private static final ByteList MIME_EUC_JP = new ByteList(ByteList.plain(EUC.getCharset()));

    public static class NKFCharset {
        private final int value;
        private final String charset;

        public NKFCharset(int v, String c) {
            value = v;
            charset = c;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getCharset() {
            return charset;
        }
    }

    public static void createNKF(Ruby runtime) {
        RubyModule nkfModule = runtime.defineModule("NKF");

        nkfModule.defineConstant("AUTO", RubyFixnum.newFixnum(runtime, AUTO.getValue()));
        nkfModule.defineConstant("JIS", RubyFixnum.newFixnum(runtime, JIS.getValue()));
        nkfModule.defineConstant("EUC", RubyFixnum.newFixnum(runtime, EUC.getValue()));
        nkfModule.defineConstant("SJIS", RubyFixnum.newFixnum(runtime, SJIS.getValue()));
        nkfModule.defineConstant("BINARY", RubyFixnum.newFixnum(runtime, BINARY.getValue()));
        nkfModule.defineConstant("NOCONV", RubyFixnum.newFixnum(runtime, NOCONV.getValue()));
        nkfModule.defineConstant("UNKNOWN", RubyFixnum.newFixnum(runtime, UNKNOWN.getValue()));
        nkfModule.defineConstant("ASCII", RubyFixnum.newFixnum(runtime, ASCII.getValue()));
        nkfModule.defineConstant("UTF8", RubyFixnum.newFixnum(runtime, UTF8.getValue()));
        nkfModule.defineConstant("UTF16", RubyFixnum.newFixnum(runtime, UTF16.getValue()));
        nkfModule.defineConstant("UTF32", RubyFixnum.newFixnum(runtime, UTF32.getValue()));
        nkfModule.defineConstant("OTHER", RubyFixnum.newFixnum(runtime, OTHER.getValue()));
        
        RubyString version = runtime.newString("2.0.7 (JRuby 2007-05-11)");
        RubyString nkfVersion = runtime.newString("2.0.7");
        RubyString nkfDate = runtime.newString("2007-05-11");

        ThreadContext context = runtime.getCurrentContext();
        
        version.freeze(context);
        nkfVersion.freeze(context);
        nkfDate.freeze(context);

        nkfModule.defineAnnotatedMethods(RubyNKF.class);
    }

    @JRubyMethod(name = "guess", required = 1, module = true)
    public static IRubyObject guess(ThreadContext context, IRubyObject recv, IRubyObject s) {
        Ruby runtime = context.getRuntime();
        if (!s.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + s.getMetaClass() + " into String");
        }
        ByteList bytes = s.convertToString().getByteList();
        ByteBuffer buf = ByteBuffer.wrap(bytes.unsafeBytes(), bytes.begin(), bytes.length());
        CharsetDecoder decoder = Charset.forName("x-JISAutoDetect").newDecoder();
        try {
            decoder.decode(buf);
        } catch (CharacterCodingException e) {
            return runtime.newFixnum(UNKNOWN.getValue());
        }
        if (!decoder.isCharsetDetected()) {
            return runtime.newFixnum(UNKNOWN.getValue());
        }
        Charset charset = decoder.detectedCharset();
        String name = charset.name();
//        System.out.println("detect: " + name + "\n");
        if ("Shift_JIS".equals(name))
            return runtime.newFixnum(SJIS.getValue());
        if ("windows-31j".equals(name))
            return runtime.newFixnum(SJIS.getValue());
        else if ("EUC-JP".equals(name))
            return runtime.newFixnum(EUC.getValue());
        else if ("ISO-2022-JP".equals(name))
            return runtime.newFixnum(JIS.getValue());
        else
            return runtime.newFixnum(UNKNOWN.getValue());
    }
    
    @JRubyMethod(name = "guess1", required = 1, module = true)
    public static IRubyObject guess1(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return guess(context, recv, str);
    }
    
    @JRubyMethod(name = "guess2", required = 1, module = true)
    public static IRubyObject guess2(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return guess(context, recv, str);
    }
    
    @JRubyMethod(name = "nkf", required = 2, module = true)
    public static IRubyObject nkf(ThreadContext context, IRubyObject recv, IRubyObject opt, IRubyObject str) {
        Ruby runtime = context.getRuntime();
        
        if (!opt.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + opt.getMetaClass() + " into String");
        }
        
        if (!str.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + str.getMetaClass() + " into String");
        }
        
        Map<String, NKFCharset> options = parseOpt(opt.convertToString().toString());
        
        if (options.get("input").getValue() == AUTO.getValue()) {
            KCode kcode = runtime.getKCode();
            if (kcode == KCode.SJIS) {
                options.put("input", SJIS);
            } else if (kcode == KCode.EUC) {
                options.put("input", EUC);
            } else if (kcode == KCode.UTF8) {
                options.put("input", UTF8);
            }
        }

        ByteList mimeString = str.convertToString().getByteList();
        ByteList mimeText = null;
        NKFCharset mimeState = detectMimeString(mimeString, options);
        if (mimeState == NOCONV) {
            mimeText = mimeString;
        } else {
            mimeText = getMimeText(mimeString);
            RubyArray array = null;
            if (mimeState == BASE64) {
                array = Pack.unpack(runtime, mimeText, new ByteList(ByteList.plain("m")));
            } else if (mimeState == QENCODE) {
                array = Pack.unpack(runtime, mimeText, new ByteList(ByteList.plain("M")));
            }
            RubyString s = (RubyString) array.entry(0);
            mimeText = s.asString().getByteList();
        }

        String decodeCharset = options.get("input").getCharset();
        String encodeCharset = options.get("output").getCharset();

        RubyString result = convert(context, decodeCharset, encodeCharset, mimeText);

        if (options.get("mime-encode") == BASE64) {
            result = encodeMimeString(runtime, result, "m"); // BASE64
        } else if (options.get("mime-encode") == QENCODE) {
            result = encodeMimeString(runtime, result, "M"); // quoted-printable
        }

        return result;
    }
    
    private static RubyString convert(ThreadContext context, String decodeCharset,
            String encodeCharset, ByteList str) {
        Ruby runtime = context.getRuntime();
        CharsetDecoder decoder;
        CharsetEncoder encoder;
        try {
            decoder = Charset.forName(decodeCharset).newDecoder();
            encoder = Charset.forName(encodeCharset).newEncoder();
        } catch (UnsupportedCharsetException e) {
            throw runtime.newArgumentError("invalid encoding");
        }
        
        ByteBuffer buf = ByteBuffer.wrap(str.unsafeBytes(), str.begin(), str.length());
        try {
            CharBuffer cbuf = decoder.decode(buf);
            buf = encoder.encode(cbuf);
        } catch (CharacterCodingException e) {
            throw runtime.newArgumentError("invalid encoding");
        }
        byte[] arr = buf.array();
        
        return runtime.newString(new ByteList(arr, 0, buf.limit()));
        
    }

    private static RubyString encodeMimeString(Ruby runtime, RubyString str, String format) {
        RubyArray array = RubyArray.newArray(runtime, 1);
        array.append(str);
        return Pack.pack(runtime, array, new ByteList(ByteList.plain(format))).chomp(runtime.getCurrentContext());
    }

    private static NKFCharset detectMimeString(ByteList str, Map<String, NKFCharset> options) {
        if (str.length() <= 6) return NOCONV;
        if (options.get("mime-decode") == NOCONV) return NOCONV;
        if (!str.startsWith(BEGIN_MIME_STRING)) return NOCONV;
        if (!str.endsWith(END_MIME_STRING)) return NOCONV;

        int pos = str.indexOf('?', 3);
        if (pos < 0) return NOCONV;
        ByteList charset = new ByteList(str, 2, pos - 2);
        if (charset.caseInsensitiveCmp(MIME_UTF8) == 0) {
            options.put("input", UTF8);
        } else if (charset.caseInsensitiveCmp(MIME_JIS) == 0) {
            options.put("input", JIS);
        } else if (charset.caseInsensitiveCmp(MIME_EUC_JP) == 0) {
            options.put("input", EUC);
        } else {
            options.put("input", ASCII);
        }

        int prev = pos;
        pos = str.indexOf('?', pos + 1);
        if (pos < 0) return NOCONV;
        char encode = str.charAt(pos - 1);

        switch (encode) {
        case 'q':
        case 'Q':
            return QENCODE;
        case 'b':
        case 'B':
            return BASE64;
        default:
            return NOCONV;
        }
    }

    private static ByteList getMimeText(ByteList str) {
        int pos = 0;
        for (int i = 3; i >= 1; i--) {
            pos = str.indexOf('?', pos + 1);
        }
        return new ByteList(str, pos + 1, str.length() - pos - 3);
    }

    private static int optionUTF(String s, int pos) {
        int n = 8;
        int first = pos + 1;
        int second = pos + 2;
        if (first < s.length() && Character.isDigit(s.charAt(first))) {
            n = Character.digit(s.charAt(first), 10);
            if (second < s.length() && Character.isDigit(s.charAt(second))) {
                n *= 10;
                n += Character.digit(s.charAt(second), 10);
            }
        }
        return n;
    }

    private static Map<String, NKFCharset> parseOpt(String s) {
        Map<String, NKFCharset> options = new HashMap<String, NKFCharset>();

        // default options
        options.put("input", AUTO);
        options.put("output", JIS);
        options.put("mime-decode", MIME_DETECT);
        options.put("mime-encode", NOCONV);
        
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
            case 'b':
                break;
            case 'u':
                break;
            case 'j': // iso-2022-jp
                options.put("output", JIS);
                break;
            case 's': // Shift_JIS
                options.put("output", SJIS);
                break;
            case 'e': // EUC-JP
                options.put("output", EUC);
                break;
            case 'w': // UTF-8
            {
                int n = optionUTF(s, i);
                if (n == 32)
                    options.put("output", UTF32);
                else if (n == 16)
                    options.put("output", UTF16);
                else
                    options.put("output", UTF8);
            }
                break;
            case 'J': // iso-2022-jp
                options.put("input", JIS);
                break;
            case 'S': // Shift_JIS
                options.put("input", SJIS);
                break;
            case 'E': // EUC-JP
                options.put("input", EUC);
                break;
            case 'W': // UTF-8
            {
                int n = optionUTF(s, i);
                if (n == 32)
                    options.put("input", UTF32);
                else if (n == 16)
                    options.put("input", UTF16);
                else
                    options.put("input", UTF8);
            }
                break;
            case 't':
                break;
            case 'r':
                break;
            case 'h':
                break;
            case 'm':
                if (i+1 >= s.length()) {
                    options.put("mime-decode", MIME_DETECT);
                    break;
                }
                switch (s.charAt(i+1)) {
                case 'B':
                    options.put("mime-decode", BASE64);
                    break;
                case 'Q':
                    options.put("mime-decode", QENCODE);
                    break;
                case 'N':
                    // TODO: non-strict option
                    break;
                case '0':
                    options.put("mime-decode", NOCONV);
                    break;
                }
                break;
            case 'M':
                if (i+1 >= s.length()) {
                    options.put("mime-encode", NOCONV);
                }
                switch (s.charAt(i+1)) {
                case 'B':
                    options.put("mime-encode", BASE64);
                    break;
                case 'Q':
                    options.put("mime-encode", QENCODE);
                    break;
                }
                break;
            case 'l':
                break;
            case 'f':
                break;
            case 'F':
                break;
            case 'Z':
                break;
            case 'X':
                break;
            case 'x':
                break;
            case 'B':
                break;
            case 'T':
                break;
            case 'd':
                break;
            case 'c':
                break;
            case 'I':
                break;
            case 'L':
                break;
            case '-':
                if (s.charAt(i+1) == '-') {
                    // long name option
                }
            default:
            }
        }
        return options;
    }
}

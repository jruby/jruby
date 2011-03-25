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
 * Copyright (C) 2007-2010 Koichiro Ohba <koichiro@meadowy.org>
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.CP1251Encoding;
import org.jcodings.specific.EUCJPEncoding;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jcodings.specific.SJISEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF8Encoding;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.Pack;

@JRubyModule(name="NKF")
public class RubyNKF {
    public static final NKFCharset AUTO = new NKFCharset(0, "x-JISAutoDetect", SJISEncoding.INSTANCE);
    // no ISO-2022-JP in jcodings
    public static final NKFCharset JIS = new NKFCharset(1, "iso-2022-jp", SJISEncoding.INSTANCE);
    public static final NKFCharset EUC = new NKFCharset(2, "EUC-JP", EUCJPEncoding.INSTANCE);
    public static final NKFCharset SJIS = new NKFCharset(3, "Windows-31J", CP1251Encoding.INSTANCE);
    public static final NKFCharset BINARY = new NKFCharset(4, null, null);
    public static final NKFCharset NOCONV = new NKFCharset(4, null, null);
    public static final NKFCharset UNKNOWN = new NKFCharset(0, null, null);
    public static final NKFCharset ASCII = new NKFCharset(5, "iso-8859-1", ISO8859_1Encoding.INSTANCE);
    public static final NKFCharset UTF8 = new NKFCharset(6, "UTF-8", UTF8Encoding.INSTANCE);
    public static final NKFCharset UTF16 = new NKFCharset(8, "UTF-16", UTF16BEEncoding.INSTANCE);
    public static final NKFCharset UTF32 = new NKFCharset(12, "UTF-32", UTF32BEEncoding.INSTANCE);
    public static final NKFCharset OTHER = new NKFCharset(16, null, null);
    public static final NKFCharset BASE64 = new NKFCharset(20, "base64", null);
    public static final NKFCharset QENCODE = new NKFCharset(21, "qencode", null);
    public static final NKFCharset MIME_DETECT = new NKFCharset(22, "MimeAutoDetect", null);

    private static final ByteList BEGIN_MIME_STRING = new ByteList(ByteList.plain("=?"));
    private static final ByteList END_MIME_STRING = new ByteList(ByteList.plain("?="));
    private static final ByteList PACK_BASE64 = new ByteList(ByteList.plain("m"));
    private static final ByteList PACK_QENCODE = new ByteList(ByteList.plain("M"));

    public static class NKFCharset {
        private final int value;
        private final String charset;
        private final Encoding encoding;

        public NKFCharset(int v, String c, Encoding encoding) {
            value = v;
            charset = c;
            this.encoding = encoding;
        }

        public int getValue() {
            return value;
        }

        public String getCharset() {
            return charset;
        }
        
        public Encoding getEncoding() {
            return encoding;
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
        // TODO: Fix charset usage for JRUBY-4553
        Ruby runtime = context.getRuntime();
        if (!s.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + s.getMetaClass() + " into String");
        }
        ByteList bytes = s.convertToString().getByteList();
        ByteBuffer buf = ByteBuffer.wrap(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        CharsetDecoder decoder;
        try {
            decoder = Charset.forName("x-JISAutoDetect").newDecoder();
        } catch (UnsupportedCharsetException e) {
            throw runtime.newStandardError("charsets.jar is required to use NKF#guess. Please install JRE which supports m17n.");
        }
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
        if ("Shift_JIS".equals(name)) {
            return runtime.newFixnum(SJIS.getValue());
        }
        if ("windows-31j".equals(name)) {
            return runtime.newFixnum(SJIS.getValue());
        } else if ("EUC-JP".equals(name)) {
            return runtime.newFixnum(EUC.getValue());
        } else if ("ISO-2022-JP".equals(name)) {
            return runtime.newFixnum(JIS.getValue());
        } else {
            return runtime.newFixnum(UNKNOWN.getValue());
        }
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

        ByteList bstr = str.convertToString().getByteList();
        Converter converter = null;
        if (Converter.isMimeText(bstr, options)) {
            converter = new MimeConverter(context, options);
        } else {
            converter = new DefaultConverter(context, options);
        }

        RubyString result = converter.convert(bstr);

        if (options.get("mime-encode") == BASE64) {
            result = Converter.encodeMimeString(runtime, result, PACK_BASE64);
        } else if (options.get("mime-encode") == QENCODE) {
            result = Converter.encodeMimeString(runtime, result, PACK_QENCODE);
        }

        return result;
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
                if (n == 32) {
                    options.put("output", UTF32);
                } else if (n == 16) {
                    options.put("output", UTF16);
                } else {
                    options.put("output", UTF8);
                }
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

    static abstract class Converter {

        protected ThreadContext context;
        protected Map<String, NKFCharset> options;

        public Converter(ThreadContext ctx, Map<String, NKFCharset> opt) {
            context = ctx;
            options = opt;
        }

        static boolean isMimeText(ByteList str, Map<String, NKFCharset> options) {
            if (str.length() <= 6) {
                return false;
            }
            if (options.get("mime-decode") == NOCONV) {
                return false;
            }
            if (str.indexOf(BEGIN_MIME_STRING) < 0) {
                return false;
            }
            if (str.lastIndexOf(END_MIME_STRING) < 0) {
                return false;
            }
            return true;
        }

        private static RubyString encodeMimeString(Ruby runtime, RubyString str, ByteList format) {
            RubyArray array = RubyArray.newArray(runtime, str);
            return Pack.pack(runtime, array, format).chomp(runtime.getCurrentContext());
        }

        abstract RubyString convert(ByteList str);

        ByteList convert_byte(ByteList str, String inputCharset, NKFCharset output) {
            String outputCharset = output.getCharset();
            CharsetDecoder decoder;
            CharsetEncoder encoder;

            try {
                decoder = Charset.forName(inputCharset).newDecoder();
                encoder = Charset.forName(outputCharset).newEncoder();
            } catch (UnsupportedCharsetException e) {
                throw context.getRuntime().newArgumentError("invalid charset");
            }

            ByteBuffer buf = ByteBuffer.wrap(str.getUnsafeBytes(), str.begin(), str.length());

            try {
                CharBuffer cbuf = decoder.decode(buf);
                encoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.IGNORE);
                buf = encoder.encode(cbuf);
            } catch (CharacterCodingException e) {
                throw context.getRuntime().newArgumentError("invalid encoding");
            }
            byte[] arr = buf.array();
            ByteList r = new ByteList(arr, 0, buf.limit());
            r.setEncoding(output.getEncoding());

            return r;
        }
    }

    static class DefaultConverter extends Converter {

        public DefaultConverter(ThreadContext ctx, Map<String, NKFCharset> opt) {
            super(ctx, opt);
        }

        RubyString convert(ByteList str) {
            NKFCharset input = options.get("input");
            NKFCharset output = options.get("output");
            ByteList b = convert_byte(str,
                    input.getCharset(),
                    output);
            return context.getRuntime().newString(b);
        }
    }

    static class MimeConverter extends Converter {

        public MimeConverter(ThreadContext ctx, Map<String, NKFCharset> opt) {
            super(ctx, opt);
        }

        private String detectCharset(String charset) {
            if (charset.compareToIgnoreCase(UTF8.getCharset()) == 0) {
                return UTF8.getCharset();
            } else if (charset.compareToIgnoreCase(JIS.getCharset()) == 0) {
                return JIS.getCharset();
            } else if (charset.compareToIgnoreCase(EUC.getCharset()) == 0) {
                return EUC.getCharset();
            } else {
                return ASCII.getCharset();
            }
        }

        private ByteList decodeMimeString(String str) {
            String[] mime = str.split("^=\\?|\\?|\\?=$");
            String charset = detectCharset(mime[1]);
            int encode = mime[2].charAt(0);
            ByteList body = new ByteList(mime[3].getBytes(), ASCIIEncoding.INSTANCE);

            RubyArray array = null;
            if ('B' == encode || 'b' == encode) { // BASE64
                array = Pack.unpack(context.getRuntime(), body, PACK_BASE64);
            } else { // Qencode
                array = Pack.unpack(context.getRuntime(), body, PACK_QENCODE);
            }
            RubyString s = (RubyString) array.entry(0);
            ByteList decodeStr = s.asString().getByteList();

            return convert_byte(decodeStr, charset, options.get("output"));
        }

        RubyString makeRubyString(ArrayList<ByteList> list) {
            ByteList r = new ByteList();
            for (ByteList l : list) {
                r.append(l);
            }
            return context.getRuntime().newString(r);
        }

        RubyString convert(ByteList str) {
            String s = str.toString();
            String[] token = s.split("\\s");
            ArrayList<ByteList> raw_data = new ArrayList<ByteList>();

            for (int i = 0; i < token.length; i++) {
                raw_data.add(decodeMimeString(token[i]));
            }

            return makeRubyString(raw_data);
        }

    }
}

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
 * Copyright (C) 2007 Koichiro Ohba <koichiro@meadowy.org>
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;

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
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyNKF.class);

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

        version.freeze();
        nkfVersion.freeze();
        nkfDate.freeze();

        nkfModule.defineModuleFunction("nkf", callbackFactory.getFastSingletonMethod("nkf", RubyKernel.IRUBY_OBJECT, RubyKernel.IRUBY_OBJECT));
        nkfModule.defineModuleFunction("guess", callbackFactory.getFastSingletonMethod("guess", RubyKernel.IRUBY_OBJECT));
        nkfModule.defineModuleFunction("guess1", callbackFactory.getFastSingletonMethod("guess1", RubyKernel.IRUBY_OBJECT));
        nkfModule.defineModuleFunction("guess2", callbackFactory.getFastSingletonMethod("guess2", RubyKernel.IRUBY_OBJECT));
    }

    public static IRubyObject guess(IRubyObject recv, IRubyObject s) {
        Ruby runtime = recv.getRuntime();
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
    
    public static IRubyObject guess1(IRubyObject recv, IRubyObject str) {
        return guess(recv, str);
    }
    
    public static IRubyObject guess2(IRubyObject recv, IRubyObject str) {
        return guess(recv, str);
    }
    
    public static IRubyObject nkf(IRubyObject recv, IRubyObject opt, IRubyObject str) {
        Ruby runtime = recv.getRuntime();
        if (!opt.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + opt.getMetaClass() + " into String");
        }
        if (!str.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + str.getMetaClass() + " into String");
        }
        
        Map options = parseOpt(opt.convertToString().toString());
        
        NKFCharset nc = (NKFCharset)options.get("input");
        if (nc.getValue() == AUTO.getValue()) {
            KCode kcode = runtime.getKCode();
            if (kcode == KCode.SJIS) {
                nc = SJIS;
            } else if (kcode == KCode.EUC) {
                nc = EUC;
            } else if (kcode == KCode.UTF8) {
                nc = UTF8;
            }
        }
        String decodeCharset = nc.getCharset();
        String encodeCharset = ((NKFCharset)options.get("output")).getCharset();
        
        return convert(decodeCharset, encodeCharset, str);
    }
    
    private static IRubyObject convert(String decodeCharset, String encodeCharset, IRubyObject str) {
        Ruby runtime = str.getRuntime();
        CharsetDecoder decoder;
        CharsetEncoder encoder;
        try {
            decoder = Charset.forName(decodeCharset).newDecoder();
            encoder = Charset.forName(encodeCharset).newEncoder();
        } catch (UnsupportedCharsetException e) {
            throw runtime.newArgumentError("invalid encoding");
        }
        
        ByteList bytes = str.convertToString().getByteList();
        ByteBuffer buf = ByteBuffer.wrap(bytes.unsafeBytes(), bytes.begin(), bytes.length());
        try {
            CharBuffer cbuf = decoder.decode(buf);
            buf = encoder.encode(cbuf);
        } catch (CharacterCodingException e) {
            throw runtime.newArgumentError("invalid encoding");
        }
        byte[] arr = buf.array();
        
        return runtime.newString(new ByteList(arr, 0, buf.limit()));
        
    }

    private static int optionUTF(String s, int i) {
        int n = 8;
        if (i+1 < s.length() && Character.isDigit(s.charAt(i+1))) {
            n = Character.digit(s.charAt(i+1), 10);
            if (i+2 < s.length() && Character.isDigit(s.charAt(i+2))) {
                n *= 10;
                n += Character.digit(s.charAt(i+2), 10);
            }
        }
        return n;
    }

    private static Map parseOpt(String s) {
        Map options = new HashMap();

        // default options
        options.put("input", AUTO);
        options.put("output", JIS);
        
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
                break;
            case 'M':
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

/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.truffle.core.string;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.truffle.platform.Platform;

import java.util.ArrayList;
import java.util.List;

public class EncodingUtils {
    public static final int ECONV_DEFAULT_NEWLINE_DECORATOR = Platform.IS_WINDOWS ? EConvFlags.UNIVERSAL_NEWLINE_DECORATOR : 0;

    private static final byte[] NULL_BYTE_ARRAY = ByteList.NULL_ARRAY;

    static final int VMODE = 0;
    static final int PERM = 1;

    public static int SET_UNIVERSAL_NEWLINE_DECORATOR_IF_ENC2(Encoding enc2, int ecflags) {
        if (enc2 != null && (ecflags & ECONV_DEFAULT_NEWLINE_DECORATOR) != 0) {
            return ecflags | EConvFlags.UNIVERSAL_NEWLINE_DECORATOR;
        }
        return ecflags;
    }

    // rb_enc_asciicompat
    public static boolean encAsciicompat(Encoding enc) {
        return encMbminlen(enc) == 1 && !encDummy(enc);
    }

    // rb_enc_mbminlen
    public static int encMbminlen(Encoding encoding) {
        return encoding.minLength();
    }

    // rb_enc_dummy_p
    public static boolean encDummy(Encoding enc) {
        return enc.isDummy();
    }

    public static boolean DECORATOR_P(byte[] sname, byte[] dname) {
        return sname == null || sname.length == 0 || sname[0] == 0;
    }

    public static List<String> encodingNames(byte[] name, int p, int end) {
        final List<String> names = new ArrayList<>();

        Encoding enc = ASCIIEncoding.INSTANCE;
        int s = p;

        int code = name[s] & 0xff;
        if (enc.isDigit(code)) return names;

        boolean hasUpper = false;
        boolean hasLower = false;
        if (enc.isUpper(code)) {
            hasUpper = true;
            while (++s < end && (enc.isAlnum(name[s] & 0xff) || name[s] == (byte)'_')) {
                if (enc.isLower(name[s] & 0xff)) hasLower = true;
            }
        }

        boolean isValid = false;
        if (s >= end) {
            isValid = true;
            names.add(new String(name, p, end));
        }

        if (!isValid || hasLower) {
            if (!hasLower || !hasUpper) {
                do {
                    code = name[s] & 0xff;
                    if (enc.isLower(code)) hasLower = true;
                    if (enc.isUpper(code)) hasUpper = true;
                } while (++s < end && (!hasLower || !hasUpper));
            }

            byte[]constName = new byte[end - p];
            System.arraycopy(name, p, constName, 0, end - p);
            s = 0;
            code = constName[s] & 0xff;

            if (!isValid) {
                if (enc.isLower(code)) constName[s] = AsciiTables.ToUpperCaseTable[code];
                for (; s < constName.length; ++s) {
                    if (!enc.isAlnum(constName[s] & 0xff)) constName[s] = (byte)'_';
                }
                if (hasUpper) {
                    names.add(new String(constName, 0, constName.length));
                }
            }
            if (hasLower) {
                for (s = 0; s < constName.length; ++s) {
                    code = constName[s] & 0xff;
                    if (enc.isLower(code)) constName[s] = AsciiTables.ToUpperCaseTable[code];
                }
                names.add(new String(constName, 0, constName.length));
            }
        }

        return names;
    }

    public interface ResizeFunction {
        /**
         * Resize the destination, returning the new begin offset.
         *
         * @param destination
         * @param len
         * @param new_len
         */
        int resize(ByteList destination, int len, int new_len);
    }

    public static final ResizeFunction strTranscodingResize = new ResizeFunction() {
        @Override
        public int resize(ByteList destination, int len, int new_len) {
            destination.setRealSize(len);
            destination.ensure(new_len);
            return destination.getBegin();
        }
    };

    /**
     * Fallback function to provide replacements for characters that fail to transcode.
     *
     * @param <State> Runtime state necessary for the function to work
     * @param <Data> Data needed for the function to execute
     */
    public interface TranscodeFallback<State, Data> {
        /**
         * Return a replacement character for the given byte range and encoding.
         *
         * @param context  runtime state for the function
         * @param fallback data for the function
         * @param ec the transcoder that stumbled over the character
         * @return true if the character was successfully replaced; false otherwise
         */
        boolean call(State context, Data fallback, EConv ec);
    }

    public static void strBufCat(ByteList str, byte[] ptrBytes, int ptr, int len) {
        int total, off = -1;

        // termlen is not relevant since we have no termination sequence

        // missing: if ptr string is inside str, off = ptr start minus str start

//        str.modify();
        if (len == 0) return;

        // much logic is missing here, since we don't manually manage the ByteList buffer

        total = str.getRealSize() + len;
        str.ensure(total);
        str.append(ptrBytes, ptr, len);
    }

    // MRI: get_encoding
    public static Encoding getEncoding(ByteList str) {
        return getActualEncoding(str.getEncoding(), str);
    }

    private static final Encoding UTF16Dummy = EncodingDB.getEncodings().get("UTF-16".getBytes()).getEncoding();
    private static final Encoding UTF32Dummy = EncodingDB.getEncodings().get("UTF-32".getBytes()).getEncoding();

    // MRI: get_actual_encoding
    public static Encoding getActualEncoding(Encoding enc, ByteList byteList) {
        return getActualEncoding(enc, byteList.getUnsafeBytes(), byteList.begin(), byteList.begin() + byteList.realSize());
    }

    public static Encoding getActualEncoding(Encoding enc, byte[] bytes, int p, int end) {
        if (enc.isDummy() && enc instanceof UnicodeEncoding) {
            // handle dummy UTF-16 and UTF-32 by scanning for BOM, as in MRI
            if (enc == UTF16Dummy && end - p >= 2) {
                int c0 = bytes[p] & 0xff;
                int c1 = bytes[p + 1] & 0xff;

                if (c0 == 0xFE && c1 == 0xFF) {
                    return UTF16BEEncoding.INSTANCE;
                } else if (c0 == 0xFF && c1 == 0xFE) {
                    return UTF16LEEncoding.INSTANCE;
                }
                return ASCIIEncoding.INSTANCE;
            } else if (enc == UTF32Dummy && end - p >= 4) {
                int c0 = bytes[p] & 0xff;
                int c1 = bytes[p + 1] & 0xff;
                int c2 = bytes[p + 2] & 0xff;
                int c3 = bytes[p + 3] & 0xff;

                if (c0 == 0 && c1 == 0 && c2 == 0xFE && c3 == 0xFF) {
                    return UTF32BEEncoding.INSTANCE;
                } else if (c3 == 0 && c2 == 0 && c1 == 0xFE && c0 == 0xFF) {
                    return UTF32LEEncoding.INSTANCE;
                }
                return ASCIIEncoding.INSTANCE;
            }
        }
        return enc;
    }

    // rb_enc_ascget
    public static int encAscget(byte[] pBytes, int p, int e, int[] len, Encoding enc) {
        int c;
        int l;

        if (e <= p) {
            return -1;
        }

        if (encAsciicompat(enc)) {
            c = pBytes[p] & 0xFF;
            if (!Encoding.isAscii((byte)c)) {
                return -1;
            }
            if (len != null) len[0] = 1;
            return c;
        }
        l = StringSupport.preciseLength(enc, pBytes, p, e);
        if (!StringSupport.MBCLEN_CHARFOUND_P(l)) {
            return -1;
        }
        c = enc.mbcToCode(pBytes, p, e);
        if (!Encoding.isAscii(c)) {
            return -1;
        }
        if (len != null) len[0] = l;
        return c;
    }

    // rb_enc_codepoint_len
    public static int encCodepointLength(byte[] pBytes, int p, int e, int[] len_p, Encoding enc) {
        int r;
        if (e <= p)
            throw new IllegalArgumentException("empty string");
        r = StringSupport.preciseLength(enc, pBytes, p, e);
        if (!StringSupport.MBCLEN_CHARFOUND_P(r)) {
            throw new IllegalArgumentException("invalid byte sequence in " + enc);
        }
        if (len_p != null) len_p[0] = StringSupport.MBCLEN_CHARFOUND_LEN(r);
        return StringSupport.codePoint(enc, pBytes, p, e);
    }

    // rb_enc_mbcput
    public static void encMbcput(int c, byte[] buf, int p, Encoding enc) {
        enc.codeToMbc(c, buf, p);
    }

    public static Encoding STR_ENC_GET(ByteListHolder str) {
        return getEncoding(str.getByteList());
    }

}

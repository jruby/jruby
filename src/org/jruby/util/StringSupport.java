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
package org.jruby.util;

import static org.jcodings.Encoding.CHAR_INVALID;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyObject;

public final class StringSupport {
    public static final int CODERANGE_MASK      = RubyObject.USER0_F | RubyObject.USER1_F;  
    public static final int CODERANGE_UNKNOWN   = 0;
    public static final int CODERANGE_7BIT      = RubyObject.USER0_F; 
    public static final int CODERANGE_VALID     = RubyObject.USER1_F;
    public static final int CODERANGE_BROKEN    = RubyObject.USER0_F | RubyObject.USER1_F;

    // rb_enc_mbclen
    public static int length(Encoding enc, byte[]bytes, int p, int end) {
        int n = enc.length(bytes, p, end);
        if (n > 0 && end - p >= n) return n;
        return end - p >= enc.minLength() ? enc.minLength() : end - p;
    }

    // rb_enc_precise_mbclen
    public static int preciseLength(Encoding enc, byte[]bytes, int p, int end) {
        if (p >= end) return -1 - (1);
        int n = enc.length(bytes, p, end);
        if (n > end - p) return -1 - (n - (end - p));
        return n;
    }
    
    public static int searchNonAscii(byte[]bytes, int p, int end) {
        while (p < end) {
            if (!Encoding.isAscii(bytes[p])) return p;
            p++;
        }
        return -1;
    }
    
    public static int codeRangeScan(Encoding enc, byte[]bytes, int p, int len) {
        if (enc == ASCIIEncoding.INSTANCE) {
            return searchNonAscii(bytes, p, p + len) != -1 ? CODERANGE_VALID : CODERANGE_7BIT;
        }

        int end = p + len;
        if (enc.isAsciiCompatible()) {
            p = searchNonAscii(bytes, p, end);
            if (p == -1) return CODERANGE_7BIT;
            
            while (p < end) {
                int cl = preciseLength(enc, bytes, p, end);
                if (cl <= 0) return CODERANGE_BROKEN;
                p += cl;
                if (p < end) {
                    p = searchNonAscii(bytes, p, end);
                    if (p == -1) return CODERANGE_VALID;
                }
            }
            return p > end ? CODERANGE_BROKEN : CODERANGE_VALID;
        }
        
        while (p < end) {        
            int cl = preciseLength(enc, bytes, p, end);
            if (cl <= 0) return CODERANGE_BROKEN;
            p += cl;
        }
        return p > end ? CODERANGE_BROKEN : CODERANGE_VALID;
    }
    
    public static long codeRangeScanRestartable(Encoding enc, byte[]bytes, int s, int end, int cr) { 
        if (cr == CODERANGE_BROKEN) return pack(end - s, cr);
        int p = s;
        
        if (enc == ASCIIEncoding.INSTANCE) {
            return pack(end - s, searchNonAscii(bytes, p, end) == -1 && cr != CODERANGE_VALID ? CODERANGE_7BIT : CODERANGE_VALID);
        } else if (enc.isAsciiCompatible()) {
            p = searchNonAscii(bytes, p, end);
            if (p == -1) return pack(end - s, cr != CODERANGE_VALID ? CODERANGE_7BIT : cr);

            while (p < end) {
                int cl = preciseLength(enc, bytes, p, end);
                if (cl <= 0) return pack(p - s, cl == CHAR_INVALID ? CODERANGE_BROKEN : CODERANGE_UNKNOWN);
                p += cl;

                if (p < end) {
                    p = searchNonAscii(bytes, p, end);
                    if (p == -1) return pack(end - s, CODERANGE_VALID);
                }
            }
        } else {
            while (p < end) {
                int cl = preciseLength(enc, bytes, p, end);
                if (cl <= 0) return pack(p - s, cl == CHAR_INVALID ? CODERANGE_BROKEN: CODERANGE_UNKNOWN);
                p += cl;
            }
        }
        return pack(p - s, p > end ? CODERANGE_BROKEN : CODERANGE_VALID);
    }

    public static int strLength(Encoding enc, byte[]bytes, int p, int end) {
        if (enc.isFixedWidth()) {
            return (end - p + enc.minLength() - 1) / enc.minLength();
        } else if (enc.isAsciiCompatible()) {
            int c = 0;
            while (p < end) {
                if (Encoding.isAscii(bytes[p])) {
                    int q = searchNonAscii(bytes, p, end);
                    if (q == -1) return c + (end - p);
                    c += q - p;
                    p = q;
                }
                p += length(enc, bytes, p, end);
                c++;
            }
            return c;
        }
        
        int c;
        for (c = 0; end > p; c++) p += length(enc, bytes, p, end);
        return c;
    }

    public static int strLength(ByteList bytes) { 
        return strLength(bytes.encoding, bytes.bytes, bytes.begin, bytes.begin + bytes.realSize);
    }

    public static long strLengthWithCodeRange(Encoding enc, byte[]bytes, int p, int end) {
        if (enc.isFixedWidth()) {
            return (end - p + enc.minLength() - 1) / enc.minLength();
        } else if (enc.isAsciiCompatible()) {
            return strLengthWithCodeRangeAsciiCompatible(enc, bytes, p, end);
        } else {
            return strLengthWithCodeRangeNonAsciiCompatible(enc, bytes, p, end);
        }
    }

    private static long strLengthWithCodeRangeAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
        int cr = 0, c = 0;
        while (p < end) {
            if (Encoding.isAscii(bytes[p])) {
                int q = searchNonAscii(bytes, p, end);
                if (q == -1) return pack(c + (end - p), cr == 0 ? CODERANGE_7BIT : cr);
                c += q - p;
                p = q;
            }
            int cl = preciseLength(enc, bytes, p, end);
            if (cl > 0) {
                cr |= CODERANGE_VALID; 
                p += cl;
            } else {
                cr = CODERANGE_BROKEN;
                p++;
            }
            c++;
        }
        return pack(c, cr == 0 ? CODERANGE_7BIT : cr);
    }

    private static long strLengthWithCodeRangeNonAsciiCompatible(Encoding enc, byte[]bytes, int p, int end) {
        int cr = 0, c = 0;
        for (c = 0; p < end; c++) {
            int cl = preciseLength(enc, bytes, p, end);
            if (cl > 0) {
                cr |= CODERANGE_VALID; 
                p += cl;
            } else {
                cr = CODERANGE_BROKEN;
                p++;
            }
        }
        return pack(c, cr == 0 ? CODERANGE_7BIT : cr);
    }

    public static long strLengthWithCodeRange(ByteList bytes) { 
        return strLengthWithCodeRange(bytes.encoding, bytes.bytes, bytes.begin, bytes.begin + bytes.realSize);
    }

    static long pack(int len, int cr) {
        return ((long)cr << 31) | len;
    }

    public static int unpackLength(long len) {
        return (int)len & 0x7fffffff;
    }

    public static int unpackCodeRange(long cr) {
        return (int)(cr >>> 31);
    }

    public static int codePoint(Ruby runtime, Encoding enc, byte[]bytes, int p, int end) {
        if (p >= end) throw runtime.newArgumentError("empty string");
        int cl = preciseLength(enc, bytes, p, end);
        if (cl <= 0) throw runtime.newArgumentError("invalid byte sequence in " + enc.getName()); 
        return enc.mbcToCode(bytes, p, end); 
    }

    public static int codeLength(Ruby runtime, Encoding enc, int c) {
        int n = enc.codeToMbcLength(c);
        if (n == 0) throw runtime.newArgumentError("invalid codepoint " + String.format("0x%x in ", c) + enc.getName());
        return n;
    }
}

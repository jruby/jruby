package org.jruby.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

public class ConvertBytes {
    private final Ruby runtime;
    private final ByteList _str;
    private int str;
    private int end;
    private byte[] data;
    private int base;
    private final boolean badcheck;
    private final boolean is19;

    public ConvertBytes(Ruby runtime, ByteList _str, int base, boolean badcheck) {
        this(runtime, _str, base, badcheck, true);
    }

    @Deprecated
    public ConvertBytes(Ruby runtime, ByteList _str, int base, boolean badcheck, boolean is19) {
        this.runtime = runtime;
        this._str = _str;
        this.str = _str.getBegin();
        this.data = _str.getUnsafeBytes();
        this.end = str + _str.getRealSize();
        this.badcheck = badcheck;
        this.base = base;
        this.is19 = is19;
    }

    public static final byte[] intToBinaryBytes(int i) {
        return intToUnsignedByteList(i, 1, LOWER_DIGITS).bytes();
    }

    public static final byte[] intToOctalBytes(int i) {
        return intToUnsignedByteList(i, 3, LOWER_DIGITS).bytes();
    }

    public static final byte[] intToHexBytes(int i) {
        return intToUnsignedByteList(i, 4, LOWER_DIGITS).bytes();
    }

    public static final byte[] intToHexBytes(int i, boolean upper) {
        return intToUnsignedByteList(i, 4, upper ? UPPER_DIGITS : LOWER_DIGITS).bytes();
    }

    public static final ByteList intToBinaryByteList(int i) {
        return new ByteList(intToBinaryBytes(i));
    }
    public static final ByteList intToOctalByteList(int i) {
        return new ByteList(intToOctalBytes(i));
    }
    public static final ByteList intToHexByteList(int i) {
        return new ByteList(intToHexBytes(i));
    }
    public static final ByteList intToHexByteList(int i, boolean upper) {
        return new ByteList(intToHexBytes(i, upper));
    }

    public static final byte[] intToByteArray(int i, int radix, boolean upper) {
        return longToByteArray(i, radix, upper);
    }

    public static final byte[] intToCharBytes(int i) {
        return longToByteList(i, 10, LOWER_DIGITS).bytes();
    }

    public static final byte[] longToBinaryBytes(long i) {
        return longToUnsignedByteList(i, 1, LOWER_DIGITS).bytes();
    }

    public static final byte[] longToOctalBytes(long i) {
        return longToUnsignedByteList(i, 3, LOWER_DIGITS).bytes();
    }

    public static final byte[] longToHexBytes(long i) {
        return longToUnsignedByteList(i, 4, LOWER_DIGITS).bytes();
    }

    public static final byte[] longToHexBytes(long i, boolean upper) {
        return longToUnsignedByteList(i, 4, upper ? UPPER_DIGITS : LOWER_DIGITS).bytes();
    }

    public static final ByteList longToBinaryByteList(long i) {
        return longToByteList(i, 2, LOWER_DIGITS);
    }
    public static final ByteList longToOctalByteList(long i) {
        return longToByteList(i, 8, LOWER_DIGITS);
    }
    public static final ByteList longToHexByteList(long i) {
        return longToByteList(i, 16, LOWER_DIGITS);
    }
    public static final ByteList longToHexByteList(long i, boolean upper) {
        return longToByteList(i, 16, upper ? UPPER_DIGITS : LOWER_DIGITS);
    }

    public static final byte[] longToByteArray(long i, int radix, boolean upper) {
        return longToByteList(i, radix, upper ? UPPER_DIGITS : LOWER_DIGITS).bytes();
    }

    public static final byte[] longToCharBytes(long i) {
        return longToByteList(i, 10, LOWER_DIGITS).bytes();
    }

    public static final ByteList longToByteList(long i) {
        return longToByteList(i, 10, LOWER_DIGITS);
    }

    public static final ByteList longToByteList(long i, int radix) {
        return longToByteList(i, radix, LOWER_DIGITS);
    }

    public static final ByteList longToByteList(long i, int radix, byte[] digitmap) {
        if (i == 0) return new ByteList(ZERO_BYTES);

        if (i == Long.MIN_VALUE) return new ByteList(MIN_VALUE_BYTES[radix]);

        boolean neg = false;
        if (i < 0) {
            i = -i;
            neg = true;
        }

        // max 64 chars for 64-bit 2's complement integer
        int len = 64;
        byte[] buf = new byte[len];

        int pos = len;
        do {
            buf[--pos] = digitmap[(int)(i % radix)];
        } while ((i /= radix) > 0);
        if (neg) buf[--pos] = (byte)'-';

        return new ByteList(buf, pos, len - pos);
    }

    private static final ByteList intToUnsignedByteList(int i, int shift, byte[] digitmap) {
        byte[] buf = new byte[32];
        int charPos = 32;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = digitmap[(int)(i & mask)];
            i >>>= shift;
        } while (i != 0);
        return new ByteList(buf, charPos, (32 - charPos), false);
    }

    private static final ByteList longToUnsignedByteList(long i, int shift, byte[] digitmap) {
        byte[] buf = new byte[64];
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = digitmap[(int)(i & mask)];
            i >>>= shift;
        } while (i != 0);
        return new ByteList(buf, charPos, (64 - charPos), false);
    }

    public static final byte[] twosComplementToBinaryBytes(byte[] in) {
        return twosComplementToUnsignedBytes(in, 1, false);
    }
    public static final byte[] twosComplementToOctalBytes(byte[] in) {
        return twosComplementToUnsignedBytes(in, 3, false);
    }
    public static final byte[] twosComplementToHexBytes(byte[] in, boolean upper) {
        return twosComplementToUnsignedBytes(in, 4, upper);
    }

    private static final byte[] ZERO_BYTES = new byte[] {(byte)'0'};

    private static final byte[][] MIN_VALUE_BYTES;
    static {
        MIN_VALUE_BYTES = new byte[37][];
        for (int i = 2; i <= 36; i++) {
            MIN_VALUE_BYTES[i] =  ByteList.plain(Long.toString(Long.MIN_VALUE, i));
        }
    }

    private static final byte[] LOWER_DIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };

    private static final byte[] UPPER_DIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'A' , 'B' ,
        'C' , 'D' , 'E' , 'F' , 'G' , 'H' ,
        'I' , 'J' , 'K' , 'L' , 'M' , 'N' ,
        'O' , 'P' , 'Q' , 'R' , 'S' , 'T' ,
        'U' , 'V' , 'W' , 'X' , 'Y' , 'Z'
        };

    public static final byte[] twosComplementToUnsignedBytes(byte[] in, int shift, boolean upper) {
        if (shift < 1 || shift > 4) {
            throw new IllegalArgumentException("shift value must be 1-4");
        }
        int ilen = in.length;
        int olen = (ilen * 8 + shift - 1 ) / shift;
        byte[] out = new byte[olen];
        int mask = (1 << shift) - 1;
        byte[] digits = upper ? UPPER_DIGITS : LOWER_DIGITS;
        int bitbuf = 0;
        int bitcnt = 0;
        for(int i = ilen, o = olen; --o >= 0; ) {
            if(bitcnt < shift) {
                bitbuf |= ((int)in[--i] & (int)0xff) << bitcnt;
                bitcnt += 8;
            }
            out[o] = digits[bitbuf & mask];
            bitbuf >>= shift;
            bitcnt -= shift;
        }
        return out;
    }

    /** rb_cstr_to_inum
     *
     */
    @Deprecated
    public static RubyInteger byteListToInum(Ruby runtime, ByteList str, int base, boolean badcheck) {
        return new ConvertBytes(runtime, str, base, badcheck, false).byteListToInum();
    }

    public static RubyInteger byteListToInum19(Ruby runtime, ByteList str, int base, boolean badcheck) {
        return new ConvertBytes(runtime, str, base, badcheck).byteListToInum();
    }

    private final static byte[] conv_digit = new byte[128];
    private final static boolean[] digit = new boolean[128];
    private final static boolean[] space = new boolean[128];
    private final static boolean[] spaceOrUnderscore = new boolean[128];

    static {
        Arrays.fill(conv_digit, (byte)-1);
        Arrays.fill(digit, false);
        for(char c = '0'; c <= '9'; c++) {
            conv_digit[c] = (byte)(c - '0');
            digit[c] = true;
        }

        for(char c = 'a'; c <= 'z'; c++) {
            conv_digit[c] = (byte)(c - 'a' + 10);
        }

        for(char c = 'A'; c <= 'Z'; c++) {
            conv_digit[c] = (byte)(c - 'A' + 10);
        }

        Arrays.fill(space, false);
        space['\t'] = true;
        space['\n'] = true;
        space[11] = true; // \v
        space['\f'] = true;
        space['\r'] = true;
        space[' '] = true;

        Arrays.fill(spaceOrUnderscore, false);
        spaceOrUnderscore['\t'] = true;
        spaceOrUnderscore['\n'] = true;
        spaceOrUnderscore[11] = true; // \v
        spaceOrUnderscore['\f'] = true;
        spaceOrUnderscore['\r'] = true;
        spaceOrUnderscore[' '] = true;
        spaceOrUnderscore['_'] = true;
    }

    public static byte[] bytesToUUIDBytes(byte[] randBytes, boolean upper) {
        ByteBuffer bytes = ByteBuffer.wrap(randBytes);
        long N0 = bytes.getInt() & 0xFFFFFFFFL;
        int n1 = bytes.getShort() & 0xFFFF;
        int n2 = bytes.getShort() & 0xFFFF;
        n2 = n2 & 0x0FFF | 0x4000;
        int n3 = bytes.getShort() & 0xFFFF;
        n3 = n3 & 0x3FFF | 0x8000;
        int n4 = bytes.getShort() & 0xFFFF;
        long N5 = bytes.getInt() & 0xFFFFFFFFL;
        byte[] convert = upper ? UPPER_DIGITS : LOWER_DIGITS;
        return new byte[]{
                convert[(int)((N0 >> 28) & 0xF)],
                convert[(int)((N0 >> 24) & 0xF)],
                convert[(int)((N0 >> 20) & 0xF)],
                convert[(int)((N0 >> 16) & 0xF)],
                convert[(int)((N0 >> 12) & 0xF)],
                convert[(int)((N0 >> 8) & 0xF)],
                convert[(int)((N0 >> 4) & 0xF)],
                convert[(int)(N0 & 0xF)],
                (byte)'-',
                convert[(n1 >> 12) & 0xF],
                convert[(n1 >> 8) & 0xF],
                convert[(n1 >> 4) & 0xF],
                convert[n1 & 0xF],
                (byte)'-',
                convert[(n2 >> 12) & 0xF],
                convert[(n2 >> 8) & 0xF],
                convert[(n2 >> 4) & 0xF],
                convert[n2 & 0xF],
                (byte)'-',
                convert[(n3 >> 12) & 0xF],
                convert[(n3 >> 8) & 0xF],
                convert[(n3 >> 4) & 0xF],
                convert[n3 & 0xF],
                (byte)'-',
                convert[(n4 >> 12) & 0xF],
                convert[(n4 >> 8) & 0xF],
                convert[(n4 >> 4) & 0xF],
                convert[n4 & 0xF],
                convert[(int)((N5 >> 28) & 0xF)],
                convert[(int)((N5 >> 24) & 0xF)],
                convert[(int)((N5 >> 20) & 0xF)],
                convert[(int)((N5 >> 16) & 0xF)],
                convert[(int)((N5 >> 12) & 0xF)],
                convert[(int)((N5 >> 8) & 0xF)],
                convert[(int)((N5 >> 4) & 0xF)],
                convert[(int)(N5 & 0xF)]
        };
    }

    /** conv_digit
     *
     */
    private byte convertDigit(byte c) {
        if(c < 0) {
            return -1;
        }
        return conv_digit[c];
    }

    /** ISSPACE
     *
     */
    private boolean isSpace(int str) {
        byte c;
        if(str == end || (c = data[str]) < 0) {
            return false;
        }
        return space[c];
    }

    /** ISDIGIT
     *
     */
    private boolean isDigit(byte[] buf, int str) {
        byte c;
        if(str == buf.length || (c = buf[str]) < 0) {
            return false;
        }
        return digit[c];
    }

    /** ISSPACE || *str == '_'
     *
     */
    private boolean isSpaceOrUnderscore(int str) {
        byte c;
        if(str == end || (c = data[str]) < 0) {
            return false;
        }
        return spaceOrUnderscore[c];
    }

    private boolean getSign() {
        //System.err.println("getSign()");
        boolean sign = true;
        if(str < end) {
            if(data[str] == '+') {
                str++;
            } else if(data[str] == '-') {
                str++;
                sign = false;
            }
        }
        //System.err.println(" getSign/" + sign);
        return sign;
    }

    private void ignoreLeadingWhitespace() {
        if(badcheck || is19) {
            while(isSpace(str)) {
                str++;
            }
        } else {
            while(isSpaceOrUnderscore(str)) {
                str++;
            }
        }
    }

    private void figureOutBase() {
        //System.err.println("figureOutBase()/base=" + base);
        if(base <= 0) {
            if(str < end && data[str] == '0') {
                if(str + 1 < end) {
                    switch(data[str+1]) {
                    case 'x':
                    case 'X':
                        base = 16;
                        break;
                    case 'b':
                    case 'B':
                        base = 2;
                        break;
                    case 'o':
                    case 'O':
                        base = 8;
                        break;
                    case 'd':
                    case 'D':
                        base = 10;
                        break;
                    default:
                        base = 8;
                    }
                } else {
                    base = 8;
                }
            } else if(base < -1) {
                base = -base;
            } else {
                base = 10;
            }
        }
        //System.err.println(" figureOutBase/base=" + base);
    }

    private int calculateLength() {
        int len = 0;
        byte second = ((str+1 < end) && data[str] == '0') ? data[str+1] : (byte)0;
        //System.err.println("calculateLength()/str=" + str);
        switch(base) {
        case 2:
            len = 1;
            if(second == 'b' || second == 'B') {
                str+=2;
            }
            break;
        case 3:
            len = 2;
            break;
        case 8:
            if(second == 'o' || second == 'O') {
                str+=2;
            }
        case 4: case 5: case 6: case 7:
            len = 3;
            break;
        case 10:
            if(second == 'd' || second == 'D') {
                str+=2;
            }
        case 9: case 11: case 12:
        case 13: case 14: case 15:
            len = 4;
            break;
        case 16:
            len = 4;
            if(second == 'x' || second == 'X') {
                str+=2;
            }
            break;
        default:
            if(base < 2 || 36 < base) {
                throw runtime.newArgumentError("illegal radix " + base);
            }
            if(base <= 32) {
                len = 5;
            } else {
                len = 6;
            }
            break;
        }

        //System.err.println(" calculateLength()/str=" + str);
        return len;
    }

    private void squeezeZeroes() {
        byte c;
        if(str < end && data[str] == '0') {
            str++;
            int us = 0;
            while((str < end) && ((c = data[str]) == '0' || c == '_')) {
                if(c == '_') {
                    if(++us >= 2) {
                        break;
                    }
                } else {
                    us += 0;
                }
                str++;
            }
            if(str == end || isSpace(str)) {
                str--;
            }
        }
    }

    private long stringToLong(int nptr, int[] endptr, int base) {
        //System.err.println("stringToLong(" + nptr + ", " + base + ")");
        if(base < 0 || base == 1 || base > 36) {
            return 0;
        }
        int save = nptr;
        int s = nptr;
        boolean overflow = false;

        while(isSpace(s)) {
            s++;
        }

        if(s != end) {
            boolean negative = false;
            if(data[s] == '-') {
                negative = true;
                s++;
            } else if(data[s] == '+') {
                negative = false;
                s++;
            }

            save = s;
            byte c;
            long i = 0;

            final long cutoff = Long.MAX_VALUE / (long)base;
            final long cutlim = Long.MAX_VALUE % (long)base;

            while(s < end) {
                //System.err.println(" stringToLong/reading c=" + data[s]);
                c = convertDigit(data[s]);
                //System.err.println(" stringToLong/converted c=" + c);
                if(c == -1 || c >= base) {
                    break;
                }
                s++;

                if(i > cutoff || (i == cutoff && c > cutlim)) {
                    overflow = true;
                } else {
                    i *= base;
                    i += c;
                }
            }

            if(s != save) {
                if(endptr != null) {
                    endptr[0] = s;
                }

                if(overflow) {
                    throw new ERange(negative ? ERange.Kind.Underflow : ERange.Kind.Overflow);
                }

                if(negative) {
                    return -i;
                } else {
                    return i;
                }
            }
        }

        if(endptr != null) {
            if(save - nptr >= 2 && (data[save-1] == 'x' || data[save-1] == 'X') && data[save-2] == '0') {
                endptr[0] = save-1;
            } else {
                endptr[0] = nptr;
            }
        }
        return 0;
    }

    public RubyInteger byteListToInum() {
        if(_str == null) {
            if(badcheck) {
                invalidString("Integer");
            }
            return runtime.newFixnum(0);
        }

        ignoreLeadingWhitespace();

        boolean sign = getSign();

        if(str < end) {
            if(data[str] == '+' || data[str] == '-') {
                if(badcheck) {
                    invalidString("Integer");
                }
                return runtime.newFixnum(0);
            }
        }

        figureOutBase();

        int len = calculateLength();

        squeezeZeroes();

        byte c = 0;
        if(str < end) {
            c = data[str];
        }
        c = convertDigit(c);
        if(c < 0 || c >= base) {
            if(badcheck) {
                invalidString("Integer");
            }
            return runtime.newFixnum(0);
        }

        if (base <= 10) {
            len *= (trailingLength());
        } else {
            len *= (end-str);
        }

        //System.err.println(" main/len=" + len);
        if(len < Long.SIZE-1) {
            int[] endPlace = new int[]{str};
            long val = stringToLong(str, endPlace, base);
            //System.err.println(" stringToLong=" + val);
            if(endPlace[0] < end && data[endPlace[0]] == '_') {
                return bigParse(len, sign);
            }
            if(badcheck) {
                if(endPlace[0] == str) {
                    invalidString("Integer"); // no number
                }

                while(isSpace(endPlace[0])) {
                    endPlace[0]++;
                }

                if(endPlace[0] < end) {
                    invalidString("Integer"); // trailing garbage
                }
            }

            if(sign) {
                return runtime.newFixnum(val);
            } else {
                return runtime.newFixnum(-val);
            }
        }
        return bigParse(len, sign);
    }

    private int trailingLength() {
        int newLen = 0;
        for (int i=str; i < end; i++) {
            if (Character.isDigit(data[i])) newLen++;
            else return newLen;
        }
        return newLen;
    }

    private RubyInteger bigParse(int len, boolean sign) {
        if(badcheck && str < end && data[str] == '_') {
            invalidString("Integer");
        }

        char[] result = new char[end-str];
        int resultIndex = 0;

        byte nondigit = -1;

        // str2big_scan_digits
        {
            while(str < end) {
                byte c = data[str++];
                byte cx = c;
                if(c == '_') {
                    if(nondigit != -1) {
                        if(badcheck) {
                            invalidString("Integer");
                        }
                        break;
                    }
                    nondigit = c;
                    continue;
                } else if((c = convertDigit(c)) < 0) {
                    break;
                }
                if(c >= base) {
                    break;
                }
                nondigit = -1;
                //System.err.println("ADDING CHAR: " + (char)cx + " with number: " + cx);
                result[resultIndex++] = (char)cx;
            }

            if(resultIndex == 0) { return runtime.newFixnum(0); }

            int tmpStr = str;
            if (badcheck) {
                // no str-- here because we don't null-terminate strings
                if (_str.getBegin()+1 < tmpStr && data[tmpStr-1] == '_') invalidString("Integer");
                while (tmpStr < end && Character.isWhitespace(data[tmpStr])) tmpStr++;
                if (tmpStr < end) {
                    invalidString("Integer");
                }

            }
        }

        String s = new String(result, 0, resultIndex);
        BigInteger z = (base == 10) ? stringToBig(s) : new BigInteger(s, base);
        if(!sign) { z = z.negate(); }

        if(badcheck) {
            if(_str.getBegin() + 1 < str && data[str-1] == '_') {
                invalidString("Integer");
            }
            while(str < end && isSpace(str)) {
                str++;
            }
            if(str < end) {
                invalidString("Integer");
            }
        }

        return RubyBignum.bignorm(runtime, z);
    }

    private BigInteger stringToBig(String str) {
        str = str.replaceAll("_", "");
        int size = str.length();
        int nDigits = 512;
        if (size < nDigits) { nDigits = size; }

        int j = size - 1;
        int i = j - nDigits + 1;

        BigInteger digits[] = new BigInteger[j / nDigits + 1];

        for(int z = 0; j >= 0; z++) {
            digits[z] = new BigInteger(str.substring(i, j + 1).trim());
            j = i - 1;
            i = j - nDigits + 1;
            if(i < 0) { i = 0; }
        }

        BigInteger b10x = BigInteger.TEN.pow(nDigits);
        int n = digits.length;
        while(n > 1) {
            i = 0;
            j = 0;
            while(i < n / 2) {
                digits[i] = digits[j].add(digits[j + 1].multiply(b10x));
                i += 1;
                j += 2;
            }
            if(j == n-1) {
                digits[i] = digits[j];
                i += 1;
            }
            n = i;
            b10x = b10x.multiply(b10x);
        }

        return digits[0];
    }

    public static class ERange extends RuntimeException {
        public static enum Kind {Overflow, Underflow};
        private Kind kind;
        public ERange() {
            super();
        }
        public ERange(Kind kind) {
            super();
            this.kind = kind;
        }
        public Kind getKind() {
            return kind;
        }
    }

    /** rb_invalid_str
     *
     */
    private void invalidString(String type) {
        IRubyObject s = RubyString.newString(runtime, _str).inspect();
        throw runtime.newArgumentError("invalid value for " + type + "(): " + s);
    }
}

package org.jruby.util;

import java.math.BigInteger;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyNumeric;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

public class Convert2 {
    private final Ruby runtime;
    private final ByteList _str;
    private int str;
    private int end;
    private byte[] data;
    private int base;
    private final boolean badcheck;

    public Convert2(Ruby runtime, ByteList _str, int base, boolean badcheck) {
        this.runtime = runtime;
        this._str = _str;
        this.str = _str.begin;
        this.data = _str.bytes;
        this.end = str + _str.realSize;
        this.badcheck = badcheck;
        this.base = base;
    }

    public static final byte[] intToBinaryBytes(int i) {
        return ByteList.plain(Integer.toBinaryString(i));
    }

    public static final byte[] intToOctalBytes(int i) {
        return ByteList.plain(Integer.toOctalString(i));
    }

    public static final byte[] intToHexBytes(int i) {
        return ByteList.plain(Integer.toHexString(i).toLowerCase());
    }

    public static final byte[] intToHexBytes(int i, boolean upper) {
        String s = Integer.toHexString(i);
        s = upper ? s.toUpperCase() : s.toLowerCase();
        return ByteList.plain(s);
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
        String s = Integer.toString(i, radix);
        s = upper ? s.toUpperCase() : s.toLowerCase();
        return ByteList.plain(s);
    }

    public static final byte[] intToCharBytes(int i) {
        return ByteList.plain(Integer.toString(i));
    }

    public static final byte[] longToBinaryBytes(long i) {
        return ByteList.plain(Long.toBinaryString(i));
    }

    public static final byte[] longToOctalBytes(long i) {
        return ByteList.plain(Long.toOctalString(i));
    }

    public static final byte[] longToHexBytes(long i) {
        return ByteList.plain(Long.toHexString(i).toLowerCase());
    }

    public static final byte[] longToHexBytes(long i, boolean upper) {
        String s = Long.toHexString(i);
        s = upper ? s.toUpperCase() : s.toLowerCase();
        return ByteList.plain(s);
    }

    public static final ByteList longToBinaryByteList(long i) {
        return new ByteList(longToBinaryBytes(i));
    }
    public static final ByteList longToOctalByteList(long i) {
        return new ByteList(longToOctalBytes(i));
    }
    public static final ByteList longToHexByteList(long i) {
        return new ByteList(longToHexBytes(i));
    }
    public static final ByteList longToHexByteList(long i, boolean upper) {
        return new ByteList(longToHexBytes(i, upper));
    }

    public static final byte[] longToByteArray(long i, int radix, boolean upper) {
        String s = Long.toString(i, radix);
        s = upper ? s.toUpperCase() : s.toLowerCase();
        return ByteList.plain(s);
    }

    public static final byte[] longToCharBytes(long i) {
        return ByteList.plain(Long.toString(i));
    }

    public static final ByteList longToByteList(long i) {
        return new ByteList(ByteList.plain(Long.toString(i)), false);
    }

    public static final ByteList longToByteList(long i, int radix) {
        return new ByteList(ByteList.plain(Long.toString(i, radix)), false);
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
    public static RubyInteger byteListToInum(Ruby runtime, ByteList str, int base, boolean badcheck) {
        //System.err.println("byteListToInum(" + str + ")");
        return new Convert2(runtime, str, base, badcheck).byteListToInum();
    }

    /** rb_cstr_to_dbl
     *
     */
    public static double byteListToDouble(Ruby runtime, ByteList str, boolean badcheck) {
        //System.err.println("byteListToInum(" + str + ")");
        return new Convert2(runtime, str, -1, badcheck).byteListToDouble();
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
        if(badcheck) {
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

        len *= (end-str);

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

    private RubyInteger bigParse(int len, boolean sign) {
        if(badcheck && str < end && data[str] == '_') {
            invalidString("Integer");
        }

        char[] result = new char[end-str];
        int resultIndex = 0;

        byte nondigit = -1;

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

        BigInteger z;
        if(resultIndex == 0) {
            z = BigInteger.ZERO;
        } else {
            z = new BigInteger(new String(result, 0, resultIndex), base);
        }

        if(!sign) {
            z = z.negate();
        }

        if(badcheck) {
            if(_str.begin + 1 < str && data[str-1] == '_') {
                invalidString("Integer");
            }
            while(str < end && isSpace(str)) {
                str++;
            }
            if(str < end) {
                invalidString("Integer");
            }
        }

        return new RubyBignum(runtime, z);
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

    public static double stringToDouble(Ruby runtime, String number) {
        ByteList s = new ByteList(ByteList.plain(number), false);
        return new Convert2(runtime, s, -1, false).stringToDouble(0, null);
    }

    private double fallbackParsing(int s, int sign, int[] endptr) {
        int start = s;
        int add = 0;
        if(sign == -1) {
            add = 1;
        }
        char[] buf = new char[(end-s) + add];
        int index = 0;
        if(sign == -1) {
            buf[index++] = '-';
        }
        boolean hasDot = false;
        while(s < end) {
            if('0' <= data[s] && data[s] <= '9') {
                buf[index++] = (char)data[s];
            } else if(!hasDot && data[s] == '.') {
                buf[index++] = '.';
                hasDot = true;
            } else {
                break;
            }
            s++;
        }

        if(s < end && (data[s] == 'e' || data[s] == 'E')) {
            ++s;
            buf[index++] = 'e';
            if(s < end && (data[s] == '+' || data[s] == '-')) {
                buf[index++] = (char)data[s];
                ++s;
            }
            while(s < end) {
                if('0' <= data[s] && data[s] <= '9') {
                    buf[index++] = (char)data[s];
                    s++;  
                } else {
                    break;
                }
            }
        }

        if(endptr != null) {
            endptr[0] = s;
        }

        return Double.parseDouble(new String(buf, 0, index));
    }

    private double stringToDouble(int nptr, int[] endptr) {
        double num;
        boolean got_dot, got_digit;
        long exponent;
        
        int s = nptr;
        while(isSpace(s)) {
            s++;
        }

        int sign = (s < end && data[s] == '-') ? -1 : 1;
        if(s < end && (data[s] == '-' || data[s] == '+')) {
            ++s;
        }
        int saveFallback = s;
        num = 0.0;
        got_dot = false;
        got_digit = false;
        exponent = 0;
        int digits = 0;

        while(s < end) {
            if('0' <= data[s] && data[s] <= '9') {
                got_digit = true;
                digits++;
                if(digits > 15) {
                    return fallbackParsing(saveFallback, sign, endptr);
                }
                
                if(num > Double.MAX_VALUE * 0.1) {
                    ++exponent;
                } else {
                    int n = data[s] - '0';
                    num = (10.0*num) + n;
                }
                
                if(got_dot) {
                    --exponent;
                }
            } else if(!got_dot && data[s] == '.') {
                got_dot = true;
            } else {
                break;
            }
            ++s;
        }

        if(!got_digit) {
            if(s+2 < end && 
               (data[s] == 'n' || data[s] == 'N') &&
               (data[s+1] == 'a' || data[s+1] == 'A') &&
               (data[s+2] == 'n' || data[s+2] == 'N')) {
                
                if(endptr != null) {
                    endptr[0] = s+3;
                }
                return Double.NaN;
            } else if(s+7 < end &&
               (data[s] == 'i' || data[s] == 'I') &&
               (data[s+1] == 'n' || data[s+1] == 'N') &&
               (data[s+2] == 'f' || data[s+2] == 'F') &&
               (data[s+3] == 'i' || data[s+3] == 'I') &&
               (data[s+4] == 'n' || data[s+4] == 'N') &&
               (data[s+5] == 'i' || data[s+5] == 'I') &&
               (data[s+6] == 't' || data[s+6] == 'T') &&
               (data[s+7] == 'y' || data[s+7] == 'Y')) {

                if(endptr != null) {
                    endptr[0] = s+8;
                }
                return sign == -1 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }
            
            if(endptr != null) {
                endptr[0] = nptr;
            }
            return 0.0;
        }

        if(s < end && (data[s] == 'e' || data[s] == 'E')) {
            ++s;
            long exp = 0;
            int[] endx = new int[]{0};
            try {
                exp = stringToLong(s, endx, 10);
            } catch(ERange e) {
                if(endptr != null) {
                    endptr[0] = endx[0];
                }
                throw e;
            }
            if(endx[0] == s) {
                endx[0] = s-1;
            }
            
            s = endx[0];
            exponent += exp;
        }

        if(endptr != null) {
            endptr[0] = s;
        }

        if(num == 0.0) {
            return 0.0 * sign;
        }

        if(exponent < 0) {
            if(num < Double.MIN_VALUE * Math.pow(10.0, (double) -exponent)) {
                throw new ERange(ERange.Kind.Underflow);
            } 
        } else if(exponent > 0) {
            if(num > Double.MAX_VALUE * Math.pow(10.0, (double) -exponent)) {
                throw new ERange(ERange.Kind.Overflow);
            } 
        }

        num *= Math.pow(10.0, (double)exponent);
        return num * sign;
    }

    public double byteListToDouble() {
        if(_str == null) {
            return 0.0;
        }

        int q = str;
        ignoreLeadingWhitespace();

        int[] endPlace = new int[]{str};
        double d = 0.0;
        
        try {
            d = stringToDouble(str, endPlace);
        } catch(ERange e) {
            d = e.getKind() == ERange.Kind.Overflow ? Double.MAX_VALUE : Double.MIN_VALUE;
            int w = endPlace[0] - str;
            String ellipsis = "";
            if(w > 20) {
                w = 20;
                ellipsis = "...";
            } else {
                ellipsis = "";
            }
            try {
                runtime.getWarnings().warn("Float " + new String(data, str, w, "ISO-8859-1") + ellipsis +" out of range");
            } catch(java.io.UnsupportedEncodingException ex) {}
        }
        
        if(str == endPlace[0]) {
            if(badcheck) {
                invalidString("Float()");
            }
            return d;
        }
        if(endPlace[0]<end) {
            byte[] buf = new byte[end-str];
            int n =0;
            System.arraycopy(data, str, buf, 0, endPlace[0]-str);
            n = endPlace[0] - str;
            str = endPlace[0];
            while(str < end) {
                if(data[str] == '_') {
                    if(badcheck) {
                        if(n == 0 || !isDigit(buf, n-1)) {
                            invalidString("Float()");
                        }
                        str++;
                        if(!isDigit(data, str)) {
                            invalidString("Float()");
                        }
                    } else {
                        str++;
                        while(str < end && data[str] == '_') {
                            str++;
                        }
                        continue;
                    }
                }
                buf[n++] = data[str++];
            }
            data = buf;
            str = 0;
            end = buf.length;

            try {
                d = stringToDouble(str, endPlace);
            } catch(ERange e) {
                d = e.getKind() == ERange.Kind.Overflow ? Double.MAX_VALUE : Double.MIN_VALUE;
                int w = endPlace[0] - str;
                String ellipsis = "";
                if(w > 20) {
                    w = 20;
                    ellipsis = "...";
                } else {
                    ellipsis = "";
                }
                try {
                    runtime.getWarnings().warn("Float " + new String(data, str, w, "ISO-8859-1") + ellipsis +" out of range");
                } catch(java.io.UnsupportedEncodingException ex) {}
            }

            if(badcheck) {
                if(str == endPlace[0]) {
                    invalidString("Float()");
                }
                while(endPlace[0] < end && isSpace(endPlace[0])) {
                    endPlace[0]++;
                }
                if(endPlace[0] < end) {
                    invalidString("Float()");
                }
            }
        }

        return d;
    }

    /** rb_invalid_str
     *
     */
    private void invalidString(String type) {
        IRubyObject s = RubyString.newString(runtime, _str).inspect();
        throw runtime.newArgumentError("invalid value for " + type + ": " + s);
    }
}

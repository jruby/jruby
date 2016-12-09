package org.jruby.truffle.util;

import org.jruby.truffle.util.ByteList;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ConvertBytes {

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

}

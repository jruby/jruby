/*
 * Copyright (c) 2007 Ola Bini
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to 
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies 
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 */
package org.rej;

class Helpers {
    private Helpers() {}
    public final static void STORE_NUMBER(byte[] d, int dix, int number) {
        d[dix] = (byte)(number&0xFF);
        d[dix+1] = (byte)((number >> 8)&0xFF);
        int vv = ((d[dix] & 0377) + ((d[dix+1]&0xFF) << 8));
        if((vv & 0x8000) != 0) {
            vv |= 0xFFFF0000;
        }
    }

    public final static void STORE_NUMBER(int[] d, int dix, int number) {
        d[dix] = (number&0xFF);
        d[dix+1] = ((number >> 8)&0xFF);
        int vv = ((d[dix] & 0377) + (d[dix+1] << 8));
        if((vv & 0x8000) != 0) {
            vv |= 0xFFFF0000;
        }
    }

    public final static void STORE_MBC(byte[] d, int dix, long c) {
        d[dix  ] = (byte)(((c)>>>24) & 0xff);
        d[dix+1] = (byte)(((c)>>>16) & 0xff);
        d[dix+2] = (byte)(((c)>>> 8) & 0xff);
        d[dix+3] = (byte)(((c)>>> 0) & 0xff);
    }

    public final static int EXTRACT_MBC(byte[] p, int pix) {
        return (p[pix]&0xFF)<<24 |
            (p[pix+1]&0xFF) <<16 |
            (p[pix+2]&0xFF) <<8 |
            (p[pix+3]&0xFF);
    }

    public final static int EXTRACT_NUMBER(byte[] b, int p) {
        int vv = (b[p] & 0xFF) | ((b[p+1]&0xFF) << 8);
        if((vv & 0x8000) != 0) {
            vv |= 0xFFFF0000;
        }
        return vv;
    }

    public final static int EXTRACT_UNSIGNED(byte[] b, int p) {
        return (b[p] & 0377) | ((b[p+1]&0xFF) << 8);
    }

    public final static char read_backslash(char c) {
        switch(c) {
        case 'n':
            return '\n';
        case 't':
            return '\t';
        case 'r':
            return '\r';
        case 'f':
            return '\f';
        case 'v':
            return 11;
        case 'a':
            return '\007';
        case 'b':
            return '\010';
        case 'e':
            return '\033';
        }
        return c;
    }

    public final static int read_special(byte[] p, int pix, int pend, int[] pp) {
        int c;
        if(pix == pend) {
            pp[0] = pix;
            return ~0;
        }
        c = p[pix++]&0xFF;
        switch(c) {
        case 'M':
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++]&0xFF;
            if(c != '-') {
                return -1;
            }
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++]&0xFF;
            pp[0] = pix;
            if(c == '\\') {
                return read_special(p, --pix, pend, pp) | 0x80;
            } else if(c == -1) { 
                return ~0;
            } else {
                return ((c & 0xff) | 0x80);
            }
        case 'C':
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++]&0xFF;
            if(c != '-') {
                return -1;
            }
        case 'c':
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++]&0xFF;
            pp[0] = pix;
            if(c == '\\') {
                c = read_special(p, --pix, pend, pp);
            } else if(c == '?') {
                return 0177;
            } else if(c == -1) {
                return ~0;
            }
            return c & 0x9f;
        default:
            pp[0] = pix+1;
            return read_backslash((char)c);
        }
    }

    final static String HEXDIGIT = "0123456789abcdef0123456789ABCDEF";
    public final static long scan_hex(byte[] p, int start, int len, int[] retlen) {
        int s = start;
        long retval = 0;
        int tmp;
        while(len-- > 0 && s<p.length && (tmp = HEXDIGIT.indexOf(p[s])) != -1) {
            retval <<= 4;
            retval |= (tmp & 15);
            s++;
        }
        retlen[0] = s-start;
        return retval;
    }

    public final static long scan_oct(byte[] p, int start, int len, int[] retlen) {
        int s = start;
        long retval = 0;

        while(len-- > 0 && s<p.length && p[s] >= '0' && p[s] <= '7') {
            retval <<= 3;
            retval |= (p[s++] - '0');
        }
        retlen[0] = s-start;
        return retval;

    }


    public final static int memcmp(byte[] s, int s1, byte[] ss, int s2, int len) {
        while(len > 0) {
            if(s[s1++] != ss[s2++]) {
                return 1;
            }
            len--;
        }
        return 0;
    }

    public final static int memcmp(byte[] s, int s1, int s2, int len) {
        return memcmp(s,s1,s,s2,len);
    }

    public final static int memcmp(char[] s, int s1, char[] ss, int s2, int len) {
        while(len > 0) {
            if(s[s1++] != ss[s2++]) {
                return 1;
            }
            len--;
        }
        return 0;
    }

    public final static int memcmp(char[] s, int s1, int s2, int len) {
        return memcmp(s,s1,s,s2,len);
    }

    final static void set_list_bits(long c1, long c2, byte[] b, int bix) {
        char sbc_size = (char)(b[bix-1]&0xFF);
        int mbc_size = EXTRACT_UNSIGNED(b,bix+sbc_size);
        int beg,end,upb;
        if(c1 > c2) {
            return;
        }
        bix+=(sbc_size+2);
        for(beg=0,upb=mbc_size;beg<upb;) {
            int mid = (beg+upb)>>>1;
            if((c1-1) > EXTRACT_MBC(b,bix+(mid*8+4))) {
                beg = mid+1;
            } else {
                upb = mid;
            }
        }
        for(end=beg,upb=mbc_size; end<upb; ) {
            int mid = (end+upb)>>>1;
            if(c2 >= (EXTRACT_MBC(b,bix+(mid*8))-1)) {
                end = mid+1;
            } else {
                upb = mid;
            }
        }
        if(beg != end) {
            if(c1 > EXTRACT_MBC(b,bix+(beg*8))) {
                c1 = EXTRACT_MBC(b,bix+(beg*8));
            }
            if(c2 < EXTRACT_MBC(b,bix+((end - 1)*8+4))) {
                c2 = EXTRACT_MBC(b,bix+((end - 1)*8+4));
            }
        }
        if(end < mbc_size && end != beg + 1) {
            System.arraycopy(b,bix+(end*8),b,bix+((beg+1)*8),(mbc_size - end)*8);
        }
        STORE_MBC(b,bix+(beg*8 + 0),c1);
        STORE_MBC(b,bix+(beg*8 + 4),c2);
        mbc_size += beg - end + 1;
        STORE_NUMBER(b,bix-2, mbc_size);
    }

    public static void err(String msg) throws PatternSyntaxException {
        throw new PatternSyntaxException(msg);
    }
}

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

public abstract class REJConstants {
    private REJConstants(){}

    public final static int NON_GREEDY = 1;
    public final static int REG_UNSET_VALUE = -1;

    public final static int NUM_NONREG_ITEMS = 5;
    public final static int NUM_REG_ITEMS = 2;
    public final static int NUM_COUNT_ITEMS = 2;

    public final static int NFAILURES=50;

    public final static int INIT_BUF_SIZE = 23;

    /** match will be done case insensetively */
    public final static int RE_OPTION_IGNORECASE  =1;
    /** perl-style extended pattern available */
    public final static int RE_OPTION_EXTENDED    =1<<1;
    /** newline will be included for . */
    public final static int RE_OPTION_MULTILINE   =1<<2;
    /** ^ and $ ignore newline */
    public final static int RE_OPTION_SINGLELINE  =1<<3;
    /** search for longest match, in accord with POSIX regexp */
    public final static int RE_OPTION_LONGEST     =1<<4;

    public final static int RE_MAY_IGNORECASE  = (RE_OPTION_LONGEST<<1);
    public final static int RE_OPTIMIZE_ANCHOR = (RE_MAY_IGNORECASE<<1);
    public final static int RE_OPTIMIZE_EXACTN = (RE_OPTIMIZE_ANCHOR<<1);
    public final static int RE_OPTIMIZE_NO_BM  = (RE_OPTIMIZE_EXACTN<<1);
    public final static int RE_OPTIMIZE_BMATCH = (RE_OPTIMIZE_NO_BM<<1);

    public final static int RE_DUP_MAX            =((1 << 15) - 1);

    public final static byte Sword = 1;
    public final static byte Sword2 = 2;

    public final static byte[] re_syntax_table = new byte[256];

    public final static int NUMBER_LENGTH = 2;

    static {
        char c;
        for(c=0; c<=0x7f; c++) {
            if(Character.isLetterOrDigit(c)) {
                re_syntax_table[c] = Sword;
            }
        }
        re_syntax_table['_'] = Sword;
        for(c=0x80; c<=0xff; c++) {
            if(Character.isLetterOrDigit(c)) {
                re_syntax_table[c] = Sword2;
            }
        }
    }
}

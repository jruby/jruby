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

import static org.rej.MBC.*;

public class CompileContext {
    public final char[] translate;
    public final int current_mbctype;
    public final byte[] re_mbctab;
    public final boolean single_byte;
    public CompileContext() {
        this(null,MBCTYPE_ASCII,mbctab_ascii,true);
    }
    public CompileContext(char[] t) {
        this(t,MBCTYPE_ASCII,mbctab_ascii,true);
    }
    public CompileContext(char[] t, int mbc, byte[] mbctab, boolean single_byte) {
        this.translate = t;
        this.current_mbctype = mbc;
        this.re_mbctab = mbctab;
        this.single_byte = single_byte;
    }

    public final static CompileContext ASCII = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_ASCII, mbctab_ascii,true);
    public final static CompileContext UTF8 = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_UTF8, mbctab_utf8,false);
    public final static CompileContext SJIS = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_SJIS, mbctab_sjis,false);
    public final static CompileContext EUC = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_EUC, mbctab_euc,false);
}

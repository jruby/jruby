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

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Registers {
    public int allocated;
    public int num_regs;
    public int[] beg;
    public int[] end;

    public static final int RE_NREGS=10;

    public void init_regs(int nregs) {
        num_regs = nregs;

        if(nregs < RE_NREGS) {
            nregs = RE_NREGS;
        }

        if(allocated == 0) {
            beg = new int[nregs];
            end = new int[nregs];
            allocated = nregs;
        } else if(allocated < nregs) {
            int[] old = beg;
            beg = new int[nregs];
            System.arraycopy(old,0,beg,0,allocated);

            old = end;
            end = new int[nregs];
            System.arraycopy(old,0,end,0,allocated);
            
            allocated = nregs;
        }
        for(int i=0;i<nregs;i++) {
            beg[i] = end[i] = -1;
        }
    }

    public Registers copy() {
        Registers n = new Registers();
        n.allocated = n.num_regs = num_regs;
        n.beg = new int[num_regs];
        n.end = new int[num_regs];
        System.arraycopy(beg,0,n.beg,0,num_regs);
        System.arraycopy(end,0,n.end,0,num_regs);
        return n;
    }
}// Registers

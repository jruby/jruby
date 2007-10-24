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

import static org.rej.REJConstants.*;
import static org.rej.MBC.*;
import static org.rej.Helpers.*;
import static org.rej.Bytecodes.*;

class MatchEnvironment {
    private byte[] p;
    private int pix;
    private final int pend;
    private int optz;
    private final int num_regs;
    private final byte[] string;
    private final int string_start;
    private int string_pos;
    private final int string_end;
    private int pos;
    private int beg;
    private final int size;

    private final CompileContext ctx;

    /* Failure point stack.  Each place that can handle a failure further
       down the line pushes a failure point on this stack.  It consists of
       restart, regend, and reg_info for all registers corresponding to the
       subexpressions we're currently inside, plus the number of such
       registers, and, finally, two char *'s.  The first char * is where to
       resume scanning the pattern; the second one is where to resume
       scanning the strings.  If the latter is zero, the failure point is a
       ``dummy''; if a failure happens and the failure point is a dummy, it
       gets discarded and the next next one is tried.  */

    private int[] stackb;
    private int stackp;
    private int stacke;
    
    private boolean best_regs_set = false;
    private int num_failure_counts = 0;

    private final Pattern self;

    /* stack & working area for re_match() */
    private int[] regstart;
    private int[] regend;
    private int[] old_regstart;
    private int[] old_regend;

    private static class RegisterInfoType {
        public boolean is_active;
        public boolean matched_something;
    }

    private RegisterInfoType[] reg_info;
    private int[] best_regstart;
    private int[] best_regend;

    private Registers regs;

    public MatchEnvironment(Pattern p, byte[] string_arg, int string_start, int size, int pos, int beg, Registers regs) {
        this.size = size;
        this.beg = beg;
        this.p = p.buffer;
        this.pix = 0;
        this.pend = p.used;
        this.num_regs = p.re_nsub;
        this.string = string_arg;
        this.string_start = string_start;
        this.optz = (int)p.options;
        this.self = p;
        this.ctx = p.ctx;
        this.pos = pos;

        this.regs = regs;

        regstart = new int[num_regs];
        regend = new int[num_regs];
        old_regstart = new int[num_regs];
        old_regend = new int[num_regs];
        reg_info = new RegisterInfoType[num_regs];
        for(int x=0;x<reg_info.length;x++) {
            reg_info[x] = new RegisterInfoType();
        }
        best_regstart = new int[num_regs];
        best_regend = new int[num_regs];

        if(regs != null) {
            regs.init_regs(num_regs);
        }

        init_stack();
        init_registers();

        /* Set up pointers to ends of strings.
           Don't allow the second string to be empty unless both are empty.  */

        /* `p' scans through the pattern as `d' scans through the data. `string_end'
           is the end of the input string that `d' points within. `d' is
           advanced into the following input string whenever necessary, but
           this happens before fetching; therefore, at the beginning of the
           loop, `d' can be pointing at the end of a string, but it cannot
           equal string2.  */

        string_pos = string_start+pos; string_end = string_start+size;
    }

    private final boolean TRANSLATE_P() {
        return ((optz&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
    }

    private final void init_stack() {
        /* Initialize the stack. */

        int i = -1;
        synchronized(self) {
            i = self.poolIndex++;
        }
        if(i < self.pool.length) {
            stackb = self.pool[i];
        } else {
            stackb = new int[(num_regs*NUM_REG_ITEMS + NUM_NONREG_ITEMS)*NFAILURES];
        }

        stackp = 0;
        stacke = stackb.length;
    }

    private final void init_registers() {
        /* Initialize subexpression text positions to -1 to mark ones that no
           ( or ( and ) or ) has been seen for. Also set all registers to
           inactive and mark them as not having matched anything or ever
           failed. */
        for(int mcnt = 0; mcnt < num_regs; mcnt++) {
            regstart[mcnt] = regend[mcnt]
                = old_regstart[mcnt] = old_regend[mcnt]
                = best_regstart[mcnt] = best_regend[mcnt] = REG_UNSET_VALUE;
            reg_info[mcnt].is_active = false;
            reg_info[mcnt].matched_something = false;
        }
    }

    private final void POP_FAILURE_COUNT() {
        int ptr = stackb[--stackp];
        int count = stackb[--stackp];
        STORE_NUMBER(p, ptr, count);
    }

    private final void POP_FAILURE_POINT() {
        long temp;
        stackp -= NUM_NONREG_ITEMS;	/* Remove failure points (and flag). */
        temp = stackb[--stackp];	/* How many regs pushed.  */
        temp *= NUM_REG_ITEMS;	/* How much to take off the stack.  */
        stackp -= temp; 		/* Remove the register info.  */
        temp = stackb[--stackp];	/* How many counters pushed.  */
        while(temp-- > 0) {
            POP_FAILURE_COUNT();
        }
        num_failure_counts = 0;	/* Reset num_failure_counts.  */
    }

    private final void SET_REGS_MATCHED() {
        for(int this_reg = 0; this_reg < num_regs; this_reg++) {
            reg_info[this_reg].matched_something = reg_info[this_reg].is_active;
        }
    }

    private final void PUSH_FAILURE_POINT(int pattern_place, int string_place) {
        int last_used_reg, this_reg;

        /* Find out how many registers are active or have been matched.
           (Aside from register zero, which is only set at the end.) */
        for(last_used_reg = num_regs-1; last_used_reg > 0; last_used_reg--) {
            if(regstart[last_used_reg]!=REG_UNSET_VALUE) {
                break;
            }
        }
                        
        if(stacke - stackp <= (last_used_reg * NUM_REG_ITEMS + NUM_NONREG_ITEMS + 1)) {
            expandStack();
        }

        stackb[stackp++] = num_failure_counts;
        num_failure_counts = 0;

        /* Now push the info for each of those registers.  */
        for(this_reg = 1; this_reg <= last_used_reg; this_reg++) {
            stackb[stackp++] = regstart[this_reg];
            stackb[stackp++] = regend[this_reg];
        }

        /* Push how many registers we saved.  */
        stackb[stackp++] = last_used_reg;

        stackb[stackp++] = pattern_place;
        stackb[stackp++] = string_place;
        stackb[stackp++] = (int)optz; /* current option status */
        stackb[stackp++] = 0; /* non-greedy flag */
    }

    private final int duplicate() {
        int regno = p[pix++];   /* Get which register to match against */
        int string_pos2, string_end2;

        /* Check if there's corresponding group */
        if(regno >= num_regs) {
            return BREAK_FAIL1;
        }
        /* Check if corresponding group is still open */
        if(reg_info[regno].is_active) {
            return BREAK_FAIL1;
        }

        /* Where in input to try to start matching.  */
        string_pos2 = regstart[regno];
        if(string_pos2 == REG_UNSET_VALUE) {
            return BREAK_FAIL1;
        }

        /* Where to stop matching; if both the place to start and
           the place to stop matching are in the same string, then
           set to the place to stop, otherwise, for now have to use
           the end of the first string.  */

        string_end2 = regend[regno];
        if(string_end2 == REG_UNSET_VALUE) {
            return BREAK_FAIL1;
        }

        for(;;) {
            /* At end of register contents => success */
            if(string_pos2 == string_end2) {
                break;
            }

            /* If necessary, advance to next segment in data.  */
            if(string_pos == string_end) {return BREAK_FAIL1;}

            /* How many characters left in this segment to match.  */
            int mcnt = string_end - string_pos;

            /* Want how many consecutive characters we can match in
               one shot, so, if necessary, adjust the count.  */
            if(mcnt > string_end2 - string_pos2) {
                mcnt = string_end2 - string_pos2;
            }

            /* Compare that many; failure if mismatch, else move
               past them.  */
            if(((self.options & RE_OPTION_IGNORECASE) != 0) ? self.memcmp_translate(string, string_pos, string_pos2, mcnt)!=0 : memcmp(string, string_pos, string_pos2, mcnt)!=0) {
                return BREAK_FAIL1;
            }
            string_pos += mcnt;
            string_pos2 += mcnt;
        }
        return BREAK_NORMAL;
    }

    private static final boolean is_in_list_sbc(int cx, byte[] b, int bix) {
        int size = b[bix++]&0xFF;
        return cx/8 < size && ((b[bix + cx/8]&0xFF)&(1<<cx%8)) != 0;
    }
  
    private static final boolean is_in_list_mbc(int cx, byte[] b, int bix) {
        int size = b[bix++]&0xFF;
        bix+=size+2;
        size = EXTRACT_UNSIGNED(b,bix-2);
        if(size == 0) {
            return false;
        }
        int i,j;
        for(i=0,j=size;i<j;) {
            int k = (i+j)>>1;
            if(cx > EXTRACT_MBC(b,bix+k*8+4)) {
                i = k+1;
            } else {
                j = k;
            }
        }
        return i<size && EXTRACT_MBC(b,bix+i*8) <= cx;
    }        

    private final boolean is_in_list(int cx, byte[] b, int bix) {
        return is_in_list_sbc(cx, b, bix) || (ctx.current_mbctype!=0 ? is_in_list_mbc(cx, b, bix) : false);
    }

    private final int handle_fail() {
        /* A restart point is known.  Restart there and pop it. */
        int this_reg;

        /* If this failure point is from a dummy_failure_point, just
           skip it.  */
        if(stackb[stackp-4] == -1 || (best_regs_set && stackb[stackp-1] == NON_GREEDY)) {
            POP_FAILURE_POINT();
            return 0;
        }
        stackp--;		/* discard greedy flag */
        optz = stackb[--stackp];
        string_pos = stackb[--stackp];
        pix = stackb[--stackp];

        /* Restore register info.  */
        int last_used_reg = stackb[--stackp];

        /* Make the ones that weren't saved -1 or 0 again. */
        for(this_reg = num_regs - 1; this_reg > last_used_reg; this_reg--) {
            regend[this_reg] = REG_UNSET_VALUE;
            regstart[this_reg] = REG_UNSET_VALUE;
            reg_info[this_reg].is_active = false;
            reg_info[this_reg].matched_something = false;
        }

        /* And restore the rest from the stack.  */
        for( ; this_reg > 0; this_reg--) {
            regend[this_reg] = stackb[--stackp];
            regstart[this_reg] = stackb[--stackp];
        }
        int mcnt = stackb[--stackp];
        while(mcnt-->0) {
            int ptr = stackb[--stackp];
            int count = stackb[--stackp];
            STORE_NUMBER(p, ptr, count);
        }
        if(pix < pend) {
            int is_a_jump_n = 0;
            int failed_paren = 0;

            int p1 = pix;
            /* If failed to a backwards jump that's part of a repetition
               loop, need to pop this failure point and use the next one.  */
            switch(p[p1]) {
            case jump_n:
            case finalize_push_n:
                is_a_jump_n = 1;
            case maybe_finalize_jump:
            case finalize_jump:
            case finalize_push:
            case jump:
                p1++;
                mcnt = EXTRACT_NUMBER(p,p1);
                p1+=2;
                if(mcnt >= 0) {
                    break;	/* should be backward jump */
                }
                p1 += mcnt;
                if((is_a_jump_n!=0 && p[p1] == succeed_n) ||
                   (is_a_jump_n==0 && p[p1] == on_failure_jump)) {
                    if(failed_paren!=0) {
                        p1++;
                        mcnt = EXTRACT_NUMBER(p, p1);
                        p1+=2;

                        PUSH_FAILURE_POINT(p1+mcnt,string_pos);
                        stackb[stackp-1] = NON_GREEDY;
                    }
                    return 0;
                }
            default:
                /* do nothing */;
                return 1;
            }
        }
        return 1;
    }

    private final void fix_regs() {
        for(int mcnt = 0; mcnt < num_regs; mcnt++) {
            regstart[mcnt] = best_regstart[mcnt];
            regend[mcnt] = best_regend[mcnt];
        }
    }

    private final void fix_best_regs() {
        for(int mcnt = 1; mcnt < num_regs; mcnt++) {
            best_regstart[mcnt] = regstart[mcnt];
            best_regend[mcnt] = regend[mcnt];
        }
    }

    public final int restore_best_regs() {
        /* If not end of string, try backtracking.  Otherwise done.  */
        if((self.options&RE_OPTION_LONGEST)!=0 && string_pos != string_end) {
            if(best_regs_set) {/* non-greedy, no need to backtrack */
                /* Restore best match.  */
                string_pos = best_regend[0];
                fix_regs();
                return 0;
            }
            while(stackp != 0 && stackb[stackp-1] == NON_GREEDY) {
                if(best_regs_set) {/* non-greedy, no need to backtrack */
                    string_pos = best_regend[0];
                    fix_regs();
                    return 0;
                }
                POP_FAILURE_POINT();
            }
            if(stackp != 0) {
                /* More failure points to try.  */

                /* If exceeds best match so far, save it.  */
                if(!best_regs_set || (string_pos > best_regend[0])) {
                    best_regs_set = true;
                    best_regend[0] = string_pos;	/* Never use regstart[0].  */
                    fix_best_regs();
                }
                return 1;
            } /* If no failure points, don't restore garbage.  */
            else if(best_regs_set) {
                /* Restore best match.  */
                string_pos = best_regend[0];
                fix_regs();
            }
        }
        return 0;
    }

    public final void convert_regs(Registers regs) {
        /* If caller wants register contents data back, convert it 
           to indices.  */
        if(regs != null) {
            regs.beg[0] = pos;
            regs.end[0] = string_pos-string_start;
            for(int mcnt = 1; mcnt < num_regs; mcnt++) {
                if(regend[mcnt] == REG_UNSET_VALUE) {
                    regs.beg[mcnt] = -1;
                    regs.end[mcnt] = -1;
                    continue;
                }
                regs.beg[mcnt] = regstart[mcnt] - string_start;
                regs.end[mcnt] = regend[mcnt] - string_start;
            }
        }
    }
        
    public final int start_memory() {
        old_regstart[p[pix]] = regstart[p[pix]];
        regstart[p[pix]] = string_pos;
        reg_info[p[pix]].is_active = true;
        reg_info[p[pix]].matched_something = false;
        pix += 2;
        return CONTINUE_MAINLOOP;
    }

    public final int stop_memory() {
        old_regend[p[pix]] = regend[p[pix]];
        regend[p[pix]] = string_pos;
        reg_info[p[pix]].is_active = false;
        pix += 2;
        return CONTINUE_MAINLOOP;
    }

    public final int anychar() {
        if(string_pos == string_end) {return BREAK_FAIL1;}
        if(ismbchar(string[string_pos],ctx)) {
            if(string_pos + mbclen(string[string_pos],ctx) > string_end) {
                return BREAK_FAIL1;
            }
            SET_REGS_MATCHED();
            string_pos += mbclen(string[string_pos],ctx);
            return BREAK_NORMAL;
        }
        if((optz&RE_OPTION_MULTILINE)==0
           && (TRANSLATE_P() ? ctx.translate[string[string_pos]] : string[string_pos]) == '\n') {
            return 1;
        }
        SET_REGS_MATCHED();
        string_pos++;
        return 0;
    }

    public final int charset() {
        boolean not;	    /* Nonzero for charset_not.  */
        boolean part = false;	    /* true if matched part of mbc */
        int dsave = string_pos + 1;
        int cc;
                    
        if(string_pos == string_end) {return BREAK_FAIL1;}
                        
        char c = (char)(string[string_pos++]&0xFF);
        if(ismbchar(c,ctx)) {
            if(string_pos + mbclen(c,ctx) - 1 <= string_end) {
                cc = c;
                c = self.MBC2WC(c, string, string_pos);
                not = is_in_list_mbc(c, p, pix);
                if(!not) {
                    part = not = is_in_list_sbc(cc, p, pix);
                }
            } else {
                not = is_in_list(c, p, pix);
            }
        } else {
            if(TRANSLATE_P()) {
                c = ctx.translate[c];
            }
            not = is_in_list(c, p, pix);
        }
        if(p[pix-1] == charset_not) {
            not = !not;
        }
            
        if(!not) {return BREAK_FAIL1;}
            
        pix += 1 + (char)(p[pix]&0xFF) + 2 + EXTRACT_UNSIGNED(p, pix + 1 + (char)(p[pix]&0xFF))*8;
        SET_REGS_MATCHED();
                    
        if(part) {
            string_pos = dsave;
        }
        return CONTINUE_MAINLOOP;
    }

    public final int anychar_repeat() {
        final boolean notMultiline = (optz&RE_OPTION_MULTILINE)==0;
        final int posBefore = string_pos;
        for (;;) {
            PUSH_FAILURE_POINT(pix,string_pos);
            if(string_pos == string_end) {
                if(string_pos != posBefore) {
                    SET_REGS_MATCHED();
                }
                return BREAK_FAIL1;
            }
            if(ismbchar(string[string_pos],ctx)) {
                if(string_pos + mbclen(string[string_pos],ctx) > string_end) {
                    return BREAK_FAIL1;
                }
                string_pos += mbclen(string[string_pos],ctx);
                continue;
            }
            if(notMultiline && string[string_pos] == '\n') {
                if(string_pos != posBefore) {
                    SET_REGS_MATCHED();
                }
                return BREAK_FAIL1;
            }
            string_pos++;
        }
    }

    public final int maybe_finalize_jump() {
        int mcnt = EXTRACT_AND_INCR_PIX();
        int p1 = pix;

        /* Compare the beginning of the repeat with what in the
           pattern follows its end. If we can establish that there
           is nothing that they would both match, i.e., that we
           would have to backtrack because of (as in, e.g., `a*a')
           then we can change to finalize_jump, because we'll
           never have to backtrack.

           This is not true in the case of alternatives: in
           `(a|ab)*' we do need to backtrack to the `ab' alternative
           (e.g., if the string was `ab').  But instead of trying to
           detect that here, the alternative has put on a dummy
           failure point which is what we will end up popping.  */

        /* Skip over open/close-group commands.  */
        while(p1 + 2 < pend) {
            if(p[p1] == stop_memory ||
               p[p1] == start_memory) {
                p1 += 3;	/* Skip over args, too.  */
            } else if(p[p1] == stop_paren) {
                p1 += 1;
            } else {
                break;
            }
        }
        if(p1 == pend) {
            p[pix-3] = finalize_jump;
        } else if(p[p1] == exactn || p[p1] == endline) {
            char c = p[p1] == endline ? '\n' : (char)(p[p1+2]&0xFF);
            int p2 = pix+mcnt;
            /* p2[0] ... p2[2] are an on_failure_jump.
               Examine what follows that.  */
            if(p[p2+3] == exactn && (p[p2+5]&0xFF) != c) {
                p[pix-3] = finalize_jump;
            } else if(p[p2+3] == charset ||
                      p[p2+3] == charset_not) {
                boolean not;
                if(ismbchar(c,ctx)) {
                    int pp = p1+3;
                    c = self.MBC2WC(c, p, pp);
                }
                /* `is_in_list()' is TRUE if c would match */
                /* That means it is not safe to finalize.  */
                not = is_in_list(c, p, p2 + 4);
                if(p[p2+3] == charset_not) {
                    not = !not;
                }
                if(!not) {
                    p[pix-3] = finalize_jump;
                }
            }
        }
        pix -= 2;		/* Point at relative address again.  */
        if(p[pix-1] != finalize_jump) {
            p[pix-1] = jump;	
            mcnt = EXTRACT_AND_INCR_PIX();
            if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
                return 1;
            }
            pix += mcnt;
            return 2;
        } 
        return 0;
    }

    public final int push_dummy_failure() {
        /* See comments just above at `dummy_failure_jump' about the
           two zeroes.  */
        int p1 = pix;
        /* Skip over open/close-group commands.  */
        while(p1 + 2 < pend) {
            if(p[p1] == stop_memory ||
               p[p1] == start_memory) {
                p1 += 3;	/* Skip over args, too.  */
            } else if(p[p1] == stop_paren) {
                p1 += 1;
            } else {
                break;
            }
        }
        if(p1 < pend && p[p1] == jump) {
            p[pix-1] = unused;
        } else {
            PUSH_FAILURE_POINT(-1,0);
        }
        return CONTINUE_MAINLOOP;
    }

    private final void expandStack() {
        int[] stackx;
        int xlen = stacke;
        stackx = new int[2*xlen];
        System.arraycopy(stackb,0,stackx,0,xlen);
        stackb = stackx;
        stacke = 2*xlen;
    }

    private final void expandStackIfNeeded() {
        if(stacke - stackp <= NUM_COUNT_ITEMS) {
            expandStack();
        }
    }

    public final int succeed_n() {
        int mcnt = EXTRACT_NUMBER(p, pix + 2);
        /* Originally, this is how many times we HAVE to succeed.  */
        if(mcnt != 0) {
            mcnt--;
            pix += 2;

            char c = (char)EXTRACT_NUMBER(p, pix);
            expandStackIfNeeded();
            stackb[stackp++] = c;
            stackb[stackp++] = pix;
            num_failure_counts++;
                
            STORE_NUMBER(p, pix, mcnt);
            pix+=2;
                
            PUSH_FAILURE_POINT(-1,0);
        } else  {
            mcnt = EXTRACT_AND_INCR_PIX();
            PUSH_FAILURE_POINT(pix+mcnt,string_pos);
        }
        return CONTINUE_MAINLOOP;
    }

    public final int jump_n() {
        int mcnt = EXTRACT_NUMBER(p, pix + 2);
        /* Originally, this is how many times we CAN jump.  */
        if(mcnt!=0) {
            mcnt--;

            char c = (char)EXTRACT_NUMBER(p, pix+2);
            expandStackIfNeeded();
            stackb[stackp++] = c;
            stackb[stackp++] = pix+2;
            num_failure_counts++;
            STORE_NUMBER(p, pix + 2, mcnt);
            mcnt = EXTRACT_AND_INCR_PIX();
            if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
                return BREAK_FAIL1;
            }
            pix += mcnt;
            return CONTINUE_MAINLOOP;
        }
        /* If don't have to jump any more, skip over the rest of command.  */
        else {
            pix += 4;
        }
        return CONTINUE_MAINLOOP;
    }

    public final int exactn() {
        /* Match the next few pattern characters exactly.
           mcnt is how many characters to match.  */
        int mcnt = p[pix++] & 0xff;
        //            System.err.println("matching " + mcnt + " exact characters");
        /* This is written out as an if-else so we don't waste time
           testing `translate' inside the loop.  */
        if(TRANSLATE_P()) {
            do {
                if(string_pos == string_end) {return BREAK_FAIL1;}
                if(p[pix] == (byte)0xff) {
                    pix++;  
                    if(--mcnt==0
                       || string_pos == string_end
                       || string[string_pos++] != p[pix++]) {
                        return BREAK_FAIL1;
                    }
                    continue;
                }
                char c = (char)(string[string_pos++]&0xFF);
                if(ismbchar(c,ctx)) {
                    int n;
                    if(c != (char)(p[pix++]&0xFF)) {
                        return BREAK_FAIL1;
                    }
                    for(n = mbclen(c,ctx) - 1; n > 0; n--) {
                        if(--mcnt==0
                           || string_pos == string_end
                           || string[string_pos++] != p[pix++]) {
                            return BREAK_FAIL1;
                        }
                    }
                    continue;
                }
                /* compiled code translation needed for ruby */
                if(ctx.translate[c] != ctx.translate[p[pix++]&0xFF]) {
                    return BREAK_FAIL1;
                }
            } while(--mcnt > 0);
        } else {
            do {
                if(string_pos == string_end) {return BREAK_FAIL1;}
                if(p[pix] == (byte)0xff) {
                    pix++; mcnt--;
                }
                if(string[string_pos++] != p[pix++]) {
                    return BREAK_FAIL1;
                }
            } while(--mcnt > 0);
        }
        SET_REGS_MATCHED();
        return CONTINUE_MAINLOOP;
    }

    private final int start_nowidth() {
        PUSH_FAILURE_POINT(-1,string_pos);
        if(stackp > RE_DUP_MAX) {
            return RETURN_M2;
        }
        int mcnt = EXTRACT_AND_INCR_PIX();
        STORE_NUMBER(p, pix+mcnt, stackp);
        return CONTINUE_MAINLOOP;
    }

    private final int stop_nowidth() {
        int mcnt = EXTRACT_AND_INCR_PIX();
        stackp = mcnt;
        string_pos = stackb[stackp-3];
        POP_FAILURE_POINT();
        return CONTINUE_MAINLOOP;
    }

    private final int stop_backtrack() {
        stackp = EXTRACT_AND_INCR_PIX();
        POP_FAILURE_POINT();
        return CONTINUE_MAINLOOP;
    }

    private final int pop_and_fail() {
        int mcnt = EXTRACT_NUMBER(p, pix+1);
        stackp = mcnt;
        POP_FAILURE_POINT();
        return BREAK_FAIL1;
    }

    private final int begline() {
        if(size == 0 || string_pos == string_start) {
            return CONTINUE_MAINLOOP;
        }
        if(string[string_pos-1] == '\n' && string_pos != string_end) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int endline() {
        if(string_pos == string_end) {
            return CONTINUE_MAINLOOP;
        } else if(string[string_pos] == '\n') {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int begbuf() {
        if(string_pos==string_start) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int endbuf() {
        if(string_pos == string_end) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int endbuf2() {
        if(string_pos == string_end) {
            return CONTINUE_MAINLOOP;
        }
        /* .. or newline just before the end of the data. */
        if(string[string_pos] == '\n' && string_pos+1 == string_end) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int begpos() {
        if(string_pos == beg) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int on_failure_jump() {
        int mcnt = EXTRACT_AND_INCR_PIX();
        PUSH_FAILURE_POINT(pix+mcnt,string_pos);
        return CONTINUE_MAINLOOP;
    }

    private final int finalize_jump() {
        if(stackp > 2 && stackb[stackp-3] == string_pos) {
            pix = stackb[stackp-4];
            POP_FAILURE_POINT();
            return CONTINUE_MAINLOOP;
        }
        POP_FAILURE_POINT();
        return BREAK_NORMAL;
    }

    private final int casefold_on() {
        optz |= RE_OPTION_IGNORECASE;
        return CONTINUE_MAINLOOP;
    }

    private final int casefold_off() {
        optz &= ~RE_OPTION_IGNORECASE;
        return CONTINUE_MAINLOOP;
    }

    private final int option_set() {
        optz = p[pix++];
        return CONTINUE_MAINLOOP;
    }

    public final int run() {
        // This loops over pattern commands.  It exits by returning from the
        // function if match is complete, or it drops through if match fails
        // at this starting point in the input data.  
        boolean gotoRestoreBestRegs = false;
        do {
            mainLoop: for(;;) {
                fail1: do {
                    // End of pattern means we might have succeeded.  
                    if(pix == pend || gotoRestoreBestRegs) {
                        if(!gotoRestoreBestRegs) {
                            if(restore_best_regs()==1) {
                                break fail1;
                            }
                        } else {
                            gotoRestoreBestRegs = false;
                        }

                        convert_regs(regs);

                        self.uninit_stack();
                        return (string_pos-string_start) - pos;
                    }

                    int var = BREAK_NORMAL;


                    //System.err.println("--executing " + (int)p[pix] + " at " + pix);
                    //System.err.println("-- -- for d: " + string_pos + " and string_end: " + string_end);
                    switch(p[pix++]) {
                        /* ( [or `(', as appropriate] is represented by start_memory,
                           ) by stop_memory.  Both of those commands are followed by
                           a register number in the next byte.  The text matched
                           within the ( and ) is recorded under that number.  */
                    case start_memory:
                        var = start_memory();
                        break;
                    case stop_memory:
                        var = stop_memory();
                        break;
                    case start_paren:
                    case stop_paren:
                        break;
                        /* \<digit> has been turned into a `duplicate' command which is
                           followed by the numeric value of <digit> as the register number.  */
                    case duplicate:
                        var = duplicate();
                        break;
                    case start_nowidth:
                        var = start_nowidth();
                        break;
                    case stop_nowidth:
                        var = stop_nowidth();
                        break;
                    case stop_backtrack:
                        var = stop_backtrack();
                        break;
                    case pop_and_fail:
                        var = pop_and_fail();
                        break;
                    case anychar:
                        var = anychar();
                        break;
                    case anychar_repeat:
                        var = anychar_repeat();
                        break;
                    case charset:
                    case charset_not: 
                        var = charset();
                        break;
                    case begline:
                        var = begline();
                        break;
                    case endline:
                        var = endline();
                        break;
                        /* Match at the very beginning of the string. */
                    case begbuf:
                        var = begbuf();
                        break;
                        /* Match at the very end of the data. */
                    case endbuf:
                        var = endbuf();
                        break;
                        /* Match at the very end of the data. */
                    case endbuf2:
                        var = endbuf2();
                        break;
                        /* `or' constructs are handled by starting each alternative with
                           an on_failure_jump that points to the start of the next
                           alternative.  Each alternative except the last ends with a
                           jump to the joining point.  (Actually, each jump except for
                           the last one really jumps to the following jump, because
                           tensioning the jumps is a hassle.)  */
                    
                        /* The start of a stupid repeat has an on_failure_jump that points
                           past the end of the repeat text. This makes a failure point so 
                           that on failure to match a repetition, matching restarts past
                           as many repetitions have been found with no way to fail and
                           look for another one.  */
                    
                        /* A smart repeat is similar but loops back to the on_failure_jump
                           so that each repetition makes another failure point.  */
                    
                        /* Match at the starting position. */
                    case begpos:
                        var = begpos();
                        break;
                    case on_failure_jump:
                        //                on_failure:
                        var = on_failure_jump();
                        break;

                        /* The end of a smart repeat has a maybe_finalize_jump back.
                           Change it either to a finalize_jump or an ordinary jump.  */
                    case maybe_finalize_jump:
                        switch(maybe_finalize_jump()) {
                        case 1:
                            break fail1;
                        case 2:
                            continue mainLoop;
                        }

                        /* Note fall through.  */

                        /* The end of a stupid repeat has a finalize_jump back to the
                           start, where another failure point will be made which will
                           point to after all the repetitions found so far.  */
                    
                        /* Take off failure points put on by matching on_failure_jump 
                           because didn't fail.  Also remove the register information
                           put on by the on_failure_jump.  */

                    case finalize_jump:
                        if(finalize_jump() == CONTINUE_MAINLOOP) {
                            continue mainLoop;
                        }

                        /* Note fall through.  */

                        /* We need this opcode so we can detect where alternatives end
                           in `group_match_null_string_p' et al.  */
                    case jump_past_alt:
                        /* fall through */
                        /* Jump without taking off any failure points.  */
                    case jump:
                        //      nofinalize:
                        var = jump();
                        break;
                    case dummy_failure_jump:
                        /* Normally, the on_failure_jump pushes a failure point, which
                           then gets popped at finalize_jump.  We will end up at
                           finalize_jump, also, and with a pattern of, say, `a+', we
                           are skipping over the on_failure_jump, so we have to push
                           something meaningless for finalize_jump to pop.  */
                        var = dummy_failure_jump();
                        break;

                        /* At the end of an alternative, we need to push a dummy failure
                           point in case we are followed by a `finalize_jump', because
                           we don't want the failure point for the alternative to be
                           popped.  For example, matching `(a|ab)*' against `aab'
                           requires that we match the `ab' alternative.  */
                    case push_dummy_failure:
                        var = push_dummy_failure();
                        break;
                        /* Have to succeed matching what follows at least n times.  Then
                           just handle like an on_failure_jump.  */
                    case succeed_n: 
                        var = succeed_n();
                        break;
                    case jump_n:
                        var = jump_n();
                        break;
                    case set_number_at:
                        var = set_number_at();
                        break;
                    case try_next:
                        var = try_next();
                        break;
                    case finalize_push:
                        var = finalize_push();
                        break;
                    case finalize_push_n:
                        var = finalize_push_n();
                        break;
                        /* Ignore these.  Used to ignore the n of succeed_n's which
                           currently have n == 0.  */
                    case unused:
                        var = CONTINUE_MAINLOOP;
                        break;
                    case casefold_on:
                        var = casefold_on();
                        break;
                    case casefold_off:
                        var = casefold_off();
                        break;
                    case option_set:
                        var = option_set();
                        break;
                    case wordbound:
                        var = wordbound();
                        break;
                    case notwordbound:
                        var = notwordbound();
                        break;
                    case wordbeg:
                        var = wordbeg();
                        break;
                    case wordend:
                        var = wordend();
                        break;
                    case wordchar:
                        var = wordchar();
                        break;
                    case notwordchar:
                        var = notwordchar();
                        break;
                    case exactn:
                        var = exactn();
                        break;
                    }


                    switch(var) {
                    case CONTINUE_MAINLOOP:
                        continue mainLoop;
                    case BREAK_FAIL1:
                        break fail1;
                    case RETURN_M2:
                        self.uninit_stack();
                        return -2;
                    }
                    continue mainLoop;
                } while(false);

                if(fail() == -1) {
                    break mainLoop;
                } else {
                    continue mainLoop;
                }
            }        

            if(best_regs_set) {
                gotoRestoreBestRegs=true;
                string_pos = best_regend[0];
                fix_regs();
            }
        } while(gotoRestoreBestRegs);

        self.uninit_stack();
        return -1;
    }

    private final int jump() {
        int mcnt = EXTRACT_AND_INCR_PIX();
        if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
            return BREAK_FAIL1;
        }
        pix += mcnt;
        return CONTINUE_MAINLOOP;
    }

    private final int EXTRACT_AND_INCR_PIX() {
        int val = EXTRACT_NUMBER(p, pix);
        pix += 2;
        return val;
    }

    private final boolean stackOutOfRange() {
        return stackp > 2 && stackb[stackp-3] == string_pos;
    }

    private final int dummy_failure_jump() {
        PUSH_FAILURE_POINT(-1,0);
        int mcnt = EXTRACT_AND_INCR_PIX();
        if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
            return BREAK_FAIL1;
        }
        pix += mcnt;
        return CONTINUE_MAINLOOP;
    }

    private final int try_next() {
        int mcnt = EXTRACT_AND_INCR_PIX();
        if(pix + mcnt < pend) {
            PUSH_FAILURE_POINT(pix,string_pos);
            stackb[stackp-1] = NON_GREEDY;
        }
        pix += mcnt;
        return CONTINUE_MAINLOOP;
    }

    private final int set_number_at() {
        int mcnt = EXTRACT_AND_INCR_PIX();
        int p1 = pix + mcnt;
        mcnt = EXTRACT_AND_INCR_PIX();
        STORE_NUMBER(p, p1, mcnt);
        return CONTINUE_MAINLOOP;
    }

    private final int finalize_push() {
        POP_FAILURE_POINT();
        int mcnt = EXTRACT_AND_INCR_PIX();
        if(mcnt < 0 && stackOutOfRange()) { /* avoid infinite loop */
            return BREAK_FAIL1;
        }
        PUSH_FAILURE_POINT(pix+mcnt,string_pos);
        stackb[stackp-1] = NON_GREEDY;
        return CONTINUE_MAINLOOP;
    }

    private final int finalize_push_n() {
        int mcnt = EXTRACT_NUMBER(p, pix + 2); 
        /* Originally, this is how many times we CAN jump.  */
        if(mcnt>0) {
            int posx, i;
            mcnt--;
            STORE_NUMBER(p, pix + 2, mcnt);
            posx = EXTRACT_NUMBER(p, pix);
            i = EXTRACT_NUMBER(p,pix+posx+5);
            if(i > 0) {
                mcnt = EXTRACT_NUMBER(p, pix);
                pix += 2;
                if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
                    return BREAK_FAIL1;
                }
                pix += mcnt;
                return CONTINUE_MAINLOOP;
            }
            POP_FAILURE_POINT();
            mcnt = EXTRACT_AND_INCR_PIX();
            PUSH_FAILURE_POINT(pix+mcnt,string_pos);
            stackb[stackp-1] = NON_GREEDY;
            pix += 2;		/* skip n */
        }
        /* If don't have to push any more, skip over the rest of command.  */
        else {
            pix += 4;
        }
        return CONTINUE_MAINLOOP;
    }

    private final int wordbound() {
        if(string_pos == 0) {
            if(string_pos == string_end) {return BREAK_FAIL1;}
            if(IS_A_LETTER(string,string_pos,string_end)) {
                return CONTINUE_MAINLOOP;
            } else {
                return BREAK_FAIL1;
            }
        }
        if(string_pos == string_end) {
            if(PREV_IS_A_LETTER(string,string_pos,string_end)) {
                return CONTINUE_MAINLOOP;
            } else {
                return BREAK_FAIL1;
            }
        }
        if(PREV_IS_A_LETTER(string,string_pos,string_end) != IS_A_LETTER(string,string_pos,string_end)) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int notwordbound() {
        if(string_pos==0) {
            if(IS_A_LETTER(string, string_pos, string_end)) {
                return BREAK_FAIL1;
            } else {
                return CONTINUE_MAINLOOP;
            }
        }
        if(string_pos == string_end) {
            if(PREV_IS_A_LETTER(string, string_pos, string_end)) {
                return BREAK_FAIL1;
            } else {
                return CONTINUE_MAINLOOP;
            }
        }
        if(PREV_IS_A_LETTER(string, string_pos, string_end) != IS_A_LETTER(string, string_pos, string_end)) {
            return BREAK_FAIL1;
        }
        return CONTINUE_MAINLOOP;
    }

    private final int wordbeg() {
        if(IS_A_LETTER(string, string_pos, string_end) && (string_pos==0 || !PREV_IS_A_LETTER(string,string_pos,string_end))) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int wordend() {
        if(string_pos!=0 && PREV_IS_A_LETTER(string, string_pos, string_end)
           && (!IS_A_LETTER(string, string_pos, string_end) || string_pos == string_end)) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_FAIL1;
    }

    private final int wordchar() {
        if(string_pos == string_end) {return BREAK_FAIL1;}
        if(!IS_A_LETTER(string,string_pos,string_end)) {
            return BREAK_FAIL1;
        }
        if(ismbchar(string[string_pos],ctx) && string_pos + mbclen(string[string_pos],ctx) - 1 < string_end) {
            string_pos += mbclen(string[string_pos],ctx) - 1;
        }
        string_pos++;
        SET_REGS_MATCHED();
        return CONTINUE_MAINLOOP;
    }
        
    private final int notwordchar() {
        if(string_pos == string_end) {return BREAK_FAIL1;}
        if(IS_A_LETTER(string, string_pos, string_end)) {
            return BREAK_FAIL1;
        }
        if(ismbchar(string[string_pos],ctx) && string_pos + mbclen(string[string_pos],ctx) - 1 < string_end) {
            string_pos += mbclen(string[string_pos],ctx) - 1;
        }
        string_pos++;
        SET_REGS_MATCHED();
        return CONTINUE_MAINLOOP;
    }

    private final boolean IS_A_LETTER(byte[] d, int dix, int dend) {
        return re_syntax_table[d[dix]&0xFF] == Sword ||
            (ctx.current_mbctype != 0 ? 
             (ctx.re_mbctab[d[dix]&0xFF] != 0 && (d[dix+mbclen(d[dix],ctx)]&0xFF)<=dend):
             re_syntax_table[d[dix]&0xFF] == Sword2);
    }

    private final boolean PREV_IS_A_LETTER(byte[] d, int dix, int dend) {
        return ((ctx.current_mbctype == MBCTYPE_SJIS)?
                IS_A_LETTER(d,dix-(((dix-1)!=0&&ismbchar(d[dix-2],ctx))?2:1),dend):
                ((ctx.current_mbctype!=0 && ((d[dix-1]&0xFF) >= 0x80)) ||
                 IS_A_LETTER(d,dix-1,dend)));
    }

    private final int fail() {
        //fail:
        do {
            if(stackp != 0) {
                if(handle_fail() == 1) {
                    return 0;
                }
            } else {
                return -1; /* Matching at this starting point really fails.  */
            }
        } while(true);
    }

    private final static int BREAK_NORMAL = 0;
    private final static int BREAK_FAIL1 = 1;
    private final static int CONTINUE_MAINLOOP = 2;
    private final static int RETURN_M2 = 3;
}

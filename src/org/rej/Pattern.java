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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.rej.REJConstants.*;
import static org.rej.MBC.*;
import static org.rej.Bytecodes.*;
import static org.rej.CompileContext.*;
import static org.rej.Helpers.*;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @mri re_pattern_buffer
 */
public class Pattern {
    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(0),ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, int begin, int length) throws PatternSyntaxException {
        return compile(pattern,begin,length,emptyPattern(0),ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(0),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, int flags) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(flags),ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, int flags, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(flags),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, Pattern bufp) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,bufp,ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, Pattern bufp, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,bufp,ctx);
    }

    private CompileContext ctx;

    private final int WC2MBC1ST(long c) {
        if(ctx.current_mbctype != MBCTYPE_UTF8) {
            return (int)((c<0x100) ? c : ((c>>8)&0xff));
        } else {
            return (int)utf8_firstbyte(c);
        }
    }

    private void compile(byte[] pattern, int start, int length, CompileContext ctx) throws PatternSyntaxException {
        this.ctx = ctx;
        CompilationEnvironment w = new CompilationEnvironment();
        w.ctx = ctx;
        w.b = buffer;
        w.bix = 0;
        w.p = pattern;
        w.pix = start;
        w.pend = start+length;
        w.c1 = 0;
        w.optz = options;
        w.self = this;

        fastmap_accurate = 0;
        must = -1;
        must_skip = null;

        if(allocated == 0) {
            allocated = INIT_BUF_SIZE;
            /* EXTEND_BUFFER loses when allocated is 0.  */
            buffer = new byte[INIT_BUF_SIZE];
            w.b = buffer;
        }

        mainParse: while(w.pix != w.pend) {
            w.PATFETCH();

            mainSwitch: do {
                switch(w.c) {
                case '$':
                    w.dollar();
                    break;

                case '^':
                    w.caret();
                    break;

                case '+':
                case '?':
                case '*':
                    w.prepareRepeat();

                    /* Star, etc. applied to an empty pattern is equivalent
                       to an empty pattern.  */
                    if(w.laststart==-1) {
                        break;
                    }
                    
                    if(w.greedy && w.many_times_ok && w.b[w.laststart] == anychar && w.bix-w.laststart <= 2) {
                        if(w.b[w.bix - 1] == stop_paren) {
                            w.bix--;
                        }
                        if(w.zero_times_ok) {
                            w.b[w.laststart] = anychar_repeat;
                        } else {
                            w.BUFPUSH(anychar_repeat);
                        }
                        break;
                    }

                    w.mainRepeat();
                    break;

                case '.':
                    w.dot();
                    break;

                case '[':
                    w.prepareCharset();
                    w.main_charset();
                    w.compact_charset();
                    break;

                case '(':
                    w.group_start();

                    if(w.c == '#') {
                        if(w.push_option!=0) {
                            w.BUFPUSH(option_set);
                            w.BUFPUSH((byte)w.optz);
                        }
                        if(w.casefold!=0) {
                            if((w.optz & RE_OPTION_IGNORECASE) != 0) {
                                w.BUFPUSH(casefold_on);
                            } else {
                                w.BUFPUSH(casefold_off);
                            }
                        }
                        break;
                    }

                    w.group_start_end();
                    break;
                case ')':
                    w.group_end();
                    break;
                case '|':
                    w.alt();
                    break;
                case '{':
                unfetch_interval: do {
                    w.start_bounded_repeat();
                    
                    if(w.lower_bound < 0 || w.c != '}') {
                        break unfetch_interval;
                    }
                    switch(w.continue_bounded_repeat()) {
                    case 0:
                        break;
                    case 1:
                        continue mainSwitch;
                    case 2:
                        break mainSwitch;
                    }
                    w.bounded_nontrivial();
                    break mainSwitch;
                } while(false);
                w.unfetch_interval();
                break mainSwitch;

                case '\\':
                    w.escape();
                    break mainSwitch;
                case '#':
                    if((w.optz & RE_OPTION_EXTENDED) != 0) {
                        while(w.pix != w.pend) {
                            w.PATFETCH();
                            if(w.c == '\n') {
                                break;
                            }
                        }
                        break mainSwitch;
                    }
                    w.gotoNormalChar = true;
                    break mainSwitch;

                case ' ':
                case '\t':
                case '\f':
                case '\r':
                case '\n':
                    if((w.optz & RE_OPTION_EXTENDED) != 0) {
                        break mainSwitch;
                    }
                default:
                    if(w.c == ']') {
                        re_warning("regexp has `]' without escape");
                    } else if(w.c == '}') {
                        re_warning("regexp has `}' without escape");
                    }
                    w.gotoNormalChar = true;
                }
            } while(w.gotoRepeat);

            w.handle_num_or_normal();
        }

        w.finalize_compilation();
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, int start, int length, Pattern bufp) throws PatternSyntaxException {
        return compile(pattern,start,length,bufp,ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile_s(byte[] pattern, int start, int length, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,start,length,emptyPattern(0),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(byte[] pattern, int start, int length, Pattern bufp, CompileContext ctx) throws PatternSyntaxException {
        bufp.compile(pattern,start,length,ctx);
        return bufp;
    }

    final char MBC2WC(char c, byte[] p, int pix) {
        if(ctx.current_mbctype == MBCTYPE_UTF8) {
            int n = mbclen(c,ctx) - 1;
            c &= (1<<(6-n)) - 1;
            while(n-- > 0) {
                c = (char)(c << 6 | ((p[pix++]&0xFF) & ((1<<6)-1)));
            }
        } else {
            c <<= 8;
            c |= (char)(p[pix]&0xFF);
        }
        return c;
    }

    private List warnings = new ArrayList();

    void re_warning(String msg) {
        warnings.add(msg);
    }

    public List getWarnings() {
        return warnings;
    }

    public void clearWarnings() {
        warnings.clear();
    }
    
    static void err(String msg) throws PatternSyntaxException {
        throw new PatternSyntaxException(msg);
    }

    private static Pattern emptyPattern(int flags) {
        Pattern p = new Pattern();
        p.options = flags;
        return p;
    }

    private Pattern() {}
    public Pattern(byte[] b, int all, byte[] fmap, int flags) {
        buffer = b;
        allocated = all;
        fastmap = fmap;
        options = flags;
    }

    int[][] pool = new int[4][];
    int poolIndex = 0;

    byte[] buffer;	  /* Space holding the compiled pattern commands.  */
    int allocated;	  /* Size of space that `buffer' points to. */
    int used;		  /* Length of portion of buffer actually occupied  */
    byte[] fastmap;	  /* Pointer to fastmap, if any, or nul if none.  */
                              /* re_search uses the fastmap, if there is one,
                                 to skip over totally implausible characters.  */
    int must;         /* Pointer to exact pattern which strings should have
			                     to be matched.  */
    int[] must_skip;  /* Pointer to exact pattern skip table for bm_search */
    public long options;  	  /* Flags for options such as extended_pattern. */
    int re_nsub;	  /* Number of subexpressions found by the compiler. */
    byte fastmap_accurate;
			                  /* Set to zero when a new pattern is stored,
			                     set to one when the fastmap is updated from it.  */
    byte can_be_null; /* Set to one by compiling fastmap
			                     if this pattern might match the null string.
			                     It does not necessarily match the null string
                                 in that case, but if this is zero, it cannot.
                                 2 as value means can match null string
                                 but at end of range or before a character
                                 listed in the fastmap.  */

    
    public final boolean MAY_TRANSLATE() {
        return ((options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0 && ctx.translate!=null);
    }


    /**
     * @mri re_search
     */
    public int search(byte[] string, final int string_start, int size, int startpos, int range, Registers regs) {
        int val=-1, anchor = 0, initpos = startpos;
        boolean doBegbuf = false;
        int pix;
        char c;
        /* Check for out-of-range starting position.  */
        if(startpos < 0 || startpos > size) {
            return -1;
        }

        /* Update the fastmap now if not correct already.  */
        synchronized(this) {
            if(fastmap!=null && fastmap_accurate==0) {
                compile_fastmap();
                /*            System.err.println("fastmap: ");
                              for(int i=0;i<fastmap.length; i++) {
                              if((i % 30) == 0) {
                              System.err.println();
                              }
                              System.err.print("" + fastmap[i] + " ");
                              }
                              System.err.println();*/
            }
        }

        /* If the search isn't to be a backwards one, don't waste time in a
           search for a pattern that must be anchored.  */
        if(used > 0) {
            switch(buffer[0]) {
            case begbuf:
                doBegbuf = true;
                break;
            case begline:
                anchor = 1;
                break;
            case begpos:
                //System.err.println("doing match1");
                val = match(string, string_start, size, startpos, regs);
                if (val >= 0) {
                    return startpos;
                }
                return val;
            default:
                break;
            }
        }
        begbuf_match: do {
            if(doBegbuf) {
                if(range > 0) {
                    if(startpos > 0) {
                        return -1;
                    } else {
                        //System.err.println("doing match2");
                        val = match(string, string_start, size, 0, regs);
                        if(val >= 0) {
                            return 0;
                        }
                        return val;
                    }
                }
                doBegbuf = false;
            }

            if((options&RE_OPTIMIZE_ANCHOR)!=0) {
                if((options&RE_OPTION_MULTILINE)!=0 && range > 0) {
                    doBegbuf = true;
                    continue begbuf_match;
                }
                anchor = 1;
            }

            if(must != -1) {
                int len = buffer[must];
                int pos=-1, pbeg, pend;

                pbeg = startpos;
                pend = startpos + range;
                if(pbeg > pend) {		/* swap pbeg,pend */
                    pos = pend; 
                    pend = pbeg; 
                    pbeg = pos;
                }
                pend = size;
                if((options&RE_OPTIMIZE_NO_BM) != 0) {
                    //System.err.println("doing slow_search");
                    pos = slow_search(buffer, must+1, len, string, string_start+pbeg, pend-pbeg, MAY_TRANSLATE()?ctx.translate:null);
                    //System.err.println("slow_search=" + pos);
                } else {
                    //System.err.println("doing bm_search (" + (must+1) + "," + len + "," + pbeg + "," +(pend-pbeg)+")");
                    pos = bm_search(buffer, must+1, len, string, string_start+pbeg, pend-pbeg, must_skip, MAY_TRANSLATE()?ctx.translate:null);
                    //System.err.println("bm_search=" + pos);
                }
                if(pos == -1) {
                    return -1;
                }
                if(range > 0 && (options&RE_OPTIMIZE_EXACTN) != 0) {
                    startpos += pos;
                    range -= pos;
                    if(range < 0) {
                        return -1;
                    }
                }
            }

            for (;;) {
                advance: do {
                    /* If a fastmap is supplied, skip quickly over characters that
                       cannot possibly be the start of a match.  Note, however, that
                       if the pattern can possibly match the null string, we must
                       test it at each starting point so that we take the first null
                       string we get.  */
                    if(fastmap!=null && startpos < size && can_be_null != 1 && !(anchor != 0 && startpos == 0)) {
                        if(range > 0) {	/* Searching forwards.  */
                            int irange = range;
                            pix = string_start+startpos;
                            startpos_adjust: while(range > 0) {
                                c = (char)(string[pix++]&0xFF);
                                if(ismbchar(c,ctx)) {
                                    int len;
                                    if(fastmap[c] != 0) {
                                        break;
                                    }
                                    len = mbclen(c,ctx) - 1;
                                    while(len-- > 0) {
                                        c = (char)(string[pix++]&0xFF);
                                        range--;
                                        if(fastmap[c] == 2) {
                                            break startpos_adjust;
                                        }
                                    }
                                } else {
                                    if(fastmap[MAY_TRANSLATE() ? ctx.translate[c] : c] != 0) {
                                        break;
                                    }
                                }
                                range--;
                            }
                            startpos += irange - range;
                        } else { /* Searching backwards.  */
                            c = (char)(string[string_start + startpos]&0xFF);
                            c &= 0xff;
                            if(MAY_TRANSLATE() ? fastmap[ctx.translate[c]]==0 : fastmap[c]==0) {
                                break advance;
                            }
                        }
 
                    }

                    if(startpos > size) {
                        return -1;
                    }
                    if((anchor!=0 || can_be_null==0) && range > 0 && size > 0 && startpos == size) {
                        return -1;
                    }
                    //System.err.println("doing match_exec(string_start=" + string_start + ",size="+size+",startpos="+startpos+",initpos="+initpos);
                    val = match_exec(string, string_start, size, startpos, initpos, regs);
                    //System.err.println("match_exec=" + val);
                    if(val >= 0) {
                        return startpos;
                    }
                    if(val == -2) {
                        return -2;
                    }

                    if(range > 0) {
                        if(anchor!=0 && startpos < size && (startpos < 1 || string[string_start + startpos-1] != '\n')) {
                            while(range > 0 && string[string_start+startpos] != '\n') {
                                range--;
                                startpos++;
                            }
                        }
                    }
                    break advance;
                } while(true);

                //advance:

                if(range==0) { 
                    break;
                } else if(range > 0) {
                    int d = startpos;
                    if(ismbchar(string[string_start+d],ctx)) {
                        int len = mbclen(string[string_start+d],ctx) - 1;
                        range-=len;
                        startpos+=len;
                        if(range==0) {
                            break;
                        }
                    }
                    range--;
                    startpos++;
                } else {
                    range++;
                    startpos--;
                    {
                        int s = string_start;
                        int d = string_start+ startpos;
                        for(pix = string_start+d; pix-- > s && ismbchar(string[pix],ctx); );
                        if(((d - pix)&1) == 0) {
                            if(range == 0) {
                                break;
                            }
                            range++;
                            startpos--;
                        }
                    }
                }
            }
            break begbuf_match;
        } while(true);
        return -1;
    }

    public int adjust_startpos(byte[] string, int begin, int size, int startpos, int range) {
        /* Update the fastmap now if not correct already.  */
        synchronized(this) {
            if(fastmap_accurate==0) {
                compile_fastmap();
                /*            System.err.println("fastmap: ");
                              for(int i=0;i<fastmap.length; i++) {
                              if((i % 30) == 0) {
                              System.err.println();
                              }
                              System.err.print("" + fastmap[i] + " ");
                              }
                              System.err.println();*/
            }
        }

        /* Adjust startpos for mbc string */
        if(ctx.current_mbctype != 0 && startpos>0 && (options&RE_OPTIMIZE_BMATCH) == 0) {
            int i = mbc_startpos(string, begin, startpos, ctx);

            if(i < startpos) {
                if(range > 0) {
                    startpos = i + mbclen(string[begin+i],ctx);
                } else {
                    int len = mbclen(string[begin+i],ctx);
                    if(i + len <= startpos) {
                        startpos = i + len;
                    } else {
                        startpos = i;
                    }
                }
            }
        }
        return startpos;
    }

    private final int memcmp_translate(char[] s, int s1, int s2, int len) {
        int p1 = s1;
        int p2 = s2;
        char cc;
        while(len>0) {
            cc = s[p1++];
            if(ismbchar(cc,ctx)) {
                int n;
                if(cc != s[p2++]) {
                    return 1;
                }
                for(n=mbclen(cc,ctx)-1; n>0; n--) {
                    if(--len == 0 || s[p1++] != s[p2++]) {
                        return 1;
                    }
                }
            } else {
                if(ctx.translate[cc] != ctx.translate[s[p2++]]) {
                    return 1;
                }
            }
            len--;
        }
        return 0;
    }

    private final int memcmp_translate(byte[] s, int s1, int s2, int len) {
        int p1 = s1;
        int p2 = s2;
        char cc;
        while(len>0) {
            cc = (char)(s[p1++]&0xFF);
            if(ismbchar(cc,ctx)) {
                int n;
                if(cc != s[p2++]) {
                    return 1;
                }
                for(n=mbclen(cc,ctx)-1; n>0; n--) {
                    if(--len == 0 || s[p1++] != s[p2++]) {
                        return 1;
                    }
                }
            } else {
                if(ctx.translate[cc] != ctx.translate[(char)(s[p2++]&0xFF)]) {
                    return 1;
                }
            }
            len--;
        }
        return 0;
    }


    /**
     * @mri re_match
     */
    public int match(byte[] string_arg, int string_start, int size, int pos, Registers regs) {
        return match_exec(string_arg, string_start, size, pos, pos, regs);
    }

    private static class MatchEnvironment {
        public byte[] p;
        public char c;
        public int p1;
        public int pix;
        public int pend;
        public int optz;
        public int num_regs;
        public byte[] string;
        public int string_start;
        public int mcnt;
        public int d;
        public int dend;
        public int pos;
        public int beg;
        public int size;

        public CompileContext ctx;

        /* Failure point stack.  Each place that can handle a failure further
           down the line pushes a failure point on this stack.  It consists of
           restart, regend, and reg_info for all registers corresponding to the
           subexpressions we're currently inside, plus the number of such
           registers, and, finally, two char *'s.  The first char * is where to
           resume scanning the pattern; the second one is where to resume
           scanning the strings.  If the latter is zero, the failure point is a
           ``dummy''; if a failure happens and the failure point is a dummy, it
           gets discarded and the next next one is tried.  */
        public int[] stacka;
        public int[] stackb;
        public int stackp;
        public int stacke;

        public boolean best_regs_set = false;
        public int num_failure_counts = 0;

        public Pattern self;

        /* stack & working area for re_match() */
        private int[] regstart;
        private int[] regend;
        private int[] old_regstart;
        private int[] old_regend;

        private static class RegisterInfoType {
            public char word;
            public boolean is_active;
            public boolean matched_something;
        }

        private RegisterInfoType[] reg_info;
        private int[] best_regstart;
        int[] best_regend;


        public final boolean TRANSLATE_P() {
            return ((optz&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
        }

        public final boolean MAY_TRANSLATE() {
            return ((self.options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0 && ctx.translate!=null);
        }

        public final void init_stack() {
            /* Initialize the stack. */
            int i = -1;
            synchronized(self) {
                i = self.poolIndex++;
            }
            if(i < self.pool.length) {
                stacka = self.pool[i];
            } else {
                stacka = new int[(num_regs*NUM_REG_ITEMS + NUM_NONREG_ITEMS)*NFAILURES];
            }

            stackb = stacka;
            stackp = 0;
            stacke = stackb.length;
        }

        public final void init_registers() {
            /* Initialize subexpression text positions to -1 to mark ones that no
               ( or ( and ) or ) has been seen for. Also set all registers to
               inactive and mark them as not having matched anything or ever
               failed. */
            for(mcnt = 0; mcnt < num_regs; mcnt++) {
                regstart[mcnt] = regend[mcnt]
                    = old_regstart[mcnt] = old_regend[mcnt]
                    = best_regstart[mcnt] = best_regend[mcnt] = REG_UNSET_VALUE;
                reg_info[mcnt].is_active = false;
                reg_info[mcnt].matched_something = false;
            }
        }

        public final void POP_FAILURE_COUNT() {
            int ptr = stackb[--stackp];
            int count = stackb[--stackp];
            STORE_NUMBER(p, ptr, count);
        }

        public final void POP_FAILURE_POINT() {
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

        public final void SET_REGS_MATCHED() {
            for(int this_reg = 0; this_reg < num_regs; this_reg++) {
                reg_info[this_reg].matched_something = reg_info[this_reg].is_active;
            }
        }

        public final void PUSH_FAILURE_POINT(int pattern_place, int string_place) {
            int last_used_reg, this_reg;

            /* Find out how many registers are active or have been matched.
               (Aside from register zero, which is only set at the end.) */
            for(last_used_reg = num_regs-1; last_used_reg > 0; last_used_reg--) {
                if(regstart[last_used_reg]!=REG_UNSET_VALUE) {
                    break;
                }
            }
                        
            if(stacke - stackp <= (last_used_reg * NUM_REG_ITEMS + NUM_NONREG_ITEMS + 1)) {
                int[] stackx;
                int xlen = stacke;
                stackx = new int[2*xlen];
                System.arraycopy(stackb,0,stackx,0,xlen);
                stackb = stackx;
                stacke = 2*xlen;
            }
            stackb[stackp++] = num_failure_counts;
            num_failure_counts = 0;

            /* Now push the info for each of those registers.  */
            for(this_reg = 1; this_reg <= last_used_reg; this_reg++) {
                stackb[stackp++] = regstart[this_reg];
                stackb[stackp++] = regend[this_reg];
                stackb[stackp++] = reg_info[this_reg].word;
            }

            /* Push how many registers we saved.  */
            stackb[stackp++] = last_used_reg;
                        
            stackb[stackp++] = pattern_place;
            stackb[stackp++] = string_place;
            stackb[stackp++] = (int)optz; /* current option status */
            stackb[stackp++] = 0; /* non-greedy flag */
        }

        public final int duplicate() {
            int regno = p[pix++];   /* Get which register to match against */
            int d2, dend2;

            /* Check if there's corresponding group */
            if(regno >= num_regs) {
                return BREAK_FAIL1;
            }
            /* Check if corresponding group is still open */
            if(reg_info[regno].is_active) {
                return BREAK_FAIL1;
            }

            /* Where in input to try to start matching.  */
            d2 = regstart[regno];
            if(d2 == REG_UNSET_VALUE) {
                return BREAK_FAIL1;
            }

            /* Where to stop matching; if both the place to start and
               the place to stop matching are in the same string, then
               set to the place to stop, otherwise, for now have to use
               the end of the first string.  */

            dend2 = regend[regno];
            if(dend2 == REG_UNSET_VALUE) {
                return BREAK_FAIL1;
            }

            for(;;) {
                /* At end of register contents => success */
                if(d2 == dend2) {
                    break;
                }

                /* If necessary, advance to next segment in data.  */
                if(d == dend) {return BREAK_FAIL1;}

                /* How many characters left in this segment to match.  */
                mcnt = dend - d;

                /* Want how many consecutive characters we can match in
                   one shot, so, if necessary, adjust the count.  */
                if(mcnt > dend2 - d2) {
                    mcnt = dend2 - d2;
                }

                /* Compare that many; failure if mismatch, else move
                   past them.  */
                if(((self.options & RE_OPTION_IGNORECASE) != 0) ? self.memcmp_translate(string, string_start+d, string_start+d2, mcnt)!=0 : memcmp(string, string_start+d, string_start+d2, mcnt)!=0) {
                    return BREAK_FAIL1;
                }
                d += mcnt;
                d2 += mcnt;
            }
            return BREAK_NORMAL;
        }

        public final boolean is_in_list_sbc(int cx, byte[] b, int bix) {
            int size = b[bix++]&0xFF;
            return cx/8 < size && ((b[bix + cx/8]&0xFF)&(1<<cx%8)) != 0;
        }
  
        public final boolean is_in_list_mbc(int cx, byte[] b, int bix) {
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

        public final boolean is_in_list(int cx, byte[] b, int bix) {
            return is_in_list_sbc(cx, b, bix) || (ctx.current_mbctype!=0 ? is_in_list_mbc(cx, b, bix) : false);
        }

        public final int handle_fail() {
            /* A restart point is known.  Restart there and pop it. */
            int last_used_reg, this_reg;

            /* If this failure point is from a dummy_failure_point, just
               skip it.  */
            if(stackb[stackp-4] == -1 || (best_regs_set && stackb[stackp-1] == NON_GREEDY)) {
                POP_FAILURE_POINT();
                return 0;
            }
            stackp--;		/* discard greedy flag */
            optz = stackb[--stackp];
            d = stackb[--stackp];
            pix = stackb[--stackp];
            /* Restore register info.  */
            last_used_reg = stackb[--stackp];

            /* Make the ones that weren't saved -1 or 0 again. */
            for(this_reg = num_regs - 1; this_reg > last_used_reg; this_reg--) {
                regend[this_reg] = REG_UNSET_VALUE;
                regstart[this_reg] = REG_UNSET_VALUE;
                reg_info[this_reg].is_active = false;
                reg_info[this_reg].matched_something = false;
            }

            /* And restore the rest from the stack.  */
            for( ; this_reg > 0; this_reg--) {
                reg_info[this_reg].word = (char)stackb[--stackp];
                regend[this_reg] = stackb[--stackp];
                regstart[this_reg] = stackb[--stackp];
            }
            mcnt = stackb[--stackp];
            while(mcnt-->0) {
                int ptr = stackb[--stackp];
                int count = stackb[--stackp];
                STORE_NUMBER(p, ptr, count);
            }
            if(pix < pend) {
                int is_a_jump_n = 0;
                int failed_paren = 0;

                p1 = pix;
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

                            PUSH_FAILURE_POINT(p1+mcnt,d);
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
            for(mcnt = 0; mcnt < num_regs; mcnt++) {
                regstart[mcnt] = best_regstart[mcnt];
                regend[mcnt] = best_regend[mcnt];
            }
        }

        private final void fix_best_regs() {
            for(mcnt = 1; mcnt < num_regs; mcnt++) {
                best_regstart[mcnt] = regstart[mcnt];
                best_regend[mcnt] = regend[mcnt];
            }
        }

        public final int restore_best_regs() {
            /* If not end of string, try backtracking.  Otherwise done.  */
            if((self.options&RE_OPTION_LONGEST)!=0 && d != dend) {
                if(best_regs_set) {/* non-greedy, no need to backtrack */
                    /* Restore best match.  */
                    d = best_regend[0];
                    fix_regs();
                    return 0;
                }
                while(stackp != 0 && stackb[stackp-1] == NON_GREEDY) {
                    if(best_regs_set) {/* non-greedy, no need to backtrack */
                        d = best_regend[0];
                        fix_regs();
                        return 0;
                    }
                    POP_FAILURE_POINT();
                }
                if(stackp != 0) {
                    /* More failure points to try.  */

                    /* If exceeds best match so far, save it.  */
                    if(!best_regs_set || (d > best_regend[0])) {
                        best_regs_set = true;
                        best_regend[0] = d;	/* Never use regstart[0].  */
                        fix_best_regs();
                    }
                    return 1;
                } /* If no failure points, don't restore garbage.  */
                else if(best_regs_set) {
                    /* Restore best match.  */
                    d = best_regend[0];
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
                regs.end[0] = d;
                for(mcnt = 1; mcnt < num_regs; mcnt++) {
                    if(regend[mcnt] == REG_UNSET_VALUE) {
                        regs.beg[mcnt] = -1;
                        regs.end[mcnt] = -1;
                        continue;
                    }
                    regs.beg[mcnt] = regstart[mcnt];
                    regs.end[mcnt] = regend[mcnt];
                }
            }
        }
        
        public final int start_memory() {
            old_regstart[p[pix]] = regstart[p[pix]];
            regstart[p[pix]] = d;
            reg_info[p[pix]].is_active = true;
            reg_info[p[pix]].matched_something = false;
            pix += 2;
            return CONTINUE_MAINLOOP;
        }

        public final int stop_memory() {
            old_regend[p[pix]] = regend[p[pix]];
            regend[p[pix]] = d;
            reg_info[p[pix]].is_active = false;
            pix += 2;
            return CONTINUE_MAINLOOP;
        }

        public final int anychar() {
            if(d == dend) {return BREAK_FAIL1;}
            if(ismbchar(string[string_start+d],ctx)) {
                if(d + mbclen(string[string_start+d],ctx) > dend) {
                    return BREAK_FAIL1;
                }
                SET_REGS_MATCHED();
                d += mbclen(string[string_start+d],ctx);
                return BREAK_NORMAL;
            }
            if((optz&RE_OPTION_MULTILINE)==0
               && (TRANSLATE_P() ? ctx.translate[string[string_start+d]] : string[string_start+d]) == '\n') {
                return 1;
            }
            SET_REGS_MATCHED();
            d++;
            return 0;
        }

        public final int charset() {
            boolean not;	    /* Nonzero for charset_not.  */
            boolean part = false;	    /* true if matched part of mbc */
            int dsave = d + 1;
            int cc;
                    
            if(d == dend) {return BREAK_FAIL1;}
                        
            c = (char)(string[string_start+d++]&0xFF);
            if(ismbchar(c,ctx)) {
                if(d + mbclen(c,ctx) - 1 <= dend) {
                    cc = c;
                    c = self.MBC2WC(c, string, string_start+d);
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
                d = dsave;
            }
            return CONTINUE_MAINLOOP;
        }

        public final int anychar_repeat() {
            for (;;) {
                PUSH_FAILURE_POINT(pix,d);
                if(d == dend) {return BREAK_FAIL1;}
                if(ismbchar(string[string_start+d],ctx)) {
                    if(d + mbclen(string[string_start+d],ctx) > dend) {
                        return BREAK_FAIL1;
                    }
                    SET_REGS_MATCHED();
                    d += mbclen(string[string_start+d],ctx);
                    continue;
                }
                if((optz&RE_OPTION_MULTILINE)==0 &&
                   (TRANSLATE_P() ? ctx.translate[string[string_start+d]] : string[string_start+d]) == '\n') {
                    return BREAK_FAIL1;
                }
                SET_REGS_MATCHED();
                d++;
            }
        }

        public final int maybe_finalize_jump() {
            mcnt = EXTRACT_NUMBER(p,pix);
            pix+=2;
            p1 = pix;

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
                c = p[p1] == endline ? '\n' : (char)(p[p1+2]&0xFF);
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
                mcnt = EXTRACT_NUMBER(p, pix);
                pix += 2;
                if(mcnt < 0 && stackp > 2 && stackb[stackp-3] == d) {/* avoid infinite loop */
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
            p1 = pix;
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

        public final int succeed_n() {
            mcnt = EXTRACT_NUMBER(p, pix + 2);
            /* Originally, this is how many times we HAVE to succeed.  */
            if(mcnt != 0) {
                mcnt--;
                pix += 2;

                c = (char)EXTRACT_NUMBER(p, pix);
                if(stacke - stackp <= NUM_COUNT_ITEMS) {
                    int[] stackx;
                    int xlen = stacke;
                    stackx = new int[2*xlen];
                    System.arraycopy(stackb,0,stackx,0,xlen);
                    stackb = stackx;
                    stacke = 2*xlen;
                }
                stackb[stackp++] = c;
                stackb[stackp++] = pix;
                num_failure_counts++;
                
                STORE_NUMBER(p, pix, mcnt);
                pix+=2;
                
                PUSH_FAILURE_POINT(-1,0);
            } else  {
                mcnt = EXTRACT_NUMBER(p, pix);
                pix+=2;
                PUSH_FAILURE_POINT(pix+mcnt,d);
            }
            return CONTINUE_MAINLOOP;
        }

        public final int jump_n() {
            mcnt = EXTRACT_NUMBER(p, pix + 2);
            /* Originally, this is how many times we CAN jump.  */
            if(mcnt!=0) {
                mcnt--;

                c = (char)EXTRACT_NUMBER(p, pix+2);
                if(stacke - stackp <= NUM_COUNT_ITEMS) {
                    int[] stackx;
                    int xlen = stacke;
                    stackx = new int[2*xlen];
                    System.arraycopy(stackb,0,stackx,0,xlen);
                    stackb = stackx;
                    stacke = 2*xlen;
                }
                stackb[stackp++] = c;
                stackb[stackp++] = pix+2;
                num_failure_counts++;
                STORE_NUMBER(p, pix + 2, mcnt);
                mcnt = EXTRACT_NUMBER(p, pix);
                pix += 2;
                if(mcnt < 0 && stackp > 2 && stackb[stackp-3] == d) {/* avoid infinite loop */
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
                    if(d == dend) {return BREAK_FAIL1;}
                    if(p[pix] == (byte)0xff) {
                        pix++;  
                        if(--mcnt==0
                           || d == dend
                           || string[string_start+d++] != p[pix++]) {
                            return BREAK_FAIL1;
                        }
                        continue;
                    }
                    c = (char)(string[string_start+d++]&0xFF);
                    if(ismbchar(c,ctx)) {
                        int n;
                        if(c != (char)(p[pix++]&0xFF)) {
                            return BREAK_FAIL1;
                        }
                        for(n = mbclen(c,ctx) - 1; n > 0; n--) {
                            if(--mcnt==0
                               || d == dend
                               || string[string_start+d++] != p[pix++]) {
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
                    if(d == dend) {return BREAK_FAIL1;}
                    if(p[pix] == (byte)0xff) {
                        pix++; mcnt--;
                    }
                    if(string[string_start+d++] != p[pix++]) {
                        return BREAK_FAIL1;
                    }
                } while(--mcnt > 0);
            }
            SET_REGS_MATCHED();
            return CONTINUE_MAINLOOP;
        }

        private final int start_nowidth() {
            PUSH_FAILURE_POINT(-1,d);
            if(stackp > RE_DUP_MAX) {
                return RETURN_M2;
            }
            int mcnt = EXTRACT_NUMBER(p, pix);
            pix+=2;
            STORE_NUMBER(p, pix+mcnt, stackp);
            return CONTINUE_MAINLOOP;
        }

        private final int stop_nowidth() {
            int mcnt = EXTRACT_NUMBER(p, pix);
            pix+=2;
            stackp = mcnt;
            d = stackb[stackp-3];
            POP_FAILURE_POINT();
            return CONTINUE_MAINLOOP;
        }

        private final int stop_backtrack() {
            int mcnt = EXTRACT_NUMBER(p, pix);
            pix+=2;
            stackp = mcnt;
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
            if(size == 0 || d == 0) {
                return CONTINUE_MAINLOOP;
            }
            if(string[string_start+d-1] == '\n' && d != dend) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int endline() {
            if(d == dend) {
                return CONTINUE_MAINLOOP;
            } else if(string[string_start+d] == '\n') {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int begbuf() {
            if(d==0) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int endbuf() {
            if(d == dend) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int endbuf2() {
            if(d == dend) {
                return CONTINUE_MAINLOOP;
            }
            /* .. or newline just before the end of the data. */
            if(string[string_start+d] == '\n' && d+1 == dend) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int begpos() {
            if(d == beg) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int on_failure_jump() {
            int mcnt = EXTRACT_NUMBER(p, pix);
            pix+=2;
            PUSH_FAILURE_POINT(pix+mcnt,d);
            return CONTINUE_MAINLOOP;
        }

        private final int finalize_jump() {
            if(stackp > 2 && stackb[stackp-3] == d) {
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

        public final int run(Registers regs) {
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
                            return d - pos;
                        }

                        int var = BREAK_NORMAL;




            //System.err.println("--executing " + (int)p[pix] + " at " + pix);
            //System.err.println("-- -- for d: " + d + " and dend: " + dend);
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
                    d = best_regend[0];
                    fix_regs();
                }
            } while(gotoRestoreBestRegs);

            self.uninit_stack();
            return -1;
        }

        private final int jump() {
            mcnt = EXTRACT_NUMBER(p, pix);
            pix += 2;
            if(mcnt < 0 && stackp > 2 && stackb[stackp-3] == d) {/* avoid infinite loop */
                return BREAK_FAIL1;
            }
            pix += mcnt;
            return CONTINUE_MAINLOOP;
        }

        private final int dummy_failure_jump() {
            PUSH_FAILURE_POINT(-1,0);
            mcnt = EXTRACT_NUMBER(p, pix);
            pix += 2;
            if(mcnt < 0 && stackp > 2 && stackb[stackp-3] == d) {/* avoid infinite loop */
                return BREAK_FAIL1;
            }
            pix += mcnt;
            return CONTINUE_MAINLOOP;
        }

        private final int try_next() {
            mcnt = EXTRACT_NUMBER(p, pix);
            pix += 2;
            if(pix + mcnt < pend) {
                PUSH_FAILURE_POINT(pix,d);
                stackb[stackp-1] = NON_GREEDY;
            }
            pix += mcnt;
            return CONTINUE_MAINLOOP;
        }

        private final int set_number_at() {
            mcnt = EXTRACT_NUMBER(p, pix);
            pix+=2;
            p1 = pix + mcnt;
            mcnt = EXTRACT_NUMBER(p, pix);
            pix+=2;
            STORE_NUMBER(p, p1, mcnt);
            return CONTINUE_MAINLOOP;
        }

        private final int finalize_push() {
            POP_FAILURE_POINT();
            mcnt = EXTRACT_NUMBER(p, pix);
            pix+=2;
            if(mcnt < 0 && stackp > 2 && stackb[stackp-3] == d) { /* avoid infinite loop */
                return BREAK_FAIL1;
            }
            PUSH_FAILURE_POINT(pix+mcnt,d);
            stackb[stackp-1] = NON_GREEDY;
            return CONTINUE_MAINLOOP;
        }

        private final int finalize_push_n() {
            mcnt = EXTRACT_NUMBER(p, pix + 2); 
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
                    if(mcnt < 0 && stackp > 2 && stackb[stackp-3] == d) {/* avoid infinite loop */
                        return BREAK_FAIL1;
                    }
                    pix += mcnt;
                    return CONTINUE_MAINLOOP;
                }
                POP_FAILURE_POINT();
                mcnt = EXTRACT_NUMBER(p,pix);
                pix+=2;
                PUSH_FAILURE_POINT(pix+mcnt,d);
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
            if(d == 0) {
                if(d == dend) {return BREAK_FAIL1;}
                if(IS_A_LETTER(string,string_start+d,string_start+dend)) {
                    return CONTINUE_MAINLOOP;
                } else {
                    return BREAK_FAIL1;
                }
            }
            if(d == dend) {
                if(PREV_IS_A_LETTER(string,string_start+d,string_start+dend)) {
                    return CONTINUE_MAINLOOP;
                } else {
                    return BREAK_FAIL1;
                }
            }
            if(PREV_IS_A_LETTER(string,string_start+d,string_start+dend) != IS_A_LETTER(string,string_start+d,string_start+dend)) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int notwordbound() {
            if(d==0) {
                if(IS_A_LETTER(string, string_start+d, string_start+dend)) {
                    return BREAK_FAIL1;
                } else {
                    return CONTINUE_MAINLOOP;
                }
            }
            if(d == dend) {
                if(PREV_IS_A_LETTER(string, string_start+d, string_start+dend)) {
                    return BREAK_FAIL1;
                } else {
                    return CONTINUE_MAINLOOP;
                }
            }
            if(PREV_IS_A_LETTER(string, string_start+d, string_start+dend) != IS_A_LETTER(string, string_start+d, string_start+dend)) {
                return BREAK_FAIL1;
            }
            return CONTINUE_MAINLOOP;
        }

        private final int wordbeg() {
            if(IS_A_LETTER(string, string_start+d, string_start+dend) && (d==0 || !PREV_IS_A_LETTER(string,string_start+d,string_start+dend))) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int wordend() {
            if(d!=0 && PREV_IS_A_LETTER(string, string_start+d, string_start+dend)
               && (!IS_A_LETTER(string, string_start+d, string_start+dend) || d == dend)) {
                return CONTINUE_MAINLOOP;
            }
            return BREAK_FAIL1;
        }

        private final int wordchar() {
            if(d == dend) {return BREAK_FAIL1;}
            if(!IS_A_LETTER(string,string_start+d,string_start+dend)) {
                return BREAK_FAIL1;
            }
            if(ismbchar(string[string_start+d],ctx) && d + mbclen(string[string_start+d],ctx) - 1 < dend) {
                d += mbclen(string[string_start+d],ctx) - 1;
            }
            d++;
            SET_REGS_MATCHED();
            return CONTINUE_MAINLOOP;
        }
        
        private final int notwordchar() {
            if(d == dend) {return BREAK_FAIL1;}
            if(IS_A_LETTER(string, string_start+d, string_start+dend)) {
                return BREAK_FAIL1;
            }
            if(ismbchar(string[string_start+d],ctx) && d + mbclen(string[string_start+d],ctx) - 1 < dend) {
                d += mbclen(string[string_start+d],ctx) - 1;
            }
            d++;
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

        public MatchEnvironment(Pattern p, byte[] string_arg, int string_start, int size, int pos, int beg, Registers regs) {
            this.size = size;
            this.beg = beg;
            this.p = p.buffer;
            this.pix = 0;
            this.p1=-1;
            this.pend = p.used;
            this.num_regs = p.re_nsub;
            this.string = string_arg;
            this.string_start = string_start;
            this.optz = (int)p.options;
            this.self = p;
            this.ctx = p.ctx;
            this.pos = pos;

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

            /* `p' scans through the pattern as `d' scans through the data. `dend'
               is the end of the input string that `d' points within. `d' is
               advanced into the following input string whenever necessary, but
               this happens before fetching; therefore, at the beginning of the
               loop, `d' can be pointing at the end of a string, but it cannot
               equal string2.  */

            d = pos; dend = size;
        }

        public final int fail() {
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
    }

    final static int BREAK_NORMAL = 0;
    final static int BREAK_FAIL1 = 1;
    final static int CONTINUE_MAINLOOP = 2;
    final static int RETURN_M2 = 3;

    /**
     * @mri re_match_exec
     */
    public int match_exec(byte[] string_arg, int string_start, int size, int pos, int beg, Registers regs) {
        return new MatchEnvironment(this,string_arg,string_start,size,pos,beg,regs).run(regs);

    }

    protected void uninit_stack() {
        synchronized(this) {
            this.poolIndex--;
        }
    }
    

    /**
     * @mri slow_match
     */
    public boolean slow_match(byte[] little, int littleix, int lend, byte[] big, int bigix, int bend, char[] translate) {
        while(littleix < lend && bigix < bend) {
            char c = (char)(little[littleix++]&0xFF);
            if(c == 0xff) {
                c = (char)(little[littleix++]&0xFF);
            }
            if(!(translate != null ? translate[big[bigix++]]==translate[c] : (char)(big[bigix++]&0xFF)==c)) { 
                break;
            }
        }
        return littleix == lend;
    }

    /**
     * @mri slow_search
     */
    public int slow_search(byte[] little, int littleix, int llen, byte[] big, int bigix, int blen, char[] translate) {
        int bsave = bigix;
        int bend = bigix+blen;
        boolean fescape = false;

        char c = (char)(little[littleix]&0xFF);
        if(c == 0xff) {
            c = (char)(little[littleix+1]&0xFF);
            fescape = true;
        } else if(translate!=null && !ismbchar(c,ctx)) {
            c = translate[c];
        }

        while(bigix < bend) {
            /* look for first character */
            if(fescape) {
                while(bigix < bend) {
                    if((big[bigix]&0xFF) == c) {
                        break;
                    }
                    bigix++;
                }
            } else if(translate!=null && !ismbchar(c,ctx)) {
                while(bigix < bend) {
                    if(ismbchar(big[bigix],ctx)) {
                        bigix+=mbclen(big[bigix],ctx)-1;
                    } else if(translate[big[bigix]&0xFF] == c) {
                        break;
                    }
                    bigix++;
                }
            } else {
                while(bigix < bend) {
                    if((big[bigix]&0xFF) == c) {
                        break;
                    }
                    if(ismbchar(big[bigix],ctx)) {
                        bigix+=mbclen(big[bigix],ctx)-1;
                    }
                    bigix++;
                }
            }

            if(slow_match(little, littleix, littleix+llen, big, bigix, bend, translate)) {
                return bigix - bsave;
            }

            if(bigix<bend) {
                bigix+=mbclen(big[bigix],ctx);
            }
        }
        return -1;
    }

    /**
     * @mri bm_search
     */
    public int bm_search(byte[] little, int littleix, int llen, byte[] big, int bigix, int blen, int[] skip, char[] translate) {
        int i, j, k;
        i = llen-1;
        if(translate != null) {
            while(i < blen) {
                k = i;
                j = llen-1;
                while(j >= 0 && translate[big[bigix+k]&0xFF] == translate[little[littleix+j]&0xFF]) {
                    k--;
                    j--;
                }
                if(j < 0) {
                    return k+1;
                }
                i += skip[translate[big[bigix+i]&0xFF]];
            }
            return -1;
        }
        while(i < blen) {
            k = i;
            j = llen-1;
            while(j >= 0 && big[bigix+k] == little[littleix+j]) {
                k--;
                j--;
            }
            if(j < 0) {
                return k+1;
            }
            i += skip[big[bigix+i]&0xFF];
        }
        return -1;
    }

    public final boolean TRANSLATE_P(long optz) {
        return ((optz&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
    }

    /**
     * @mri re_compile_fastmap
     */
    private final void compile_fastmap() {
        int size = used;
        byte[] p = buffer;
        int pix = 0;
        int pend = size;
        int j,k;
        int is_a_succeed_n;
        
        int[] stacka = new int[NFAILURES];
        int[] stackb = stacka;
        int stackp = 0;
        int stacke = NFAILURES;
        long optz = options;

        Arrays.fill(fastmap, 0, 256, (byte)0);

        fastmap_accurate = 1;
        can_be_null = 0;

        while(pix != -1) {
            is_a_succeed_n = 0;
            if(pix == pend) {
                can_be_null = 1;
                break;
            }
            switch(p[pix++]) {
            case exactn:
                if(p[pix+1] == (byte)0xff) {
                    if(TRANSLATE_P(optz)) {
                        fastmap[ctx.translate[p[pix+2]]] = 2;
                    } else {
                        fastmap[p[pix+2]&0xFF] = 2;
                    }
                    options |= RE_OPTIMIZE_BMATCH;
                } else if(TRANSLATE_P(optz)) {
                    fastmap[ctx.translate[p[pix+1]]] = 1;
                } else {
                    fastmap[p[pix+1]&0xFF] = 1;
                }
                break;
            case begline:
            case begbuf:
            case begpos:
            case endbuf:
            case endbuf2:
            case wordbound:
            case notwordbound:
            case wordbeg:
            case wordend:
            case pop_and_fail:
            case push_dummy_failure:
            case start_paren:
            case stop_paren:
                continue;
            case casefold_on:
                options |= RE_MAY_IGNORECASE;
                optz |= RE_OPTION_IGNORECASE;
                continue;
            case casefold_off:
                optz &= ~RE_OPTION_IGNORECASE;
                continue;
            case option_set:
                optz = p[pix++];
                continue;
            case endline:
                if(TRANSLATE_P(optz)) {
                    fastmap[ctx.translate['\n']] = 1;
                } else {
                    fastmap['\n'] = 1;
                }
                if((optz & RE_OPTION_SINGLELINE) == 0 && can_be_null == 0) {
                    can_be_null = 2;
                }
                break;
            case jump_n:
            case finalize_jump:
            case maybe_finalize_jump:
            case jump:
            case jump_past_alt:
            case dummy_failure_jump:
            case finalize_push:
            case finalize_push_n:
                j = EXTRACT_NUMBER(p,pix);
                pix += 2;
                pix += j;	
                if(j > 0) {
                    continue;
                }
                /* Jump backward reached implies we just went through
                   the body of a loop and matched nothing.
                   Opcode jumped to should be an on_failure_jump.
                   Just treat it like an ordinary jump.
                   For a * loop, it has pushed its failure point already;
                   If so, discard that as redundant.  */
                if(p[pix] != on_failure_jump && p[pix] != try_next && p[pix] != succeed_n) {
                    continue;
                }
                pix++;
                j = EXTRACT_NUMBER(p,pix);
                pix+=2;
                pix += j;
                if(stackp != 0 && stackb[stackp] == pix) {
                    stackp--;		/* pop */
                }
                continue;
            case try_next:
            case start_nowidth:
            case stop_nowidth:
            case stop_backtrack:
                pix += 2;
                continue;
            case succeed_n:
                is_a_succeed_n = 1;
                /* Get to the number of times to succeed.  */
                k = EXTRACT_NUMBER(p, pix + 2);
                /* Increment p past the n for when k != 0.  */
                if(k != 0) {
                    pix += 4;
                    continue;
                }
                /* fall through */
            case on_failure_jump:
                j = EXTRACT_NUMBER(p,pix);
                pix += 2;
                if(pix + j < pend) {
                    if(stackp == stacke) {
                        int[] stackx;
                        int xlen = stacke;
                        stackx = new int[2*xlen];
                        System.arraycopy(stackb,0,stackx,0,xlen);
                        stackb = stackx;
                        stacke = 2*xlen;
                    }
                    stackb[++stackp] = pix + j;	/* push */
                } else {
                    can_be_null = 1;
                }
                if(is_a_succeed_n>0) {
                    k = EXTRACT_NUMBER(p,pix);	/* Skip the n.  */
                    pix += 2;
                }
                continue;
            case set_number_at:
                pix += 4;
                continue;
            case start_memory:
            case stop_memory:
                pix += 2;
                continue;
            case duplicate:
                can_be_null = 1;
                if(p[pix] >= re_nsub) {
                    break;
                }
                fastmap['\n'] = 1;
            case anychar_repeat:
            case anychar:
                for(j = 0; j < 256; j++) {
                    if(j != '\n' || ((optz & RE_OPTION_MULTILINE)) != 0) {
                        fastmap[j] = 1;
                    }
                }
                if(can_be_null!=0) {
                    return;
                }
                /* Don't return; check the alternative paths
                   so we can set can_be_null if appropriate.  */
                if(p[pix-1] == anychar_repeat) {
                    continue;
                }
                break;
      case wordchar:
          for(j = 0; j < 0x80; j++) {
              if(re_syntax_table[j] == Sword) {
                  fastmap[j] = 1;
              }
          }
          switch(ctx.current_mbctype) {
          case MBCTYPE_ASCII:
              for(j = 0x80; j < 256; j++) {
                  if(re_syntax_table[j] == Sword2) {
                      fastmap[j] = 1;
                  }
              }
              break;
          case MBCTYPE_EUC:
          case MBCTYPE_SJIS:
          case MBCTYPE_UTF8:
              for(j = 0x80; j < 256; j++) {
                  if(ctx.re_mbctab[j] != 0) {
                      fastmap[j] = 1;
                  }
              }
              break;
          }
          break;
      case notwordchar:
          for(j = 0; j < 0x80; j++) {
              if(re_syntax_table[j] != Sword) {
                  fastmap[j] = 1;
              }
          }
          switch(ctx.current_mbctype) {
          case MBCTYPE_ASCII:
              for(j = 0x80; j < 256; j++) {
                  if(re_syntax_table[j] != Sword2) {
                      fastmap[j] = 1;
                  }
              }
              break;
          case MBCTYPE_EUC:
          case MBCTYPE_SJIS:
          case MBCTYPE_UTF8:
              for(j = 0x80; j < 256; j++) {
                  if(ctx.re_mbctab[j] == 0) {
                      fastmap[j] = 1;
                  }
              }
              break;
          }
          break;
      case charset:
          /* NOTE: Charset for single-byte chars never contain
             multi-byte char.  See set_list_bits().  */
          for(j = p[pix++] * 8 - 1; j >= 0; j--) {
              if((p[pix + j / 8] & (1 << (j % 8))) != 0) {
                  int tmp = TRANSLATE_P(optz)?ctx.translate[j]:j;
                  fastmap[tmp] = 1;
              }
          }
          {
              int c, beg, end;

              pix += p[pix-1] + 2;
              size = EXTRACT_UNSIGNED(p,pix-2);
              for(j = 0; j < size; j++) {
                  c = EXTRACT_MBC(p,pix+j*8);
                  beg = WC2MBC1ST(c);
                  c = EXTRACT_MBC(p,pix+j*8+4);
                  end = WC2MBC1ST(c);
                  /* set bits for 1st bytes of multi-byte chars.  */
                  while(beg <= end) {
                      /* NOTE: Charset for multi-byte chars might contain
                         single-byte chars.  We must reject them. */
                      if(c < 0x100) {
                          fastmap[beg] = 2;
                          options |= RE_OPTIMIZE_BMATCH;
                      } else if(ismbchar((char)beg,ctx)) {
                          fastmap[beg] = 1;
                      }
                      beg++;
                  }
              }
          }
          break;
            case charset_not:
                /* S: set of all single-byte chars.
                   M: set of all first bytes that can start multi-byte chars.
                   s: any set of single-byte chars.
                   m: any set of first bytes that can start multi-byte chars.
                   
                   We assume S+M = U.
                   ___      _   _
                   s+m = (S*s+M*m).  */
                /* Chars beyond end of map must be allowed */
                /* NOTE: Charset_not for single-byte chars might contain
                   multi-byte chars.  See set_list_bits(). */
                for(j = p[pix] * 8; j < 256; j++) {
                    if(!ismbchar(j,ctx)) {
                        fastmap[j] = 1;
                    }
                }

                for(j = p[pix++] * 8 - 1; j >= 0; j--) {
                    if((p[pix + j / 8] & (1 << (j % 8))) == 0) {
                        if(!ismbchar(j,ctx)) {
                            fastmap[j] = 1;
                        }
                    }
                }
                {
                    long c, beg;
                    int num_literal = 0;
                    pix += p[pix-1] + 2;
                    size = EXTRACT_UNSIGNED(p,pix-2);
                    if(size == 0) {
                        for(j = 0x80; j < 256; j++) {
                            if(ismbchar(j,ctx)) {
                                fastmap[j] = 1;
                            }
                        }
                        break;
                    }
                    for(j = 0,c = 0;j < size; j++) {
                        int cc = EXTRACT_MBC(p,pix+j*8);
                        beg = WC2MBC1ST(cc);
                        while(c <= beg) {
                            if(ismbchar((int)c, ctx)) {
                                fastmap[(int)c] = 1;
                            }
                            c++;
                        }
                        cc = EXTRACT_MBC(p,pix+j*8+4);
                        if(cc < 0xff) {
                            num_literal = 1;
                            while(c <= cc) {
                                if(ismbchar((int)c, ctx)) {
                                    fastmap[(int)c] = 1;
                                }
                                c++;
                            }
                        }
                        c = WC2MBC1ST(cc);
                    }

                    for(j = (int)c; j < 256; j++) {
                        if(num_literal != 0) {
                            fastmap[j] = 1;
                        }
                        if(ismbchar(j,ctx)) {
                            fastmap[j] = 1;
                        }
                    }
                }
                break;
            }
            /* Get here means we have successfully found the possible starting
               characters of one path of the pattern.  We need not follow this
               path any farther.  Instead, look at the next alternative
               remembered in the stack.  */
            if(stackp != 0) {
                pix = stackb[stackp--];		/* pop */
            } else {
                break;
            }
        }
    }

    public static void tmain4(String[] args) throws Exception {
        byte[] ccc = args[1].getBytes("ISO-8859-1");
        Registers reg = new Registers();
        System.out.println(Pattern.compile(args[0].getBytes()).search(ccc,0,ccc.length,0,ccc.length,reg));
        for(int i=0;i<reg.num_regs;i++) {
            System.err.println("[" + i + "]" + reg.beg[i] + ":" + reg.end[i] + "=" + new String(ccc,reg.beg[i],reg.end[i]-reg.beg[i]));
        }
    }

    public static void tmain1(String[] args) throws Exception {
        Pattern.compile(args[0].getBytes());
    }

    public static void tmain2(String[] args) throws Exception {
        System.err.println("Speed test");
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(args[0]));
        java.util.List strings = new java.util.ArrayList();
        String s;
        while((s = reader.readLine()) != null) {
            strings.add(s);
        }
        reader.close();
        String[] sss = (String[])strings.toArray(new String[0]);
        int times = 1000;

        long b1 = System.currentTimeMillis();

        for(int j=0;j<times;j++) {
            for(int i=0;i<sss.length;i++) {
                java.util.regex.Pattern.compile(sss[i]);
            }
        }
        long a1 = System.currentTimeMillis();

        long b2 = System.currentTimeMillis();
        for(int j=0;j<times;j++) {
            for(int i=0;i<sss.length;i++) {
                Pattern.compile(sss[i].getBytes());
            }
        }
        long a2 = System.currentTimeMillis();

        System.out.println("Compiling " + (times*sss.length) + " regexps took java.util.regex: " + ((a1-b1)) + "ms");
        System.out.println("Compiling " + (times*sss.length) + " regexps took REJ: " + ((a2-b2)) + "ms");
    }

    public static void main(String[] args) throws Exception {
        System.err.println("Speed test");

        //        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(args[0]);

        long b1 = System.currentTimeMillis();
        int times = 100000;
        /*
        for(int j=0;j<times;j++) {
            p1.matcher(args[1]).find();
        }
        */
        long a1 = System.currentTimeMillis();

        long b2 = System.currentTimeMillis();
        Pattern p2 = Pattern.compile(args[0].getBytes());
        byte[] ss = args[1].getBytes("ISO-8859-1");
        Registers rgs = new Registers();
        for(int j=0;j<times;j++) {
            p2.search(ss,0,ss.length,0,ss.length,rgs);
        }
        long a2 = System.currentTimeMillis();

        //        System.out.println("Searching " + (times) + " regexps took java.util.regex: " + ((a1-b1)) + "ms");
        System.out.println("Searching " + (times) + " regexps took REJ: " + ((a2-b2)) + "ms");
    }

    public static void tmain3(String[] args) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(args[0]));
        java.util.List strings = new java.util.ArrayList();
        String s;
        while((s = reader.readLine()) != null) {
            strings.add(s);
        }
        reader.close();
        String[] sss = (String[])strings.toArray(new String[0]);

        int times = 13;

        java.util.Map tes = new java.util.TreeMap();

        for(int i=0;i<sss.length;i++) {
            long a1 = System.currentTimeMillis();
            for(int j=0;j<times;j++) {
                Pattern.compile(sss[i].getBytes());
            }
            long b1 = System.currentTimeMillis();
            tes.put(new Long(b1-a1),sss[i]);
        }

        for(java.util.Iterator iter = tes.entrySet().iterator();iter.hasNext();) {
            System.err.println(iter.next());
        }
    }
}// Pattern

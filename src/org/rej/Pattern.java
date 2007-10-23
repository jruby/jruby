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

    CompileContext ctx;

    private final int WC2MBC1ST(long c) {
        if(ctx.current_mbctype != MBCTYPE_UTF8) {
            return (int)((c<0x100) ? c : ((c>>8)&0xff));
        } else {
            return (int)utf8_firstbyte(c);
        }
    }

    private void compile(byte[] pattern, int start, int length, CompileContext ctx) throws PatternSyntaxException {
        this.ctx = ctx;
        new CompilationEnvironment(ctx,buffer,pattern,start,start+length,options,this).compile();
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

    final int memcmp_translate(char[] s, int s1, int s2, int len) {
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

    final int memcmp_translate(byte[] s, int s1, int s2, int len) {
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

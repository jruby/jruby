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

import static org.rej.Bytecodes.*;
import static org.rej.REJConstants.*;
import static org.rej.MBC.*;
import static org.rej.Helpers.*;

class CompilationEnvironment {
    private CompileContext ctx;
    private byte[] b;
    private int bix;
    private byte[] p;
    private int pix;
    private int pend;
    private char c, c1;
    private int p0;
    private long optz;
    private int[] numlen = new int[1];
    private int nextp;

    /* Address of the count-byte of the most recently inserted `exactn'
       command.  This makes it possible to tell whether a new exact-match
       character can be added to that command or requires a new `exactn'
       command.  */

    private int pending_exact = -1;

    /* Address of the place where a forward-jump should go to the end of
       the containing expression.  Each alternative of an `or', except the
       last, ends with a forward-jump of this sort.  */

    private int fixup_alt_jump = -1;
        
    /* Address of start of the most recently finished expression.
       This tells postfix * where to find the start of its operand.  */

    private int laststart = -1;

    /* In processing a repeat, 1 means zero matches is allowed.  */

    private boolean zero_times_ok = false;

    /* In processing a repeat, 1 means many matches is allowed.  */

    private boolean many_times_ok = false;

    private boolean greedy = false;

    /* Address of beginning of regexp, or inside of last (.  */

    private int begalt = 0;

    /* Place in the uncompiled pattern (i.e., the {) to
       which to go back if the interval is invalid.  */
    private int beg_interval;

    /* In processing an interval, at least this many matches must be made.  */
    private int lower_bound;

    /* In processing an interval, at most this many matches can be made.  */
    private int upper_bound;

    /* Stack of information saved by ( and restored by ).
       Five stack elements are pushed by each (:
       First, the value of b.
       Second, the value of fixup_alt_jump.
       Third, the value of begalt.
       Fourth, the value of regnum.
       Fifth, the type of the paren. */

    private int[] stacka = new int[40];
    private int[] stackb = stacka;
    private int stackp = 0;
    private int stacke = 40;

    /* Counts ('s as they are encountered.  Remembered for the matching ),
       where it becomes the register number to put in the stop_memory
       command.  */

    private int regnum = 1;

    private int range = 0;
    private int had_mbchar = 0;
    private int had_num_literal = 0;
    private int had_char_class = 0;

    private boolean gotoRepeat=false;
    private boolean gotoNormalChar=false;
    private boolean gotoNumericChar=false;

    private Pattern self;

    public CompilationEnvironment(CompileContext ctx, byte[] buffer, byte[] pattern, int start, int end, long options, Pattern pat) {
        this.ctx = ctx;
        this.b = buffer;
        this.bix = 0;
        this.p = pattern;
        this.pix = start;
        this.pend = end;
        this.c1 = 0;
        this.optz = options;
        this.self = pat;

        self.fastmap_accurate = 0;
        self.must = -1;
        self.must_skip = null;

        if(self.allocated == 0) {
            self.allocated = INIT_BUF_SIZE;
            /* EXTEND_BUFFER loses when allocated is 0.  */
            self.buffer = new byte[INIT_BUF_SIZE];
            this.b = self.buffer;
        }
    }

    public void compile() {
        mainParse: while(pix != pend) {
            PATFETCH();

            mainSwitch: do {
                switch(c) {
                case '$':
                    dollar();
                    break;

                case '^':
                    caret();
                    break;

                case '+':
                case '?':
                case '*':
                    prepareRepeat();

                    /* Star, etc. applied to an empty pattern is equivalent
                       to an empty pattern.  */
                    if(laststart==-1) {
                        break;
                    }
                    
                    if(greedy && many_times_ok && b[laststart] == anychar && bix-laststart <= 2) {
                        if(b[bix - 1] == stop_paren) {
                            bix--;
                        }
                        if(zero_times_ok) {
                            b[laststart] = anychar_repeat;
                        } else {
                            BUFPUSH(anychar_repeat);
                        }
                        break;
                    }

                    mainRepeat();
                    break;

                case '.':
                    dot();
                    break;

                case '[':
                    prepareCharset();
                    main_charset();
                    compact_charset();
                    break;

                case '(':
                    group_start();

                    if(c == '#') {
                        if(push_option!=0) {
                            BUFPUSH(option_set);
                            BUFPUSH((byte)optz);
                        }
                        if(casefold!=0) {
                            if((optz & RE_OPTION_IGNORECASE) != 0) {
                                BUFPUSH(casefold_on);
                            } else {
                                BUFPUSH(casefold_off);
                            }
                        }
                        break;
                    }

                    group_start_end();
                    break;
                case ')':
                    group_end();
                    break;
                case '|':
                    alt();
                    break;
                case '{':
                unfetch_interval: do {
                    start_bounded_repeat();
                    
                    if(lower_bound < 0 || c != '}') {
                        break unfetch_interval;
                    }
                    switch(continue_bounded_repeat()) {
                    case 0:
                        break;
                    case 1:
                        continue mainSwitch;
                    case 2:
                        break mainSwitch;
                    }
                    bounded_nontrivial();
                    break mainSwitch;
                } while(false);
                unfetch_interval();
                break mainSwitch;

                case '\\':
                    escape();
                    break mainSwitch;
                case '#':
                    if((optz & RE_OPTION_EXTENDED) != 0) {
                        while(pix != pend) {
                            PATFETCH();
                            if(c == '\n') {
                                break;
                            }
                        }
                        break mainSwitch;
                    }
                    gotoNormalChar = true;
                    break mainSwitch;

                case ' ':
                case '\t':
                case '\f':
                case '\r':
                case '\n':
                    if((optz & RE_OPTION_EXTENDED) != 0) {
                        break mainSwitch;
                    }
                default:
                    if(c == ']') {
                        self.re_warning("regexp has `]' without escape");
                    } else if(c == '}') {
                        self.re_warning("regexp has `}' without escape");
                    }
                    gotoNormalChar = true;
                }
            } while(gotoRepeat);

            handle_num_or_normal();
        }

        finalize_compilation();
    }

    public final void PATFETCH() {
        if(pix == pend) {
            err("premature end of regular expression");
        }
        c = (char)(p[pix++]&0xFF);
        if(TRANSLATE_P()) {
            c = ctx.translate[c];
        }
    }

    public final boolean TRANSLATE_P() {
        return ((optz&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
    }

    public final boolean MAY_TRANSLATE() {
        return ((self.options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0 && ctx.translate!=null);
    }

    public final void BUFPUSH(char ch) {
        BUFPUSH((byte)ch);
    }

    public final void BUFPUSH(byte ch) {
        GET_BUFFER_SPACE(1);
        b[bix++] = ch;
    }

    public final void PATFETCH_RAW() {
        if(pix == pend) {
            err("premature end of regular expression");
        }
        c = (char)(p[pix++]&0xFF);
    }

    public final void PATFETCH_RAW_c1() {
        if(pix == pend) {
            err("premature end of regular expression");
        }
        c1 = (char)(p[pix++]&0xFF);
    }

    private final static void insert_op_2(byte op, byte[] b, int there, int current_end, int num_1, int num_2) {
        int pfrom = current_end;
        int pto = current_end + NUMBER_LENGTH*2 + 1;

        System.arraycopy(b,there,b,there+NUMBER_LENGTH*2+1,pfrom-there);

        b[there] = op;
        STORE_NUMBER(b, there + 1, num_1);
        STORE_NUMBER(b, there + NUMBER_LENGTH + 1, num_2);
    }

    private final static void store_jump_n(byte[] b, int from, byte opcode, int to, int n) {
        b[from] = opcode;
        STORE_NUMBER(b, from + 1, to - (from + NUMBER_LENGTH + 1));
        STORE_NUMBER(b, from + NUMBER_LENGTH + 1, n);
    }

    private final static void store_jump(byte[] b, int from, byte opcode, int to) {
        b[from] = opcode;
        STORE_NUMBER(b, from+1, to-(from+NUMBER_LENGTH+1));
    }

    private final static void insert_jump_n(byte op, byte[] b, int from, int to, int current_end, int n) {
        int pfrom = current_end;
        int pto = current_end+NUMBER_LENGTH*2 + 1;

        System.arraycopy(b,from,b,from+NUMBER_LENGTH*2 + 1,pfrom-from);
        store_jump_n(b, from, op, to, n);
    }


    private final static void insert_jump(byte op, byte[] b, int from, int to, int current_end) {
        int pfrom = current_end;
        int pto = current_end+NUMBER_LENGTH+1;
        System.arraycopy(b,from,b,from+NUMBER_LENGTH+1,pfrom-from);
        store_jump(b, from, op, to);
    }

    public final void dollar() {
        if((optz & RE_OPTION_SINGLELINE) != 0) {
            BUFPUSH(endbuf);
        } else {
            p0 = pix;
            /* When testing what follows the $,
               look past the \-constructs that don't consume anything.  */
            while(p0 != pend) {
                if(p[p0] == '\\' && p0 + 1 != pend && (p[p0+1] == 'b' || p[p0+1] == 'B')) {
                    p0 += 2;
                } else {
                    break;
                }
            }
            BUFPUSH(endline);
        }
    }

    public final void caret() {
        if((optz & RE_OPTION_SINGLELINE) != 0) {
            BUFPUSH(begbuf);
        } else {
            BUFPUSH(begline);
        }
    }

    public final void prepareRepeat() {
        if(!gotoRepeat) {
            /* If there is no previous pattern, char not special. */
            if(laststart==-1) {
                err("invalid regular expression; there's no previous pattern, to which '"+
                    (char)c
                    +"' would define cardinality at " + pix);
            }
            /* If there is a sequence of repetition chars,
               collapse it down to just one.  */
            zero_times_ok = c != '+';
            many_times_ok = c != '?';
            greedy = true;
                
            if(pix != pend) {
                PATFETCH();
                switch (c) {
                case '?':
                    greedy = false;
                    break;
                case '*':
                case '+':
                    err("nested *?+ in regexp");
                default:
                    pix--;
                    break;
                }
            }
        } else {
            gotoRepeat = false;
        }
    }

    public final void mainRepeat() {
        /* Now we know whether or not zero matches is allowed
           and also whether or not two or more matches is allowed.  */
        if(many_times_ok) {
            /* If more than one repetition is allowed, put in at the
               end a backward relative jump from b to before the next
               jump we're going to put in below (which jumps from
               laststart to after this jump).  */
            GET_BUFFER_SPACE(3);
            store_jump(b,bix,greedy?maybe_finalize_jump:finalize_push,laststart-3);
            bix += 3;  	/* Because store_jump put stuff here.  */
        }

        /* On failure, jump from laststart to next pattern, which will be the
           end of the buffer after this jump is inserted.  */
        GET_BUFFER_SPACE(3);
        insert_jump(on_failure_jump, b, laststart, bix + 3, bix);
        bix += 3;

        if(zero_times_ok) {
            if(!greedy) {
                GET_BUFFER_SPACE(3);
                insert_jump(try_next, b, laststart, bix + 3, bix);
                bix += 3;
            }
        } else {
            /* At least one repetition is required, so insert a
               `dummy_failure_jump' before the initial
               `on_failure_jump' instruction of the loop. This
               effects a skip over that instruction the first time
               we hit that loop.  */
            GET_BUFFER_SPACE(3);
            insert_jump(dummy_failure_jump, b, laststart, laststart + 6, bix);
            bix += 3;
        }
    }

    public final void dot() { 
        laststart = bix;
        BUFPUSH(anychar);
    }

    public final void prepareCharset() {
        if(pix == pend) {
            err("invalid regular expression; '[' can't be the last character ie. can't start range at the end of pattern");
        }

        while((bix + 9 + 32) > self.allocated) {
            EXTEND_BUFFER();
        }

        laststart = bix;
        if(p[pix] == '^') {
            BUFPUSH(charset_not);
            pix++;
        } else {
            BUFPUSH(charset);
        }
        p0 = pix;
            
        BUFPUSH((byte)32);
        Arrays.fill(b,bix,bix + 32 + 2,(byte)0);
                    
        had_mbchar = 0;
        had_num_literal = 0;
        had_char_class = 0;
    }

    public int last = -1;

    public final void charset_w() {
        for(c = 0; c < 256; c++) {
            if(re_syntax_table[c] == Sword || (ctx.current_mbctype==0 && re_syntax_table[c] == Sword2)) {
                SET_LIST_BIT(c);
            }
        }
        if(ctx.current_mbctype != 0) {
            set_list_bits(0x80, 0xffffffff, b, bix);
        }
        had_char_class = 1;
        last = -1;
    }

    public final void charset_W() {
        for(c = 0; c < 256; c++) {
            if(re_syntax_table[c] != Sword &&
               ((ctx.current_mbctype>0 && ctx.re_mbctab[c] == 0) ||
                (ctx.current_mbctype==0 && re_syntax_table[c] != Sword2))) {
                SET_LIST_BIT(c);
            }
        }
        had_char_class = 1;
        last = -1;
    }

    public final void charset_s() {
        for(c = 0; c < 256; c++) {
            if(Character.isWhitespace(c)) {
                SET_LIST_BIT(c);
            }
        }
        had_char_class = 1;
        last = -1;
    }

    public final void charset_S() {
        for(c = 0; c < 256; c++) {
            if(!Character.isWhitespace(c)) {
                SET_LIST_BIT(c);
            }
        }
        if(ctx.current_mbctype>0) {
            set_list_bits(0x80, 0xffffffff, b, bix);
        }
        had_char_class = 1;
        last = -1;
    }

    public final void charset_d() {
        for(c = '0'; c <= '9'; c++) {
            SET_LIST_BIT(c);
        }
        had_char_class = 1;
        last = -1;
    }

    public final void charset_D() {
        for(c = 0; c < 256; c++) {
            if(!Character.isDigit(c)) {
                SET_LIST_BIT(c);
            }
        }
        if(ctx.current_mbctype>0) {
            set_list_bits(0x80, 0xffffffff, b, bix);
        }
        had_char_class = 1;
        last = -1;
    }

    public final void charset_x() {
        c = (char)scan_hex(p, pix, 2, numlen);
        if(numlen[0] == 0) {
            err("Invalid escape character syntax");
        }
        pix += numlen[0];
        had_num_literal = 1;
    }

    public final void charset_digit() {
        pix--;
        c = (char)scan_oct(p, pix, 3, numlen);
        pix += numlen[0];
        had_num_literal = 1;
    }

    public final void charset_special() {
        --pix;
        c = (char)read_special(p, pix, pend, numlen);
        if(c > 255) {
            err("Invalid escape character syntax");
        }
        pix = numlen[0];
        had_num_literal = 1;
    }

    public final void charset_default() {
        c = read_backslash(c);
        if(ismbchar(c,ctx)) {
            if(pix + mbclen(c,ctx) - 1 >= pend) {
                err("premature end of regular expression");
            }
            c = self.MBC2WC(c, p, pix);
            pix += mbclen(c,ctx) - 1;
            had_mbchar++;
        }
    }

    private final static byte[] ALNUM_BYTES = new byte[]{'a','l','n','u','m'};
    private final static byte[] ALPHA_BYTES = new byte[]{'a','l','p','h','a'};
    private final static byte[] BLANK_BYTES = new byte[]{'b','l','a','n','k'};
    private final static byte[] CNTRL_BYTES = new byte[]{'c','n','t','r','l'};
    private final static byte[] DIGIT_BYTES = new byte[]{'d','i','g','i','t'};
    private final static byte[] GRAPH_BYTES = new byte[]{'g','r','a','p','h'};
    private final static byte[] LOWER_BYTES = new byte[]{'l','o','w','e','r'};
    private final static byte[] PRINT_BYTES = new byte[]{'p','r','i','n','t'};
    private final static byte[] PUNCT_BYTES = new byte[]{'p','u','n','c','t'};
    private final static byte[] SPACE_BYTES = new byte[]{'s','p','a','c','e'};
    private final static byte[] UPPER_BYTES = new byte[]{'u','p','p','e','r'};
    private final static byte[] XDIGIT_BYTES = new byte[]{'x','d','i','g','i','t'};

    public final void charset_posixclass(byte[] _str, int start, int length) {
        int ch;
        boolean is_alnum = memcmp(_str, start, ALNUM_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_alpha = memcmp(_str, start, ALPHA_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_blank = memcmp(_str, start, BLANK_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_cntrl = memcmp(_str, start, CNTRL_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_digit = memcmp(_str, start, DIGIT_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_graph = memcmp(_str, start, GRAPH_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_lower = memcmp(_str, start, LOWER_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_print = memcmp(_str, start, PRINT_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_punct = memcmp(_str, start, PUNCT_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_space = memcmp(_str, start, SPACE_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_upper = memcmp(_str, start, UPPER_BYTES, 0, Math.min(length,5)) == 0;
        boolean is_xdigit= memcmp(_str, start,XDIGIT_BYTES, 0, Math.min(length,6)) == 0;

        if (!(is_alnum || is_alpha || is_blank || is_cntrl ||
              is_digit || is_graph || is_lower || is_print ||
              is_punct || is_space || is_upper || is_xdigit)){
            err("invalid regular expression; [:"+_str+":] is not a character class");
        }
                                
        /* Throw away the ] at the end of the character class.  */
                                    
        PATFETCH();

        if(pix == pend)  {
            err("invalid regular expression; range doesn't have ending ']' after a character class");
        }

        for(ch = 0; ch < 256; ch++) {
            if(      (is_alnum  && Character.isLetterOrDigit(ch))
                     || (is_alpha  && Character.isLetter(ch))
                     || (is_blank  && (ch == ' ' || ch == '\t'))
                     || (is_cntrl  && Character.isISOControl(ch))
                     || (is_digit  && Character.isDigit(ch))
                     || (is_graph  && (!Character.isWhitespace(ch) && !Character.isISOControl(ch)))
                     || (is_lower  && Character.isLowerCase(ch))
                     || (is_print  && (' ' == ch || (!Character.isWhitespace(ch) && !Character.isISOControl(ch))))
                     || (is_punct  && (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch) && !Character.isISOControl(ch)))
                     || (is_space  && Character.isWhitespace(ch))
                     || (is_upper  && Character.isUpperCase(ch))
                     || (is_xdigit && HEXDIGIT.indexOf(ch) != -1)) {
                SET_LIST_BIT((char)ch);
            }
        }
        had_char_class = 1;
    }

    public final void charset_range() {
        if(last > c) {
            err("invalid regular expression");
        }
        range = 0;
        if(had_mbchar == 0) {
            if((optz & RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null) {
                for (;last<=c;last++) {
                    SET_LIST_BIT((char)(ctx.translate[last]&0xFF));
                }
            } else {
                for(;last<=c;last++) {
                    SET_LIST_BIT((char)last);
                }
            }
        } else if (had_mbchar == 2) {
            set_list_bits(last, c, b, bix);
        } else {
            /* restriction: range between sbc and mbc */
            err("invalid regular expression");
        }
    }

    public final void charset_range_trans() {
        if(((optz & RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null) && c < 0x100) {
            c = ctx.translate[c];
        }
        if(had_mbchar == 0 && (ctx.current_mbctype == 0 || had_num_literal == 0)) {
            SET_LIST_BIT(c);
            had_num_literal = 0;
        } else {
            set_list_bits(c, c, b, bix);
        }
    }

    public final void main_charset() {
        boolean gotoRangeRepeat=false;
        int size;
        last = -1;

        /* Read in characters and ranges, setting map bits.  */
        charsetLoop: for (;;) {
            if(!gotoRangeRepeat) {
                size = -1;
                last = -1;
                if((size = EXTRACT_UNSIGNED(b,bix+32))!=0 || ctx.current_mbctype!=0) {
                    /* Ensure the space is enough to hold another interval
                       of multi-byte chars in charset(_not)?.  */
                    size = 32 + 2 + size*8 + 8;
                    while(bix + size + 1 > self.allocated) {
                        EXTEND_BUFFER();
                    }
                }
            } else {
                gotoRangeRepeat = false;
            }

            if(range>0 && had_char_class>0) {
                err("invalid regular expression; can't use character class as an end value of range");
            }
            PATFETCH_RAW();

            if(c == ']') {
                if(pix == p0 + 1) {
                    if(pix == pend) {
                        err("invalid regular expression; empty character class");
                    }
                    self.re_warning("character class has `]' without escape");
                } else {
                    /* Stop if this isn't merely a ] inside a bracket
                       expression, but rather the end of a bracket
                       expression.  */
                    break charsetLoop;
                }
            }
            /* Look ahead to see if it's a range when the last thing
               was a character class.  */
            if(had_char_class > 0 && c == '-' && p[pix] != ']') {
                err("invalid regular expression; can't use character class as a start value of range");
            }
            if(ismbchar(c,ctx)) {
                if(pix + mbclen(c,ctx) - 1 >= pend) {
                    err("premature end of regular expression");
                }
                c = self.MBC2WC(c, p, pix);
                pix += mbclen(c,ctx) - 1;
                had_mbchar++;
            }
            had_char_class = 0;

            if(c == '-' && ((pix != p0 + 1 && pix < pend && p[pix] != ']') ||
                            (pix+2<pend && p[pix] == '-' && p[pix+1] != ']') ||
                            range>0)) {
                self.re_warning("character class has `-' without escape");
            }
            if(c == '[' && p[pix] != ':') {
                self.re_warning("character class has `[' without escape");
            }


            /* \ escapes characters when inside [...].  */
            if(c == '\\') {
                PATFETCH_RAW();
                switch(c) {
                case 'w':
                    charset_w();
                    continue charsetLoop;
                case 'W':
                    charset_W();
                    continue charsetLoop;
                case 's':
                    charset_s();
                    continue charsetLoop;
                case 'S':
                    charset_S();
                    continue charsetLoop;
                case 'd':
                    charset_d();
                    continue charsetLoop;
                case 'D':
                    charset_D();
                    continue charsetLoop;
                case 'x':
                    charset_x();
                    break;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    charset_digit();
                    break;
                case 'M':
                case 'C':
                case 'c':
                    charset_special();
                    break;
                default:
                    charset_default();
                    break;
                }
            } else if(c == '[' && p[pix] == ':') { /* [:...:] */
                /* Leave room for the null.  */
                byte[] str = new byte[7];
                PATFETCH_RAW();
                c1 = 0;

                /* If pattern is `[[:'.  */
                if(pix == pend) {
                    err("invalid regular expression; re can't end '[[:'");
                }

                for(;;) {
                    PATFETCH_RAW();
                    if(c == ':' || c == ']' || pix == pend || c1 == 6) {
                        break;
                    }
                    str[c1++] = (byte)c;
                }

                /* If isn't a word bracketed by `[:' and `:]':
                   undo the ending character, the letters, and
                   the leading `:' and `['.  */
                                
                if(c == ':' && p[pix] == ']') {
                    charset_posixclass(str,0,c1);
                    continue charsetLoop;
                } else {
                    c1 += 2;
                    pix -= c1;
                    self.re_warning("character class has `[' without escape");
                    c = '[';
                }
            }

            /* Get a range.  */
            if(range > 0) {
                charset_range();
            } else if(pix+2<pend && p[pix] == '-' && p[pix+1] != ']') {
                last = c;
                PATFETCH_RAW_c1();
                range = 1;
                gotoRangeRepeat = true;
                continue charsetLoop;
            } else {
                charset_range_trans();
            }
            had_mbchar = 0;
        }
    }

    public final void compact_charset() {
        /* Discard any character set/class bitmap bytes that are all
           0 at the end of the map. Decrement the map-length byte too.  */
        while(b[bix-1] > 0 && b[bix+b[bix-1]-1] == 0) {
            b[bix-1]--; 
        }
        if(b[bix-1] != 32) {
            System.arraycopy(b,bix+32,b,bix+b[bix-1],2+EXTRACT_UNSIGNED(b,bix+32)*8);
        }
        bix += b[bix-1] + 2 + EXTRACT_UNSIGNED(b,bix+b[bix-1])*8;
        had_num_literal = 0;
    }

    public int old_options;
    public int push_option = 0;
    public int casefold = 0;

    public final void group_settings() {
        boolean negative = false;

        if(pix == pend) {
            err("premature end of regular expression");
        }
                        
        c = (char)(p[pix++]&0xFF);

        switch (c) {
        case 'x': case 'm': case 'i': case '-':
            for(;;) {
                switch (c) {
                case '-':
                    negative = true;
                    break;
                case ':':
                case ')':
                    break;
                case 'x':
                    if(negative) {
                        optz &= ~RE_OPTION_EXTENDED;
                    } else {
                        optz |= RE_OPTION_EXTENDED;
                    }
                    break;
                case 'm':
                    if(negative) {
                        if((optz&RE_OPTION_MULTILINE) != 0) {
                            optz &= ~RE_OPTION_MULTILINE;
                        }
                    }else if ((optz&RE_OPTION_MULTILINE) == 0) {
                        optz |= RE_OPTION_MULTILINE;
                    }
                    push_option = 1;
                    break;
                case 'i':
                    if(negative) {
                        if((optz&RE_OPTION_IGNORECASE) != 0) {
                            optz &= ~RE_OPTION_IGNORECASE;
                        }
                    } else if ((optz&RE_OPTION_IGNORECASE) == 0) {
                        optz |= RE_OPTION_IGNORECASE;
                    }
                    casefold = 1;
                    break;
                default:
                    err("undefined (?...) inline option");
                }
                if(c == ')') {
                    c = '#';	/* read whole in-line options */
                    break;
                }
                if(c == ':') {
                    break;
                }
                                
                if(pix == pend) {
                    err("premature end of regular expression");
                }
                c = (char)(p[pix++]&0xFF);
            }
            break;

        case '#':
            for(;;) {
                if(pix == pend) {
                    err("premature end of regular expression");
                }
                c = (char)(p[pix++]&0xFF);
                if((optz & RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null) {
                    c = ctx.translate[c];
                }
                if(c == ')') {
                    break;
                }
            }
            c = '#';
            break;

        case ':':
        case '=':
        case '!':
        case '>':
            break;
        default:
            err("undefined (?...) sequence");
        }
    }

    public final void group_start() {
        old_options = (int)optz;
        push_option = 0;
        casefold = 0;
        PATFETCH();
            
        if(c == '?') {
            group_settings();
        } else {
            pix--;
            c = '(';
        }
    }

    public final void DOUBLE_STACK() {
        int[] stackx;
        int xlen = stacke;
        stackx = new int[2*xlen];
        System.arraycopy(stackb,0,stackx,0,xlen);
        stackb = stackx;
        stacke = 2*xlen;
    }

    public final void group_start_end() {
        if(stackp+8 >= stacke) {
            DOUBLE_STACK();
        }

        /* Laststart should point to the start_memory that we are about
           to push (unless the pattern has RE_NREGS or more ('s).  */
        /* obsolete: now RE_NREGS is just a default register size. */
        stackb[stackp++] = bix;    
        stackb[stackp++] = fixup_alt_jump != -1 ? fixup_alt_jump - 0 + 1 : 0;
        stackb[stackp++] = begalt;
        switch(c) {
        case '(':
            BUFPUSH(start_memory);
            BUFPUSH((byte)regnum);
            stackb[stackp++] = regnum++;
            stackb[stackp++] = bix;
            BUFPUSH((byte)0);
            /* too many ()'s to fit in a byte. (max 254) */
            if(regnum >= 255) {
                err("regular expression too big");
            }
            break;
        case '=':
        case '!':
        case '>':
            BUFPUSH(start_nowidth);
            stackb[stackp++] = bix;
            BUFPUSH((byte)0);
            BUFPUSH((byte)0);
            if(c != '!') {
                break;
            }
            BUFPUSH(on_failure_jump);
            stackb[stackp++] = bix;
            BUFPUSH((byte)0);
            BUFPUSH((byte)0);
            break;
        case ':':
            BUFPUSH(start_paren);
            pending_exact = -1;
        default:
            break;
        }
        if(push_option != 0) {
            BUFPUSH(option_set);
            BUFPUSH((byte)optz);
        }
        if(casefold != 0) {
            if((optz & RE_OPTION_IGNORECASE)!=0) {
                BUFPUSH(casefold_on);
            } else {
                BUFPUSH(casefold_off);
            }
        }
        stackb[stackp++] = c;
        stackb[stackp++] = old_options;
        fixup_alt_jump = -1;
        laststart = -1;
        begalt = bix;
    }

    public final void group_end() {
        if(stackp == 0) { 
            err("unmatched )");
        }

        pending_exact = -1;
        if(fixup_alt_jump != -1) {
            /* Push a dummy failure point at the end of the
               alternative for a possible future
               `finalize_jump' to pop.  See comments at
               `push_dummy_failure' in `re_match'.  */
            BUFPUSH(push_dummy_failure);
                        
            /* We allocated space for this jump when we assigned
               to `fixup_alt_jump', in the `handle_alt' case below.  */
            store_jump(b, fixup_alt_jump, jump, bix);
        }
        if(optz != stackb[stackp-1]) {
            if (((optz ^ stackb[stackp-1]) & RE_OPTION_IGNORECASE) != 0) {
                BUFPUSH((optz&RE_OPTION_IGNORECASE) != 0 ? casefold_off:casefold_on);
            }
            if ((optz ^ stackb[stackp-1]) != RE_OPTION_IGNORECASE) {
                BUFPUSH(option_set);
                BUFPUSH((byte)stackb[stackp-1]);
            }
        }
        p0 = bix;
        optz = stackb[--stackp];
        switch(c = (char)(((byte)stackb[--stackp])&0xFF)) {
        case '(': {
            int v1 = stackb[--stackp];
            self.buffer[v1] = (byte)(regnum - stackb[stackp-1]);
            GET_BUFFER_SPACE(3);
            b[bix++] = stop_memory;
            b[bix++] = (byte)stackb[stackp-1];
            b[bix++] = (byte)(regnum - stackb[stackp-1]);
            stackp--;
        }
            break;

        case '!':
            BUFPUSH(pop_and_fail);
            /* back patch */

            STORE_NUMBER(self.buffer,stackb[stackp-1], bix - stackb[stackp-1] - 2);
            stackp--;
            /* fall through */
        case '=':
            BUFPUSH(stop_nowidth);
            /* tell stack-pos place to start_nowidth */
            STORE_NUMBER(self.buffer,stackb[stackp-1], bix - stackb[stackp-1] - 2);
            BUFPUSH((byte)0); /* space to hold stack pos */
            BUFPUSH((byte)0);
            stackp--;
            break;
        case '>':
            BUFPUSH(stop_backtrack);
            /* tell stack-pos place to start_nowidth */
            STORE_NUMBER(self.buffer,stackb[stackp-1], bix - stackb[stackp-1] - 2);
            BUFPUSH((byte)0); /* space to hold stack pos */
            BUFPUSH((byte)0);
            stackp--;
            break;
        case ':':
            BUFPUSH(stop_paren);
            break;
        default:
            break;
        }
        begalt = stackb[--stackp];
        stackp--;
        fixup_alt_jump = stackb[stackp] != 0 ? stackb[stackp]  - 1 : -1;
        laststart = stackb[--stackp];
        if(c == '!' || c == '=') {
            laststart = bix;
        }
    }

    public final void alt() {
        /* Insert before the previous alternative a jump which
           jumps to this alternative if the former fails.  */
        GET_BUFFER_SPACE(3);
        insert_jump(on_failure_jump, b, begalt, bix + 6, bix);
        pending_exact = -1;
        bix += 3;
        /* The alternative before this one has a jump after it
           which gets executed if it gets matched.  Adjust that
           jump so it will jump to this alternative's analogous
           jump (put in below, which in turn will jump to the next
           (if any) alternative's such jump, etc.).  The last such
           jump jumps to the correct final destination.  A picture:
           _____ _____ 
           |   | |   |   
           |   v |   v 
           a | b   | c   
                       
           If we are at `b', then fixup_alt_jump right now points to a
           three-byte space after `a'.  We'll put in the jump, set
           fixup_alt_jump to right after `b', and leave behind three
           bytes which we'll fill in when we get to after `c'.  */

        if(fixup_alt_jump != -1) {
            store_jump(b, fixup_alt_jump, jump_past_alt, bix);
        }

        /* Mark and leave space for a jump after this alternative,
           to be filled in later either by next alternative or
           when know we're at the end of a series of alternatives.  */
        fixup_alt_jump = bix;
        GET_BUFFER_SPACE(3);
        bix += 3;
        laststart = -1;
        begalt = bix;
    }

    public final void start_bounded_repeat() {
        /* If there is no previous pattern, this is an invalid pattern.  */
        if(laststart == -1) {
            err("invalid regular expression; there's no previous pattern, to which '{' would define cardinality at " + pix);
        }
        if(pix == pend) {
            err("invalid regular expression; '{' can't be last character");
        }

        beg_interval = pix - 1;

        lower_bound = -1;			/* So can see if are set.  */
        upper_bound = -1;

        if(pix != pend) {
            PATFETCH();
            while(Character.isDigit(c)) {
                if(lower_bound < 0) {
                    lower_bound = 0;
                }
                lower_bound = lower_bound * 10 + c - '0';
                if(pix == pend) {
                    break;
                }
                PATFETCH();
            }
        } 	

        if(c == ',') {
            if(pix != pend) {
                PATFETCH();
                while(Character.isDigit(c)) {
                    if(upper_bound < 0) {
                        upper_bound = 0;
                    }
                    upper_bound = lower_bound * 10 + c - '0';
                    if(pix == pend) {
                        break;
                    }
                    PATFETCH();
                }
            } 	
        } else {
            /* Interval such as `{1}' => match exactly once. */
            upper_bound = lower_bound;
        }
    }

    public final int continue_bounded_repeat() {
        if(lower_bound >= RE_DUP_MAX || upper_bound >= RE_DUP_MAX) {
            err("too big quantifier in {,}");
        }
        if(upper_bound < 0) {
            upper_bound = RE_DUP_MAX;
        }
        if(lower_bound > upper_bound) {
            err("can't do {n,m} with n > m");
        }

        beg_interval = 0;
        pending_exact = 0;
                    
        greedy = true;

        if(pix != pend) {
            PATFETCH();
            if(c == '?') {
                greedy = false;
            } else {
                pix--;
            }
        }

        if(lower_bound == 0) {
            zero_times_ok = true;
            if(upper_bound == RE_DUP_MAX) {
                many_times_ok = true;
                gotoRepeat = true;
                c = '*';
                return 1;
            }
            if(upper_bound == 1) {
                many_times_ok = false;
                gotoRepeat = true;
                c = '*';
                return 1;
            }
        }
        if(lower_bound == 1) {
            if(upper_bound == 1) {
                /* No need to repeat */
                return 2;
            }
            if(upper_bound == RE_DUP_MAX) {
                many_times_ok = true;
                zero_times_ok = false;
                gotoRepeat = true;
                c = '*';
                return 1;
            }
        }

        /* If upper_bound is zero, don't want to succeed at all; 
           jump from laststart to b + 3, which will be the end of
           the buffer after this jump is inserted.  */

        if(upper_bound == 0) {
            GET_BUFFER_SPACE(3);
            insert_jump(jump, b, laststart, bix + 3, bix);
            bix += 3;
            return 2;
        }

        /* If lower_bound == upper_bound, repeat count can be removed */
        if(lower_bound == upper_bound) {
            int mcnt;
            int skip_stop_paren = 0;

            if(b[bix-1] == stop_paren) {
                skip_stop_paren = 1;
                bix--;
            }

            if (b[laststart] == exactn && b[laststart+1]+2 == bix - laststart && b[laststart+1]*lower_bound < 256) {
                mcnt = b[laststart+1];
                GET_BUFFER_SPACE((lower_bound-1)*mcnt);
                b[laststart+1] = (byte)(lower_bound*mcnt);
                while(--lower_bound > 0) {
                    System.arraycopy(b,laststart+2,b,bix,mcnt);
                    bix+=mcnt;
                }
                if(skip_stop_paren != 0) {
                    BUFPUSH(stop_paren);
                }
                return 2;
            }

            if(lower_bound < 5 && bix - laststart < 10) {
                /* 5 and 10 are the magic numbers */
                mcnt = bix - laststart;
                GET_BUFFER_SPACE((lower_bound-1)*mcnt);
                while(--lower_bound > 0) {
                    System.arraycopy(b, laststart, b, bix, mcnt);
                    bix+=mcnt;
                }
                if(skip_stop_paren!=0) {
                    BUFPUSH(stop_paren);
                }
                return 2;
            }
            if(skip_stop_paren!=0) {
                bix++; /* push back stop_paren */
            }
        }
        return 0;
    }

    public final void bounded_nontrivial() {
        /* Otherwise, we have a nontrivial interval.  When
           we're all done, the pattern will look like:
           set_number_at <jump count> <upper bound>
           set_number_at <succeed_n count> <lower bound>
           succeed_n <after jump addr> <succed_n count>
           <body of loop>
           jump_n <succeed_n addr> <jump count>
           (The upper bound and `jump_n' are omitted if
           `upper_bound' is 1, though.)  */
        /* If the upper bound is > 1, we need to insert
           more at the end of the loop.  */
        int nbytes = upper_bound == 1 ? 10 : 20;
        GET_BUFFER_SPACE(nbytes);

        /* Initialize lower bound of the `succeed_n', even
           though it will be set during matching by its
           attendant `set_number_at' (inserted next),
           because `re_compile_fastmap' needs to know.
           Jump to the `jump_n' we might insert below.  */
        insert_jump_n(succeed_n, b, laststart, bix + (nbytes/2), bix, lower_bound);
        bix += 5; 	/* Just increment for the succeed_n here.  */
                        
        /* Code to initialize the lower bound.  Insert 
           before the `succeed_n'.  The `5' is the last two
           bytes of this `set_number_at', plus 3 bytes of
           the following `succeed_n'.  */
        insert_op_2(set_number_at, b, laststart, bix, 5, lower_bound);
        bix += 5;

        if(upper_bound > 1) {
            /* More than one repetition is allowed, so
               append a backward jump to the `succeed_n'
               that starts this interval.
                               
               When we've reached this during matching,
               we'll have matched the interval once, so
               jump back only `upper_bound - 1' times.  */
            GET_BUFFER_SPACE(5);
            store_jump_n(b, bix, greedy?jump_n:finalize_push_n, laststart + 5, upper_bound - 1);
            bix += 5;

            /* The location we want to set is the second
               parameter of the `jump_n'; that is `b-2' as
               an absolute address.  `laststart' will be
               the `set_number_at' we're about to insert;
               `laststart+3' the number to set, the source
               for the relative address.  But we are
               inserting into the middle of the pattern --
               so everything is getting moved up by 5.
               Conclusion: (b - 2) - (laststart + 3) + 5,
               i.e., b - laststart.

               We insert this at the beginning of the loop
               so that if we fail during matching, we'll
               reinitialize the bounds.  */
            insert_op_2(set_number_at, b, laststart, bix, bix - laststart, upper_bound - 1);
            bix += 5;
        }
    }
        
    public final void unfetch_interval() {
        // unfetch_interval:
        /* If an invalid interval, match the characters as literals.  */
        self.re_warning("regexp has invalid interval");
        pix = beg_interval;
        beg_interval = 0;
        /* normal_char and normal_backslash need `c'.  */
        PATFETCH();
        gotoNormalChar = true;
    }

    public final void escape() {
        if(pix == pend) {
            err("invalid regular expression; '\\' can't be last character");
        }
        /* Do not translate the character after the \, so that we can
           distinguish, e.g., \B from \b, even if we normally would
           translate, e.g., B to b.  */
        c = (char)(p[pix++]&0xFF);
        switch (c) {
        case 's':
        case 'S':
        case 'd':
        case 'D':
            while(bix + 9 + 32 > self.allocated) {
                EXTEND_BUFFER();
            }

            laststart = bix;
            if(c == 's' || c == 'd') {
                b[bix++] = charset;
            } else {
                b[bix++] = charset_not;
            }
            b[bix++] = 32;
            Arrays.fill(b,bix,bix+34,(byte)0);
            if(c == 's' || c == 'S') {
                SET_LIST_BIT(' ');
                SET_LIST_BIT('\t');
                SET_LIST_BIT('\n');
                SET_LIST_BIT('\r');
                SET_LIST_BIT('\f');
            } else {
                char cc;
                for (cc = '0'; cc <= '9'; cc++) {
                    SET_LIST_BIT(cc);
                }
            }

            while(b[bix-1] > 0 && b[bix+b[bix-1]-1] == 0) { 
                b[bix-1]--;
            }
            if(b[bix-1] != 32) {
                System.arraycopy(b,bix+32,b,bix+b[bix-1],  2 + EXTRACT_UNSIGNED(b,bix+32)*8);
            }
            bix += b[bix-1] + 2 + EXTRACT_UNSIGNED(b, bix+b[bix-1])*8;
            break;
        case 'w':
            laststart = bix;
            BUFPUSH(wordchar);
            break;
        case 'W':
            laststart = bix;
            BUFPUSH(notwordchar);
            break;

            /******************* NOT IN RUBY
                    case '<':
                        SH(wordbeg);
                        break;

                    case '>':
                        SH(wordend);
                        break;
            ********************/

        case 'b':
            BUFPUSH(wordbound);
            break;

        case 'B':
            BUFPUSH(notwordbound);
            break;

        case 'A':
            BUFPUSH(begbuf);
            break;

        case 'Z':
            if((self.options & RE_OPTION_SINGLELINE) == 0) {
                BUFPUSH(endbuf2);
                break;
            }
            /* fall through */
        case 'z':
            BUFPUSH(endbuf);
            break;

        case 'G':
            BUFPUSH(begpos);
            break;

            /* hex */
        case 'x':
            had_mbchar = 0;
            c = (char)scan_hex(p, pix, 2, numlen);
            if(numlen[0] == 0) {
                err("Invalid escape character syntax");
            }
            pix += numlen[0];
            had_num_literal = 1;
            gotoNumericChar = true;
            return;

            /* octal */
        case '0':
            had_mbchar = 0;
            c = (char)scan_oct(p, pix, 2, numlen);
            pix += numlen[0];
            had_num_literal = 1;
            gotoNumericChar = true;
            return;

            /* back-ref or octal */
        case '1': case '2': case '3':
        case '4': case '5': case '6':
        case '7': case '8': case '9':
            pix--;
            p0 = pix;
            had_mbchar = 0;
            c1 = 0;

            if(pix != pend) {
                PATFETCH();
                while(Character.isDigit(c)) {
                    if(c1 < 0) {
                        c1 = 0;
                    }
                    c1 = (char)(c1 * 10 + c - '0');
                    if(pix == pend) {
                        break;
                    }
                    PATFETCH();
                }
            } 	

            if(!Character.isDigit(c)) {
                pix--;
            }
                        
            if(9 < c1 && c1 >= regnum) {
                /* need to get octal */
                c = (char)(scan_oct(p, p0, 3, numlen) & 0xff);
                pix = p0 + numlen[0];
                c1 = 0;
                had_num_literal = 1;
                gotoNumericChar = true;
                return;
            }

            laststart = bix;
            BUFPUSH(duplicate);
            BUFPUSH((byte)c1);
            break;

        case 'M':
        case 'C':
        case 'c':
            p0 = --pix;
            int[] p00 = new int[]{p0};
            c = (char)read_special(p, pix, pend, p00);
            p0 = p00[0];
            if(c > 255) {
                err("Invalid escape character syntax");
            }
            pix = p0;
            had_num_literal = 1;
            gotoNumericChar = true;
            return;
        default:
            c = read_backslash(c);
            gotoNormalChar = true;
        }
    }

    public final void handle_num_or_normal() {
        if(gotoNormalChar) {
            /* Expects the character in `c'.  */
            had_mbchar = 0;
            if(ismbchar(c,ctx)) {
                had_mbchar = 1;
                c1 = (char)pix;
            }
        }
        if(gotoNormalChar || gotoNumericChar) {
            nextp = pix + mbclen(c,ctx) - 1;

            if(pending_exact==-1 || pending_exact + b[pending_exact] + 1 != bix || 
               b[pending_exact] >= (c1!=0 ? 0176 : 0177) || (nextp < pend &&
                                                             (p[nextp] == '+' || p[nextp] == '?'
                                                              || p[nextp] == '*' || p[nextp] == '^'
                                                              || p[nextp] == '{'))) {
                laststart = bix;
                BUFPUSH(exactn);
                pending_exact = bix;
                BUFPUSH((byte)0);
            }
            if(had_num_literal!=0 || c == 0xff) {
                BUFPUSH((byte)0xFF);
                b[pending_exact]++;
                had_num_literal = 0;
            }
            BUFPUSH(c);
            b[pending_exact]++;
            if(had_mbchar!=0) {
                int len = mbclen(c,ctx) - 1;
                while(len-- > 0) {
                    if(pix == pend) {
                        err("premature end of regular expression");
                    }
                    c = (char)(p[pix++]&0xFF);
                    BUFPUSH(c);
                    b[pending_exact]++;
                }
            }

            gotoNormalChar = false;
            gotoNumericChar = false;
        }
    }

    public final void finalize_compilation() {
        if(fixup_alt_jump!=-1) {
            store_jump(b, fixup_alt_jump, jump, bix);
        }
        if(stackp > 0) {
            err("unmatched (");
        }

        /* set optimize flags */
        laststart = 0;
        if(laststart != bix) {
            if(b[laststart] == dummy_failure_jump) {
                laststart += 3;
            } else if(b[laststart] == try_next) {
                laststart += 3;
            }
            if(b[laststart] == anychar_repeat) {
                self.options |= RE_OPTIMIZE_ANCHOR;
            }
        }
        self.used = bix;
        self.re_nsub = regnum;
        laststart = 0;
        if(laststart != bix) {
            if(b[laststart] == start_memory) {
                laststart += 3;
            }
            if(b[laststart] == exactn) {
                self.options |= RE_OPTIMIZE_EXACTN;
                self.must = laststart+1;
            }
        }
        if(self.must==-1) {
            self.must = calculate_must_string(self.buffer, bix);
        }

        if(ctx.current_mbctype == MBCTYPE_SJIS) {
            self.options |= RE_OPTIMIZE_NO_BM;
        } else if(self.must != -1) {
            int i;
            int len = self.buffer[self.must];

            for(i=1; i<len && i < len; i++) {
                if(self.buffer[self.must+i] == (byte)0xff ||
                   (ctx.current_mbctype!=0 && ismbchar(self.buffer[self.must+i],ctx))) {
                    self.options |= RE_OPTIMIZE_NO_BM;
                    break;
                }
            }
            if((self.options & RE_OPTIMIZE_NO_BM) == 0) {
                self.must_skip = new int[256];
                bm_init_skip(self.must_skip, self.buffer, self.must+1,
                             self.buffer[self.must],
                             (((self.options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0) && ctx.translate!=null)?ctx.translate:null);
            }
        }

        /*
          System.err.println("compiled into pattern of length: " + self.used);
          for(int i=0;i<self.used;i++) {
          System.err.print(""+(int)self.buffer[i] + " ");
          }
          System.err.println();
        */

        for(int i=0;i<self.pool.length;i++) {
            self.pool[i] = new int[(regnum*NUM_REG_ITEMS + NUM_NONREG_ITEMS)*NFAILURES];
        }
    }


    private final void SET_LIST_BIT(char c) {
        b[bix+c/8] |= 1 << (c%8);
    }

    private final void GET_BUFFER_SPACE(int n) {
        while(bix+n >= self.allocated) {
            EXTEND_BUFFER();
        }
    }

    private final void EXTEND_BUFFER() {
        byte[] old_buffer = self.buffer;
        self.allocated *= 2;
        self.buffer = new byte[self.allocated];
        b = self.buffer;
        System.arraycopy(old_buffer,0,self.buffer,0,old_buffer.length);
    }

    private final static void bm_init_skip(int[] skip, byte[] patb, int patix, int m, char[] trans) {
        int j, c;
        
        for(c=0; c<256; c++) {
            skip[c] = m;
        }
        if(trans != null) {
            for (j=0; j<m-1; j++) {
                skip[trans[patb[patix+j]&0xFF]] = m-1-j;
            }
        } else {
            for(j=0; j<m-1; j++) {
                skip[patb[patix+j]&0xFF] = m-1-j;
            }
        }
    }

    private final static int calculate_must_string(byte[] start, int end) {
        int mcnt;
        int max = 0;
        int p = 0;
        int pend = end;
        int must = -1;
        if(null == start || start.length == 0) {
            return -1;
        }

        while(p<pend) {
            switch(start[p++]&0xFF) {
            case unused:
                break;
            case exactn:
                mcnt = start[p]&0xFF;
                if(mcnt > max) {
                    must = p;
                    max = mcnt;
                }
                p += mcnt+1;
                break;
            case start_memory:
            case stop_memory:
                p += 2;
                break;
            case duplicate:
            case option_set:
                p++;
                break;
            case casefold_on:
            case casefold_off:
                return -1;		/* should not check must_string */
            case pop_and_fail:
            case anychar:
            case anychar_repeat:
            case begline:
            case endline:
            case wordbound:
            case notwordbound:
            case wordbeg:
            case wordend:
            case wordchar:
            case notwordchar:
            case begbuf:
            case endbuf:
            case endbuf2:
            case begpos:
            case push_dummy_failure:
            case start_paren:
            case stop_paren:
                break;
            case charset:
            case charset_not:
                mcnt = start[p++]&0xFF;
                p += mcnt;
                mcnt = EXTRACT_UNSIGNED(start, p);
                p+=2;
                while(mcnt-- > 0) {
                    p += 8;
                }
                break;
            case on_failure_jump:
                mcnt = EXTRACT_NUMBER(start, p);
                p+=2;
                if(mcnt > 0) {
                    p += mcnt;
                }
                if(start[p-3] == jump) {
                    p -= 2;
                    mcnt = EXTRACT_NUMBER(start, p);
                    p+=2;
                    if(mcnt > 0) {
                        p += mcnt;
                    }
                }
                break;
            case dummy_failure_jump:
            case succeed_n: 
            case try_next:
            case jump:
                mcnt = EXTRACT_NUMBER(start, p);
                p+=2;
                if(mcnt > 0) {
                    p += mcnt;
                }
                break;
            case start_nowidth:
            case stop_nowidth:
            case stop_backtrack:
            case finalize_jump:
            case maybe_finalize_jump:
            case finalize_push:
                p += 2;
                break;
            case jump_n: 
            case set_number_at: 
            case finalize_push_n:
                p += 4;
                break;
            default:
                break;
            }
        }

        return must;
    }
}

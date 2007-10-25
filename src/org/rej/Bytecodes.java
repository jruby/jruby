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

abstract class Bytecodes {
    private Bytecodes(){}

    public final static String[] NAMES = {
        "unused",
        "exactn",
        "begline",
        "endline",
        "begbuf",
        "endbuf",
        "endbuf2",
        "begpos",
        "jump",
        "jump_past_alt",
        "on_failure_jump",
        "finalize_jump",
        "maybe_finalize_jump",
        "dummy_failure_jump",
        "push_dummy_failure",
        "succeed_n",
        "jump_n",
        "try_next",
        "finalize_push",
        "finalize_push_n",
        "set_number_at",
        "anychar",
        "anychar_repeat",
        "charset",
        "charest_not",
        "start_memory",
        "stop_memory",
        "start_paren",
        "stop_paren",
        "casefold_on",
        "casefold_off",
        "option_set",
        "start_nowidth",
        "stop_nowidth",
        "pop_and_fail",
        "stop_backtrack",
        "duplicate",
        "wordchar",
        "notwordchar",
        "wordbeg",
        "wordend",
        "wordbound",
        "notwordbound"};

    public static String describe(final byte[] pattern, final int start, final int length) {
        final int pend = start+length;
        int p = start;

        StringBuffer result = new StringBuffer();
        System.err.println("pattern is " + length + " bytes long");
        while(p < pend) {
            result.append("-").append(NAMES[pattern[p]]).append("\n");
            switch(pattern[p++]) {
            case anychar_repeat:
                break;
            case exactn: {
                int mcnt = pattern[p++]&0xFF;
                result.append(" ").append(mcnt).append(" characters to match").append("\n");
                result.append("  \"");
                for(int i=0;i<mcnt;i++) {
                    if(pattern[p] == (byte)0xff) {
                        p++;
                        result.append("\\").append(pattern[p++]&0xFF);
                    } else {
                        result.append((char)(pattern[p++]&0xFF));
                    }
                }
                result.append("\"\n");
                break;
            }
            default:
                result.append(" can't handle arguments for this node").append("\n");
                return result.toString();
            }
        }
        return result.toString();
    }

    /* These are the command codes that appear in compiled regular
       expressions, one per byte.  Some command codes are followed by
       argument bytes.  A command code can specify any interpretation
       whatsoever for its arguments.  Zero-bytes may appear in the compiled
       regular expression.*/
    public final static byte unused = 0;
    public final static byte exactn = 1; /* Followed by one byte giving n, then by n literal bytes.  */
    public final static byte begline = 2;  /* Fail unless at beginning of line.  */
    public final static byte endline = 3;  /* Fail unless at end of line.  */
    public final static byte begbuf = 4;   /* Succeeds if at beginning of buffer (if emacs) or at beginning
                                               of string to be matched (if not).  */
    public final static byte endbuf = 5;   /* Analogously, for end of buffer/string.  */
    public final static byte endbuf2 = 6;  /* End of buffer/string, or newline just before it.  */
    public final static byte begpos = 7;   /* Matches where last scan//gsub left off.  */
    public final static byte jump = 8;     /* Followed by two bytes giving relative address to jump to.  */
    public final static byte jump_past_alt = 9;/* Same as jump, but marks the end of an alternative.  */
    public final static byte on_failure_jump = 10;	 /* Followed by two bytes giving relative address of 
                                                        place to resume at in case of failure.  */
    public final static byte finalize_jump = 11;	 /* Throw away latest failure point and then jump to 
                                                        address.  */
    public final static byte maybe_finalize_jump = 12; /* Like jump but finalize if safe to do so.
                                                           This is used to jump back to the beginning
                                                           of a repeat.  If the command that follows
                                                           this jump is clearly incompatible with the
                                                           one at the beginning of the repeat, such that
                                                           we can be sure that there is no use backtracking
                                                           out of repetitions already completed,
                                                           then we finalize.  */
    public final static byte dummy_failure_jump = 13;  /* Jump, and push a dummy failure point. This 
                                                           failure point will be thrown away if an attempt 
                                                           is made to use it for a failure. A + construct 
                                                           makes this before the first repeat.  Also
                                                           use it as an intermediary kind of jump when
                                                           compiling an or construct.  */
    public final static byte push_dummy_failure = 14; /* Push a dummy failure point and continue.  Used at the end of
                                                          alternatives.  */
    public final static byte succeed_n = 15;	 /* Used like on_failure_jump except has to succeed n times;
                                                    then gets turned into an on_failure_jump. The relative
                                                    address following it is useless until then.  The
                                                    address is followed by two bytes containing n.  */
    public final static byte jump_n = 16;	 /* Similar to jump, but jump n times only; also the relative
                                                address following is in turn followed by yet two more bytes
                                                containing n.  */
    public final static byte try_next = 17;    /* Jump to next pattern for the first time,
                                                   leaving this pattern on the failure stack. */
    public final static byte finalize_push = 18;	/* Finalize stack and push the beginning of the pattern
                                                       on the stack to retry (used for non-greedy match) */
    public final static byte finalize_push_n = 19;	/* Similar to finalize_push, buf finalize n time only */
    public final static byte set_number_at = 20;	/* Set the following relative location to the
                                                       subsequent number.  */
    public final static byte anychar = 21;	 /* Matches any (more or less) one character excluding newlines.  */
    public final static byte anychar_repeat = 22;	 /* Matches sequence of characters excluding newlines.  */
    public final static byte charset = 23;     /* Matches any one char belonging to specified set.
                                                   First following byte is number of bitmap bytes.
                                                   Then come bytes for a bitmap saying which chars are in.
                                                   Bits in each byte are ordered low-bit-first.
                                                   A character is in the set if its bit is 1.
                                                   A character too large to have a bit in the map
                                                   is automatically not in the set.  */
    public final static byte charset_not = 24; /* Same parameters as charset, but match any character
                                                   that is not one of those specified.  */
    public final static byte start_memory = 25; /* Start remembering the text that is matched, for
                                                    storing in a memory register.  Followed by one
                                                    byte containing the register number.  Register numbers
                                                    must be in the range 0 through RE_NREGS.  */
    public final static byte stop_memory = 26; /* Stop remembering the text that is matched
                                                   and store it in a memory register.  Followed by
                                                   one byte containing the register number. Register
                                                   numbers must be in the range 0 through RE_NREGS.  */
    public final static byte start_paren = 27;    /* Place holder at the start of (?:..). */
    public final static byte stop_paren = 28;    /* Place holder at the end of (?:..). */
    public final static byte casefold_on = 29;   /* Turn on casefold flag. */
    public final static byte casefold_off = 30;  /* Turn off casefold flag. */
    public final static byte option_set = 31;	   /* Turn on multi line match (match with newlines). */
    public final static byte start_nowidth = 32; /* Save string point to the stack. */
    public final static byte stop_nowidth = 33;  /* Restore string place at the point start_nowidth. */
    public final static byte pop_and_fail = 34;  /* Fail after popping nowidth entry from stack. */
    public final static byte stop_backtrack = 35;  /* Restore backtrack stack at the point start_nowidth. */
    public final static byte duplicate = 36;   /* Match a duplicate of something remembered.
                                                   Followed by one byte containing the index of the memory 
                                                   register.  */
    public final static byte wordchar = 37;    /* Matches any word-constituent character.  */
    public final static byte notwordchar = 38; /* Matches any char that is not a word-constituent.  */
    public final static byte wordbeg = 39;	 /* Succeeds if at word beginning.  */
    public final static byte wordend = 40;	 /* Succeeds if at word end.  */
    public final static byte wordbound = 41;   /* Succeeds if at a word boundary.  */
    public final static byte notwordbound = 42; /* Succeeds if not at a word boundary.  */
}

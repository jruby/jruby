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

import static org.rej.REJConstants.*;
import static org.rej.MBC.*;
import static org.rej.Helpers.*;
import static org.rej.Bytecodes.*;

class MatchEnvironmentSingleByte {
    private byte[] pattern;
    private int patternIndex;
    private final int patternEnd;
    private int optionFlags;
    private final int registerCount;
    private final byte[] string;
    private final int stringStart;
    private int stringIndex;
    private final int stringEnd;
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

    private int[] failureStack;
    private int failureStackPointer;
    private int failureStackEnd;
    
    private boolean bestRegistersSet = false;
    private int failureCountNums = 0;

    private final Pattern self;

    /* stack & working area for re_match() */
    private int[] registerStart;
    private int[] registerEnd;
    private boolean[] registerActive;
    private boolean[] registerMatchedSomething;
    private int[] bestRegisterStart;
    private int[] bestRegisterEnd;

    private Registers registers;

    private boolean shouldCaseTranslate;
    private boolean isMultiLine;
    private boolean isLongestMatch;

    public MatchEnvironmentSingleByte(Pattern p, byte[] string, int stringStart, int size, int pos, int beg, Registers registers) {
        this.size = size;
        this.beg = beg;
        this.pattern = p.buffer;
        this.patternIndex = 0;
        this.patternEnd = p.used;
        this.registerCount = p.re_nsub;
        this.string = string;
        this.stringStart = stringStart;
        this.optionFlags = (int)p.options;
        this.self = p;
        this.ctx = p.ctx;
        this.pos = pos;

        //        System.err.println(Bytecodes.describe(this.p,pix,pend));

        this.registers = registers;

        registerStart = new int[registerCount];
        registerEnd = new int[registerCount];
        registerActive = new boolean[registerCount]; 
        registerMatchedSomething = new boolean[registerCount];
        bestRegisterStart = new int[registerCount];
        bestRegisterEnd = new int[registerCount];

        if(registers != null) {
            registers.initRegisters(registerCount);
        }

        initStack();
        initRegisters();

        /* Set up pointers to ends of strings.
           Don't allow the second string to be empty unless both are empty.  */

        /* `p' scans through the pattern as `d' scans through the data. `string_end'
           is the end of the input string that `d' points within. `d' is
           advanced into the following input string whenever necessary, but
           this happens before fetching; therefore, at the beginning of the
           loop, `d' can be pointing at the end of a string, but it cannot
           equal string2.  */

        stringIndex = stringStart+pos; stringEnd = stringStart+size;
        this.shouldCaseTranslate = shouldCaseTranslate();
        this.isMultiLine = (optionFlags&RE_OPTION_MULTILINE)!=0;
        this.isLongestMatch = (optionFlags&RE_OPTION_LONGEST)!=0;
    }

    private final boolean shouldCaseTranslate() {
        return ((optionFlags&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
    }

    private final void initStack() {
        /* Initialize the stack. */

        int i = -1;
        synchronized(self) {
            i = self.poolIndex++;
        }
        if(i < self.pool.length) {
            failureStack = self.pool[i];
        } else {
            failureStack = new int[(registerCount*NUM_REG_ITEMS + NUM_NONREG_ITEMS)*NFAILURES];
        }

        failureStackPointer = 0;
        failureStackEnd = failureStack.length;
    }

    private final void initRegisters() {
        /* Initialize subexpression text positions to -1 to mark ones that no
           ( or ( and ) or ) has been seen for. Also set all registers to
           inactive and mark them as not having matched anything or ever
           failed. */
        Arrays.fill(registerStart, REG_UNSET_VALUE);
        Arrays.fill(registerEnd, REG_UNSET_VALUE);
        Arrays.fill(bestRegisterStart, REG_UNSET_VALUE);
        Arrays.fill(bestRegisterEnd, REG_UNSET_VALUE);
    }

    private final void popFailureCount() {
        int ptr = failureStack[--failureStackPointer];
        int count = failureStack[--failureStackPointer];
        storeNumber(pattern, ptr, count);
    }

    private final void popFailurePoint() {
        long temp;
        failureStackPointer -= NUM_NONREG_ITEMS;	/* Remove failure points (and flag). */
        temp = failureStack[--failureStackPointer];	/* How many regs pushed.  */
        temp *= NUM_REG_ITEMS;	/* How much to take off the stack.  */
        failureStackPointer -= temp; 		/* Remove the register info.  */
        temp = failureStack[--failureStackPointer];	/* How many counters pushed.  */
        while(temp-- > 0) {
            popFailureCount();
        }
        failureCountNums = 0;	/* Reset num_failure_counts.  */
    }

    private final void setMatchedRegisters() {
        for(int this_reg = 0; this_reg < registerCount; this_reg++) {
            registerMatchedSomething[this_reg] = registerActive[this_reg];
        }
    }

    private final void pushFailurePoint(int patternPlace, int stringPlace) {
        int lastUsedRegister, thisRegister;

        /* Find out how many registers are active or have been matched.
           (Aside from register zero, which is only set at the end.) */
        for(lastUsedRegister = registerCount-1; lastUsedRegister > 0; lastUsedRegister--) {
            if(registerStart[lastUsedRegister]!=REG_UNSET_VALUE) {
                break;
            }
        }
                        
        if(failureStackEnd - failureStackPointer <= (lastUsedRegister * NUM_REG_ITEMS + NUM_NONREG_ITEMS + 1)) {
            expandStack();
        }

        failureStack[failureStackPointer++] = failureCountNums;
        failureCountNums = 0;

        System.arraycopy(registerStart,1,failureStack,failureStackPointer,lastUsedRegister);
        failureStackPointer+=lastUsedRegister;

        System.arraycopy(registerEnd,1,failureStack,failureStackPointer,lastUsedRegister);
        failureStackPointer+=lastUsedRegister;

        /* Push how many registers we saved.  */
        failureStack[failureStackPointer++] = lastUsedRegister;

        failureStack[failureStackPointer++] = 0; //Used for failure places that should match more than once

        failureStack[failureStackPointer++] = patternPlace;
        failureStack[failureStackPointer++] = stringPlace;
        failureStack[failureStackPointer++] = (int)optionFlags; /* current option status */
        failureStack[failureStackPointer++] = 0; /* non-greedy flag */
    }

    private final int bytecode_duplicate() {
        int registerNumber = pattern[patternIndex++];   /* Get which register to match against */
        int stringPosition2, stringEnd2;

        /* Check if there's corresponding group */
        if(registerNumber >= registerCount) {
            return BREAK_WITH_FAIL1;
        }
        /* Check if corresponding group is still open */
        if(registerActive[registerNumber]) {
            return BREAK_WITH_FAIL1;
        }

        /* Where in input to try to start matching.  */
        stringPosition2 = registerStart[registerNumber];
        if(stringPosition2 == REG_UNSET_VALUE) {
            return BREAK_WITH_FAIL1;
        }

        /* Where to stop matching; if both the place to start and
           the place to stop matching are in the same string, then
           set to the place to stop, otherwise, for now have to use
           the end of the first string.  */

        stringEnd2 = registerEnd[registerNumber];
        if(stringEnd2 == REG_UNSET_VALUE) {
            return BREAK_WITH_FAIL1;
        }

        for(;;) {
            /* At end of register contents => success */
            if(stringPosition2 == stringEnd2) {
                break;
            }

            /* If necessary, advance to next segment in data.  */
            if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}

            /* How many characters left in this segment to match.  */
            int mcnt = stringEnd - stringIndex;

            /* Want how many consecutive characters we can match in
               one shot, so, if necessary, adjust the count.  */
            if(mcnt > stringEnd2 - stringPosition2) {
                mcnt = stringEnd2 - stringPosition2;
            }

            /* Compare that many; failure if mismatch, else move
               past them.  */
            if(shouldCaseTranslate ? self.memcmp_translate(string, stringIndex, stringPosition2, mcnt)!=0 : memcmp(string, stringIndex, stringPosition2, mcnt)!=0) {
                return BREAK_WITH_FAIL1;
            }
            stringIndex += mcnt;
            stringPosition2 += mcnt;
        }
        return BREAK_NORMALLY;
    }

    private static final boolean isInList(int cx, byte[] b, int bix) {
        int size = b[bix++]&0xFF;
        return cx/8 < size && ((b[bix + cx/8]&0xFF)&(1<<cx%8)) != 0;
    }

    private final int handleFailure() {
        /* A restart point is known.  Restart there and pop it. */
        int thisRegister;
        int res;

        /* If this failure point is from a dummy_failure_point, just
           skip it.  */
        if(failureStack[failureStackPointer-4] == -1 || (bestRegistersSet && failureStack[failureStackPointer-1] == NON_GREEDY)) {
            popFailurePoint();
            return 0;
        }

        if((res=failureStack[failureStackPointer-5]) > 0) {
            // Assume options don't change for this failure
            stringIndex = failureStack[failureStackPointer-3] + res;
            patternIndex = failureStack[failureStackPointer-4];

            int lastUsedRegister = failureStack[failureStackPointer-6];

            thisRegister = lastUsedRegister;

            Arrays.fill(registerStart, lastUsedRegister+1, registerCount, REG_UNSET_VALUE);
            Arrays.fill(registerEnd, lastUsedRegister+1, registerCount, REG_UNSET_VALUE);
            Arrays.fill(registerActive, lastUsedRegister+1, registerCount, false);
            Arrays.fill(registerMatchedSomething, lastUsedRegister+1, registerCount, false);
            
            System.arraycopy(failureStack, failureStackPointer-(6+thisRegister), registerEnd, 1, thisRegister);
            System.arraycopy(failureStack, failureStackPointer-(6+2*thisRegister), registerStart, 1, thisRegister);

            failureStack[failureStackPointer-5]--;
        } else {
            failureStackPointer--;		/* discard greedy flag */
            optionFlags = failureStack[--failureStackPointer];
            stringIndex = failureStack[--failureStackPointer];
            patternIndex = failureStack[--failureStackPointer];

            failureStackPointer--;		/* discard failure point count for now */

            /* Restore register info.  */
            int lastUsedRegister = failureStack[--failureStackPointer];

            thisRegister = lastUsedRegister;

            Arrays.fill(registerStart, lastUsedRegister+1, registerCount, REG_UNSET_VALUE);
            Arrays.fill(registerEnd, lastUsedRegister+1, registerCount, REG_UNSET_VALUE);
            Arrays.fill(registerActive, lastUsedRegister+1, registerCount, false);
            Arrays.fill(registerMatchedSomething, lastUsedRegister+1, registerCount, false);

            failureStackPointer -= thisRegister;
            System.arraycopy(failureStack, failureStackPointer, registerEnd, 1, thisRegister);
            failureStackPointer -= thisRegister;
            System.arraycopy(failureStack, failureStackPointer, registerStart, 1, thisRegister);

            int mcnt = failureStack[--failureStackPointer];
            while(mcnt-->0) {
                int ptr = failureStack[--failureStackPointer];
                int count = failureStack[--failureStackPointer];
                storeNumber(pattern, ptr, count);
            }

            if(patternIndex < patternEnd) {
                int isA_jump_n = 0;
                int failedParen = 0;

                int patternIndex1 = patternIndex;
                /* If failed to a backwards jump that's part of a repetition
                   loop, need to pop this failure point and use the next one.  */
                switch(pattern[patternIndex1]) {
                case jump_n:
                case finalize_push_n:
                    isA_jump_n = 1;
                case maybe_finalize_jump:
                case finalize_jump:
                case finalize_push:
                case jump:
                    patternIndex1++;
                    mcnt = extractNumber(pattern,patternIndex1);
                    patternIndex1+=2;
                    if(mcnt >= 0) {
                        break;	/* should be backward jump */
                    }
                    patternIndex1 += mcnt;
                    if((isA_jump_n!=0 && pattern[patternIndex1] == succeed_n) ||
                       (isA_jump_n==0 && pattern[patternIndex1] == on_failure_jump)) {
                        if(failedParen!=0) {
                            patternIndex1++;
                            mcnt = extractNumber(pattern, patternIndex1);
                            patternIndex1+=2;

                            pushFailurePoint(patternIndex1+mcnt,stringIndex);
                            failureStack[failureStackPointer-1] = NON_GREEDY;
                            //System.err.println("handle_fail: " + DESCRIBE_FAILURE_POINT());
                        }
                        return 0;
                    }
                default:
                    /* do nothing */;
                    return 1;
                }
            }
        }

        return 1;
    }

    private final void fixRegisters() {
        System.arraycopy(bestRegisterStart, 0, registerStart, 0, registerCount);
        System.arraycopy(bestRegisterEnd, 0, registerEnd, 0, registerCount);
    }

    private final void fixBestRegisters() {
        System.arraycopy(registerStart, 1, bestRegisterStart, 1, registerCount-1);
        System.arraycopy(registerEnd, 1, bestRegisterEnd, 1, registerCount-1);
    }

    public final int restoreBestRegisters() {
        /* If not end of string, try backtracking.  Otherwise done.  */
        if(isLongestMatch && stringIndex != stringEnd) {
            if(bestRegistersSet) {/* non-greedy, no need to backtrack */
                /* Restore best match.  */
                stringIndex = bestRegisterEnd[0];
                fixRegisters();
                return 0;
            }
            while(failureStackPointer != 0 && failureStack[failureStackPointer-1] == NON_GREEDY) {
                popFailurePoint();
            }
            if(failureStackPointer != 0) {
                /* More failure points to try.  */
                bestRegistersSet = true;
                bestRegisterEnd[0] = stringIndex;	/* Never use regstart[0].  */
                fixBestRegisters();
                return 1;
            }
        }
        return 0;
    }

    public final void convertRegisters(Registers regs) {
        /* If caller wants register contents data back, convert it 
           to indices.  */
        if(regs != null) {
            regs.beg[0] = pos;
            regs.end[0] = stringIndex-stringStart;
            for(int mcnt = 1; mcnt < registerCount; mcnt++) {
                if(registerEnd[mcnt] == REG_UNSET_VALUE) {
                    regs.beg[mcnt] = -1;
                    regs.end[mcnt] = -1;
                    continue;
                }
                regs.beg[mcnt] = registerStart[mcnt] - stringStart;
                regs.end[mcnt] = registerEnd[mcnt] - stringStart;
            }
        }
    }
        
    public final int bytecode_start_memory() {
        registerStart[pattern[patternIndex]] = stringIndex;
        registerActive[pattern[patternIndex]] = true;
        registerMatchedSomething[pattern[patternIndex]] = false;
        patternIndex += 2;
        return CONTINUE_MAINLOOP;
    }

    public final int bytecode_stop_memory() {
        registerEnd[pattern[patternIndex]] = stringIndex;
        registerActive[pattern[patternIndex]] = false;
        patternIndex += 2;
        return CONTINUE_MAINLOOP;
    }

    public final int bytecode_anychar() {
        if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}
        if(!isMultiLine
           && (shouldCaseTranslate ? ctx.translate[string[stringIndex]] : string[stringIndex]) == '\n') {
            return 1;
        }
        setMatchedRegisters();
        stringIndex++;
        return 0;
    }

    public final int bytecode_charset() {
        boolean not;	    /* Nonzero for charset_not.  */
        boolean part = false;	    /* true if matched part of mbc */
        int dsave = stringIndex + 1;
        int cc;
                    
        if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}
                        
        char c = (char)(string[stringIndex++]&0xFF);
        if(shouldCaseTranslate()) {
            c = ctx.translate[c];
        }
        not = isInList(c, pattern, patternIndex);

        if(pattern[patternIndex-1] == charset_not) {
            not = !not;
        }
            
        if(!not) {return BREAK_WITH_FAIL1;}
            
        patternIndex += 1 + (char)(pattern[patternIndex]&0xFF) + 2 + extractUnsigned(pattern, patternIndex + 1 + (char)(pattern[patternIndex]&0xFF))*8;
        setMatchedRegisters();
                    
        if(part) {
            stringIndex = dsave;
        }
        return CONTINUE_MAINLOOP;
    }

    public final void bytecode_anychar_repeat() {
        final int posBefore = stringIndex;
        pushFailurePoint(patternIndex,stringIndex);
        if(!isMultiLine) {
            for (;;) {
                //            System.err.println("anychar_repeat: " + DESCRIBE_FAILURE_POINT());
                if(stringIndex == stringEnd) {
                    if(stringIndex != posBefore) {
                        setMatchedRegisters();
                    }
                    failureStack[failureStackPointer-5] = stringIndex - posBefore;
                    return;
                }
                byte c = string[stringIndex];
                if(c == '\n') {
                    if(stringIndex != posBefore) {
                        setMatchedRegisters();
                    }
                    failureStack[failureStackPointer-5] = stringIndex - posBefore;
                    return;
                }
                stringIndex++;
            }

        } else {
            for (;;) {
                //            System.err.println("anychar_repeat: " + DESCRIBE_FAILURE_POINT());
                if(stringIndex == stringEnd) {
                    if(stringIndex != posBefore) {
                        setMatchedRegisters();
                    }
                    failureStack[failureStackPointer-5] = stringIndex - posBefore;
                    return;
                }
                byte c = string[stringIndex];
                stringIndex++;
            }
        }
    }

    public final int bytecode_maybe_finalize_jump() {
        int mcnt = extractNumberAndIncrementPatternIndex();
        int patternIndex1 = patternIndex;

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
        while(patternIndex1 + 2 < patternEnd) {
            if(pattern[patternIndex1] == stop_memory ||
               pattern[patternIndex1] == start_memory) {
                patternIndex1 += 3;	/* Skip over args, too.  */
            } else if(pattern[patternIndex1] == stop_paren) {
                patternIndex1 += 1;
            } else {
                break;
            }
        }
        if(patternIndex1 == patternEnd) {
            pattern[patternIndex-3] = finalize_jump;
        } else if(pattern[patternIndex1] == exactn || pattern[patternIndex1] == endline) {
            char c = pattern[patternIndex1] == endline ? '\n' : (char)(pattern[patternIndex1+2]&0xFF);
            int p2 = patternIndex+mcnt;
            /* p2[0] ... p2[2] are an on_failure_jump.
               Examine what follows that.  */
            if(pattern[p2+3] == exactn && (pattern[p2+5]&0xFF) != c) {
                pattern[patternIndex-3] = finalize_jump;
            } else if(pattern[p2+3] == charset ||
                      pattern[p2+3] == charset_not) {
                boolean not;
                /* `is_in_list()' is TRUE if c would match */
                /* That means it is not safe to finalize.  */
                not = isInList(c, pattern, p2 + 4);
                if(pattern[p2+3] == charset_not) {
                    not = !not;
                }
                if(!not) {
                    pattern[patternIndex-3] = finalize_jump;
                }
            }
        }
        patternIndex -= 2;		/* Point at relative address again.  */
        if(pattern[patternIndex-1] != finalize_jump) {
            pattern[patternIndex-1] = jump;	
            mcnt = extractNumberAndIncrementPatternIndex();
            if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
                return 1;
            }
            patternIndex += mcnt;
            return 2;
        } 
        return 0;
    }

    public final int bytecode_push_dummy_failure() {
        /* See comments just above at `dummy_failure_jump' about the
           two zeroes.  */
        int p1 = patternIndex;
        /* Skip over open/close-group commands.  */
        while(p1 + 2 < patternEnd) {
            if(pattern[p1] == stop_memory ||
               pattern[p1] == start_memory) {
                p1 += 3;	/* Skip over args, too.  */
            } else if(pattern[p1] == stop_paren) {
                p1 += 1;
            } else {
                break;
            }
        }
        if(p1 < patternEnd && pattern[p1] == jump) {
            pattern[patternIndex-1] = unused;
        } else {
            pushFailurePoint(-1,0);
            //System.err.println("push_dummy_failure: " + DESCRIBE_FAILURE_POINT());
        }
        return CONTINUE_MAINLOOP;
    }

    private final void expandStack() {
        int[] stackx;
        int xlen = failureStackEnd;
        stackx = new int[2*xlen];
        System.arraycopy(failureStack,0,stackx,0,xlen);
        failureStack = stackx;
        failureStackEnd = 2*xlen;
    }

    private final void expandStackIfNeeded() {
        if(failureStackEnd - failureStackPointer <= NUM_COUNT_ITEMS) {
            expandStack();
        }
    }

    public final int bytecode_succeed_n() {
        int mcnt = extractNumber(pattern, patternIndex + 2);
        /* Originally, this is how many times we HAVE to succeed.  */
        if(mcnt != 0) {
            mcnt--;
            patternIndex += 2;

            char c = (char)extractNumber(pattern, patternIndex);
            expandStackIfNeeded();
            failureStack[failureStackPointer++] = c;
            failureStack[failureStackPointer++] = patternIndex;
            failureCountNums++;
                
            storeNumber(pattern, patternIndex, mcnt);
            patternIndex+=2;
                
            pushFailurePoint(-1,0);
        } else  {
            mcnt = extractNumberAndIncrementPatternIndex();
            pushFailurePoint(patternIndex+mcnt,stringIndex);
        }
        //System.err.println("succeed_n: " + DESCRIBE_FAILURE_POINT());
        return CONTINUE_MAINLOOP;
    }

    public final int bytecode_jump_n() {
        int mcnt = extractNumber(pattern, patternIndex + 2);
        /* Originally, this is how many times we CAN jump.  */
        if(mcnt!=0) {
            mcnt--;

            char c = (char)extractNumber(pattern, patternIndex+2);
            expandStackIfNeeded();
            failureStack[failureStackPointer++] = c;
            failureStack[failureStackPointer++] = patternIndex+2;
            failureCountNums++;
            storeNumber(pattern, patternIndex + 2, mcnt);
            mcnt = extractNumberAndIncrementPatternIndex();
            if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
                return BREAK_WITH_FAIL1;
            }
            patternIndex += mcnt;
            return CONTINUE_MAINLOOP;
        }
        /* If don't have to jump any more, skip over the rest of command.  */
        else {
            patternIndex += 4;
        }
        return CONTINUE_MAINLOOP;
    }

    public final int bytecode_exactn() {
        /* Match the next few pattern characters exactly.
           mcnt is how many characters to match.  */
        int mcnt = pattern[patternIndex++] & 0xff;
        //            System.err.println("matching " + mcnt + " exact characters");
        /* This is written out as an if-else so we don't waste time
           testing `translate' inside the loop.  */
        if(shouldCaseTranslate) {
            do {
                if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}
                char pc = (char)(pattern[patternIndex]&0xFF);
                if(pc == 0xff) {
                    patternIndex++;  
                    if(--mcnt==0
                       || stringIndex == stringEnd
                       || string[stringIndex++] != pattern[patternIndex++]) {
                        return BREAK_WITH_FAIL1;
                    }
                    continue;
                }
                char c = (char)(string[stringIndex++]&0xFF);
                /* compiled code translation needed for ruby */
                if(ctx.translate[c] != ctx.translate[pattern[patternIndex++]&0xFF]) {
                    return BREAK_WITH_FAIL1;
                }
            } while(--mcnt > 0);
        } else {
            do {
                if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}
                byte pc = pattern[patternIndex];
                if(pc == (byte)0xff) {
                    patternIndex++; mcnt--;
                    pc = pattern[patternIndex];
                }
                patternIndex++;
                if(string[stringIndex++] != pc) {
                    return BREAK_WITH_FAIL1;
                }
            } while(--mcnt > 0);
        }
        setMatchedRegisters();
        return CONTINUE_MAINLOOP;
    }

    private final String describeFailurePoint() {
        StringBuffer result = new StringBuffer(">>FailurePoint:").append("\n");
        int top = failureStackPointer;
        result.append("  Greedy: ").append(failureStack[--top] == 0).append("\n");
        result.append("  Optz: ").append(failureStack[--top]).append("\n");
        result.append("  String: ").append(failureStack[--top]).append("\n");
        result.append("  Pattern: ").append(failureStack[--top]).append("\n");
        result.append("  Repeat: ").append(failureStack[--top]).append("\n");
        int last_used_reg = failureStack[--top];
        result.append("  Regs: ").append(last_used_reg).append("\n");
        for(int this_reg = 1; this_reg <= last_used_reg; this_reg++) {
            int regend = failureStack[--top];
            int regstart = failureStack[--top];
            result.append("   [").append(this_reg).append("]=").append(regstart).append("-").append(regend).append("\n");
        }
        int num_failures = failureStack[--top];
        result.append("  Failures: ").append(num_failures).append("\n");
        for(int fail = 0; fail < num_failures; fail++) {
            int ptr = failureStack[--top];
            int count = failureStack[--top];
            result.append("   pattern[").append(ptr).append("]=").append(count).append("\n");
        }
        return result.toString();
    }

    private final int bytecode_start_nowidth() {
        pushFailurePoint(-1,stringIndex);
        //System.err.println("start_nowidth: " + DESCRIBE_FAILURE_POINT());
        if(failureStackPointer > RE_DUP_MAX) {
            return RETURN_OUT_OF_MEMORY;
        }
        int mcnt = extractNumberAndIncrementPatternIndex();
        storeNumber(pattern, patternIndex+mcnt, failureStackPointer);
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_stop_nowidth() {
        int mcnt = extractNumberAndIncrementPatternIndex();
        failureStackPointer = mcnt;
        stringIndex = failureStack[failureStackPointer-3];
        popFailurePoint();
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_stop_backtrack() {
        failureStackPointer = extractNumberAndIncrementPatternIndex();
        popFailurePoint();
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_pop_and_fail() {
        int mcnt = extractNumber(pattern, patternIndex+1);
        failureStackPointer = mcnt;
        popFailurePoint();
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_begline() {
        if(size == 0 || stringIndex == stringStart) {
            return CONTINUE_MAINLOOP;
        }
        if(string[stringIndex-1] == '\n' && stringIndex != stringEnd) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_endline() {
        if(stringIndex == stringEnd) {
            return CONTINUE_MAINLOOP;
        } else if(string[stringIndex] == '\n') {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_begbuf() {
        if(stringIndex==stringStart) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_endbuf() {
        if(stringIndex == stringEnd) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_endbuf2() {
        if(stringIndex == stringEnd) {
            return CONTINUE_MAINLOOP;
        }
        /* .. or newline just before the end of the data. */
        if(string[stringIndex] == '\n' && stringIndex+1 == stringEnd) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_begpos() {
        if(stringIndex == beg) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_on_failure_jump() {
        int mcnt = extractNumberAndIncrementPatternIndex();
        pushFailurePoint(patternIndex+mcnt,stringIndex);
        //System.err.println("on_failure_jump: " + DESCRIBE_FAILURE_POINT());
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_finalize_jump() {
        if(failureStackPointer > 2 && failureStack[failureStackPointer-3] == stringIndex) {
            patternIndex = failureStack[failureStackPointer-4];
            popFailurePoint();
            return CONTINUE_MAINLOOP;
        }
        popFailurePoint();
        return BREAK_NORMALLY;
    }

    private final int bytecode_casefold_on() {
        optionFlags |= RE_OPTION_IGNORECASE;
        this.shouldCaseTranslate = shouldCaseTranslate();
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_casefold_off() {
        optionFlags &= ~RE_OPTION_IGNORECASE;
        this.shouldCaseTranslate = false;
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_option_set() {
        optionFlags = pattern[patternIndex++];
        this.shouldCaseTranslate = shouldCaseTranslate();
        this.isMultiLine = (optionFlags&RE_OPTION_MULTILINE)!=0;
        this.isLongestMatch = (optionFlags&RE_OPTION_LONGEST)!=0;
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
                    if(patternIndex == patternEnd || gotoRestoreBestRegs) {
                        if(!gotoRestoreBestRegs) {
                            if(restoreBestRegisters()==1) {
                                break fail1;
                            }
                        } else {
                            gotoRestoreBestRegs = false;
                        }

                        convertRegisters(registers);

                        self.uninitStack();
                        return (stringIndex-stringStart) - pos;
                    }

                    int var = BREAK_NORMALLY;


                    //                    System.err.println("--executing " + Bytecodes.NAMES[(int)p[pix]] + " at " + pix);
                    //System.err.println("-- -- for d: " + string_pos + " and string_end: " + string_end);
                    switch(pattern[patternIndex++]) {
                        /* ( [or `(', as appropriate] is represented by start_memory,
                           ) by stop_memory.  Both of those commands are followed by
                           a register number in the next byte.  The text matched
                           within the ( and ) is recorded under that number.  */
                    case start_memory:
                        var = bytecode_start_memory();
                        break;
                    case stop_memory:
                        var = bytecode_stop_memory();
                        break;
                    case start_paren:
                    case stop_paren:
                        break;
                        /* \<digit> has been turned into a `duplicate' command which is
                           followed by the numeric value of <digit> as the register number.  */
                    case duplicate:
                        var = bytecode_duplicate();
                        break;
                    case start_nowidth:
                        var = bytecode_start_nowidth();
                        break;
                    case stop_nowidth:
                        var = bytecode_stop_nowidth();
                        break;
                    case stop_backtrack:
                        var = bytecode_stop_backtrack();
                        break;
                    case pop_and_fail:
                        var = bytecode_pop_and_fail();
                        break;
                    case anychar:
                        var = bytecode_anychar();
                        break;
                    case anychar_repeat: 
                        bytecode_anychar_repeat();
                        break fail1;
                    case charset:
                    case charset_not: 
                        var = bytecode_charset();
                        break;
                    case begline:
                        var = bytecode_begline();
                        break;
                    case endline:
                        var = bytecode_endline();
                        break;
                        /* Match at the very beginning of the string. */
                    case begbuf:
                        var = bytecode_begbuf();
                        break;
                        /* Match at the very end of the data. */
                    case endbuf:
                        var = bytecode_endbuf();
                        break;
                        /* Match at the very end of the data. */
                    case endbuf2:
                        var = bytecode_endbuf2();
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
                        var = bytecode_begpos();
                        break;
                    case on_failure_jump:
                        //                on_failure:
                        var = bytecode_on_failure_jump();
                        break;

                        /* The end of a smart repeat has a maybe_finalize_jump back.
                           Change it either to a finalize_jump or an ordinary jump.  */
                    case maybe_finalize_jump:
                        switch(bytecode_maybe_finalize_jump()) {
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
                        if(bytecode_finalize_jump() == CONTINUE_MAINLOOP) {
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
                        var = bytecode_dummy_failure_jump();
                        break;

                        /* At the end of an alternative, we need to push a dummy failure
                           point in case we are followed by a `finalize_jump', because
                           we don't want the failure point for the alternative to be
                           popped.  For example, matching `(a|ab)*' against `aab'
                           requires that we match the `ab' alternative.  */
                    case push_dummy_failure:
                        var = bytecode_push_dummy_failure();
                        break;
                        /* Have to succeed matching what follows at least n times.  Then
                           just handle like an on_failure_jump.  */
                    case succeed_n: 
                        var = bytecode_succeed_n();
                        break;
                    case jump_n:
                        var = bytecode_jump_n();
                        break;
                    case set_number_at:
                        var = bytecode_set_number_at();
                        break;
                    case try_next:
                        var = bytecode_try_next();
                        break;
                    case finalize_push:
                        var = bytecode_finalize_push();
                        break;
                    case finalize_push_n:
                        var = bytecode_finalize_push_n();
                        break;
                        /* Ignore these.  Used to ignore the n of succeed_n's which
                           currently have n == 0.  */
                    case unused:
                        var = CONTINUE_MAINLOOP;
                        break;
                    case casefold_on:
                        var = bytecode_casefold_on();
                        break;
                    case casefold_off:
                        var = bytecode_casefold_off();
                        break;
                    case option_set:
                        var = bytecode_option_set();
                        break;
                    case wordbound:
                        var = bytecode_wordbound();
                        break;
                    case notwordbound:
                        var = bytecode_notwordbound();
                        break;
                    case wordbeg:
                        var = bytecode_wordbeg();
                        break;
                    case wordend:
                        var = bytecode_wordend();
                        break;
                    case wordchar:
                        var = bytecode_wordchar();
                        break;
                    case notwordchar:
                        var = bytecode_notwordchar();
                        break;
                    case exactn:
                        var = bytecode_exactn();
                        break;
                    }


                    switch(var) {
                    case CONTINUE_MAINLOOP:
                        continue mainLoop;
                    case BREAK_WITH_FAIL1:
                        break fail1;
                    case RETURN_OUT_OF_MEMORY:
                        self.uninitStack();
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

            if(bestRegistersSet) {
                gotoRestoreBestRegs=true;
                stringIndex = bestRegisterEnd[0];
                fixRegisters();
            }
        } while(gotoRestoreBestRegs);

        self.uninitStack();
        return -1;
    }

    private final int jump() {
        int mcnt = extractNumberAndIncrementPatternIndex();
        if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
            return BREAK_WITH_FAIL1;
        }
        patternIndex += mcnt;
        return CONTINUE_MAINLOOP;
    }

    private final int extractNumberAndIncrementPatternIndex() {
        int val = extractNumber(pattern, patternIndex);
        patternIndex += 2;
        return val;
    }

    private final boolean stackOutOfRange() {
        return failureStackPointer > 2 && failureStack[failureStackPointer-3] == stringIndex;
    }

    private final int bytecode_dummy_failure_jump() {
        pushFailurePoint(-1,0);
        //System.err.println("dummy_failure_jump: " + DESCRIBE_FAILURE_POINT());
        int mcnt = extractNumberAndIncrementPatternIndex();
        if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
            return BREAK_WITH_FAIL1;
        }
        patternIndex += mcnt;
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_try_next() {
        int mcnt = extractNumberAndIncrementPatternIndex();
        if(patternIndex + mcnt < patternEnd) {
            pushFailurePoint(patternIndex,stringIndex);
            failureStack[failureStackPointer-1] = NON_GREEDY;
            //System.err.println("try_next: " + DESCRIBE_FAILURE_POINT());
        }
        patternIndex += mcnt;
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_set_number_at() {
        int mcnt = extractNumberAndIncrementPatternIndex();
        int p1 = patternIndex + mcnt;
        mcnt = extractNumberAndIncrementPatternIndex();
        storeNumber(pattern, p1, mcnt);
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_finalize_push() {
        popFailurePoint();
        int mcnt = extractNumberAndIncrementPatternIndex();
        if(mcnt < 0 && stackOutOfRange()) { /* avoid infinite loop */
            return BREAK_WITH_FAIL1;
        }
        pushFailurePoint(patternIndex+mcnt,stringIndex);
        failureStack[failureStackPointer-1] = NON_GREEDY;
        //System.err.println("finalize_push: " + DESCRIBE_FAILURE_POINT());
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_finalize_push_n() {
        int mcnt = extractNumber(pattern, patternIndex + 2); 
        /* Originally, this is how many times we CAN jump.  */
        if(mcnt>0) {
            int posx, i;
            mcnt--;
            storeNumber(pattern, patternIndex + 2, mcnt);
            posx = extractNumber(pattern, patternIndex);
            i = extractNumber(pattern,patternIndex+posx+5);
            if(i > 0) {
                mcnt = extractNumber(pattern, patternIndex);
                patternIndex += 2;
                if(mcnt < 0 && stackOutOfRange()) {/* avoid infinite loop */
                    return BREAK_WITH_FAIL1;
                }
                patternIndex += mcnt;
                return CONTINUE_MAINLOOP;
            }
            popFailurePoint();
            mcnt = extractNumberAndIncrementPatternIndex();
            pushFailurePoint(patternIndex+mcnt,stringIndex);
            failureStack[failureStackPointer-1] = NON_GREEDY;
            //System.err.println("fainluze_push_n: " + DESCRIBE_FAILURE_POINT());
            patternIndex += 2;		/* skip n */
        }
        /* If don't have to push any more, skip over the rest of command.  */
        else {
            patternIndex += 4;
        }
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_wordbound() {
        if(stringIndex == 0) {
            if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}
            if(isALetter(string,stringIndex,stringEnd)) {
                return CONTINUE_MAINLOOP;
            } else {
                return BREAK_WITH_FAIL1;
            }
        }
        if(stringIndex == stringEnd) {
            if(previousIsALetter(string,stringIndex,stringEnd)) {
                return CONTINUE_MAINLOOP;
            } else {
                return BREAK_WITH_FAIL1;
            }
        }
        if(previousIsALetter(string,stringIndex,stringEnd) != isALetter(string,stringIndex,stringEnd)) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_notwordbound() {
        if(stringIndex==0) {
            if(isALetter(string, stringIndex, stringEnd)) {
                return BREAK_WITH_FAIL1;
            } else {
                return CONTINUE_MAINLOOP;
            }
        }
        if(stringIndex == stringEnd) {
            if(previousIsALetter(string, stringIndex, stringEnd)) {
                return BREAK_WITH_FAIL1;
            } else {
                return CONTINUE_MAINLOOP;
            }
        }
        if(previousIsALetter(string, stringIndex, stringEnd) != isALetter(string, stringIndex, stringEnd)) {
            return BREAK_WITH_FAIL1;
        }
        return CONTINUE_MAINLOOP;
    }

    private final int bytecode_wordbeg() {
        if(isALetter(string, stringIndex, stringEnd) && (stringIndex==0 || !previousIsALetter(string,stringIndex,stringEnd))) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_wordend() {
        if(stringIndex!=0 && previousIsALetter(string, stringIndex, stringEnd)
           && (!isALetter(string, stringIndex, stringEnd) || stringIndex == stringEnd)) {
            return CONTINUE_MAINLOOP;
        }
        return BREAK_WITH_FAIL1;
    }

    private final int bytecode_wordchar() {
        if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}
        if(!isALetter(string,stringIndex,stringEnd)) {
            return BREAK_WITH_FAIL1;
        }
        stringIndex++;
        setMatchedRegisters();
        return CONTINUE_MAINLOOP;
    }
        
    private final int bytecode_notwordchar() {
        if(stringIndex == stringEnd) {return BREAK_WITH_FAIL1;}
        if(isALetter(string, stringIndex, stringEnd)) {
            return BREAK_WITH_FAIL1;
        }
        stringIndex++;
        setMatchedRegisters();
        return CONTINUE_MAINLOOP;
    }

    private final boolean isALetter(byte[] d, int dix, int dend) {
        return re_syntax_table[d[dix]&0xFF] == Sword;
    }

    private final boolean previousIsALetter(byte[] d, int dix, int dend) {
        return re_syntax_table[d[dix-1]&0xFF] == Sword;
    }

    private final int fail() {
        //fail:
        do {
            if(failureStackPointer != 0) {
                if(handleFailure() == 1) {
                    return 0;
                }
            } else {
                return -1; /* Matching at this starting point really fails.  */
            }
        } while(true);
    }

    private final static int BREAK_NORMALLY = 0;
    private final static int BREAK_WITH_FAIL1 = 1;
    private final static int CONTINUE_MAINLOOP = 2;
    private final static int RETURN_OUT_OF_MEMORY = 3;
}

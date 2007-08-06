/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.UnsupportedEncodingException;

import jregex.Matcher;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  amoore
 */
public abstract class RubyMatchData extends RubyObject {
    public static RubyClass createMatchDataClass(Ruby runtime) {
        // TODO: Is NOT_ALLOCATABLE_ALLOCATOR ok here, since you can't actually instantiate MatchData directly?
        RubyClass matchDataClass = runtime.defineClass("MatchData", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.defineGlobalConstant("MatchingData", matchDataClass);

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyMatchData.class);

        matchDataClass.defineFastMethod("captures", callbackFactory.getFastMethod("captures"));
        matchDataClass.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        matchDataClass.defineFastMethod("size", callbackFactory.getFastMethod("size"));
        matchDataClass.defineFastMethod("length", callbackFactory.getFastMethod("size"));
        matchDataClass.defineFastMethod("offset", callbackFactory.getFastMethod("offset", RubyKernel.IRUBY_OBJECT));
        matchDataClass.defineFastMethod("begin", callbackFactory.getFastMethod("begin", RubyKernel.IRUBY_OBJECT));
        matchDataClass.defineFastMethod("end", callbackFactory.getFastMethod("end", RubyKernel.IRUBY_OBJECT));
        matchDataClass.defineFastMethod("to_a", callbackFactory.getFastMethod("to_a"));
        matchDataClass.defineFastMethod("[]", callbackFactory.getFastOptMethod("aref"));
        matchDataClass.defineFastMethod("pre_match", callbackFactory.getFastMethod("pre_match"));
        matchDataClass.defineFastMethod("post_match", callbackFactory.getFastMethod("post_match"));
        matchDataClass.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        matchDataClass.defineFastMethod("string", callbackFactory.getFastMethod("string"));

        matchDataClass.getMetaClass().undefineMethod("new");

        return matchDataClass;
    }

    protected Matcher matcher;

    public RubyMatchData(Ruby runtime, Matcher matcher) {
        super(runtime, runtime.getClass("MatchData"));
        this.matcher = matcher;
    }
    
    public abstract IRubyObject captures();

    public IRubyObject subseq(long beg, long len) {
        // Subsequence begins at a valid index and a positive length
        if (beg < 0 || beg > getSize() || len < 0) {
            getRuntime().getNil();
        }
        
        if (beg + len > getSize()) {
            len = getSize() - beg;
        }
        if (len < 0) {
            len = 0;
        }
        if (len == 0) {
            return getRuntime().newArray();
        }
        RubyArray arr = RubyArray.newArray(getRuntime(), len);
        for (long i = beg, j = beg+len; i < j; i++) {
            arr.append(group(i));
        }
        return arr;
    }

    public long getSize() {
        return matcher.groupCount();
    }
    
    public boolean proceed() {
        boolean b = matcher.proceed();
        invalidateRegs();
        return b;
    }
    
    public boolean find() {
        boolean b = matcher.find();
        invalidateRegs();
        return b;
    }

    public void invalidateRegs() {
    }

    public abstract IRubyObject group(long n);

    public int matchStartPosition() {
        return matcher.start();
    }

    public int matchEndPosition() {
        return matcher.end();
    }
    
    // version to work with Java primitives for efficiency
    private boolean outOfBounds(long n) {
        return n < 0 || n >= getSize();
    }

    //
    // Methods of the MatchData Class:
    //

    /** match_aref
     *
     */
    public IRubyObject aref(IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(getRuntime(), args, 1, 2);
        if (argc == 2) {
            int beg = RubyNumeric.fix2int(args[0]);
            int len = RubyNumeric.fix2int(args[1]);
            if (beg < 0) {
                beg += getSize();
            }
            return subseq(beg, len);
        }
        if (args[0] instanceof RubyFixnum) {
            int idx = RubyNumeric.fix2int(args[0]);
            if (idx < 0) {
                idx += getSize();
            }
            return group(idx);
        }
        if (args[0] instanceof RubyBignum) {
            throw getRuntime().newIndexError("index too big");
        }
        if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange) args[0]).getBeginLength(getSize(), true, false);
            if (begLen == null) {
                return getRuntime().getNil();
            }
            return subseq(begLen[0], begLen[1]);
        }
        return group(RubyNumeric.num2long(args[0]));
    }

    /** match_begin
     *
     */
    public IRubyObject begin(IRubyObject index) {
        int idx  = RubyNumeric.num2int(index);
        
        if (idx < 0 || idx >= getSize()) throw getRuntime().newIndexError("index " + idx + " out of matches");
        
        int answer = begin(idx);
        
        return answer == -1 ? getRuntime().getNil() : getRuntime().newFixnum(answer);
    }
    
    public int begin(int index) {
        return outOfBounds(index) || !matcher.isCaptured(index) ? -1 : matcher.start(index);
    }

    /** match_end
     *
     */
    public IRubyObject end(IRubyObject index) {
        int idx  = RubyNumeric.num2int(index);
        
        if (idx < 0 || idx >= getSize()) throw getRuntime().newIndexError("index " + idx + " out of matches");
        
        int answer = end(idx);

        return answer == -1 ? getRuntime().getNil() : getRuntime().newFixnum(answer);
    }
    
    public int end(int index) {
        return outOfBounds(index) || !matcher.isCaptured(index) ? -1 : matcher.end(index); 
    }
    
    public IRubyObject inspect() {
    	return anyToString();
    }

    /** match_size
     *
     */
    public RubyFixnum size() {
        return getRuntime().newFixnum(getSize());
    }

    /** match_offset
     *
     */
    public IRubyObject offset(IRubyObject index) {
        int idx = RubyNumeric.num2int(index);
        
        if (idx < 0 || idx >= getSize()) throw getRuntime().newIndexError("index " + idx + " out of matches");
        
        int beg = begin(idx);
        if (beg < 0) return getRuntime().newArrayNoCopy(new IRubyObject[]{getRuntime().getNil(), getRuntime().getNil()});
        
        return getRuntime().newArrayNoCopy(new IRubyObject[] { getRuntime().newFixnum(beg), getRuntime().newFixnum(end(idx))});
    }

    /** match_pre_match
     *
     */
    public abstract RubyString pre_match();

    /** match_post_match
     *
     */
    public abstract RubyString post_match();

    /** match_string
     *
     */
    public abstract RubyString string();

    /** match_to_a
     *
     */
    public abstract RubyArray to_a();

    /** match_to_s
     *
     */
    public abstract IRubyObject to_s();
    public abstract IRubyObject doClone();

    public static final class JavaString extends RubyMatchData {
        private String original;
        public JavaString(Ruby runtime, String original, Matcher matcher) {
            super(runtime, matcher);
            this.original = original;
        }

        public IRubyObject captures() {
            RubyArray arr = getRuntime().newArray(matcher.groupCount());
        
            for (int i = 1; i < matcher.groupCount(); i++) {
                if (matcher.group(i) == null) {
                    arr.append(getRuntime().getNil());
                } else {
                    arr.append(RubyString.newUnicodeString(getRuntime(), matcher.group(i)));
                }
            }
        
            return arr;
        }

        public IRubyObject group(long n) {
            // Request an invalid group OR group is an empty match
            if (n < 0 || n >= getSize() || matcher.group((int)n) == null) {
                return getRuntime().getNil();
            }
            // Fix for JRUBY-97: Temporary fix pending 
            // decision on UTF8-based string implementation.
            // String#substring reuses the storage of the original string
            // <http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4513622> 
            // Wrapping the String#substring in new String prevents this.
            // This wrapping alone was enough to fix the failing test cases in
            // JRUBY-97, but at the same time the testcase remained very slow
            // The additional minor optimizations to RubyString as part of the fix
            // dramatically improve the performance. 
    
            return RubyString.newUnicodeString(getRuntime(), matcher.group((int)n));
//            return getRuntime().newString(matcher.group((int)n));
        }

        public RubyString pre_match() {
            return RubyString.newUnicodeString(getRuntime(), matcher.prefix());
        }
        public RubyString post_match() {
            return RubyString.newUnicodeString(getRuntime(), matcher.suffix());
        }

        public RubyString string() {
            RubyString frozenString = RubyString.newUnicodeString(getRuntime(), original);
            System.out.println(frozenString);
            frozenString.freeze();
            return frozenString;
        }

        public RubyArray to_a() {
            RubyArray arr = getRuntime().newArray(matcher.groupCount());
        
            for (int i = 0; i < matcher.groupCount(); i++) {
                if (matcher.group(i) == null) {
                    arr.append(getRuntime().getNil());
                } else {
                    arr.append(RubyString.newUnicodeString(getRuntime(), matcher.group(i)));
                }
            }
        
            return arr;
        }

        public IRubyObject to_s() {
            return RubyString.newUnicodeString(getRuntime(), matcher.group(0));
        }

        public IRubyObject doClone() {
            return new JavaString(getRuntime(), original, matcher);
        }
        
        public int matchStartPosition() {
            int position = 0;
            try {
                position = matcher.prefix().getBytes("UTF8").length;
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return position;
        }
    }

    public static final class RString extends RubyMatchData {
        private RubyString original;
        private int len;
        private int[] start;
        private int[] end;

        public RString(Ruby runtime, RubyString original, Matcher matcher) {
            super(runtime, matcher);
            this.original = (RubyString)(original.strDup()).freeze();
            invalidateRegs();
        }

        public void invalidateRegs() {
            len = matcher.groupCount();
            if(start == null || start.length != len) {
                start = new int[len];
                end = new int[len];
            }
            for(int i=0;i<len;i++) {
                if(matcher.isCaptured(i)) {
                    start[i] = matcher.start(i);
                    end[i] = matcher.end(i);
                } else {
                    start[i] = -1;
                }
            }
        }

        public long getSize() {
            return len;
        }

        private RubyArray match_array(int st) {
            RubyArray ary = RubyArray.newArray(getRuntime(),len);
            RubyString target = original;
            boolean taint = isTaint();
            for(int i=st; i<len; i++) {
                if(start[i] == -1) {
                    ary.append(getRuntime().getNil());
                } else {
                    IRubyObject str = target.makeShared(start[i], end[i]-start[i]);
                    if(taint) {
                        str.setTaint(true);
                    }
                    ary.append(str);
                }
            }
            return ary;
        }

        public IRubyObject captures() {
            return match_array(1);
        }

        public IRubyObject nth_match(int nth) {
            if(nth >= len) {
                return getRuntime().getNil();
            }
            if(nth < 0) {
                nth += len;
                if(nth <= 0) {
                    return getRuntime().getNil();
                }
            }
            if(start[nth] == -1) {
                return getRuntime().getNil();
            }
            int st = start[nth];
            int llen = end[nth] - st;
            IRubyObject str = original.makeShared(st,llen);
            str.infectBy(this);
            return str;
        }

        public IRubyObject group(long _n) {
            int n = (int)_n;
            if(n < 0 || n >= len || start[n] == -1) {
                return getRuntime().getNil();
            }
            int st = start[n];
            int llen = end[n] - st;
            return original.makeShared(st,llen);
        }

        public RubyString pre_match() {
            RubyString str = (RubyString)original.makeShared(0,start[0]);
            if(isTaint()) {
                str.setTaint(true);
            }
            return str;
        }
        public RubyString post_match() {
            RubyString str = original;
            int pos = end[0];
            str = (RubyString)str.makeShared(pos,str.getByteList().length()-pos);
            if(isTaint()) {
                str.setTaint(true);
            }
            return str;
        }

        public RubyString string() {
            return original;
        }

        public RubyArray to_a() {
            return match_array(0);
        }

        private IRubyObject last_match() {
            return nth_match(0);
        }

        public IRubyObject to_s() {
            IRubyObject str = last_match();
            if(str.isNil()) {
                str = getRuntime().newString();
            }
            if(isTaint()) {
                str.setTaint(true);
            }
            if(original.isTaint()) {
                str.setTaint(true);
            }
            return str;
        }

        public IRubyObject doClone() {
            return new RString(getRuntime(), original, matcher);
        }

        public int matchStartPosition() {
            return start[0];
        }

        public int matchEndPosition() {
            return end[0];
        }
    }
}

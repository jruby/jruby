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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  amoore
 */
public class RubyMatchData extends RubyObject {
    private String str;
    private int[] begin;
    private int[] end;

    public RubyMatchData(IRuby runtime, String str, int[] begin, int[] end) {
        super(runtime, runtime.getClass("MatchData"));
        this.str = str;
        this.begin = begin;
        this.end = end;
    }

    public static RubyClass createMatchDataClass(IRuby runtime) {
        RubyClass matchDataClass = runtime.defineClass("MatchData", runtime.getObject());
        runtime.defineGlobalConstant("MatchingData", matchDataClass);

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyMatchData.class);

        matchDataClass.defineMethod("captures", callbackFactory.getMethod("captures"));
        matchDataClass.defineMethod("clone", callbackFactory.getMethod("rbClone"));
        matchDataClass.defineMethod("inspect", callbackFactory.getMethod("inspect"));
        matchDataClass.defineMethod("size", callbackFactory.getMethod("size"));
        matchDataClass.defineMethod("length", callbackFactory.getMethod("size"));
        matchDataClass.defineMethod("offset", callbackFactory.getMethod("offset", RubyFixnum.class));
        matchDataClass.defineMethod("begin", callbackFactory.getMethod("begin", RubyFixnum.class));
        matchDataClass.defineMethod("end", callbackFactory.getMethod("end", RubyFixnum.class));
        matchDataClass.defineMethod("to_a", callbackFactory.getMethod("to_a"));
        matchDataClass.defineMethod("[]", callbackFactory.getOptMethod("aref"));
        matchDataClass.defineMethod("pre_match", callbackFactory.getMethod("pre_match"));
        matchDataClass.defineMethod("post_match", callbackFactory.getMethod("post_match"));
        matchDataClass.defineMethod("to_s", callbackFactory.getMethod("to_s"));
        matchDataClass.defineMethod("string", callbackFactory.getMethod("string"));

        matchDataClass.getMetaClass().undefineMethod("new");

        return matchDataClass;
    }
    
    public IRubyObject captures() {
        RubyArray arr = getRuntime().newArray(begin.length);
        for (long i = 1; i < begin.length; i++) {
            arr.append(group(i));
        }
        return arr;
    }

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
        
        RubyArray arr = getRuntime().newArray(0);
        for (long i = beg; i < beg + len; i++) {
            arr.append(group(i));
        }
        return arr;
    }

    public long getSize() {
        return begin.length;
    }

    public IRubyObject group(long n) {
    	// Request an invalid group OR group is an empty match
        if (n < 0 || n >= getSize() || begin[(int) n] == -1) {
            return getRuntime().getNil();
        }

        return getRuntime().newString(
        		str.substring(begin[(int) n], end[(int) n]));
    }

    public int matchStartPosition() {
        return begin[0];
    }

    public int matchEndPosition() {
        return end[0];
    }

    private boolean outOfBounds(RubyFixnum index) {
        return outOfBounds(index.getLongValue());
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
        int argc = checkArgumentCount(args, 1, 2);
        if (argc == 2) {
            int beg = RubyNumeric.fix2int(args[0]);
            int len = RubyNumeric.fix2int(args[1]);
            if (beg < 0) {
                beg += getSize();
            }
            return subseq(beg, len);
        }
        if (args[0] instanceof RubyFixnum) {
            return group(RubyNumeric.fix2int(args[0]));
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
    public IRubyObject begin(RubyFixnum index) {
        long lIndex = index.getLongValue();
        long answer = begin(lIndex);
        
        return answer == -1 ? getRuntime().getNil() : getRuntime().newFixnum(answer);
    }
    
    public long begin(long index) {
        return outOfBounds(index) ? -1 : begin[(int) index];
    }

    /** match_end
     *
     */
    public IRubyObject end(RubyFixnum index) {
        int lIndex = RubyNumeric.fix2int(index);
        long answer = end(lIndex);

        return answer == -1 ? getRuntime().getNil() : getRuntime().newFixnum(answer);
    }
    
    public long end(long index) {
        return outOfBounds(index) ? -1 : end[(int) index]; 
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
    public IRubyObject offset(RubyFixnum index) {
        if (outOfBounds(index)) {
            return getRuntime().getNil();
        }
        return getRuntime().newArray(new IRubyObject[] { begin(index), end(index)});
    }

    /** match_pre_match
     *
     */
    public RubyString pre_match() {
        return getRuntime().newString(str.substring(0, begin[0]));
    }

    /** match_post_match
     *
     */
    public RubyString post_match() {
        return getRuntime().newString(str.substring(end[0]));
    }

    /** match_string
     *
     */
    public RubyString string() {
        RubyString frozenString = getRuntime().newString(str);
        frozenString.freeze();
        return frozenString;
    }

    /** match_to_a
     *
     */
    public RubyArray to_a() {
        RubyArray arr = getRuntime().newArray(begin.length);
        for (long i = 0; i < begin.length; i++) {
            arr.append(group(i));
        }
        return arr;
    }

    /** match_to_s
     *
     */
    public IRubyObject to_s() {
        return getRuntime().newString(str.substring(begin[0], end[0]));
    }

    /** match_clone
     *
     */
    public IRubyObject rbClone() {
        int len = (int) getSize();
        int[] begin_p = new int[len];
        int[] end_p = new int[len];
        System.arraycopy(begin, 0, begin_p, 0, len);
        System.arraycopy(end, 0, end_p, 0, len);
        return new RubyMatchData(getRuntime(), str, begin_p, end_p);
    }
}

/*
 * RubyMatchData.java - No description
 * Created on 18. Oktober 2001, 17:21
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby;

import org.jruby.exceptions.IndexError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  amoore
 * @version $Revision$
 */
public class RubyMatchData extends RubyObject {
    private String str;
    private int[] begin;
    private int[] end;

    public RubyMatchData(Ruby ruby, String str, int[] begin, int[] end) {
        super(ruby, ruby.getClass("MatchData"));
        this.str = str;
        this.begin = begin;
        this.end = end;
    }

    public static RubyClass createMatchDataClass(Ruby ruby) {
        RubyClass matchDataClass = ruby.defineClass("MatchData", ruby.getClasses().getObjectClass());
        ruby.defineGlobalConstant("MatchingData", matchDataClass);

        CallbackFactory callbackFactory = ruby.callbackFactory();

        matchDataClass.defineMethod("clone", callbackFactory.getMethod(RubyMatchData.class, "rbClone"));
        matchDataClass.defineMethod("size", callbackFactory.getMethod(RubyMatchData.class, "size"));
        matchDataClass.defineMethod("length", callbackFactory.getMethod(RubyMatchData.class, "size"));
        matchDataClass.defineMethod("offset", callbackFactory.getMethod(RubyMatchData.class, "offset", RubyFixnum.class));
        matchDataClass.defineMethod("begin", callbackFactory.getMethod(RubyMatchData.class, "begin", RubyFixnum.class));
        matchDataClass.defineMethod("end", callbackFactory.getMethod(RubyMatchData.class, "end", RubyFixnum.class));
        matchDataClass.defineMethod("to_a", callbackFactory.getMethod(RubyMatchData.class, "to_a"));
        matchDataClass.defineMethod("[]", callbackFactory.getOptMethod(RubyMatchData.class, "aref"));
        matchDataClass.defineMethod("pre_match", callbackFactory.getMethod(RubyMatchData.class, "pre_match"));
        matchDataClass.defineMethod("post_match", callbackFactory.getMethod(RubyMatchData.class, "post_match"));
        matchDataClass.defineMethod("to_s", callbackFactory.getMethod(RubyMatchData.class, "to_s"));
        matchDataClass.defineMethod("string", callbackFactory.getMethod(RubyMatchData.class, "string"));

        matchDataClass.getMetaClass().undefineMethod("new");

        return matchDataClass;
    }

    public RubyArray subseq(long beg, long len) {
        if (beg > getSize()) {
            return RubyArray.nilArray(getRuntime());
        }
        if (beg < 0 || len < 0) {
            return RubyArray.nilArray(getRuntime());
        }

        if (beg + len > getSize()) {
            len = getSize() - beg;
        }
        if (len < 0) {
            len = 0;
        }
        if (len == 0) {
            return RubyArray.newArray(getRuntime());
        }

        RubyArray arr = RubyArray.newArray(getRuntime(), 0);
        for (long i = beg; i < beg + len; i++) {
            arr.append(group(i));
        }
        return arr;
    }

    public long getSize() {
        return (long) begin.length;
    }

    public IRubyObject group(long n) {
        if (n < 0 || n >= getSize()) {
            return getRuntime().getNil();
        }
        return RubyString.newString(getRuntime(), str.substring(begin[(int) n], end[(int) n]));
    }

    public int matchStartPosition() {
        return begin[0];
    }

    public int matchEndPosition() {
        return end[0];
    }

    private boolean outOfBounds(RubyFixnum index) {
        long n = index.getLongValue();
        return (n < 0 || n >= getSize());
    }

    //
    // Methods of the MatchData Class:
    //

    /** match_aref
     *
     */
    public IRubyObject aref(IRubyObject[] args) {
        int argc = argCount(args, 1, 2);
        if (argc == 2) {
            long beg = RubyNumeric.fix2long(args[0]);
            long len = RubyNumeric.fix2long(args[1]);
            if (beg < 0) {
                beg += getSize();
            }
            return subseq(beg, len);
        }
        if (args[0] instanceof RubyFixnum) {
            return group(RubyNumeric.fix2long(args[0]));
        }
        if (args[0] instanceof RubyBignum) {
            throw new IndexError(getRuntime(), "index too big");
        }
        if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange) args[0]).getBeginLength(getSize(), true, false);
            if (begLen != null) {
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
        if (outOfBounds(index)) {
            return getRuntime().getNil();
        }
        return RubyFixnum.newFixnum(getRuntime(), begin[(int) index.getLongValue()]);
    }

    /** match_end
     *
     */
    public IRubyObject end(RubyFixnum index) {
        if (outOfBounds(index)) {
            return getRuntime().getNil();
        }
        return RubyFixnum.newFixnum(getRuntime(), end[(int) index.getLongValue()]);
    }

    /** match_size
     *
     */
    public RubyFixnum size() {
        return RubyFixnum.newFixnum(getRuntime(), getSize());
    }

    /** match_offset
     *
     */
    public IRubyObject offset(RubyFixnum index) {
        if (outOfBounds(index)) {
            return getRuntime().getNil();
        }
        return RubyArray.newArray(getRuntime(), new IRubyObject[] { begin(index), end(index)});
    }

    /** match_pre_match
     *
     */
    public RubyString pre_match() {
        return RubyString.newString(getRuntime(), str.substring(0, begin[0]));
    }

    /** match_post_match
     *
     */
    public RubyString post_match() {
        return RubyString.newString(getRuntime(), str.substring(end[0]));
    }

    /** match_string
     *
     */
    public RubyString string() {
        return RubyString.newString(getRuntime(), str);
    }

    /** match_to_a
     *
     */
    public RubyArray to_a() {
        return subseq(0, begin.length);
    }

    /** match_to_s
     *
     */
    public RubyString to_s() {
        return RubyString.newString(getRuntime(), str.substring(begin[0], end[0]));
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

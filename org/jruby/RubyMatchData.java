/*
 * RubyMatchData.java - No description
 * Created on 18. Oktober 2001, 17:21
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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

import org.jruby.exceptions.*;

/**
 *
 * @author  amoore
 * @version 
 */
public class RubyMatchData extends RubyObject {
    private String str;
    private int[] begin;
    private int[] end;
  
    public RubyMatchData(Ruby ruby, String str, int[] begin, int[] end) {
        super(ruby, ruby.getClasses().getMatchDataClass());
        this.str = str;
        this.begin = begin;
        this.end = end;
    }

    public RubyObject subseq(long beg, long len) {
        if (beg > size()) {
            return getRuby().getNil();
        }
        if (beg < 0 || len < 0) {
            return getRuby().getNil();
        }
        
        if (beg + len > size()) {
            len = size() - beg;
        }
        if (len < 0) {
            len = 0;
        }
        if (len == 0) {
            return RubyArray.m_newArray(getRuby());
        }
        
        RubyArray arr = RubyArray.m_newArray(getRuby());
        for (long i = beg; i < beg + len; i++) {
            arr.push(group(i));
        }
        return arr;
    }
    
    public long size() {
        return (long)begin.length;
    }

    public RubyObject group(long n) {
        if (n < 0 || n >= size()) {
            return getRuby().getNil();
        }
        return RubyString.m_newString(getRuby(), str.substring(begin[(int)n], end[(int)n]));
    }
    
    public int matchStartPosition() {
        return begin[0];
    }
    
    public int matchEndPosition() {
        return end[0];
    }
    
    private boolean outOfBounds(RubyFixnum index) {
        long n = index.getLongValue();
        return (n < 0 || n >= size());
    }
    
    //
    // Methods of the MatchData Class:
    //
    
    /** match_aref
     *
     */
    public RubyObject m_aref(RubyObject[] args) {
        RubyObject result = null;
        if (args.length == 2) {
            long beg = ((RubyFixnum)args[0]).getValue();
            long len = ((RubyFixnum)args[1]).getValue();
            if (beg < 0) {
                beg += size();
            }
            return subseq(beg, len);
        }
        if (args.length == 1) {
            if (args[0] instanceof RubyFixnum) {
                return group(((RubyFixnum)args[0]).getValue());
            }
            if (args[0] instanceof RubyBignum) {
                throw new RubyIndexException("index too big");
            }
            if (args[0] instanceof RubyRange) {
                long[] begLength = ((RubyRange)args[0]).getBeginLength(size());
                if (begLength != null) {
                    return subseq(begLength[0], begLength[1]);
                }
            }
        }
        return getRuby().getNil();
    }
    
    /** match_begin
     *
     */
    public RubyObject m_begin(RubyFixnum index) {
        if (outOfBounds(index)) {
            return getRuby().getNil();
        }
        return RubyFixnum.m_newFixnum(getRuby(), begin[(int)index.getValue()]);
    }

    /** match_end
     *
     */
    public RubyObject m_end(RubyFixnum index) {
        if (outOfBounds(index)) {
            return getRuby().getNil();
        }
        return RubyFixnum.m_newFixnum(getRuby(), end[(int)index.getValue()]);
    }

    /** match_size
     *
     */
    public RubyFixnum m_size() {
        return RubyFixnum.m_newFixnum(getRuby(), size());
    }

    /** match_offset
     *
     */
    public RubyObject m_offset(RubyFixnum index) {
        if (outOfBounds(index)) {
            return getRuby().getNil();
        }
        return RubyArray.m_create(getRuby(), new RubyObject[] { m_begin(index), m_end(index) });
    }

    /** match_pre_match
     *
     */
    public RubyString m_pre_match() {
        return RubyString.m_newString(getRuby(), str.substring(0, begin[0]));
    }

    /** match_post_match
     *
     */
    public RubyString m_post_match() {
        return RubyString.m_newString(getRuby(), str.substring(end[0]));
    }

    /** match_string
     *
     */
    public RubyString m_string() {
        return RubyString.m_newString(getRuby(), str);
    }

    /** match_to_a
     *
     */
    public RubyObject m_to_a() {
        RubyString[] arr = new RubyString[begin.length];
        return subseq(0, begin.length);
    }

    /** match_to_s
     *
     */
    public RubyString m_to_s() {
        return RubyString.m_newString(getRuby(), str.substring(begin[0], end[0]));
    }
    
    /** match_clone
     *
     */
    public RubyObject m_clone() {
        int len = (int)size();
        int[] begin_p = new int[len];
        int[] end_p = new int[len];
        System.arraycopy(begin, 0, begin_p, 0, len);
        System.arraycopy(end, 0, end_p, 0, len);
        return new RubyMatchData(getRuby(), new String(str), begin_p, end_p);
    }
}

/*
 * GNURegexpAdapter.java - No description
 * Created on 05. November 2001, 21:48
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

package org.jruby.regexp;

import gnu.regexp.*;

import org.jruby.Ruby;
import org.jruby.RubyMatchData;
import org.jruby.RubyObject;
import org.jruby.exceptions.RubyRegexpException;

/**
 * Regexp adapter for gnu.regexp.
 */
public class GNURegexpAdapter extends IRegexpAdapter {

    private RE re;
    private int cflags = 0;
    //private int eflags = RE.REG_NOTBOL | RE.REG_NOTEOL;
    private int eflags = 0;
    private boolean extended;

    /**
     * Compile the regex.
     */
    public void compile(Ruby ruby, String pattern) throws RubyRegexpException {
        if (extended) {
            pattern = unextend(pattern);
        }

        try {
            this.re = new RE(pattern, cflags);
        } catch (REException e) {
            throw new RubyRegexpException(ruby, e.getMessage());
        }
    }

    /**
     * Set whether matches should be case-insensitive or not
     */
    public void setCasefold(boolean set) {
        if (set) {
            cflags |= RE.REG_ICASE;
        } else {
            cflags &= ~RE.REG_ICASE;
        }
    }

    /**
     * Get whether matches are case-insensitive or not
     */
    public boolean getCasefold() {
        return (cflags & RE.REG_ICASE) > 0;
    }

    /**
     * Set whether patterns can contain comments and extra whitespace
     */
    public void setExtended(boolean set) {
        extended = set;
    }

    /**
     * Set whether the dot metacharacter should match newlines
     */
    public void setMultiline(boolean set) {
        if (set) {
            cflags |= RE.REG_DOT_NEWLINE;
        } else {
            cflags &= ~RE.REG_DOT_NEWLINE;
        }
    }

    /**
     * Does the given argument match the pattern?
     */
    public RubyObject search(Ruby ruby, String target, int startPos) {
        REMatch match = re.getMatch(target, startPos, eflags);
        if (match != null) {
            int count = re.getNumSubs() + 1;
            int[] begin = new int[count];
            int[] end = new int[count];
            for (int i = 0; i < count; i++) {
                begin[i] = match.getStartIndex(i);
                end[i] = match.getEndIndex(i);
            }
            return new RubyMatchData(ruby, target, begin, end);
        }
        return ruby.getNil();
    }
}

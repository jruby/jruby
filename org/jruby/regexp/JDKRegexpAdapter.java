/*
 * GNURegexpAdapter.java - No description
 * Created on 10. Oct 2001, 00:01
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby.regexp;

import java.util.regex.*;

import org.jruby.Ruby;
import org.jruby.RubyMatchData;
import org.jruby.RubyObject;
import org.jruby.exceptions.RubyRegexpException;

/**
 * Regexp adapter for gnu.regexp.
 */
public class JDKRegexpAdapter extends IRegexpAdapter {

    private Pattern pattern;
    private Matcher matcher;
    private int cflags = Pattern.MULTILINE;

    /**
     * Compile the regex.
     */
    public void compile(String regex) throws RubyRegexpException {
        try {
            pattern = Pattern.compile(regex, cflags);
        } catch (PatternSyntaxException e) {
            throw new RubyRegexpException(e.getMessage());
        }
    }

    /**
     * Set whether matches should be case-insensitive or not
     */
    public void setCasefold(boolean set) {
        if (set) {
            cflags |= Pattern.CASE_INSENSITIVE;
        } else {
            cflags &= ~ Pattern.CASE_INSENSITIVE;
        }
    }

    /**
     * Get whether matches are case-insensitive or not
     */
    public boolean getCasefold() {
        return (cflags & Pattern.CASE_INSENSITIVE) > 0;
    }

    /**
     * Set whether patterns can contain comments and extra whitespace
     */
    public void setExtended(boolean set) {
        if (set) {
            cflags |= Pattern.COMMENTS;
        } else {
            cflags &= ~Pattern.COMMENTS;
        }
    }

    /**
     * Set whether the dot metacharacter should match newlines
     */
    public void setMultiline(boolean set) {
        if (set) {
            cflags |= Pattern.DOTALL;
        } else {
            cflags &= ~Pattern.DOTALL;
        }
    }

    /**
     * Does the given argument match the pattern?
     */
    public RubyObject search(Ruby ruby, String target, int startPos) {
        if (matcher == null) {
            matcher = pattern.matcher(target);
        }
        if (matcher.find(startPos)) {
            int count = matcher.groupCount() + 1;
            int[] begin = new int[count];
            int[] end = new int[count];
            for (int i = 0; i < count; i++) {
                begin[i] = matcher.start(i);
                end[i] = matcher.end(i);
            }
            return new RubyMatchData(ruby, target, begin, end);
        }
        return ruby.getNil();
    }
}


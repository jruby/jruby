/*
 * IRegexpAdapter.java - No description
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

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.exceptions.RubyRegexpException;

/**
 * Regexp adapter base class.
 * This abstract class is used to decouple ruby from the actual regexp engine
 */
public abstract class IRegexpAdapter
{
    private static final String[] REGEXP_ADAPTER =
        { "org.jruby.regexp.JDKRegexpAdapter", "org.jruby.regexp.GNURegexpAdapter", "org.jruby.regexp.ORORegexpAdapter" };

    public static Class getAdapterClass() {
        for (int i = 0; i < REGEXP_ADAPTER.length; i++) {
            try {
                return Class.forName(REGEXP_ADAPTER[i]);
            } catch (ClassNotFoundException cnfExcptn) {
            } catch (NoClassDefFoundError ncdfError) {
            }
        }
        throw new RuntimeException("No regexp adapter found.");
    }

    /**
     * Compile the regex.
     */
    public abstract void compile(Ruby ruby, String pattern) throws RubyRegexpException;

    /**
     * Set whether matches should be case-insensitive or not
     */
    public abstract void setCasefold(boolean set) ;

    /**
     * Get whether matches are case-insensitive or not
     */
    public abstract boolean getCasefold() ;

    /**
     * Set whether patterns can contain comments and extra whitespace
     */
    public abstract void setExtended(boolean set) ;

    /**
     * Set whether the dot metacharacter should match newlines
     */
    public abstract void setMultiline(boolean set) ;

    /**
     * Does the given argument match the pattern?
     */
    public abstract RubyObject search(Ruby ruby, String target, int startPos) ;
    
    /**
     * Removes whitespace and comments from regexp, for those libs 
     * (like gnu.regexp) that don't support extended syntax.
     */
    public String unextend(String re) {
        boolean inClass = false;
        int len = re.length();
        StringBuffer sbuf = new StringBuffer(len);
        int pos = 0;
        char c;
        while (pos < len) {
            c = re.charAt(pos);
            if (c == '\\' && ++pos < len) {
                sbuf.append('\\').append(re.charAt(pos));
            } else if (c == '[' && ++pos < len) {
                sbuf.append(c);
                inClass = true;
                if ((c = re.charAt(pos)) == ']') {
                    sbuf.append(c);
                }
            } else if (c == ']') {
                sbuf.append(c);
                inClass = false;
            } else if (c == '#') {
                if (pos > 2 && re.charAt(pos-2) == '(' && re.charAt(pos-1) == '?') {
                    sbuf.append(c);
                } else {
                    pos++;
                    for (; pos < len && re.charAt(pos) != '\n' && re.charAt(pos) != '\r'; pos++);
                }
            } else if (inClass || !Character.isWhitespace(c)) {
                sbuf.append(c);
            }
            pos++;
        }
        return sbuf.toString();
    }
}


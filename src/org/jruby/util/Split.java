/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Thomas E Enebo
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby; 
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 * 
 */
package org.jruby.util;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyNumeric;
import org.jruby.RubyArray;
import org.jruby.RubyMatchData;
import org.jruby.Ruby;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Internals for String#split
 */
public class Split {
    private Ruby runtime;
    private int limit = 0;
    private RubyRegexp pattern;
    private String splitee;
    private List result = new ArrayList();

    public Split(Ruby runtime, String splitee, IRubyObject[] args) {
        if (args.length > 2) {
            throw new ArgumentError(runtime, args.length, 2);
        }
        this.splitee = splitee;
        this.runtime = runtime;
        this.pattern = getPattern(args);
        this.limit = getLimit(args);
    }

    public RubyArray results() {
        process();
        RubyArray resultArray = RubyArray.newArray(runtime, result.size());
        Iterator iter = result.iterator();
        while (iter.hasNext()) {
            resultArray.append(RubyString.newString(runtime, (String) iter.next()));
        }
        return resultArray;
    }

    private void process() {
        if (limit == 1) {
            result.add(splitee);
            return;
        }
        int pos = 0;
		int beg = 0;
		int hits = 0;
		int len = splitee.length();
        RubyString rubySplitee = RubyString.newString(runtime, splitee);
		while ((beg = pattern.search(rubySplitee, pos)) > -1) {
			hits++;
			RubyMatchData matchData = (RubyMatchData) runtime.getBackref();
			int end = matchData.matchEndPosition();
			// We do not want to add any elements if the split pattern is
			// at the front of the string;  Unless we match against an empty
			// re.  (This is icky as it is the side-effect of the ternary 
			// conditional in the addResult below).
			if (beg != 0 || beg == 0 && beg == end) {
			    addResult(substring(splitee, pos, (beg == pos && end == beg) ? 1 : beg - pos));
			    // Add to list any /(.)/ matched.
			    long extraPatterns = matchData.getSize();
			    for (int i = 1; i < extraPatterns; i++) {
			        addResult(((RubyString) matchData.group(i)).getValue());
			    }
			}

			pos = (end == beg) ? beg + 1 : end;
			if (hits + 1 == limit) {
				break;
			}
		}
		if (hits == 0) {
			addResult(splitee);
		} else if (pos <= len) {
			addResult(substring(splitee, pos, len - pos));
		}
		if (limit == 0) {
			while (((String) result.get(result.size() - 1)).length() == 0) {
				result.remove(result.size() - 1);
			}
		}
    }

    private int getLimit(IRubyObject[] args) {
        if (args.length == 2) {
            return RubyNumeric.fix2int(args[1]);
        } else {
            return 0;
        }
    }

    private RubyRegexp getPattern(IRubyObject[] args) {
        if (args.length == 0) {
            return RubyRegexp.newRegexp(runtime, "\\s+", 0);
        }
        if (args[0] instanceof RubyRegexp) {
            return RubyRegexp.regexpValue(args[0]);
        } else {
            String stringPattern = RubyString.stringValue(args[0]).getValue();
            
            if (stringPattern.equals(" ")) {
                return RubyRegexp.newRegexp(runtime, "\\s+", 0);
            } else {
                return RubyRegexp.newRegexp(runtime, unescapeString(stringPattern), 0);
            }
        }
    }

    // Perhaps somewhere else in jruby this can be done easier.
    // I did not rely on jdk1.4 (or I could use 1.4 regexp to do this).
    private String unescapeString(String unescapedString) {
        int length = unescapedString.length();
        char[] charsToEscape = {'|', '(', ')', '.', '*', 
                '[', ']', '^', '$', '\\'};
        StringBuffer buf = new StringBuffer();
        
        for (int i = 0; i < length; i++) {
            char c = unescapedString.charAt(i);
            
            for (int j = 0; j < charsToEscape.length; j++) {
                if (c == charsToEscape[j]) {
                    buf.append('\\');
                    break;
                }
            }
            
            buf.append(c);
        }
        
        return buf.toString();
    }

    private void addResult(String string) {
        if (string == null) {
            return;
        }
        result.add(string);
    }

	private String substring(String string, int start, int length) {
		int stringLength = string.length();
		if (length < 0 || start > stringLength) {
			return null;
		}
		if (start < 0) {
			start += stringLength;
			if (start < 0) {
				return null;
			}
		}
		int end = Math.min(stringLength, start + length);
		return string.substring(start, end);
	}
}
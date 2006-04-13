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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyMatchData;
import org.jruby.RubyNumeric;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Internals for String#split
 */
public class Split {
    private boolean isWhitespace = false;
    private IRuby runtime;
    private int limit = 0;
    private RubyRegexp pattern;
    private String splitee;
    private List result = new ArrayList();

    public Split(IRuby runtime, String splitee, IRubyObject[] args) {
        if (args.length > 2) {
            throw runtime.newArgumentError(args.length, 2);
        }
        this.splitee = splitee;
        this.runtime = runtime;
        this.pattern = getPattern(args);
        this.limit = getLimit(args);
    }

    public RubyArray results() {
        process();
        RubyArray resultArray = runtime.newArray(result.size());
        Iterator iter = result.iterator();
        while (iter.hasNext()) {
            resultArray.append(runtime.newString((String) iter.next()));
        }
        return resultArray;
    }

    private void process() {
        if (limit == 1) {
            result.add(splitee);
            return;
        }
        int last = 0;
		int beg = 0;
		int hits = 0;
		int len = splitee.length();
		while ((beg = pattern.searchAgain(splitee)) > -1) {
			hits++;
			RubyMatchData matchData = (RubyMatchData) runtime.getCurrentContext().getBackref();
			int end = matchData.matchEndPosition();

			// Skip first positive lookahead match
			if (beg == 0 && beg == end) {
				continue;
			}

			// Whitespace splits are supposed to ignore leading whitespace
			if (beg != 0 || !isWhitespace) {
			    addResult(substring(splitee, last, (beg == last && end == beg) ? 1 : beg - last));
			    // Add to list any /(.)/ matched.
			    long extraPatterns = matchData.getSize();
			    for (int i = 1; i < extraPatterns; i++) {
                    IRubyObject matchValue = matchData.group(i);
                    if (!matchValue.isNil()) {
                        addResult(((RubyString) matchValue).toString());
                    }
			    }
			}

			last = end;
			
			if (hits + 1 == limit) {
				break;
			}
		}
		if (hits == 0) {
			addResult(splitee);
		} else if (last <= len) {
			addResult(substring(splitee, last, len - last));
		}
		if (limit == 0 && result.size() > 0) {
			for (int size = result.size() - 1; 
			    size >= 0 && ((String) result.get(size)).length() == 0; size--) { 
				result.remove(size);
			}
		}
    }

    private static int getLimit(IRubyObject[] args) {
        if (args.length == 2) {
            return RubyNumeric.fix2int(args[1]);
        }
		return 0;
    }

    private RubyRegexp getPattern(IRubyObject[] args) {
        if (args.length == 0) {
            isWhitespace = true;
            return RubyRegexp.newRegexp(runtime, "\\s+", 0, null);
        }
        if (args[0] instanceof RubyRegexp) {
            // Even if we have whitespace-only explicit regexp we do not
            // mark it as whitespace.  Apparently, this is so ruby can
            // still get the do not ignore the front match behavior.
            return RubyRegexp.regexpValue(args[0]);
        }
		String stringPattern = RubyString.stringValue(args[0]).toString();
		
		if (stringPattern.equals(" ")) {
		    isWhitespace = true;
		    return RubyRegexp.newRegexp(runtime, "\\s+", 0, null);
		}
		return RubyRegexp.newRegexp(runtime, unescapeString(stringPattern), 0, null);
    }

    // Perhaps somewhere else in jruby this can be done easier.
    // I did not rely on jdk1.4 (or I could use 1.4 regexp to do this).
    private static String unescapeString(String unescapedString) {
        int length = unescapedString.length();
        char[] charsToEscape = {'|', '(', ')', '.', '*', 
                '[', ']', '^', '$', '\\', '?'};
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

	private static String substring(String string, int start, int length) {
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

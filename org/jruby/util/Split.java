/*
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
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
    private IRubyObject[] args;
    private int limit = 0;
    private RubyRegexp pattern;
    private String splitee;
    private List result = new ArrayList();

    public Split(Ruby runtime, RubyString splitee, IRubyObject[] args) {
        if (args.length > 2) {
            throw new ArgumentError(runtime, args.length, 2);
        }
        this.splitee = splitee.getValue();
        this.args = args;
        this.runtime = runtime;
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
		int argc = args.length;
		if (argc == 2) {
			limit = RubyNumeric.fix2int(args[1]);
		}
		int pos = 0;
		int beg = 0;
		int hits = 0;
		int len = splitee.length();
		if (argc > 0) {
			if (args[0] instanceof RubyRegexp) {
				pattern = RubyRegexp.regexpValue(args[0]);
			} else {
				RubyString stringPattern = RubyString.stringValue(args[0]);
				if (stringPattern.getValue().equals(" ")) {
					pattern = RubyRegexp.newRegexp(runtime, "\\s+", 0);
				} else {
					pattern = RubyRegexp.newRegexp(stringPattern, 0);
				}
			}
		} else {
			pattern = RubyRegexp.newRegexp(runtime, "\\s+", 0);
		}
        if (limit == 1) {
            result.add(splitee);
            return;
        }
		while ((beg = pattern.search(RubyString.newString(runtime, splitee), pos)) > -1) {
			hits++;
			int end = ((RubyMatchData) runtime.getBackref()).matchEndPosition();
			addResult(substring(splitee, pos, (beg == pos && end == beg) ? 1 : beg - pos));
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

    private void addResult(String string) {
        if (string == null) {
            return;
        }
        result.add(string);
    }

	private String substring(String str, int beg, int len) {
		int length = str.length();
		if (len < 0 || beg > length) {
			return null;
		}
		if (beg < 0) {
			beg += length;
			if (beg < 0) {
				return null;
			}
		}
		int end = Math.min(length, beg + len);
		return str.substring(beg, end);
	}
}
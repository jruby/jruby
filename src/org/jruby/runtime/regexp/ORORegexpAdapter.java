/*
 * Copyright (C) 2002 Takashi Okamoto <tora@debian.org>
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

package org.jruby.runtime.regexp;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.jruby.Ruby;
import org.jruby.RubyMatchData;
import org.jruby.exceptions.RegexpError;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Regexp adapter for Jakarta ORO.
 * @author Takashi Okamoto <tora@debian.org>
 * @version $Revision$
 */
public class ORORegexpAdapter extends IRegexpAdapter {

	private Pattern pattern;
	private int cflags = 0;
	private int eflags = 0;
	private boolean extended;

	/**
	 * Compile the regex.
	 */
	public void compile(Ruby ruby, String pattern) throws RegexpError {
		if (extended) {
			pattern = unextend(pattern);
		}

		try {
			PatternCompiler compiler = new Perl5Compiler();
			this.pattern = compiler.compile(pattern, cflags);
		} catch (MalformedPatternException e) {
			throw new RegexpError(ruby, e.getMessage());
		}
	}

	/**
	 * Set whether matches should be case-insensitive or not
	 */
	public void setCasefold(boolean set) {
		if (set) {
			cflags |= Perl5Compiler.CASE_INSENSITIVE_MASK;
		} else {
			cflags &= ~Perl5Compiler.CASE_INSENSITIVE_MASK;
		}
	}

	/**
	 * Get whether matches are case-insensitive or not
	 */
	public boolean getCasefold() {
		return (cflags & Perl5Compiler.CASE_INSENSITIVE_MASK) > 0;
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
			cflags |= Perl5Compiler.MULTILINE_MASK;
		} else {
			cflags &= ~Perl5Compiler.MULTILINE_MASK;
		}
	}

	/**
	 * Does the given argument match the pattern?
	 */
	public IRubyObject search(Ruby ruby, String target, int startPos) {
		PatternMatcherInput pmi = new PatternMatcherInput(target);
		pmi.setCurrentOffset(startPos);

		PatternMatcher matcher = new Perl5Matcher();
		boolean result = matcher.contains(pmi, pattern);

		if (result == true) {
			MatchResult match = matcher.getMatch();
			int count = match.groups();
			int[] begin = new int[count];
			int[] end = new int[count];
			for (int i = 0; i < count; i++) {
				begin[i] = match.beginOffset(i);
				end[i] = match.endOffset(i);
			}
			return new RubyMatchData(ruby, target, begin, end);
		}
		return ruby.getNil();
	}
}
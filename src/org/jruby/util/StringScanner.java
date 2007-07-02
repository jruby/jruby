package org.jruby.util;

import jregex.Matcher;
import jregex.Pattern;

/**
 * @author kscott
 *
 */
public class StringScanner {
	
	private String string;
	private Matcher matcher;
	private int pos = 0;
	private int lastPos = -1;
	private int matchStart = -1;
	private int matchEnd = -1;

	/**
	 * 
	 */
	public StringScanner() {
		this("");
	}
	
	public StringScanner(CharSequence string) {
		this.string = string.toString();
	}
	
	public boolean isEndOfString() {
		return pos == string.length();
	}
	
	public boolean isBeginningOfLine() {
		return pos == 0 || string.charAt(pos - 1) == '\n';
	}
	
	public CharSequence getString() {
		return string;
	}
	
	private void resetMatchData() {
		matcher = null;
		matchStart = -1;
		matchEnd = -1;
	}

	public void terminate() {
		pos = string.length();
		lastPos = -1;
		resetMatchData();
	}
	
	public void reset() {
		pos = 0;
		lastPos = -1;
		resetMatchData();
	}
	
	public void setString(CharSequence string) {
		this.string = string.toString();
		reset();
	}
	
	public void append(CharSequence string) {
		StringBuffer buf = new StringBuffer();
		// JDK 1.4 Doesn't have a constructor that takes a CharSequence
		buf.append(this.string);
		buf.append(string);
		this.string = buf.toString();
	}
	
	public CharSequence rest() {
		return string.subSequence(pos, string.length());
	}
	
	public int getPos() {
		return pos;
	}
	
	public void setPos(int pos) {
		if (pos > string.length()) {
			throw new IllegalArgumentException("index out of range.");
		}
		this.pos = pos;
	}
	
	public char getChar() {
		if (isEndOfString()) {
			return 0;
		} else {
			matcher = null;
			matchStart = pos;
			matchEnd = pos + 1;
			lastPos = pos;
			return string.charAt(pos++);
		}
	}
	
	public boolean matched() {
		return matchStart > -1;
	}
	
	public CharSequence group(int n) {
		if (!matched()) {
			return null;
		}
		if (matcher == null && matchEnd - matchStart == 1) {
			// Handle the getChar() is a match case
			return string.subSequence(matchStart, matchEnd);
		}
		if (n >= matcher.groupCount()) {
			return null;
		}
		return matcher.group(n);
	}
	
	public CharSequence preMatch() {
		if (matched()) {
			return string.subSequence(0, matchStart);
		} else {
			return null;
		}
	}
	
	public CharSequence postMatch() {
		if (matched()) {
			return string.subSequence(matchEnd, string.length());
		} else {
			return null;
		}
	}
	
	public CharSequence matchedValue() {
		if (matched()) {
			return string.subSequence(matchStart, matchEnd);
		} else {
			return null;
		}
	}
	
	public int matchedSize() {
		if (matcher == null) {
			return -1;
		} else {
			return matchEnd - matchStart;
		}
	}
	
	public void unscan() {
		if (lastPos != -1) {
			pos = lastPos;
			resetMatchData();
		} else {
			throw new IllegalStateException("unscan() cannot be called after an unmached scan.");
		}
	}
	
	public int matches(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string.subSequence(pos, string.length()).toString());
			if (matcher.find() && matcher.start() == 0) {
				matchStart = pos;
				matchEnd = matcher.end();
				return matchEnd;
			} else {
				resetMatchData();
			}
		}
		
		return -1;
	}
	
	public CharSequence scanUntil(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string);
            matcher.setOffset(pos);
			if (matcher.find()) {
				lastPos = pos;
				matchStart = matcher.start() + pos;
				matchEnd = matcher.end() + pos;
				pos = matchEnd;
				return string.subSequence(lastPos, pos);
			} else {
				lastPos = -1;
				resetMatchData();
			}
		}
		
		return null;
	}
	
	public CharSequence scan(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string.subSequence(pos, string.length()).toString());
			if (matcher.find() && matcher.start() == 0) {
				lastPos = pos;
				matchStart = pos;
				pos += matcher.end();
				matchEnd = pos;
				return matcher.group(0);
			} else {
				lastPos = -1;
				resetMatchData();
			}
		}
		
		return null;
	}
	
	public CharSequence check(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string.subSequence(pos, string.length()).toString());
			if (matcher.find() && matcher.start() == 0) {
				matchStart = pos;
				matchEnd = matchStart + matcher.end();
				return matcher.group(0);
			} else {
				resetMatchData();
			}
		}
		
		return null;
	}
	
	public CharSequence checkUntil(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string);
            matcher.setOffset(pos);
			if (matcher.find()) {
				matchStart = matcher.start() + pos;
				matchEnd = matcher.end() + pos;
				return string.subSequence(pos, matcher.end() + pos);
			} else {
				resetMatchData();
			}
		}
		
		return null;
	}
	
	public int skip(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string.subSequence(pos, string.length()).toString());
			if (matcher.find() && matcher.start() == 0) {
				lastPos = pos;
				matchStart = pos;
				int end = matcher.end();
				pos += end;
				matchEnd = pos;
				return end;
			} else {
				resetMatchData();
			}
		}
		
		return -1;
	}
	
	public int skipUntil(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string);
            matcher.setOffset(pos);
			if (matcher.find()) {
				lastPos = pos;
				pos = matcher.end() + lastPos;
				matchStart = matcher.start() + lastPos;
				matchEnd = pos;
				return pos - lastPos;
			} else {
				resetMatchData();
			}
		}
		
		return -1;
	}
	
	public int exists(Pattern pattern) {
		if (!isEndOfString()) {
			matcher = pattern.matcher(string);
            matcher.setOffset(pos);
			if (matcher.find()) {
				matchStart = matcher.start() + pos;
				matchEnd = matcher.end() + pos;
				return matchEnd - pos;
			} else {
				resetMatchData();
			}
		}
		
		return -1;
	}
	
	public CharSequence peek(int length) {
		int end = pos + length;
		if (end > string.length()) {
			end = string.length(); 
		}
		return string.subSequence(pos, end);
	}
}

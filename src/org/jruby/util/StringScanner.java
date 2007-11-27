package org.jruby.util;

import org.joni.Option;
import org.joni.Region;
import org.joni.Regex;


/*
 * TODO: this is INCREDIBLY inefficient right now, with Joni. should be rewritten to use matcher and so on
 */
/**
 * @author kscott
 *
 */
public class StringScanner {
	private String string;
	private int pos = 0;
	private int lastPos = -1;
	private int matchStart = -1;
	private int matchEnd = -1;
	private Region regs;

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
		regs = null;
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
			regs = null;
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
		if(!matched()) {
			return null;
		}
		if(regs == null && matchEnd - matchStart == 1) {
			// Handle the getChar() is a match case
			return string.subSequence(matchStart, matchEnd);
		}
		if(n >= regs.numRegs) {
			return null;
		}
		return string.subSequence(regs.beg[n]+lastPos,regs.end[n]+lastPos);
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
		if (regs == null) {
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
	
	public int matches(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) == 0) {
                matchStart = pos;
                matchEnd = regs.end[0]+pos;
            } else {
                resetMatchData();
            }
		}
		
		return -1;
	}
	
	public CharSequence scanUntil(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) >= 0) {
                lastPos = pos;
                matchStart = regs.beg[0]+pos;
                matchEnd = regs.end[0]+pos;
                pos = matchEnd;
                return string.subSequence(lastPos, pos);
            } else {
                lastPos = -1;
                resetMatchData();
            }
		}
		
		return null;
	}
	
	public CharSequence scan(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) == 0) {
                lastPos = pos;
                matchStart = pos;
                pos = regs.end[0]+lastPos;
                matchEnd = pos;
                return string.subSequence(regs.beg[0]+lastPos,regs.end[0]+lastPos);
            } else {
                lastPos = -1;
                resetMatchData();
            }
		}
		
		return null;
	}
	
	public CharSequence check(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) == 0) {
                matchStart = pos;
                matchEnd = regs.end[0]+pos;
                return string.subSequence(regs.beg[0]+pos,regs.end[0]+pos);
            } else {
                resetMatchData();
            }
		}
		
		return null;
	}
	
	public CharSequence checkUntil(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) >= 0) {
                matchStart = regs.beg[0]+pos;
                matchEnd = regs.end[0]+pos;
                return string.subSequence(pos,matchEnd);
            } else {
                resetMatchData();
            }
		}
		
		return null;
	}
	
	public int skip(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) == 0) {
                lastPos = pos;
                matchStart = pos;
                pos = regs.end[0]+lastPos;
                matchEnd = pos;
                return regs.end[0] + lastPos - lastPos;
            } else {
                resetMatchData();
            }
		}
		
		return -1;
	}
	
	public int skipUntil(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) >= 0) {
                lastPos = pos;
                pos = regs.end[0]+lastPos;
                matchStart = regs.beg[0]+lastPos;
                matchEnd = pos;
                return pos-lastPos;
            } else {
                resetMatchData();
            }
		}
		
		return -1;
	}
	
	public int exists(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            byte[] ccc = ByteList.plain(string);
            if(pattern.matcher(ccc,pos,ccc.length).search(pos,ccc.length,regs, Option.NONE) >= 0) {
                matchStart = regs.beg[0]+pos;
                matchEnd = regs.end[0]+pos;
                return matchEnd-pos;
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

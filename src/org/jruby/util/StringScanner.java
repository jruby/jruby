package org.jruby.util;

import org.joni.Option;
import org.joni.Region;
import org.joni.Regex;


/**
 * @author kscott
 *
 */
public class StringScanner {
	private ByteList string;
	private int pos = 0;
	private int lastPos = -1;
	private int matchStart = -1;
	private int matchEnd = -1;
	private Region regs;

	/**
	 * 
	 */
	public StringScanner() {
		this(new ByteList(0));
	}
	
	public StringScanner(ByteList string) {
		this.string = string;
	}
	
	public boolean isEndOfString() {
		return pos == string.realSize;
	}
	
	public boolean isBeginningOfLine() {
		return pos == 0 || string.bytes[string.begin + pos - 1] == '\n';
	}
	
	public ByteList getString() {
		return string;
	}
	
	private void resetMatchData() {
		regs = null;
		matchStart = -1;
		matchEnd = -1;
	}

	public void terminate() {
		pos = string.realSize;
		lastPos = -1;
		resetMatchData();
	}
	
	public void reset() {
		pos = 0;
		lastPos = -1;
		resetMatchData();
	}
	
	public void setString(ByteList string) {
		this.string = string;
		reset();
	}
	
	public void append(ByteList string) {
        this.string.append(string);
	}
	
	public ByteList rest() {
		return string.makeShared(pos,string.realSize-pos);
	}
	
	public int getPos() {
		return pos;
	}
	
	public void setPos(int pos) {
		if (pos > string.realSize) {
			throw new IllegalArgumentException("index out of range.");
		}
		this.pos = pos;
	}
	
	public byte getChar() {
		if (isEndOfString()) {
			return 0;
		} else {
			regs = null;
			matchStart = pos;
			matchEnd = pos + 1;
			lastPos = pos;
			return string.bytes[string.begin + pos++];
		}
	}
	
	public boolean matched() {
		return matchStart > -1;
	}
	
	public ByteList group(int n) {
		if(!matched()) {
			return null;
		}
		if(regs == null && matchEnd - matchStart == 1) {
			// Handle the getChar() is a match case
			return string.makeShared(matchStart, matchEnd-matchStart);
		}
		if(n >= regs.numRegs) {
			return null;
		}
		return string.makeShared(regs.beg[n]+lastPos,regs.end[n] - regs.beg[n]);
	}
	
	public ByteList preMatch() {
		if (matched()) {
			return string.makeShared(0, matchStart);
		} else {
			return null;
		}
	}
	
	public ByteList postMatch() {
		if (matched()) {
			return string.makeShared(matchEnd, string.realSize - matchEnd);
		} else {
			return null;
		}
	}
	
	public ByteList matchedValue() {
		if (matched()) {
			return string.makeShared(matchStart, matchEnd - matchStart);
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
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) == 0) {
                matchStart = pos;
                matchEnd = regs.end[0]+pos;
            } else {
                resetMatchData();
            }
		}
		
		return -1;
	}
	
	public ByteList scanUntil(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) >= 0) {
                lastPos = pos;
                matchStart = regs.beg[0]+pos;
                matchEnd = regs.end[0]+pos;
                pos = matchEnd;
                return string.makeShared(lastPos, pos-lastPos);
            } else {
                lastPos = -1;
                resetMatchData();
            }
		}
		
		return null;
	}
	
	public ByteList scan(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) == 0) {
                lastPos = pos;
                matchStart = pos;
                pos = regs.end[0]+lastPos;
                matchEnd = pos;
                return string.makeShared(regs.beg[0]+lastPos,regs.end[0] - regs.beg[0]);
            } else {
                lastPos = -1;
                resetMatchData();
            }
		}
		
		return null;
	}
	
	public ByteList check(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) == 0) {
                matchStart = pos;
                matchEnd = regs.end[0]+pos;
                return string.makeShared(regs.beg[0]+pos,regs.end[0]-regs.beg[0]);
            } else {
                resetMatchData();
            }
		}
		
		return null;
	}
	
	public ByteList checkUntil(Regex pattern) {
		if (!isEndOfString()) {
            if(regs == null) {
                regs = new Region();
            }
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) >= 0) {
                matchStart = regs.beg[0]+pos;
                matchEnd = regs.end[0]+pos;
                return string.makeShared(pos,matchEnd-pos);
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
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) == 0) {
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
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) >= 0) {
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
            if(pattern.matcher(string.bytes,string.begin+pos,string.begin+string.realSize).search(string.begin+pos,string.begin+string.realSize,regs, Option.NONE) >= 0) {
                matchStart = regs.beg[0]+pos;
                matchEnd = regs.end[0]+pos;
                return matchEnd-pos;
            } else {
                resetMatchData();
            }
		}
		
		return -1;
	}
	
	public ByteList peek(int length) {
		int end = pos + length;
		if (end > string.realSize) {
			end = string.realSize; 
		}
		return string.makeShared(pos, end-pos);
	}
}

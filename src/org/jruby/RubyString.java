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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.jruby;

import org.jruby.internal.runtime.methods.DirectInvocationMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.StringMetaClass;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Pack;
import org.jruby.util.PrintfFormat;
import org.jruby.util.Split;

/**
 *
 * @author  jpetersen
 */
public class RubyString extends RubyObject {
    // Default record seperator
    private static final String DEFAULT_RS = "\n";

	private static final String encoding = "ISO8859_1";

	private StringBuffer value;
    private CharSequence chars;
	
    public static abstract class StringMethod extends DirectInvocationMethod {
        public StringMethod(RubyModule implementationClass, Arity arity, Visibility visibility) {
            super(implementationClass, arity, visibility);
        }
        
        public IRubyObject internalCall(IRuby runtime, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
            RubyString s = (RubyString)receiver;
            
            return invoke(s, args);
        }
        
        public abstract IRubyObject invoke(RubyString target, IRubyObject[] args);
        
    };
    
    // @see IRuby.newString(...)
	private RubyString(IRuby runtime, CharSequence value) {
		this(runtime, runtime.getString(), value);
	}

	private RubyString(IRuby runtime, RubyClass rubyClass, CharSequence value) {
		super(runtime, rubyClass);

        assert value != null;

        // defer creation of StringBuffer until needed
        chars = value;
	}

	public Class getJavaClass() {
		return String.class;
	}

	public String toString() {
		return getValue().toString();
	}

	public static String bytesToString(byte[] bytes) {
		try {
			return new String(bytes, encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			assert false : "unsupported encoding " + e;
            return null;
		}
	}

	public static byte[] stringToBytes(String string) {
		try {
			return string.getBytes(encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			assert false : "unsupported encoding " + e;
            return null;
		}
	}

	public byte[] toByteArray() {
		return stringToBytes(toString());
	}

	public static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isUpper(char c) {
		return c >= 'A' && c <= 'Z';
	}

	public static boolean isLower(char c) {
		return c >= 'a' && c <= 'z';
	}

	public static boolean isLetter(char c) {
		return isUpper(c) || isLower(c);
	}

	public static boolean isAlnum(char c) {
		return isUpper(c) || isLower(c) || isDigit(c);
	}

	public static boolean isPrint(char c) {
		return c >= 0x20 && c <= 0x7E;
	}

    public RubyFixnum hash() {
        return getRuntime().newFixnum(toString().hashCode());
    }
    
    // Common enough check to make it a convenience method.
    private boolean sameAs(Object other) {
        return toString().equals(other.toString());
    }

	/** rb_obj_as_string
	 *
	 */
	public static RubyString objAsString(IRubyObject obj) {
		return (RubyString) (obj instanceof RubyString ? obj : 
			obj.callMethod("to_s"));
	}

	/** rb_str_cmp
	 *
	 */
	public int cmp(RubyString other) {
	    int cmp = toString().compareTo(other.toString());

		return cmp < 0 ? -1 : cmp > 0 ? 1 : 0; 
	}

	/** rb_to_id
	 *
	 */
	public String asSymbol() {
		return toString();
	}

	
	/** Create a new String which uses the same Ruby runtime and the same
	 *  class like this String.
	 *
	 *  This method should be used to satisfy RCR #38.
	 *
	 */
	public RubyString newString(CharSequence s) {
		return new RubyString(getRuntime(), getType(), s);
	}

	// Methods of the String class (rb_str_*):

	/** rb_str_new2
	 *
	 */
	public static RubyString newString(IRuby runtime, CharSequence str) {
		return new RubyString(runtime, str);
	}

	public static RubyString newString(IRuby runtime, byte[] bytes) {
		return runtime.newString(bytesToString(bytes));
	}


	/** rb_str_dup
	 *
	 */
	public IRubyObject dup() {
		return newString(toString()).infectBy(this);
	}

	/** rb_str_clone
	 *
	 */
	public IRubyObject rbClone() {
		IRubyObject newObject = dup();
		newObject.initCopy(this);
		newObject.setFrozen(isFrozen());
		return newObject;
	}
    
	public RubyString cat(CharSequence str) {
        getMutableValue().append(str);
		return this;
	}

	public IRubyObject to_str() {
		return this;
	}

	/** rb_str_replace_m
	 *
	 */
	public RubyString replace(IRubyObject other) {
		RubyString newValue = stringValue(other);
		if (this == other || sameAs(newValue)) {
			return this;
		}
		setValue(new StringBuffer(newValue.getValue().toString()));
		return (RubyString) infectBy(newValue); 
	}

	public RubyString reverse() {
		return newString(toString()).reverse_bang();
	}

	public RubyString reverse_bang() {
        getMutableValue().reverse();
		return this;
	}

	/** rb_str_s_new
	 *
	 */
	public static RubyString newInstance(IRubyObject recv, IRubyObject[] args) {
		RubyString newString = recv.getRuntime().newString("");
		newString.setMetaClass((RubyClass) recv);
		newString.callInit(args);
		return newString;
	}

	public IRubyObject initialize(IRubyObject[] args) {
	    if (checkArgumentCount(args, 0, 1) == 1) {
	        replace(args[0]);
	    }
	    return this;
	}

	public IRubyObject casecmp(IRubyObject other) {
		RubyString thisLCString = getRuntime().newString(toString().toLowerCase());
		RubyString lcString = getRuntime().newString(stringValue(other).toString().toLowerCase());

		return ((StringMetaClass)thisLCString.getMetaClass()).op_cmp.call(getRuntime(), thisLCString, thisLCString.getMetaClass(), "<=>", new IRubyObject[] {lcString}, false);
	}
    
	/** rb_str_match
	 *
	 */
	public IRubyObject match(IRubyObject other) {
		if (other instanceof RubyRegexp) {
			return ((RubyRegexp) other).match(this);
		} else if (other instanceof RubyString) {
			return RubyRegexp.newRegexp((RubyString) other, 0, null).match(this);
		}
		return other.callMethod("=~", this);
	}

	/** rb_str_match2
	 *
	 */
	public IRubyObject match2() {
		return RubyRegexp.newRegexp(this, 0, null).match2();
	}
	
	/**
	 * String#match(pattern)
	 * 
	 * @param pattern Regexp or String 
	 */
	public IRubyObject match3(IRubyObject pattern) {
		if (pattern instanceof RubyRegexp) {
			return ((RubyRegexp)pattern).search2(toString());
		} else if (pattern instanceof RubyString) {
			RubyRegexp regexp = RubyRegexp.newRegexp((RubyString) pattern, 0, null);
			return regexp.search2(toString());
		} 
		
		return getRuntime().getNil();
	}

	/** rb_str_capitalize
	 *
	 */
	public IRubyObject capitalize() {
        RubyString result = (RubyString) dup();
        result.capitalize_bang();
        return result;
	}

	/** rb_str_capitalize_bang
	 *
	 */
	public IRubyObject capitalize_bang() {
        if (isEmpty()) {
            return getRuntime().getNil();
        }
        StringBuffer buffer = new StringBuffer(toString().toLowerCase());
        char capital = buffer.charAt(0);
        if (Character.isLowerCase(capital)) {
            buffer.setCharAt(0, Character.toUpperCase(capital));
        }
        if (sameAs(buffer)) {
            return getRuntime().getNil();
        }
        setValue(buffer);
        return this;
	}

	/** rb_str_upcase
	 *
	 */
	public RubyString upcase() {
		return newString(toString().toUpperCase());
	}

	/** rb_str_upcase_bang
	 *
	 */
	public IRubyObject upcase_bang() {
		String result = toString().toUpperCase();
        if (sameAs(result)) {
            return getRuntime().getNil();
        }
        setValue(new StringBuffer(result));
		return this;
	}

	/** rb_str_downcase
	 *
	 */
	public RubyString downcase() {
		return newString(toString().toLowerCase());
	}

	/** rb_str_downcase_bang
	 *
	 */
	public IRubyObject downcase_bang() {
        String result = toString().toLowerCase();
        if (sameAs(result)) {
            return getRuntime().getNil();
        }
		setValue(new StringBuffer(result));
		return this;
	}

	/** rb_str_swapcase
	 *
	 */
	public RubyString swapcase() {
	    RubyString newString = newString(toString());
		IRubyObject swappedString = newString.swapcase_bang();
		
		return (RubyString) (swappedString.isNil() ? newString : swappedString);
	}

	/** rb_str_swapcase_bang
	 *
	 */
	public IRubyObject swapcase_bang() {
		StringBuffer string = getMutableValue();
        int length = string.length();
		boolean changesMade = false;

        for (int i = 0; i < length; i++) {
            char c = string.charAt(i);
            
            if (!Character.isLetter(c)) {
                continue;
            } else if (Character.isLowerCase(c)) {
                changesMade = true;
                string.setCharAt(i, Character.toUpperCase(c));
            } else {
                changesMade = true;
                string.setCharAt(i, Character.toLowerCase(c));
            }
        }

        return changesMade ? this : getRuntime().getNil(); 
	}

	/** rb_str_dump
	 *
	 */
	public RubyString dump() {
		return inspect(true);
	}
	
	public IRubyObject insert(IRubyObject indexArg, IRubyObject stringArg) {
	    int index = (int) indexArg.convertToInteger().getLongValue();
	    if (index < 0) {
	        index += getValue().length() + 1;
	    }
	    
	    if (index < 0 || index > getValue().length()) {
	    	throw getRuntime().newIndexError("index " + index + " out of range");
	    }
	    
	    String insert = stringArg.convertToString().toString();
	    
	    getMutableValue().insert(index, insert);
	    
	    return this;
	}

	/** rb_str_inspect
	 *
	 */
	public IRubyObject inspect() {
		return inspect(false);
	}

	private RubyString inspect(boolean dump) {
		final int length = getValue().length();

		StringBuffer sb = new StringBuffer(length + 2 + length / 100);

		sb.append('\"');

		for (int i = 0; i < length; i++) {
			char c = getValue().charAt(i);

			if (isAlnum(c)) {
				sb.append(c);
			} else if (c == '\"' || c == '\\') {
				sb.append('\\').append(c);
			} else if (dump && c == '#') {
				sb.append('\\').append(c);
			} else if (isPrint(c)) {
				sb.append(c);
			} else if (c == '\n') {
				sb.append('\\').append('n');
			} else if (c == '\r') {
				sb.append('\\').append('r');
			} else if (c == '\t') {
				sb.append('\\').append('t');
			} else if (c == '\f') {
				sb.append('\\').append('f');
			} else if (c == '\u000B') {
				sb.append('\\').append('v');
			} else if (c == '\u0007') {
				sb.append('\\').append('a');
			} else if (c == '\u001B') {
				sb.append('\\').append('e');
			} else {
				sb.append(new PrintfFormat("\\%.3o").sprintf(c));
			}
		}

		sb.append('\"');

		return getRuntime().newString(sb.toString());
	}

	/** rb_str_length
	 *
	 */
	public RubyFixnum length() {
		return getRuntime().newFixnum(getValue().length());
	}

	/** rb_str_empty
	 *
	 */
	public RubyBoolean empty() {
		return getRuntime().newBoolean(isEmpty());
	}

    private boolean isEmpty() {
        return getValue().length() == 0;
    }

    /** rb_str_append
	 *
	 */
	public RubyString append(IRubyObject other) {
		infectBy(other);
		return cat(stringValue(other).getValue());
	}

	/** rb_str_concat
	 *
	 */
	public RubyString concat(IRubyObject other) {
		if ((other instanceof RubyFixnum) && ((RubyFixnum) other).getLongValue() < 256) {
			return cat("" + (char) ((RubyFixnum) other).getLongValue());
		}
		return append(other);
	}

	/* rb_str_to_str */
	public static RubyString stringValue(IRubyObject object) {
		return (RubyString) (object instanceof RubyString ? object :
			object.convertType(RubyString.class, "String", "to_str"));
	}

	/** rb_str_sub
	 *
	 */
	public IRubyObject sub(IRubyObject[] args) {
		return sub(args, false);
	}

	/** rb_str_sub_bang
	 *
	 */
	public IRubyObject sub_bang(IRubyObject[] args) {
		return sub(args, true);
	}

	private IRubyObject sub(IRubyObject[] args, boolean bang) {
		IRubyObject repl = getRuntime().getNil();
		boolean iter = false;
        ThreadContext tc = getRuntime().getCurrentContext();
        
		if (args.length == 1 && tc.isBlockGiven()) {
			iter = true;
		} else if (args.length == 2) {
			repl = args[1];
		} else {
			throw getRuntime().newArgumentError("wrong number of arguments");
		}
		RubyRegexp pat = RubyRegexp.regexpValue(args[0]);

		if (pat.search(toString(), 0) >= 0) {
			RubyMatchData match = (RubyMatchData) tc.getBackref();
			RubyString newStr = match.pre_match();
			newStr.append(iter ? tc.yield(match.group(0)) : pat.regsub(repl, match));
			newStr.append(match.post_match());
			newStr.setTaint(isTaint() || repl.isTaint());
			if (bang) {
				replace(newStr);
				return this;
			}

			return newStr;
		}
        
        return bang ? getRuntime().getNil() : this; 
	}

	/** rb_str_gsub
	 *
	 */
	public IRubyObject gsub(IRubyObject[] args) {
		return gsub(args, false);
	}

	/** rb_str_gsub_bang
	 *
	 */
	public IRubyObject gsub_bang(IRubyObject[] args) {
		return gsub(args, true);
	}

	private IRubyObject gsub(IRubyObject[] args, boolean bang) {
		IRubyObject repl = getRuntime().getNil();
		RubyMatchData match;
		boolean iter = false;
        ThreadContext tc = getRuntime().getCurrentContext();
		if (args.length == 1 && tc.isBlockGiven()) {
			iter = true;
		} else if (args.length == 2) {
			repl = args[1];
		} else {
			throw getRuntime().newArgumentError("wrong number of arguments");
		}
		boolean taint = repl.isTaint();
		RubyRegexp pat = RubyRegexp.regexpValue(args[0]);

		int beg = pat.search(toString(), 0);
		if (beg < 0) {
			return bang ? getRuntime().getNil() : dup();
		}
		StringBuffer sbuf = new StringBuffer();
		String str = toString();
		IRubyObject newStr;
		int offset = 0;
		while (beg >= 0) {
			match = (RubyMatchData) tc.getBackref();
			sbuf.append(str.substring(offset, beg));
			newStr = iter ? tc.yield(match.group(0)) : pat.regsub(repl, match);
			taint |= newStr.isTaint();
            sbuf.append(newStr.toString());
			offset = match.matchEndPosition();
			beg = pat.search(toString(), offset == beg ? beg + 1 : offset);
		}

		sbuf.append(str.substring(offset, str.length()));
			
		if (bang) {
			setTaint(isTaint() || taint);
			setValue(sbuf);
			return this;
		}
		RubyString result = newString(sbuf.toString());
		result.setTaint(isTaint() || taint);
		return result;
	}

	/** rb_str_index_m
	 *
	 */
	public IRubyObject index(IRubyObject[] args) {
		return index(args, false);
	}

	/** rb_str_rindex_m
	 *
	 */
	public IRubyObject rindex(IRubyObject[] args) {
		return index(args, true);
	}

	/**
	 *	@fixme may be a problem with pos when doing reverse searches
	 */
	private IRubyObject index(IRubyObject[] args, boolean reverse) {
		//FIXME may be a problem with pos when doing reverse searches
		int pos = 0;
		if (reverse) {
			pos = getValue().length();
		}
		if (checkArgumentCount(args, 1, 2) == 2) {
			pos = RubyNumeric.fix2int(args[1]);
		}
		if (pos < 0) {
			pos += getValue().length();
			if (pos < 0) {
				return getRuntime().getNil();
			}
		}
		if (args[0] instanceof RubyRegexp) {
		    int doNotLookPastIfReverse = pos;
		    
		    // RubyRegexp doesn't (yet?) support reverse searches, so we
		    // find all matches and use the last one--very inefficient.
		    // XXX - find a better way
		    pos = ((RubyRegexp) args[0]).search(toString(), reverse ? 0 : pos);

			int dummy = pos;
			while (reverse && dummy > -1 && dummy <= doNotLookPastIfReverse) {
				pos = dummy;
				dummy = ((RubyRegexp) args[0]).search(toString(), pos + 1);
			}
		} else if (args[0] instanceof RubyString) {
			String sub = ((RubyString) args[0]).toString();
			pos = reverse ? getMutableValue().lastIndexOf(sub, pos) : getMutableValue().indexOf(sub, pos);
		} else if (args[0] instanceof RubyFixnum) {
			char c = (char) ((RubyFixnum) args[0]).getLongValue();
			pos = reverse ? toString().lastIndexOf(c, pos) : toString().indexOf(c, pos);
		} else {
			throw getRuntime().newArgumentError("wrong type of argument");
		}

        return pos == -1 ? getRuntime().getNil() : getRuntime().newFixnum(pos); 
	}

	/* rb_str_substr */
	private IRubyObject substr(int beg, int len) {
		int length = getValue().length();
		if (len < 0 || beg > length) {
			return getRuntime().getNil();
		}
		if (beg < 0) {
			beg += length;
			if (beg < 0) {
				return getRuntime().getNil();
			}
		}
		int end = Math.min(length, beg + len);
		return newString(getMutableValue().substring(beg, end)).infectBy(this);
	}

	/* rb_str_replace */
	private IRubyObject replace(int beg, int len, RubyString replaceWith) {
		if (beg + len >= getValue().length()) { 
			len = getValue().length() - beg;
		}
        
        getMutableValue().delete(beg, beg + len);
        getMutableValue().insert(beg, replaceWith.toString());
        
		return infectBy(replaceWith); 
	}

	/** rb_str_aref, rb_str_aref_m
	 *
	 */
	public IRubyObject aref(IRubyObject[] args) {
	    if (checkArgumentCount(args, 1, 2) == 2) {
            if (args[0] instanceof RubyRegexp) {
                IRubyObject match = RubyRegexp.regexpValue(args[0]).match(toString(), 0);
                long idx = args[1].convertToInteger().getLongValue();
                return RubyRegexp.nth_match((int) idx, match);
            } 
	        return substr(RubyNumeric.fix2int(args[0]), RubyNumeric.fix2int(args[1]));
	    }
        
	    if (args[0] instanceof RubyRegexp) {
	    	return RubyRegexp.regexpValue(args[0]).search(toString(), 0) >= 0 ?
	            RubyRegexp.last_match(getRuntime().getCurrentContext().getBackref()) :
	            	getRuntime().getNil();
	    } else if (args[0] instanceof RubyString) {
	        return toString().indexOf(stringValue(args[0]).toString()) != -1 ?
	            args[0] : getRuntime().getNil();
	    } else if (args[0] instanceof RubyRange) {
	        long[] begLen = ((RubyRange) args[0]).getBeginLength(getValue().length(), true, false);
	        return begLen == null ? getRuntime().getNil() :
	        	substr((int) begLen[0], (int) begLen[1]);
	    }
	    int idx = (int) args[0].convertToInteger().getLongValue();
	    if (idx < 0) {
	        idx += getValue().length();
	    }
	    return idx < 0 || idx >= getValue().length() ? getRuntime().getNil() : 
	    	getRuntime().newFixnum(getValue().charAt(idx));
	}
    
    /**
     * rb_str_subpat_set
     * 
     */
    private void subpatSet(RubyRegexp regexp, int nth, IRubyObject repl) {
        int found = regexp.search(this.toString(), 0);
        if (found == -1) {
            throw getRuntime().newIndexError("regexp not matched");
        }

        RubyMatchData match = (RubyMatchData) getRuntime().getCurrentContext()
                .getBackref();

        if (nth >= match.getSize()) {
            throw getRuntime().newIndexError("index " + nth + " out of regexp");
        }
        if (nth < 0) {
            if (-nth >= match.getSize()) {
                throw getRuntime().newIndexError("index " + nth + " out of regexp");
            }
            nth += match.getSize();
        }

        IRubyObject group = match.group(nth);
        if (getRuntime().getNil().equals(group)) {
            throw getRuntime().newIndexError(
                    "regexp group " + nth + " not matched");
        }

        int beg = (int) match.begin(nth);
        int len = (int) (match.end(nth) - beg);

        replace(beg, len, stringValue(repl));

    }

	/** rb_str_aset, rb_str_aset_m
	 *
	 */
	public IRubyObject aset(IRubyObject[] args) {
		testFrozen("class");
		int strLen = getValue().length();
		if (checkArgumentCount(args, 2, 3) == 3) {
            if (args[0] instanceof RubyFixnum) {
                RubyString repl = stringValue(args[2]);
                int beg = RubyNumeric.fix2int(args[0]);
                int len = RubyNumeric.fix2int(args[1]);
                if (len < 0) {
                    throw getRuntime().newIndexError("negative length");
                }
                if (beg < 0) {
                    beg += strLen;
                }
                if (beg < 0 || (beg > 0 && beg >= strLen)) {
                    throw getRuntime().newIndexError(
                            "string index out of bounds");
                }
                if (beg + len > strLen) {
                    len = strLen - beg;
                }
                replace(beg, len, repl);
                return repl;
            }
            if (args[0] instanceof RubyRegexp) {
                RubyString repl = stringValue(args[2]);
                int nth = RubyNumeric.fix2int(args[1]);
                subpatSet((RubyRegexp) args[0], nth, repl); 
                return repl;
            }
		}
		if (args[0] instanceof RubyFixnum) { // RubyNumeric?
			int idx = RubyNumeric.fix2int(args[0]); // num2int?
			if (idx < 0) {
				idx += getValue().length();
			}
			if (idx < 0 || idx >= getValue().length()) {
				throw getRuntime().newIndexError("string index out of bounds");
			}
			if (args[1] instanceof RubyFixnum) {
                getMutableValue().setCharAt(idx, (char) RubyNumeric.fix2int(args[1])); 
			} else {
				replace(idx, 1, stringValue(args[1]));
			}
			return args[1];
		}
		if (args[0] instanceof RubyRegexp) {
			sub_bang(args);
			return args[1];
		}
		if (args[0] instanceof RubyString) {
			RubyString orig = stringValue(args[0]);
			int beg = toString().indexOf(orig.toString());
			if (beg != -1) {
				replace(beg, orig.getValue().length(), stringValue(args[1]));
			}
			return args[1];
		}
		if (args[0] instanceof RubyRange) {
			long[] idxs = ((RubyRange) args[0]).getBeginLength(getValue().length(), true, true);
			replace((int) idxs[0], (int) idxs[1], stringValue(args[1]));
			return args[1];
		}
		throw getRuntime().newTypeError("wrong argument type");
	}

	/** rb_str_slice_bang
	 *
	 */
	public IRubyObject slice_bang(IRubyObject[] args) {
		int argc = checkArgumentCount(args, 1, 2);
		IRubyObject[] newArgs = new IRubyObject[argc + 1];
		newArgs[0] = args[0];
		if (argc > 1) {
			newArgs[1] = args[1];
		}
		newArgs[argc] = newString("");
		IRubyObject result = aref(args);
		if (result.isNil()) {
			return result;
		}
		aset(newArgs);
		return result;
	}

	public IRubyObject succ() {
        return ((RubyString) dup()).succ_bang();
	}

	public IRubyObject succ_bang() {
        if (getValue().length() == 0) {
            return this;
        }
        
        StringBuffer sbuf = getMutableValue();
        boolean alnumSeen = false;
        int pos = -1;
        char c = 0;
        char n = 0;
        for (int i = sbuf.length() - 1; i >= 0; i--) {
            c = sbuf.charAt(i);
            if (isAlnum(c)) {
                alnumSeen = true;
                if ((isDigit(c) && c < '9') || (isLower(c) && c < 'z') || (isUpper(c) && c < 'Z')) {
                    sbuf.setCharAt(i, (char) (c + 1));
                    pos = -1;
                    break;
                }
                pos = i;
                n = isDigit(c) ? '0' : (isLower(c) ? 'a' : 'A');
                sbuf.setCharAt(i, n);
            }
        }
        if (!alnumSeen) {
            for (int i = sbuf.length() - 1; i >= 0; i--) {
                c = sbuf.charAt(i);
                if (c < 0xff) {
                    sbuf.setCharAt(i, (char) (c + 1));
                    pos = -1;
                    break;
                }
                pos = i;
                n = '\u0001';
                sbuf.setCharAt(i, '\u0000');
            }
        }
        if (pos > -1) {
            // This represents left most digit in a set of incremented
            // values?  Therefore leftmost numeric must be '1' and not '0'
            // 999 -> 1000, not 999 -> 0000.  whereas chars should be 
            // zzz -> aaaa
            sbuf.insert(pos, isDigit(c) ? '1' : (isLower(c) ? 'a' : 'A'));
        }

        return this;
	}

	/** rb_str_upto_m
	 *
	 */
	public IRubyObject upto(IRubyObject str) {
		return upto(str, false);
	}

    /* rb_str_upto */
    public IRubyObject upto(IRubyObject str, boolean excl) {
        // alias 'this' to 'beg' for ease of comparison with MRI
        RubyString beg = this;
        RubyString end = stringValue(str);

        int n = beg.cmp(end);
        if (n > 0 || (excl && n == 0)) {
            return beg;
        }
        
        RubyString afterEnd = stringValue(end.succ());
        RubyString current = beg;

        while (!current.equals(afterEnd)) {
            getRuntime().getCurrentContext().yield(current);
            if (!excl && current.equals(end)) {
                break;
            }
            
            current = (RubyString) current.succ();
            if (excl && current.equals(end)) {
                break;
            }
            if (current.length().getLongValue() > end.length().getLongValue()) {
                break;
            }
        }

        return beg;

    }


	/** rb_str_include
	 *
	 */
	public RubyBoolean include(IRubyObject obj) {
		if (obj instanceof RubyFixnum) {
			char c = (char) RubyNumeric.fix2int(obj);
			return getRuntime().newBoolean(toString().indexOf(c) != -1);
		}
		String str = stringValue(obj).toString();
		return getRuntime().newBoolean(getMutableValue().indexOf(str) != -1);
	}

	/** rb_str_to_i
	 *
	 */
	public IRubyObject to_i(IRubyObject[] args) {
		long base = checkArgumentCount(args, 0, 1) == 0 ? 10 : ((RubyInteger) args[0].convertType(RubyInteger.class,
                "Integer", "to_i")).getLongValue();
		return RubyNumeric.str2inum(getRuntime(), this, (int) base);
	}

	/** rb_str_oct
	 *
	 */
	public IRubyObject oct() {
		if (isEmpty()) {
			return getRuntime().newFixnum(0);
		}
		
		int base = 8;
		String str = toString().trim();
		int pos = (str.charAt(0) == '-' || str.charAt(0) == '+') ? 1 : 0;
		if (str.indexOf("0x") == pos || str.indexOf("0X") == pos) {
			base = 16;
		} else if (str.indexOf("0b") == pos || str.indexOf("0B") == pos) {
			base = 2;
		}
		return RubyNumeric.str2inum(getRuntime(), this, base);
	}

	/** rb_str_hex
	 *
	 */
	public IRubyObject hex() {
		return RubyNumeric.str2inum(getRuntime(), this, 16);
	}

	/** rb_str_to_f
	 *
	 */
	public IRubyObject to_f() {
		return RubyNumeric.str2fnum(getRuntime(), this);
	}

	/** rb_str_split
	 *
	 */
	public RubyArray split(IRubyObject[] args) {
        return new Split(getRuntime(), this.toString(), args).results();
	}

	/** rb_str_scan
	 *
	 */
	public IRubyObject scan(IRubyObject arg) {
		RubyRegexp pattern = RubyRegexp.regexpValue(arg);
		int start = 0;
        ThreadContext tc = getRuntime().getCurrentContext();
        
		if (!tc.isBlockGiven()) {
			RubyArray ary = getRuntime().newArray();
			while (pattern.search(toString(), start) != -1) {
				RubyMatchData md = (RubyMatchData) tc.getBackref();
                ary.append(md.getSize() == 1 ? md.group(0) : md.subseq(1, md.getSize()));

                if (md.matchEndPosition() == md.matchStartPosition()) {
					start++;
				} else {
					start = md.matchEndPosition();
				}
			}
			return ary;
		}

		while (pattern.search(toString(), start) != -1) {
			RubyMatchData md = (RubyMatchData) tc.getBackref();
            tc.yield(md.getSize() == 1 ? md.group(0) : md.subseq(1, md.getSize()));

            if (md.matchEndPosition() == md.matchStartPosition()) {
				start++;
			} else {
				start = md.matchEndPosition();
			}
		}
		return this;
	}
    
    private IRubyObject justify(IRubyObject [] args, boolean leftJustify) {
        checkArgumentCount(args, 1, 2);
        int length = RubyNumeric.fix2int(args[0]);
        if (length <= getValue().length()) {
            return dup();
        }
        
        String paddingArg;
        
        if (args.length == 2) {
            paddingArg = args[1].convertToString().toString();
            if (paddingArg.length() == 0) {
                throw getRuntime().newArgumentError("zero width padding");
            }
        } else {
            paddingArg = " ";
        }
            
        StringBuffer sbuf = new StringBuffer(length);
        String thisStr = toString();
        
        if (leftJustify) {
            sbuf.append(thisStr);
        }
        
        // Add n whole paddings
        int whole = (length - thisStr.length()) / paddingArg.length();
        for (int w = 0; w < whole; w++ ) {
            sbuf.append(paddingArg);
        }
        
        // Add fractional amount of padding to make up difference
        int fractionalLength = (length - thisStr.length()) % paddingArg.length();
        if (fractionalLength > 0) {
            sbuf.append(paddingArg.substring(0, fractionalLength));
        }
        
        if (!leftJustify) {
            sbuf.append(thisStr);
        }
        
        RubyString ret = newString(sbuf.toString());
        
        if (args.length == 2) {
            ret.infectBy(args[1]);
        }
        
        return ret;
    }

	/** rb_str_ljust
	 *
	 */
	public IRubyObject ljust(IRubyObject [] args) {
        return justify(args, true);
	}

	/** rb_str_rjust
	 *
	 */
    public IRubyObject rjust(IRubyObject [] args) {
        return justify(args, false);
    }
    
	public IRubyObject center(IRubyObject[] args) {
        checkArgumentCount(args, 1, 2);
		int len = RubyNumeric.fix2int(args[0]);
        String pad = args.length == 2 ? args[1].convertToString().toString() : " ";
		int strLen = getValue().length();
        int padLen = pad.length();
        
        if (padLen == 0) {
            throw getRuntime().newArgumentError("zero width padding");
        }
		if (len <= strLen) {
			return dup();
		}
		StringBuffer sbuf = new StringBuffer(len);
        int lead = (len - strLen) / 2;
        for (int i = 0; i < lead; i++) {
            sbuf.append(pad.charAt(i % padLen));
        }
        sbuf.append(getValue());
        int remaining = len - (lead + strLen);
        for (int i = 0; i < remaining; i++) {
            sbuf.append(pad.charAt(i % padLen));
        }
		return newString(sbuf.toString());
	}

	public IRubyObject chop() {
        RubyString newString = (RubyString) dup();
        
        newString.chop_bang();

        return newString;
	}

	public IRubyObject chop_bang() {
        int end = getValue().length() - 1;
        
        if (end < 0) {
            return getRuntime().getNil();
        }
        
        if (getValue().charAt(end) == '\n') {
            if (end > 0 && getValue().charAt(end-1) == '\r') {
                end--;
            }
        } 
        
        getMutableValue().delete(end, getValue().length());
		return this;
	}

	public RubyString chomp(IRubyObject[] args) {
        RubyString result = (RubyString) dup();
        
        result.chomp_bang(args);
        
        return result;
	}

	/** 
     * rb_str_chomp_bang
     * 
	 * In the common case, removes CR and LF characters in various ways depending on the value of
     *   the optional args[0].
     * If args.length==0 removes one instance of CR, CRLF or LF from the end of the string.  
     * If args.length>0 and args[0] is "\n" then same behaviour as args.length==0 .
     * If args.length>0 and args[0] is "" then removes trailing multiple LF or CRLF (but no CRs at
     *   all(!)).  
     * @param args See method description.
	 */
	public IRubyObject chomp_bang(IRubyObject[] args) {
        if (isEmpty()) {
            return getRuntime().getNil();
        }

        // Separator (a.k.a. $/) can be overriden by the -0 (zero) command line option
        String separator = (args.length == 0) ? 
                getRuntime().getGlobalVariables().get("$/").asSymbol() : args[0].asSymbol();
        
        if (separator.equals(DEFAULT_RS)) {
            int end = getValue().length() - 1;
            int removeCount = 0;
            
            if (end < 0) {
                return getRuntime().getNil();
            }
            
            if (getValue().charAt(end) == '\n') {
                removeCount++;
                if (end > 0 && getValue().charAt(end-1) == '\r') {
                    removeCount++;
                }
            } else if (getValue().charAt(end) == '\r') {
                removeCount++;
            }
            
            if (removeCount == 0) {
                return getRuntime().getNil();
            }
            
            getMutableValue().delete(end - removeCount + 1, getValue().length());
            return this;
        }

        if (separator.length() == 0) {
            int end = getValue().length() - 1;
            int removeCount = 0;
            while(end - removeCount >= 0 && getValue().charAt(end - removeCount) == '\n') {
                removeCount++;
                if (end - removeCount >= 0 && getValue().charAt(end - removeCount) == '\r') {
                    removeCount++;
                }
            }
            if (removeCount == 0) {
                return getRuntime().getNil();
            }
            
            getMutableValue().delete(end - removeCount + 1, getValue().length());
            return this;
        }
        
        // Uncommon case of str.chomp!("xxx")
        if (toString().endsWith(separator)) {
            getMutableValue().delete(getValue().length() - separator.length(), getValue().length());
            return this;
        }
		return getRuntime().getNil();
	}

	public IRubyObject lstrip() {
		int length = getValue().length();
		int i = 0;
		
		for (; i < length; i++) {
			if (!Character.isWhitespace(getValue().charAt(i))) {
				break;
			}
		}
		
		return newString(getValue().subSequence(i, getValue().length()));
	}
	
	public IRubyObject lstrip_bang() {
		RubyString newValue = (RubyString) lstrip();
		
		if (sameAs(newValue)) {
			return getRuntime().getNil();
		}
		setValue(newValue.getValue());
		
		return this;
	}

	public IRubyObject rstrip() {
		int i = getValue().length() - 1;
		
		for (; i >= 0; i--) {
			if (!Character.isWhitespace(getValue().charAt(i))) {
				break;
			}
		}
		
		return newString(getValue().subSequence(0, i + 1));
	}

	public IRubyObject rstrip_bang() {
		RubyString newValue = (RubyString) rstrip();
		
		if (sameAs(newValue)) {
			return getRuntime().getNil();
		}
		setValue(newValue.getValue());
		
		return this;
	}

	/** rb_str_strip
	 *
	 */
	public IRubyObject strip() {
		if (isEmpty()) {
			return dup();
		}
		return newString(toString().trim());
	}

	/** rb_str_strip_bang
	 *
	 */
	public IRubyObject strip_bang() {
		if (isEmpty()) {
			return getRuntime().getNil();
		}
		String newStr = toString().trim();
		if (sameAs(newStr)) {
			return getRuntime().getNil();
		}
		setValue(new StringBuffer(newStr));
		return this;
	}

	private static String expandTemplate(String spec, boolean invertOK) {
		int len = spec.length();
		if (len <= 1) {
			return spec;
		}
		StringBuffer sbuf = new StringBuffer();
		int pos = (invertOK && spec.charAt(0) == '^') ? 1 : 0;
		while (pos < len) {
			char c1 = spec.charAt(pos), c2;
			if (pos + 2 < len && spec.charAt(pos + 1) == '-') {
				if ((c2 = spec.charAt(pos + 2)) > c1) {
					for (int i = c1; i <= c2; i++) {
						sbuf.append((char) i);
					}
				}
				pos += 3;
				continue;
			}
			sbuf.append(c1);
			pos++;
		}
		return sbuf.toString();
	}

	private String setupTable(String[] specs) {
		int[] table = new int[256];
		int numSets = 0;
		for (int i = 0; i < specs.length; i++) {
			String template = expandTemplate(specs[i], true);
			boolean invert = specs[i].length() > 1 && specs[i].charAt(0) == '^';
			for (int j = 0; j < 256; j++) {
				if (template.indexOf(j) != -1) {
					table[j] += invert ? -1 : 1;
				}
			}
			numSets += invert ? 0 : 1;
		}
		StringBuffer sbuf = new StringBuffer();
		for (int k = 0; k < 256; k++) {
			if (table[k] == numSets) {
				sbuf.append((char) k);
			}
		}
		return sbuf.toString();
	}

	/** rb_str_count
	 *
	 */
	public IRubyObject count(IRubyObject[] args) {
		int argc = checkArgumentCount(args, 1, -1);
		String[] specs = new String[argc];
		for (int i = 0; i < argc; i++) {
			specs[i] = stringValue(args[i]).toString();
		}
		String table = setupTable(specs);

		int count = 0;
		for (int j = 0; j < getValue().length(); j++) {
			if (table.indexOf(getValue().charAt(j)) != -1) {
				count++;
			}
		}
		return getRuntime().newFixnum(count);
	}

	private String getDelete(IRubyObject[] args) {
		int argc = checkArgumentCount(args, 1, -1);
		String[] specs = new String[argc];
		for (int i = 0; i < argc; i++) {
			specs[i] = stringValue(args[i]).toString();
		}
		String table = setupTable(specs);

		int strLen = getValue().length();
		StringBuffer sbuf = new StringBuffer(strLen);
		char c;
		for (int j = 0; j < strLen; j++) {
			c = getValue().charAt(j);
			if (table.indexOf(c) == -1) {
				sbuf.append(c);
			}
		}
		return sbuf.toString();
	}

	/** rb_str_delete
	 *
	 */
	public IRubyObject delete(IRubyObject[] args) {
		return newString(getDelete(args)).infectBy(this);
	}

	/** rb_str_delete_bang
	 *
	 */
	public IRubyObject delete_bang(IRubyObject[] args) {
		String newStr = getDelete(args);
		if (sameAs(newStr)) {
			return getRuntime().getNil();
		}
		setValue(new StringBuffer(newStr));
		return this;
	}

	private StringBuffer getSqueeze(IRubyObject[] args) {
		int argc = args.length;
		String[] specs = null;
		if (argc > 0) {
			specs = new String[argc];
			for (int i = 0; i < argc; i++) {
				specs[i] = stringValue(args[i]).toString();
			}
		}
		String table = specs == null ? null : setupTable(specs);

		int strLen = getValue().length();
		if (strLen <= 1) {
			return getMutableValue();
		}
		StringBuffer sbuf = new StringBuffer(strLen);
		char c1 = getValue().charAt(0);
		sbuf.append(c1);
		char c2;
		for (int j = 1; j < strLen; j++) {
			c2 = getValue().charAt(j);
			if (c2 == c1 && (table == null || table.indexOf(c2) != -1)) {
				continue;
			}
			sbuf.append(c2);
			c1 = c2;
		}
		return sbuf;
	}

	/** rb_str_squeeze
	 *
	 */
	public IRubyObject squeeze(IRubyObject[] args) {
		return newString(getSqueeze(args).toString()).infectBy(this);
	}

	/** rb_str_squeeze_bang
	 *
	 */
	public IRubyObject squeeze_bang(IRubyObject[] args) {
		StringBuffer newStr = getSqueeze(args);
		if (sameAs(newStr)) {
			return getRuntime().getNil();
		}
		setValue(newStr);
		return this;
	}

	private String tr(IRubyObject search, IRubyObject replace, boolean squeeze) {
		String srchSpec = search.convertToString().toString();
		String srch = expandTemplate(srchSpec, true);
		if (srchSpec.startsWith("^")) {
			StringBuffer sbuf = new StringBuffer(256);
			for (int i = 0; i < 256; i++) {
				char c = (char) i;
				if (srch.indexOf(c) == -1) {
					sbuf.append(c);
				}
			}
			srch = sbuf.toString();
		}
		String repl = expandTemplate(replace.convertToString().toString(), false);

		int strLen = getValue().length();
		if (strLen == 0 || srch.length() == 0) {
			return toString();
		}
		int repLen = repl.length();
		StringBuffer sbuf = new StringBuffer(strLen);
		int last = -1;
		for (int i = 0; i < strLen; i++) {
			char cs = getValue().charAt(i);
			int pos = srch.indexOf(cs);
			if (pos == -1) {
				sbuf.append(cs);
				last = -1;
			} else if (repLen > 0) {
				char cr = repl.charAt(Math.min(pos, repLen - 1));
				if (squeeze && cr == last) {
					continue;
				}
				sbuf.append(cr);
				last = cr;
			}
		}
		return sbuf.toString();
	}

	/** rb_str_tr
	 *
	 */
	public IRubyObject tr(IRubyObject search, IRubyObject replace) {
		return newString(tr(search, replace, false)).infectBy(this);
	}

	/** rb_str_tr_bang
	 *
	 */
	public IRubyObject tr_bang(IRubyObject search, IRubyObject replace) {
		String newStr = tr(search, replace, false);
		if (sameAs(newStr)) {
			return getRuntime().getNil();
		}
		setValue(new StringBuffer(newStr));
		return this;
	}

	/** rb_str_tr_s
	 *
	 */
	public IRubyObject tr_s(IRubyObject search, IRubyObject replace) {
		return newString(tr(search, replace, true)).infectBy(this);
	}

	/** rb_str_tr_s_bang
	 *
	 */
	public IRubyObject tr_s_bang(IRubyObject search, IRubyObject replace) {
		String newStr = tr(search, replace, true);
		if (sameAs(newStr)) {
			return getRuntime().getNil();
		}
		setValue(new StringBuffer(newStr));
		return this;
	}

	/** rb_str_each_line
	 *
	 */
	public IRubyObject each_line(IRubyObject[] args) {
		int strLen = getValue().length();
		if (strLen == 0) {
			return this;
		}
		String sep;
		if (checkArgumentCount(args, 0, 1) == 1) {
			sep = RubyRegexp.escapeSpecialChars(stringValue(args[0]).toString());
		} else {
			sep = RubyRegexp.escapeSpecialChars(getRuntime().getGlobalVariables().get("$/").asSymbol());
		}
		if (sep == null) {
			sep = "(?:\\n|\\r\\n?)";
		} else if (sep.length() == 0) {
			sep = "(?:\\n|\\r\\n?){2,}";
		}
		RubyRegexp pat = RubyRegexp.newRegexp(getRuntime(), ".*?" + sep, RubyRegexp.RE_OPTION_MULTILINE, null);
		int start = 0;
        ThreadContext tc = getRuntime().getCurrentContext();
        
		while (pat.search(toString(), start) != -1) {
			RubyMatchData md = (RubyMatchData) tc.getBackref();
			tc.yield(md.group(0));
			start = md.matchEndPosition();
		}
		if (start < strLen) {
			tc.yield(substr(start, strLen - start));
		}
		return this;
	}

	/**
	 * rb_str_each_byte
	 */
	public RubyString each_byte() {
		byte[] lByteValue = toByteArray();
		int lLength = lByteValue.length;
		for (int i = 0; i < lLength; i++) {
			getRuntime().getCurrentContext().yield(getRuntime().newFixnum(Math.abs(lByteValue[i])));
		}
		return this;
	}

	/** rb_str_intern
	 *
	 */
	public RubySymbol intern() {
        String s = toString();
        if (s.equals("")) {
            throw getRuntime().newArgumentError("interning empty string");
        }
        if (s.indexOf('\0') >= 0) {
            throw getRuntime().newArgumentError("symbol string may not contain '\\0'");
        }
        return RubySymbol.newSymbol(getRuntime(), toString());
    }

    public RubySymbol to_sym() {
        return intern();
    }
    
    public RubyInteger sum(IRubyObject[] args) {
        long bitSize = 16;
        if (args.length > 0) {
        	bitSize = ((RubyInteger) args[0].convertType(RubyInteger.class,
                    "Integer", "to_i")).getLongValue();
        }

        long result = 0;
        char[] characters = toString().toCharArray();
        for (int i = 0; i < characters.length; i++) {
            result += characters[i];
        }
        return getRuntime().newFixnum(bitSize == 0 ? result : result % (long) Math.pow(2, bitSize)); 
    }

	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write('"');
		output.dumpString(toString());
	}

	public static RubyString unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
		RubyString result = input.getRuntime().newString(input.unmarshalString());
        input.registerLinkTarget(result);
        return result;
	}

    /**
     * @see org.jruby.util.Pack#unpack
     */
    public RubyArray unpack(IRubyObject obj) {
        return Pack.unpack(this.toString(), stringValue(obj));
    }

    /**
     * Mutator for internal string representation.
     * 
     * @param value The new java.lang.String this RubyString should encapsulate
     */
    public void setValue(CharSequence value) {
        this.chars = value;
        this.value = null;
    }

    /**
     * Mutator for internal string representation.
     * 
     * @param value The new java.lang.String this RubyString should encapsulate
     */
    public void setMutableValue(StringBuffer value) {
        this.chars = this.value = value;
    }

	/**
	 * Accessor for internal string representation.
	 * 
	 * @return The java.lang.String this RubyString encapsulates.
	 */
	public StringBuffer getMutableValue() {
		return value != null ? value : (StringBuffer)(chars = value = new StringBuffer(chars.toString()));
	}
    
    public CharSequence getValue() {
        return chars;
    }
}

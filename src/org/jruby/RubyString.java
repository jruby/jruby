/*
 * RubyString.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby;

import java.util.Locale;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.IndexError;
import org.jruby.exceptions.TypeError;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Asserts;
import org.jruby.util.Pack;
import org.jruby.util.PrintfFormat;
import org.jruby.util.Split;
import org.jruby.internal.runtime.builtin.definitions.StringDefinition;

/**
 *
 * @author  jpetersen
 */
public class RubyString extends RubyObject implements IndexCallable {

	private static final String encoding = "iso8859-1";

	private String value;

	private RubyString(Ruby ruby) {
		this(ruby, ruby.getClasses().getStringClass(), null);
	}

	public RubyString(Ruby ruby, String str) {
		this(ruby, ruby.getClasses().getStringClass(), str);
        Asserts.notNull(str);
	}

	public RubyString(Ruby ruby, RubyClass rubyClass, String str) {
		super(ruby, rubyClass);
		this.value = str;
	}

	public static RubyString nilString(Ruby ruby) {
		return new RubyString(ruby) {
			public boolean isNil() {
				return true;
			}
		};
	}

	public Class getJavaClass() {
		return String.class;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String newValue) {
		value = newValue;
	}

	public String toString() {
		return getValue();
	}

	public static String bytesToString(byte[] bytes) {
		try {
			return new String(bytes, encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			Asserts.notReached("unsupported encoding " + e);
            return null;
		}
	}

	public static byte[] stringToBytes(String string) {
		try {
			return string.getBytes(encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			Asserts.notReached("unsupported encoding " + e);
            return null;
		}
	}

	public byte[] toByteArray() {
		return stringToBytes(value);
	}

	public static RubyClass createStringClass(Ruby ruby) {
        RubyClass stringClass = new StringDefinition(ruby).getType();

		stringClass.defineMethod("swapcase", CallbackFactory.getMethod(RubyString.class, "swapcase"));
		stringClass.defineMethod("upcase!", CallbackFactory.getMethod(RubyString.class, "upcase_bang"));
		stringClass.defineMethod("downcase!", CallbackFactory.getMethod(RubyString.class, "downcase_bang"));
		stringClass.defineMethod("capitalize!", CallbackFactory.getMethod(RubyString.class, "capitalize_bang"));
		stringClass.defineMethod("swapcase!", CallbackFactory.getMethod(RubyString.class, "swapcase_bang"));

		stringClass.defineMethod("hex", CallbackFactory.getMethod(RubyString.class, "hex"));
		stringClass.defineMethod("oct", CallbackFactory.getMethod(RubyString.class, "oct"));
		stringClass.defineMethod("split", CallbackFactory.getOptMethod(RubyString.class, "split"));
		stringClass.defineMethod("reverse", CallbackFactory.getMethod(RubyString.class, "reverse"));
		stringClass.defineMethod("reverse!", CallbackFactory.getMethod(RubyString.class, "reverse_bang"));

		stringClass.defineMethod("include?", CallbackFactory.getMethod(RubyString.class, "include", IRubyObject.class));

		stringClass.defineMethod("scan", CallbackFactory.getMethod(RubyString.class, "scan", IRubyObject.class));

		stringClass.defineMethod("ljust", CallbackFactory.getMethod(RubyString.class, "ljust", IRubyObject.class));
		stringClass.defineMethod("rjust", CallbackFactory.getMethod(RubyString.class, "rjust", IRubyObject.class));
		stringClass.defineMethod("center", CallbackFactory.getMethod(RubyString.class, "center", IRubyObject.class));

		stringClass.defineMethod("sub", CallbackFactory.getOptMethod(RubyString.class, "sub"));
		stringClass.defineMethod("gsub", CallbackFactory.getOptMethod(RubyString.class, "gsub"));
		stringClass.defineMethod("chop", CallbackFactory.getMethod(RubyString.class, "chop"));
		stringClass.defineMethod("chomp", CallbackFactory.getOptMethod(RubyString.class, "chomp"));
		stringClass.defineMethod("strip", CallbackFactory.getMethod(RubyString.class, "strip"));

		stringClass.defineMethod("sub!", CallbackFactory.getOptMethod(RubyString.class, "sub_bang"));
		stringClass.defineMethod("gsub!", CallbackFactory.getOptMethod(RubyString.class, "gsub_bang"));
		stringClass.defineMethod("chop!", CallbackFactory.getMethod(RubyString.class, "chop_bang"));
		stringClass.defineMethod("chomp!", CallbackFactory.getOptMethod(RubyString.class, "chomp_bang"));
		stringClass.defineMethod("strip!", CallbackFactory.getMethod(RubyString.class, "strip_bang"));

		stringClass.defineMethod("tr", CallbackFactory.getOptMethod(RubyString.class, "tr"));
		stringClass.defineMethod("tr_s", CallbackFactory.getOptMethod(RubyString.class, "tr_s"));
		stringClass.defineMethod("delete", CallbackFactory.getOptMethod(RubyString.class, "delete"));
		stringClass.defineMethod("squeeze", CallbackFactory.getOptMethod(RubyString.class, "squeeze"));
		stringClass.defineMethod("count", CallbackFactory.getOptMethod(RubyString.class, "count"));

		stringClass.defineMethod("tr!", CallbackFactory.getOptMethod(RubyString.class, "tr_bang"));
		stringClass.defineMethod("tr_s!", CallbackFactory.getOptMethod(RubyString.class, "tr_s_bang"));
		stringClass.defineMethod("delete!", CallbackFactory.getOptMethod(RubyString.class, "delete_bang"));
		stringClass.defineMethod("squeeze!", CallbackFactory.getOptMethod(RubyString.class, "squeeze_bang"));

		stringClass.defineMethod("each_line", CallbackFactory.getOptMethod(RubyString.class, "each_line"));
		stringClass.defineMethod("each", CallbackFactory.getOptMethod(RubyString.class, "each_line"));
		stringClass.defineMethod("each_byte", CallbackFactory.getMethod(RubyString.class, "each_byte"));

		stringClass.defineMethod("slice!", CallbackFactory.getOptMethod(RubyString.class, "slice_bang"));

		stringClass.defineMethod("unpack", CallbackFactory.getMethod(RubyString.class, "unpack", RubyString.class));
		return stringClass;
	}

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
        case StringDefinition.RBCLONE:
            return rbClone();
        case StringDefinition.DUP:
            return dup();
        case StringDefinition.OP_CMP:
            return op_cmp(args[0]);
        case StringDefinition.EQUAL:
            return equal(args[0]);
		case StringDefinition.HASH:
			return hash();
        case StringDefinition.OP_PLUS:
            return op_plus(args[0]);
        case StringDefinition.OP_MUL:
            return op_mul(args[0]);
        case StringDefinition.FORMAT:
            return format(args[0]);
        case StringDefinition.LENGTH:
            return length();
        case StringDefinition.EMPTY:
            return empty();
        case StringDefinition.MATCH:
            return match(args[0]);
        case StringDefinition.MATCH2:
            return match2();
        case StringDefinition.SUCC:
            return succ();
        case StringDefinition.SUCC_BANG:
            return succ_bang();
        case StringDefinition.UPTO:
            return upto(args[0]);
        case StringDefinition.REPLACE:
            return replace(args[0]);
        case StringDefinition.TO_I:
            return to_i();
        case StringDefinition.TO_F:
            return to_f();
        case StringDefinition.TO_S:
            return to_s();
        case StringDefinition.INSPECT:
            return inspect();
        case StringDefinition.CONCAT:
            return concat(args[0]);
        case StringDefinition.INTERN:
            return intern();
        case StringDefinition.SUM:
            return sum(args);
        case StringDefinition.DUMP:
            return dump();
        case StringDefinition.UPCASE:
            return upcase();
        case StringDefinition.DOWNCASE:
            return downcase();
        case StringDefinition.CAPITALIZE:
            return capitalize();
        case StringDefinition.INDEX:
            return index(args);
        case StringDefinition.RINDEX:
            return rindex(args);
        case StringDefinition.AREF:
            return aref(args);
        case StringDefinition.ASET:
            return aset(args);
        }
        return super.callIndexed(index, args);
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
        return RubyFixnum.newFixnum(runtime, getValue().hashCode());
    }

	/** rb_obj_as_string
	 *
	 */
	public static RubyString objAsString(IRubyObject obj) {
		if (obj instanceof RubyString) {
			return (RubyString) obj;
		}
		return (RubyString) obj.callMethod("to_s");
	}

	/** rb_str_cmp
	 *
	 */
	public int cmp(RubyString other) {
		/* use Java implementatiom */
		return getValue().compareTo(other.getValue());
	}

	/** rb_to_id
	 *
	 */
	public String asSymbol() {
		return getValue();
	}

	/** Create a new String which uses the same Ruby runtime and the same
	 *  class like this String.
	 *
	 *  This method should be used to satisfy RCR #38.
	 *
	 */
	public RubyString newString(String s) {
		return new RubyString(getRuntime(), getType(), s);
	}

	// Methods of the String class (rb_str_*):

	/** rb_str_new2
	 *
	 */
	public static RubyString newString(Ruby ruby, String str) {
		return new RubyString(ruby, str);
	}

	/** rb_str_new
	 *
	 */
	public static RubyString newString(Ruby ruby, String str, int len) {
		return new RubyString(ruby, str.substring(0, len));
	}

	public static RubyString newString(Ruby ruby, byte[] bytes) {
		return newString(ruby, bytesToString(bytes));
	}


	/** rb_str_dup
	 *
	 */
	public IRubyObject dup() {
		RubyString newStr = newString(getValue());
		newStr.infectBy(this);
		return newStr;
	}

	/** rb_str_clone
	 *
	 */
	public IRubyObject rbClone() {
		IRubyObject newObject = dup();

		newObject.setupClone(this);

		return newObject;
	}

	/** rb_str_cat
	 *
	 */
	public RubyString cat(String str) {
		value = value + str;
		return this;
	}

	/** rb_str_to_s
	 *
	 */
	public RubyString to_s() {
		return this;
	}

	/** rb_str_replace_m
	 *
	 */
	public RubyString replace(IRubyObject other) {
		RubyString str = stringValue(other);
		if (this == other || getValue().equals(str.getValue())) {
			return this;
		}
		setValue(str.getValue());
		infectBy(str);
		return this;
	}

	/** rb_str_reverse
	 *
	 */
	public RubyString reverse() {
		StringBuffer sb = new StringBuffer(getValue());
        sb.reverse();
		return newString(sb.toString());
	}

	/** rb_str_reverse_bang
	 *
	 */
	public RubyString reverse_bang() {
		StringBuffer sb = new StringBuffer(getValue());
        sb.reverse();
		setValue(sb.toString());
		return this;
	}

	/** rb_str_s_new
	 *
	 */
	public static RubyString newInstance(IRubyObject recv, IRubyObject[] args) {
		RubyString newString = newString(recv.getRuntime(), "");
		newString.setMetaClass((RubyClass) recv);

		newString.callInit(args);

		return newString;
	}

	/** rb_str_cmp_m
	 *
	 */
	public RubyFixnum op_cmp(IRubyObject other) {
		return RubyFixnum.newFixnum(getRuntime(), cmp(stringValue(other)));
	}

	/** rb_str_equal
	 *
	 */
	public RubyBoolean equal(IRubyObject other) {
		if (other == this) {
			return getRuntime().getTrue();
		} else if (!(other instanceof RubyString)) {
			return getRuntime().getFalse();
		}
		/* use Java implementation */
		return getValue().equals(((RubyString) other).getValue()) ? getRuntime().getTrue() : getRuntime().getFalse();
	}

	/** rb_str_match
	 *
	 */
	public IRubyObject match(IRubyObject other) {
		if (other instanceof RubyRegexp) {
			return ((RubyRegexp) other).match(this);
		} else if (other instanceof RubyString) {
			return RubyRegexp.newRegexp((RubyString) other, 0).match(this);
		}
		return other.callMethod("=~", this);
	}

	/** rb_str_match2
	 *
	 */
	public IRubyObject match2() {
		return RubyRegexp.newRegexp(this, 0).match2();
	}

	/** rb_str_capitalize
	 *
	 */
	public RubyString capitalize() {
		final int length = getValue().length();

		StringBuffer sb = new StringBuffer(length);
		if (length > 0) {
			sb.append(Character.toUpperCase(getValue().charAt(0)));
		}
		if (length > 1) {
			sb.append(getValue().toLowerCase().substring(1));
		}

		return newString(sb.toString());
	}

	/** rb_str_capitalize_bang
	 *
	 */
	public RubyString capitalize_bang() {
		final int length = getValue().length();

		StringBuffer sb = new StringBuffer(length);
		if (length > 0) {
			sb.append(Character.toUpperCase(getValue().charAt(0)));
		}
		if (length > 1) {
			sb.append(getValue().toLowerCase().substring(1));
		}

		setValue(sb.toString());

		return this;
	}

	/** rb_str_upcase
	 *
	 */
	public RubyString upcase() {
		return newString(getValue().toUpperCase());
	}

	/** rb_str_upcase_bang
	 *
	 */
	public RubyString upcase_bang() {
		setValue(getValue().toUpperCase());
		return this;
	}

	/** rb_str_downcase
	 *
	 */
	public RubyString downcase() {
		return newString(getValue().toLowerCase());
	}

	/** rb_str_downcase_bang
	 *
	 */
	public RubyString downcase_bang() {
		setValue(getValue().toLowerCase());
		return this;
	}

	/** rb_str_swapcase
	 *
	 */
	public RubyString swapcase() {
		RubyString newString = newString(getValue());
		return newString.swapcase_bang();
	}

	/** rb_str_swapcase_bang
	 *
	 */
	public RubyString swapcase_bang() {
		char[] chars = getValue().toCharArray();
		StringBuffer sb = new StringBuffer(chars.length);

		for (int i = 0; i < chars.length; i++) {
			if (!Character.isLetter(chars[i])) {
				sb.append(chars[i]);
			} else if (Character.isLowerCase(chars[i])) {
				sb.append(Character.toUpperCase(chars[i]));
			} else {
				sb.append(Character.toLowerCase(chars[i]));
			}
		}
		setValue(getValue().toLowerCase());

		return this;
	}

	/** rb_str_dump
	 *
	 */
	public RubyString dump() {
		return inspect(true);
	}

	/** rb_str_inspect
	 *
	 */
	public RubyString inspect() {
		return inspect(false);
	}

	private RubyString inspect(boolean dump) {
		final int length = getValue().length();

		StringBuffer sb = new StringBuffer(length + 2 + (length / 100));

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

		return newString(getRuntime(), sb.toString());
	}

	/** rb_str_plus
	 *
	 */
	public RubyString op_plus(IRubyObject other) {
		RubyString str = stringValue(other);
		RubyString newString = newString(getValue() + str.getValue());
		newString.infectBy(str);
		return newString;
	}

	/** rb_str_mul
	 *
	 */
	public RubyString op_mul(IRubyObject other) {
		RubyInteger otherInteger =
                (RubyInteger) other.convertType(RubyInteger.class, "Integer", "to_int");
        long len = otherInteger.getLongValue();

		if (len < 0) {
			throw new ArgumentError(getRuntime(), "negative argument");
		}

		if (len > 0 && Long.MAX_VALUE / len < getValue().length()) {
			throw new ArgumentError(getRuntime(), "argument too big");
		}
		StringBuffer sb = new StringBuffer((int) (getValue().length() * len));

		for (int i = 0; i < len; i++) {
			sb.append(getValue());
		}

		RubyString newString = newString(sb.toString());
		newString.setTaint(isTaint());
		return newString;
	}

	/** rb_str_length
	 *
	 */
	public RubyFixnum length() {
		return RubyFixnum.newFixnum(getRuntime(), getValue().length());
	}

	/** rb_str_empty
	 *
	 */
	public RubyBoolean empty() {
		return RubyBoolean.newBoolean(getRuntime(), getValue().length() == 0);
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
			char c = (char) ((RubyFixnum) other).getLongValue();
			return cat("" + c);
		}
		return append(other);
	}

	/* rb_str_to_str */
	public static RubyString stringValue(IRubyObject anObject) {
		if (anObject instanceof RubyString) {
			return (RubyString) anObject;
		} else {
			return (RubyString) anObject.convertType(RubyString.class, "String", "to_str");
		}
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
		if (args.length == 1 && getRuntime().isBlockGiven()) {
			iter = true;
		} else if (args.length == 2) {
			repl = args[1];
		} else {
			throw new ArgumentError(getRuntime(), "wrong number of arguments");
		}
		RubyRegexp pat = RubyRegexp.regexpValue(args[0]);

		if (pat.search(this, 0) >= 0) {
			RubyMatchData match = (RubyMatchData) getRuntime().getBackref();
			RubyString newStr = match.pre_match();
			newStr.append((iter ? getRuntime().yield(match.group(0)) : pat.regsub(repl, match)));
			newStr.append(match.post_match());
			if (bang) {
				replace(newStr);
				return this;
			}
			return newStr;
		}
		if (bang) {
			return getRuntime().getNil();
		}
		return this;
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
		if (args.length == 1 && getRuntime().isBlockGiven()) {
			iter = true;
		} else if (args.length == 2) {
			repl = args[1];
		} else {
			throw new ArgumentError(getRuntime(), "wrong number of arguments");
		}
		boolean taint = repl.isTaint();
		RubyRegexp pat = RubyRegexp.regexpValue(args[0]);

		int beg = pat.search(this, 0);
		if (beg < 0) {
			return bang ? getRuntime().getNil() : dup();
		}
		StringBuffer sbuf = new StringBuffer();
		String str = getValue();
		IRubyObject newStr;
		int offset = 0;
		while (beg >= 0) {
			match = (RubyMatchData) getRuntime().getBackref();
			sbuf.append(str.substring(offset, beg));
			newStr = iter ? getRuntime().yield(match.group(0)) : pat.regsub(repl, match);
			taint |= newStr.isTaint();
			sbuf.append(((RubyString) newStr).getValue());
			offset = match.matchEndPosition();
			beg = pat.search(this, (offset == beg ? beg + 1 : offset));
		}
		sbuf.append(str.substring(offset, str.length()));
		if (bang) {
			setTaint(isTaint() || taint);
			setValue(sbuf.toString());
			return this;
		}
		RubyString result = newString(sbuf.toString());
		result.setTaint(taint);
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
		if (reverse)
			pos = getValue().length();
		if (argCount(args, 1, 2) == 2) {
			pos = RubyNumeric.fix2int(args[1]);
		}
		if (pos < 0) {
			pos += getValue().length();
			if (pos < 0) {
				return getRuntime().getNil();
			}
		}
		if (args[0] instanceof RubyRegexp) {
			pos = ((RubyRegexp) args[0]).search(this, pos);
			// RubyRegexp doesn't (yet?) support reverse searches, so we
			// find all matches and use the last one--very inefficient.
			// XXX - find a better way
			int dummy = pos;
			while (reverse && dummy > -1) {
				pos = dummy;
				dummy = ((RubyRegexp) args[0]).search(this, pos + 1);
			}
		} else if (args[0] instanceof RubyString) {
			String sub = ((RubyString) args[0]).getValue();
			pos = reverse ? getValue().lastIndexOf(sub, pos) : getValue().indexOf(sub, pos);
		} else if (args[0] instanceof RubyFixnum) {
			char c = (char) ((RubyFixnum) args[0]).getLongValue();
			pos = reverse ? getValue().lastIndexOf(c, pos) : getValue().indexOf(c, pos);
		} else {
			throw new ArgumentError(getRuntime(), "wrong type of argument");
		}

		if (pos == -1) {
			return getRuntime().getNil();
		}
		return RubyFixnum.newFixnum(getRuntime(), pos);
	}

	/* rb_str_substr */
	private IRubyObject substr(int beg, int len) {
		int strLen = getValue().length();
		if (len < 0 || beg > strLen) {
			return getRuntime().getNil();
		}
		if (beg < 0) {
			beg += strLen;
			if (beg < 0) {
				return getRuntime().getNil();
			}
		}
		int end = Math.min(strLen, beg + len);
		RubyString newStr = newString(getValue().substring(beg, end));
		newStr.infectBy(this);
		return newStr;
	}

	/* rb_str_replace */
	private IRubyObject replace(int beg, int len, RubyString repl) {
		int strLen = getValue().length();
		if (beg + len >= strLen) {
			len = strLen - beg;
		}
		setValue(getValue().substring(0, beg) + repl.getValue() + getValue().substring(beg + len));
		infectBy(repl);
		return this;
	}

	/** rb_str_aref, rb_str_aref_m
	 *
	 */
	public IRubyObject aref(IRubyObject[] args) {
		if (argCount(args, 1, 2) == 2) {
			int beg = RubyNumeric.fix2int(args[0]);
			int len = RubyNumeric.fix2int(args[1]);
			return substr(beg, len);
		}
		if (args[0] instanceof RubyFixnum) {
			int idx = RubyNumeric.fix2int(args[0]);
			if (idx < 0) {
				idx += getValue().length();
			}
			if (idx < 0 || idx >= getValue().length()) {
				return getRuntime().getNil();
			}
			return RubyFixnum.newFixnum(getRuntime(), getValue().charAt(idx));
		}
		if (args[0] instanceof RubyRegexp) {
			if (RubyRegexp.regexpValue(args[0]).search(this, 0) >= 0) {
				return RubyRegexp.last_match(getRuntime().getBackref());
			}
			return getRuntime().getNil();
		}
		if (args[0] instanceof RubyString) {
			if (getValue().indexOf(stringValue(args[0]).getValue()) != -1) {
				return args[0];
			}
			return getRuntime().getNil();
		}
		if (args[0] instanceof RubyRange) {
			long[] begLen = ((RubyRange) args[0]).getBeginLength(getValue().length(), true, false);
			if (begLen == null) {
				return getRuntime().getNil();
			}
			return substr((int) begLen[0], (int) begLen[1]);
		}
		int idx = (int) RubyNumeric.num2long(args[0]);
		if (idx < 0) {
			idx += getValue().length();
		}
		if (idx < 0 || idx >= getValue().length()) {
			return getRuntime().getNil();
		}
		return RubyFixnum.newFixnum(getRuntime(), getValue().charAt(idx));
	}

	/** rb_str_aset, rb_str_aset_m
	 *
	 */
	public IRubyObject aset(IRubyObject[] args) {
		int strLen = getValue().length();
		if (argCount(args, 2, 3) == 3) {
			RubyString repl = stringValue(args[2]);
			int beg = RubyNumeric.fix2int(args[0]);
			int len = RubyNumeric.fix2int(args[1]);
			if (len < 0) {
				throw new IndexError(getRuntime(), "negative length");
			}
			if (beg < 0) {
				beg += strLen;
			}
			if (beg < 0 || beg >= strLen) {
				throw new IndexError(getRuntime(), "string index out of bounds");
			}
			if (beg + len > strLen) {
				len = strLen - beg;
			}
			replace(beg, len, repl);
			return repl;
		}
		if (args[0] instanceof RubyFixnum) { // RubyNumeric?
			int idx = RubyNumeric.fix2int(args[0]); // num2int?
			if (idx < 0) {
				idx += getValue().length();
			}
			if (idx < 0 || idx >= getValue().length()) {
				throw new IndexError(getRuntime(), "string index out of bounds");
			}
			if (args[1] instanceof RubyFixnum) {
				char c = (char) RubyNumeric.fix2int(args[1]);
				setValue(getValue().substring(0, idx) + c + getValue().substring(idx + 1));
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
			int beg = getValue().indexOf(orig.getValue());
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
		throw new TypeError(getRuntime(), "wrong argument type");
	}

	/** rb_str_slice_bang
	 *
	 */
	public IRubyObject slice_bang(IRubyObject[] args) {
		int argc = argCount(args, 1, 2);
		IRubyObject[] newArgs = new IRubyObject[argc + 1];
		newArgs[0] = args[0];
		if (argc > 1) {
			newArgs[1] = args[1];
		}
		newArgs[argc] = newString("");
		IRubyObject result = aref(args);
		aset(newArgs);
		return result;
	}

	/** rb_str_format
	 *
	 */
	public IRubyObject format(IRubyObject arg) {
		if (arg instanceof RubyArray) {
			Object[] args = new Object[((RubyArray) arg).getLength()];
			for (int i = 0; i < args.length; i++) {
				args[i] = JavaUtil.convertRubyToJava(((RubyArray) arg).entry(i));
			}
			return RubyString.newString(runtime, new PrintfFormat(Locale.US, getValue()).sprintf(args));
		} else {
			return RubyString.newString(runtime, new PrintfFormat(Locale.US, getValue()).sprintf(JavaUtil.convertRubyToJava(arg)));
		}
	}

	/** rb_str_succ
	 *
	 */
	public IRubyObject succ() {
		return succ(false);
	}

	/** rb_str_succ_bang
	 *
	 */
	public IRubyObject succ_bang() {
		return succ(true);
	}

	private RubyString succ(boolean bang) {
		if (getValue().length() == 0) {
			return bang ? this : (RubyString) dup();
		}
		StringBuffer sbuf = new StringBuffer(getValue());
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
				} else {
					pos = i;
					n = isDigit(c) ? '0' : (isLower(c) ? 'a' : 'A');
					sbuf.setCharAt(i, n);
				}
			}
		}
		if (!alnumSeen) {
			for (int i = sbuf.length() - 1; i >= 0; i--) {
				c = sbuf.charAt(i);
				if (c < 0xff) {
					sbuf.setCharAt(i, (char) (c + 1));
					pos = -1;
					break;
				} else {
					pos = i;
					n = '\u0001';
					sbuf.setCharAt(i, '\u0000');
				}
			}
		}
		if (pos > -1) {
			sbuf.insert(pos, n);
		}

		if (bang) {
			setValue(sbuf.toString());
			return this;
		} else {
			RubyString newStr = (RubyString) dup();
			newStr.setValue(sbuf.toString());
			return newStr;
		}
	}

	/** rb_str_upto_m
	 *
	 */
	public IRubyObject upto(IRubyObject str) {
		return upto(str, false);
	}

	/* rb_str_upto */
	public IRubyObject upto(IRubyObject str, boolean excl) {
		RubyString current = this;
		RubyString end = stringValue(str);
		while (current.cmp(end) <= 0) {
			getRuntime().yield(current);
			if (current.cmp(end) == 0) {
				break;
			}
			current = current.succ(false);
			if (excl && current.cmp(end) == 0) {
				break;
			}
			if (current.getValue().length() > end.getValue().length()) {
				break;
			}
		}
		return this;
	}

	/** rb_str_include
	 *
	 */
	public RubyBoolean include(IRubyObject obj) {
		if (obj instanceof RubyFixnum) {
			char c = (char) RubyNumeric.fix2int(obj);
			return RubyBoolean.newBoolean(getRuntime(), getValue().indexOf(c) != -1);
		}
		String str = stringValue(obj).getValue();
		return RubyBoolean.newBoolean(getRuntime(), getValue().indexOf(str) != -1);
	}

	/** rb_str_to_i
	 *
	 */
	public IRubyObject to_i() {
		return RubyNumeric.str2inum(getRuntime(), this, 10);
	}

	/** rb_str_oct
	 *
	 */
	public IRubyObject oct() {
		int base = 8;
		String str = getValue().trim();
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
        return new Split(runtime, this.getValue(), args).results();
	}

	/** rb_str_scan
	 *
	 */
	public IRubyObject scan(IRubyObject arg) {
		RubyRegexp pat = RubyRegexp.regexpValue(arg);
		int start = 0;
		if (!getRuntime().isBlockGiven()) {
			RubyArray ary = RubyArray.newArray(getRuntime());
			while (pat.search(this, start) != -1) {
				RubyMatchData md = (RubyMatchData) getRuntime().getBackref();
				if (md.getSize() == 1) {
					ary.append(md.group(0));
				} else {
					ary.append(md.subseq(1, md.getSize()));
				}
				if (md.matchEndPosition() == md.matchStartPosition()) {
					start++;
				} else {
					start = md.matchEndPosition();
				}
			}
			return ary;
		}

		while (pat.search(this, start) != -1) {
			RubyMatchData md = (RubyMatchData) getRuntime().getBackref();
			if (md.getSize() == 1) {
				getRuntime().yield(md.group(0));
			} else {
				getRuntime().yield(md.subseq(1, md.getSize()));
			}
			if (md.matchEndPosition() == md.matchStartPosition()) {
				start++;
			} else {
				start = md.matchEndPosition();
			}
		}
		return this;
	}

	/** rb_str_ljust
	 *
	 */
	public IRubyObject ljust(IRubyObject arg) {
		int len = RubyNumeric.fix2int(arg);
		if (len <= getValue().length()) {
			return dup();
		}
		Object[] args = new Object[] { getValue(), new Integer(-len)};
		return newString(new PrintfFormat("%*2$s").sprintf(args));
	}

	/** rb_str_rjust
	 *
	 */
	public IRubyObject rjust(IRubyObject arg) {
		int len = RubyNumeric.fix2int(arg);
		if (len <= getValue().length()) {
			return dup();
		}
		Object[] args = new Object[] { getValue(), new Integer(len)};
		return newString(new PrintfFormat("%*2$s").sprintf(args));
	}

	/** rb_str_center
	 *
	 */
	public IRubyObject center(IRubyObject arg) {
		int len = RubyNumeric.fix2int(arg);
		int strLen = getValue().length();
		if (len <= strLen) {
			return dup();
		}
		StringBuffer sbuf = new StringBuffer(len);
		int lead = (len - strLen) / 2;
		int pos = 0;
		while (pos < len) {
			if (pos == lead) {
				sbuf.append(getValue());
				pos += strLen;
			} else {
				sbuf.append(' ');
				pos++;
			}
		}
		return newString(sbuf.toString());
	}

	private String getChop() {
		if (getValue().length() == 0) {
			return null;
		}
		int end = getValue().length() - 1;
		if (getValue().charAt(end) == '\n' && end > 0) {
			if (getValue().charAt(end - 1) == '\r') {
				end--;
			}
		}
		return getValue().substring(0, end);
	}

	/** rb_str_chop
	 *
	 */
	public IRubyObject chop() {
		String newStr = getChop();
		if (newStr == null) {
			return dup();
		}
		return newString(newStr);
	}

	/** rb_str_chop_bang
	 *
	 */
	public IRubyObject chop_bang() {
		String newStr = getChop();
		if (newStr == null) {
			return getRuntime().getNil();
		}
		setValue(newStr);
		return this;
	}

	protected String getChomp(IRubyObject[] args) {
		if (getValue().length() == 0) {
			return null;
		}
		String sep = null;
		if (argCount(args, 0, 1) == 1) {
			sep = stringValue(args[0]).getValue();
		}
		int end = -1;
		if (sep != null && getValue().endsWith(sep)) {
			return getValue().substring(0, getValue().lastIndexOf(sep));
		}
		// $/ is coming up nil, so check for 'standard' line separators
		if (getValue().endsWith("\r\n")) {
			end = getValue().length() - 2;
		} else if (getValue().endsWith("\n") || getValue().endsWith("\r")) {
			end = getValue().length() - 1;
		}
		return end == -1 ? null : getValue().substring(0, end);
	}

	/** rb_str_chomp
	 *
	 */
	public IRubyObject chomp(IRubyObject[] args) {
		String newStr = getChomp(args);
		if (newStr == null) {
			return dup();
		}
		return newString(newStr);
	}

	/** rb_str_chomp_bang
	 *
	 */
	public IRubyObject chomp_bang(IRubyObject[] args) {
		String newStr = getChomp(args);
		if (newStr == null) {
			return getRuntime().getNil();
		}
		setValue(newStr);
		return this;
	}

	/** rb_str_strip
	 *
	 */
	public IRubyObject strip() {
		if (getValue().length() == 0) {
			return dup();
		}
		return newString(getValue().trim());
	}

	/** rb_str_strip_bang
	 *
	 */
	public IRubyObject strip_bang() {
		if (getValue().length() == 0) {
			return getRuntime().getNil();
		}
		String newStr = getValue().trim();
		if (newStr.equals(getValue())) {
			return getRuntime().getNil();
		}
		setValue(newStr);
		return this;
	}

	private String expandTemplate(String spec, boolean invertOK) {
		int len = spec.length();
		if (len <= 1) {
			return spec;
		}
		StringBuffer sbuf = new StringBuffer();
		int pos = (invertOK && spec.startsWith("^")) ? 1 : 0;
		char c1, c2;
		while (pos < len) {
			c1 = spec.charAt(pos);
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
			boolean invert = (specs[i].length() > 1 && specs[i].startsWith("^"));
			for (int j = 0; j < 256; j++) {
				if (template.indexOf(j) != -1) {
					table[j] += (invert ? -1 : 1);
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
		int argc = argCount(args, 1, -1);
		String[] specs = new String[argc];
		for (int i = 0; i < argc; i++) {
			specs[i] = stringValue(args[i]).getValue();
		}
		String table = setupTable(specs);

		int count = 0;
		for (int j = 0; j < getValue().length(); j++) {
			if (table.indexOf(getValue().charAt(j)) != -1) {
				count++;
			}
		}
		return RubyFixnum.newFixnum(getRuntime(), count);
	}

	private String getDelete(IRubyObject[] args) {
		int argc = argCount(args, 1, -1);
		String[] specs = new String[argc];
		for (int i = 0; i < argc; i++) {
			specs[i] = stringValue(args[i]).getValue();
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
		RubyString newStr = newString(getDelete(args));
		newStr.infectBy(this);
		return newStr;
	}

	/** rb_str_delete_bang
	 *
	 */
	public IRubyObject delete_bang(IRubyObject[] args) {
		String newStr = getDelete(args);
		if (newStr.equals(getValue())) {
			return getRuntime().getNil();
		}
		setValue(newStr);
		return this;
	}

	private String getSqueeze(IRubyObject[] args) {
		int argc = args.length;
		String[] specs = null;
		if (argc > 0) {
			specs = new String[argc];
			for (int i = 0; i < argc; i++) {
				specs[i] = stringValue(args[i]).getValue();
			}
		}
		String table = specs == null ? null : setupTable(specs);

		int strLen = getValue().length();
		if (strLen <= 1) {
			return getValue();
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
		return sbuf.toString();
	}

	/** rb_str_squeeze
	 *
	 */
	public IRubyObject squeeze(IRubyObject[] args) {
		RubyString newStr = newString(getSqueeze(args));
		newStr.infectBy(this);
		return newStr;
	}

	/** rb_str_squeeze_bang
	 *
	 */
	public IRubyObject squeeze_bang(IRubyObject[] args) {
		String newStr = getSqueeze(args);
		if (newStr.equals(getValue())) {
			return getRuntime().getNil();
		}
		setValue(newStr);
		return this;
	}

	private String tr(IRubyObject[] args, boolean squeeze) {
		if (args.length != 2) {
			throw new ArgumentError(getRuntime(), "wrong number of arguments");
		}
		String srchSpec = stringValue(args[0]).getValue();
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
		String repl = expandTemplate(stringValue(args[1]).getValue(), false);

		int strLen = getValue().length();
		if (strLen == 0 || srch.length() == 0) {
			return getValue();
		}
		int repLen = repl.length();
		StringBuffer sbuf = new StringBuffer(strLen);
		char cs, cr;
		int last = -1;
		int pos;
		for (int i = 0; i < strLen; i++) {
			cs = getValue().charAt(i);
			pos = srch.indexOf(cs);
			if (pos == -1) {
				sbuf.append(cs);
				last = -1;
			} else if (repLen > 0) {
				cr = repl.charAt(Math.min(pos, repLen - 1));
				if (squeeze && (int) cr == last) {
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
	public IRubyObject tr(IRubyObject[] args) {
		RubyString newStr = newString(tr(args, false));
		newStr.infectBy(this);
		return newStr;
	}

	/** rb_str_tr_bang
	 *
	 */
	public IRubyObject tr_bang(IRubyObject[] args) {
		String newStr = tr(args, false);
		if (newStr.equals(getValue())) {
			return getRuntime().getNil();
		}
		setValue(newStr);
		return this;
	}

	/** rb_str_tr_s
	 *
	 */
	public IRubyObject tr_s(IRubyObject[] args) {
		RubyString newStr = newString(tr(args, true));
		newStr.infectBy(this);
		return newStr;
	}

	/** rb_str_tr_s_bang
	 *
	 */
	public IRubyObject tr_s_bang(IRubyObject[] args) {
		String newStr = tr(args, true);
		if (newStr.equals(getValue())) {
			return getRuntime().getNil();
		}
		setValue(newStr);
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
		String sep = null;
		if (argCount(args, 0, 1) == 1) {
			sep = RubyRegexp.quote(stringValue(args[0]).getValue());
		}
		if (sep == null) {
			sep = "(?:\\n|\\r\\n?)";
		} else if (sep.length() == 0) {
			sep = "(?:\\n|\\r\\n?){2,}";
		}
		RubyRegexp pat = RubyRegexp.newRegexp(getRuntime(), ".*?" + sep, RubyRegexp.RE_OPTION_MULTILINE);
		int start = 0;
		while (pat.search(this, start) != -1) {
			RubyMatchData md = (RubyMatchData) getRuntime().getBackref();
			getRuntime().yield(md.group(0));
			start = md.matchEndPosition();
		}
		if (start < strLen) {
			getRuntime().yield(substr(start, strLen - start));
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
			runtime.yield(RubyFixnum.newFixnum(runtime, lByteValue[i]));
		}
		return this;
	}

	/** rb_str_intern
	 *
	 */
	public RubySymbol intern() {
		return RubySymbol.newSymbol(getRuntime(), getValue());
	}

    public RubyInteger sum(IRubyObject[] args) {
        long bitSize = 16;
        if (args.length > 0) {
            RubyInteger sizeArgument =
                (RubyInteger) args[0].convertType(RubyInteger.class,
                                                  "Integer",
                                                  "to_int");
            bitSize = sizeArgument.getLongValue();
        }

        int result = 0;
        char[] characters = value.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            result += characters[i];
        }
        return RubyFixnum.newFixnum(getRuntime(), result % (2 * bitSize - 1));
    }

	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write('"');
		output.dumpString(getValue());
	}

	public static RubyString unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
		RubyString result = RubyString.newString(input.getRuntime(), input.unmarshalString());
        input.registerLinkTarget(result);
        return result;
	}

    /**
     * @see org.jruby.util.Pack#unpack
     */
	public RubyArray unpack(RubyString iFmt) {
        return Pack.unpack(this.value, iFmt);
    }
}

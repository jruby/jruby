package org.jruby;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.StringScanner;

/**
 * @author kscott
 *
 */
public class RubyStringScanner extends RubyObject {
	
	private StringScanner scanner;
	
	public static RubyClass createScannerClass(final IRuby runtime) {
		RubyClass scannerClass = runtime.defineClass("StringScanner",runtime.getObject());
		CallbackFactory callbackFactory = runtime.callbackFactory(RubyStringScanner.class);
		
		scannerClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
		scannerClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
		scannerClass.defineMethod("<<", callbackFactory.getMethod("concat", IRubyObject.class));
		scannerClass.defineMethod("concat", callbackFactory.getMethod("concat", IRubyObject.class));
		scannerClass.defineMethod("[]", callbackFactory.getMethod("group", RubyFixnum.class));
		scannerClass.defineMethod("beginning_of_line?", callbackFactory.getMethod("bol_p"));
		scannerClass.defineMethod("bol?", callbackFactory.getMethod("bol_p"));
		scannerClass.defineMethod("check", callbackFactory.getMethod("check", RubyRegexp.class));
		scannerClass.defineMethod("check_until", callbackFactory.getMethod("check_until", RubyRegexp.class));
		scannerClass.defineMethod("clear", callbackFactory.getMethod("terminate"));
		scannerClass.defineMethod("empty?", callbackFactory.getMethod("eos_p"));
		scannerClass.defineMethod("eos?", callbackFactory.getMethod("eos_p"));
		scannerClass.defineMethod("exist?", callbackFactory.getMethod("exist_p", RubyRegexp.class));
		scannerClass.defineMethod("get_byte", callbackFactory.getMethod("getch"));
		scannerClass.defineMethod("getbyte", callbackFactory.getMethod("getch"));
		scannerClass.defineMethod("getch", callbackFactory.getMethod("getch"));
		scannerClass.defineMethod("inspect", callbackFactory.getMethod("inspect"));
		scannerClass.defineMethod("match?", callbackFactory.getMethod("match_p", RubyRegexp.class));
		scannerClass.defineMethod("matched", callbackFactory.getMethod("matched"));
		scannerClass.defineMethod("matched?", callbackFactory.getMethod("matched_p"));
		scannerClass.defineMethod("matched_size", callbackFactory.getMethod("matched_size"));
		scannerClass.defineMethod("matchedsize", callbackFactory.getMethod("matched_size"));
		scannerClass.defineMethod("peek", callbackFactory.getMethod("peek", RubyFixnum.class));
		scannerClass.defineMethod("peep", callbackFactory.getMethod("peek", RubyFixnum.class));
		scannerClass.defineMethod("pointer", callbackFactory.getMethod("pos"));
		scannerClass.defineMethod("pointer=", callbackFactory.getMethod("set_pos", RubyFixnum.class));
		scannerClass.defineMethod("pos=", callbackFactory.getMethod("set_pos", RubyFixnum.class));
		scannerClass.defineMethod("pos", callbackFactory.getMethod("pos"));
		scannerClass.defineMethod("post_match", callbackFactory.getMethod("post_match"));
		scannerClass.defineMethod("pre_match", callbackFactory.getMethod("pre_match"));
		scannerClass.defineMethod("reset", callbackFactory.getMethod("reset"));
		scannerClass.defineMethod("rest", callbackFactory.getMethod("rest"));
		scannerClass.defineMethod("rest?", callbackFactory.getMethod("rest_p"));
		scannerClass.defineMethod("rest_size", callbackFactory.getMethod("rest_size"));
		scannerClass.defineMethod("restsize", callbackFactory.getMethod("rest_size"));
		scannerClass.defineMethod("scan", callbackFactory.getMethod("scan", RubyRegexp.class));
		scannerClass.defineMethod("scan_full", callbackFactory.getMethod("scan_full", RubyRegexp.class, RubyBoolean.class, RubyBoolean.class));
		scannerClass.defineMethod("scan_until", callbackFactory.getMethod("scan_until", RubyRegexp.class));
		scannerClass.defineMethod("search_full", callbackFactory.getMethod("search_full", RubyRegexp.class, RubyBoolean.class, RubyBoolean.class));
		scannerClass.defineMethod("skip", callbackFactory.getMethod("skip", RubyRegexp.class));
		scannerClass.defineMethod("skip_until", callbackFactory.getMethod("skip_until", RubyRegexp.class));
		scannerClass.defineMethod("string", callbackFactory.getMethod("string"));
		scannerClass.defineMethod("string=", callbackFactory.getMethod("set_string", RubyString.class));
		scannerClass.defineMethod("terminate", callbackFactory.getMethod("terminate"));
		scannerClass.defineMethod("unscan", callbackFactory.getMethod("unscan"));
		
		return scannerClass;
	}
	
	public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyStringScanner result = new RubyStringScanner(recv.getRuntime(),(RubyClass)recv);
        result.callInit(args);
        return result;
    }
	
	protected RubyStringScanner(IRuby runtime, RubyClass type) {
		super(runtime, type);
	}
	
	public IRubyObject initialize(IRubyObject[] args) {
		if (checkArgumentCount(args, 0, 2) > 0) {
			scanner = new StringScanner(args[0].convertToString().getValue());
		} else {
			scanner = new StringScanner();
		}
		return this;
	}
	
	public IRubyObject concat(IRubyObject obj) {
		scanner.append(obj.convertToString().getValue());
		return this;
	}
	
	private RubyBoolean trueOrFalse(boolean p) {
		if (p) {
			return getRuntime().getTrue();
		} else {
			return getRuntime().getFalse();
		}
	}
	
	private IRubyObject positiveFixnumOrNil(int val) {
		if (val > -1) {
			return RubyFixnum.newFixnum(getRuntime(), (long)val);
		} else {
			return getRuntime().getNil();
		}
	}
	
	private IRubyObject stringOrNil(CharSequence cs) {
		if (cs == null) {
			return getRuntime().getNil();
		} else {
			return RubyString.newString(getRuntime(), cs);
		}
	}

	public IRubyObject group(RubyFixnum num) {
		return stringOrNil(scanner.group(RubyFixnum.fix2int(num)));
	}
	
	public RubyBoolean bol_p() {
		return trueOrFalse(scanner.isBeginningOfLine());
	}
	
	public IRubyObject check(RubyRegexp rx) {
		return stringOrNil(scanner.check(rx.getPattern()));
	}
	
	public IRubyObject check_until(RubyRegexp rx) {
		return stringOrNil(scanner.checkUntil(rx.getPattern()));
	}
	
	public IRubyObject terminate() {
		scanner.terminate();
		return this;
	}
	
	public RubyBoolean eos_p() {
		return trueOrFalse(scanner.isEndOfString());
	}
	
	public IRubyObject exist_p(RubyRegexp rx) {
		return positiveFixnumOrNil(scanner.exists(rx.getPattern()));
	}
	
	public IRubyObject getch() {
		char c = scanner.getChar();
		if (c == 0) {
			return getRuntime().getNil();
		} else {
			return RubyString.newString(getRuntime(), new Character(c).toString());
		}
	}
	
	public IRubyObject inspect() {
		return super.inspect();
	}
	
	public IRubyObject match_p(RubyRegexp rx) {
		return positiveFixnumOrNil(scanner.matches(rx.getPattern()));
	}
	
	public IRubyObject matched() {
		return stringOrNil(scanner.matchedValue());
	}
	
	public RubyBoolean matched_p() {
		return trueOrFalse(scanner.matched());
	}
	
	public IRubyObject matched_size() {
		return positiveFixnumOrNil(scanner.matchedSize());
	}
	
	public IRubyObject peek(RubyFixnum length) {
		return RubyString.newString(getRuntime(), scanner.peek(RubyFixnum.fix2int(length)));
	}
	
	public RubyFixnum pos() {
		return RubyFixnum.newFixnum(getRuntime(), (long)scanner.getPos());
	}
	 
	public RubyFixnum set_pos(RubyFixnum pos) {
		try {
			scanner.setPos(RubyFixnum.fix2int(pos));
		} catch (IllegalArgumentException e) {
			throw getRuntime().newRangeError("index out of range");
		}
		return pos;
	}
	
	public IRubyObject post_match() {
		return stringOrNil(scanner.postMatch());
	}
	
	public IRubyObject pre_match() {
		return stringOrNil(scanner.preMatch());
	}
	
	public IRubyObject reset() {
		scanner.reset();
		return this;
	}
	
	public RubyString rest() {
		return RubyString.newString(getRuntime(), scanner.rest());
	}
	
	public RubyBoolean rest_p() {
		return trueOrFalse(!scanner.isEndOfString());
	}
	
	public RubyFixnum rest_size() {
		return RubyFixnum.newFixnum(getRuntime(), (long)scanner.rest().length());
	}
	
	public IRubyObject scan(RubyRegexp rx) {
		return stringOrNil(scanner.scan(rx.getPattern()));
	}
	
	public IRubyObject scan_full(RubyRegexp rx, RubyBoolean adv_ptr, RubyBoolean ret_str) {
		if (adv_ptr.isTrue()) {
			if (ret_str.isTrue()) {
				return stringOrNil(scanner.scan(rx.getPattern()));
			} else {
				return positiveFixnumOrNil(scanner.skip(rx.getPattern()));
			}
		} else {
			if (ret_str.isTrue()) {
				return stringOrNil(scanner.check(rx.getPattern()));
			} else {
				return positiveFixnumOrNil(scanner.matches(rx.getPattern()));
			}
		}
	}
	
	public IRubyObject scan_until(RubyRegexp rx) {
		return stringOrNil(scanner.scanUntil(rx.getPattern()));
	}
	
	public IRubyObject search_full(RubyRegexp rx, RubyBoolean adv_ptr, RubyBoolean ret_str) {
		if (adv_ptr.isTrue()) {
			if (ret_str.isTrue()) {
				return stringOrNil(scanner.scanUntil(rx.getPattern()));
			} else {
				return positiveFixnumOrNil(scanner.skipUntil(rx.getPattern()));
			}
		} else {
			if (ret_str.isTrue()) {
				return stringOrNil(scanner.checkUntil(rx.getPattern()));
			} else {
				return positiveFixnumOrNil(scanner.exists(rx.getPattern()));
			}
		}
	}
	
	public IRubyObject skip(RubyRegexp rx) {
		return positiveFixnumOrNil(scanner.skip(rx.getPattern()));
	}
	
	public IRubyObject skip_until(RubyRegexp rx) {
		return positiveFixnumOrNil(scanner.skipUntil(rx.getPattern()));
	}
	
	public RubyString string() {
		return RubyString.newString(getRuntime(), scanner.getString());
	}
	
	public RubyString set_string(RubyString str) {
		scanner.setString(str.getValue());
		return str;
	}
	
	public IRubyObject unscan() {
		scanner.unscan();
		return this;
	}
}

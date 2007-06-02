package org.jruby;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.StringScanner;

/**
 * @author kscott
 *
 */
public class RubyStringScanner extends RubyObject {
	
	private StringScanner scanner;
    
    private static ObjectAllocator STRINGSCANNER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyStringScanner(runtime, klass);
        }
    };
	
	public static RubyClass createScannerClass(final Ruby runtime) {
		RubyClass scannerClass = runtime.defineClass("StringScanner", runtime.getObject(), STRINGSCANNER_ALLOCATOR);
		CallbackFactory callbackFactory = runtime.callbackFactory(RubyStringScanner.class);
		
		scannerClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
		scannerClass.defineFastMethod("<<", callbackFactory.getFastMethod("concat", RubyKernel.IRUBY_OBJECT));
		scannerClass.defineFastMethod("concat", callbackFactory.getFastMethod("concat", RubyKernel.IRUBY_OBJECT));
		scannerClass.defineFastMethod("[]", callbackFactory.getFastMethod("group", RubyFixnum.class));
		scannerClass.defineFastMethod("beginning_of_line?", callbackFactory.getFastMethod("bol_p"));
		scannerClass.defineFastMethod("bol?", callbackFactory.getFastMethod("bol_p"));
		scannerClass.defineFastMethod("check", callbackFactory.getFastMethod("check", RubyRegexp.class));
		scannerClass.defineFastMethod("check_until", callbackFactory.getFastMethod("check_until", RubyRegexp.class));
		scannerClass.defineFastMethod("clear", callbackFactory.getFastMethod("terminate"));
		scannerClass.defineFastMethod("empty?", callbackFactory.getFastMethod("eos_p"));
		scannerClass.defineFastMethod("eos?", callbackFactory.getFastMethod("eos_p"));
		scannerClass.defineFastMethod("exist?", callbackFactory.getFastMethod("exist_p", RubyRegexp.class));
		scannerClass.defineFastMethod("get_byte", callbackFactory.getFastMethod("getch"));
		scannerClass.defineFastMethod("getbyte", callbackFactory.getFastMethod("getch"));
		scannerClass.defineFastMethod("getch", callbackFactory.getFastMethod("getch"));
		scannerClass.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
		scannerClass.defineFastMethod("match?", callbackFactory.getFastMethod("match_p", RubyRegexp.class));
		scannerClass.defineFastMethod("matched", callbackFactory.getFastMethod("matched"));
		scannerClass.defineFastMethod("matched?", callbackFactory.getFastMethod("matched_p"));
		scannerClass.defineFastMethod("matched_size", callbackFactory.getFastMethod("matched_size"));
		scannerClass.defineFastMethod("matchedsize", callbackFactory.getFastMethod("matched_size"));
		scannerClass.defineFastMethod("peek", callbackFactory.getFastMethod("peek", RubyFixnum.class));
		scannerClass.defineFastMethod("peep", callbackFactory.getFastMethod("peek", RubyFixnum.class));
		scannerClass.defineFastMethod("pointer", callbackFactory.getFastMethod("pos"));
		scannerClass.defineFastMethod("pointer=", callbackFactory.getFastMethod("set_pos", RubyFixnum.class));
		scannerClass.defineFastMethod("pos=", callbackFactory.getFastMethod("set_pos", RubyFixnum.class));
		scannerClass.defineFastMethod("pos", callbackFactory.getFastMethod("pos"));
		scannerClass.defineFastMethod("post_match", callbackFactory.getFastMethod("post_match"));
		scannerClass.defineFastMethod("pre_match", callbackFactory.getFastMethod("pre_match"));
		scannerClass.defineFastMethod("reset", callbackFactory.getFastMethod("reset"));
		scannerClass.defineFastMethod("rest", callbackFactory.getFastMethod("rest"));
		scannerClass.defineFastMethod("rest?", callbackFactory.getFastMethod("rest_p"));
		scannerClass.defineFastMethod("rest_size", callbackFactory.getFastMethod("rest_size"));
		scannerClass.defineFastMethod("restsize", callbackFactory.getFastMethod("rest_size"));
		scannerClass.defineFastMethod("scan", callbackFactory.getFastMethod("scan", RubyRegexp.class));
		scannerClass.defineFastMethod("scan_full", callbackFactory.getFastMethod("scan_full", RubyRegexp.class, RubyBoolean.class, RubyBoolean.class));
		scannerClass.defineFastMethod("scan_until", callbackFactory.getFastMethod("scan_until", RubyRegexp.class));
		scannerClass.defineFastMethod("search_full", callbackFactory.getFastMethod("search_full", RubyRegexp.class, RubyBoolean.class, RubyBoolean.class));
		scannerClass.defineFastMethod("skip", callbackFactory.getFastMethod("skip", RubyRegexp.class));
		scannerClass.defineFastMethod("skip_until", callbackFactory.getFastMethod("skip_until", RubyRegexp.class));
		scannerClass.defineFastMethod("string", callbackFactory.getFastMethod("string"));
		scannerClass.defineFastMethod("string=", callbackFactory.getFastMethod("set_string", RubyString.class));
		scannerClass.defineFastMethod("terminate", callbackFactory.getFastMethod("terminate"));
		scannerClass.defineFastMethod("unscan", callbackFactory.getFastMethod("unscan"));
		
		return scannerClass;
	}
	
	protected RubyStringScanner(Ruby runtime, RubyClass type) {
		super(runtime, type);
	}
	
	public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
		if (Arity.checkArgumentCount(getRuntime(), args, 0, 2) > 0) {
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

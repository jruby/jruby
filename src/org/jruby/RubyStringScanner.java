package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.StringScanner;
import org.jruby.util.ByteList;

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
        scannerClass.defineAnnotatedMethods(RubyStringScanner.class);
        return scannerClass;
    }

    protected RubyStringScanner(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "initialize", optional = 2, frame = true)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        if (Arity.checkArgumentCount(getRuntime(), args, 0, 2) > 0) {
            scanner = new StringScanner(args[0].convertToString().getByteList());
        } else {
            scanner = new StringScanner();
        }
        return this;
    }

    @JRubyMethod(name = {"contact", "<<"}, required = 1)
    public IRubyObject concat(IRubyObject obj) {
        scanner.append(obj.convertToString().getByteList());
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
            return RubyFixnum.newFixnum(getRuntime(), (long) val);
        } else {
            return getRuntime().getNil();
        }
    }

    private IRubyObject stringOrNil(ByteList cs) {
        if (cs == null) {
            return getRuntime().getNil();
        } else {
            return RubyString.newStringShared(getRuntime(), cs);
        }
    }

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject group(IRubyObject num) {
        return stringOrNil(scanner.group(RubyFixnum.fix2int(num)));
    }

    @JRubyMethod(name = {"bol?", "beginning_of_line"})
    public RubyBoolean bol_p() {
        return trueOrFalse(scanner.isBeginningOfLine());
    }

    @JRubyMethod(name = "check", required = 1)
    public IRubyObject check(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return stringOrNil(scanner.check(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "check_until", required = 1)
    public IRubyObject check_until(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return stringOrNil(scanner.checkUntil(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "terminate")
    public IRubyObject terminate() {
        scanner.terminate();
        return this;
    }

    @JRubyMethod(name = {"eos?", "empty?"})
    public RubyBoolean eos_p() {
        return trueOrFalse(scanner.isEndOfString());
    }

    @JRubyMethod(name = "exist?", required = 1)
    public IRubyObject exist_p(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return positiveFixnumOrNil(scanner.exists(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "getbyte")
    public IRubyObject getbyte() {
        getRuntime().getWarnings().warn("StringScanner#getbyte is obsolete; use #get_byte instead");
        return get_byte();
    }
    
    @JRubyMethod(name = "get_byte")
    public IRubyObject get_byte() {
        // FIXME: should we be distinguishing between chars and bytes?
        return getch();
    }

    @JRubyMethod(name = "getch")
    public IRubyObject getch() {
        byte c = scanner.getChar();
        if (c == 0) {
            return getRuntime().getNil();
        } else {
            return RubyString.newString(getRuntime(), new ByteList(new byte[]{c}, false));
        }
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect() {
        return super.inspect();
    }

    @JRubyMethod(name = "match?", required = 1)
    public IRubyObject match_p(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return positiveFixnumOrNil(scanner.matches(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "matched")
    public IRubyObject matched() {
        return stringOrNil(scanner.matchedValue());
    }

    @JRubyMethod(name = "matched?")
    public RubyBoolean matched_p() {
        return trueOrFalse(scanner.matched());
    }

    @JRubyMethod(name = "matched_size")
    public IRubyObject matched_size() {
        return positiveFixnumOrNil(scanner.matchedSize());
    }

    @JRubyMethod(name = "peek", required = 1)
    public IRubyObject peek(IRubyObject length) {
        return RubyString.newStringShared(getRuntime(), scanner.peek(RubyFixnum.fix2int(length)));
    }

    @JRubyMethod(name = "pos")
    public RubyFixnum pos() {
        return RubyFixnum.newFixnum(getRuntime(), (long) scanner.getPos());
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject pos) {
        try {
            scanner.setPos(RubyFixnum.fix2int(pos));
        } catch (IllegalArgumentException e) {
            throw getRuntime().newRangeError("index out of range");
        }
        return pos;
    }

    @JRubyMethod(name = "post_match")
    public IRubyObject post_match() {
        return stringOrNil(scanner.postMatch());
    }

    @JRubyMethod(name = "pre_match")
    public IRubyObject pre_match() {
        return stringOrNil(scanner.preMatch());
    }

    @JRubyMethod(name = "reset")
    public IRubyObject reset() {
        scanner.reset();
        return this;
    }

    @JRubyMethod(name = "rest")
    public RubyString rest() {
        return RubyString.newStringShared(getRuntime(), scanner.rest());
    }

    @JRubyMethod(name = "rest?")
    public RubyBoolean rest_p() {
        return trueOrFalse(!scanner.isEndOfString());
    }

    @JRubyMethod(name = "rest_size")
    public RubyFixnum rest_size() {
        return RubyFixnum.newFixnum(getRuntime(), (long) scanner.rest().length());
    }

    @JRubyMethod(name = "scan", required = 1)
    public IRubyObject scan(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return stringOrNil(scanner.scan(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "scan_full", required = 3)
    public IRubyObject scan_full(IRubyObject rx, IRubyObject adv_ptr, IRubyObject ret_str) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        RubyRegexp reg = (RubyRegexp)rx;
        if (adv_ptr.isTrue()) {
            if (ret_str.isTrue()) {
                return stringOrNil(scanner.scan(reg.getPattern()));
            } else {
                return positiveFixnumOrNil(scanner.skip(reg.getPattern()));
            }
        } else {
            if (ret_str.isTrue()) {
                return stringOrNil(scanner.check(reg.getPattern()));
            } else {
                return positiveFixnumOrNil(scanner.matches(reg.getPattern()));
            }
        }
    }

    @JRubyMethod(name = "scan_until", required = 1)
    public IRubyObject scan_until(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return stringOrNil(scanner.scanUntil(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "search_full", required = 3)
    public IRubyObject search_full(IRubyObject rx, IRubyObject adv_ptr, IRubyObject ret_str) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        RubyRegexp reg = (RubyRegexp)rx;
        if (adv_ptr.isTrue()) {
            if (ret_str.isTrue()) {
                return stringOrNil(scanner.scanUntil(reg.getPattern()));
            } else {
                return positiveFixnumOrNil(scanner.skipUntil(reg.getPattern()));
            }
        } else {
            if (ret_str.isTrue()) {
                return stringOrNil(scanner.checkUntil(reg.getPattern()));
            } else {
                return positiveFixnumOrNil(scanner.exists(reg.getPattern()));
            }
        }
    }

    @JRubyMethod(name = "skip", required = 1)
    public IRubyObject skip(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return positiveFixnumOrNil(scanner.skip(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "skip_until", required = 1)
    public IRubyObject skip_until(IRubyObject rx) {
        if (!(rx instanceof RubyRegexp)) throw getRuntime().newTypeError(rx, getRuntime().getRegexp());
        return positiveFixnumOrNil(scanner.skipUntil(((RubyRegexp)rx).getPattern()));
    }

    @JRubyMethod(name = "string")
    public RubyString string() {
        return RubyString.newStringShared(getRuntime(), scanner.getString());
    }

    @JRubyMethod(name = "string=", required = 1)
    public IRubyObject set_string(IRubyObject str) {
        if (!(str instanceof RubyString)) throw getRuntime().newTypeError(str, getRuntime().getString());
        scanner.setString(((RubyString)str).getByteList());
        return str;
    }

    @JRubyMethod(name = "unscan")
    public IRubyObject unscan() {
        scanner.unscan();
        return this;
    }

    @JRubyMethod(name = "must_C_version", meta = true)
    public static IRubyObject mustCversion(IRubyObject recv) {
        return recv;
    }
}

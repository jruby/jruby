package org.jruby;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.encoding.Encoding;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author kscott
 *
 */
public class RubyStringScanner extends RubyObject {

    private RubyString str;
    private int pos = 0;
    private int lastPos = -1;

    private Region regs;
    private int beg = -1;
    private int end = -1;

    private static final int MATCHED_STR_SCN_F = 1 << 11;     
    
    private static ObjectAllocator STRINGSCANNER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyStringScanner(runtime, klass);
        }
    };

    public static RubyClass createScannerClass(final Ruby runtime) {
        RubyClass scannerClass = runtime.defineClass("StringScanner", runtime.getObject(), STRINGSCANNER_ALLOCATOR);
        scannerClass.defineAnnotatedMethods(RubyStringScanner.class);
        scannerClass.setConstant("Version", runtime.newString("0.7.0").freeze());
        scannerClass.setConstant("Id", runtime.newString("$Id: strscan.c 13506 2007-09-24 08:56:24Z nobu $").freeze());
        return scannerClass;
    }

    private void clearMatched() {
        flags &= ~MATCHED_STR_SCN_F;
    }

    private void setMatched() {
        flags |= MATCHED_STR_SCN_F;
    }

    private boolean isMatched() {
        return (flags & MATCHED_STR_SCN_F) != 0;
    }
    
    private void check() {
        if (str == null) throw getRuntime().newArgumentError("uninitialized StringScanner object");
    }

    protected RubyStringScanner(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "initialize", required = 1, optional = 1, frame = true)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        Arity.checkArgumentCount(getRuntime(), args, 1, 2); // second one allowed, but ignored (MRI)
        str = args[0].convertToString();        
        return this;
    }
    
    @JRubyMethod(name = "initialize_copy", required = 1)
    public IRubyObject initialize_copy(IRubyObject other) {
        if (this == other) return this;
        if (!(other instanceof RubyStringScanner)) throw getRuntime().newTypeError("wrong argument type " + other.getMetaClass() + " (expected StringScanner)");

        RubyStringScanner otherScanner = (RubyStringScanner)other;
        str = otherScanner.str;
        pos = otherScanner.pos;
        lastPos = otherScanner.lastPos;
        flags = otherScanner.flags;

        regs = otherScanner.regs != null ? otherScanner.regs.clone() : null;
        beg = otherScanner.beg;
        end = otherScanner.end;

        return this;
    }
    
    @JRubyMethod(name = "reset")
    public IRubyObject reset() {
        check();
        pos = 0;
        clearMatched();
        return this;
    }
    
    @JRubyMethod(name = "terminate")
    public IRubyObject terminate() {
        check();
        pos = str.getByteList().realSize;
        clearMatched();
        return this;
    }
    
    @JRubyMethod(name = "clear")
    public IRubyObject clear() {
        check();
        getRuntime().getWarnings().warn("StringScanner#clear is obsolete; use #terminate instead");
        return terminate();
    }
    
    @JRubyMethod(name = "string")
    public RubyString string() {
        return str;
    }

    @JRubyMethod(name = "string=", required = 1)
    public IRubyObject set_string(IRubyObject str) {
        this.str = (RubyString)str.convertToString().strDup().freeze();
        pos = 0;
        clearMatched();
        return str;
    }

    @JRubyMethod(name = {"concat", "<<"}, required = 1)
    public IRubyObject concat(IRubyObject obj) {
        check();
        str.append(obj); // append will call convertToString()
        return this;
    }
    
    @JRubyMethod(name = "pos")
    public RubyFixnum pos() {
        check();
        return RubyFixnum.newFixnum(getRuntime(), pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject pos) {
        check();
        int i = RubyNumeric.num2int(pos);
        int size = str.getByteList().realSize;
        if (i < 0) i += size;
        if (i < 0 || i > size) throw getRuntime().newRangeError("index out of range.");
        this.pos = i;
        return RubyFixnum.newFixnum(getRuntime(), i);
    }

    private IRubyObject extractRange(int beg, int end) {
        int size = str.getByteList().realSize;
        if (beg > size) return getRuntime().getNil();
        if (end > size) end = size;
        return str.makeShared(beg, end - beg);
    }
    
    private IRubyObject extractBegLen(int beg, int len) {
        int size = str.getByteList().realSize;
        if (beg > size) return getRuntime().getNil();
        if (beg + len > size) len = size - beg;
        return str.makeShared(beg, len);
    }
    
    private IRubyObject scan(IRubyObject regex, boolean succptr, boolean getstr, boolean headonly) {
        if (!(regex instanceof RubyRegexp)) throw getRuntime().newTypeError("wrong argument type " + regex.getMetaClass() + " (expected Regexp)");
        check();
        
        Regex pattern = ((RubyRegexp)regex).getPattern();

        clearMatched();
        int rest = str.getByteList().realSize - pos;
        if (rest < 0) return getRuntime().getNil();

        ByteList value = str.getByteList();
        Matcher matcher = pattern.matcher(value.bytes, value.begin + pos, value.begin + value.realSize);

        final int ret;
        if (headonly) {
            ret = matcher.match(value.begin + pos, value.begin + value.realSize, Option.NONE);            
        } else {
            ret = matcher.search(value.begin + pos, value.begin + value.realSize, Option.NONE);
        }

        regs = matcher.getRegion(); 
        if (regs == null) {
            beg = matcher.getBegin();
            end = matcher.getEnd();
        } else {
            beg = regs.beg[0];
            end = regs.end[0];
        }

        if (ret < 0) return getRuntime().getNil();
        setMatched();

        lastPos = pos;
        if (succptr) pos += end;
        return  getstr ? extractBegLen(lastPos, end) : RubyFixnum.newFixnum(getRuntime(), end);
    }
    
    @JRubyMethod(name = "scan", required = 1)
    public IRubyObject scan(IRubyObject regex) {
        return scan(regex, true, true, true);
    }
    
    @JRubyMethod(name = "match?", required = 1)
    public IRubyObject match_p(IRubyObject regex) {
        return scan(regex, false, false, true);
    }
    
    @JRubyMethod(name = "skip", required = 1)
    public IRubyObject skip(IRubyObject regex) {
        return scan(regex, true, false, true);
    }
    
    @JRubyMethod(name = "check", required = 1)
    public IRubyObject check(IRubyObject regex) {
        return scan(regex, false, true, true);
    }
    
    @JRubyMethod(name = "scan_full", required = 3)
    public IRubyObject scan_full(IRubyObject regex, IRubyObject s, IRubyObject f) {
        return scan(regex, s.isTrue(), f.isTrue(), true);
    }

    @JRubyMethod(name = "scan_until", required = 1)
    public IRubyObject scan_until(IRubyObject regex) {
        return scan(regex, true, true, false);
    }
    
    @JRubyMethod(name = "exist?", required = 1)
    public IRubyObject exist_p(IRubyObject regex) {
        return scan(regex, false, false, false);        
    }
    
    @JRubyMethod(name = "skip_until", required = 1)
    public IRubyObject skip_until(IRubyObject regex) {
        return scan(regex, true, false, false);
    }

    @JRubyMethod(name = "check_until", required = 1)
    public IRubyObject check_until(IRubyObject regex) {
        return scan(regex, false, true, false);        
    }
    
    @JRubyMethod(name = "search_full", required = 3)
    public IRubyObject search_full(IRubyObject regex, IRubyObject s, IRubyObject f) {
        return scan(regex, s.isTrue(), f.isTrue(), false);
    }

    private void adjustRegisters() {
        beg = 0;
        end = pos - lastPos;
        regs = null;
    }
    
    @JRubyMethod(name = "getch")
    public IRubyObject getch() {
        check();
        clearMatched();
        
        ByteList value = str.getByteList();
        if (pos >= value.realSize) return getRuntime().getNil();

        Encoding enc = getRuntime().getKCode().getEncoding();
        
        int len;
        if (enc.isSingleByte()) {
            len = 1;
        } else {
            len = enc.length(value.bytes[value.begin + pos]);
        }
        
        if (pos + len > value.realSize) len = value.realSize - pos;
        lastPos = pos;
        pos += len;
        
        setMatched();
        adjustRegisters();
        
        return extractRange(lastPos + beg, lastPos + end);
    }
    
    @JRubyMethod(name = "get_byte")
    public IRubyObject get_byte() {
        check();
        clearMatched();
        if (pos >= str.getByteList().realSize) return getRuntime().getNil();
        
        lastPos = pos;
        pos++;
        
        setMatched();
        adjustRegisters();
        
        return extractRange(lastPos + beg, lastPos + end);
    }
    
    @JRubyMethod(name = "getbyte")
    public IRubyObject getbyte() {
        getRuntime().getWarnings().warn("StringScanner#getbyte is obsolete; use #get_byte instead");
        return getbyte();
    }
    
    @JRubyMethod(name = "peek", required = 1)
    public IRubyObject peek(IRubyObject length) {
        check();
        int len = RubyNumeric.num2int(length);
        ByteList value = str.getByteList();
        if (pos >= value.realSize) return RubyString.newEmptyString(getRuntime()).infectBy(str);
        
        if (pos + len > value.realSize) len = value.realSize - pos;
        return extractBegLen(pos, len);
    }
    
    @JRubyMethod(name = "peep", required = 1)
    public IRubyObject peep(IRubyObject length) {
        getRuntime().getWarnings().warn("StringScanner#peep is obsolete; use #peek instead");
        return peek(length);
    }
    
    @JRubyMethod(name = "unscan")
    public IRubyObject unscan() {
        check();
        // TODO: should be ScanError here 
        if (!isMatched()) throw getRuntime().newEOFError("unscan failed: previous match had failed");
        pos = lastPos;
        clearMatched();
        return this;
    }
    
    @JRubyMethod(name = {"bol?", "beginning_of_line"})
    public IRubyObject bol_p() {
        check();
        ByteList value = str.getByteList();
        if (pos > value.realSize) return getRuntime().getNil();
        if (pos == 0) return getRuntime().getTrue();
        return value.bytes[(value.begin + pos) - 1] == (byte)'\n' ? getRuntime().getTrue() : getRuntime().getFalse();        
    }
    
    @JRubyMethod(name = "eos?")
    public RubyBoolean eos_p() {
        check();
        return pos >= str.getByteList().realSize ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p() {
        getRuntime().getWarnings().warn("StringScanner#empty? is obsolete; use #eos? instead");
        return eos_p();
    }
    
    @JRubyMethod(name = "rest?")
    public RubyBoolean rest_p() {
        check();
        return pos >= str.getByteList().realSize ? getRuntime().getFalse() : getRuntime().getTrue();
    }

    @JRubyMethod(name = "matched?")
    public RubyBoolean matched_p() {
        check();
        return isMatched() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "matched")
    public IRubyObject matched() {
        check();
        if (!isMatched()) return getRuntime().getNil(); 
        return extractRange(lastPos + beg, lastPos + end);
    }
    
    @JRubyMethod(name = "matched_size")
    public IRubyObject matched_size() {
        check();
        if (!isMatched()) return getRuntime().getNil();
        return RubyFixnum.newFixnum(getRuntime(), end - beg);
    }

    @JRubyMethod(name = "matchedsize")
    public IRubyObject matchedsize() {
        getRuntime().getWarnings().warn("StringScanner#matchedsize is obsolete; use #matched_size instead");
        return matched_size();        
    }

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(IRubyObject idx) {
        check();
        if (!isMatched()) return getRuntime().getNil();
        int i = RubyNumeric.num2int(idx);
        
        int numRegs = regs == null ? 1 : regs.numRegs;
        if (i < 0) i += numRegs;
        if (i < 0 || i >= numRegs) return getRuntime().getNil();
        
        if (regs == null) {
            assert i == 0;
            if (beg == -1) return getRuntime().getNil();
            return extractRange(lastPos + beg, lastPos + end);
        } else {
            if (regs.beg[i] == -1) return getRuntime().getNil();
            return extractRange(lastPos + regs.beg[i], lastPos + regs.end[i]);
        }
    }

    @JRubyMethod(name = "pre_match")
    public IRubyObject pre_match() {
        check();
        if (!isMatched()) return getRuntime().getNil();
        return extractRange(0, lastPos + beg);
    }
    
    @JRubyMethod(name = "post_match")
    public IRubyObject post_match() {
        check();
        if (!isMatched()) return getRuntime().getNil();
        return extractRange(lastPos + end, str.getByteList().realSize);
    }
    
    @JRubyMethod(name = "rest")
    public IRubyObject rest() {
        check();
        ByteList value = str.getByteList();
        if (pos >= value.realSize) return RubyString.newEmptyString(getRuntime()).infectBy(str);
        return extractRange(pos, value.realSize);
    }
    
    @JRubyMethod(name = "rest_size")
    public RubyFixnum rest_size() {
        check();
        ByteList value = str.getByteList();
        if (pos >= value.realSize) return RubyFixnum.zero(getRuntime());
        return RubyFixnum.newFixnum(getRuntime(), value.realSize - pos);
    }

    @JRubyMethod(name = "restsize")
    public RubyFixnum restsize() {
        getRuntime().getWarnings().warn("StringScanner#restsize is obsolete; use #rest_size instead");
        return rest_size();
    }
    
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect() {
        if (str == null) return inspect("(uninitialized)");
        if (pos >= str.getByteList().realSize) return inspect("fin");
        if (pos == 0) return inspect(pos + "/" + str.getByteList().realSize + " @ " + inspect2());
        return inspect(pos + "/" + str.getByteList().realSize + " " + inspect1() + " @ " + inspect2()); 
    }
    
    private IRubyObject inspect(String msg) {
        IRubyObject result = getRuntime().newString("#<" + getMetaClass() + " " + msg + ">"); 
        if (str != null) result.infectBy(str);
        return result;
    }
    
    private static final int INSPECT_LENGTH = 5;
    
    private IRubyObject inspect1() {
        if (pos == 0) return RubyString.newEmptyString(getRuntime());
        if (pos > INSPECT_LENGTH) {
            return RubyString.newString(getRuntime(), "...".getBytes()).append(str.substr(pos - INSPECT_LENGTH, INSPECT_LENGTH)).inspect();
        } else {
            return str.substr(0, pos).inspect();
        }
    }
    
    private IRubyObject inspect2() {
        if (pos >= str.getByteList().realSize) return RubyString.newEmptyString(getRuntime());
        int len = str.getByteList().realSize - pos;
        if (len > INSPECT_LENGTH) {
            return ((RubyString)str.substr(pos, INSPECT_LENGTH)).cat("...".getBytes()).inspect();
        } else {
            return str.substr(pos, len).inspect();
        }
    }

    @JRubyMethod(name = "must_C_version", meta = true)
    public static IRubyObject mustCversion(IRubyObject recv) {
        return recv;
    }
}

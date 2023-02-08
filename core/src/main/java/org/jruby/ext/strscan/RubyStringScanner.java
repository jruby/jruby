/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.strscan;

import org.jcodings.Encoding;
import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyMatchData;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.util.Iterator;

import static org.jruby.runtime.Visibility.PRIVATE;

/**
 * @author kscott
 */
@JRubyClass(name = "StringScanner")
public class RubyStringScanner extends RubyObject {

    private RubyString str;
    private int curr = 0;
    private int prev = -1;

    private Region regs;
    private Regex pattern;
    private int scannerFlags;
    private boolean fixedAnchor;

    private static final int MATCHED_STR_SCN_F = 1 << 11;

    public static RubyClass createScannerClass(final Ruby runtime) {
        RubyClass Object = runtime.getObject();

        RubyClass scannerClass = runtime.defineClass("StringScanner", Object, RubyStringScanner::new);

        RubyClass standardError = runtime.getStandardError();
        RubyClass error = scannerClass.defineClassUnder("Error", standardError, standardError.getAllocator());
        if (!Object.isConstantDefined("ScanError")) {
            Object.defineConstant("ScanError", error);
        }

        RubyString version = runtime.newString("3.0.2");
        version.setFrozen(true);
        scannerClass.setConstant("Version", version);
        RubyString id = runtime.newString("$Id$");
        id.setFrozen(true);
        scannerClass.setConstant("Id", id);

        scannerClass.defineAnnotatedMethods(RubyStringScanner.class);

        return scannerClass;
    }

    // Provided temporarily to bridge the gap between joni 2.1 and 2.2
    private static final RegionAdapter REGION_ADAPTER;
    static {
        RegionAdapter adapter;
        try {
            Region.class.getMethod("newRegion", int.class, int.class);
            // ok, proceed with factory-based adapter
            adapter = new FactoryRegionAdapter();
        } catch (NoSuchMethodException | SecurityException ex) {
            adapter = new OldRegionAdapter();
        }
        REGION_ADAPTER = adapter;
    }

    private interface RegionAdapter {
        Region newRegion(int beg, int end);
        int getBeg(Region region, int index);
        int getEnd(Region region, int index);
        int setBeg(Region region, int index, int value);
        int setEnd(Region region, int index, int value);
        int getNumRegs(Region region);
    }

    private static class OldRegionAdapter implements RegionAdapter {
        @Override
        @SuppressWarnings("deprecation")
        public Region newRegion(int beg, int end) {
            return new Region(beg, end);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getBeg(Region region, int index) {
            return region.beg[index];
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getEnd(Region region, int index) {
            return region.end[index];
        }

        @Override
        @SuppressWarnings("deprecation")
        public int setBeg(Region region, int index, int value) {
            return region.beg[index] = value;
        }

        @Override
        @SuppressWarnings("deprecation")
        public int setEnd(Region region, int index, int value) {
            return region.end[index] = value;
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getNumRegs(Region region) {
            return region.numRegs;
        }
    }

    private static class FactoryRegionAdapter implements RegionAdapter {
        @Override
        public Region newRegion(int beg, int end) {
            return Region.newRegion(beg, end);
        }

        @Override
        public int getBeg(Region region, int index) {
            return region.getBeg(index);
        }

        @Override
        public int getEnd(Region region, int index) {
            return region.getEnd(index);
        }

        @Override
        public int setBeg(Region region, int index, int value) {
            return region.setBeg(index, value);
        }

        @Override
        public int setEnd(Region region, int index, int value) {
            return region.setEnd(index, value);
        }

        @Override
        public int getNumRegs(Region region) {
            return region.getNumRegs();
        }
    }

    private void clearMatched() {
        scannerFlags &= ~MATCHED_STR_SCN_F;
    }

    private void setMatched() {
        scannerFlags |= MATCHED_STR_SCN_F;
    }

    private boolean isMatched() {
        return (scannerFlags & MATCHED_STR_SCN_F) != 0;
    }

    private void check(ThreadContext context) {
        if (str == null) throw context.runtime.newArgumentError("uninitialized StringScanner object");
    }

    protected RubyStringScanner(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject string) {
        return initialize(context, string, context.nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject string, IRubyObject dupOrOpts) {
        this.str = string.convertToString();
        this.fixedAnchor = ArgsUtil.extractKeywordArg(context, "fixed_anchor", dupOrOpts).isTrue();
        this.regs = REGION_ADAPTER.newRegion(0, 0);

        return this;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject string, IRubyObject dup, IRubyObject opts) {
        return initialize(context, string, opts);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        if (this == other) return this;
        if (!(other instanceof RubyStringScanner)) {
            throw context.runtime.newTypeError("wrong argument type " + other.getMetaClass() + " (expected StringScanner)");
        }

        RubyStringScanner otherScanner = (RubyStringScanner) other;
        str = otherScanner.str;
        curr = otherScanner.curr;
        prev = otherScanner.prev;
        scannerFlags = otherScanner.scannerFlags;

        regs = otherScanner.regs.clone();
        pattern = otherScanner.pattern;
        fixedAnchor = otherScanner.fixedAnchor;

        return this;
    }

    @JRubyMethod(name = "reset")
    public IRubyObject reset(ThreadContext context) {
        check(context);
        curr = 0;
        clearMatched();
        return this;
    }

    @JRubyMethod(name = "terminate")
    public IRubyObject terminate(ThreadContext context) {
        check(context);
        curr = str.getByteList().getRealSize();
        clearMatched();
        return this;
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context) {
        check(context);
        Ruby runtime = context.runtime;
        if (runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.DEPRECATED_METHOD, "StringScanner#clear is obsolete; use #terminate instead");
        }
        return terminate(context);
    }

    @JRubyMethod(name = "string")
    public RubyString string() {
        return str;
    }

    @JRubyMethod(name = "string=")
    public IRubyObject set_string(ThreadContext context, IRubyObject str) {
        this.str = RubyString.stringValue(str);
        curr = 0;
        clearMatched();
        return str;
    }

    @JRubyMethod(name = {"concat", "<<"})
    public IRubyObject concat(ThreadContext context, IRubyObject obj) {
        check(context);
        str.append(obj.convertToString());
        return this;
    }

    @JRubyMethod(name = {"pos", "pointer"})
    public RubyFixnum pos(ThreadContext context) {
        check(context);
        return RubyFixnum.newFixnum(context.runtime, curr);
    }

    @JRubyMethod(name = {"pos=", "pointer="})
    public IRubyObject set_pos(ThreadContext context, IRubyObject pos) {
        check(context);

        Ruby runtime = context.runtime;

        int i = RubyNumeric.num2int(pos);
        int size = str.getByteList().getRealSize();
        if (i < 0) i += size;
        if (i < 0 || i > size) throw runtime.newRangeError("index out of range.");
        this.curr = i;

        return RubyFixnum.newFixnum(runtime, i);
    }

    @JRubyMethod(name = "charpos")
    public IRubyObject charpos(ThreadContext context) {
        Ruby runtime = context.runtime;

        ByteList strBL = str.getByteList();
        int strBeg = strBL.begin();

        return runtime.newFixnum(StringSupport.strLength(strBL.getEncoding(), strBL.unsafeBytes(), strBeg, strBeg + curr));
    }

    private IRubyObject extractRange(Ruby runtime, int beg, int end) {
        int size = str.getByteList().getRealSize();

        if (beg > size) return runtime.getNil();
        if (end > size) end = size;

        return str.makeSharedString(runtime, beg, end - beg);
    }

    private IRubyObject extractBegLen(Ruby runtime, int beg, int len) {
        assert len >= 0;

        int size = str.getByteList().getRealSize();

        if (beg > size) return runtime.getNil();
        len = Math.min(len, size - beg);

        return str.makeSharedString(runtime, beg, len);
    }

    final ThreadLocal<Matcher> currentMatcher = new ThreadLocal<>();
    final RubyThread.Task<RubyStringScanner, Integer> task = new RubyThread.Task<RubyStringScanner, Integer>() {
        @Override
        public Integer run(ThreadContext context, RubyStringScanner rubyStringScanner) throws InterruptedException {
            ByteList value = str.getByteList();
            return currentMatcher.get().matchInterruptible(value.begin() + curr, value.begin() + value.realSize(), Option.NONE);
        }

        @Override
        public void wakeup(RubyThread thread, RubyStringScanner rubyStringScanner) {
            thread.getNativeThread().interrupt();
        }
    };

    // MRI: strscan_do_scan
    private IRubyObject scan(ThreadContext context, IRubyObject regex, boolean succptr, boolean getstr, boolean headonly) {
        final Ruby runtime = context.runtime;

        if (headonly) {
            if (!(regex instanceof RubyRegexp)) {
                regex = regex.convertToString();
            }
        } else {
            if (!(regex instanceof RubyRegexp)) {
                throw runtime.newTypeError("wrong argument type " + regex.getMetaClass() + " (expected Regexp)");
            }
        }

        check(context);

        ByteList strBL = str.getByteList();
        int strBeg = strBL.getBegin();

        clearMatched();

        if (restLen() < 0) {
            return context.nil;
        }

        if (regex instanceof RubyRegexp) {
            pattern = ((RubyRegexp) regex).preparePattern(str);

            int currPtr = currPtr();
            int range = currPtr + restLen();

            Matcher matcher = pattern.matcher(strBL.getUnsafeBytes(), matchTarget(), range);
            final int ret;
            if (headonly) {
                ret = RubyRegexp.matcherMatch(context, matcher, currPtr, range, Option.NONE);
            } else {
                ret = RubyRegexp.matcherSearch(context, matcher, currPtr, range, Option.NONE);
            }

            Region matchRegion = matcher.getRegion();
            if (matchRegion == null) {
                regs = REGION_ADAPTER.newRegion(matcher.getBegin(), matcher.getEnd());
            } else {
                regs = matchRegion;
            }

            if  (ret == -2) {
                throw runtime.newRaiseException((RubyClass) getMetaClass().getConstant("ScanError"), "regexp buffer overflow");
            }
            if (ret < 0) return context.nil;
        } else {
            RubyString pattern = (RubyString) regex;

            str.checkEncoding(pattern);

            if (restLen() < pattern.size()) {
                return context.nil;
            }

            ByteList patternBL = pattern.getByteList();
            int patternSize = patternBL.realSize();

            if (ByteList.memcmp(strBL.unsafeBytes(), strBeg + curr, patternBL.unsafeBytes(), patternBL.begin(), patternSize) != 0) {
                return context.nil;
            }

            setRegisters(patternSize);
        }

        setMatched();
        prev = curr;

        if (succptr) {
            succ();
        }

        int length = lastMatchLength();

        if (getstr) {
            return extractBegLen(runtime, prev, length);
        }

        return RubyFixnum.newFixnum(runtime, length);
    }

    private int lastMatchLength() {
        if (fixedAnchor) {
            return REGION_ADAPTER.getEnd(regs, 0) - prev;
        } else {
            return REGION_ADAPTER.getEnd(regs, 0);
        }
    }

    private void succ() {
        if (fixedAnchor) {
            this.curr = REGION_ADAPTER.getEnd(regs, 0);
        } else {
            this.curr += REGION_ADAPTER.getEnd(regs, 0);
        }
    }

    private int currPtr() {
        return str.getByteList().getBegin() + curr;
    }

    private int matchTarget() {
        if (fixedAnchor) {
            return str.getByteList().getBegin();
        } else {
            return str.getByteList().getBegin() + curr;
        }
    }

    private int restLen() {
        return str.size() - curr;
    }

    // MRI: set_registers
    private void setRegisters(int length) {
        if (fixedAnchor) {
            regs = REGION_ADAPTER.newRegion(curr, curr + length);
        } else {
            regs = REGION_ADAPTER.newRegion(0, length);
        }
    }

    @JRubyMethod(name = "scan")
    public IRubyObject scan(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, true, true, true);
    }

    @JRubyMethod(name = "match?")
    public IRubyObject match_p(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, false, false, true);
    }

    @JRubyMethod(name = "skip")
    public IRubyObject skip(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, true, false, true);
    }

    @JRubyMethod(name = "check")
    public IRubyObject check(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, false, true, true);
    }

    @JRubyMethod(name = "scan_full")
    public IRubyObject scan_full(ThreadContext context, IRubyObject regex, IRubyObject s, IRubyObject f) {
        return scan(context, regex, s.isTrue(), f.isTrue(), true);
    }

    @JRubyMethod(name = "scan_until")
    public IRubyObject scan_until(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, true, true, false);
    }

    @JRubyMethod(name = "exist?")
    public IRubyObject exist_p(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, false, false, false);
    }

    @JRubyMethod(name = "skip_until")
    public IRubyObject skip_until(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, true, false, false);
    }

    @JRubyMethod(name = "check_until")
    public IRubyObject check_until(ThreadContext context, IRubyObject regex) {
        return scan(context, regex, false, true, false);
    }

    @JRubyMethod(name = "search_full")
    public IRubyObject search_full(ThreadContext context, IRubyObject regex, IRubyObject s, IRubyObject f) {
        return scan(context, regex, s.isTrue(), f.isTrue(), false);
    }

    // MRI: adjust_register_to_matched
    private void adjustRegisters() {
        if (fixedAnchor) {
            regs = REGION_ADAPTER.newRegion(prev, curr);
        } else {
            regs = REGION_ADAPTER.newRegion(0, curr - prev);
        }
    }

    private int adjustRegisterPosition(int position) {
        if (fixedAnchor) {
            return position;
        } else {
            return prev + position;
        }
    }

    @JRubyMethod(name = "getch")
    public IRubyObject getch(ThreadContext context) {
        return getchCommon(context);
    }

    public IRubyObject getchCommon(ThreadContext context) {
        check(context);
        clearMatched();
        ByteList strBL = str.getByteList();
        int strSize = strBL.getRealSize();

        if (curr >= strSize) return context.nil;

        Ruby runtime = context.runtime;

        Encoding strEnc = strBL.getEncoding();
        int setBeg = strBL.getBegin();

        int len = strEnc.isSingleByte() ? 1 : StringSupport.length(strEnc, strBL.getUnsafeBytes(), setBeg + curr, setBeg + strSize);
        len = Math.min(len, restLen());

        prev = curr;
        curr += len;

        setMatched();
        adjustRegisters();

        return extractRange(runtime,
                adjustRegisterPosition(REGION_ADAPTER.getBeg(regs, 0)),
                adjustRegisterPosition(REGION_ADAPTER.getEnd(regs, 0)));
    }

    @JRubyMethod(name = "get_byte")
    public IRubyObject get_byte(ThreadContext context) {
        check(context);
        clearMatched();
        if (curr >= str.getByteList().getRealSize()) return context.nil;

        prev = curr;
        curr++;

        setMatched();
        adjustRegisters();

        return extractRange(context.runtime,
                adjustRegisterPosition(REGION_ADAPTER.getBeg(regs, 0)),
                adjustRegisterPosition(REGION_ADAPTER.getEnd(regs, 0)));
    }

    @JRubyMethod(name = "getbyte")
    public IRubyObject getbyte(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.DEPRECATED_METHOD, "StringScanner#getbyte is obsolete; use #get_byte instead");
        }
        return get_byte(context);
    }

    @JRubyMethod(name = "peek")
    public IRubyObject peek(ThreadContext context, IRubyObject length) {
        check(context);

        int len = RubyNumeric.num2int(length);
        if (len < 0) {
            throw context.runtime.newArgumentError("negative string size (or size too big)");
        }

        ByteList value = str.getByteList();
        if (curr >= value.getRealSize()) return RubyString.newEmptyString(context.runtime);
        if (curr + len > value.getRealSize()) len = value.getRealSize() - curr;

        return extractBegLen(context.runtime, curr, len);
    }

    @JRubyMethod(name = "peep")
    public IRubyObject peep(ThreadContext context, IRubyObject length) {
        Ruby runtime = context.runtime;
        if (runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.DEPRECATED_METHOD, "StringScanner#peep is obsolete; use #peek instead");
        }
        return peek(context, length);
    }

    @JRubyMethod(name = "unscan")
    public IRubyObject unscan(ThreadContext context) {
        check(context);

        if (!isMatched()) {
            Ruby runtime = context.runtime;

            RubyClass errorClass = runtime.getClass("StringScanner").getClass("Error");
            throw RaiseException.from(runtime, errorClass, "unscan failed: previous match had failed");
        }

        curr = prev;
        clearMatched();

        return this;
    }

    @JRubyMethod(name = "beginning_of_line?", alias = "bol?")
    public IRubyObject bol_p(ThreadContext context) {
        check(context);

        ByteList value = str.getByteList();
        if (curr > value.getRealSize()) return context.nil;
        if (curr == 0) return context.tru;
        return value.getUnsafeBytes()[(value.getBegin() + curr) - 1] == (byte) '\n' ? context.tru : context.fals;
    }

    @JRubyMethod(name = "eos?")
    public RubyBoolean eos_p(ThreadContext context) {
        check(context);
        return curr >= str.getByteList().getRealSize() ? context.tru : context.fals;
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.DEPRECATED_METHOD, "StringScanner#empty? is obsolete; use #eos? instead");
        }
        return eos_p(context);
    }

    @JRubyMethod(name = "rest?")
    public RubyBoolean rest_p(ThreadContext context) {
        check(context);
        return curr >= str.getByteList().getRealSize() ? context.fals : context.tru;
    }

    @JRubyMethod(name = "matched?")
    public RubyBoolean matched_p(ThreadContext context) {
        check(context);
        return isMatched() ? context.tru : context.fals;
    }

    @JRubyMethod(name = "matched")
    public IRubyObject matched(ThreadContext context) {
        check(context);
        if (!isMatched()) return context.nil;
        return extractRange(context.runtime,
                adjustRegisterPosition(REGION_ADAPTER.getBeg(regs, 0)),
                adjustRegisterPosition(REGION_ADAPTER.getEnd(regs, 0)));
    }

    @JRubyMethod(name = "matched_size")
    public IRubyObject matched_size(ThreadContext context) {
        check(context);
        if (!isMatched()) return context.nil;
        return RubyFixnum.newFixnum(context.runtime, REGION_ADAPTER.getEnd(regs, 0) - REGION_ADAPTER.getBeg(regs, 0));
    }

    @JRubyMethod(name = "matchedsize")
    public IRubyObject matchedsize(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.DEPRECATED_METHOD, "StringScanner#matchedsize is obsolete; use #matched_size instead");
        }
        return matched_size();
    }

    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject idx) {
        check(context);

        if (!isMatched()) {
            return context.nil;
        }

        if (idx instanceof RubySymbol || idx instanceof RubyString) {
            if (pattern == null) return context.nil;
        }

        Ruby runtime = context.runtime;

        int i = RubyMatchData.backrefNumber(runtime, pattern, regs, idx);

        return extractRegion(context, i);
    }

    private IRubyObject extractRegion(ThreadContext context, int i) {
        int numRegs = REGION_ADAPTER.getNumRegs(regs);

        if (i < 0) i += numRegs;
        if (i < 0 || i >= numRegs || REGION_ADAPTER.getBeg(regs, i) == -1) {
            return context.nil;
        }

        return extractRange(context.runtime,
                adjustRegisterPosition(REGION_ADAPTER.getBeg(regs, i)),
                adjustRegisterPosition(REGION_ADAPTER.getEnd(regs, i)));
    }

    @JRubyMethod(name = "pre_match")
    public IRubyObject pre_match(ThreadContext context) {
        check(context);
        if (!isMatched()) {
            return context.nil;
        }
        return extractRange(context.runtime, 0, adjustRegisterPosition(REGION_ADAPTER.getBeg(regs, 0)));
    }

    @JRubyMethod(name = "post_match")
    public IRubyObject post_match(ThreadContext context) {
        check(context);

        if (!isMatched()) {
            return context.nil;
        }

        return extractRange(context.runtime,
                adjustRegisterPosition(REGION_ADAPTER.getEnd(regs, 0)),
                str.getByteList().getRealSize());
    }

    @JRubyMethod(name = "rest")
    public IRubyObject rest(ThreadContext context) {
        check(context);
        Ruby runtime = context.runtime;

        ByteList value = str.getByteList();

        if (curr >= value.getRealSize()) {
            return RubyString.newEmptyString(runtime);
        }

        return extractRange(runtime, curr, value.getRealSize());
    }

    @JRubyMethod(name = "rest_size")
    public RubyFixnum rest_size(ThreadContext context) {
        check(context);
        Ruby runtime = context.runtime;

        ByteList value = str.getByteList();

        if (curr >= value.getRealSize()) return RubyFixnum.zero(runtime);

        return RubyFixnum.newFixnum(runtime, value.getRealSize() - curr);
    }

    @JRubyMethod(name = "restsize")
    public RubyFixnum restsize(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.DEPRECATED_METHOD, "StringScanner#restsize is obsolete; use #rest_size instead");
        }
        return rest_size(context);
    }

    @JRubyMethod(name = "inspect")
    @Override
    public IRubyObject inspect() {
        if (str == null) return inspect("(uninitialized)");
        if (curr >= str.getByteList().getRealSize()) return inspect("fin");
        if (curr == 0) return inspect(curr + "/" + str.getByteList().getRealSize() + " @ " + inspect2());
        return inspect(curr + "/" + str.getByteList().getRealSize() + " " + inspect1() + " @ " + inspect2());
    }

    @JRubyMethod(name = "fixed_anchor?")
    public IRubyObject fixed_anchor_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, fixedAnchor);
    }

    @JRubyMethod(name = "named_captures")
    public IRubyObject named_captured(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject nil = context.nil;

        RubyHash captures = RubyHash.newHash(runtime);

        Iterator<NameEntry> nameEntryIterator = pattern.namedBackrefIterator();

        while (nameEntryIterator.hasNext()) {
            NameEntry nameEntry = nameEntryIterator.next();
            IRubyObject value = nil;

            for (int i : nameEntry.getBackRefs()) {
                value = extractRegion(context, i);
            }

            int nameP = nameEntry.nameP;
            captures.op_aset(context, RubyString.newStringShared(runtime, nameEntry.name, nameP, nameEntry.nameEnd - nameP), value);
        }

        return captures;
    }

    private IRubyObject inspect(String msg) {
        RubyString result = getRuntime().newString("#<" + getMetaClass() + " " + msg + ">");
        return result;
    }

    private static final int INSPECT_LENGTH = 5;

    private static final byte[] DOT_BYTES = "...".getBytes();

    private IRubyObject inspect1() {
        final Ruby runtime = getRuntime();
        if (curr == 0) return RubyString.newEmptyString(runtime);
        if (curr > INSPECT_LENGTH) {
            return RubyString.newStringNoCopy(runtime, DOT_BYTES).append(str.substr(runtime, curr - INSPECT_LENGTH, INSPECT_LENGTH)).inspect();
        }
        return str.substr(runtime, 0, curr).inspect();
    }

    private IRubyObject inspect2() {
        final Ruby runtime = getRuntime();
        if (curr >= str.getByteList().getRealSize()) return RubyString.newEmptyString(runtime);
        int len = str.getByteList().getRealSize() - curr;
        if (len > INSPECT_LENGTH) {
            return ((RubyString) str.substr(runtime, curr, INSPECT_LENGTH)).cat(DOT_BYTES).inspect();
        }
        return str.substr(runtime, curr, len).inspect();
    }

    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        if (!isMatched()) return context.nil;
        return context.runtime.newFixnum(REGION_ADAPTER.getNumRegs(regs));
    }

    @JRubyMethod(name = "captures")
    public IRubyObject captures(ThreadContext context) {
        int i, numRegs;
        RubyArray newAry;

        if (!isMatched()) return context.nil;

        Ruby runtime = context.runtime;

        numRegs = REGION_ADAPTER.getNumRegs(regs);
        newAry = RubyArray.newArray(runtime, numRegs);

        for (i = 1; i < numRegs; i++) {
            IRubyObject str = extractRange(runtime,
                    adjustRegisterPosition(REGION_ADAPTER.getBeg(regs, i)),
                    adjustRegisterPosition(REGION_ADAPTER.getEnd(regs, i)));
            newAry.push(str);
        }

        return newAry;
    }

    @JRubyMethod(name = "values_at", rest = true)
    public IRubyObject values_at(ThreadContext context, IRubyObject[] args) {
        int i;
        RubyArray newAry;

        if (!isMatched()) return context.nil;

        Ruby runtime = context.runtime;

        newAry = RubyArray.newArray(runtime, args.length);
        for (i = 0; i < args.length; i++) {
            newAry.push(op_aref(context, args[i]));
        }

        return newAry;
    }

    @Deprecated
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        str = args[0].convertToString();
        return this;
    }

    @Deprecated
    public IRubyObject initialize_copy(IRubyObject other) {
        return initialize_copy(getRuntime().getCurrentContext(), other);
    }

    @Deprecated
    public IRubyObject concat(IRubyObject obj) {
        return concat(getRuntime().getCurrentContext(), obj);
    }

    @Deprecated
    public RubyFixnum pos() {
        return pos(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject set_pos(IRubyObject pos) {
        return set_pos(getRuntime().getCurrentContext(), pos);
    }

    @Deprecated
    public IRubyObject getch19(ThreadContext context) {
        return getch(context);
    }

    @Deprecated
    public IRubyObject reset() {
        return reset(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject unscan() {
        return unscan(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject matched_size() {
        return matched_size(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject bol_p() {
        return bol_p(getRuntime().getCurrentContext());
    }

    @Deprecated
    public RubyFixnum rest_size() {
        return rest_size(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject getchCommon(ThreadContext context, boolean is1_9) {
        return getchCommon(context);
    }

    /**
     * @deprecated Only defined for backward compatibility in CRuby.
     */
    @Deprecated
    @JRubyMethod(name = "must_C_version", meta = true)
    public static IRubyObject mustCversion(IRubyObject recv) {
        return recv;
    }
}

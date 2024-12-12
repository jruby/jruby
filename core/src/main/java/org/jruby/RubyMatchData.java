/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jcodings.Encoding;
import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Regex;
import org.joni.Region;
import org.joni.exception.JOniException;
import org.joni.exception.ValueException;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ByteListHolder;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * @author olabini
 */
@JRubyClass(name="MatchData")
public class RubyMatchData extends RubyObject {
    Region regs;        // captures
    int begin, end;     // begin and end are used when not groups defined
    RubyString str;     // source string
    private Object pattern; // Regex or (un-quoted) RubyString
    transient RubyRegexp regexp;
    private boolean charOffsetUpdated;
    private Region charOffsets;

    public static RubyClass createMatchDataClass(ThreadContext context, RubyClass Object) {
        return defineClass(context, "MatchData", Object, RubyMatchData::new).
                reifiedClass(RubyMatchData.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyMatchData.class)).
                classIndex(ClassIndex.MATCHDATA).
                defineMethods(context, RubyMatchData.class).
                tap(c -> c.getSingletonClass().undefMethods(context, "new", "allocate"));
    }

    public RubyMatchData(Ruby runtime) {
        super(runtime, runtime.getMatchData());
    }

    public RubyMatchData(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    final void initMatchData(RubyString str, Matcher matcher, Regex pattern) {

        // FIXME: This is pretty gross; we should have a cleaner initialization
        // that doesn't depend on package-visible fields and ideally is atomic,
        // probably using an immutable structure we replace all at once.

        // The region must be cloned because a subsequent match will update the
        // region, resulting in the MatchData created here pointing at the
        // incorrect region (capture/group).
        Region region = matcher.getRegion(); // lazy, null when no groups defined
        this.regs = region == null ? null : region.clone();
        this.begin = matcher.getBegin();
        this.end = matcher.getEnd();
        this.pattern = pattern;
        this.regexp = null;

        this.charOffsets = null;
        this.charOffsetUpdated = false;

        this.str = str.newFrozen();
    }

    final void initMatchData(RubyString str, int beg, RubyString pattern) {

        this.regs = null;
        this.begin = beg;
        this.end = beg + pattern.size();
        this.pattern = pattern.newFrozen();
        this.regexp = null;

        this.charOffsets = null;
        this.charOffsetUpdated = false;

        this.str = str.newFrozen();
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        RubyMatchData match = (RubyMatchData) clone;
        match.regs = this.regs;
        match.begin = this.begin;
        match.end = this.end;
        match.pattern = this.pattern;
        match.regexp = this.regexp;
        match.charOffsetUpdated = this.charOffsetUpdated;
        match.charOffsets = this.charOffsets;
        // match.str = this.str; // uninitialized MatchData?!?
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.MATCHDATA;
    }

    private static final class Pair implements Comparable<Pair> {
        int bytePos, charPos;
        @Override
        public int compareTo(Pair pair) {
            return bytePos - pair.bytePos;
        }
    }

    private static void updatePairs(ByteList value, Encoding encoding, Pair[] pairs) {
        Arrays.sort(pairs);

        byte[] bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int s = p;
        int c = 0;

        for (Pair pair : pairs) {
            int q = s + pair.bytePos;
            c += StringSupport.strLength(encoding, bytes, p, q);
            pair.charPos = c;
            p = q;
        }
    }

    private void updateCharOffsetOnlyOneReg(ByteList value, Encoding encoding) {
        if (charOffsetUpdated) return;

        if (charOffsets == null || charOffsets.getNumRegs() < 1) charOffsets = Region.newRegion(1);

        if (encoding.maxLength() == 1) {
            charOffsets.setBeg(0, begin);
            charOffsets.setEnd(0, end);
            charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[2];
        if (begin >= 0) {
            pairs[0] = new Pair();
            pairs[0].bytePos = begin;
            pairs[1] = new Pair();
            pairs[1].bytePos = end;
        }

        updatePairs(value, encoding, pairs);

        if (begin < 0) {
            charOffsets.setBeg(0, charOffsets.setEnd(0, -1));
            return;
        }
        Pair key = new Pair();
        key.bytePos = begin;
        charOffsets.setBeg(0, pairs[Arrays.binarySearch(pairs, key)].charPos);
        key.bytePos = end;
        charOffsets.setEnd(0, pairs[Arrays.binarySearch(pairs, key)].charPos);

        charOffsetUpdated = true;
    }

    private void updateCharOffsetManyRegs(ByteList value, Encoding encoding) {
        if (charOffsetUpdated) return;

        final Region regs = this.regs;
        int numRegs = regs.getNumRegs();

        if (charOffsets == null || charOffsets.getNumRegs() < numRegs) charOffsets = Region.newRegion(numRegs);

        if (encoding.maxLength() == 1) {
            for (int i = 0; i < numRegs; i++) {
                charOffsets.setBeg(i, regs.getBeg(i));
                charOffsets.setEnd(i, regs.getEnd(i));
            }
            charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[numRegs * 2];
        for (int i = 0; i < pairs.length; i++) pairs[i] = new Pair();

        int numPos = 0;
        for (int i = 0; i < numRegs; i++) {
            if (regs.getBeg(i) < 0) continue;
            pairs[numPos++].bytePos = regs.getBeg(i);
            pairs[numPos++].bytePos = regs.getEnd(i);
        }

        updatePairs(value, encoding, pairs);

        Pair key = new Pair();
        for (int i = 0; i < regs.getNumRegs(); i++) {
            if (regs.getBeg(i) < 0) {
                charOffsets.setBeg(i, charOffsets.setEnd(i, -1));
                continue;
            }
            key.bytePos = regs.getBeg(i);
            charOffsets.setBeg(i, pairs[Arrays.binarySearch(pairs, key)].charPos);
            key.bytePos = regs.getEnd(i);
            charOffsets.setEnd(i, pairs[Arrays.binarySearch(pairs, key)].charPos);
        }

        charOffsetUpdated = true;
    }

    private void updateCharOffset() {
        if (charOffsetUpdated) return;

        ByteList value = str.getByteList();
        Encoding enc = value.getEncoding();

        if (regs == null) {
            updateCharOffsetOnlyOneReg(value, enc);
        } else {
            updateCharOffsetManyRegs(value, enc);
        }

        charOffsetUpdated = true;
    }

    private static final int MATCH_BUSY = ObjectFlags.MATCH_BUSY;

    // rb_match_busy
    public final void use() {
        flags |= MATCH_BUSY;
    }

    public final boolean used() {
        return (flags & MATCH_BUSY) != 0;
    }

    final void check() {
        if (str == null) throw typeError(getRuntime().getCurrentContext(), "uninitialized Match");
    }

    final Regex getPattern() {
        final Object pattern = this.pattern;
        if (pattern instanceof Regex) return (Regex) pattern;
        if (pattern == null) throw typeError(getRuntime().getCurrentContext(), "uninitialized Match (missing pattern)");
        // when a regexp is avoided for matching we lazily instantiate one from the unquoted string :
        Regex regexPattern = RubyRegexp.getQuotedRegexpFromCache(metaClass.runtime, (RubyString) pattern, RegexpOptions.NULL_OPTIONS);
        this.pattern = regexPattern;
        return regexPattern;
    }

    private RubyRegexp getRegexp() {
        RubyRegexp regexp = this.regexp;
        if (regexp != null) return regexp;
        final Regex pattern = getPattern();
        return this.regexp = RubyRegexp.newRegexp(metaClass.runtime, (ByteList) pattern.getUserObject(), pattern);
    }

    private RubyArray<?> match_array(ThreadContext context, int start) {
        check();

        if (regs == null) {
            if (start != 0) return newEmptyArray(context);
            return begin == -1 ?
                    newArray(context, context.nil) :
                    newArray(context, str.makeSharedString(context.runtime, begin, end - begin));
        }

        int count = regs.getNumRegs() - start;
        var arr = newRawArray(context, count);
        for (int i=0; i < count; i++) {
            int beg = regs.getBeg(i+start);
            arr.storeInternal(context, i, beg == -1 ?
                    context.nil :
                    str.makeSharedString(context.runtime, beg, regs.getEnd(i+start) - beg));
        }
        return arr.finishRawArray(context);
    }

    public IRubyObject group(long n) {
        return RubyRegexp.nth_match((int)n, this);
    }

    public IRubyObject group(int n) {
        return RubyRegexp.nth_match(n, this);
    }

    public int getNameToBackrefNumber(String name) {
        try {
            byte[] bytes = name.getBytes();
            return getPattern().nameToBackrefNumber(bytes, 0, bytes.length, regs);
        } catch (JOniException je) {
            throw metaClass.runtime.newIndexError(je.getMessage());
        }
    }

    // This returns a list of values in the order the names are defined (named capture local var
    // feature uses this).
    public IRubyObject[] getNamedBackrefValues(Ruby runtime) {
        final Regex pattern = getPattern();
        if (pattern.numberOfNames() == 0) return NULL_ARRAY;

        IRubyObject[] values = new IRubyObject[pattern.numberOfNames()];

        int j = 0;
        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();

            int nth = pattern.nameToBackrefNumber(e.name, e.nameP, e.nameEnd, regs);
            values[j++] = RubyRegexp.nth_match(nth, this);
        }

        return values;
    }

    @JRubyMethod
    public IRubyObject byteoffset(ThreadContext context, IRubyObject group) {
        int index = backrefNumber(context.runtime, group);
        Region regs = this.regs;

        backrefNumberCheck(context, index);

        int start = regs.getBeg(index);
        return start < 0 ?
                newArray(context, context.nil, context.nil) :
                newArray(context, asFixnum(context, start), asFixnum(context, regs.getEnd(index)));
    }

    @JRubyMethod
    @Override
    public RubyString inspect(ThreadContext context) {
        if (str == null) return (RubyString) anyToString();

        RubyString result = newString(context, "#<");
        result.append(getMetaClass().getRealClass().to_s(context));

        NameEntry[] names = new NameEntry[regs == null ? 1 : regs.getNumRegs()];

        final Regex pattern = getPattern();
        for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
            NameEntry e = i.next();
            for (int num : e.getBackRefs()) names[num] = e;
        }

        for (int i = 0; i < names.length; i++) {
            result.cat((byte)' ');
            if (i > 0) {
                NameEntry e = names[i];
                if (e != null) {
                    result.cat(e.name, e.nameP, e.nameEnd - e.nameP);
                } else {
                    result.cat((byte)('0' + i));
                }
                result.cat((byte)':');
            }
            IRubyObject v = RubyRegexp.nth_match(i, this);
            if (v.isNil()) {
                result.cat(RubyNil.nilBytes); // "nil"
            } else {
                result.append(v.inspect(context));
            }
        }

        return result.cat((byte)'>');
    }

    @JRubyMethod
    public RubyRegexp regexp(ThreadContext context, Block block) {
        check();
        return getRegexp();
    }

    @JRubyMethod
    public IRubyObject names(ThreadContext context, Block block) {
        check();
        return getRegexp().names(context);
    }

    /** match_to_a
     *
     */
    @JRubyMethod
    @Override
    public RubyArray to_a(ThreadContext context) {
        return match_array(context, 0);
    }

    @JRubyMethod(rest = true)
    public IRubyObject values_at(ThreadContext context, IRubyObject[] args) {
        check();

        var result = newRawArray(context, args.length);

        for (IRubyObject arg : args) {
            if (arg instanceof RubyFixnum) {
                result.append(context, RubyRegexp.nth_match(arg.convertToInteger().getIntValue(), this));
            } else {
                int num = namevToBackrefNumber(context, arg);
                if (num >= 0) {
                    result.append(context, RubyRegexp.nth_match(num, this));
                } else {
                    matchAryAref(context, arg, result);
                }
            }
        }

        return result.finishRawArray(context);
    }

    public IRubyObject values_at(IRubyObject[] args) {
        return values_at(metaClass.runtime.getCurrentContext(), args);
    }

    /** match_captures
     *
     */
    @JRubyMethod
    public IRubyObject captures(ThreadContext context) {
        return match_array(context, 1);
    }

    private int nameToBackrefNumber(RubyString str) {
        check();
        return nameToBackrefNumber(metaClass.runtime, getPattern(), regs, str);
    }

    private static int nameToBackrefNumber(Ruby runtime, Regex pattern, Region regs, ByteListHolder str) {
        assert pattern != null;
        ByteList value = str.getByteList();
        try {
            return pattern.nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), regs);
        } catch (JOniException je) {
            if (je instanceof ValueException) {
                throw runtime.newIndexError(str(runtime, "undefined group name reference: ", runtime.newString(value)));
            }
            // FIXME: I think we could only catch ValueException here, but someone needs to audit that.
            throw runtime.newIndexError(je.getMessage());
        }
    }

    private static int nameToBackrefNumber(Regex pattern, Region regs, ByteList name) {
        try {
            return pattern.nameToBackrefNumber(name.getUnsafeBytes(), name.getBegin(), name.getBegin() + name.getRealSize(), regs);
        } catch (JOniException je) {
            return -1;
        }
    }

    public final int backrefNumber(Ruby runtime, IRubyObject obj) {
        check();
        return backrefNumber(runtime, getPattern(), regs, obj);
    }

    public static int backrefNumber(Ruby runtime, Regex pattern, Region regs, IRubyObject obj) {
        if (obj instanceof RubySymbol sym) {
            return nameToBackrefNumber(runtime, pattern, regs, (RubyString) sym.to_s(runtime.getCurrentContext()));
        }
        if (obj instanceof RubyString str) return nameToBackrefNumber(runtime, pattern, regs, str);
        return RubyNumeric.num2int(obj);
    }

    // MRI: namev_to_backref_number
    private int namevToBackrefNumber(ThreadContext context, IRubyObject name) {
        int num = -1;

        switch (name.getType().getClassIndex()) {
            case SYMBOL:
                name = name.asString();
	            /* fall through */
            case STRING:
                Ruby runtime = context.runtime;
                if (regexp.isNil() || RubyEncoding.areCompatible(regexp, name) == null ||
                        (num = nameToBackrefNumber(runtime, regexp.getPattern(), regs, name.convertToString())) < 1) {
                    nameToBackrefError(runtime, name.toString());
                }
                return num;

            default:
                return -1;
        }
    }

    private int nameToBackrefError(Ruby runtime, String name) {
        throw runtime.newIndexError("undefined group name reference " + name);
    }

    // MRI: match_ary_subseq
    private IRubyObject matchArySubseq(ThreadContext context, int beg, int len, RubyArray result) {
        assert result != null;

        int olen = regs.getNumRegs();
        int wantedEnd = beg + len;
        int j, end = Math.min(olen, wantedEnd);

        if (len == 0) return result;

        for (j = beg; j < end; j++) {
            result.append(context, RubyRegexp.nth_match(j, this));
        }

        // if not enough groups, force length to be as wide as desired by setting last value to nil
        if (wantedEnd > j) {
            int newLength = result.size() + wantedEnd - j;
            result.storeInternal(context, newLength - 1, context.nil);
        }

        return result;
    }

    // MRI: match_ary_aref
    private IRubyObject matchAryAref(ThreadContext context, IRubyObject index, RubyArray result) {
        int[] begLen = new int[2];
        int numRegs = regs.getNumRegs();

        /* check if idx is Range */
        IRubyObject isRange = RubyRange.rangeBeginLength(context, index, numRegs, begLen, 1);

        if (isRange.isNil()) return context.nil;

        if (!isRange.isTrue()) {
            IRubyObject nthMatch = RubyRegexp.nth_match(index.convertToInteger().getIntValue(), this);

            // this should never happen here, but MRI allows any VALUE for result
            // if (result.isNil()) return nthMatch;

            return result.push(context, nthMatch);
        }

        return matchArySubseq(context, begLen[0], begLen[1], result);
    }

    /** match_aref
     *
     */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject idx) {
        check();
        IRubyObject result = op_arefCommon(context, idx);
        return result == null ? to_a(context).aref(context, idx) : result;
    }

    /** match_aref
    *
    */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject idx, IRubyObject rest) {
        IRubyObject result;
        return !rest.isNil() || (result = op_arefCommon(context, idx)) == null ?
                to_a(context).aref(context, idx, rest) : result;
    }

    private IRubyObject op_arefCommon(ThreadContext context, IRubyObject idx) {
        if (idx instanceof RubyFixnum) {
            int num = RubyNumeric.fix2int(idx);
            if (num >= 0) return RubyRegexp.nth_match(num, this);
        } else {
            if (idx instanceof RubySymbol) {
                return RubyRegexp.nth_match(nameToBackrefNumber((RubyString) ((RubySymbol) idx).to_s(context)), this);
            }
            if (idx instanceof RubyString) {
                return RubyRegexp.nth_match(nameToBackrefNumber((RubyString) idx), this);
            }
        }
        return null;
    }

    public final IRubyObject at(final int nth) {
        return RubyRegexp.nth_match(nth, this);
    }

    /** match_size
     *
     */
    @JRubyMethod(name = {"size", "length"})
    public IRubyObject size(ThreadContext context) {
        check();
        return regs == null ? RubyFixnum.one(context.runtime) : asFixnum(context, regs.getNumRegs());
    }

    /**
     * MRI: match_begin
     */
    @JRubyMethod
    public IRubyObject begin(ThreadContext context, IRubyObject index) {
        check();
        final int i = backrefNumber(context.runtime, index);

        backrefNumberCheck(context, i);

        int b = regs == null ? begin : regs.getBeg(i);
        if (b < 0) return context.nil;

        updateCharOffset();

        return asFixnum(context, charOffsets.getBeg(i));
    }

    /** match_end
     *
     */
    @JRubyMethod
    public IRubyObject end(ThreadContext context, IRubyObject index) {
        check();

        final int i = backrefNumber(context.runtime, index);

        backrefNumberCheck(context, i);

        int e = regs == null ? end : regs.getEnd(i);
        if (e < 0) return context.nil;

        if ( ! str.singleByteOptimizable() ) {
            updateCharOffset();
            e = charOffsets.getEnd(i);
        }

        return asFixnum(context, e);
    }

    @Deprecated
    public IRubyObject offset19(ThreadContext context, IRubyObject index) {
        return offset(context, index);
    }

    /** match_offset
     *
     */
    @JRubyMethod(name = "offset")
    public IRubyObject offset(ThreadContext context, IRubyObject index) {
        check();

        final int i = backrefNumber(context.runtime, index);
        backrefNumberCheck(context, i);

        int b, e;
        if (regs == null) {
            b = begin;
            e = end;
        } else {
            b = regs.getBeg(i);
            e = regs.getEnd(i);
        }

        if (b < 0) return newArray(context, context.nil, context.nil);

        if ( ! str.singleByteOptimizable() ) {
            updateCharOffset();
            b = charOffsets.getBeg(i);
            e = charOffsets.getEnd(i);
        }

        return newArray(context, asFixnum(context, b), asFixnum(context, e));
    }

    /** match_pre_match
     *
     */
    @JRubyMethod
    public IRubyObject pre_match(ThreadContext context) {
        check();
        if (begin == -1) return context.nil;

        return str.makeSharedString(context.runtime, 0, begin);
    }

    @JRubyMethod
    public IRubyObject match(ThreadContext context, IRubyObject nth) {
        int index = nthToIndex(context, nth);
        Region regs = this.regs;

        backrefNumberCheck(context, index);

        int start = regs.getBeg(index);

        if (start < 0) return context.nil;

        int end = regs.getEnd(index);

        return str.makeSharedString(context.runtime, start, end - start);
    }

    @JRubyMethod
    public IRubyObject match_length(ThreadContext context, IRubyObject nth) {
        int index = nthToIndex(context, nth);
        Region regs = this.regs;

        backrefNumberCheck(context, index);

        int start = regs.getBeg(index);
        if (start < 0) return context.nil;

        int end = regs.getEnd(index);
        ByteList strBytes = str.getByteList();

        int length = StringSupport.strLength(
                strBytes.getEncoding(),
                strBytes.unsafeBytes(),
                start,
                end,
                str.getCodeRange());

        return asFixnum(context, length);
    }

    private int nthToIndex(ThreadContext context, IRubyObject id) {
        int index = namevToBackrefNumber(context, id);

        if (index == -1 && id instanceof RubyInteger) {
            index = id.convertToInteger().getIntValue();
        }
        return index;
    }

    private void backrefNumberCheck(ThreadContext context, int i) {
        if (i < 0 || (regs == null ? 1 : regs.getNumRegs()) <= i) {
            throw context.runtime.newIndexError("index " + i + " out of matches");
        }
    }

    /** match_post_match
     *
     */
    @JRubyMethod
    public IRubyObject post_match(ThreadContext context) {
        check();
        if (begin == -1) return context.nil;

        final int strLen = str.getByteList().length();
        return str.makeSharedString(context.runtime, end, strLen - end);
    }

    /** match_to_s
     *
     */
    @JRubyMethod
    @Override
    public IRubyObject to_s(ThreadContext context) {
        check();
        IRubyObject ss = RubyRegexp.last_match(this);
        return ss.isNil() ? newEmptyString(context) : ss;
    }

    /** match_string
     *
     */
    @JRubyMethod
    public IRubyObject string() {
        check();
        return str; //str is frozen
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject original) {
        if (this == original) return this;

        checkFrozen();
        if (!(original instanceof RubyMatchData orig)) throw typeError(context, "wrong argument class");

        str = orig.str;
        regs = orig.regs;

        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RubyMatchData that)) return false;

        return (this.str == that.str || (this.str != null && this.str.equals(that.str))) &&
               (this.regexp == that.regexp || (this.getRegexp().equals(that.getRegexp()))) &&
               (this.charOffsets == that.charOffsets || (this.charOffsets != null && this.charOffsets.equals(that.charOffsets))) &&
               this.charOffsetUpdated == that.charOffsetUpdated &&
               this.begin == that.begin && this.end == that.end;
    }

    @JRubyMethod(name = {"eql?", "=="})
    public IRubyObject eql_p(ThreadContext context, IRubyObject obj) {
        return equals(obj) ? context.tru : context.fals;
    }

    @Override
    public int hashCode() {
        check();
        return getPattern().hashCode() ^ str.hashCode();
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashCode());
    }

    @JRubyMethod(keywords = true, optional = 1)
    public RubyHash named_captures(ThreadContext context, IRubyObject[] args) {
        check();

        int argc = Arity.checkArgumentCount(context, args.length, 0, 0, true);
        RubyHash hash = newSmallHash(context);
        if (regexp == context.nil) return hash;

        final boolean symbolizeNames;
        if (argc == 1) {
            if (!(args[0] instanceof RubyHash)) throw argumentError(context, 1, 0);
            IRubyObject value = ArgsUtil.extractKeywordArg(context, (RubyHash) args[0], "symbolize_names");
            symbolizeNames = value != null && value.isTrue();
        } else {
            symbolizeNames = false;
        }

        getNamedBackrefKeys(context).forEach(entry -> {
            IRubyObject key = symbolizeNames ? symbolFromNameEntry(context, entry) : stringFromNameEntry(context, entry);
            boolean found = false;

            for (int b : entry.getBackRefs()) {
                IRubyObject value = RubyRegexp.nth_match(b, this);

                if (value.isTrue()) {
                    hash.op_aset(context, key, value);
                    found = true;
                }
            }

            if (!found) hash.op_aset(context, key, context.nil);
        });

        return hash;
    }

    @JRubyMethod
    public IRubyObject deconstruct(ThreadContext context) {
        return match_array(context, 1);
    }

    @JRubyMethod
    public IRubyObject deconstruct_keys(ThreadContext context, IRubyObject what) {
        RubyHash hash = newSmallHash(context);

        if (what.isNil()) {
            getNamedBackrefKeys(context).forEach(entry -> {
                RubySymbol key = symbolFromNameEntry(context, entry);

                for (int b : entry.getBackRefs()) {
                    IRubyObject value = RubyRegexp.nth_match(b, this);
                    if (value.isTrue()) hash.op_aset(context, key, value);
                }
            });
        } else if (what instanceof RubyArray arr) {
            if (getPattern().numberOfNames() < arr.size()) return hash;

            Iterable<IRubyObject> iterable = () -> arr.rubyStream().iterator();
            for (IRubyObject obj : iterable) {
                if (!(obj instanceof RubySymbol requestedKey)) {
                    throw typeError(context, str(context.runtime, "wrong argument type ", obj.getMetaClass(), " (expected Symbol)"));
                }

                int index = nameToBackrefNumber(getPattern(), regs, requestedKey.getBytes());
                if (index == -1) break;

                IRubyObject value = RubyRegexp.nth_match(index, this);
                if (!value.isTrue()) break;

                hash.op_aset(context, requestedKey, value);
            }
        } else {
            throw typeError(context, str(context.runtime, "wrong argument type ", what.getMetaClass(), " (expected Array)"));
        }
        return hash;
    }

    private Stream<NameEntry> getNamedBackrefKeys(ThreadContext context) {
        Iterable<NameEntry> iterable = () -> getPattern().namedBackrefIterator();
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private static ByteList byteListFromNameEntry(ThreadContext context, NameEntry entry, Encoding encoding) {
        return new ByteList(entry.name, entry.nameP, entry.nameEnd - entry.nameP, encoding, false);
    }

    private RubySymbol symbolFromNameEntry(ThreadContext context, NameEntry entry) {
        return asSymbol(context, byteListFromNameEntry(context, entry, regexp.getEncoding()));
    }

    private RubyString stringFromNameEntry(ThreadContext context, NameEntry entry) {
        return RubyString.newStringShared(context.runtime, byteListFromNameEntry(context, entry, regexp.getEncoding()));
    }

    /**
     * Get the begin offset of the given region, or -1 if the region does not exist.
     *
     * @param i the region for which to fetch the begin offset
     * @return the begin offset for the region
     */
    public int begin(int i) {
        if (regs == null) {
            if (i > 1) return -1;
            return begin;
        }
        if (i > regs.getNumRegs()) return -1;
        return regs.getBeg(i);
    }

    /**
     * Get the end offset of the given region, or -1 if the region does not exist.
     *
     * @param i the region for which to fetch the end offset
     * @return the end offset for the region
     */
    public int end(int i) {
        if (regs == null) {
            if (i > 1) return -1;
            return end;
        }
        if (i > regs.getNumRegs()) return -1;
        return regs.getEnd(i);
    }

    /**
     * Fetch the number of regions in this match.
     *
     * @return the number of regions in this match
     */
    public int numRegs() {
        return regs == null ? 1 : regs.getNumRegs();
    }

    @Deprecated
    @Override
    public RubyArray to_a() {
        return match_array(getRuntime().getCurrentContext(), 0);
    }

    @Deprecated
    public IRubyObject op_aref(IRubyObject idx) {
        return op_aref(getRuntime().getCurrentContext(), idx);
    }

    @Deprecated
    public IRubyObject op_aref(IRubyObject idx, IRubyObject rest) {
        return op_aref(getRuntime().getCurrentContext(), idx, rest);
    }

}

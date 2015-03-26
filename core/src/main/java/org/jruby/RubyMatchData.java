/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import org.jcodings.Encoding;
import org.joni.NameEntry;
import org.joni.Regex;
import org.joni.Region;
import org.joni.exception.JOniException;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * @author olabini
 */
@JRubyClass(name="MatchData")
public class RubyMatchData extends RubyObject {
    Region regs;        // captures
    int begin, end;     // begin and end are used when not groups defined
    RubyString str;     // source string
    Regex pattern;
    RubyRegexp regexp;
    boolean charOffsetUpdated;
    Region charOffsets;

    public static RubyClass createMatchDataClass(Ruby runtime) {
        RubyClass matchDataClass = runtime.defineClass("MatchData", runtime.getObject(), MATCH_DATA_ALLOCATOR);
        runtime.setMatchData(matchDataClass);

        matchDataClass.setClassIndex(ClassIndex.MATCHDATA);
        matchDataClass.setReifiedClass(RubyMatchData.class);
        
        runtime.defineGlobalConstant("MatchingData", matchDataClass);
        matchDataClass.kindOf = new RubyModule.JavaClassKindOf(RubyMatchData.class);

        matchDataClass.getMetaClass().undefineMethod("new");
        matchDataClass.defineAnnotatedMethods(RubyMatchData.class);
        return matchDataClass;
    }

    private static ObjectAllocator MATCH_DATA_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyMatchData(runtime, klass);
        }
    };

    public RubyMatchData(Ruby runtime) {
        super(runtime, runtime.getMatchData());
    }

    public RubyMatchData(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        RubyMatchData match = (RubyMatchData)clone;
        match.regs = regs;
        match.begin = begin;
        match.end = end;
        match.pattern = pattern;
        match.regexp = regexp;
        match.charOffsetUpdated = charOffsetUpdated;
        match.charOffsets = charOffsets;
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

    private void updatePairs(ByteList value, Encoding encoding, Pair[] pairs) {
        Arrays.sort(pairs);

        int length = pairs.length;
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int s = p;
        int c = 0;
        
        for (int i = 0; i < length; i++) {
            int q = s + pairs[i].bytePos;
            c += StringSupport.strLength(encoding, bytes, p, q);
            pairs[i].charPos = c;
            p = q;
        }
    }

    private void updateCharOffsetOnlyOneReg(ByteList value, Encoding encoding) {
        if (charOffsetUpdated) return;

        if (charOffsets == null || charOffsets.numRegs < 1) charOffsets = new Region(1);
        
        if (encoding.maxLength() == 1) {
            charOffsets.beg[0] = begin;
            charOffsets.end[0] = end;
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
            charOffsets.beg[0] = charOffsets.end[0] = -1;
            return;
        }
        Pair key = new Pair();
        key.bytePos = begin;
        charOffsets.beg[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        key.bytePos = end;
        charOffsets.end[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;

        charOffsetUpdated = true;
    }

    private void updateCharOffsetManyRegs(ByteList value, Encoding encoding) {
        if (charOffsetUpdated) return;

        final Region regs = this.regs;
        int numRegs = regs.numRegs;

        if (charOffsets == null || charOffsets.numRegs < numRegs) charOffsets = new Region(numRegs);
        
        if (encoding.maxLength() == 1) {
            for (int i = 0; i < numRegs; i++) {
                charOffsets.beg[i] = regs.beg[i];
                charOffsets.end[i] = regs.end[i];
            }
            charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[numRegs * 2];
        for (int i = 0; i < pairs.length; i++) pairs[i] = new Pair();

        int numPos = 0;
        for (int i = 0; i < numRegs; i++) {
            if (regs.beg[i] < 0) continue;
            pairs[numPos++].bytePos = regs.beg[i];
            pairs[numPos++].bytePos = regs.end[i];
        }

        updatePairs(value, encoding, pairs);

        Pair key = new Pair();
        for (int i = 0; i < regs.numRegs; i++) {
            if (regs.beg[i] < 0) {
                charOffsets.beg[i] = charOffsets.end[i] = -1;
                continue;
            }
            key.bytePos = regs.beg[i];
            charOffsets.beg[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
            key.bytePos = regs.end[i];
            charOffsets.end[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
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

    private static final int MATCH_BUSY = USER2_F;

    // rb_match_busy
    public final void use() {
        flags |= MATCH_BUSY; 
    }

    public final boolean used() {
        return (flags & MATCH_BUSY) != 0;
    }

    void check() {
        if (str == null) throw getRuntime().newTypeError("uninitialized Match");
    }

    private void checkLazyRegexp() {
        if (regexp == null) regexp = RubyRegexp.newRegexp(getRuntime(), (ByteList)pattern.getUserObject(), pattern);
    }
    
    private RubyString makeShared(Ruby runtime, RubyString str, int begin, int length) {
        return str.makeShared19(runtime, begin, length);
    }

    private RubyArray match_array(Ruby runtime, int start) {
        check();
        if (regs == null) {
            if (start != 0) return runtime.newEmptyArray();
            if (begin == -1) {
                return runtime.newArray(runtime.getNil());
            } else {
                RubyString ss = makeShared(runtime, str, begin, end - begin);
                if (isTaint()) ss.setTaint(true);
                return runtime.newArray(ss);
            }
        } else {
            RubyArray arr = runtime.newArray(regs.numRegs - start);
            for (int i=start; i<regs.numRegs; i++) {
                if (regs.beg[i] == -1) {
                    arr.append(runtime.getNil());
                } else {
                    RubyString ss = makeShared(runtime, str, regs.beg[i], regs.end[i] - regs.beg[i]);                   
                    if (isTaint()) ss.setTaint(true); 
                    arr.append(ss);
                }
            }
            return arr;
        }
        
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
            return pattern.nameToBackrefNumber(bytes, 0, bytes.length, regs);
        } catch (JOniException je) {
            throw getRuntime().newIndexError(je.getMessage());
        }
    }

    // This returns a list of values in the order the names are defined (named capture local var
    // feature uses this).
    public IRubyObject[] getNamedBackrefValues(Ruby runtime) {
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
    @Override
    public IRubyObject inspect() {
        if (str == null) return anyToString();

        Ruby runtime = getRuntime();
        RubyString result = runtime.newString();
        result.cat((byte)'#').cat((byte)'<');
        result.append(getMetaClass().getRealClass().to_s());

        NameEntry[]names = new NameEntry[regs == null ? 1 : regs.numRegs];

        if (pattern.numberOfNames() > 0) {
            for (Iterator<NameEntry> i = pattern.namedBackrefIterator(); i.hasNext();) {
                NameEntry e = i.next();
                for (int num : e.getBackRefs()) names[num] = e;
            }
        }

        for (int i=0; i<names.length; i++) {
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
                result.cat("nil".getBytes());
            } else {
                result.append(((RubyString)v).inspect19());
            }
        }

        return result.cat((byte)'>');
    }

    @JRubyMethod
    public IRubyObject regexp(ThreadContext context, Block block) {
        check();
        checkLazyRegexp();
        return regexp;
    }

    @JRubyMethod
    public IRubyObject names(ThreadContext context, Block block) {
        check();
        checkLazyRegexp();
        return regexp.names(context);
    }

    /** match_to_a
     *
     */
    @JRubyMethod
    @Override
    public RubyArray to_a() {
        return match_array(getRuntime(), 0);
    }

    @JRubyMethod(rest = true)
    public IRubyObject values_at(IRubyObject[] args) {
        return to_a().values_at(args);
    }

    /** match_captures
     *
     */
    @JRubyMethod
    public IRubyObject captures(ThreadContext context) {
        return match_array(context.runtime, 1);
    }

    private int nameToBackrefNumber(RubyString str) {
        ByteList value = str.getByteList();
        try {
            return pattern.nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), regs);
        } catch (JOniException je) {
            throw getRuntime().newIndexError(je.getMessage());
        }
    }

    public final int backrefNumber(IRubyObject obj) {
        check();
        if (obj instanceof RubySymbol) {
            return nameToBackrefNumber((RubyString)((RubySymbol)obj).id2name());
        } else if (obj instanceof RubyString) {
            return nameToBackrefNumber((RubyString)obj);
        } else {
            return RubyNumeric.num2int(obj);
        }
    }

    /** match_aref
    *
    */
    public IRubyObject op_aref(IRubyObject idx) {
        return op_aref19(idx);
    }

    /** match_aref
     *
     */
    public IRubyObject op_aref(IRubyObject idx, IRubyObject rest) {
        return op_aref19(idx, rest);
    }

    /** match_aref
     *
     */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref19(IRubyObject idx) {
        check();
        IRubyObject result = op_arefCommon(idx);
        return result == null ? ((RubyArray)to_a()).aref19(idx) : result;
    }

    /** match_aref
    *
    */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref19(IRubyObject idx, IRubyObject rest) {
        IRubyObject result;
        return !rest.isNil() || (result = op_arefCommon(idx)) == null ? ((RubyArray)to_a()).aref19(idx, rest) : result;
    }

    private IRubyObject op_arefCommon(IRubyObject idx) {
        if (idx instanceof RubyFixnum) {
            int num = RubyNumeric.fix2int(idx);
            if (num >= 0) return RubyRegexp.nth_match(num, this);
        } else {
            if (idx instanceof RubySymbol) {
                return RubyRegexp.nth_match(nameToBackrefNumber((RubyString)((RubySymbol)idx).id2name()), this);
            } else if (idx instanceof RubyString) {
                return RubyRegexp.nth_match(nameToBackrefNumber((RubyString)idx), this);
            }
        }
        return null;
    }

    /** match_size
     *
     */
    @JRubyMethod(name = {"size", "length"})
    public IRubyObject size(ThreadContext context) {
        check();
        Ruby runtime = context.runtime;
        return regs == null ? RubyFixnum.one(runtime) : RubyFixnum.newFixnum(runtime, regs.numRegs);
    }

    /**
     * MRI: match_begin
     */
    @JRubyMethod
    public IRubyObject begin(ThreadContext context, IRubyObject index) {
        Ruby runtime = context.runtime;

        int i = backrefNumber(index);

        check();
        if (i < 0 || (regs == null ? 1 : regs.numRegs) <= i) {
            throw runtime.newIndexError("index " + i + " out of matches");
        }

        int b = regs == null ? begin : regs.beg[i];

        if (b < 0) return context.nil;

        updateCharOffset();

        return RubyFixnum.newFixnum(runtime, charOffsets.beg[i]);
    }

    /** match_end
     *
     */
    @JRubyMethod
    public IRubyObject end(ThreadContext context, IRubyObject index) {
        check();

        int i = backrefNumber(index);
        Ruby runtime = context.runtime;

        if (i < 0 || (regs == null ? 1 : regs.numRegs) <= i) throw runtime.newIndexError("index " + i + " out of matches");

        int e = regs == null ? end : regs.end[i];

        if (e < 0) return runtime.getNil();

        if (!str.singleByteOptimizable()) {
            updateCharOffset();
            e = charOffsets.end[i];
        }

        return RubyFixnum.newFixnum(runtime, e);
    }

    /** match_offset
     *
     */
    public IRubyObject offset(ThreadContext context, IRubyObject index) {
        return offset19(context, index);
    }

    @JRubyMethod(name = "offset")
    public IRubyObject offset19(ThreadContext context, IRubyObject index) {
        return offsetCommon(context, backrefNumber(index), true);
    }

    private IRubyObject offsetCommon(ThreadContext context, int i, boolean is_19) {
        check();
        Ruby runtime = context.runtime;
        if (i < 0 || (regs == null ? 1 : regs.numRegs) <= i) throw runtime.newIndexError("index " + i + " out of matches");
        int b, e;
        if (regs == null) {
            b = begin;
            e = end;
        } else {
            b = regs.beg[i];
            e = regs.end[i];
        }
        if (b < 0) return runtime.newArray(runtime.getNil(), runtime.getNil());
        if (is_19 && !str.singleByteOptimizable()) {
            updateCharOffset();
            b = charOffsets.beg[i];
            e = charOffsets.end[i];
        }
        return runtime.newArray(RubyFixnum.newFixnum(runtime, b), RubyFixnum.newFixnum(runtime, e));
    }

    /** match_pre_match
     *
     */
    @JRubyMethod
    public IRubyObject pre_match(ThreadContext context) {
        check();
        if (begin == -1) {
            return context.runtime.getNil();
        }
        return makeShared(context.runtime, str, 0, begin).infectBy(this);
    }

    /** match_post_match
     *
     */
    @JRubyMethod
    public IRubyObject post_match(ThreadContext context) {
        check();
        if (begin == -1) {
            return context.runtime.getNil();
        }
        return makeShared(context.runtime, str, end, str.getByteList().length() - end).infectBy(this);
    }

    /** match_to_s
     *
     */
    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        check();
        IRubyObject ss = RubyRegexp.last_match(this);
        if (ss.isNil()) ss = RubyString.newEmptyString(getRuntime());
        if (isTaint()) ss.setTaint(true);
        return ss;
    }

    /** match_string
     *
     */
    @JRubyMethod
    public IRubyObject string() {
        check();
        return str; //str is frozen
    }

    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        if (this == original) return this;

        checkFrozen();

        if (!(original instanceof RubyMatchData)) {
            throw getRuntime().newTypeError("wrong argument class");
        }

        RubyMatchData origMatchData = (RubyMatchData)original;
        str = origMatchData.str;
        regs = origMatchData.regs;

        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RubyMatchData)) return false;

        RubyMatchData match = (RubyMatchData)other;

        return (this.str == match.str || (this.str != null && this.str.equals(match.str))) &&
                (this.regexp == match.regexp || (this.regexp != null && this.regexp.equals(match.regexp))) &&
                (this.charOffsets == match.charOffsets || (this.charOffsets != null && this.charOffsets.equals(match.charOffsets))) &&
                this.begin == match.begin &&
                this.end == match.end &&
                this.charOffsetUpdated == match.charOffsetUpdated;
    }

    @JRubyMethod(name = {"eql?", "=="}, required = 1)
    @Override
    public IRubyObject eql_p(IRubyObject obj) {
        return getRuntime().newBoolean(equals(obj));
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        check();
        return getRuntime().newFixnum(pattern.hashCode() ^ str.hashCode());
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
        if (i > regs.numRegs) return -1;
        return regs.beg[i];
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
        if (i > regs.numRegs) return -1;
        return regs.end[i];
    }

    /**
     * Fetch the number of regions in this match.
     *
     * @return the number of regions in this match
     */
    public int numRegs() {
        return regs == null ? 1 : regs.numRegs;
    }

}

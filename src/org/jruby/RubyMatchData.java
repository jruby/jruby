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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.util.Iterator;

import org.joni.NameEntry;
import org.joni.Regex;
import org.joni.Region;
import org.joni.exception.JOniException;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author olabini
 */
@JRubyClass(name="MatchData")
public class RubyMatchData extends RubyObject {
    Region regs;        // captures
    int begin;          // begin and end are used when not groups defined
    int end;
    RubyString str;
    RubyRegexp regexp;
    
    public static RubyClass createMatchDataClass(Ruby runtime) {
        RubyClass matchDataClass = runtime.defineClass("MatchData", runtime.getObject(), MATCH_DATA_ALLOCATOR);
        runtime.setMatchData(matchDataClass);
        runtime.defineGlobalConstant("MatchingData", matchDataClass);
        matchDataClass.kindOf = new RubyModule.KindOf() {
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyMatchData;
            }
        };

        matchDataClass.getMetaClass().undefineMethod("new");
        matchDataClass.defineAnnotatedMethods(RubyMatchData.class);
        return matchDataClass;
    }

    private static ObjectAllocator MATCH_DATA_ALLOCATOR = new ObjectAllocator() {
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

    public final static int MATCH_BUSY = USER2_F;

    // rb_match_busy
    public final void use() {
        flags |= MATCH_BUSY; 
    }

    public final boolean used() {
        return (flags & MATCH_BUSY) != 0;
    }

    private void check() {
        if (regexp == null) throw getRuntime().newTypeError("uninitialized Match");
    }

    private RubyArray match_array(Ruby runtime, int start) {
        check();
        if (regs == null) {
            if (start != 0) return runtime.newEmptyArray();
            if (begin == -1) {
                return getRuntime().newArray(runtime.getNil());
            } else {
                RubyString ss = str.makeShared(runtime, begin, end - begin);
                if (isTaint()) ss.setTaint(true);
                return getRuntime().newArray(ss);
            }
        } else {
            RubyArray arr = getRuntime().newArray(regs.numRegs - start);
            for (int i=start; i<regs.numRegs; i++) {
                if (regs.beg[i] == -1) {
                    arr.append(getRuntime().getNil());
                } else {
                    RubyString ss = str.makeShared(runtime, regs.beg[i], regs.end[i] - regs.beg[i]);
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

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect() {
        if (regexp == null) return anyToString();

        RubyString result = getRuntime().newString();
        result.cat((byte)'#').cat((byte)'<');
        result.append(getMetaClass().getRealClass().to_s());

        NameEntry[]names = new NameEntry[regs == null ? 1 : regs.numRegs];

        Regex pattern = regexp.getPattern();
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
                result.append(v.inspect());
            }
        }

        return result.cat((byte)'>');
    }

    @JRubyMethod(name = "regexp", compat = CompatVersion.RUBY1_9)
    public IRubyObject regexp(ThreadContext context, Block block) {
        check();
        return regexp;
    }

    @JRubyMethod(name = "names", compat = CompatVersion.RUBY1_9)
    public IRubyObject names(ThreadContext context, Block block) {
        check();
        return regexp.names(context);
    }

    /** match_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    @Override
    public RubyArray to_a() {
        return match_array(getRuntime(), 0);
    }

    @JRubyMethod(name = "values_at", required = 1, rest = true)
    public IRubyObject values_at(IRubyObject[] args) {
        return to_a().values_at(args);
    }

    @JRubyMethod(name = "select", frame = true, compat = CompatVersion.RUBY1_8)
    public IRubyObject select(ThreadContext context, Block block) {
        Ruby runtime = context.getRuntime();
        final RubyArray result;
        if (regs == null) {
            if (begin < 0) return runtime.newEmptyArray();
            IRubyObject s = str.substr(runtime, begin, end - begin);
            s.setTaint(isTaint());
            result = block.yield(context, s).isTrue() ? runtime.newArray(s) : runtime.newEmptyArray();
        } else {
            result = runtime.newArray();
            boolean taint = isTaint();
            for (int i = 0; i < regs.numRegs; i++) {
                IRubyObject s = str.substr(runtime, regs.beg[i], regs.end[i] - regs.beg[i]);
                if (taint) s.setTaint(true);
                if (block.yield(context, s).isTrue()) result.append(s);
            }
        }
        return result;
    }

    /** match_captures
     *
     */
    @JRubyMethod(name = "captures")
    public IRubyObject captures(ThreadContext context) {
        return match_array(context.getRuntime(), 1);
    }

    private int nameToBackrefNumber(RubyString str) {
        ByteList value = str.getByteList();
        try {
            return regexp.getPattern().nameToBackrefNumber(value.bytes, value.begin, value.begin + value.realSize, regs);
        } catch (JOniException je) {
            throw getRuntime().newIndexError(je.getMessage());
        }
    }

    final int backrefNumber(IRubyObject obj) {
        check();
        if (obj instanceof RubySymbol) {
            return nameToBackrefNumber((RubyString)((RubySymbol)obj).id2name());
        } else if (obj instanceof RubyString) {
            return nameToBackrefNumber((RubyString)obj);
        } else {
            return RubyNumeric.num2int(obj);
        }
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public IRubyObject op_aref(IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return op_aref(args[0]);
        case 2:
            return op_aref(args[0], args[1]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** match_aref
    *
    */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(IRubyObject idx) {
        if (!(idx instanceof RubyFixnum) || ((RubyFixnum)idx).getLongValue() < 0) {
            return ((RubyArray)to_a()).aref(idx);
        }
        return RubyRegexp.nth_match(RubyNumeric.fix2int(idx), this);
    }

    /** match_aref
     *
     */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(IRubyObject idx, IRubyObject rest) {
        if (!rest.isNil() || !(idx instanceof RubyFixnum) || ((RubyFixnum)idx).getLongValue() < 0) {
            return ((RubyArray)to_a()).aref(idx, rest);
        }
        return RubyRegexp.nth_match(RubyNumeric.fix2int(idx), this);
    }

    /** match_aref
     *
     */
    @JRubyMethod(name = "[]", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_aref19(IRubyObject idx) {
        IRubyObject result = op_arefCommon(idx);
        return result == null ? ((RubyArray)to_a()).aref(idx) : result;
    }

    /** match_aref
    *
    */
    @JRubyMethod(name = "[]", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_aref19(IRubyObject idx, IRubyObject rest) {
        IRubyObject result;
        return !rest.isNil() || (result = op_arefCommon(idx)) == null ? ((RubyArray)to_a()).aref(idx, rest) : result;
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
        Ruby runtime = context.getRuntime();
        return regs == null ? RubyFixnum.one(runtime) : RubyFixnum.newFixnum(runtime, regs.numRegs);
    }

    /** match_begin
     *
     */
    @JRubyMethod(name = "begin")
    public IRubyObject begin(ThreadContext context, IRubyObject index) {
        return beginCommon(context, RubyNumeric.num2int(index));
    }

    @JRubyMethod(name = "begin", compat = CompatVersion.RUBY1_9)
    public IRubyObject begin19(ThreadContext context, IRubyObject index) {
       return beginCommon(context, backrefNumber(index));
    }

    private IRubyObject beginCommon(ThreadContext context, int i) {
        check();
        Ruby runtime = context.getRuntime();
        if (regs == null) {
            if (i != 0) throw runtime.newIndexError("index " + i + " out of matches");
            if (begin < 0) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, begin);
        } else {
            if (i < 0 || regs.numRegs <= i) throw runtime.newIndexError("index " + i + " out of matches");
            if (regs.beg[i] < 0) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, regs.beg[i]);
        }
    }

    /** match_end
     *
     */
    @JRubyMethod(name = "end")
    public IRubyObject end(ThreadContext context, IRubyObject index) {
        return endCommon(context, RubyNumeric.num2int(index));
    }

    @JRubyMethod(name = "end", compat = CompatVersion.RUBY1_9)
    public IRubyObject end19(ThreadContext context, IRubyObject index) {
       return endCommon(context, backrefNumber(index));
    }

    private IRubyObject endCommon(ThreadContext context, int i) {
        check();
        Ruby runtime = context.getRuntime(); 
        if (regs == null) {
            if (i != 0) throw runtime.newIndexError("index " + i + " out of matches");
            if (end < 0) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, end);
        } else {
            if (i < 0 || regs.numRegs <= i) throw runtime.newIndexError("index " + i + " out of matches");
            if (regs.end[i] < 0) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, regs.end[i]);
        }
    }

    /** match_offset
     *
     */
    @JRubyMethod(name = "offset")
    public IRubyObject offset(ThreadContext context, IRubyObject index) {
        return offsetCommon(context, RubyNumeric.num2int(index));

    }

    @JRubyMethod(name = "offset", compat = CompatVersion.RUBY1_9)
    public IRubyObject offset19(ThreadContext context, IRubyObject index) {
        return offsetCommon(context, backrefNumber(index));
    }

    private IRubyObject offsetCommon(ThreadContext context, int i) {
        check();
        Ruby runtime = context.getRuntime();

        if (regs == null) {
            if (i != 0) throw runtime.newIndexError("index " + i + " out of matches");
            if (begin < 0) return runtime.newArray(runtime.getNil(), runtime.getNil());
            return runtime.newArray(RubyFixnum.newFixnum(runtime, begin),RubyFixnum.newFixnum(runtime, end));            
        } else {
            if (i < 0 || regs.numRegs <= i) throw runtime.newIndexError("index " + i + " out of matches");
            if (regs.beg[i] < 0) return runtime.newArray(runtime.getNil(), runtime.getNil());
            return runtime.newArray(RubyFixnum.newFixnum(runtime, regs.beg[i]),RubyFixnum.newFixnum(runtime, regs.end[i]));
        }
    }
    /** match_pre_match
     *
     */
    @JRubyMethod(name = "pre_match")
    public IRubyObject pre_match(ThreadContext context) {
        check();
        RubyString ss;
        
        if (regs == null) {
            if (begin == -1) return context.getRuntime().getNil();
            ss = str.makeShared(context.getRuntime(), 0, begin);
        } else {
            if (regs.beg[0] == -1) return context.getRuntime().getNil();
            ss = str.makeShared(context.getRuntime(), 0, regs.beg[0]);
        }
        
        if (isTaint()) ss.setTaint(true);
        return ss;
    }

    /** match_post_match
     *
     */
    @JRubyMethod(name = "post_match")
    public IRubyObject post_match(ThreadContext context) {
        check();
        RubyString ss;

        if (begin == -1) return context.getRuntime().getNil();
        ss = str.makeShared(context.getRuntime(), end, str.getByteList().length() - end);

        if(isTaint()) ss.setTaint(true);
        return ss;
    }

    /** match_to_s
     *
     */
    @JRubyMethod(name = "to_s")
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
    @JRubyMethod(name = "string")
    public IRubyObject string() {
        check();
        return str; //str is frozen
    }

    @JRubyMethod(name = "initialize_copy", required = 1)
    public IRubyObject initialize_copy(IRubyObject original) {
        if (this == original) return this;
        
        if (!(getMetaClass() == original.getMetaClass())){ // MRI also does a pointer comparison here
            throw getRuntime().newTypeError("wrong argument class");
        }

        RubyMatchData origMatchData = (RubyMatchData)original;
        str = origMatchData.str;
        regs = origMatchData.regs;

        return this;
    }
}

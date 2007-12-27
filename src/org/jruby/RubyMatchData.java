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

import org.joni.Regex;
import org.joni.Region;
import org.joni.exception.JOniException;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author olabini
 */
public class RubyMatchData extends RubyObject {
    Region regs;        // captures
    int begin;          // begin and end are used when not groups defined
    int end;
    RubyString str;
    Regex pattern;
    
    public static RubyClass createMatchDataClass(Ruby runtime) {
        // TODO: Is NOT_ALLOCATABLE_ALLOCATOR ok here, since you can't actually instantiate MatchData directly?
        RubyClass matchDataClass = runtime.defineClass("MatchData", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setMatchData(matchDataClass);
        runtime.defineGlobalConstant("MatchingData", matchDataClass);
        matchDataClass.kindOf = new RubyModule.KindOf() {
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyMatchData;
            }
        };

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyMatchData.class);

        matchDataClass.getMetaClass().undefineMethod("new");
        
        matchDataClass.defineAnnotatedMethods(RubyMatchData.class);
        matchDataClass.dispatcher = callbackFactory.createDispatcher(matchDataClass);

        return matchDataClass;
    }

    public RubyMatchData(Ruby runtime) {
        super(runtime, runtime.getMatchData());
    }

    public final static int MATCH_BUSY = USER2_F;

    // rb_match_busy
    public final void use() {
        flags |= MATCH_BUSY; 
    }

    public final boolean used() {
        return (flags & MATCH_BUSY) != 0;
    }

    private RubyArray match_array(int start) {
        if (regs == null) {
            if (start != 0) return getRuntime().newEmptyArray();
            if (begin == -1) {
                return getRuntime().newArray(getRuntime().getNil());
            } else {
                RubyString ss = str.makeShared(begin, end - begin);
                if (isTaint()) ss.setTaint(true);
                return getRuntime().newArray(ss);
            }
        } else {
            RubyArray arr = getRuntime().newArray(regs.numRegs - start);
            for (int i=start; i<regs.numRegs; i++) {
                if (regs.beg[i] == -1) {
                    arr.append(getRuntime().getNil());
                } else {
                    RubyString ss = str.makeShared(regs.beg[i], regs.end[i] - regs.beg[i]);
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

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect() {
        return anyToString();
    }

    /** match_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    public RubyArray to_a() {
        return match_array(0);
    }

    @JRubyMethod(name = "values_at", required = 1, rest = true)
    public IRubyObject values_at(IRubyObject[] args) {
        return to_a().values_at(args);
    }

    @JRubyMethod(name = "select", frame = true)
    public IRubyObject select(Block block) {
        return block.yield(getRuntime().getCurrentContext(), to_a());
    }

    /** match_captures
     *
     */
    @JRubyMethod(name = "captures")
    public IRubyObject captures() {
        return match_array(1);
    }

    private int nameToBackrefNumber(RubyString str) {
        ByteList value = str.getByteList();
        try {
            return pattern.nameToBackrefNumber(value.bytes, value.begin, value.begin + value.realSize, regs);
        } catch (JOniException je) {
            throw getRuntime().newIndexError(je.getMessage());
        }
    }

    /** match_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1, optional = 1)
    public IRubyObject op_aref(IRubyObject[] args) {
        final IRubyObject rest = Arity.checkArgumentCount(getRuntime(), args,1,2) == 2 ? args[1] : null;
        final IRubyObject idx = args[0];

        if (rest == null || rest.isNil()) {
            if (idx instanceof RubyFixnum) {
                int num = RubyNumeric.fix2int(idx);
                if (num >= 0) return RubyRegexp.nth_match(num, this);                
            } else {
                RubyString str;
                if (idx instanceof RubySymbol) {
                    str = (RubyString)((RubySymbol)idx).id2name();
                } else if (idx instanceof RubyString) {
                    str = (RubyString)idx;
                } else {
                    return ((RubyArray)to_a()).aref(args);
                }
                return RubyRegexp.nth_match(nameToBackrefNumber(str), this);
            }
        }
        return ((RubyArray)to_a()).aref(args);
    }

    /** match_size
     *
     */
    @JRubyMethod(name = {"size", "length"})
    public IRubyObject size() {
        return regs == null ? RubyFixnum.one(getRuntime()) : RubyFixnum.newFixnum(getRuntime(), regs.numRegs);
    }

    /** match_begin
     *
     */
    @JRubyMethod(name = "begin", required = 1)
    public IRubyObject begin(IRubyObject index) {
        int i = RubyNumeric.num2int(index);

        if (regs == null) {
            if (i != 0) throw getRuntime().newIndexError("index " + i + " out of matches");
            if (begin < 0) return getRuntime().getNil();
            return RubyFixnum.newFixnum(getRuntime(), begin);
        } else {
            if (i < 0 || regs.numRegs <= i) throw getRuntime().newIndexError("index " + i + " out of matches");
            if (regs.beg[i] < 0) return getRuntime().getNil();
            return RubyFixnum.newFixnum(getRuntime(), regs.beg[i]);
        }
    }

    /** match_end
     *
     */
    @JRubyMethod(name = "end", required = 1)
    public IRubyObject end(IRubyObject index) {
        int i = RubyNumeric.num2int(index);

        if (regs == null) {
            if (i != 0) throw getRuntime().newIndexError("index " + i + " out of matches");
            if (end < 0) return getRuntime().getNil();
            return RubyFixnum.newFixnum(getRuntime(), end);
        } else {
            if (i < 0 || regs.numRegs <= i) throw getRuntime().newIndexError("index " + i + " out of matches");
            if (regs.end[i] < 0) return getRuntime().getNil();
            return RubyFixnum.newFixnum(getRuntime(), regs.end[i]);
        }
    }

    /** match_offset
     *
     */
    @JRubyMethod(name = "offset", required = 1)
    public IRubyObject offset(IRubyObject index) {
        int i = RubyNumeric.num2int(index);
        Ruby runtime = getRuntime();

        if (regs == null) {
            if (i != 0) throw getRuntime().newIndexError("index " + i + " out of matches");
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
    public IRubyObject pre_match() {
        RubyString ss;
        
        if (regs == null) {
            if(begin == -1) return getRuntime().getNil();
            ss = str.makeShared(0, begin);
        } else {
            if(regs.beg[0] == -1) return getRuntime().getNil();
            ss = str.makeShared(0, regs.beg[0]);
        }
        
        if (isTaint()) ss.setTaint(true);
        return ss;
    }

    /** match_post_match
     *
     */
    @JRubyMethod(name = "post_match")
    public IRubyObject post_match() {
        RubyString ss;
        
        if (regs == null) {
            if (begin == -1) return getRuntime().getNil();
            ss = str.makeShared(end, str.getByteList().length() - end);
        } else {
            if (regs.beg[0] == -1) return getRuntime().getNil();
            ss = str.makeShared(regs.end[0], str.getByteList().length() - regs.end[0]);
        }
        
        if(isTaint()) ss.setTaint(true);
        return ss;
    }

    /** match_to_s
     *
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s() {
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

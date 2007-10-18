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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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

import org.jruby.anno.JRubyMethod;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;

import org.rej.Registers;

/**
 *
 * @author  amoore
 */
public class RubyMatchDataRej extends RubyObject {
    Registers regs;
    RubyString str;
    
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

    public RubyMatchDataRej(Ruby runtime) {
        super(runtime, runtime.getMatchData());
    }

    private RubyArray match_array(int start) {
        RubyArray arr = getRuntime().newArray(regs.num_regs-start);
        boolean taint = isTaint();
        for(int i=start;i<regs.num_regs;i++) {
            if(regs.beg[i] == -1) {
                arr.append(getRuntime().getNil());
            } else {
                RubyString ss = str.makeShared(regs.beg[i], regs.end[i] - regs.beg[i]);
                if(isTaint()) {
                    ss.setTaint(true); 
                }
                arr.append(ss);
            }
        }
        return arr;
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

    /** match_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1, optional = 1)
    public IRubyObject op_aref(IRubyObject[] args) {
        IRubyObject idx;
        IRubyObject rest = getRuntime().getNil();
        if(Arity.checkArgumentCount(getRuntime(), args,1,2) == 2) {
            rest = args[1];
        }
        idx = args[0];
        if(!rest.isNil() || !(idx instanceof RubyFixnum) || RubyNumeric.fix2int(idx) < 0) {
            return ((RubyArray)to_a()).aref(args);
        }
        return RubyRegexp.nth_match(RubyNumeric.fix2int(idx),this);
    }

    /** match_size
     *
     */
    @JRubyMethod(name = "size", name2 = "length")
    public IRubyObject size() {
        return getRuntime().newFixnum(regs.num_regs);
    }

    /** match_begin
     *
     */
    @JRubyMethod(name = "begin", required = 1)
    public IRubyObject begin(IRubyObject index) {
        int i = RubyNumeric.num2int(index);
        
        if(i < 0 || regs.num_regs <= i) {
            throw getRuntime().newIndexError("index " + i + " out of matches");
        }
        if(regs.beg[i] < 0) {
            return getRuntime().getNil();
        }
        return getRuntime().newFixnum(regs.beg[i]);
    }

    /** match_end
     *
     */
    @JRubyMethod(name = "end", required = 1)
    public IRubyObject end(IRubyObject index) {
        int i = RubyNumeric.num2int(index);
        
        if(i < 0 || regs.num_regs <= i) {
            throw getRuntime().newIndexError("index " + i + " out of matches");
        }
        if(regs.end[i] < 0) {
            return getRuntime().getNil();
        }
        return getRuntime().newFixnum(regs.end[i]);
    }

    /** match_offset
     *
     */
    @JRubyMethod(name = "offset", required = 1)
    public IRubyObject offset(IRubyObject index) {
        int i = RubyNumeric.num2int(index);

        if(i < 0 || regs.num_regs <= i) {
            throw getRuntime().newIndexError("index " + i + " out of matches");
        }

        if(regs.beg[i] < 0) {
            return getRuntime().newArray(getRuntime().getNil(),getRuntime().getNil());
        }
        return getRuntime().newArray(getRuntime().newFixnum(regs.beg[i]),getRuntime().newFixnum(regs.end[i]));
    }

    /** match_pre_match
     *
     */
    @JRubyMethod(name = "pre_match")
    public IRubyObject pre_match() {
        if(regs.beg[0] == -1) {
            return getRuntime().getNil();
        }
        RubyString ss = str.makeShared(0, regs.beg[0]);
        if(isTaint()) {
            ss.setTaint(true);
        }
        return ss;
    }

    /** match_post_match
     *
     */
    @JRubyMethod(name = "post_match")
    public IRubyObject post_match() {
        if(regs.beg[0] == -1) {
            return getRuntime().getNil();
        }
        RubyString ss = str.makeShared(regs.end[0], str.getByteList().length() - regs.end[0]);
        if(isTaint()) {
            ss.setTaint(true);
        }
        return ss;
    }

    /** match_to_s
     *
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s() {
        IRubyObject ss = RubyRegexp.last_match(this);
        if(ss.isNil()) {
            ss = RubyString.newEmptyString(getRuntime(), getRuntime().getString());
        }
        if(isTaint()) {
            ss.setTaint(true);
        }
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

        RubyMatchDataRej origMatchData = (RubyMatchDataRej)original;
        str = origMatchData.str;
        regs = origMatchData.regs;

        return this;
    }
}

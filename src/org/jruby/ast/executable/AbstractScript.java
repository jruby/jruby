/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.executable;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public abstract class AbstractScript implements Script {
    public AbstractScript() {
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, Block block) {
        return __file__(context, self, IRubyObject.NULL_ARRAY, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        return __file__(context, self, new IRubyObject[] {arg}, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        return __file__(context, self, new IRubyObject[] {arg1, arg2}, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return __file__(context, self, new IRubyObject[] {arg1, arg2, arg3}, block);
    }
    
    public IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return null;
    }
    
    public IRubyObject run(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return __file__(context, self, args, block);
    }
    
    public final RubyFixnum getFixnum0(Ruby runtime, long value) {
        if (fixnum0 == null) {
            fixnum0 = runtime.newFixnum(value);
        }
        return fixnum0;
    }
    
    public final RubyFixnum getFixnum1(Ruby runtime, long value) {
        if (fixnum1 == null) {
            fixnum1 = runtime.newFixnum(value);
        }
        return fixnum1;
    }
    
    public final RubyFixnum getFixnum2(Ruby runtime, long value) {
        if (fixnum2 == null) {
            fixnum2 = runtime.newFixnum(value);
        }
        return fixnum2;
    }
    
    public final RubyFixnum getFixnum3(Ruby runtime, long value) {
        if (fixnum3 == null) {
            fixnum3 = runtime.newFixnum(value);
        }
        return fixnum3;
    }
    
    public final RubyFixnum getFixnum4(Ruby runtime, long value) {
        if (fixnum4 == null) {
            fixnum4 = runtime.newFixnum(value);
        }
        return fixnum4;
    }
    
    public final RubyFixnum getFixnum5(Ruby runtime, long value) {
        if (fixnum5 == null) {
            fixnum5 = runtime.newFixnum(value);
        }
        return fixnum5;
    }
    
    public final RubyFixnum getFixnum6(Ruby runtime, long value) {
        if (fixnum6 == null) {
            fixnum6 = runtime.newFixnum(value);
        }
        return fixnum6;
    }
    
    public final RubyFixnum getFixnum7(Ruby runtime, long value) {
        if (fixnum7 == null) {
            fixnum7 = runtime.newFixnum(value);
        }
        return fixnum7;
    }
    
    public final RubyFixnum getFixnum8(Ruby runtime, long value) {
        if (fixnum8 == null) {
            fixnum8 = runtime.newFixnum(value);
        }
        return fixnum8;
    }
    
    public final RubyFixnum getFixnum9(Ruby runtime, long value) {
        if (fixnum9 == null) {
            fixnum9 = runtime.newFixnum(value);
        }
        return fixnum9;
    }
    
    public final RubyFixnum getFixnum10(Ruby runtime, long value) {
        if (fixnum10 == null) {
            fixnum10 = runtime.newFixnum(value);
        }
        return fixnum10;
    }
    
    public final RubyFixnum getFixnum11(Ruby runtime, long value) {
        if (fixnum11 == null) {
            fixnum11 = runtime.newFixnum(value);
        }
        return fixnum11;
    }
    
    public final RubyFixnum getFixnum12(Ruby runtime, long value) {
        if (fixnum12 == null) {
            fixnum12 = runtime.newFixnum(value);
        }
        return fixnum12;
    }
    
    public final RubyFixnum getFixnum13(Ruby runtime, long value) {
        if (fixnum13 == null) {
            fixnum13 = runtime.newFixnum(value);
        }
        return fixnum13;
    }
    
    public final RubyFixnum getFixnum14(Ruby runtime, long value) {
        if (fixnum14 == null) {
            fixnum14 = runtime.newFixnum(value);
        }
        return fixnum14;
    }
    
    public final RubyFixnum getFixnum15(Ruby runtime, long value) {
        if (fixnum15 == null) {
            fixnum15 = runtime.newFixnum(value);
        }
        return fixnum15;
    }
    
    public final RubyFixnum getFixnum16(Ruby runtime, long value) {
        if (fixnum16 == null) {
            fixnum16 = runtime.newFixnum(value);
        }
        return fixnum16;
    }
    
    public final RubyFixnum getFixnum17(Ruby runtime, long value) {
        if (fixnum17 == null) {
            fixnum17 = runtime.newFixnum(value);
        }
        return fixnum17;
    }
    
    public final RubyFixnum getFixnum18(Ruby runtime, long value) {
        if (fixnum18 == null) {
            fixnum18 = runtime.newFixnum(value);
        }
        return fixnum18;
    }
    
    public final RubyFixnum getFixnum19(Ruby runtime, long value) {
        if (fixnum19 == null) {
            fixnum19 = runtime.newFixnum(value);
        }
        return fixnum19;
    }
    
    public final RubyFixnum getFixnum20(Ruby runtime, long value) {
        if (fixnum20 == null) {
            fixnum20 = runtime.newFixnum(value);
        }
        return fixnum20;
    }
    
    public final RubyFixnum getFixnum21(Ruby runtime, long value) {
        if (fixnum21 == null) {
            fixnum21 = runtime.newFixnum(value);
        }
        return fixnum21;
    }
    
    public final RubyFixnum getFixnum22(Ruby runtime, long value) {
        if (fixnum22 == null) {
            fixnum22 = runtime.newFixnum(value);
        }
        return fixnum22;
    }
    
    public final RubyFixnum getFixnum23(Ruby runtime, long value) {
        if (fixnum23 == null) {
            fixnum23 = runtime.newFixnum(value);
        }
        return fixnum23;
    }
    
    public final RubyFixnum getFixnum24(Ruby runtime, long value) {
        if (fixnum24 == null) {
            fixnum24 = runtime.newFixnum(value);
        }
        return fixnum24;
    }
    
    public final RubyFixnum getFixnum25(Ruby runtime, long value) {
        if (fixnum25 == null) {
            fixnum25 = runtime.newFixnum(value);
        }
        return fixnum25;
    }
    
    public final RubyFixnum getFixnum26(Ruby runtime, long value) {
        if (fixnum26 == null) {
            fixnum26 = runtime.newFixnum(value);
        }
        return fixnum26;
    }
    
    public final RubyFixnum getFixnum27(Ruby runtime, long value) {
        if (fixnum27 == null) {
            fixnum27 = runtime.newFixnum(value);
        }
        return fixnum27;
    }
    
    public final RubyFixnum getFixnum28(Ruby runtime, long value) {
        if (fixnum28 == null) {
            fixnum28 = runtime.newFixnum(value);
        }
        return fixnum28;
    }
    
    public final RubyFixnum getFixnum29(Ruby runtime, long value) {
        if (fixnum29 == null) {
            fixnum29 = runtime.newFixnum(value);
        }
        return fixnum29;
    }
    
    public final RubyFixnum getFixnum30(Ruby runtime, long value) {
        if (fixnum30 == null) {
            fixnum30 = runtime.newFixnum(value);
        }
        return fixnum30;
    }
    
    public final RubyFixnum getFixnum31(Ruby runtime, long value) {
        if (fixnum31 == null) {
            fixnum31 = runtime.newFixnum(value);
        }
        return fixnum31;
    }
    
    public final RubyFixnum getFixnum32(Ruby runtime, long value) {
        if (fixnum32 == null) {
            fixnum32 = runtime.newFixnum(value);
        }
        return fixnum32;
    }
    
    public final RubyFixnum getFixnum33(Ruby runtime, long value) {
        if (fixnum33 == null) {
            fixnum33 = runtime.newFixnum(value);
        }
        return fixnum33;
    }
    
    public final RubyFixnum getFixnum34(Ruby runtime, long value) {
        if (fixnum34 == null) {
            fixnum34 = runtime.newFixnum(value);
        }
        return fixnum34;
    }
    
    public final RubyFixnum getFixnum35(Ruby runtime, long value) {
        if (fixnum35 == null) {
            fixnum35 = runtime.newFixnum(value);
        }
        return fixnum35;
    }
    
    public final RubyFixnum getFixnum36(Ruby runtime, long value) {
        if (fixnum36 == null) {
            fixnum36 = runtime.newFixnum(value);
        }
        return fixnum36;
    }
    
    public final RubyFixnum getFixnum37(Ruby runtime, long value) {
        if (fixnum37 == null) {
            fixnum37 = runtime.newFixnum(value);
        }
        return fixnum37;
    }
    
    public final RubyFixnum getFixnum38(Ruby runtime, long value) {
        if (fixnum38 == null) {
            fixnum38 = runtime.newFixnum(value);
        }
        return fixnum38;
    }
    
    public final RubyFixnum getFixnum39(Ruby runtime, long value) {
        if (fixnum39 == null) {
            fixnum39 = runtime.newFixnum(value);
        }
        return fixnum39;
    }
    
    public final RubyFixnum getFixnum40(Ruby runtime, long value) {
        if (fixnum40 == null) {
            fixnum40 = runtime.newFixnum(value);
        }
        return fixnum40;
    }
    
    public final RubyFixnum getFixnum41(Ruby runtime, long value) {
        if (fixnum41 == null) {
            fixnum41 = runtime.newFixnum(value);
        }
        return fixnum41;
    }
    
    public final RubyFixnum getFixnum42(Ruby runtime, long value) {
        if (fixnum42 == null) {
            fixnum42 = runtime.newFixnum(value);
        }
        return fixnum42;
    }
    
    public final RubyFixnum getFixnum43(Ruby runtime, long value) {
        if (fixnum43 == null) {
            fixnum43 = runtime.newFixnum(value);
        }
        return fixnum43;
    }
    
    public final RubyFixnum getFixnum44(Ruby runtime, long value) {
        if (fixnum44 == null) {
            fixnum44 = runtime.newFixnum(value);
        }
        return fixnum44;
    }
    
    public final RubyFixnum getFixnum45(Ruby runtime, long value) {
        if (fixnum45 == null) {
            fixnum45 = runtime.newFixnum(value);
        }
        return fixnum45;
    }
    
    public final RubyFixnum getFixnum46(Ruby runtime, long value) {
        if (fixnum46 == null) {
            fixnum46 = runtime.newFixnum(value);
        }
        return fixnum46;
    }
    
    public final RubyFixnum getFixnum47(Ruby runtime, long value) {
        if (fixnum47 == null) {
            fixnum47 = runtime.newFixnum(value);
        }
        return fixnum47;
    }
    
    public final RubyFixnum getFixnum48(Ruby runtime, long value) {
        if (fixnum48 == null) {
            fixnum48 = runtime.newFixnum(value);
        }
        return fixnum48;
    }
    
    public final RubyFixnum getFixnum49(Ruby runtime, long value) {
        if (fixnum49 == null) {
            fixnum49 = runtime.newFixnum(value);
        }
        return fixnum49;
    }

    public final CallSite getCallSite(int index) {
        return callSites[index];
    }

    public final RubySymbol getSymbol(Ruby runtime, int index, String name) {
        RubySymbol symbol = symbols[index];
        if (symbol == null) return symbols[index] = runtime.newSymbol(name);
        return symbol;
    }

    public final void initCallSites(int size) {
        callSites = new CallSite[size];
    }

    public final void initSymbols(int size) {
        symbols = new RubySymbol[size];
    }

    public static CallSite[] setCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getCallSite(name);
        return callSites;
    }

    public static CallSite[] setFunctionalCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getFunctionalCallSite(name);
        return callSites;
    }

    public static CallSite[] setVariableCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getVariableCallSite(name);
        return callSites;
    }

    public CallSite[] callSites;
    public RubySymbol[] symbols;
    
    public RubyFixnum fixnum0;
    public RubyFixnum fixnum1;
    public RubyFixnum fixnum2;
    public RubyFixnum fixnum3;
    public RubyFixnum fixnum4;
    public RubyFixnum fixnum5;
    public RubyFixnum fixnum6;
    public RubyFixnum fixnum7;
    public RubyFixnum fixnum8;
    public RubyFixnum fixnum9;
    public RubyFixnum fixnum10;
    public RubyFixnum fixnum11;
    public RubyFixnum fixnum12;
    public RubyFixnum fixnum13;
    public RubyFixnum fixnum14;
    public RubyFixnum fixnum15;
    public RubyFixnum fixnum16;
    public RubyFixnum fixnum17;
    public RubyFixnum fixnum18;
    public RubyFixnum fixnum19;
    public RubyFixnum fixnum20;
    public RubyFixnum fixnum21;
    public RubyFixnum fixnum22;
    public RubyFixnum fixnum23;
    public RubyFixnum fixnum24;
    public RubyFixnum fixnum25;
    public RubyFixnum fixnum26;
    public RubyFixnum fixnum27;
    public RubyFixnum fixnum28;
    public RubyFixnum fixnum29;
    public RubyFixnum fixnum30;
    public RubyFixnum fixnum31;
    public RubyFixnum fixnum32;
    public RubyFixnum fixnum33;
    public RubyFixnum fixnum34;
    public RubyFixnum fixnum35;
    public RubyFixnum fixnum36;
    public RubyFixnum fixnum37;
    public RubyFixnum fixnum38;
    public RubyFixnum fixnum39;
    public RubyFixnum fixnum40;
    public RubyFixnum fixnum41;
    public RubyFixnum fixnum42;
    public RubyFixnum fixnum43;
    public RubyFixnum fixnum44;
    public RubyFixnum fixnum45;
    public RubyFixnum fixnum46;
    public RubyFixnum fixnum47;
    public RubyFixnum fixnum48;
    public RubyFixnum fixnum49;
}

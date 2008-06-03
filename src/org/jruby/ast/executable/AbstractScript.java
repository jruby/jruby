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
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public abstract class AbstractScript implements Script {
    public AbstractScript() {}
    
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
    
    public final synchronized RubySymbol getSymbol0(Ruby runtime, String symbol) {
        if (symbol0 == null) {
            symbol0 = runtime.fastNewSymbol(symbol);
        }
        return symbol0;
    }
    
    public final synchronized RubySymbol getSymbol1(Ruby runtime, String symbol) {
        if (symbol1 == null) {
            symbol1 = runtime.fastNewSymbol(symbol);
        }
        return symbol1;
    }
    
    public final synchronized RubySymbol getSymbol2(Ruby runtime, String symbol) {
        if (symbol2 == null) {
            symbol2 = runtime.fastNewSymbol(symbol);
        }
        return symbol2;
    }
    
    public final synchronized RubySymbol getSymbol3(Ruby runtime, String symbol) {
        if (symbol3 == null) {
            symbol3 = runtime.fastNewSymbol(symbol);
        }
        return symbol3;
    }
    
    public final synchronized RubySymbol getSymbol4(Ruby runtime, String symbol) {
        if (symbol4 == null) {
            symbol4 = runtime.fastNewSymbol(symbol);
        }
        return symbol4;
    }
    
    public final synchronized RubySymbol getSymbol5(Ruby runtime, String symbol) {
        if (symbol5 == null) {
            symbol5 = runtime.fastNewSymbol(symbol);
        }
        return symbol5;
    }
    
    public final synchronized RubySymbol getSymbol6(Ruby runtime, String symbol) {
        if (symbol6 == null) {
            symbol6 = runtime.fastNewSymbol(symbol);
        }
        return symbol6;
    }
    
    public final synchronized RubySymbol getSymbol7(Ruby runtime, String symbol) {
        if (symbol7 == null) {
            symbol7 = runtime.fastNewSymbol(symbol);
        }
        return symbol7;
    }
    
    public final synchronized RubySymbol getSymbol8(Ruby runtime, String symbol) {
        if (symbol8 == null) {
            symbol8 = runtime.fastNewSymbol(symbol);
        }
        return symbol8;
    }
    
    public final synchronized RubySymbol getSymbol9(Ruby runtime, String symbol) {
        if (symbol9 == null) {
            symbol9 = runtime.fastNewSymbol(symbol);
        }
        return symbol9;
    }
    
    public final synchronized RubySymbol getSymbol10(Ruby runtime, String symbol) {
        if (symbol10 == null) {
            symbol10 = runtime.fastNewSymbol(symbol);
        }
        return symbol10;
    }
    
    public final synchronized RubySymbol getSymbol11(Ruby runtime, String symbol) {
        if (symbol11 == null) {
            symbol11 = runtime.fastNewSymbol(symbol);
        }
        return symbol11;
    }
    
    public final synchronized RubySymbol getSymbol12(Ruby runtime, String symbol) {
        if (symbol12 == null) {
            symbol12 = runtime.fastNewSymbol(symbol);
        }
        return symbol12;
    }
    
    public final synchronized RubySymbol getSymbol13(Ruby runtime, String symbol) {
        if (symbol13 == null) {
            symbol13 = runtime.fastNewSymbol(symbol);
        }
        return symbol13;
    }
    
    public final synchronized RubySymbol getSymbol14(Ruby runtime, String symbol) {
        if (symbol14 == null) {
            symbol14 = runtime.fastNewSymbol(symbol);
        }
        return symbol14;
    }
    
    public final synchronized RubySymbol getSymbol15(Ruby runtime, String symbol) {
        if (symbol15 == null) {
            symbol15 = runtime.fastNewSymbol(symbol);
        }
        return symbol15;
    }
    
    public final synchronized RubySymbol getSymbol16(Ruby runtime, String symbol) {
        if (symbol16 == null) {
            symbol16 = runtime.fastNewSymbol(symbol);
        }
        return symbol16;
    }
    
    public final synchronized RubySymbol getSymbol17(Ruby runtime, String symbol) {
        if (symbol17 == null) {
            symbol17 = runtime.fastNewSymbol(symbol);
        }
        return symbol17;
    }
    
    public final synchronized RubySymbol getSymbol18(Ruby runtime, String symbol) {
        if (symbol18 == null) {
            symbol18 = runtime.fastNewSymbol(symbol);
        }
        return symbol18;
    }
    
    public final synchronized RubySymbol getSymbol19(Ruby runtime, String symbol) {
        if (symbol19 == null) {
            symbol19 = runtime.fastNewSymbol(symbol);
        }
        return symbol19;
    }
    
    public final synchronized RubySymbol getSymbol20(Ruby runtime, String symbol) {
        if (symbol20 == null) {
            symbol20 = runtime.fastNewSymbol(symbol);
        }
        return symbol20;
    }
    
    public final synchronized RubySymbol getSymbol21(Ruby runtime, String symbol) {
        if (symbol21 == null) {
            symbol21 = runtime.fastNewSymbol(symbol);
        }
        return symbol21;
    }
    
    public final synchronized RubySymbol getSymbol22(Ruby runtime, String symbol) {
        if (symbol22 == null) {
            symbol22 = runtime.fastNewSymbol(symbol);
        }
        return symbol22;
    }
    
    public final synchronized RubySymbol getSymbol23(Ruby runtime, String symbol) {
        if (symbol23 == null) {
            symbol23 = runtime.fastNewSymbol(symbol);
        }
        return symbol23;
    }
    
    public final synchronized RubySymbol getSymbol24(Ruby runtime, String symbol) {
        if (symbol24 == null) {
            symbol24 = runtime.fastNewSymbol(symbol);
        }
        return symbol24;
    }
    
    public final synchronized RubySymbol getSymbol25(Ruby runtime, String symbol) {
        if (symbol25 == null) {
            symbol25 = runtime.fastNewSymbol(symbol);
        }
        return symbol25;
    }
    
    public final synchronized RubySymbol getSymbol26(Ruby runtime, String symbol) {
        if (symbol26 == null) {
            symbol26 = runtime.fastNewSymbol(symbol);
        }
        return symbol26;
    }
    
    public final synchronized RubySymbol getSymbol27(Ruby runtime, String symbol) {
        if (symbol27 == null) {
            symbol27 = runtime.fastNewSymbol(symbol);
        }
        return symbol27;
    }
    
    public final synchronized RubySymbol getSymbol28(Ruby runtime, String symbol) {
        if (symbol28 == null) {
            symbol28 = runtime.fastNewSymbol(symbol);
        }
        return symbol28;
    }
    
    public final synchronized RubySymbol getSymbol29(Ruby runtime, String symbol) {
        if (symbol29 == null) {
            symbol29 = runtime.fastNewSymbol(symbol);
        }
        return symbol29;
    }
    
    public final synchronized RubySymbol getSymbol30(Ruby runtime, String symbol) {
        if (symbol30 == null) {
            symbol30 = runtime.fastNewSymbol(symbol);
        }
        return symbol30;
    }
    
    public final synchronized RubySymbol getSymbol31(Ruby runtime, String symbol) {
        if (symbol31 == null) {
            symbol31 = runtime.fastNewSymbol(symbol);
        }
        return symbol31;
    }
    
    public final synchronized RubySymbol getSymbol32(Ruby runtime, String symbol) {
        if (symbol32 == null) {
            symbol32 = runtime.fastNewSymbol(symbol);
        }
        return symbol32;
    }
    
    public final synchronized RubySymbol getSymbol33(Ruby runtime, String symbol) {
        if (symbol33 == null) {
            symbol33 = runtime.fastNewSymbol(symbol);
        }
        return symbol33;
    }
    
    public final synchronized RubySymbol getSymbol34(Ruby runtime, String symbol) {
        if (symbol34 == null) {
            symbol34 = runtime.fastNewSymbol(symbol);
        }
        return symbol34;
    }
    
    public final synchronized RubySymbol getSymbol35(Ruby runtime, String symbol) {
        if (symbol35 == null) {
            symbol35 = runtime.fastNewSymbol(symbol);
        }
        return symbol35;
    }
    
    public final synchronized RubySymbol getSymbol36(Ruby runtime, String symbol) {
        if (symbol36 == null) {
            symbol36 = runtime.fastNewSymbol(symbol);
        }
        return symbol36;
    }
    
    public final synchronized RubySymbol getSymbol37(Ruby runtime, String symbol) {
        if (symbol37 == null) {
            symbol37 = runtime.fastNewSymbol(symbol);
        }
        return symbol37;
    }
    
    public final synchronized RubySymbol getSymbol38(Ruby runtime, String symbol) {
        if (symbol38 == null) {
            symbol38 = runtime.fastNewSymbol(symbol);
        }
        return symbol38;
    }
    
    public final synchronized RubySymbol getSymbol39(Ruby runtime, String symbol) {
        if (symbol39 == null) {
            symbol39 = runtime.fastNewSymbol(symbol);
        }
        return symbol39;
    }
    
    public final synchronized RubySymbol getSymbol40(Ruby runtime, String symbol) {
        if (symbol40 == null) {
            symbol40 = runtime.fastNewSymbol(symbol);
        }
        return symbol40;
    }
    
    public final synchronized RubySymbol getSymbol41(Ruby runtime, String symbol) {
        if (symbol41 == null) {
            symbol41 = runtime.fastNewSymbol(symbol);
        }
        return symbol41;
    }
    
    public final synchronized RubySymbol getSymbol42(Ruby runtime, String symbol) {
        if (symbol42 == null) {
            symbol42 = runtime.fastNewSymbol(symbol);
        }
        return symbol42;
    }
    
    public final synchronized RubySymbol getSymbol43(Ruby runtime, String symbol) {
        if (symbol43 == null) {
            symbol43 = runtime.fastNewSymbol(symbol);
        }
        return symbol43;
    }
    
    public final synchronized RubySymbol getSymbol44(Ruby runtime, String symbol) {
        if (symbol44 == null) {
            symbol44 = runtime.fastNewSymbol(symbol);
        }
        return symbol44;
    }
    
    public final synchronized RubySymbol getSymbol45(Ruby runtime, String symbol) {
        if (symbol45 == null) {
            symbol45 = runtime.fastNewSymbol(symbol);
        }
        return symbol45;
    }
    
    public final synchronized RubySymbol getSymbol46(Ruby runtime, String symbol) {
        if (symbol46 == null) {
            symbol46 = runtime.fastNewSymbol(symbol);
        }
        return symbol46;
    }
    
    public final synchronized RubySymbol getSymbol47(Ruby runtime, String symbol) {
        if (symbol47 == null) {
            symbol47 = runtime.fastNewSymbol(symbol);
        }
        return symbol47;
    }
    
    public final synchronized RubySymbol getSymbol48(Ruby runtime, String symbol) {
        if (symbol48 == null) {
            symbol48 = runtime.fastNewSymbol(symbol);
        }
        return symbol48;
    }
    
    public final synchronized RubySymbol getSymbol49(Ruby runtime, String symbol) {
        if (symbol49 == null) {
            symbol49 = runtime.fastNewSymbol(symbol);
        }
        return symbol49;
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
    
    public CallSite site0;
    public CallSite site1;
    public CallSite site2;
    public CallSite site3;
    public CallSite site4;
    public CallSite site5;
    public CallSite site6;
    public CallSite site7;
    public CallSite site8;
    public CallSite site9;
    public CallSite site10;
    public CallSite site11;
    public CallSite site12;
    public CallSite site13;
    public CallSite site14;
    public CallSite site15;
    public CallSite site16;
    public CallSite site17;
    public CallSite site18;
    public CallSite site19;
    public CallSite site20;
    public CallSite site21;
    public CallSite site22;
    public CallSite site23;
    public CallSite site24;
    public CallSite site25;
    public CallSite site26;
    public CallSite site27;
    public CallSite site28;
    public CallSite site29;
    public CallSite site30;
    public CallSite site31;
    public CallSite site32;
    public CallSite site33;
    public CallSite site34;
    public CallSite site35;
    public CallSite site36;
    public CallSite site37;
    public CallSite site38;
    public CallSite site39;
    public CallSite site40;
    public CallSite site41;
    public CallSite site42;
    public CallSite site43;
    public CallSite site44;
    public CallSite site45;
    public CallSite site46;
    public CallSite site47;
    public CallSite site48;
    public CallSite site49;
    
    public RubySymbol symbol0;
    public RubySymbol symbol1;
    public RubySymbol symbol2;
    public RubySymbol symbol3;
    public RubySymbol symbol4;
    public RubySymbol symbol5;
    public RubySymbol symbol6;
    public RubySymbol symbol7;
    public RubySymbol symbol8;
    public RubySymbol symbol9;
    public RubySymbol symbol10;
    public RubySymbol symbol11;
    public RubySymbol symbol12;
    public RubySymbol symbol13;
    public RubySymbol symbol14;
    public RubySymbol symbol15;
    public RubySymbol symbol16;
    public RubySymbol symbol17;
    public RubySymbol symbol18;
    public RubySymbol symbol19;
    public RubySymbol symbol20;
    public RubySymbol symbol21;
    public RubySymbol symbol22;
    public RubySymbol symbol23;
    public RubySymbol symbol24;
    public RubySymbol symbol25;
    public RubySymbol symbol26;
    public RubySymbol symbol27;
    public RubySymbol symbol28;
    public RubySymbol symbol29;
    public RubySymbol symbol30;
    public RubySymbol symbol31;
    public RubySymbol symbol32;
    public RubySymbol symbol33;
    public RubySymbol symbol34;
    public RubySymbol symbol35;
    public RubySymbol symbol36;
    public RubySymbol symbol37;
    public RubySymbol symbol38;
    public RubySymbol symbol39;
    public RubySymbol symbol40;
    public RubySymbol symbol41;
    public RubySymbol symbol42;
    public RubySymbol symbol43;
    public RubySymbol symbol44;
    public RubySymbol symbol45;
    public RubySymbol symbol46;
    public RubySymbol symbol47;
    public RubySymbol symbol48;
    public RubySymbol symbol49;
    
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

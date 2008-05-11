/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.executable;

import org.jruby.Ruby;
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
    
    public synchronized RubySymbol getSymbol0(Ruby runtime, String symbol) {
        if (symbol0 == null) {
            symbol0 = runtime.fastNewSymbol(symbol);
        }
        return symbol0;
    }
    
    public synchronized RubySymbol getSymbol1(Ruby runtime, String symbol) {
        if (symbol1 == null) {
            symbol1 = runtime.fastNewSymbol(symbol);
        }
        return symbol1;
    }
    
    public synchronized RubySymbol getSymbol2(Ruby runtime, String symbol) {
        if (symbol2 == null) {
            symbol2 = runtime.fastNewSymbol(symbol);
        }
        return symbol2;
    }
    
    public synchronized RubySymbol getSymbol3(Ruby runtime, String symbol) {
        if (symbol3 == null) {
            symbol3 = runtime.fastNewSymbol(symbol);
        }
        return symbol3;
    }
    
    public synchronized RubySymbol getSymbol4(Ruby runtime, String symbol) {
        if (symbol4 == null) {
            symbol4 = runtime.fastNewSymbol(symbol);
        }
        return symbol4;
    }
    
    public synchronized RubySymbol getSymbol5(Ruby runtime, String symbol) {
        if (symbol5 == null) {
            symbol5 = runtime.fastNewSymbol(symbol);
        }
        return symbol5;
    }
    
    public synchronized RubySymbol getSymbol6(Ruby runtime, String symbol) {
        if (symbol6 == null) {
            symbol6 = runtime.fastNewSymbol(symbol);
        }
        return symbol6;
    }
    
    public synchronized RubySymbol getSymbol7(Ruby runtime, String symbol) {
        if (symbol7 == null) {
            symbol7 = runtime.fastNewSymbol(symbol);
        }
        return symbol7;
    }
    
    public synchronized RubySymbol getSymbol8(Ruby runtime, String symbol) {
        if (symbol8 == null) {
            symbol8 = runtime.fastNewSymbol(symbol);
        }
        return symbol8;
    }
    
    public synchronized RubySymbol getSymbol9(Ruby runtime, String symbol) {
        if (symbol9 == null) {
            symbol9 = runtime.fastNewSymbol(symbol);
        }
        return symbol9;
    }
    
    public synchronized RubySymbol getSymbol10(Ruby runtime, String symbol) {
        if (symbol10 == null) {
            symbol10 = runtime.fastNewSymbol(symbol);
        }
        return symbol10;
    }
    
    public synchronized RubySymbol getSymbol11(Ruby runtime, String symbol) {
        if (symbol11 == null) {
            symbol11 = runtime.fastNewSymbol(symbol);
        }
        return symbol11;
    }
    
    public synchronized RubySymbol getSymbol12(Ruby runtime, String symbol) {
        if (symbol12 == null) {
            symbol12 = runtime.fastNewSymbol(symbol);
        }
        return symbol12;
    }
    
    public synchronized RubySymbol getSymbol13(Ruby runtime, String symbol) {
        if (symbol13 == null) {
            symbol13 = runtime.fastNewSymbol(symbol);
        }
        return symbol13;
    }
    
    public synchronized RubySymbol getSymbol14(Ruby runtime, String symbol) {
        if (symbol14 == null) {
            symbol14 = runtime.fastNewSymbol(symbol);
        }
        return symbol14;
    }
    
    public synchronized RubySymbol getSymbol15(Ruby runtime, String symbol) {
        if (symbol15 == null) {
            symbol15 = runtime.fastNewSymbol(symbol);
        }
        return symbol15;
    }
    
    public synchronized RubySymbol getSymbol16(Ruby runtime, String symbol) {
        if (symbol16 == null) {
            symbol16 = runtime.fastNewSymbol(symbol);
        }
        return symbol16;
    }
    
    public synchronized RubySymbol getSymbol17(Ruby runtime, String symbol) {
        if (symbol17 == null) {
            symbol17 = runtime.fastNewSymbol(symbol);
        }
        return symbol17;
    }
    
    public synchronized RubySymbol getSymbol18(Ruby runtime, String symbol) {
        if (symbol18 == null) {
            symbol18 = runtime.fastNewSymbol(symbol);
        }
        return symbol18;
    }
    
    public synchronized RubySymbol getSymbol19(Ruby runtime, String symbol) {
        if (symbol19 == null) {
            symbol19 = runtime.fastNewSymbol(symbol);
        }
        return symbol19;
    }
    
    public synchronized RubySymbol getSymbol20(Ruby runtime, String symbol) {
        if (symbol20 == null) {
            symbol20 = runtime.fastNewSymbol(symbol);
        }
        return symbol20;
    }
    
    public synchronized RubySymbol getSymbol21(Ruby runtime, String symbol) {
        if (symbol21 == null) {
            symbol21 = runtime.fastNewSymbol(symbol);
        }
        return symbol21;
    }
    
    public synchronized RubySymbol getSymbol22(Ruby runtime, String symbol) {
        if (symbol22 == null) {
            symbol22 = runtime.fastNewSymbol(symbol);
        }
        return symbol22;
    }
    
    public synchronized RubySymbol getSymbol23(Ruby runtime, String symbol) {
        if (symbol23 == null) {
            symbol23 = runtime.fastNewSymbol(symbol);
        }
        return symbol23;
    }
    
    public synchronized RubySymbol getSymbol24(Ruby runtime, String symbol) {
        if (symbol24 == null) {
            symbol24 = runtime.fastNewSymbol(symbol);
        }
        return symbol24;
    }
    
    public synchronized RubySymbol getSymbol25(Ruby runtime, String symbol) {
        if (symbol25 == null) {
            symbol25 = runtime.fastNewSymbol(symbol);
        }
        return symbol25;
    }
    
    public synchronized RubySymbol getSymbol26(Ruby runtime, String symbol) {
        if (symbol26 == null) {
            symbol26 = runtime.fastNewSymbol(symbol);
        }
        return symbol26;
    }
    
    public synchronized RubySymbol getSymbol27(Ruby runtime, String symbol) {
        if (symbol27 == null) {
            symbol27 = runtime.fastNewSymbol(symbol);
        }
        return symbol27;
    }
    
    public synchronized RubySymbol getSymbol28(Ruby runtime, String symbol) {
        if (symbol28 == null) {
            symbol28 = runtime.fastNewSymbol(symbol);
        }
        return symbol28;
    }
    
    public synchronized RubySymbol getSymbol29(Ruby runtime, String symbol) {
        if (symbol29 == null) {
            symbol29 = runtime.fastNewSymbol(symbol);
        }
        return symbol29;
    }
    
    public synchronized RubySymbol getSymbol30(Ruby runtime, String symbol) {
        if (symbol30 == null) {
            symbol30 = runtime.fastNewSymbol(symbol);
        }
        return symbol30;
    }
    
    public synchronized RubySymbol getSymbol31(Ruby runtime, String symbol) {
        if (symbol31 == null) {
            symbol31 = runtime.fastNewSymbol(symbol);
        }
        return symbol31;
    }
    
    public synchronized RubySymbol getSymbol32(Ruby runtime, String symbol) {
        if (symbol32 == null) {
            symbol32 = runtime.fastNewSymbol(symbol);
        }
        return symbol32;
    }
    
    public synchronized RubySymbol getSymbol33(Ruby runtime, String symbol) {
        if (symbol33 == null) {
            symbol33 = runtime.fastNewSymbol(symbol);
        }
        return symbol33;
    }
    
    public synchronized RubySymbol getSymbol34(Ruby runtime, String symbol) {
        if (symbol34 == null) {
            symbol34 = runtime.fastNewSymbol(symbol);
        }
        return symbol34;
    }
    
    public synchronized RubySymbol getSymbol35(Ruby runtime, String symbol) {
        if (symbol35 == null) {
            symbol35 = runtime.fastNewSymbol(symbol);
        }
        return symbol35;
    }
    
    public synchronized RubySymbol getSymbol36(Ruby runtime, String symbol) {
        if (symbol36 == null) {
            symbol36 = runtime.fastNewSymbol(symbol);
        }
        return symbol36;
    }
    
    public synchronized RubySymbol getSymbol37(Ruby runtime, String symbol) {
        if (symbol37 == null) {
            symbol37 = runtime.fastNewSymbol(symbol);
        }
        return symbol37;
    }
    
    public synchronized RubySymbol getSymbol38(Ruby runtime, String symbol) {
        if (symbol38 == null) {
            symbol38 = runtime.fastNewSymbol(symbol);
        }
        return symbol38;
    }
    
    public synchronized RubySymbol getSymbol39(Ruby runtime, String symbol) {
        if (symbol39 == null) {
            symbol39 = runtime.fastNewSymbol(symbol);
        }
        return symbol39;
    }
    
    public synchronized RubySymbol getSymbol40(Ruby runtime, String symbol) {
        if (symbol40 == null) {
            symbol40 = runtime.fastNewSymbol(symbol);
        }
        return symbol40;
    }
    
    public synchronized RubySymbol getSymbol41(Ruby runtime, String symbol) {
        if (symbol41 == null) {
            symbol41 = runtime.fastNewSymbol(symbol);
        }
        return symbol41;
    }
    
    public synchronized RubySymbol getSymbol42(Ruby runtime, String symbol) {
        if (symbol42 == null) {
            symbol42 = runtime.fastNewSymbol(symbol);
        }
        return symbol42;
    }
    
    public synchronized RubySymbol getSymbol43(Ruby runtime, String symbol) {
        if (symbol43 == null) {
            symbol43 = runtime.fastNewSymbol(symbol);
        }
        return symbol43;
    }
    
    public synchronized RubySymbol getSymbol44(Ruby runtime, String symbol) {
        if (symbol44 == null) {
            symbol44 = runtime.fastNewSymbol(symbol);
        }
        return symbol44;
    }
    
    public synchronized RubySymbol getSymbol45(Ruby runtime, String symbol) {
        if (symbol45 == null) {
            symbol45 = runtime.fastNewSymbol(symbol);
        }
        return symbol45;
    }
    
    public synchronized RubySymbol getSymbol46(Ruby runtime, String symbol) {
        if (symbol46 == null) {
            symbol46 = runtime.fastNewSymbol(symbol);
        }
        return symbol46;
    }
    
    public synchronized RubySymbol getSymbol47(Ruby runtime, String symbol) {
        if (symbol47 == null) {
            symbol47 = runtime.fastNewSymbol(symbol);
        }
        return symbol47;
    }
    
    public synchronized RubySymbol getSymbol48(Ruby runtime, String symbol) {
        if (symbol48 == null) {
            symbol48 = runtime.fastNewSymbol(symbol);
        }
        return symbol48;
    }
    
    public synchronized RubySymbol getSymbol49(Ruby runtime, String symbol) {
        if (symbol49 == null) {
            symbol49 = runtime.fastNewSymbol(symbol);
        }
        return symbol49;
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
}

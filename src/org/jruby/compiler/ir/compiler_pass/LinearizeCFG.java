package org.jruby.compiler.ir.compiler_pass;

import java.util.List;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.BasicBlock;

public class LinearizeCFG implements CompilerPass
{
    public LinearizeCFG() { }

    public boolean isPreOrder()  { return true; }

    public void run(IR_Scope s) { 
        if (s instanceof IR_ExecutionScope) {
            System.out.println("Linearizing cfg for " + s);
            List<BasicBlock> bbs = ((IR_ExecutionScope)s).getCFG().linearize();
            StringBuffer buf = new StringBuffer();
            for (BasicBlock b : bbs) {
                buf.append(b.toStringInstrs());
            }
            System.out.print(buf);
        }
    }
}

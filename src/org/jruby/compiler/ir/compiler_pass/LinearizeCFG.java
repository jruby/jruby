package org.jruby.compiler.ir.compiler_pass;

import java.util.List;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.BasicBlock;

public class LinearizeCFG implements CompilerPass
{
    public LinearizeCFG() { }

    public boolean isPreOrder()  { return true; }

    public void run(IRScope s) {
        if (s instanceof IRExecutionScope) {
//            System.out.println("Linearizing cfg for " + s);
				CFG cfg = ((IRExecutionScope)s).getCFG();
            List<BasicBlock> bbs = cfg.linearize();
/*
            StringBuffer buf = new StringBuffer();
            for (BasicBlock b : bbs) {
                buf.append(b.toStringInstrs());
            }
            System.out.print(buf);
*/
        }
    }
}

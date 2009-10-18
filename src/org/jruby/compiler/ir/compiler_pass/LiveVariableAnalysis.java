package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class LiveVariableAnalysis implements CompilerPass
{
    public LiveVariableAnalysis() { }

    public boolean isPreOrder() { return false; }

    public void run(IR_Scope s)
    {
        if (!(s instanceof IR_Method))
            return;

        CFG c = ((IR_Method)s).getCFG();
        LiveVariablesProblem lvp = new LiveVariablesProblem();
        lvp.setup(c);
        lvp.compute_MOP_Solution();
        c.setLVP(lvp);
    }
}

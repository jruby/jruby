package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;

public class LiveVariableAnalysis implements CompilerPass
{
    public LiveVariableAnalysis() { }

    public boolean isPreOrder() { return false; }

    public void run(IRScope s)
    {
        if (!(s instanceof IRMethod))
            return;

        CFG c = ((IRMethod)s).getCFG();
        LiveVariablesProblem lvp = new LiveVariablesProblem();
		  String lvpName = lvp.getName();
        lvp.setup(c);
        lvp.compute_MOP_Solution();
        c.setDataFlowSolution(lvp.getName(), lvp);
//        System.out.println("LVP for " + s + " is: " + lvp);
        for (IRClosure x: ((IRMethod)s).getClosures()) {
			  CFG xc = x.getCFG();
			  if (xc != null)
				  lvp = (LiveVariablesProblem)xc.getDataFlowSolution(lvpName);
           else
				  System.out.println("Null cfg for: " + x);
/*
           System.out.println("LVP for closure: " + x + " is: " + lvp);
           System.out.println("Live on entry:");
           for (Variable v: lvp.getVarsLiveOnEntry())
              System.out.print(" " + v);
           System.out.println("\nLive on exit:");
           for (Variable v: lvp.getVarsLiveOnExit())
              System.out.print(" " + v);
           System.out.println("");
**/
        }
    }
}

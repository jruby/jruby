package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class LiveVariableAnalysis implements CompilerPass {
    private static final Logger LOG = LoggerFactory.getLogger("LiveVariableAnalysis");

    public boolean isPreOrder() {
        return false;
    }

    public void run(IRScope s) {
        if (!(s instanceof IRMethod)) return;

        CFG cfg = ((IRMethod) s).getCFG();
        LiveVariablesProblem lvp = new LiveVariablesProblem();
        String lvpName = lvp.getName();
        
        lvp.setup(cfg);
        lvp.compute_MOP_Solution();
        cfg.setDataFlowSolution(lvp.getName(), lvp);
//        System.out.println("LVP for " + s + " is: " + lvp);
        for (IRClosure x: ((IRMethod) s).getClosures()) {
            CFG closureXFG = x.getCFG();
            if (closureXFG != null) {
                lvp = (LiveVariablesProblem) closureXFG.getDataFlowSolution(lvpName);
            } else {
                LOG.debug("Null cfg for: " + x);
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
}

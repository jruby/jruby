package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.Set;

// This problem tries to find places to insert binding stores -- for spilling local variables onto a heap store
// It does better than spilling all local variables to the heap at all call sites.  This is similar to a
// available expressions analysis in that it tries to propagate availability of stores through the flow graph.
//
// We have piggybacked the problem of identifying sites where binding allocation instrutions are necessary.  So,
// strictly speaking, this is a AND of two independent dataflow analyses -- we are doing these together for
// efficiency reasons, and also because the binding allocation problem is also a forwards flow problem and is a
// relatively straightforward analysis.
public class BindingStorePlacementProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public BindingStorePlacementProblem()       
    { 
        super(DataFlowProblem.DF_Direction.FORWARD); 
    }

    public String        getName() { return "Binding Stores Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new BindingStorePlacementNode(this, bb);  }
    @Override
    public String        getDataFlowVarsForOutput() { return ""; }

    public boolean scopeDefinesVariable(Variable v) { 
        return getCFG().definesLocalVariable(v);
    }

    public boolean scopeUsesVariable(Variable v) { 
        return getCFG().usesLocalVariable(v);
    }

    public void addStoreAndBindingAllocInstructions()
    {
        for (FlowGraphNode n: _fgNodes) {
            BindingStorePlacementNode bspn = (BindingStorePlacementNode)n;
            bspn.addStoreAndBindingAllocInstructions();
        }
    }
}

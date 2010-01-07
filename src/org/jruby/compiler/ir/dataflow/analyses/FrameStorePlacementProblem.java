package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.Set;

// This problem tries to find places to insert frame stores -- for spilling local variables onto a heap store
// It does better than spilling all local variables to the heap at all call sites.  This is similar to a
// available expressions analysis in that it tries to propagate availability of stores through the flow graph.
//
// We have piggybacked the problem of identifying sites where frame allocation instrutions are necessary.  So,
// strictly speaking, this is a AND of two independent dataflow analyses -- we are doing these together for
// efficiency reasons, and also because the frame allocation problem is also a forwards flow problem and is a
// relatively straightforward analysis.
public class FrameStorePlacementProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public FrameStorePlacementProblem()       
    { 
        super(DataFlowProblem.DF_Direction.FORWARD); 
        _usedVars = new java.util.HashSet<Variable>(); 
        _defVars = new java.util.HashSet<Variable>();
    }

    public String        getName() { return "Frame Stores Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new FrameStorePlacementNode(this, bb);  }
    @Override
    public String        getDataFlowVarsForOutput() { return ""; }
    public void          recordUsedVar(Variable v) { _usedVars.add(v); }
    public void          recordDefVar(Variable v) { _defVars.add(v); }

    public boolean scopeDefinesVariable(Variable v) { 
        if (_defVars.contains(v)) {
            return true;
        }
        else {
            for (IR_Closure cl: getCFG().getScope().getClosures()) {
                FrameStorePlacementProblem nestedProblem = (FrameStorePlacementProblem)cl.getCFG().getDataFlowSolution(DataFlowConstants.FSP_NAME);
                if (nestedProblem.scopeDefinesVariable(v)) 
                    return true;
            }

            return false;
        }
    }

    public boolean scopeUsesVariable(Variable v) { 
        if (_usedVars.contains(v)) {
            return true;
        }
        else {
            for (IR_Closure cl: getCFG().getScope().getClosures()) {
                FrameStorePlacementProblem nestedProblem = (FrameStorePlacementProblem)cl.getCFG().getDataFlowSolution(DataFlowConstants.FSP_NAME);
                if (nestedProblem.scopeUsesVariable(v)) 
                    return true;
            }

            return false;
        }
    }

    public void addStoreAndFrameAllocInstructions()
    {
        for (FlowGraphNode n: _fgNodes) {
            FrameStorePlacementNode fspn = (FrameStorePlacementNode)n;
            fspn.addStoreAndFrameAllocInstructions();
        }
    }

/* ----------- Private Interface ------------ */
    private Set<Variable> _usedVars;    // Variables used in this scope
    private Set<Variable> _defVars;     // Variables defined in this scope
}

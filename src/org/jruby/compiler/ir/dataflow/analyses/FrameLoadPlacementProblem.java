package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.Set;

public class FrameLoadPlacementProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public FrameLoadPlacementProblem()
    { 
        super(DataFlowProblem.DF_Direction.BACKWARD);
        _initLoads = new java.util.HashSet<Variable>();
        _defVars = new java.util.HashSet<Variable>();
    }

    public String getName()                  { return "Frame Loads Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new FrameLoadPlacementNode(this, bb);  }
    public String        getDataFlowVarsForOutput() { return ""; }
    public void          initNestedProblem(Set<Variable> neededLoads) { _initLoads = neededLoads; }
    public Set<Variable> getNestedProblemInitLoads() { return _initLoads; }
    public void          recordDefVar(Variable v) { _defVars.add(v); }
    public boolean       scopeDefinesVariable(Variable v) { return _defVars.contains(v); } 

    public void addLoads()
    {
        for (FlowGraphNode n: _fgNodes)
           ((FrameLoadPlacementNode)n).addLoads();
    }

/* ----------- Private Interface ------------ */
    private Set<Variable> _initLoads;
    private Set<Variable> _defVars;      // Variables defined in this scope
}

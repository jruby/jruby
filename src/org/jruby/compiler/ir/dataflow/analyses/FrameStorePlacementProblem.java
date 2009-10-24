package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.Set;

public class FrameStorePlacementProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public FrameStorePlacementProblem()       
    { 
        super(DataFlowProblem.DF_Direction.FORWARD); 
        _initStores = new java.util.HashSet<Variable>(); 
        _udVars = new java.util.HashSet<Variable>(); 
    }

    public String getName()                   { return "Frame Stores Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new FrameStorePlacementNode(this, bb);  }
    public String        getDataFlowVarsForOutput() { return ""; }
    public void          initNestedProblem(Set<Variable> neededStores) { _initStores = neededStores; }
    public Set<Variable> getNestedProblemInitStores() { return _initStores; }
    public void          recordUseDefVar(Variable v) { _udVars.add(v); }
    public boolean       scopeDefinesOrUsesVariable(Variable v) { return _udVars.contains(v); } 

    public void addStores()
    {
        for (FlowGraphNode n: _fgNodes)
           ((FrameStorePlacementNode)n).addStores();
    }

/* ----------- Private Interface ------------ */
    private Set<Variable> _initStores;  // Stores that need to be performed at entrance of the cfg -- non-null only for closures 
    private Set<Variable> _udVars;      // Variables used/defined in this scope
}

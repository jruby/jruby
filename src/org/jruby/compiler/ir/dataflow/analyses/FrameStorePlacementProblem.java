package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.STORE_TO_FRAME_Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class FrameStorePlacementProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public FrameStorePlacementProblem()       
    { 
        super(DataFlowProblem.DF_Direction.FORWARD); 
        _initStores = new java.util.HashSet<Variable>(); 
        _usedVars = new java.util.HashSet<Variable>(); 
        _defVars = new java.util.HashSet<Variable>();
    }

    public String getName()                   { return "Frame Stores Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new FrameStorePlacementNode(this, bb);  }
    public String        getDataFlowVarsForOutput() { return ""; }
    public void          initNestedProblem(Set<Variable> neededStores) { _initStores = neededStores; }
    public Set<Variable> getNestedProblemInitStores() { return _initStores; }
    public void          recordUsedVar(Variable v) { _usedVars.add(v); }
    public void          recordDefVar(Variable v) { _defVars.add(v); }
    public boolean       scopeDefinesOrUsesVariable(Variable v) { return _usedVars.contains(v) || _defVars.contains(v); } 
    public boolean       scopeDefinesVariable(Variable v) { return _defVars.contains(v); } 

    public void addStores()
    {
        // In the dataflow problem, compute_MEET has to use Union of store sets of predecessors.
        // But, we are instead using Intersection of store sets.  
        //
        // So, while adding stores below, we have to adding any missing stores on each path.  
        // For a basic block b, We do this as follows:
        //   Compute DIFF = OUT(b) - INTERSECTION(IN(p), for all dataflow successors p of b) 
        // For all variables in diff, add a store at the end of b
        for (FlowGraphNode n: _fgNodes) {
            FrameStorePlacementNode fspn = (FrameStorePlacementNode)n;
            fspn.addStores();

            Set<Variable> x = null;
            for (CFG_Edge e: outgoingEdgesOf(fspn.getBB())) {
                FrameStorePlacementNode p = (FrameStorePlacementNode)getFlowGraphNode(e._src);
                if (x == null)
                    x = new HashSet<Variable>(p._inDirtyVars);
                else
                    x.retainAll(p._inDirtyVars);
            }

            Set<Variable> diff = new HashSet<Variable>(fspn._outDirtyVars);
            if (x != null)
                diff.removeAll(x);

            // Add loads for all variables in ls 
            if (!diff.isEmpty()) {
                IR_ExecutionScope s = getCFG().getScope();
                List<IR_Instr> instrs = fspn.getBB().getInstrs();
                ListIterator<IR_Instr> it = instrs.listIterator(instrs.size());  // Go to end of BB
                for (Variable v: diff)
                    it.add(new STORE_TO_FRAME_Instr(s, v._name, v));
            }
        }
    }

/* ----------- Private Interface ------------ */
    private Set<Variable> _initStores;  // Stores that need to be performed at entrance of the cfg -- non-null only for closures 
    private Set<Variable> _usedVars;      // Variables used in this scope
    private Set<Variable> _defVars;     // Variables defined in this scope
}

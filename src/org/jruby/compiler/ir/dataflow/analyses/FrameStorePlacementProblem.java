package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
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
        _initStores = new java.util.HashSet<Variable>(); 
        _usedVars = new java.util.HashSet<Variable>(); 
        _defVars = new java.util.HashSet<Variable>();
    }

    public String        getName() { return "Frame Stores Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new FrameStorePlacementNode(this, bb);  }
    public String        getDataFlowVarsForOutput() { return ""; }
    public Set<Variable> getNestedProblemInitStores() { return _initStores; }
    public void          recordUsedVar(Variable v) { _usedVars.add(v); }
    public void          recordDefVar(Variable v) { _defVars.add(v); }
    public void          initNestedProblem(Set<Variable> neededStores) { _initStores = neededStores; }

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

    public boolean scopeDefinesOrUsesVariable(Variable v) { 
        if (_usedVars.contains(v) || _defVars.contains(v)) {
            return true;
        }
        else {
            for (IR_Closure cl: getCFG().getScope().getClosures()) {
                FrameStorePlacementProblem nestedProblem = (FrameStorePlacementProblem)cl.getCFG().getDataFlowSolution(DataFlowConstants.FSP_NAME);
                if (nestedProblem.scopeDefinesOrUsesVariable(v)) 
                    return true;
            }

            return false;
        }
    }

    public void addStoreAndFrameAllocInstructions()
    {
        // In the dataflow problem, compute_MEET has to use Union of store sets of predecessors.
        // But, we are instead using Intersection of store sets.  
        //
        //         A     B
        //          \   /
        //            C
        //
        // In the above example, C will only add stores that are present at end of both A & B.
        // This means that at the end of A & B , we need to add any stores that were omitted in C!
        //
        // For reducible control flow graphs (I think Ruby only produces such graphs -- unsure),
        // there will be exactly one df successor for situations where we do need to add these
        // stores.  In the example above, A & B have exactly one df successor C.
        //
        // But, the generic form of the fixup we need to do is as follows:
        // For a basic block b, compute
        //   DIFF = OUT(b) - INTERSECTION(IN(s), for all dataflow successors s of b)
        //
        // For all variables in DIFF, add a store at the end of b

        for (FlowGraphNode n: _fgNodes) {
            FrameStorePlacementNode fspn = (FrameStorePlacementNode)n;
            fspn.addStoreAndFrameAllocInstructions();

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

            // Add loads for all variables in diff 
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
    private Set<Variable> _usedVars;    // Variables used in this scope
    private Set<Variable> _defVars;     // Variables defined in this scope
}

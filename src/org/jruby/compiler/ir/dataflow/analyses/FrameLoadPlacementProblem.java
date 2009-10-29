package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.LOAD_FROM_FRAME_Instr;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

public class FrameLoadPlacementProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public FrameLoadPlacementProblem()
    { 
        super(DataFlowProblem.DF_Direction.BACKWARD);
        _initLoads = new java.util.HashSet<Variable>();
        _defVars = new java.util.HashSet<Variable>();
        _usedVars = new java.util.HashSet<Variable>();
    }

    public String getName()                  { return "Frame Loads Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new FrameLoadPlacementNode(this, bb);  }
    public String        getDataFlowVarsForOutput() { return ""; }
    public void          initNestedProblem(Set<Variable> neededLoads) { _initLoads = neededLoads; }
    public Set<Variable> getNestedProblemInitLoads() { return _initLoads; }
    public void          recordDefVar(Variable v) { _defVars.add(v); }
    public void          recordUsedVar(Variable v) { _usedVars.add(v); }

    public boolean scopeDefinesVariable(Variable v) { 
        if (_defVars.contains(v)) {
            return true;
        }
        else {
            for (IR_Closure cl: getCFG().getScope().getClosures()) {
                FrameLoadPlacementProblem nestedProblem = (FrameLoadPlacementProblem)cl.getCFG().getDataFlowSolution(DataFlowConstants.FLP_NAME);
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
                FrameLoadPlacementProblem nestedProblem = (FrameLoadPlacementProblem)cl.getCFG().getDataFlowSolution(DataFlowConstants.FLP_NAME);
                if (nestedProblem.scopeUsesVariable(v)) 
                    return true;
            }

            return false;
        }
    }

    public void addLoads()
    {
        // In the dataflow problem, compute_MEET has to use Union of load sets of predecessors.
        // But, we are instead using Intersection of load sets.  
        //
        //            A
        //          /   \
        //         B     C
        //
        // In the above example, A will only add loads that are present at beginning of both B & C
        // This means that at the beginning of B & C , we need to add any loads that were omitted in A!
        //
        // For reducible control flow graphs (I think Ruby only produces such graphs -- unsure),
        // there will be exactly one df successor for situations where we do need to add these
        // stores.  In the example above, B & C have exactly one df successor (cfg predecessor) C.
        //
        // But, the generic form of the fixup we need to do is as follows:
        // For every basic block b, compute 
        //   DIFF = OUT(b) - INTERSECTION(IN(s), for all dataflow successors s of b) 
        //
        // For all variables in DIFF, add a load at the beginning of b

        for (FlowGraphNode n: _fgNodes) {
            FrameLoadPlacementNode flpn = (FrameLoadPlacementNode)n;
            flpn.addLoads();

            Set<Variable> x = null;
            for (CFG_Edge e: incomingEdgesOf(flpn.getBB())) {  // This is a reverse df problem ==> dataflow successors = cfg incoming
                FrameLoadPlacementNode p = (FrameLoadPlacementNode)getFlowGraphNode(e._src);
                if (x == null)
                    x = new HashSet<Variable>(p._inReqdLoads);
                else
                    x.retainAll(p._inReqdLoads);
            }

            Set<Variable> diff = new HashSet<Variable>(flpn._outReqdLoads);
            if (x != null)
                diff.removeAll(x);

            // Add loads for all variables in ls 
            if (!diff.isEmpty()) {
                IR_ExecutionScope s = getCFG().getScope();
                ListIterator<IR_Instr> instrs = flpn.getBB().getInstrs().listIterator();
                for (Variable v: diff)
                    instrs.add(new LOAD_FROM_FRAME_Instr(v, s, v._name));
            }
        }
    }

/* ----------- Private Interface ------------ */
    private Set<Variable> _initLoads;
    private Set<Variable> _defVars;      // Variables defined in this scope
    private Set<Variable> _usedVars;     // Variables used in this scope
}

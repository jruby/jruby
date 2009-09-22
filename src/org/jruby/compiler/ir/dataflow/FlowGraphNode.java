package org.jruby.compiler.ir.dataflow;

import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;
import org.jruby.compiler.ir.instructions.IR_Instr;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/* This framework right now implicitly uses the CFG as the flow graph -- perhaps it is worth abstracting away from this assumption
 * so that we can use this framework over non-CFG flow graphs.
 *
 * In any case, till such time we generalize this to other kinds of flow graphs, a flow graph node is just a wrapper over a CFG node,
 * the basic block.  As such, a flow graph is not explicitly constructed.
 *
 * Different dataflow problems encapsulate different dataflow properties, and a different flow graph node is built in each case */
abstract public class FlowGraphNode
{
    public FlowGraphNode(DataFlowProblem p, BasicBlock n) { _prob = p; _bb = n; }
   
/* ----------- Public abstract methods ---------- */
    /** Initialize this data flow node to compute the new solution
        This is done before iteratively calling the MEET operator. */
    public abstract void    initSolnForNode();

    /** "MEET" current solution of "IN/OUT" with "OUT/IN(pred)",
        where "pred" is a predecessor of the current node!
        The choice of "IN/OUT" is determined by the direction of data flow. */
    public abstract void    compute_MEET(CFG_Edge edge, FlowGraphNode pred);

    /** Compute "OUT/IN" for the current node!  The choice of "IN/OUT" is determined by the direction of data flow. 
     *  OUT/IN = transfer-function(facts at start/end of node, instructions of current node processed in fwd/reverse dirn) */
    public abstract boolean applyTransferFunction();

    /** Builds the data-flow variables (or facts) for a particular instruction. */
    public abstract void buildDataFlowVars(IR_Instr i);

/* ----------- Public methods with a default implementation ---------- */
    /** Initialize this data flow node for solving the current problem
        This is done after building dataflow variables for the problem. */
    public void init() { }

    /** After meet has been performed, do some more logic. */
    public void finalizeSolnForNode() {};

    /** Builds the data-flow variables (or facts) for a particular node.
        Need only create the DF_Var for them to be added to the  problem. 
        Goes over the instructions in this basic block and collect 
        all relevant LOCAL data flow vars for this problem! */
    public void buildDataFlowVars()
    {
        for (IR_Instr i: _bb.getInstrs())
            buildDataFlowVars(i);
    }

    public void computeDataFlowInfo(List<FlowGraphNode> workList, BitSet bbSet)
    {
        bbSet.clear(_bb.getID());
   
        // Compute meet over all "sources" and compute "destination" basic blocks that should then be processed. 
        // sources & targets depends on direction of the data flow problem
        List<BasicBlock> dsts = new ArrayList<BasicBlock>();
        initSolnForNode();
        if (_prob.getFlowDirection() == DataFlowProblem.DF_Direction.FORWARD) {
            for (CFG_Edge e: _prob.incomingEdgesOf(_bb))
                compute_MEET(e, _prob.getFlowGraphNode(e._src));
            for (CFG_Edge e: _prob.outgoingEdgesOf(_bb))
                dsts.add(e._dst);
        }
        else if (_prob.getFlowDirection() == DataFlowProblem.DF_Direction.BACKWARD) {
            for (CFG_Edge e: _prob.outgoingEdgesOf(_bb))
                compute_MEET(e, _prob.getFlowGraphNode(e._dst));
            for (CFG_Edge e: _prob.incomingEdgesOf(_bb))
                dsts.add(e._src);
        }
        else {
            throw new RuntimeException("Bidirectional data flow computation not implemented yet!");
        }

        finalizeSolnForNode();

       // If the solution has changed, add "dsts" to the work list.
       // No duplicates please which is why we have bbset.
        boolean changed = applyTransferFunction();
        if (changed) {
            for (BasicBlock d: dsts) {
                int id = d.getID();
                if (bbSet.get(id) == false) {
                    bbSet.set(id);
                    workList.add(_prob.getFlowGraphNode(d));
                }
            }
        }
    }

/* --------- Protected fields/methods below --------- */
    protected DataFlowProblem _prob;   // Dataflow problem with which this node is associated
    protected BasicBlock _bb;   			// CFG node for which this node contains info.
}

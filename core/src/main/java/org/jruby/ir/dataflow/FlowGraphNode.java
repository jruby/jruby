package org.jruby.ir.dataflow;

import java.util.BitSet;
import java.util.List;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.util.Edge;

/* This framework right now implicitly uses the CFG as the flow graph -- perhaps it is worth abstracting away from this assumption
 * so that we can use this framework over non-CFG flow graphs.
 *
 * In any case, till such time we generalize this to other kinds of flow graphs, a flow graph node is just a wrapper over a CFG node,
 * the basic block.  As such, a flow graph is not explicitly constructed.
 *
 * Different dataflow problems encapsulate different dataflow properties, and a different flow graph node is built in each case */
public abstract class FlowGraphNode {
    public FlowGraphNode(DataFlowProblem p, BasicBlock n) {
        problem = p;
        basicBlock = n;
        rescuer = problem.getScope().cfg().getRescuerBBFor(basicBlock);
    }

    /**
     * Initialize this data flow node to compute the new solution
     * This is done before iteratively calling the MEET operator.
     */
    public abstract void initSolnForNode();

    /**
     * "MEET" current solution of "IN/OUT" with "OUT/IN(pred)", where "pred"
     * is a predecessor of the current node!  The choice of "IN/OUT" is
     * determined by the direction of data flow.
     */
    public abstract void compute_MEET(Edge e, BasicBlock source, FlowGraphNode pred);

    /** Compute "OUT/IN" for the current node!  The choice of "IN/OUT" is
     * determined by the direction of data flow.  OUT/IN = transfer-function
     * (facts at start/end of node, instructions of current node processed in
     * fwd/reverse dirn)
     */
    public abstract boolean applyTransferFunction();

    /**
     * Builds the data-flow variables (or facts) for a particular instruction.
     */
    public abstract void buildDataFlowVars(Instr i);

    /**
     * Initialize this data flow node for solving the current problem
     * This is done after building dataflow variables for the problem.
     */
    public void init() {
    }

    /**
     * After meet has been performed, do some more logic.
     */
    public void finalizeSolnForNode() {
    }

    public BasicBlock getBB() {
        return basicBlock;
    }

    /** Builds the data-flow variables (or facts) for a particular node.
        Need only create the DF_Var for them to be added to the  problem.
        Goes over the instructions in this basic block and collect
        all relevant LOCAL data flow vars for this problem! */
    public void buildDataFlowVars() {
        for (Instr i: basicBlock.getInstrs()) {
            buildDataFlowVars(i);
        }
    }

    private void processDestBB(List<FlowGraphNode> workList, BitSet bbSet, BasicBlock d) {
        int id = d.getID();
        if (bbSet.get(id) == false) {
            bbSet.set(id);
            workList.add(problem.getFlowGraphNode(d));
        }
    }

    public void computeDataFlowInfo(List<FlowGraphNode> workList, BitSet bbSet) {
        // System.out.println("----- processing bb " + basicBlock.getID() + " -----");
        bbSet.clear(basicBlock.getID());

        // Compute meet over all "sources" and compute "destination" basic blocks that should then be processed.
        // sources & targets depends on direction of the data flow problem
        initSolnForNode();
        if (problem.getFlowDirection() == DataFlowProblem.DF_Direction.FORWARD) {
            for (Edge e: problem.getScope().cfg().getIncomingEdges(basicBlock)) {
                BasicBlock b = (BasicBlock)e.getSource().getData();
                compute_MEET(e, b, problem.getFlowGraphNode(b));
            }
        } else if (problem.getFlowDirection() == DataFlowProblem.DF_Direction.BACKWARD) {
            for (Edge e: problem.getScope().cfg().getOutgoingEdges(basicBlock)) {
                BasicBlock b = (BasicBlock)e.getDestination().getData();
                compute_MEET(e, b, problem.getFlowGraphNode(b));
            }
        } else {
            throw new RuntimeException("Bidirectional data flow computation not implemented yet!");
        }

        finalizeSolnForNode();

       // If the solution has changed, add "dsts" to the work list.
       // No duplicates please which is why we have bbset.
        boolean changed = applyTransferFunction();
        if (changed) {
            if (problem.getFlowDirection() == DataFlowProblem.DF_Direction.FORWARD) {
                for (BasicBlock b: problem.getScope().cfg().getOutgoingDestinations(basicBlock)) {
                    processDestBB(workList, bbSet, b);
                }
            } else if (problem.getFlowDirection() == DataFlowProblem.DF_Direction.BACKWARD) {
                for (BasicBlock b: problem.getScope().cfg().getIncomingSources(basicBlock)) {
                    processDestBB(workList, bbSet, b);
                }
            }
        }
    }

    public boolean hasExceptionsRescued() {
        return rescuer != null;
    }

    public FlowGraphNode getExceptionTargetNode() {
        // If there is a rescue node, on exception, control goes to the rescuer bb.  If not, it goes to the scope exit.
        return problem.getFlowGraphNode(rescuer == null ? problem.getScope().cfg().getExitBB() : rescuer);
    }

    public FlowGraphNode getNonExitBBExceptionTargetNode() {
        // If there is a rescue node, on exception, control goes to the rescuer bb.  If not, it goes to the scope exit.
        return rescuer == null ? null : problem.getFlowGraphNode(rescuer);
    }

/* --------- protected fields/methods below --------- */
    protected DataFlowProblem problem;   // Dataflow problem with which this node is associated
    protected BasicBlock basicBlock;          // CFG node for which this node contains info.
    private   BasicBlock rescuer;   // Basicblock that protects any exceptions raised in this node
}

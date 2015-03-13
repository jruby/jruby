package org.jruby.ir.dataflow;

import org.jruby.dirgra.Edge;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;

/* This framework right now implicitly uses the CFG as the flow graph -- perhaps it is worth abstracting away from this assumption
 * so that we can use this framework over non-CFG flow graphs.
 *
 * In any case, till such time we generalize this to other kinds of flow graphs, a flow graph node is just a wrapper over a CFG node,
 * the basic block.  As such, a flow graph is not explicitly constructed.
 *
 * Different dataflow problems encapsulate different dataflow properties, and a different flow graph node is built in each case */
public abstract class FlowGraphNode<T extends DataFlowProblem<T, U>, U extends FlowGraphNode<T, U>> {
    public FlowGraphNode(T problem, BasicBlock basicBlock) {
        this.problem = problem;
        this.basicBlock = basicBlock;

        // Cache the rescuer node for easy access
        rescuer = problem.getScope().getCFG().getRescuerBBFor(basicBlock);
    }

    /**
     * Builds the data-flow variables (or facts) for a particular instruction.
     */
    public abstract void buildDataFlowVars(Instr i);

    /**
     * Initialize this data flow node for solving the current problem
     * This is done after building dataflow variables for the problem.
     */
    public void init() { }

    /**
     * Initialize this data flow node to compute the new solution
     * This is done before iteratively calling the MEET operator.
     */
    public abstract void applyPreMeetHandler();

    /**
     * "MEET" current solution of "IN/OUT" with "OUT/IN(pred)", where "pred"
     * is a predecessor of the current node!  The choice of "IN/OUT" is
     * determined by the direction of data flow.
     */
    public abstract void compute_MEET(Edge e, U pred);

    /**
     * Any setting up of state/initialization before applying transfer function
     */
    public void initSolution() { }

    /**
     * Apply transfer function to the instruction
     */
    public abstract void applyTransferFunction(Instr i);

    /**
     * Did dataflow solution for this node change from last time?
     */
    public abstract boolean solutionChanged();

    /**
     * Any required cleanup of state after applying transfer function
     */
    public void finalizeSolution() { }

    public BasicBlock getBB() {
        return basicBlock;
    }

    /**
     * Get the control flow graph
     */
    public CFG getCFG() {
        return problem.scope.getCFG();
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

    private void processDestBB(List<U> workList, BitSet bbSet, BasicBlock d) {
        int id = d.getID();
        if (!bbSet.get(id)) {
            bbSet.set(id);
            workList.add(problem.getFlowGraphNode(d));
        }
    }

    public void computeDataFlowInfo(List<U> workList, BitSet bbSet) {
        if (problem.getFlowDirection() == DataFlowProblem.DF_Direction.BIDIRECTIONAL) {
            throw new RuntimeException("Bidirectional data flow computation not implemented yet!");
        }

        // System.out.println("----- processing bb " + basicBlock.getID() + " -----");
        bbSet.clear(basicBlock.getID());

        // Compute meet over all "sources" and compute "destination" basic blocks that should then be processed.
        // sources & targets depends on direction of the data flow problem
        applyPreMeetHandler();

        if (problem.getFlowDirection() == DataFlowProblem.DF_Direction.FORWARD) {
            computeDataFlowInfoForward(workList, bbSet);
        } else {
            computeDataFlowInfoBackward(workList, bbSet);
        }
    }

    public void computeDataFlowInfoBackward(List<U> workList, BitSet bbSet) {
        for (Edge<BasicBlock> e: getCFG().getOutgoingEdges(basicBlock)) {
            compute_MEET(e, problem.getFlowGraphNode(e.getDestination().getData()));
        }

        initSolution();                                  // Initialize computation

        // Apply transfer function (analysis-specific) based on new facts after computing MEET
        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            Instr i = it.previous();
            // System.out.println("TF: Processing: " + i);
            applyTransferFunction(i);
        }

        // If the solution has changed, add "dsts" to the work list.
        // No duplicates please which is why we have bbset.
        if (solutionChanged()) {
            for (BasicBlock b: getCFG().getIncomingSources(basicBlock)) {
                processDestBB(workList, bbSet, b);
            }
        }

        finalizeSolution();                              // Any post-computation cleanup
    }

    public void computeDataFlowInfoForward(List<U> workList, BitSet bbSet) {
        for (Edge<BasicBlock> e: getCFG().getIncomingEdges(basicBlock)) {
            compute_MEET(e, problem.getFlowGraphNode(e.getSource().getData()));
        }

        initSolution();                                  // Initialize computation

        // Apply transfer function (analysis-specific) based on new facts after computing MEET
        for (Instr i : basicBlock.getInstrs()) {
            // System.out.println("TF: Processing: " + i);
            applyTransferFunction(i);
        }

        // If the solution has changed, add "dsts" to the work list.
        // No duplicates please which is why we have bbset.
        if (solutionChanged()) {
            for (BasicBlock b: getCFG().getOutgoingDestinations(basicBlock)) {
                processDestBB(workList, bbSet, b);
            }
        }

        finalizeSolution();                              // Any post-computation cleanup
    }

    public boolean hasExceptionsRescued() {
        return rescuer != null;
    }

    public U getExceptionTargetNode() {
        // If there is a rescue node, on exception, control goes to the rescuer bb.  If not, it goes to the scope exit.
        return problem.getFlowGraphNode(rescuer == null ? getCFG().getExitBB() : rescuer);
    }

/* --------- protected fields/methods below --------- */
    protected final T problem;   // Dataflow problem with which this node is associated
    protected final BasicBlock basicBlock;     // CFG node for which this node contains info.
    private final BasicBlock rescuer;        // Basicblock that protects any exceptions raised in this node
}

package org.jruby.ir.representations;

import org.jruby.dirgra.DirectedGraph;
import org.jruby.dirgra.Edge;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.*;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.util.*;

/**
 * Represents the base build of a CFG.  All information here is accessed via
 * delegation from the CFG itself so this is meant as an internal
 * organizational structure for a build.
 */
public class CFG {
    public enum EdgeType {
        REGULAR,       // Any non-special edge.  Not really used.
        EXCEPTION,     // Edge to exception handling basic blocks
        FALL_THROUGH,  // Edge which is the natural fall through choice on a branch
        EXIT           // Edge to dummy exit BB
    }

    private static final Logger LOG = LoggerFactory.getLogger("CFG");

    private IRScope scope;
    private Map<Label, BasicBlock> bbMap;

    // Map of bb -> first bb of the rescue block that initiates exception handling for all exceptions thrown within this bb
    private Map<BasicBlock, BasicBlock> rescuerMap;

    /** Entry BB */
    private BasicBlock entryBB;

    /** Exit BB */
    private BasicBlock exitBB;

    /** BB that traps all exception-edges out of the cfg where we could add any cleanup/ensure code (ex: pop frames, etc.) */
    private BasicBlock globalEnsureBB;

    /** The graph itself */
    private DirectedGraph<BasicBlock> graph;

    private int nextBBId;       // Next available basic block id

    LinkedList<BasicBlock> postOrderList; // Post order traversal list of the cfg

    public CFG(IRScope scope) {
        this.scope = scope;
        this.graph = new DirectedGraph<>();
        this.bbMap = new HashMap<>();
        this.rescuerMap = new HashMap<>();
        this.nextBBId = 0;
        this.entryBB = this.exitBB = null;
        this.globalEnsureBB = null;
        this.postOrderList = null;
    }

    public int getNextBBID() {
        nextBBId++;
        return nextBBId;
    }

    public int getMaxNodeID() {
        return nextBBId;
    }

    public boolean bbIsProtected(BasicBlock b) {
        return getRescuerBBFor(b) != null;
    }

    public BasicBlock getBBForLabel(Label label) {
        return bbMap.get(label);
    }

    public BasicBlock getEntryBB() {
        return entryBB;
    }

    public BasicBlock getExitBB() {
        return exitBB;
    }

    public BasicBlock getGlobalEnsureBB() {
        return globalEnsureBB;
    }

    public LinkedList<BasicBlock> postOrderList() {
        // SSS FIXME: This caching is fragile -- requires invalidation
        // on change of CFG state. We need a better setup than this.
        if (postOrderList == null) postOrderList = buildPostOrderList();
        return postOrderList;
    }

    public Iterator<BasicBlock> getPostOrderTraverser() {
        return postOrderList().iterator();
    }

    public Iterator<BasicBlock> getReversePostOrderTraverser() {
        return postOrderList().descendingIterator();
    }

    public void resetState() {
        // SSS FIXME: anything else?
        postOrderList = null;
    }

    public IRScope getScope() {
        return scope;
    }

    /**
     * How many BasicBlocks are there in this CFG?
     */
    public int size() {
        return graph.size();
    }

    public Collection<BasicBlock> getBasicBlocks() {
        return graph.allData();
    }

    public Collection<BasicBlock> getSortedBasicBlocks() {
        return graph.getInorderData();
    }

    public void addEdge(BasicBlock source, BasicBlock destination, Object type) {
        graph.findOrCreateVertexFor(source).addEdgeTo(destination, type);
    }

    public int inDegree(BasicBlock b) {
        return graph.findVertexFor(b).inDegree();
    }

    public int outDegree(BasicBlock b) {
        return graph.findVertexFor(b).outDegree();
    }

    public Iterable<BasicBlock> getIncomingSources(BasicBlock block) {
        return graph.findVertexFor(block).getIncomingSourcesData();
    }

    public Iterable<Edge<BasicBlock>> getIncomingEdges(BasicBlock block) {
        return graph.findVertexFor(block).getIncomingEdges();
    }

    public BasicBlock getIncomingSource(BasicBlock block) {
        return graph.findVertexFor(block).getIncomingSourceData();
    }

    public BasicBlock getIncomingSourceOfType(BasicBlock block, Object type) {
        return graph.findVertexFor(block).getIncomingSourceDataOfType(type);
    }

    public Edge<BasicBlock> getIncomingEdgeOfType(BasicBlock block, Object type) {
        return graph.findVertexFor(block).getIncomingEdgeOfType(type);
    }

    public Edge<BasicBlock> getOutgoingEdgeOfType(BasicBlock block, Object type) {
        return graph.findVertexFor(block).getOutgoingEdgeOfType(type);
    }

    public BasicBlock getOutgoingDestination(BasicBlock block) {
        return graph.findVertexFor(block).getOutgoingDestinationData();
    }

    public BasicBlock getOutgoingDestinationOfType(BasicBlock block, Object type) {
        return graph.findVertexFor(block).getOutgoingDestinationDataOfType(type);
    }

    public Iterable<BasicBlock> getOutgoingDestinations(BasicBlock block) {
        return graph.findVertexFor(block).getOutgoingDestinationsData();
    }

    public Iterable<BasicBlock> getOutgoingDestinationsOfType(BasicBlock block, Object type) {
        return graph.findVertexFor(block).getOutgoingDestinationsDataOfType(type);
    }

    public Iterable<BasicBlock> getOutgoingDestinationsNotOfType(BasicBlock block, Object type) {
        return graph.findVertexFor(block).getOutgoingDestinationsDataNotOfType(type);
    }

    public Collection<Edge<BasicBlock>> getOutgoingEdges(BasicBlock block) {
        return graph.findVertexFor(block).getOutgoingEdges();
    }

    public Iterable<Edge<BasicBlock>> getOutgoingEdgesNotOfType(BasicBlock block, Object type) {
        return graph.findVertexFor(block).getOutgoingEdgesNotOfType(type);
    }

    public BasicBlock getRescuerBBFor(BasicBlock block) {
        return rescuerMap.get(block);
    }

    /* Add 'b' as a global ensure block that protects all unprotected blocks in this scope */
    public void addGlobalEnsureBB(BasicBlock geb) {
        assert globalEnsureBB == null: "CFG for scope " + getScope() + " already has a global ensure block.";

        addBasicBlock(geb);
        addEdge(geb, getExitBB(), EdgeType.EXIT);

        for (BasicBlock b: getBasicBlocks()) {
            if (b != geb && !bbIsProtected(b) && b != getEntryBB()) {
                addEdge(b, geb, EdgeType.EXCEPTION);
                setRescuerBB(b, geb);
            }
        }

        globalEnsureBB = geb;
    }

    public void setRescuerBB(BasicBlock block, BasicBlock rescuerBlock) {
        rescuerMap.put(block, rescuerBlock);
    }

    /**
     *  Build the Control Flow Graph
     */
    public DirectedGraph<BasicBlock> build(List<Instr> instrs) {
        // Map of label & basic blocks which are waiting for a bb with that label
        Map<Label, List<BasicBlock>> forwardRefs = new HashMap<>();

        // List of bbs that have a 'return' instruction
        List<BasicBlock> returnBBs = new ArrayList<>();

        // List of bbs that have a 'throw' instruction
        List<BasicBlock> exceptionBBs = new ArrayList<>();

        // Stack of nested rescue regions
        Stack<ExceptionRegion> nestedExceptionRegions = new Stack<>();

        // List of all rescued regions
        List<ExceptionRegion> allExceptionRegions = new ArrayList<>();

        // Dummy entry basic block (see note at end to see why)
        entryBB = createBB(nestedExceptionRegions);

        // First real bb
        BasicBlock firstBB = createBB(nestedExceptionRegions);

        // Build the rest!
        BasicBlock currBB = firstBB;
        BasicBlock newBB;
        boolean bbEnded = false;
        boolean nextBBIsFallThrough = true;
        for (Instr i : instrs) {
            // System.out.println("Processing: " + i);
            Operation iop = i.getOperation();
            if (iop == Operation.LABEL) {
                Label l = ((LabelInstr) i).getLabel();
                newBB = createBB(l, nestedExceptionRegions);
                // Jump instruction bbs dont add an edge to the succeeding bb by default
                if (nextBBIsFallThrough) graph.addEdge(currBB, newBB, EdgeType.FALL_THROUGH);
                currBB = newBB;
                bbEnded = false;
                nextBBIsFallThrough = true;

                // Add forward reference edges
                List<BasicBlock> frefs = forwardRefs.get(l);
                if (frefs != null) {
                    for (BasicBlock b : frefs) {
                        graph.addEdge(b, newBB, EdgeType.REGULAR);
                    }
                }
            } else if (bbEnded && iop != Operation.EXC_REGION_END) {
                newBB = createBB(nestedExceptionRegions);
                // Jump instruction bbs dont add an edge to the succeeding bb by default
                if (nextBBIsFallThrough) graph.addEdge(currBB, newBB, EdgeType.FALL_THROUGH); // currBB cannot be null!
                currBB = newBB;
                bbEnded = false;
                nextBBIsFallThrough = true;
            }

            if (i instanceof ExceptionRegionStartMarkerInstr) {
                // We dont need the instruction anymore -- so it is not added to the CFG.
                ExceptionRegionStartMarkerInstr ersmi = (ExceptionRegionStartMarkerInstr) i;
                ExceptionRegion rr = new ExceptionRegion(ersmi.getFirstRescueBlockLabel(), currBB);
                rr.addBB(currBB);
                allExceptionRegions.add(rr);

                if (!nestedExceptionRegions.empty()) {
                    nestedExceptionRegions.peek().addNestedRegion(rr);
                }

                nestedExceptionRegions.push(rr);
            } else if (i instanceof ExceptionRegionEndMarkerInstr) {
                // We dont need the instruction anymore -- so it is not added to the CFG.
                nestedExceptionRegions.pop().setEndBB(currBB);
            } else if (iop.endsBasicBlock()) {
                bbEnded = true;
                currBB.addInstr(i);
                Label tgt;
                nextBBIsFallThrough = false;
                if (i instanceof BranchInstr) {
                    tgt = ((BranchInstr) i).getJumpTarget();
                    nextBBIsFallThrough = true;
                } else if (i instanceof JumpInstr) {
                    tgt = ((JumpInstr) i).getJumpTarget();
                } else if (iop.isReturn()) { // BREAK, RETURN, CLOSURE_RETURN
                    tgt = null;
                    returnBBs.add(currBB);
                } else if (i instanceof ThrowExceptionInstr) {
                    tgt = null;
                    exceptionBBs.add(currBB);
                } else {
                    throw new RuntimeException("Unhandled case in CFG builder for basic block ending instr: " + i);
                }

                if (tgt != null) addEdge(currBB, tgt, forwardRefs);
            } else if (iop != Operation.LABEL) {
                currBB.addInstr(i);
            }
        }

        // Process all rescued regions
        for (ExceptionRegion rr: allExceptionRegions) {
            // When this exception region represents an unrescued region
            // from a copied ensure block, we have a dummy label
            Label rescueLabel = rr.getFirstRescueBlockLabel();
            if (!Label.UNRESCUED_REGION_LABEL.equals(rescueLabel)) {
                BasicBlock firstRescueBB = bbMap.get(rescueLabel);
                // Mark the BB as a rescue entry BB
                firstRescueBB.markRescueEntryBB();

                // Record a mapping from the region's exclusive basic blocks to the first bb that will start exception handling for all their exceptions.
                // Add an exception edge from every exclusive bb of the region to firstRescueBB
                for (BasicBlock b: rr.getExclusiveBBs()) {
                    setRescuerBB(b, firstRescueBB);
                    graph.addEdge(b, firstRescueBB, EdgeType.EXCEPTION);
                }
            }
        }

        buildExitBasicBlock(nestedExceptionRegions, firstBB, returnBBs, exceptionBBs, nextBBIsFallThrough, currBB, entryBB);

        // System.out.println("-------------- CFG before optimizing --------------");
        // System.out.println("\nGraph:\n" + toStringGraph());
        // System.out.println("\nInstructions:\n" + toStringInstrs());

        optimize(returnBBs); // remove useless cfg edges & orphaned bbs

        return graph;
    }

    private void addEdge(BasicBlock src, Label targetLabel, Map<Label, List<BasicBlock>> forwardRefs) {
        BasicBlock target = bbMap.get(targetLabel);

        if (target != null) {
            graph.addEdge(src, target, EdgeType.REGULAR);
            return;
        }

        // Add a forward reference from target -> source
        List<BasicBlock> forwardReferences = forwardRefs.get(targetLabel);

        if (forwardReferences == null) {
            forwardReferences = new ArrayList<>();
            forwardRefs.put(targetLabel, forwardReferences);
        }

        forwardReferences.add(src);
    }

    /**
     * Create special empty exit BasicBlock that all BasicBlocks will eventually
     * flow into.  All Edges to this 'dummy' BasicBlock will get marked with
     * an edge type of EXIT.
     *
     * Special BasicBlocks worth noting:
     * 1. Exceptions, Returns, Entry(why?) -> ExitBB
     * 2. Returns -> ExitBB
     */
    private BasicBlock buildExitBasicBlock(Stack<ExceptionRegion> nestedExceptionRegions, BasicBlock firstBB,
            List<BasicBlock> returnBBs, List<BasicBlock> exceptionBBs, boolean nextIsFallThrough, BasicBlock currBB, BasicBlock entryBB) {
        exitBB = createBB(nestedExceptionRegions);

        graph.addEdge(entryBB, exitBB, EdgeType.EXIT);
        graph.addEdge(entryBB, firstBB, EdgeType.FALL_THROUGH);

        for (BasicBlock rb : returnBBs) {
            graph.addEdge(rb, exitBB, EdgeType.EXIT);
        }

        for (BasicBlock rb : exceptionBBs) {
            graph.addEdge(rb, exitBB, EdgeType.EXIT);
        }

        if (nextIsFallThrough) graph.addEdge(currBB, exitBB, EdgeType.EXIT);

        return exitBB;
    }

    private BasicBlock createBB(Label label, Stack<ExceptionRegion> nestedExceptionRegions) {
        BasicBlock basicBlock = new BasicBlock(this, label);
        addBasicBlock(basicBlock);
        if (label.isGlobalEnsureBlockLabel()) {
            globalEnsureBB = basicBlock;
        }

        if (!nestedExceptionRegions.empty()) nestedExceptionRegions.peek().addBB(basicBlock);

        return basicBlock;
    }

    private BasicBlock createBB(Stack<ExceptionRegion> nestedExceptionRegions) {
        return createBB(scope.getNewLabel(), nestedExceptionRegions);
    }

    public void addBasicBlock(BasicBlock bb) {
        graph.findOrCreateVertexFor(bb); // adds vertex to graph
        bbMap.put(bb.getLabel(), bb);

        // Reset so later dataflow analyses get all basic blocks
        postOrderList = null;
    }

    public void removeEdge(Edge edge) {
        graph.removeEdge(edge);
    }

    public void removeAllOutgoingEdgesForBB(BasicBlock b) {
        graph.findVertexFor(b).removeAllOutgoingEdges();
    }

    private void deleteOrphanedBlocks(DirectedGraph<BasicBlock> graph) {
        // System.out.println("\nGraph:\n" + toStringGraph());
        // System.out.println("\nInstructions:\n" + toStringInstrs());

        // FIXME: Quick and dirty implementation
        while (true) {
            BasicBlock bbToRemove = null;
            for (BasicBlock b : graph.allData()) {
                if (b == entryBB) continue; // Skip entry bb!

                // Every other bb should have at least one incoming edge
                if (graph.findVertexFor(b).getIncomingEdges().isEmpty()) {
                    bbToRemove = b;
                    break;
                }
            }
            if (bbToRemove == null) break;

            removeBB(bbToRemove);
            removeNestedScopesFromBB(bbToRemove);
        }
    }

    private boolean mergeBBs(BasicBlock a, BasicBlock b) {
        BasicBlock aR = getRescuerBBFor(a);
        BasicBlock bR = getRescuerBBFor(b);

        // We can merge 'a' and 'b' if one of the following is true:
        // 1. 'a' and 'b' are both not empty
        //    They are protected by the same rescue block.
        //    NOTE: We need not check the ensure block map because all ensure blocks are already
        //    captured in the bb rescue block map.  So, if aR == bR, it is guaranteed that the
        //    ensure blocks for the two are identical.
        // 2. One of 'a' or 'b' is empty.  We dont need to check for rescue block match because
        //    an empty basic block cannot raise an exception, can it?
        if (aR == bR || a.isEmpty() || b.isEmpty()) {
            // First, remove straight-line jump, if present
            Instr lastInstr = a.getLastInstr();
            if (lastInstr instanceof JumpInstr) a.removeInstr(lastInstr);

            // Swallow b's instrs.
            a.swallowBB(b);

            // Fixup edges
            removeEdge(a, b);
            for (Edge<BasicBlock> e : getOutgoingEdges(b)) {
                addEdge(a, e.getDestination().getData(), e.getType());
            }

            // Delete bb
            removeBB(b);

            // Update rescue map
            if (aR == null && bR != null) {
                setRescuerBB(a, bR);
            }

            return true;
        } else {
            return false;
        }
    }

    public void removeBB(BasicBlock b) {
        graph.removeVertexFor(b);
        bbMap.remove(b.getLabel());
        rescuerMap.remove(b);
    }

    /**
     * Wrapped IRClosures in dead BB are lexically rooted to that dead BB so they can
     * be removed from the parent scope if the BB they live in died.
     */
    private void removeNestedScopesFromBB(BasicBlock bb) {
        for (Instr instr: bb.getInstrs()) {
            for (Operand oper: instr.getOperands()) {
                if (oper instanceof WrappedIRClosure) {
                    scope.removeClosure(((WrappedIRClosure) oper).getClosure());
                    break; // Only one WrappedIRClosure possible per instr
                }
            }
        }
    }

    public void collapseStraightLineBBs() {
        // Collect cfgs in a list first since the cfg/graph API returns an iterator
        // over live data.  But, basic block merging modifies that live data.
        //
        // SSS FIXME: So, we need a cfg/graph API that returns an iterator over
        // frozen data rather than live data.
        List<BasicBlock> cfgBBs = new ArrayList<>();
        for (BasicBlock b: getBasicBlocks()) cfgBBs.add(b);

        Set<BasicBlock> mergedBBs = new HashSet<>();
        for (BasicBlock b: cfgBBs) {
            if (!mergedBBs.contains(b) && outDegree(b) == 1) {
                for (Edge<BasicBlock> e : getOutgoingEdges(b)) {
                    BasicBlock outB = e.getDestination().getData();
                    if (e.getType() != EdgeType.EXCEPTION && inDegree(outB) == 1 && mergeBBs(b, outB)) {
                        mergedBBs.add(outB);
                    }
                }
            }
        }
    }

    private void optimize(List<BasicBlock> returnBBs) {
        // Propagate returns backwards where possible.
        // If:
        // - there is an edge from BB: x -> r, and
        // - r has a single instr, the return, and
        // - last instr of x is a copy, and
        // - the copy feeds the return
        // then:
        // - replace the copy with a return that returns the copied value
        // - remove the edge from x -> r
        // - add an edge from x -> exit-bb
        //
        // If a jump intervenes in 'x', skip over it and if merge succeeds,
        // delete the jump.
        List<Edge> toRemove = new ArrayList<>();
        for (BasicBlock retBB: returnBBs) {
            List<Instr> rbInstrs = retBB.getInstrs();
            Instr first = rbInstrs.get(0);
            if (first instanceof ReturnInstr) {
                Operand rv = ((ReturnInstr)first).getReturnValue();
                if (rv instanceof Variable) {
                    for (Edge<BasicBlock> e : getIncomingEdges(retBB)) {
                        BasicBlock srcBB = e.getSource().getData();
                        List<Instr> srcInstrs = srcBB.getInstrs();
                        int n = srcInstrs.size();

                        // Skip over empty bbs
                        if (n == 0) continue;

                        Instr jump = null;
                        Instr last = srcInstrs.get(n-1);
                        // Skip over a jump
                        if (last instanceof JumpInstr && n > 2) {
                            jump = last;
                            last = srcInstrs.get(n-2);
                        }
                        // Merge
                        if (last instanceof CopyInstr && ((CopyInstr)last).getResult().equals(rv)) {
                            srcInstrs.set(n-1, new ReturnInstr(((CopyInstr)last).getSource()));
                            toRemove.add(e);
                            addEdge(srcBB, exitBB, EdgeType.EXIT);
                            // System.out.println("Merged " + last + " with " + ri + " in " + scope);
                            if (jump != null) {
                                srcInstrs.remove(jump);
                                // System.out.println("Deleting " + jump);
                            }
                        }
                    }
                }
            }
        }
        for (Edge edge: toRemove) {
            graph.removeEdge(edge);
        }

        // SSS FIXME: Can't we not add some of these exception edges in the first place??
        // Remove exception edges from blocks that couldn't possibly thrown an exception!
        toRemove = new ArrayList<>();
        for (BasicBlock b : graph.allData()) {
            boolean noExceptions = true;
            for (Instr i : b.getInstrs()) {
                if (i.canRaiseException()) {
                    noExceptions = false;
                    break;
                }
            }

            if (noExceptions) {
                for (Edge<BasicBlock> e : graph.findVertexFor(b).getOutgoingEdgesOfType(EdgeType.EXCEPTION)) {
                    BasicBlock source = e.getSource().getData();
                    BasicBlock destination = e.getDestination().getData();
                    toRemove.add(e);

                    if (rescuerMap.get(source) == destination) rescuerMap.remove(source);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            for (Edge edge: toRemove) {
                graph.removeEdge(edge);
            }
        }

        deleteOrphanedBlocks(graph);

        collapseStraightLineBBs();
    }

    public String toStringGraph() {
        return graph.toString();
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder();

        for (BasicBlock b : getSortedBasicBlocks()) {
            buf.append(b.toStringInstrs());
        }
        buf.append("\n\n------ Rescue block map ------\n");
        List<BasicBlock> e = new ArrayList<>(rescuerMap.keySet());
        Collections.sort(e);

        for (BasicBlock bb : e) {
            buf.append("BB ").append(bb.getID()).append(" --> BB ").append(rescuerMap.get(bb).getID()).append("\n");
        }

        List<IRClosure> closures = scope.getClosures();
        if (!closures.isEmpty()) {
            buf.append("\n\n------ Closures encountered in this scope ------\n");
            for (IRClosure c : closures) {
                buf.append(c.toStringBody());
            }
            buf.append("------------------------------------------------\n");
        }

        return buf.toString();
    }

    public void removeEdge(BasicBlock a, BasicBlock b) {
       graph.removeEdge(a, b);
    }

    private LinkedList<BasicBlock> buildPostOrderList() {
        BasicBlock             root    = getEntryBB();
        LinkedList<BasicBlock> list    = new LinkedList<>();
        Stack<BasicBlock>      stack   = new Stack<>();
        boolean[]              visited = new boolean[1 + getMaxNodeID()];

        stack.push(root);
        visited[root.getID()] = true;

        // Non-recursive post-order traversal (the added flag is required to handle cycles and common ancestors)
        while (!stack.empty()) {
            // Check if all children of the top of the stack have been added
            BasicBlock b = stack.peek();
            boolean allChildrenVisited = true;
            for (BasicBlock dst: getOutgoingDestinations(b)) {
                int dstID = dst.getID();
                if (!visited[dstID]) {
                    allChildrenVisited = false;
                    // This ensures that no matter what order we visit children, we process exit nodes before anything.
                    // else.  Alternatively, getOutgoingDestinations(..) would have to return nodes in a specific order
                    // that is dependent on basic block numbering -- which would be fragile.
                    if (graph.findVertexFor(dst).outDegree() == 0) {
                        list.add(dst);
                    } else {
                        stack.push(dst);
                    }
                    visited[dstID] = true;
                }
            }

            // If all children have been added previously, we are ready with 'b' in this round!
            if (allChildrenVisited) {
                stack.pop();
                list.add(b);
            }
        }

        // Sanity check!
        for (BasicBlock b : getBasicBlocks()) {
            if (!visited[b.getID()]) {
                printError("BB " + b.getID() + " missing from po list!");
                break;
            }
        }

        return list;
    }

    /**
     * Clone this CFG and return a new one.
     *
     * @param info context object to perform the clone
     * @param clonedScope already cloned IRScope which this new CFG will belong to
     * @return a newly cloned CFG
     */
    public CFG clone(CloneInfo info, IRScope clonedScope) {
        CFG newCFG = new CFG(clonedScope);
        Map<BasicBlock, BasicBlock> cloneBBMap = new HashMap<>();

        // Part 1: Clone all BBs and stuff in map so graph building in 2 will have all BBs available.
        for (BasicBlock bb: getBasicBlocks()) {                 // clone bbs
            BasicBlock newBB = bb.clone(info, newCFG);
            newCFG.addBasicBlock(newBB);
            cloneBBMap.put(bb, newBB);
        }

        // Part 2: Clone graph (build new edges from new BBs made in previous phase)
        for (BasicBlock bb: getBasicBlocks()) {
            BasicBlock newSource = cloneBBMap.get(bb);
            for (Edge<BasicBlock> edge : getOutgoingEdges(bb)) {
                BasicBlock newDestination = cloneBBMap.get(edge.getDestination().getData());
                newCFG.addEdge(newSource, newDestination, edge.getType());
            }
        }

        // Part 3: clone all non-derivable fields
        for (BasicBlock bb: rescuerMap.keySet()) {              // clone rescuer map
            newCFG.setRescuerBB(cloneBBMap.get(bb), cloneBBMap.get(rescuerMap.get(bb)));
        }

        newCFG.entryBB = cloneBBMap.get(entryBB);               // clone entry BB
        newCFG.exitBB  = cloneBBMap.get(exitBB);                // clone exit BB
        newCFG.globalEnsureBB = cloneBBMap.get(globalEnsureBB); // clone GEB

        return newCFG;
    }

    private void printError(String message) {
        LOG.error(message + "\nGraph:\n" + this + "\nInstructions:\n" + toStringInstrs());
    }
}

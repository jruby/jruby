package org.jruby.ir.representations;

import org.jruby.dirgra.DirectedGraph;
import org.jruby.dirgra.Edge;
import org.jruby.ir.IRManager;
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

    private static final Logger LOG = LoggerFactory.getLogger(CFG.class);

    private final IRScope scope;
    private final Map<Label, BasicBlock> bbMap;

    // Map of bb -> first bb of the rescue block that initiates exception handling for all exceptions thrown within this bb
    private final Map<BasicBlock, BasicBlock> rescuerMap;

    /** Entry BB */
    private BasicBlock entryBB;

    /** Exit BB */
    private BasicBlock exitBB;

    /** List of bbs that have a 'return' instruction */
    List<BasicBlock> returnBBs = new ArrayList<>();

    /** BB that traps all exception-edges out of the cfg where we could add any cleanup/ensure code (ex: pop frames, etc.) */
    private BasicBlock globalEnsureBB;

    /** The graph itself */
    private final DirectedGraph<BasicBlock, EdgeType> graph;

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

    public IRManager getManager() {
        return scope.getManager();
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

    public void addEdge(BasicBlock source, BasicBlock destination, EdgeType type) {
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

    public Iterable<Edge<BasicBlock, EdgeType>> getIncomingEdges(BasicBlock block) {
        return graph.findVertexFor(block).getIncomingEdges();
    }

    public BasicBlock getIncomingSourceOfType(BasicBlock block, EdgeType type) {
        return graph.findVertexFor(block).getIncomingSourceDataOfType(type);
    }

    public BasicBlock getOutgoingDestinationOfType(BasicBlock block, EdgeType type) {
        return graph.findVertexFor(block).getOutgoingDestinationDataOfType(type);
    }

    public Iterable<BasicBlock> getOutgoingDestinations(BasicBlock block) {
        return graph.findVertexFor(block).getOutgoingDestinationsData();
    }

    public Iterable<BasicBlock> getOutgoingDestinationsOfType(BasicBlock block, EdgeType type) {
        return graph.findVertexFor(block).getOutgoingDestinationsDataOfType(type);
    }

    public Iterable<BasicBlock> getOutgoingDestinationsNotOfType(BasicBlock block, EdgeType type) {
        return graph.findVertexFor(block).getOutgoingDestinationsDataNotOfType(type);
    }

    public Collection<Edge<BasicBlock, EdgeType>> getOutgoingEdges(BasicBlock block) {
        return graph.findVertexFor(block).getOutgoingEdges();
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
    public DirectedGraph<BasicBlock, EdgeType> build(Instr[] instrs) {
        // Map of label & basic blocks which are waiting for a bb with that label
        Map<Label, List<BasicBlock>> forwardRefs = new HashMap<>();

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
        for (Instr i: instrs) {
            // System.out.println("Processing: " + i);
            Operation iop = i.getOperation();
            if (iop == Operation.LABEL) {
                Label l = ((LabelInstr) i).getLabel();
                newBB = createBB(l, nestedExceptionRegions);
                // Jump instruction bbs don't add an edge to the succeeding bb by default
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
                // Jump instruction bbs don't add an edge to the succeeding bb by default
                if (nextBBIsFallThrough) graph.addEdge(currBB, newBB, EdgeType.FALL_THROUGH); // currBB cannot be null!
                currBB = newBB;
                bbEnded = false;
                nextBBIsFallThrough = true;
            }

            if (i instanceof ExceptionRegionStartMarkerInstr ersmi) {
                // We don't need the instruction anymore -- so it is not added to the CFG.
                ExceptionRegion rr = new ExceptionRegion(ersmi.getFirstRescueBlockLabel(), currBB);
                rr.addBB(currBB);
                allExceptionRegions.add(rr);

                if (!nestedExceptionRegions.empty()) {
                    nestedExceptionRegions.peek().addNestedRegion(rr);
                }

                nestedExceptionRegions.push(rr);
            } else if (i instanceof ExceptionRegionEndMarkerInstr) {
                // We don't need the instruction anymore -- so it is not added to the CFG.
                nestedExceptionRegions.pop().setEndBB(currBB);
            } else if (iop.endsBasicBlock()) {
                bbEnded = true;
                currBB.addInstr(i);
                Label tgt = null;
                nextBBIsFallThrough = false;
                if (i instanceof BranchInstr) {
                    tgt = ((BranchInstr) i).getJumpTarget();
                    nextBBIsFallThrough = true;
                } else if (i instanceof MultiBranchInstr) {
                    Label[] tgts = ((MultiBranchInstr) i).getJumpTargets();
                    for (Label l : tgts) addEdge(currBB, l, forwardRefs);
                } else if (i instanceof JumpInstr) {
                    tgt = ((JumpInstr) i).getJumpTarget();
                } else if (iop.isReturn()) { // BREAK, RETURN, CLOSURE_RETURN
                    returnBBs.add(currBB);
                } else if (i instanceof ThrowExceptionInstr) {
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

                // Record a mapping from the region's exclusive basic blocks to the first bb that will start exception
                // handling for all their exceptions. Add an exception edge from every exclusive bb of the region to
                // firstRescueBB unless it is incapable of raising.
                for (BasicBlock b: rr.getExclusiveBBs()) {
                    if (b.canRaiseExceptions()) {
                        setRescuerBB(b, firstRescueBB);
                        graph.addEdge(b, firstRescueBB, EdgeType.EXCEPTION);
                    }
                }
            }
        }

        buildExitBasicBlock(nestedExceptionRegions, firstBB, returnBBs, exceptionBBs, nextBBIsFallThrough, currBB, entryBB);

        // System.out.println("-------------- CFG before optimizing --------------");
        // System.out.println("\nGraph:\n" + toStringGraph());
        // System.out.println("\nInstructions:\n" + toStringInstrs());

        optimize(); // remove useless cfg edges & orphaned bbs

        return graph;
    }

    // A branch may have become something else...let's fix up the CFG
    public void fixupEdges(BasicBlock bb) {
        Instr lastInstr = bb.getLastInstr();
        if (lastInstr instanceof BranchInstr) {
            // We assume branches will not turn into other branches, so we ignore this
        } else if (bb.getLastInstr() instanceof JumpTargetInstr) { // this is really a jump branch already covered
            for (Edge<BasicBlock, EdgeType> edge: getOutgoingEdges(bb)) {
                if (edge.getType() == EdgeType.FALL_THROUGH) graph.removeEdge(edge);
            }
        } else {
            for (Edge<BasicBlock, EdgeType> edge: getOutgoingEdges(bb)) {
                if (edge.getType() == EdgeType.REGULAR) graph.removeEdge(edge);
            }
        }
    }

    private void addEdge(BasicBlock src, Label targetLabel, Map<Label, List<BasicBlock>> forwardRefs) {
        BasicBlock target = bbMap.get(targetLabel);

        if (target != null) {
            graph.addEdge(src, target, EdgeType.REGULAR);
            return;
        }

        // Add a forward reference from target -> source
        List<BasicBlock> forwardReferences = forwardRefs.computeIfAbsent(targetLabel, k -> new ArrayList<>());

        forwardReferences.add(src);
    }

    /**
     * Create special empty exit BasicBlock that all BasicBlocks will eventually
     * flow into.  All Edges to this 'dummy' BasicBlock will get marked with
     * an edge type of EXIT.
     *
     * Special BasicBlocks worth noting:
     * 1. Exceptions, Returns, Entry(why?)$ -&gt; ExitBB
     * 2. Returns$ -&gt; ExitBB
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

    public void removeAllOutgoingEdgesForBB(BasicBlock b) {
        graph.findVertexFor(b).removeAllOutgoingEdges();
    }

    private void deleteOrphanedBlocks(DirectedGraph<BasicBlock, EdgeType> graph) {
        // System.out.println("\nGraph:\n" + toStringGraph());
        // System.out.println("\nInstructions:\n" + toStringInstrs());

        Queue<BasicBlock> worklist = new LinkedList<>();
        Set<BasicBlock> living = new HashSet<>();
        worklist.add(entryBB);
        living.add(entryBB);

        while (!worklist.isEmpty()) {
            BasicBlock current = worklist.remove();

            for (BasicBlock bb: graph.findVertexFor(current).getOutgoingDestinationsData()) {
                if (!living.contains(bb)) {
                    worklist.add(bb);
                    living.add(bb);
                }
            }
        }

        // Seems like Java should have simpler way of doing this.
        // We cannot just remove in this loop, or we get concmodexc.
        Set<BasicBlock> dead = new HashSet<>();
        for (BasicBlock bb: graph.allData()) {
            if (!living.contains(bb)) dead.add(bb);
        }

        for (BasicBlock bb: dead) {
            removeBB(bb);
            removeNestedScopesFromBB(bb);
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
        // 2. One of 'a' or 'b' is empty.  We don't need to check for rescue block match because
        //    an empty basic block cannot raise an exception, can it?
        if (aR == bR || a.isEmpty() || b.isEmpty()) {
            // First, remove straight-line jump, if present
            Instr lastInstr = a.getLastInstr();
            if (lastInstr instanceof JumpInstr) a.removeInstr(lastInstr);

            // Swallow b's instrs.
            a.swallowBB(b);

            // Fixup edges
            removeEdge(a, b);
            for (Edge<BasicBlock, EdgeType> e : getOutgoingEdges(b)) {
                addEdge(a, e.getDestination().getData(), e.getType());
            }

            // Move all incoming edges of b to be incoming edges of a.
            for (Edge<BasicBlock, EdgeType> e : getIncomingEdges(b)) {
                BasicBlock fixupBB = e.getSource().getData();
                removeEdge(fixupBB, b);
                addEdge(fixupBB, a, e.getType());

                // a -fall-through->b handled above. Any jumps to b must be pointed to a's label.
                Instr fixupLastInstr = fixupBB.getLastInstr();
                if (fixupLastInstr instanceof JumpTargetInstr) {
                    ((JumpTargetInstr) fixupLastInstr).setJumpTarget(a.getLabel());
                }
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
        if (b == globalEnsureBB) globalEnsureBB = null;

        graph.removeVertexFor(b);
        bbMap.remove(b.getLabel());
        rescuerMap.remove(b);
        returnBBs.remove(b);
    }

    /**
     * Wrapped IRClosures in dead BB are lexically rooted to that dead BB, so they can
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
        List<BasicBlock> cfgBBs = new ArrayList<>(getBasicBlocks());

        Set<BasicBlock> mergedBBs = new HashSet<>();
        for (BasicBlock b: cfgBBs) {
            if (!mergedBBs.contains(b) && outDegree(b) == 1) {
                for (Edge<BasicBlock, EdgeType> e : getOutgoingEdges(b)) {
                    BasicBlock outB = e.getDestination().getData();

                    // 1:1 BBs can just be one since there is only one place to go.  An empty entering any BB can merge
                    // since the empty one does nothing (Note: mergeBBs uses empty as destination impl-wise but outcome
                    // is the same).
                    if (e.getType() != EdgeType.EXCEPTION && (inDegree(outB) == 1 || b.isEmpty()) && mergeBBs(b, outB)) {
                        mergedBBs.add(outB);
                    }
                }
            }
        }
    }

    public void optimize() {
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
        List<Edge<BasicBlock, EdgeType>> toRemove = new ArrayList<>();
        for (BasicBlock retBB: returnBBs) {
            List<Instr> rbInstrs = retBB.getInstrs();
            Instr first = rbInstrs.get(0);
            if (first instanceof ReturnInstr) {
                Operand rv = ((ReturnInstr)first).getReturnValue();
                if (rv instanceof Variable) {
                    for (Edge<BasicBlock, EdgeType> e : getIncomingEdges(retBB)) {
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
        for (Edge<BasicBlock, EdgeType> edge: toRemove) {
            graph.removeEdge(edge);
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

        /*
        Collection<IRClosure> closures = scope.getClosures();
        if (!closures.isEmpty()) {
            buf.append("\n\n------ Closures encountered in this scope ------\n");
            for (IRClosure c : closures) {
                buf.append(c.toStringBody());
            }
            buf.append("------------------------------------------------\n");
        }
        */

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
            for (Edge<BasicBlock, EdgeType> edge : getOutgoingEdges(bb)) {
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

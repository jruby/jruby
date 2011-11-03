package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.BranchInstr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.compiler.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JumpIndirectInstr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.LabelInstr;
import org.jruby.compiler.ir.instructions.SetReturnAddressInstr;
import org.jruby.compiler.ir.instructions.ThrowExceptionInstr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.util.DirectedGraph;
import org.jruby.compiler.ir.util.Edge;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

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
    
    private IRExecutionScope scope;
    private Map<Label, BasicBlock> bbMap = new HashMap<Label, BasicBlock>();
        
    // Map of bb -> first bb of the rescue block that initiates exception handling for all exceptions thrown within this bb
    private Map<BasicBlock, BasicBlock> rescuerMap = new HashMap<BasicBlock, BasicBlock>();
        
    // Map of bb -> first bb of the ensure block that protects this bb
    private Map<BasicBlock, BasicBlock> ensurerMap = new HashMap<BasicBlock, BasicBlock>();
        
    private List<ExceptionRegion> outermostERs = new ArrayList<ExceptionRegion>();
    
    private BasicBlock entryBB = null;
    private BasicBlock exitBB = null;
    private DirectedGraph<BasicBlock> graph = new DirectedGraph<BasicBlock>();
    
    private int nextBBId = 0;       // Next available basic block id
    
    LinkedList<BasicBlock> postOrderList = null; // Post order traversal list of the cfg    
    
    public CFG(IRExecutionScope scope) {
        this.scope = scope;
    }
    
    public int getNextBBID() {
        nextBBId++;
        return nextBBId;
    }

    public int getMaxNodeID() {
        return nextBBId;
    }     

    public boolean bbIsProtected(BasicBlock b) {
        // No need to look in ensurerMap because (_bbEnsurerMap(b) != null) => (_bbResucerMap(b) != null)
        return getRescuerBBFor(b) != null;
    }    
    
    public BasicBlock getBBForLabel(Label label) {
        return bbMap.get(label);
    }
    
    public BasicBlock getEnsurerBBFor(BasicBlock block) {
        return ensurerMap.get(block);
    }
    
    public BasicBlock getEntryBB() {
        return entryBB;
    }
    
    public BasicBlock getExitBB() {
        return exitBB;
    }
    
    public List<ExceptionRegion> getOutermostExceptionRegions() {
        return outermostERs;
    }
    
    public LinkedList<BasicBlock> postOrderList() {
        if (postOrderList == null) postOrderList = buildPostOrderList();
        return postOrderList;
    }
    
    public ListIterator<BasicBlock> getPostOrderTraverser() {
        return postOrderList().listIterator();
    }

    public ListIterator<BasicBlock> getReversePostOrderTraverser() {
        return postOrderList().listIterator(size());
    }    
    
    public IRExecutionScope getScope() {
        return scope;
    }    
    
    public int size() {
        return graph.size();
    }
    
    public Collection<BasicBlock> getBasicBlocks() {
        return graph.allData();
    }
    
    public Iterable<BasicBlock> getIncomingSources(BasicBlock block) {
        return graph.vertexFor(block).getIncomingSourcesData();
    }
    
    public Iterable<Edge<BasicBlock>> getIncomingEdges(BasicBlock block) {
        return graph.vertexFor(block).getIncomingEdges();
    }
    
    public BasicBlock getIncomingSource(BasicBlock block) {
        return graph.vertexFor(block).getIncomingSourceData();
    }    
    
    public BasicBlock getIncomingSourceOfType(BasicBlock block, Object type) {
        return graph.vertexFor(block).getIncomingSourceDataOfType(type);
    }
    
    public Edge<BasicBlock> getIncomingEdgeOfType(BasicBlock block, Object type) {
        return graph.vertexFor(block).getIncomingEdgeOfType(type);
    }

    public Edge<BasicBlock> getOutgoingEdgeOfType(BasicBlock block, Object type) {
        return graph.vertexFor(block).getOutgoingEdgeOfType(type);
    }
    
    public BasicBlock getOutgoingDestination(BasicBlock block) {
        return graph.vertexFor(block).getOutgoingDestinationData();
    }    
    
    public BasicBlock getOutgoingDestinationOfType(BasicBlock block, Object type) {
        return graph.vertexFor(block).getOutgoingDestinationDataOfType(type);
    }
    
    public Iterable<BasicBlock> getOutgoingDestinations(BasicBlock block) {
        return graph.vertexFor(block).getOutgoingDestinationsData();
    }
    
    public Iterable<BasicBlock> getOutgoingDestinationsOfType(BasicBlock block, Object type) {
        return graph.vertexFor(block).getOutgoingDestinationsDataOfType(type);
    }
    
    public Iterable<BasicBlock> getOutgoingDestinationsNotOfType(BasicBlock block, Object type) {
        return graph.vertexFor(block).getOutgoingDestinationsDataNotOfType(type);
    }
    
    public Set<Edge<BasicBlock>> getOutgoingEdges(BasicBlock block) {
        return graph.vertexFor(block).getOutgoingEdges();
    }
    
    public Iterable<Edge<BasicBlock>> getOutgoingEdgesNotOfType(BasicBlock block, Object type) {
        return graph.vertexFor(block).getOutgoingEdgesNotOfType(type);
    }
    
    public BasicBlock getRescuerBBFor(BasicBlock block) {
        return rescuerMap.get(block);
    }
    
    public void addEdge(BasicBlock source, BasicBlock destination, Object type) {
        graph.vertexFor(source).addEdgeTo(destination, type);
    }
    
    /* Add 'b' as a global ensure block that protects all unprotected blocks in this scope */
    public void addGlobalEnsureBlock(BasicBlock geb) {
        addEdge(geb, getExitBB(), EdgeType.EXIT);
        
        for (BasicBlock basicBlock: getBasicBlocks()) {
            if (basicBlock != geb && !bbIsProtected(basicBlock)) {
                addEdge(basicBlock, geb, EdgeType.EXCEPTION);
                setRescuerBB(basicBlock, geb);
                setEnsurerBB(basicBlock, geb);
            }
        }
    }     
    
    public void putBBForLabel(Label label, BasicBlock block) {
        bbMap.put(label, block);
    }
    
    public void setEnsurerBB(BasicBlock block, BasicBlock ensureBlock) {
        ensurerMap.put(block, ensureBlock);
    }
    
    public void setRescuerBB(BasicBlock block, BasicBlock exceptionBlock) {
        rescuerMap.put(block, exceptionBlock);
    }
    
    /**
     *  Build the Control Flow Graph
     */
    public DirectedGraph<BasicBlock> build(List<Instr> instrs) {
        // Map of label & basic blocks which are waiting for a bb with that label
        Map<Label, List<BasicBlock>> forwardRefs = new HashMap<Label, List<BasicBlock>>();

        // Map of return address variable and all possible targets (required to connect up ensure blocks with their targets)
        Map<Variable, Set<Label>> retAddrMap = new HashMap<Variable, Set<Label>>();
        Map<Variable, BasicBlock> retAddrTargetMap = new HashMap<Variable, BasicBlock>();

        // List of bbs that have a 'return' instruction
        List<BasicBlock> returnBBs = new ArrayList<BasicBlock>();

        // List of bbs that have a 'throw' instruction
        List<BasicBlock> exceptionBBs = new ArrayList<BasicBlock>();

        // Stack of nested rescue regions
        Stack<ExceptionRegion> nestedExceptionRegions = new Stack<ExceptionRegion>();

        // List of all rescued regions
        List<ExceptionRegion> allExceptionRegions = new ArrayList<ExceptionRegion>();

        // Dummy entry basic block (see note at end to see why)
        entryBB = createBB(nestedExceptionRegions);

        // First real bb
        BasicBlock firstBB = createBB(nestedExceptionRegions);

        // Build the rest!
        BasicBlock currBB = firstBB;
        BasicBlock newBB = null;
        boolean bbEnded = false;
        boolean nextBBIsFallThrough = true;
        for (Instr i : instrs) {
            Operation iop = i.getOperation();
            if (iop == Operation.LABEL) {
                Label l = ((LabelInstr) i).label;
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
            } else if (bbEnded && (iop != Operation.EXC_REGION_END)) {
                newBB = createBB(nestedExceptionRegions);
                // Jump instruction bbs dont add an edge to the succeeding bb by default
                if (nextBBIsFallThrough) graph.addEdge(currBB, newBB, EdgeType.FALL_THROUGH); // currBB cannot be null!
                currBB = newBB;
                bbEnded = false;
                nextBBIsFallThrough = true;
            }

            if (i instanceof ExceptionRegionStartMarkerInstr) {
// SSS: Do we need this anymore?
//                currBB.addInstr(i);
                ExceptionRegionStartMarkerInstr ersmi = (ExceptionRegionStartMarkerInstr) i;
                ExceptionRegion rr = new ExceptionRegion(ersmi.firstRescueBlockLabel, ersmi.ensureBlockLabel);
                rr.addBB(currBB);
                allExceptionRegions.add(rr);

                if (nestedExceptionRegions.empty()) {
                    outermostERs.add(rr);
                } else {
                    nestedExceptionRegions.peek().addNestedRegion(rr);
                }

                nestedExceptionRegions.push(rr);
            } else if (i instanceof ExceptionRegionEndMarkerInstr) {
// SSS: Do we need this anymore?
//                currBB.addInstr(i);
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
                } else if (i instanceof JumpIndirectInstr) {
                    tgt = null;
                    Set<Label> retAddrs = retAddrMap.get(((JumpIndirectInstr) i).getJumpTarget());
                    for (Label l : retAddrs) {
                        addEdge(currBB, l, forwardRefs);
                    }
                    // Record the target bb for the retaddr var for any set_addr instrs that appear later and use the same retaddr var
                    retAddrTargetMap.put(((JumpIndirectInstr) i).getJumpTarget(), currBB);
                } else {
                    throw new RuntimeException("Unhandled case in CFG builder for basic block ending instr: " + i);
                }

                if (tgt != null) addEdge(currBB, tgt, forwardRefs);
            } else if (iop != Operation.LABEL) {
                currBB.addInstr(i);
            }

            if (i instanceof SetReturnAddressInstr) {
                Variable v = i.getResult();
                Label tgtLbl = ((SetReturnAddressInstr) i).getReturnAddr();
                BasicBlock tgtBB = retAddrTargetMap.get(v);
                // If we have the target bb, add the edge
                // If not, record it for fixup later
                if (tgtBB != null) {
                    addEdge(tgtBB, tgtLbl, forwardRefs);
                } else {
                    Set<Label> addrs = retAddrMap.get(v);
                    if (addrs == null) {
                        addrs = new HashSet<Label>();
                        retAddrMap.put(v, addrs);
                    }
                    addrs.add(tgtLbl);
                }
            } else if (i instanceof CallInstr) { // Build CFG for the closure if there exists one
                Operand closureArg = ((CallInstr) i).getClosureArg();
                if (closureArg instanceof MetaObject) {
                    ((IRClosure) ((MetaObject) closureArg).scope).buildCFG();
                }
            }
        }

        // Process all rescued regions
        for (ExceptionRegion rr : allExceptionRegions) {
            BasicBlock firstRescueBB = bbMap.get(rr.getFirstRescueBlockLabel());

            // 1. Tell the region that firstRescueBB is its protector!
            rr.setFirstRescueBB(firstRescueBB);

            // 2. Record a mapping from the region's exclusive basic blocks to the first bb that will start exception handling for all their exceptions.
            // 3. Add an exception edge from every exclusive bb of the region to firstRescueBB
            BasicBlock ensureBlockBB = rr.getEnsureBlockLabel() == null ? null : bbMap.get(rr.getEnsureBlockLabel());
            for (BasicBlock b : rr.getExclusiveBBs()) {
                rescuerMap.put(b, firstRescueBB);
                graph.addEdge(b, firstRescueBB, EdgeType.EXCEPTION);
                if (ensureBlockBB != null) {
                    ensurerMap.put(b, ensureBlockBB);
                    // SSS FIXME: This is a conservative edge because when a rescue block is present
                    // that catches an exception, control never reaches the ensure block directly.
                    // Only when we get an error or threadkill even, or when breaks propagate upward
                    // do we need to hit an ensure directly.  This edge is present to account for that
                    // control-flow scneario.
                    graph.addEdge(b, ensureBlockBB, EdgeType.EXCEPTION);
                }
            }
        }
        
        buildExitBasicBlock(nestedExceptionRegions, firstBB, returnBBs, exceptionBBs, nextBBIsFallThrough, currBB, entryBB);

        optimize(); // remove useless cfg edges & orphaned bbs
        
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
            forwardReferences = new ArrayList<BasicBlock>();
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
        bbMap.put(label, basicBlock);
        graph.vertexFor(basicBlock);
        
        if (!nestedExceptionRegions.empty()) nestedExceptionRegions.peek().addBB(basicBlock);

        return basicBlock;
    }

    private BasicBlock createBB(Stack<ExceptionRegion> nestedExceptionRegions) {
        return createBB(scope.getNewLabel(), nestedExceptionRegions);
    }
    

    public void removeEdge(Edge edge) {
        graph.removeEdge(edge);
    }    
    
    private void deleteOrphanedBlocks(DirectedGraph<BasicBlock> graph) {
        // System.out.println("\nGraph:\n" + getGraph().toString());
        // System.out.println("\nInstructions:\n" + toStringInstrs());

        // FIXME: Quick and dirty implementation
        while (true) {
            BasicBlock bbToRemove = null;
            for (BasicBlock b : graph.allData()) {
                if (b == entryBB) continue; // Skip entry bb!

                // Every other bb should have at least one incoming edge
                if (graph.vertexFor(b).getIncomingEdges().isEmpty()) {
                    bbToRemove = b;
                    break;
                }
            }
            if (bbToRemove == null) break;

            removeBB(bbToRemove);
        }
    }    
     
    void removeBB(BasicBlock b) {
        graph.removeVertexFor(b);
        bbMap.remove(b.getLabel());
        rescuerMap.remove(b);
        ensurerMap.remove(b);
        // SSS FIXME: Patch up rescued regions as well??
    }
    
    private void optimize() {
        // SSS FIXME: Can't we not add some of these exception edges in the first place??
        // Remove exception edges from blocks that couldn't possibly thrown an exception!
        List<Edge> toRemove = new ArrayList<Edge>();
        for (BasicBlock b : graph.allData()) {
            boolean noExceptions = true;
            for (Instr i : b.getInstrs()) {
                if (i.canRaiseException()) {
                    noExceptions = false;
                    break;
                }
            }

            if (noExceptions) {
                for (Edge<BasicBlock> e : graph.vertexFor(b).getOutgoingEdgesOfType(EdgeType.EXCEPTION)) {
                    BasicBlock source = e.getSource().getData();
                    BasicBlock destination = e.getDestination().getData();
                    toRemove.add(e);
                        
                    if (rescuerMap.get(source) == destination) rescuerMap.remove(source);
                    if (ensurerMap.get(source) == destination) ensurerMap.remove(source);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            for (Edge edge: toRemove) {
                graph.removeEdge(edge);
            }
        }

        deleteOrphanedBlocks(graph);
    }
    
    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder();

        
        for (BasicBlock b : getBasicBlocks()) {
            buf.append(b.toStringInstrs());
        }
        buf.append("\n\n------ Rescue block map ------\n");
        for (BasicBlock bb : rescuerMap.keySet()) {
            buf.append("BB ").append(bb.getID()).append(" --> BB ").append(rescuerMap.get(bb).getID()).append("\n");
        }
        buf.append("\n\n------ Ensure block map ------\n");
        for (BasicBlock bb : ensurerMap.keySet()) {
            buf.append("BB ").append(bb.getID()).append(" --> BB ").append(ensurerMap.get(bb).getID()).append("\n");
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

    void removeEdge(BasicBlock a, BasicBlock b) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    private LinkedList<BasicBlock> buildPostOrderList() {
        LinkedList<BasicBlock> list = new LinkedList<BasicBlock>();
        BasicBlock root = getEntryBB();
        Stack<BasicBlock> stack = new Stack<BasicBlock>();
        stack.push(root);
        BitSet bbSet = new BitSet(1 + getMaxNodeID());
        bbSet.set(root.getID());

        // Non-recursive post-order traversal (the added flag is required to handle cycles and common ancestors)
        while (!stack.empty()) {
            // Check if all children of the top of the stack have been added
            BasicBlock b = stack.peek();
            boolean allChildrenDone = true;
            for (BasicBlock dst : getOutgoingDestinations(b)) {
                int dstID = dst.getID();
                if (!bbSet.get(dstID)) {
                    allChildrenDone = false;
                    stack.push(dst);
                    bbSet.set(dstID);
                }
            }

            // If all children have been added previously, we are ready with 'b' in this round!
            if (allChildrenDone) {
                stack.pop();
                list.add(b);
            }
        }

        // Sanity check!
        for (BasicBlock b : getBasicBlocks()) {
            if (!bbSet.get(b.getID())) {
                printError("BB " + b.getID() + " missing from po list!");
                break;
            }
        }
        
        return list;
    }    
    
    private void printError(String message) {
        LOG.error(message + "\nGraph:\n" + this + "\nInstructions:\n" + toStringInstrs());
    }      
}

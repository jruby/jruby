package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.Tuple;
import org.jruby.compiler.ir.instructions.BranchInstr;
import org.jruby.compiler.ir.instructions.CaseInstr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.JUMP_INDIRECT_Instr;
import org.jruby.compiler.ir.instructions.LABEL_Instr;
import org.jruby.compiler.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.compiler.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.SET_RETADDR_Instr;
import org.jruby.compiler.ir.instructions.ThrowExceptionInstr;
import org.jruby.compiler.ir.instructions.YieldInstr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.LocalVariable;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.util.DirectedGraph;
import org.jruby.compiler.ir.util.Edge;

public class CFG {
    public enum EdgeType {
        REGULAR, DUMMY_EDGE, EXCEPTION_EDGE
    }

    IRExecutionScope scope;   // Scope (method/closure) to which this cfg belongs
    BasicBlock entryBB;        // Entry BB -- dummy
    BasicBlock exitBB;         // Exit BB -- dummy
    int nextBBId;       // Next available basic block id
    DirectedGraph<BasicBlock> cfg;           // The actual graph
    LinkedList<BasicBlock> postOrderList; // Post order traversal list of the cfg
    Map<String, DataFlowProblem> dfProbs;       // Map of name -> dataflow problem
    Map<Label, BasicBlock> bbMap;         // Map of label -> basic blocks with that label
    BasicBlock[] fallThruMap;   // Map of basic block id -> fall thru basic block
    List<BasicBlock> linearizedBBList;  // Linearized list of bbs
    Map<BasicBlock, BasicBlock> bbRescuerMap;  // Map of bb -> first bb of the rescue block that initiates exception handling for all exceptions thrown within this bb
    Map<BasicBlock, BasicBlock> bbEnsurerMap;   // Map of bb -> first bb of the ensure block that protects this bb
    List<ExceptionRegion> outermostERs;  // Outermost exception regions
    private Instr[] instrs;
    private Set<Variable> definedLocalVars;   // Local variables defined in this scope
    private Set<Variable> usedLocalVars;      // Local variables used in this scope

    public CFG(IRExecutionScope s) {
        nextBBId = 0; // Init before building basic blocks below!
        scope = s;
        postOrderList = null;
        dfProbs = new HashMap<String, DataFlowProblem>();
        bbMap = new HashMap<Label, BasicBlock>();
        outermostERs = new ArrayList<ExceptionRegion>();
        bbRescuerMap = new HashMap<BasicBlock, BasicBlock>();
        bbEnsurerMap = new HashMap<BasicBlock, BasicBlock>();
        instrs = null;
    }

    public DirectedGraph getGraph() {
        return cfg;
    }

    public IRExecutionScope getScope() {
        return scope;
    }

    public BasicBlock getEntryBB() {
        return entryBB;
    }

    public BasicBlock getExitBB() {
        return exitBB;
    }

    public int getNextBBID() {
        nextBBId++;
        return nextBBId;
    }

    public int getMaxNodeID() {
        return nextBBId;
    }

    // NOTE: Because nodes can be removed, this may be smaller than getMaxNodeID()
    public int numNodes() {
        return cfg.allData().size();
    }

    public Set<Edge<BasicBlock>> incomingEdgesOf(BasicBlock bb) {
        return cfg.vertexFor(bb).getIncomingEdges();
    }

    public Set<Edge<BasicBlock>> outgoingEdgesOf(BasicBlock bb) {
        return cfg.vertexFor(bb).getOutgoingEdges();
    }

    public Collection<BasicBlock> getNodes() {
        return cfg.allData();
    }

    public BasicBlock getTargetBB(Label l) {
        return bbMap.get(l);
    }

    public BasicBlock getRescuerBB(BasicBlock b) {
        return bbRescuerMap.get(b);
    }

    public boolean bbIsProtected(BasicBlock b) {
        // No need to look in ensurerMap because (_bbEnsurerMap(b) != null) => (_bbResucerMap(b) != null)
        return (getRescuerBB(b) != null);
    }

    // SSS FIXME: Extremely inefficient
    public int getRescuerPC(Instr excInstr) {
        for (BasicBlock b : linearizedBBList) {
            for (Instr i : b.getInstrs()) {
                if (i == excInstr) {
                    BasicBlock rescuerBB = bbRescuerMap.get(b);
                    return (rescuerBB == null) ? -1 : rescuerBB.getLabel().getTargetPC();
                }
            }
        }

        // SSS FIXME: Cannot happen! Throw runtime exception
        System.err.println("Fell through looking for rescuer ipc for " + excInstr);
        return -1;
    }

    // SSS FIXME: Extremely inefficient
    public int getEnsurerPC(Instr excInstr) {
        for (BasicBlock b : linearizedBBList) {
            for (Instr i : b.getInstrs()) {
                if (i == excInstr) {
                    BasicBlock ensurerBB = bbEnsurerMap.get(b);
                    return (ensurerBB == null) ? -1 : ensurerBB.getLabel().getTargetPC();
                }
            }
        }

        // SSS FIXME: Cannot happen! Throw runtime exception
        System.err.println("Fell through looking for ensurer ipc for " + excInstr);
        return -1;
    }

    /* Add 'b' as a global ensure block that protects all unprotected blocks in this scope */
    public void addGlobalEnsureBlock(BasicBlock geb) {
        cfg.addEdge(geb, exitBB, EdgeType.DUMMY_EDGE);
        
        for (BasicBlock basicBlock: cfg.allData()) {
            if (basicBlock != geb && !bbIsProtected(basicBlock)) {
                cfg.addEdge(basicBlock, geb, EdgeType.EXCEPTION_EDGE);
                bbRescuerMap.put(basicBlock, geb);
                bbEnsurerMap.put(basicBlock, geb);
            }
        }
    }

    private Label getNewLabel() {
        return scope.getNewLabel();
    }

    private BasicBlock createNewBB(Label l, DirectedGraph g, Map<Label, BasicBlock> bbMap, Stack<ExceptionRegion> nestedExceptionRegions) {
        BasicBlock b = new BasicBlock(this, l);
        bbMap.put(b.getLabel(), b);
        g.vertexFor(b);
        if (!nestedExceptionRegions.empty()) nestedExceptionRegions.peek().addBB(b);

        return b;
    }

    private BasicBlock createNewBB(DirectedGraph g, Map<Label, BasicBlock> bbMap, Stack<ExceptionRegion> nestedExceptionRegions) {
        return createNewBB(getNewLabel(), g, bbMap, nestedExceptionRegions);
    }

    private void removeBB(BasicBlock b) {
        cfg.removeVertexFor(b);
        bbMap.remove(b.getLabel());
        bbRescuerMap.remove(b);
        bbEnsurerMap.remove(b);
        // SSS FIXME: Patch up rescued regions as well??
    }

    private void addEdge(DirectedGraph g, BasicBlock src, Label tgt, Map<Label, BasicBlock> bbMap, Map<Label, List<BasicBlock>> forwardRefs) {
        BasicBlock tgtBB = bbMap.get(tgt);
        if (tgtBB != null) {
            g.addEdge(src, tgtBB, EdgeType.REGULAR);
        } else {
            // Add a forward reference from tgt -> src
            List<BasicBlock> frefs = forwardRefs.get(tgt);
            if (frefs == null) {
                frefs = new ArrayList<BasicBlock>();
                forwardRefs.put(tgt, frefs);
            }
            frefs.add(src);
        }
    }

    public Instr[] prepareForInterpretation() {
        if (instrs != null) return instrs; // Already prepared

        List<Instr> newInstrs = new ArrayList<Instr>();
        List<BasicBlock> bbs = null;
        try {
            bbs = linearize();
        } catch (RuntimeException e) {
            System.err.println("============= ERROR ================");
            System.err.println("Encountered exception: " + e + " while linearizing");
            System.err.println("\nGraph:\n" + getGraph().toString());
            System.err.println("\nInstructions:\n" + toStringInstrs());
            System.err.println("====================================");
            throw e;
        }

        // Set up a bb array that maps labels to targets -- just to make sure old code continues to work! 
        setupFallThruMap();

        // Set up IPCs
        HashMap<Label, Integer> labelIPCMap = new HashMap<Label, Integer>();
        List<Label> labelsToFixup = new ArrayList<Label>();
        int ipc = 0;
        for (BasicBlock b : bbs) {
            labelIPCMap.put(b.getLabel(), ipc);
            labelsToFixup.add(b.getLabel());
            for (Instr i : b.getInstrs()) {
                newInstrs.add(i);
                ipc++;
            }
        }

        // Fix up labels
        for (Label l : labelsToFixup) {
            l.setTargetPC(labelIPCMap.get(l));
        }

        // Exit BB ipc
        getExitBB().getLabel().setTargetPC(ipc + 1);

        instrs = newInstrs.toArray(new Instr[newInstrs.size()]);
        return instrs;
    }

    public Instr[] getInstrArray() {
        return instrs;
    }

    public void build(List<Instr> instrs) {
        // Map of label & basic blocks which are waiting for a bb with that label
        Map<Label, List<BasicBlock>> forwardRefs = new HashMap<Label, List<BasicBlock>>();

        // Map of return address variable and all possible targets (required to connect up ensure blocks with their targets)
        Map<Variable, Set<Label>> retAddrMap = new HashMap<Variable, Set<Label>>();
        Map<Variable, BasicBlock> retAddrTargetMap = new HashMap<Variable, BasicBlock>();

        // List of bbs that have a 'return' instruction
        List<BasicBlock> retBBs = new ArrayList<BasicBlock>();

        // List of bbs that have a 'throw' instruction
        List<BasicBlock> excBBs = new ArrayList<BasicBlock>();

        // Stack of nested rescue regions
        Stack<ExceptionRegion> nestedExceptionRegions = new Stack<ExceptionRegion>();

        // List of all rescued regions
        List<ExceptionRegion> allExceptionRegions = new ArrayList<ExceptionRegion>();

        DirectedGraph g = new DirectedGraph<BasicBlock>();
        
        // Dummy entry basic block (see note at end to see why)
        entryBB = createNewBB(g, bbMap, nestedExceptionRegions);

        // First real bb
        BasicBlock firstBB = createNewBB(g, bbMap, nestedExceptionRegions);

        // Build the rest!
        BasicBlock currBB = firstBB;
        BasicBlock newBB = null;
        boolean bbEnded = false;
        boolean bbEndedWithControlXfer = false;
        for (Instr i : instrs) {
            Operation iop = i.operation;
            if (iop == Operation.LABEL) {
                Label l = ((LABEL_Instr) i)._lbl;
                newBB = createNewBB(l, g, bbMap, nestedExceptionRegions);
                // Jump instruction bbs dont add an edge to the succeeding bb by default
                if (!bbEndedWithControlXfer) g.addEdge(currBB, newBB, EdgeType.REGULAR);
                currBB = newBB;

                // Add forward reference edges
                List<BasicBlock> frefs = forwardRefs.get(l);
                if (frefs != null) {
                    for (BasicBlock b : frefs) {
                        g.addEdge(b, newBB, EdgeType.REGULAR);
                    }
                }
                bbEnded = false;
                bbEndedWithControlXfer = false;
            } else if (bbEnded && (iop != Operation.EXC_REGION_END)) {
                newBB = createNewBB(g, bbMap, nestedExceptionRegions);
                // Jump instruction bbs dont add an edge to the succeeding bb by default
                if (!bbEndedWithControlXfer) g.addEdge(currBB, newBB, EdgeType.REGULAR); // currBB cannot be null!
                currBB = newBB;
                bbEnded = false;
                bbEndedWithControlXfer = false;
            }

            if (i instanceof ExceptionRegionStartMarkerInstr) {
// SSS: Do we need this anymore?
//                currBB.addInstr(i);
                ExceptionRegionStartMarkerInstr ersmi = (ExceptionRegionStartMarkerInstr) i;
                ExceptionRegion rr = new ExceptionRegion(ersmi._rescueBlockLabels, ersmi._ensureBlockLabel);
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
                if (i instanceof BranchInstr) {
                    tgt = ((BranchInstr) i).getJumpTarget();
                } else if (i instanceof JumpInstr) {
                    tgt = ((JumpInstr) i).getJumpTarget();
                    bbEndedWithControlXfer = true;
                } else if (i instanceof CaseInstr) {
                    // CASE IR instructions are dummy instructions
                    // -- all when/then clauses have been converted into if-then-else blocks
                    tgt = null;
                } else if (iop.isReturn()) { // BREAK, RETURN, CLOSURE_RETURN
                    tgt = null;
                    retBBs.add(currBB);
                    bbEndedWithControlXfer = true;
                } else if (i instanceof ThrowExceptionInstr) {
                    tgt = null;
                    excBBs.add(currBB);
                    bbEndedWithControlXfer = true;
                } else if (i instanceof JUMP_INDIRECT_Instr) {
                    tgt = null;
                    bbEndedWithControlXfer = true;
                    Set<Label> retAddrs = retAddrMap.get(((JUMP_INDIRECT_Instr) i).getJumpTarget());
                    for (Label l : retAddrs) {
                        addEdge(g, currBB, l, bbMap, forwardRefs);
                    }
                    // Record the target bb for the retaddr var for any set_addr instrs that appear later and use the same retaddr var
                    retAddrTargetMap.put(((JUMP_INDIRECT_Instr) i).getJumpTarget(), currBB);
                } else {
                    tgt = null;
                }

                if (tgt != null) addEdge(g, currBB, tgt, bbMap, forwardRefs);
            } else if (iop != Operation.LABEL) {
                currBB.addInstr(i);
            }

            if (i instanceof SET_RETADDR_Instr) {
                Variable v = i.getResult();
                Label tgtLbl = ((SET_RETADDR_Instr) i).getReturnAddr();
                BasicBlock tgtBB = retAddrTargetMap.get(v);
                // If we have the target bb, add the edge
                // If not, record it for fixup later
                if (tgtBB != null) {
                    addEdge(g, tgtBB, tgtLbl, bbMap, forwardRefs);
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
            BasicBlock firstRescueBB = getTargetBB(rr.getFirstRescueBlockLabel());

            // 1. Tell the region that firstRescueBB is its protector!
            rr.setFirstRescueBB(firstRescueBB);

            // 2. Record a mapping from the region's exclusive basic blocks to the first bb that will start exception handling for all their exceptions.
            // 3. Add an exception edge from every exclusive bb of the region to firstRescueBB
            for (BasicBlock b : rr.getExclusiveBBs()) {
                bbRescuerMap.put(b, firstRescueBB);
                if (rr.getEnsureBlockLabel() != null) {
                    bbEnsurerMap.put(b, getTargetBB(rr.getEnsureBlockLabel()));
                }
                g.addEdge(b, firstRescueBB, EdgeType.EXCEPTION_EDGE);
            }
        }

        // Dummy entry and exit basic blocks and other dummy edges are needed to maintain the CFG 
        // in a canonical form with certain invariants:
        // 1. all control begins with a single entry bb (and it dominates all other bbs in the cfg)
        // 2. all control ends with a single exit bb (and it post-dominates all other bbs in the cfg)
        //
        // So, add dummy edges from:
        // * dummy entry -> dummy exit
        // * dummy entry -> first basic block (real entry)
        // * all return bbs to the exit bb
        // * all exception bbs to the exit bb (mark these exception edges)
        // * last bb     -> dummy exit (only if the last bb didn't end with a control transfer!
        exitBB = createNewBB(g, bbMap, nestedExceptionRegions);
        g.addEdge(entryBB, exitBB, EdgeType.DUMMY_EDGE);
        g.addEdge(entryBB, firstBB, EdgeType.DUMMY_EDGE);
        for (BasicBlock rb : retBBs) {
            g.addEdge(rb, exitBB, EdgeType.DUMMY_EDGE);
        }
        for (BasicBlock rb : excBBs) {
            g.addEdge(rb, exitBB, EdgeType.EXCEPTION_EDGE);
        }
        if (!bbEndedWithControlXfer) {
            g.addEdge(currBB, exitBB, EdgeType.DUMMY_EDGE);
        }

        cfg = g;

        // remove useless cfg edges & orphaned bbs
        optimizeCFG();
    }

    /*
    private void setupBBArray() {
    int n = getMaxNodeID();
    _bbArray = new BasicBlock[n];
    for (BasicBlock x : _bbMap.values()) {
    _bbArray[x.getID() - 1] = x;
    }
    }
     */
    private void setupFallThruMap() {
        List<BasicBlock> bbs = linearize();
        fallThruMap = new BasicBlock[1 + getMaxNodeID()];
        BasicBlock prev = null;
        for (BasicBlock b : bbs) {
            if (prev != null) fallThruMap[prev.getID()] = b;
            prev = b;
        }
    }

    private void mergeBBs(BasicBlock a, BasicBlock b) {
        BasicBlock aR = bbRescuerMap.get(a);
        BasicBlock bR = bbRescuerMap.get(b);
        // We can merge 'a' and 'b' if one of the following is true:
        // 1. 'a' and 'b' are both not empty
        //    They are protected by the same rescue block.
        //    NOTE: We need not check the ensure block map because all ensure blocks are already
        //    captured in the bb rescue block map.  So, if aR == bR, it is guaranteed that the
        //    ensure blocks for the two are identical.
        // 2. One of 'a' or 'b' is empty.  We dont need to check for rescue block match because
        //    an empty basic block cannot raise an exception, can it.
        if ((aR == bR) || a.isEmpty() || b.isEmpty()) {
            a.swallowBB(b);
            cfg.removeEdge(a, b);
            for (Edge<BasicBlock> e : cfg.vertexFor(b).getOutgoingEdges()) {
                cfg.addEdge(a, e.getDestination().getData(), e.getType());
            }

            removeBB(b);

            // Update rescue and ensure maps
            if ((aR == null) && (bR != null)) {
                bbRescuerMap.put(a, bR);
                BasicBlock aE = bbEnsurerMap.get(a);
                BasicBlock bE = bbEnsurerMap.get(b);
                if ((aE == null) && (bE != null)) bbEnsurerMap.put(a, bE);
            }
        }
    }

    // callBB will only have a single successor & splitBB will only have a single predecessor
    // after inlining the callee.  Merge them with their successor/predecessors respectively
    private void mergeStraightlineBBs(BasicBlock callBB, BasicBlock splitBB) {
        Set<Edge<BasicBlock>> edges = outgoingEdgesOf(callBB);
        assert (edges.size() == 1);
        mergeBBs(callBB, edges.iterator().next().getDestination().getData());

        edges = incomingEdgesOf(splitBB);
        assert (edges.size() == 1);
        mergeBBs(edges.iterator().next().getSource().getData(), splitBB);
    }

    private void inlineClosureAtYieldSite(InlinerInfo ii, IRClosure cl, BasicBlock yieldBB, YieldInstr yield) {
        // Mark this closure as inlined so we dont run any destructive operations on it.
        // since the closure in its original form will get destroyed by the inlining.
        cl.markInlined();

        // 1. split yield site bb and move outbound edges from yield site bb to split bb.
        BasicBlock splitBB = yieldBB.splitAtInstruction(yield, getNewLabel(), false);
        cfg.vertexFor(splitBB);
        bbMap.put(splitBB.getLabel(), splitBB);
        List<Edge<BasicBlock>> edgesToRemove = new java.util.ArrayList<Edge<BasicBlock>>();
        for (Edge<BasicBlock> e : outgoingEdgesOf(yieldBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
            edgesToRemove.add(e);
        }
        // Ugh! I get exceptions if I try to pass the set I receive from outgoingEdgesOf!  What a waste!
        for (Edge e : edgesToRemove) {
            cfg.removeEdge(e);
        }

        // 2. Merge closure cfg into the current cfg
        // NOTE: No need to clone basic blocks in the closure because they are part of the caller's cfg
        // and is being merged in at the yield site -- there is no need for the closure after the merge.
        CFG ccfg = cl.getCFG();
        BasicBlock cEntry = ccfg.getEntryBB();
        BasicBlock cExit = ccfg.getExitBB();
        for (BasicBlock b : ccfg.getNodes()) {
            if (b != cEntry && b != cExit) {
                cfg.vertexFor(b);
                bbMap.put(b.getLabel(), b);
                b.updateCFG(this);
                b.processClosureArgAndReturnInstrs(ii, yield);
            }
        }
        for (BasicBlock b : ccfg.getNodes()) {
            if (b != cEntry && b != cExit) {
                for (Edge<BasicBlock> e : ccfg.outgoingEdgesOf(b)) {
                    BasicBlock c = e.getDestination().getData();
                    if (c != cExit) cfg.addEdge(b, c, e.getType());
                }
            }
        }
        
        for (Edge<BasicBlock> e : cfg.vertexFor(cEntry).getOutgoingEdges()) {
            BasicBlock destination = e.getDestination().getData();
            if (destination != cExit) cfg.addEdge(yieldBB, destination, e.getType());
        }
        
        for (Edge<BasicBlock> e : cfg.vertexFor(cExit).getIncomingEdges()) {
            BasicBlock source = e.getSource().getData();
            if (source != cEntry) {
                if (e.getType() == EdgeType.EXCEPTION_EDGE) {
                    // e._src has an explicit throw that returns from the closure
                    // after inlining, if the yield instruction has a rescuer, then the
                    // throw has to be captured by the rescuer as well.
                    BasicBlock rescuerOfSplitBB = bbRescuerMap.get(splitBB);
                    cfg.addEdge(source, rescuerOfSplitBB != null ? rescuerOfSplitBB : exitBB, EdgeType.EXCEPTION_EDGE);
                } else {
                    cfg.addEdge(source, splitBB, e.getType());
                }
            }
        }

        // 5. No need to clone rescued regions -- just assimilate them
        for (ExceptionRegion r : ccfg.outermostERs) {
            outermostERs.add(r);
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as yieldB
        BasicBlock yieldBBrescuer = bbRescuerMap.get(yieldBB);
        if (yieldBBrescuer != null) bbRescuerMap.put(splitBB, yieldBBrescuer);

        BasicBlock yieldBBensurer = bbEnsurerMap.get(yieldBB);
        if (yieldBBensurer != null) bbEnsurerMap.put(splitBB, yieldBBensurer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by yieldBBrescuer/yieldBBensurer
        Map<BasicBlock, BasicBlock> cRescuerMap = ccfg.bbRescuerMap;
        Map<BasicBlock, BasicBlock> cEnsurerMap = ccfg.bbEnsurerMap;
        for (BasicBlock cb : ccfg.getNodes()) {
            if (cb != cEntry && cb != cExit) {
                BasicBlock cbProtector = cRescuerMap.get(cb);
                if (cbProtector != null) {
                    bbRescuerMap.put(cb, cbProtector);
                } else if (yieldBBrescuer != null) {
                    bbRescuerMap.put(cb, yieldBBrescuer);
                }

                BasicBlock cbEnsurer = cEnsurerMap.get(cb);
                if (cbEnsurer != null) {
                    bbEnsurerMap.put(cb, cbEnsurer);
                } else if (yieldBBensurer != null) {
                    bbEnsurerMap.put(cb, yieldBBensurer);
                }
            }
        }

        // 7. callBB will only have a single successor & splitBB will only have a single predecessor
        //    after inlining the callee.  Merge them with their successor/predecessors respectively
        //    Merge only after fixing up the rescuer map above
        mergeStraightlineBBs(yieldBB, splitBB);
    }

    public void inlineMethod(IRMethod m, BasicBlock callBB, CallInstr call) {
        InlinerInfo ii = new InlinerInfo(call, this);

        // 1. split callsite bb and move outbound edges from callsite bb to split bb.
        BasicBlock splitBB = callBB.splitAtInstruction(call, getNewLabel(), false);
        bbMap.put(splitBB.getLabel(), splitBB);
        cfg.vertexFor(splitBB);
        List<Edge<BasicBlock>> edgesToRemove = new java.util.ArrayList<Edge<BasicBlock>>();
        for (Edge<BasicBlock> e : outgoingEdgesOf(callBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
            edgesToRemove.add(e);
        }
        // Ugh! I get exceptions if I try to pass the set I receive from outgoingEdgesOf!  What a waste! 
        // That is why I build the new list edgesToRemove
        for (Edge edge: edgesToRemove) {
            cfg.removeEdge(edge);
        }

        // 2. clone callee
        CFG mcfg = m.getCFG();
        BasicBlock mEntry = mcfg.getEntryBB();
        BasicBlock mExit = mcfg.getExitBB();
        DirectedGraph g = new DirectedGraph<BasicBlock>();
        for (BasicBlock b : mcfg.getNodes()) {
            if (b != mEntry && b != mExit) {
                BasicBlock bCloned = b.cloneForInlining(ii);
                cfg.vertexFor(bCloned);
                bbMap.put(bCloned.getLabel(), bCloned);
            }
        }

        // 3. set up new edges
        for (BasicBlock x : mcfg.getNodes()) {
            if (x != mEntry && x != mExit) {
                BasicBlock rx = ii.getRenamedBB(x);
                for (Edge<BasicBlock> e : mcfg.outgoingEdgesOf(x)) {
                    BasicBlock b = e.getDestination().getData();
                    if (b != mExit) {
                        cfg.addEdge(rx, ii.getRenamedBB(b), e.getType());
                    }
                }
            }
        }

        // 4. Hook up entry/exit edges
        for (Edge<BasicBlock> e : mcfg.outgoingEdgesOf(mEntry)) {
            BasicBlock destination = e.getDestination().getData();
            if (destination != mExit) {
                cfg.addEdge(callBB, ii.getRenamedBB(destination), e.getType());
            }
        }

        for (Edge<BasicBlock> e : mcfg.incomingEdgesOf(mExit)) {
            BasicBlock source = e.getSource().getData();
            if (source != mEntry) {
                if (e.getType() == EdgeType.EXCEPTION_EDGE) {
                    // e._src has an explicit throw that returns from the callee
                    // after inlining, if the caller instruction has a rescuer, then the
                    // throw has to be captured by the rescuer as well.
                    BasicBlock rescuerOfSplitBB = bbRescuerMap.get(splitBB);
                    cfg.addEdge(ii.getRenamedBB(source), rescuerOfSplitBB != null ? rescuerOfSplitBB : exitBB, EdgeType.EXCEPTION_EDGE);
                } else {
                    cfg.addEdge(ii.getRenamedBB(source), splitBB, e.getType());
                }
            }
        }

        // 5. Clone exception regions
        for (ExceptionRegion r : mcfg.outermostERs) {
            outermostERs.add(r.cloneForInlining(ii));
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as callBB
        BasicBlock callBBrescuer = bbRescuerMap.get(callBB);
        if (callBBrescuer != null) bbRescuerMap.put(splitBB, callBBrescuer);

        BasicBlock callBBensurer = bbEnsurerMap.get(callBB);
        if (callBBensurer != null) bbEnsurerMap.put(splitBB, callBBensurer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by callBBrescuer.
        Map<BasicBlock, BasicBlock> mRescuerMap = mcfg.bbRescuerMap;
        Map<BasicBlock, BasicBlock> mEnsurerMap = mcfg.bbEnsurerMap;
        for (BasicBlock x : mcfg.getNodes()) {
            if (x != mEntry && x != mExit) {
                BasicBlock xRenamed = ii.getRenamedBB(x);
                BasicBlock xProtector = mRescuerMap.get(x);
                if (xProtector != null) {
                    bbRescuerMap.put(xRenamed, ii.getRenamedBB(xProtector));
                } else if (callBBrescuer != null) {
                    bbRescuerMap.put(xRenamed, callBBrescuer);
                }

                BasicBlock xEnsurer = mEnsurerMap.get(x);
                if (xEnsurer != null) {
                    bbEnsurerMap.put(xRenamed, ii.getRenamedBB(xEnsurer));
                } else if (callBBensurer != null) {
                    bbEnsurerMap.put(xRenamed, callBBensurer);
                }
            }
        }

        // 7. callBB will only have a single successor & splitBB will only have a single predecessor
        //    after inlining the callee.  Merge them with their successor/predecessors respectively
        //    Merge only after fixing up the rescuer map above
        mergeStraightlineBBs(callBB, splitBB);

        // 8. Inline any closure argument passed into the call.
        Operand closureArg = call.getClosureArg();
        List yieldSites = ii.getYieldSites();
        if (closureArg != null && !yieldSites.isEmpty()) {
            // Detect unlikely but contrived scenarios where there are far too many yield sites that could lead to code blowup
            // if we inline the closure at all those yield sites!
            if (yieldSites.size() > 1) {
                throw new RuntimeException("Encountered " + yieldSites.size() + " yield sites.  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");
            }

            if (!(closureArg instanceof MetaObject)) {
                throw new RuntimeException("Encountered a dynamic closure arg.  Cannot inline it here!  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");
            }

            Tuple t = (Tuple) yieldSites.get(0);
            inlineClosureAtYieldSite(ii, (IRClosure) ((MetaObject) closureArg).scope, (BasicBlock) t.a, (YieldInstr) t.b);
        }

        // Update the bb array
        setupFallThruMap();
    }

    private void buildPostOrderTraversal() {
        postOrderList = new LinkedList<BasicBlock>();
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
            for (Edge<BasicBlock> e : cfg.vertexFor(b).getOutgoingEdges()) {
                BasicBlock dst = e.getDestination().getData();
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
                postOrderList.add(b);
            }
        }

        // Sanity check!
        for (BasicBlock b : getNodes()) {
            if (!bbSet.get(b.getID())) {
                System.err.println("BB " + b.getID() + " missing from po list!");
                System.err.println("CFG: " + cfg);
                System.err.println("Instrs: " + toStringInstrs());
                break;
            }
        }
    }

    public ListIterator<BasicBlock> getPostOrderTraverser() {
        if (postOrderList == null) buildPostOrderTraversal();

        return postOrderList.listIterator();
    }

    public ListIterator<BasicBlock> getReversePostOrderTraverser() {
        if (postOrderList == null) buildPostOrderTraversal();

        return postOrderList.listIterator(numNodes());
    }

    private Integer intersectDomSets(Integer[] idomMap, Integer nb1, Integer nb2) {
        while (nb1 != nb2) {
            while (nb1 < nb2) {
                nb1 = idomMap[nb1];
            }
            while (nb2 < nb1) {
                nb2 = idomMap[nb2];
            }
        }

        return nb1;
    }

    public void buildDominatorTree() {
        int maxNodeId = getMaxNodeID();

        // Set up a map of bbid -> post order numbering
        Integer[] bbToPoNumbers = new Integer[maxNodeId + 1];
        BasicBlock[] poNumbersToBB = new BasicBlock[maxNodeId + 1];
        ListIterator<BasicBlock> it = getPostOrderTraverser();
        int n = 0;
        while (it.hasNext()) {
            BasicBlock b = it.next();
            bbToPoNumbers[b.getID()] = n;
            poNumbersToBB[n] = b;
            n++;
        }

        // Construct the dominator sets using the fast dominance algorithm by
        // Keith D. Cooper, Timothy J. Harvey, and Ken Kennedy.
        // http://www.cs.rice.edu/~keith/EMBED/dom.pdf (tip courtesy Slava Pestov)
        //
        // Faster than the standard iterative data-flow algorithm
        //
        // This maps a bb's post-order number to the bb's idom post-order number.
        // We convert this po-number -> po-number map to a bb -> bb map later on!
        Integer[] idoms = new Integer[maxNodeId + 1];

        BasicBlock root = getEntryBB();
        Integer rootPoNumber = bbToPoNumbers[root.getID()];
        idoms[rootPoNumber] = rootPoNumber;
        boolean changed = true;
        while (changed) {
            changed = false;
            it = getReversePostOrderTraverser();
            while (it.hasPrevious()) {
                BasicBlock b = it.previous();
                if (b == root) continue;

                // Non-root -- process it
                Integer bPoNumber = bbToPoNumbers[b.getID()];
                Integer oldBIdom = idoms[bPoNumber];
                Integer newBIdom = null;

                // newBIdom is initialized to be some (first-encountered, for ex.) processed predecessor of 'b'.
                for (Edge<BasicBlock> e : cfg.vertexFor(b).getIncomingEdges()) {
                    BasicBlock src = e.getSource().getData();
                    Integer srcPoNumber = bbToPoNumbers[src.getID()];
                    if (idoms[srcPoNumber] != null) {
//                        System.out.println("Initialized idom(" + bPoNumber + ")=" + srcPoNumber);
                        newBIdom = srcPoNumber;
                        break;
                    }
                }

                // newBIdom should not be null
                assert newBIdom != null;

                // Now, intersect dom sets of all of b's predecessors 
                Integer processedPred = newBIdom;
                for (Edge<BasicBlock> e : cfg.vertexFor(b).getIncomingEdges()) {
                    // Process b's predecessors except the initialized bidom value
                    BasicBlock src = e.getSource().getData();
                    Integer srcPoNumber = bbToPoNumbers[src.getID()];
                    Integer srcIdom = idoms[srcPoNumber];
                    if ((srcIdom != null) && (srcPoNumber != processedPred)) {
//                        Integer old = newBIdom;
                        newBIdom = intersectDomSets(idoms, srcPoNumber, newBIdom);
//                        System.out.println("Intersect " + srcIdom + " & " + old + " = " + newBIdom);
                    }
                }

                // Has something changed?
                if (oldBIdom != newBIdom) {
                    changed = true;
                    idoms[bPoNumber] = newBIdom;
//                    System.out.println("Changed: idom(" + bPoNumber + ")= " + newBIdom);
                }
            }
        }

        // Convert the idom map based on post order numbers to one based on basic blocks
        Map<BasicBlock, BasicBlock> idomMap = new HashMap<BasicBlock, BasicBlock>();
        for (Integer i = 0; i < maxNodeId; i++) {
            idomMap.put(poNumbersToBB[i], poNumbersToBB[idoms[i]]);
//            System.out.println("IDOM(" + poNumbersToBB[i].getID() + ") = " + poNumbersToBB[idoms[i]].getID());
        }
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder();
        for (BasicBlock b : getNodes()) {
            buf.append(b.toStringInstrs());
        }

        buf.append("\n\n------ Rescue block map ------\n");
        for (BasicBlock bb : bbRescuerMap.keySet()) {
            buf.append("BB ").append(bb.getID()).append(" --> BB ").append(bbRescuerMap.get(bb).getID()).append("\n");
        }
        buf.append("\n\n------ Ensure block map ------\n");
        for (BasicBlock bb : bbEnsurerMap.keySet()) {
            buf.append("BB ").append(bb.getID()).append(" --> BB ").append(bbEnsurerMap.get(bb).getID()).append("\n");
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

    public void setDataFlowSolution(String name, DataFlowProblem p) {
        dfProbs.put(name, p);
    }

    public DataFlowProblem getDataFlowSolution(String name) {
        return dfProbs.get(name);
    }

    private void pushBBOnStack(Stack<BasicBlock> stack, BitSet bbSet, BasicBlock bb) {
        if (!bbSet.get(bb.getID())) {
            // System.out.println("pushing " + bb);
            stack.push(bb);
            bbSet.set(bb.getID());
        }
    }

    public void deleteOrphanedBlocks() {
        // System.out.println("\nGraph:\n" + getGraph().toString());
        // System.out.println("\nInstructions:\n" + toStringInstrs());

        // FIXME: Quick and dirty implementation
        while (true) {
            BasicBlock bbToRemove = null;
            for (BasicBlock b : getNodes()) {
                if (b == entryBB) continue; // Skip entry bb!

                // Every other bb should have at least one incoming edge
                if (incomingEdgesOf(b).isEmpty()) {
                    bbToRemove = b;
                    break;
                }
            }
            if (bbToRemove == null) break;

//                System.out.println("Removing orphaned BB: " + bbToRemove);
            removeBB(bbToRemove);
        }
    }

    public void splitCalls() {
        // FIXME: (Enebo) We are going to make a SplitCallInstr so this logic can be separate
        // from unsplit calls.  Comment out until new SplitCall is created.
//        for (BasicBlock b: getNodes()) {
//            List<Instr> bInstrs = b.getInstrs();
//            for (ListIterator<Instr> it = ((ArrayList<Instr>)b.getInstrs()).listIterator(); it.hasNext(); ) {
//                Instr i = it.next();
//                // Only user calls, not Ruby & JRuby internal calls
//                if (i.operation == Operation.CALL) {
//                    CallInstr call = (CallInstr)i;
//                    Operand   r    = call.getReceiver();
//                    Operand   m    = call.getMethodAddr();
//                    Variable  mh   = _scope.getNewTemporaryVariable();
//                    MethodLookupInstr mli = new MethodLookupInstr(mh, m, r);
//                    // insert method lookup at the right place
//                    it.previous();
//                    it.add(mli);
//                    it.next();
//                    // update call address
//                    call.setMethodAddr(mh);
//                }
//            }
//        }
//
//        List<IRClosure> closures = _scope.getClosures();
//        if (!closures.isEmpty()) {
//            for (IRClosure c : closures) {
//                c.getCFG().splitCalls();
//            }
//        }
    }

    public void optimizeCFG() {
        // SSS FIXME: Can't we not add some of these exception edges in the first place??
        // Remove exception edges from blocks that couldn't possibly thrown an exception!
        List<Edge> toRemove = new ArrayList<Edge>();
        for (BasicBlock b : getNodes()) {
            boolean noExceptions = true;
            for (Instr i : b.getInstrs()) {
                if (i.canRaiseException()) {
                    noExceptions = false;
                    break;
                }
            }

            if (noExceptions) {
                for (Edge<BasicBlock> e : cfg.vertexFor(b).getOutgoingEdgesOfType(EdgeType.EXCEPTION_EDGE)) {
                    toRemove.add(e);
                        
                    if (bbRescuerMap.get(e.getSource()) == e.getDestination().getData()) {
                        bbRescuerMap.remove(e.getSource());
                    }
                    if (bbEnsurerMap.get(e.getSource()) == e.getDestination().getData()) {
                        bbEnsurerMap.remove(e.getSource());
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            for (Edge edge: toRemove) {
                cfg.removeEdge(edge);
            }
        }

        deleteOrphanedBlocks();
    }

    public List<BasicBlock> linearize() {
        if (linearizedBBList != null) return linearizedBBList; // Already linearized

        // System.out.println("--- start ---");
        linearizedBBList = new ArrayList<BasicBlock>();

        // Linearize the basic blocks of the cfg!
        // This is a simple linearization -- nothing fancy
        BasicBlock root = getEntryBB();
        BitSet bbSet = new BitSet(1 + getMaxNodeID());
        bbSet.set(root.getID());
        Stack<BasicBlock> stack = new Stack<BasicBlock>();

        // Push all exception edge targets (first bbs of rescue blocks) first
        // so that rescue handlers are laid out last at the end of the method,
        // outside the common execution path.
        for (Edge<BasicBlock> edge: cfg.edgesOfType(EdgeType.EXCEPTION_EDGE)) {
            pushBBOnStack(stack, bbSet, edge.getDestination().getData());
        }

        // Root next!
        stack.push(root);

        while (!stack.empty()) {
            BasicBlock b = stack.pop();
            // System.out.println("processing bb: " + b.getID());
            linearizedBBList.add(b);

            if (b == exitBB) {
                assert stack.empty();
            } else if (b == entryBB) {
                int i = 0;
                BasicBlock[] bs = new BasicBlock[3];
                for (Edge<BasicBlock> e : cfg.vertexFor(b).getOutgoingEdges()) {
                    bs[i++] = e.getDestination().getData();
                }
                BasicBlock b1 = bs[0], b2 = bs[1], b3 = bs[2];
                if (b3 != null) {
                    pushBBOnStack(stack, bbSet, b3);
                }
                if (b2 == null) {
                    pushBBOnStack(stack, bbSet, b1);
                } else if (b1.getID() < b2.getID()) {
                    pushBBOnStack(stack, bbSet, b2);
                    pushBBOnStack(stack, bbSet, b1);
                } else {
                    pushBBOnStack(stack, bbSet, b1);
                    pushBBOnStack(stack, bbSet, b2);
                }
            } else {
                // Find the basic block that is the target of the 'taken' branch
                Instr lastInstr = b.getLastInstr();
                if (lastInstr == null) {
                    // Only possible for the root block with 2 edges + blocks with just 1 target with no instructions
                    BasicBlock b1 = null, b2 = null;
                    for (Edge<BasicBlock> e : cfg.vertexFor(b).getOutgoingEdges()) {
                        if (b1 == null) {
                            b1 = e.getDestination().getData();
                        } else if (b2 == null) {
                            b2 = e.getDestination().getData();
                        } else {
                            System.err.println("============= ERROR ================");
                            System.err.println("Encountered bb: " + b.getID() + " with no instrs. and more than 2 targets!!");
                            System.err.println("\nGraph:\n" + getGraph().toString());
                            System.err.println("\nInstructions:\n" + toStringInstrs());
                            System.err.println("====================================");
                            throw new RuntimeException("Encountered bb: " + b.getID() + " with no instrs. and more than 2 targets!!");
                        }
                    }

                    assert (b1 != null);
                    if (b2 == null) {
                        pushBBOnStack(stack, bbSet, b1);
                    } else if (b1.getID() < b2.getID()) {
                        pushBBOnStack(stack, bbSet, b2);
                        pushBBOnStack(stack, bbSet, b1);
                    } else {
                        pushBBOnStack(stack, bbSet, b1);
                        pushBBOnStack(stack, bbSet, b2);
                    }
                } else {
                    // System.out.println("last instr is: " + lastInstr);
                    BasicBlock blockToIgnore = null;
                    if (lastInstr instanceof JumpInstr) {
                        blockToIgnore = bbMap.get(((JumpInstr) lastInstr).target);

                        // Check if all of blockToIgnore's predecessors get to it with a jump!
                        // This can happen because of exceptions and rescue handlers
                        // If so, dont ignore it.  Process it right away (because everyone will end up ignoring this block!)
                        boolean allJumps = true;
                        for (Edge<BasicBlock> e : cfg.vertexFor(blockToIgnore).getIncomingEdges()) {
                            if (!(e.getSource().getData().getLastInstr() instanceof JumpInstr)) {
                                allJumps = false;
                            }
                        }

                        if (allJumps) blockToIgnore = null;
                    } else if (lastInstr instanceof BranchInstr) {
                        // Push the taken block onto the stack first so that it gets processed last!
                        BasicBlock takenBlock = bbMap.get(((BranchInstr) lastInstr).getJumpTarget());
                        pushBBOnStack(stack, bbSet, takenBlock);
                        blockToIgnore = takenBlock;
                    }

                    // Push everything else
                    for (Edge<BasicBlock> e : cfg.vertexFor(b).getOutgoingEdges()) {
                        BasicBlock x = e.getDestination().getData();
                        if (x != blockToIgnore) pushBBOnStack(stack, bbSet, x);
                    }
                }
                assert !stack.empty();
            }
        }

        // Verify that all bbs have been laid out!
        for (BasicBlock b : cfg.allData()) {
            if (!bbSet.get(b.getID())) {
                throw new RuntimeException("Bad CFG linearization: BB " + b.getID() + " has been missed!");
            }
        }

        // Fixup (add/remove) jumps where appropriate
        int n = linearizedBBList.size();
        for (int i = 0; i < n; i++) {
            BasicBlock curr = linearizedBBList.get(i);
            Instr li = curr.getLastInstr();
            if ((i + 1) < n) {
                BasicBlock next = linearizedBBList.get(i + 1);

                // If curr ends in a jump to next, remove the jump!
                if (li instanceof JumpInstr) {
                    if (next == bbMap.get(((JumpInstr) li).target)) {
//                        System.out.println("BB " + curr.getID() + " falls through in layout to BB " + next.getID() + ".  Removing jump from former bb!"); 
                        curr.removeInstr(li);
                    }
                } // If curr has a single successor and next is not it, and curr does't end in a control transfer instruction, add a jump!
                else {
                    Set<Edge<BasicBlock>> succs = cfg.vertexFor(curr).getOutgoingEdges();
                    if (succs.size() == 1) {
                        BasicBlock tgt = succs.iterator().next().getDestination().getData();
                        if ((tgt != next) && ((li == null) || !li.operation.transfersControl())) {
//                            System.out.println("BB " + curr.getID() + " doesn't fall through to " + next.getID() + ".  Adding a jump to " + tgt._label);
                            curr.addInstr(new JumpInstr(tgt.getLabel()));
                        }
                    }
                }

                if (curr == exitBB) {
                    // Add a dummy ret
//                    System.out.println("Exit bb is not the last bb in the layout!  Adding a dummy return!");
                    curr.addInstr(new ReturnInstr(Nil.NIL));
                }
            } else if (curr != exitBB) {
                Set<Edge<BasicBlock>> succs = cfg.vertexFor(curr).getOutgoingEdges();
                assert succs.size() == 1;
                BasicBlock tgt = succs.iterator().next().getDestination().getData();
                if ((li == null) || !li.operation.transfersControl()) {
//                    System.out.println("BB " + curr.getID() + " is the last bb in the layout! Adding a jump to " + tgt._label);
                    curr.addInstr(new JumpInstr(tgt.getLabel()));
                }
            }
        }
        // System.out.println("--- end ---");

        return linearizedBBList;
    }

    @Override
    public String toString() {
        return "CFG[" + scope.getScopeName() + ":" + scope.getName() + "]";
    }

    public void setUpUseDefLocalVarMaps() {
        definedLocalVars = new java.util.HashSet<Variable>();
        usedLocalVars = new java.util.HashSet<Variable>();
        for (BasicBlock bb : cfg.allData()) {
            for (Instr i : bb.getInstrs()) {
                for (Variable v : i.getUsedVariables()) {
                    if (v instanceof LocalVariable) usedLocalVars.add(v);
                }
                Variable v = i.getResult();
                if ((v != null) && (v instanceof LocalVariable)) definedLocalVars.add(v);
            }
        }

        for (IRClosure cl : getScope().getClosures()) {
            cl.getCFG().setUpUseDefLocalVarMaps();
        }
    }

    public boolean usesLocalVariable(Variable v) {
        if (usedLocalVars == null) setUpUseDefLocalVarMaps();
        if (usedLocalVars.contains(v)) return true;

        for (IRClosure cl : getScope().getClosures()) {
            if (cl.getCFG().usesLocalVariable(v)) return true;
        }

        return false;
    }

    public boolean definesLocalVariable(Variable v) {
        if (definedLocalVars == null) setUpUseDefLocalVarMaps();
        if (definedLocalVars.contains(v)) return true;

        for (IRClosure cl : getScope().getClosures()) {
            if (cl.getCFG().definesLocalVariable(v)) return true;
        }

        return false;
    }
}

package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.BitSet;
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
import org.jruby.compiler.ir.instructions.BREAK_Instr;
import org.jruby.compiler.ir.instructions.CaseInstr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.JUMP_INDIRECT_Instr;
import org.jruby.compiler.ir.instructions.LABEL_Instr;
import org.jruby.compiler.ir.instructions.MethodLookupInstr;
import org.jruby.compiler.ir.instructions.RESCUED_BODY_START_MARKER_Instr;
import org.jruby.compiler.ir.instructions.RESCUED_BODY_END_MARKER_Instr;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.SET_RETADDR_Instr;
import org.jruby.compiler.ir.instructions.THROW_EXCEPTION_Instr;
import org.jruby.compiler.ir.instructions.YieldInstr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;

import org.jgrapht.*;
import org.jgrapht.graph.*;

public class CFG {
    public enum CFG_Edge_Type { REGULAR, DUMMY_EDGE, JUMP_EDGE, FALLTHRU_EDGE, FORWARD_EDGE, BACK_EDGE, EXIT_EDGE, EXCEPTION_EDGE }

    public static class CFG_Edge {
        final public BasicBlock _src;
        final public BasicBlock _dst;
        public CFG_Edge_Type _type;

        public CFG_Edge(BasicBlock s, BasicBlock d) {
            _src = s;
            _dst = d;
            _type = CFG_Edge_Type.REGULAR;   // Unknown type to start with
        }

        @Override
        public String toString() {
            return "<" + _src.getID() + " --> " + _dst.getID() + "> (" + _type + ")";
        }
    }

    IRExecutionScope _scope;   // Scope (method/closure) to which this cfg belongs
    BasicBlock _entryBB;        // Entry BB -- dummy
    BasicBlock _exitBB;         // Exit BB -- dummy
    int        _nextBBId;       // Next available basic block id
    DirectedGraph<BasicBlock, CFG_Edge> _cfg;           // The actual graph
    LinkedList<BasicBlock>              _postOrderList; // Post order traversal list of the cfg
    Map<String, DataFlowProblem>        _dfProbs;       // Map of name -> dataflow problem
    Map<Label, BasicBlock>              _bbMap;         // Map of label -> basic blocks with that label
    BasicBlock[]                        _fallThruMap;   // Map of basic block id -> fall thru basic block
    List<BasicBlock>                    _linearizedBBList;  // Linearized list of bbs
    Map<BasicBlock, BasicBlock>         _bbRescuerMap;  // Map of bb -> first bb of the rescue block that initiates exception handling for all exceptions thrown within this bb
    List<RescuedRegion>                 _outermostRRs;  // Outermost rescued regions

    public CFG(IRExecutionScope s) {
        _nextBBId = 0; // Init before building basic blocks below!
        _scope = s;
        _postOrderList = null;
        _dfProbs = new HashMap<String, DataFlowProblem>();
        _bbMap = new HashMap<Label, BasicBlock>();
        _outermostRRs = new ArrayList<RescuedRegion>();
        _bbRescuerMap = new HashMap<BasicBlock, BasicBlock>();
    }

    public DirectedGraph getGraph() {
        return _cfg;
    }

    public IRExecutionScope getScope() {
        return _scope;
    }

    public BasicBlock getEntryBB() {
        return _entryBB;
    }

    public BasicBlock getExitBB() {
        return _exitBB;
    }

    public int getNextBBID() {
        _nextBBId++;
        return _nextBBId;
    }

    public int getMaxNodeID() {
        return _nextBBId;
    }

    // NOTE: Because nodes can be removed, this may be smaller than getMaxNodeID()
    public int numNodes() {
        return _cfg.vertexSet().size();
    }

    public Set<CFG_Edge> incomingEdgesOf(BasicBlock bb) {
        return _cfg.incomingEdgesOf(bb);
    }

    public Set<CFG_Edge> outgoingEdgesOf(BasicBlock bb) {
        return _cfg.outgoingEdgesOf(bb);
    }

    public Set<BasicBlock> getNodes() {
        return _cfg.vertexSet();
    }

    // SSS FIXME: This is only valid temporarily while the cfg blocks
    // haven't been reordered around.  This code is there temporarily
    // to get Tom & Charlie started on the IR interpreter.
    public BasicBlock getFallThroughBB(BasicBlock bb) {
        return _fallThruMap[bb.getID()];
    }

    public BasicBlock getTargetBB(Label l) {
        return _bbMap.get(l);
    }

    private Label getNewLabel() {
        return _scope.getNewLabel();
    }

    private BasicBlock createNewBB(Label l, DirectedGraph<BasicBlock, CFG_Edge> g, Map<Label, BasicBlock> bbMap, Stack<RescuedRegion> nestedRescuedRegions) {
        BasicBlock b = new BasicBlock(this, l);
        bbMap.put(b._label, b);
        g.addVertex(b);
        if (!nestedRescuedRegions.empty())
            nestedRescuedRegions.peek().addBB(b);
        return b;
    }

    private BasicBlock createNewBB(DirectedGraph<BasicBlock, CFG_Edge> g, Map<Label, BasicBlock> bbMap, Stack<RescuedRegion> nestedRescuedRegions) {
        return createNewBB(getNewLabel(), g, bbMap, nestedRescuedRegions);
    }

    private void removeBB(BasicBlock b) {
        _cfg.removeVertex(b);
        _bbMap.remove(b._label);
        _bbRescuerMap.remove(b);
        // SSS FIXME: Patch up rescued regions as well??
    }

    private void addEdge(DirectedGraph<BasicBlock, CFG_Edge> g, BasicBlock src, Label tgt, Map<Label, BasicBlock> bbMap, Map<Label, List<BasicBlock>> forwardRefs) {
        BasicBlock tgtBB = bbMap.get(tgt);
        if (tgtBB != null) {
            g.addEdge(src, tgtBB);
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

    private DefaultDirectedGraph<BasicBlock, CFG_Edge> getNewCFG() {
        return new DefaultDirectedGraph<BasicBlock, CFG_Edge>(
                    new EdgeFactory<BasicBlock, CFG_Edge>() {
                        public CFG_Edge createEdge(BasicBlock s, BasicBlock d) { return new CFG_Edge(s, d); }
                    });
    }

    private Instr[] _instrs = null;
    public Instr[] prepareForInterpretation() {
        if (_instrs != null) // Done
            return _instrs;

        List<Instr> instrs = new ArrayList<Instr>();
        List<BasicBlock> bbs = linearize();

        // Set up a bb array that maps labels to targets -- just to make sure old code continues to work! 
        setupFallThruMap();

        // Set up IPCs
        HashMap<Label, Integer> labelIPCMap = new HashMap<Label, Integer>();
        List<Label> labelsToFixup = new ArrayList<Label>();
        int ipc = 0;
        for (BasicBlock b: bbs) {
            labelIPCMap.put(b.getLabel(), ipc);
            for (Instr i: b.getInstrs()) {
                instrs.add(i);
                Operation op = i.operation;
                if (op == Operation.BEQ)
                    labelsToFixup.add(((BranchInstr)i).getJumpTarget());
                else if (op == Operation.JUMP) 
                    labelsToFixup.add(((JumpInstr)i).getJumpTarget());
                else if (op == Operation.RECV_OPT_ARG || op == Operation.SET_RETADDR) {
                    for (Operand o: i.getOperands()) {
                        if (o instanceof Label) {
                            labelsToFixup.add((Label)o);
                        }
                    }
                }
                ipc++;
            }
        }

        // Fix up labels
        for (Label l: labelsToFixup) {
            l.setTargetPC(labelIPCMap.get(l));
        }

        _instrs = instrs.toArray(new Instr[instrs.size()]);
        return _instrs;
    }

    public Instr[] getInstrArray() { return _instrs; }

    public void build(List<Instr> instrs) {
        // Map of label & basic blocks which are waiting for a bb with that label
        Map<Label, List<BasicBlock>> forwardRefs = new HashMap<Label, List<BasicBlock>>();

        // Map of return address variable and all possible targets (required to connect up ensure blocks with their targets)
        Map<Variable, Set<Label>> retAddrMap = new HashMap<Variable, Set<Label>>();

        // List of bbs that have a 'return' instruction
        List<BasicBlock> retBBs = new ArrayList<BasicBlock>();

        // List of bbs that have a 'throw' instruction
        List<BasicBlock> excBBs = new ArrayList<BasicBlock>();

        // Stack of nested rescue regions
        Stack<RescuedRegion> nestedRescuedRegions = new Stack<RescuedRegion>();

        // List of all rescued regions
        List<RescuedRegion> allRescuedRegions = new ArrayList<RescuedRegion>();

        DirectedGraph<BasicBlock, CFG_Edge> g = getNewCFG();

        // Dummy entry basic block (see note at end to see why)
        _entryBB = createNewBB(g, _bbMap, nestedRescuedRegions);

        // First real bb
        BasicBlock firstBB = createNewBB(g, _bbMap, nestedRescuedRegions);

        // Build the rest!
        BasicBlock prevBB = null;
        BasicBlock currBB = firstBB;
        BasicBlock newBB = null;
        boolean bbEnded = false;
        boolean bbEndedWithControlXfer = false;
        for (Instr i : instrs) {
            Operation iop = i.operation;
            if (iop == Operation.LABEL) {
                Label l = ((LABEL_Instr) i)._lbl;
                prevBB = currBB;
                newBB = createNewBB(l, g, _bbMap, nestedRescuedRegions);
                if (!bbEndedWithControlXfer) // Jump instruction bbs dont add an edge to the succeeding bb by default
                {
                    g.addEdge(currBB, newBB);
                }
                currBB = newBB;

                // Add forward reference edges
                List<BasicBlock> frefs = forwardRefs.get(l);
                if (frefs != null) {
                    for (BasicBlock b : frefs) {
                        g.addEdge(b, newBB);
                    }
                }
                bbEnded = false;
                bbEndedWithControlXfer = false;
            } else if (bbEnded && (iop != Operation.RESCUE_BODY_END)) {
                prevBB = currBB;
                newBB = createNewBB(g, _bbMap, nestedRescuedRegions);
                if (!bbEndedWithControlXfer) // Jump instruction bbs dont add an edge to the succeeding bb by default
                {
                    g.addEdge(currBB, newBB); // currBB cannot be null!
                }
                currBB = newBB;
                bbEnded = false;
                bbEndedWithControlXfer = false;
            }

            if (i instanceof RESCUED_BODY_START_MARKER_Instr) {
// SSS: Do we need this anymore?
//                currBB.addInstr(i);
                RESCUED_BODY_START_MARKER_Instr rbsmi = (RESCUED_BODY_START_MARKER_Instr)i;
                RescuedRegion rr = new RescuedRegion(rbsmi._elseBlock, rbsmi._rescueBlockLabels);
                rr.addBB(currBB);
                allRescuedRegions.add(rr);

                if (nestedRescuedRegions.empty())
                    _outermostRRs.add(rr);
                else
                    nestedRescuedRegions.peek().addNestedRegion(rr);

                nestedRescuedRegions.push(rr);
            } else if (i instanceof RESCUED_BODY_END_MARKER_Instr) {
// SSS: Do we need this anymore?
//                currBB.addInstr(i);
                nestedRescuedRegions.pop().setEndBB(currBB);
            } else if (iop.endsBasicBlock()) {
                bbEnded = true;
                currBB.addInstr(i);
                Label tgt;
                if (i instanceof BranchInstr) {
                    tgt = ((BranchInstr) i).getJumpTarget();
                } else if (i instanceof JumpInstr) {
                    tgt = ((JumpInstr) i).getJumpTarget();
                    bbEndedWithControlXfer = true;
                } // CASE IR instructions are dummy instructions
                // -- all when/then clauses have been converted into if-then-else blocks
                else if (i instanceof CaseInstr) {
                    tgt = null;
                } // SSS FIXME: To be done
                else if (i instanceof BREAK_Instr) {
                    tgt = null;
                    bbEndedWithControlXfer = true;
                } else if (i instanceof ReturnInstr) {
                    tgt = null;
                    retBBs.add(currBB);
                    bbEndedWithControlXfer = true;
                } else if (i instanceof THROW_EXCEPTION_Instr) {
                    tgt = null;
                    excBBs.add(currBB);
                    bbEndedWithControlXfer = true;
                } else if (i instanceof JUMP_INDIRECT_Instr) {
                    tgt = null;
                    bbEndedWithControlXfer = true;
                    Set<Label> retAddrs = retAddrMap.get(((JUMP_INDIRECT_Instr) i)._target);
                    for (Label l : retAddrs) {
                        addEdge(g, currBB, l, _bbMap, forwardRefs);
                    }
                } else {
                    tgt = null;
                }

                if (tgt != null) {
                    addEdge(g, currBB, tgt, _bbMap, forwardRefs);
                }
            } else if (iop != Operation.LABEL) {
                currBB.addInstr(i);
            }

            if (i instanceof SET_RETADDR_Instr) {
                Variable v = i.getResult();
                Set<Label> addrs = retAddrMap.get(v);
                if (addrs == null) {
                    addrs = new HashSet<Label>();
                    retAddrMap.put(v, addrs);
                }
                addrs.add(((SET_RETADDR_Instr) i).getReturnAddr());
            } else if (i instanceof CallInstr) { // Build CFG for the closure if there exists one
                Operand closureArg = ((CallInstr)i).getClosureArg();
                if (closureArg instanceof MetaObject) {
                    ((IRClosure)((MetaObject)closureArg).scope).buildCFG();
                }
            }
        }

        // Process all rescued regions
        for (RescuedRegion rr: allRescuedRegions) {
            BasicBlock firstRescueBB = getTargetBB(rr.getFirstRescueBlockLabel());

            // 1. Tell the region that firstRescueBB is its protector!
            rr.setFirstRescueBB(firstRescueBB);

            // 2. Record a mapping from the region's exclusive basic blocks to the first bb that will start exception handling for all their exceptions.
            // 3. Add an exception edge from every exclusive bb of the region to firstRescueBB
            for (BasicBlock b: rr._exclusiveBBs) {
                _bbRescuerMap.put(b, firstRescueBB);
                g.addEdge(b, firstRescueBB)._type = CFG_Edge_Type.EXCEPTION_EDGE;
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
        _exitBB = createNewBB(g, _bbMap, nestedRescuedRegions);
        g.addEdge(_entryBB, _exitBB)._type = CFG_Edge_Type.DUMMY_EDGE;
        g.addEdge(_entryBB, firstBB)._type = CFG_Edge_Type.DUMMY_EDGE;
        for (BasicBlock rb : retBBs) {
            g.addEdge(rb, _exitBB)._type = CFG_Edge_Type.DUMMY_EDGE;
        }
        for (BasicBlock rb : excBBs) {
            g.addEdge(rb, _exitBB)._type = CFG_Edge_Type.EXCEPTION_EDGE;
        }
        if (!bbEndedWithControlXfer) {
            g.addEdge(currBB, _exitBB)._type = CFG_Edge_Type.DUMMY_EDGE;
        }

        _cfg = g;

        // Delete orphaned (with no incoming edges) blocks
        deleteOrphanedBlocks();
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
        _fallThruMap = new BasicBlock[1+getMaxNodeID()];
        BasicBlock prev = null;
        for (BasicBlock b: bbs) {
           if (prev != null)
              _fallThruMap[prev.getID()] = b;
           prev = b;
        }
    }

    private void mergeBBs(BasicBlock a, BasicBlock b) {
        BasicBlock aR = _bbRescuerMap.get(a);
        BasicBlock bR = _bbRescuerMap.get(b);
        // We can merge the two basic blocks only if they are protected by the same rescue block!
        if ((aR == bR) || a.isEmpty() || b.isEmpty()) {
            a.swallowBB(b);
            _cfg.removeEdge(a, b);
            for (CFG_Edge e: _cfg.outgoingEdgesOf(b)) {
                _cfg.addEdge(a, e._dst)._type = e._type;
            }

            removeBB(b);

            if ((aR == null) && (bR != null))
                _bbRescuerMap.put(a, bR);
        }
    }

        // callBB will only have a single successor & splitBB will only have a single predecessor
        // after inlining the callee.  Merge them with their successor/predecessors respectively
    private void mergeStraightlineBBs(BasicBlock callBB, BasicBlock splitBB) {
        Set<CFG_Edge> edges = outgoingEdgesOf(callBB);
        assert(edges.size() == 1);
        mergeBBs(callBB, edges.iterator().next()._dst);

        edges = incomingEdgesOf(splitBB);
        assert(edges.size() == 1);
        mergeBBs(edges.iterator().next()._src, splitBB);
    }

    private void inlineClosureAtYieldSite(InlinerInfo ii, IRClosure cl, BasicBlock yieldBB, YieldInstr yield) {
        // 1. split yield site bb and move outbound edges from yield site bb to split bb.
        BasicBlock splitBB = yieldBB.splitAtInstruction(yield, getNewLabel(), false);
        _cfg.addVertex(splitBB);
        _bbMap.put(splitBB._label, splitBB);
        List<CFG_Edge> edgesToRemove = new java.util.ArrayList<CFG_Edge>();
        for (CFG_Edge e: outgoingEdgesOf(yieldBB)) {
            _cfg.addEdge(splitBB, e._dst)._type = e._type;
            edgesToRemove.add(e);
        }
        // Ugh! I get exceptions if I try to pass the set I receive from outgoingEdgesOf!  What a waste!
        _cfg.removeAllEdges(edgesToRemove);

        // 2. Merge closure cfg into the current cfg
        // NOTE: No need to clone basic blocks in the closure because they are part of the caller's cfg
        // and is being merged in at the yield site -- there is no need for the closure after the merge.
        CFG ccfg = cl.getCFG();
        BasicBlock cEntry = ccfg.getEntryBB(); 
        BasicBlock cExit  = ccfg.getExitBB(); 
        for (BasicBlock b: ccfg.getNodes()) {
            if (b != cEntry && b != cExit) {
              _cfg.addVertex(b);
              _bbMap.put(b._label, b);
              b.updateCFG(this);
              b.processClosureArgAndReturnInstrs(ii, yield);
            }
        }
        for (CFG_Edge e: ccfg.outgoingEdgesOf(cEntry)) {
            if (e._dst != cExit)
                _cfg.addEdge(yieldBB, e._dst)._type = e._type;
        }
        for (CFG_Edge e: ccfg.incomingEdgesOf(cExit)) {
            if (e._src != cEntry) {
                if (e._type == CFG_Edge_Type.EXCEPTION_EDGE) {
                    // e._src has an explicit throw that returns from the closure
                    // after inlining, if the yield instruction has a rescuer, then the
                    // throw has to be captured by the rescuer as well.
                    BasicBlock rescuerOfSplitBB = _bbRescuerMap.get(splitBB);
                    _cfg.addEdge(e._src, rescuerOfSplitBB != null ? rescuerOfSplitBB : _exitBB)._type = CFG_Edge_Type.EXCEPTION_EDGE;
                }
                else {
                    _cfg.addEdge(e._src, splitBB)._type = e._type;
                }
            }
        }

        // 5. No need to clone rescued regions -- just assimilate them
        for (RescuedRegion r: ccfg._outermostRRs) {
            _outermostRRs.add(r);
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as yieldB
        BasicBlock yieldBBrescuer = _bbRescuerMap.get(yieldBB);
        if (yieldBBrescuer != null)
            _bbRescuerMap.put(splitBB, yieldBBrescuer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by callBBrescuer.
        Map<BasicBlock, BasicBlock> cRescuerMap = ccfg._bbRescuerMap;
        for (BasicBlock cb: ccfg.getNodes()) {
            if (cb != cEntry && cb != cExit) {
                BasicBlock cbProtector = cRescuerMap.get(cb);
                if (cbProtector != null)
                    _bbRescuerMap.put(cb, cbProtector);
                else if (yieldBBrescuer != null)
                    _bbRescuerMap.put(cb, yieldBBrescuer);
            }
        }

        // 7. callBB will only have a single successor & splitBB will only have a single predecessor
        //    after inlining the callee.  Merge them with their successor/predecessors respectively
        //    Merge only after fixing up the rescuer map above
        mergeStraightlineBBs(yieldBB, splitBB);
    }

    public void inlineMethod(IRMethod m, BasicBlock callBB, CallInstr call) {
        InlinerInfo ii =  new InlinerInfo(call, this);

        // 1. split callsite bb and move outbound edges from callsite bb to split bb.
        BasicBlock splitBB = callBB.splitAtInstruction(call, getNewLabel(), false);
        _bbMap.put(splitBB._label, splitBB);
        _cfg.addVertex(splitBB);
        List<CFG_Edge> edgesToRemove = new java.util.ArrayList<CFG_Edge>();
        for (CFG_Edge e: outgoingEdgesOf(callBB)) {
            _cfg.addEdge(splitBB, e._dst)._type = e._type;
            edgesToRemove.add(e);
        }
        // Ugh! I get exceptions if I try to pass the set I receive from outgoingEdgesOf!  What a waste! 
        // That is why I build the new list edgesToRemove
        _cfg.removeAllEdges(edgesToRemove);

        // 2. clone callee
        CFG mcfg = m.getCFG();
        BasicBlock mEntry = mcfg.getEntryBB(); 
        BasicBlock mExit  = mcfg.getExitBB(); 
        DirectedGraph<BasicBlock, CFG_Edge> g = getNewCFG();
        for (BasicBlock b: mcfg.getNodes()) {
            if (b != mEntry && b != mExit) {
              BasicBlock bCloned = b.cloneForInlining(ii);
              _cfg.addVertex(bCloned);
              _bbMap.put(bCloned._label, bCloned);
            }
        }

        // 3. set up new edges
        for (BasicBlock x: mcfg.getNodes()) {
            if (x != mEntry && x != mExit) {
                BasicBlock rx = ii.getRenamedBB(x);
                for (CFG_Edge e: mcfg.outgoingEdgesOf(x)) {
                    BasicBlock b = e._dst;
                    if (b != mExit)
                        _cfg.addEdge(rx, ii.getRenamedBB(b))._type = e._type;
                }
            }
        }

        // 4. Hook up entry/exit edges
        for (CFG_Edge e: mcfg.outgoingEdgesOf(mEntry)) {
            if (e._dst != mExit)
                _cfg.addEdge(callBB, ii.getRenamedBB(e._dst))._type = e._type;
        }

        for (CFG_Edge e: mcfg.incomingEdgesOf(mExit)) {
            if (e._src != mEntry) {
                if (e._type == CFG_Edge_Type.EXCEPTION_EDGE) {
                    // e._src has an explicit throw that returns from the callee
                    // after inlining, if the caller instruction has a rescuer, then the
                    // throw has to be captured by the rescuer as well.
                    BasicBlock rescuerOfSplitBB = _bbRescuerMap.get(splitBB);
                    _cfg.addEdge(ii.getRenamedBB(e._src), rescuerOfSplitBB != null ? rescuerOfSplitBB : _exitBB)._type = CFG_Edge_Type.EXCEPTION_EDGE;
                }
                else {
                    _cfg.addEdge(ii.getRenamedBB(e._src), splitBB)._type = e._type;
                }
            }
        }

        // 5. Clone rescued regions
        for (RescuedRegion r: mcfg._outermostRRs) {
            _outermostRRs.add(r.cloneForInlining(ii));
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as callBB
        BasicBlock callBBrescuer = _bbRescuerMap.get(callBB);
        if (callBBrescuer != null)
            _bbRescuerMap.put(splitBB, callBBrescuer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by callBBrescuer.
        Map<BasicBlock, BasicBlock> mRescuerMap = mcfg._bbRescuerMap;
        for (BasicBlock x: mcfg.getNodes()) {
            if (x != mEntry && x != mExit) {
                BasicBlock xRenamed   = ii.getRenamedBB(x);
                BasicBlock xProtector = mRescuerMap.get(x);
                if (xProtector != null)
                    _bbRescuerMap.put(xRenamed, ii.getRenamedBB(xProtector));
                else if (callBBrescuer != null)
                    _bbRescuerMap.put(xRenamed, callBBrescuer);
            }
        }

        // 7. callBB will only have a single successor & splitBB will only have a single predecessor
        //    after inlining the callee.  Merge them with their successor/predecessors respectively
        //    Merge only after fixing up the rescuer map above
        mergeStraightlineBBs(callBB, splitBB);

        // 8. Inline any closure argument passed into the call.
        Operand closureArg = call.getClosureArg();
        List    yieldSites = ii.getYieldSites();
        if (closureArg != null && !yieldSites.isEmpty()) {
            // Detect unlikely but contrived scenarios where there are far too many yield sites that could lead to code blowup
            // if we inline the closure at all those yield sites!
            if (yieldSites.size() > 1)
                throw new RuntimeException("Encountered " + yieldSites.size() + " yield sites.  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");

            if (!(closureArg instanceof MetaObject))
                throw new RuntimeException("Encountered a dynamic closure arg.  Cannot inline it here!  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");

            Tuple t = (Tuple)yieldSites.get(0);
            inlineClosureAtYieldSite(ii, (IRClosure)((MetaObject)closureArg).scope, (BasicBlock)t.a, (YieldInstr)t.b);
        }

        // Update the bb array
        setupFallThruMap();
    }

    private void buildPostOrderTraversal() {
        _postOrderList = new LinkedList<BasicBlock>();
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
            for (CFG_Edge e : _cfg.outgoingEdgesOf(b)) {
                BasicBlock dst = e._dst;
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
                _postOrderList.add(b);
            }
        }
    }

    public ListIterator<BasicBlock> getPostOrderTraverser() {
        if (_postOrderList == null) {
            buildPostOrderTraversal();
        }

        return _postOrderList.listIterator();
    }

    public ListIterator<BasicBlock> getReversePostOrderTraverser() {
        if (_postOrderList == null) {
            buildPostOrderTraversal();
        }

        return _postOrderList.listIterator(numNodes());
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
                if (b == root) {
                    continue;
                }

                // Non-root -- process it
                Integer bPoNumber = bbToPoNumbers[b.getID()];
                Integer oldBIdom = idoms[bPoNumber];
                Integer newBIdom = null;

                // newBIdom is initialized to be some (first-encountered, for ex.) processed predecessor of 'b'.
                for (CFG_Edge e : _cfg.incomingEdgesOf(b)) {
                    BasicBlock src = e._src;
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
                for (CFG_Edge e : _cfg.incomingEdgesOf(b)) {
                    // Process b's predecessors except the initialized bidom value
                    BasicBlock src = e._src;
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
        StringBuffer buf = new StringBuffer();
        for (BasicBlock b: getNodes()) {
            buf.append(b.toStringInstrs());
        }

        buf.append("\n\n------ Exception handling map ------\n");
        for (BasicBlock bb: _bbRescuerMap.keySet()) {
            buf.append("BB " + bb.getID() + " --> BB " + _bbRescuerMap.get(bb).getID() + "\n");
        }

        List<IRClosure> closures = _scope.getClosures();
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
        _dfProbs.put(name, p);
    }

    public DataFlowProblem getDataFlowSolution(String name) {
        return _dfProbs.get(name);
    }

    private void pushBBOnStack(Stack<BasicBlock> stack, BitSet bbSet, BasicBlock bb) {
        if (!bbSet.get(bb.getID())) {
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
            for (BasicBlock b: getNodes()) {
                // Skip entry bb!
                if (b == _entryBB)
                    continue;

                // Every other bb should have at least one incoming edge
                if (incomingEdgesOf(b).size() == 0) {
                    bbToRemove = b;
                    break;
                }
            }
            if (bbToRemove == null)
                break;
            else
                removeBB(bbToRemove);
        }
    }

    public void splitCalls() {
        for (BasicBlock b: getNodes()) {
            List<Instr> bInstrs = b.getInstrs();
            for (ListIterator<Instr> it = ((ArrayList<Instr>)b.getInstrs()).listIterator(); it.hasNext(); ) {
                Instr i = it.next();
                // Only user calls, not Ruby & JRuby internal calls
                if (i.operation == Operation.CALL) {
                    CallInstr call = (CallInstr)i;
                    Operand   r    = call.getReceiver();
                    Operand   m    = call.getMethodAddr();
                    Variable  mh   = _scope.getNewTemporaryVariable();
                    MethodLookupInstr mli = new MethodLookupInstr(mh, m, r);
                    // insert method lookup at the right place
                    it.previous();
                    it.add(mli);
                    it.next();
                    // update call address
                    call.setMethodAddr(mh);
                }
            }
        }

        List<IRClosure> closures = _scope.getClosures();
        if (!closures.isEmpty()) {
            for (IRClosure c : closures) {
                c.getCFG().splitCalls();
            }
        }
    }

    public List<BasicBlock> linearize() {
        if (_linearizedBBList != null) // Done!
            return _linearizedBBList;

        _linearizedBBList = new ArrayList<BasicBlock>();
       
        // Linearize the basic blocks of the cfg!
        // This is a simple linearization -- nothing fancy
        BasicBlock root = getEntryBB();
        BitSet bbSet = new BitSet(1+getMaxNodeID());
        bbSet.set(root.getID());
        Stack<BasicBlock> stack = new Stack<BasicBlock>();

        // Push all exception edge targets (first bbs of rescue blocks) first so that rescue handlers are laid out last
        // at the end of the method, outside the common execution path
        for (CFG_Edge e: _cfg.edgeSet()) {
            if (e._type == CFG_Edge_Type.EXCEPTION_EDGE)
                pushBBOnStack(stack, bbSet, e._dst);
        }

        // Root next!
        stack.push(root);

        while (!stack.empty()) {
            BasicBlock b = stack.pop();
//            System.out.println("processing bb: " + b.getID());
            _linearizedBBList.add(b);

            if (b == _exitBB) {
                assert stack.empty();
            }
            else {
                assert !stack.empty();

               // Find the basic block that is the target of the 'taken' branch
               Instr lastInstr = b.getLastInstr();
               if (lastInstr == null) {
                   // Only possible for the root block with 2 edges + blocks with just 1 target with no instructions
                   BasicBlock b1 = null, b2 = null; 
                   for (CFG_Edge e: _cfg.outgoingEdgesOf(b)) {
                       if (b1 == null)
                           b1 = e._dst;
                       else if (b2 == null)
                           b2 = e._dst;
                       else
                           throw new RuntimeException("Encountered bb: " + b.getID() + " with no instrs. and more than 2 targets!!");
                   }

                   assert (b1 != null);

                   // Process lower number target first!
                   if (b2 == null) {
                       pushBBOnStack(stack, bbSet, b1);
                   }
                   else if (b1.getID() < b2.getID()) {
                       pushBBOnStack(stack, bbSet, b2);
                       pushBBOnStack(stack, bbSet, b1);
                   }
                   else {
                       pushBBOnStack(stack, bbSet, b1);
                       pushBBOnStack(stack, bbSet, b2);
                   }
               }
               else {
//                  System.out.println("last instr is: " + lastInstr);
                  BasicBlock blockToIgnore = null;
                  if (lastInstr instanceof JumpInstr) {
                      blockToIgnore = _bbMap.get(((JumpInstr)lastInstr).target);

                      // Check if all of blockToIgnore's predecessors get to it with a jump!
                      // This can happen because of exceptions and rescue handlers
                      // If so, dont ignore it.  Process it right away (because everyone will end up ignoring this block!)
                      boolean allJumps = true;
                      for (CFG_Edge e: _cfg.incomingEdgesOf(blockToIgnore)) {
                          if (! (e._src.getLastInstr() instanceof JumpInstr))
                              allJumps = false;
                      }

                      if (allJumps)
                          blockToIgnore = null;
                  }
                  else if (lastInstr instanceof BranchInstr) {
                      // Push the taken block onto the stack first so that it gets processed last!
                      BasicBlock takenBlock = _bbMap.get(((BranchInstr)lastInstr).getJumpTarget());
                      pushBBOnStack(stack, bbSet, takenBlock);
                      blockToIgnore = takenBlock;
                  }
         
                  // Push everything else
                  for (CFG_Edge e: _cfg.outgoingEdgesOf(b)) {
                      BasicBlock x = e._dst;
                      if (x != blockToIgnore)
                          pushBBOnStack(stack, bbSet, x);
                  }
               }
            }
        }

        // Verify that all bbs have been laid out!
        for (BasicBlock b: getNodes()) {
            if (!bbSet.get(b.getID()))
                throw new RuntimeException("Bad CFG linearization: BB " + b.getID() + " has been missed!");
        }

        // Fixup (add/remove) jumps where appropriate
        int n = _linearizedBBList.size();
        for (int i = 0; i < n; i++) {
            BasicBlock curr = _linearizedBBList.get(i); 
            Instr   li   = curr.getLastInstr();
            if ((i+1) < n) {
                BasicBlock next = _linearizedBBList.get(i+1);

                // If curr ends in a jump to next, remove the jump!
                if (li instanceof JumpInstr) {
                    if (next == _bbMap.get(((JumpInstr)li).target)) {
                        System.out.println("BB " + curr.getID() + " falls through in layout to BB " + next.getID() + ".  Removing jump from former bb!"); 
                        curr.removeInstr(li);
                    }
                }
                // If curr has a single successor and next is not it, and curr does't end in a control transfer instruction, add a jump!
                else {
                    Set<CFG_Edge> succs = _cfg.outgoingEdgesOf(curr);
                    if (succs.size() == 1) {
                        BasicBlock tgt = succs.iterator().next()._dst;
                        if ((tgt != next) && ((li == null) || !li.operation.xfersControl())) {
                            System.out.println("BB " + curr.getID() + " doesn't fall through to " + next.getID() + ".  Adding a jump to " + tgt._label);
                            curr.addInstr(new JumpInstr(tgt._label));
                        }
                    }
                }

                if (curr == _exitBB) {
                    // Add a dummy ret
                    System.out.println("Exit bb is not the last bb in the layout!  Adding a dummy return!");
                    curr.addInstr(new ReturnInstr(Nil.NIL));
                }
            }
            else if (curr != _exitBB) {
                Set<CFG_Edge> succs = _cfg.outgoingEdgesOf(curr);
                assert succs.size() == 1;
                BasicBlock tgt = succs.iterator().next()._dst;
                if ((li == null) || !li.operation.xfersControl()) {
                    System.out.println("BB " + curr.getID() + " is the last bb in the layout! Adding a jump to " + tgt._label);
                    curr.addInstr(new JumpInstr(tgt._label));
                }
            }
        }

        return _linearizedBBList;
    }
}

package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.BRANCH_Instr;
import org.jruby.compiler.ir.instructions.BREAK_Instr;
import org.jruby.compiler.ir.instructions.CASE_Instr;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.JUMP_Instr;
import org.jruby.compiler.ir.instructions.LABEL_Instr;
import org.jruby.compiler.ir.instructions.RETURN_Instr;
import org.jruby.compiler.ir.operands.Label;

import org.jgrapht.*;
import org.jgrapht.graph.*;

public class CFG
{
    enum CFG_Edge_Type { UNKNOWN, DUMMY_EDGE, FORWARD_EDGE, BACK_EDGE, EXIT_EDGE, EXCEPTION_EDGE }

    public static class CFG_Edge
    {
        final public BasicBlock _src;
        final public BasicBlock _dst;
        CFG_Edge_Type _type;

        public CFG_Edge(BasicBlock s, BasicBlock d)
        {
            _src = s;
            _dst = d;
            _type = CFG_Edge_Type.UNKNOWN;   // Unknown type to start with
        }

        public String toString()
        {
            return "<" + _src.getID() + " --> " + _dst.getID() + ">";
        }
    }

    IR_Scope   _scope;     // Scope (method/closure) to which this cfg belongs
    BasicBlock _entryBB;   // Entry BB -- dummy
    BasicBlock _exitBB;    // Exit BB -- dummy
    DirectedGraph<BasicBlock, CFG_Edge> _cfg;  // The actual graph
    int        _nextBBId;  // Next available basic block id

    public CFG(IR_Scope s)
    {
        _nextBBId = 0; // Init before building basic blocks below!
        _scope = s;
    }

    public DirectedGraph getGraph()
    {
        return _cfg;
    }

    public BasicBlock getRoot()
    {
        return _entryBB;
    }

    public int getNextBBID()
    {
       _nextBBId++;
       return _nextBBId;
    }

    public int getMaxNodeID()
    {
       return _nextBBId;
    }

    public Set<CFG_Edge> incomingEdgesOf(BasicBlock bb)
    {
        return _cfg.incomingEdgesOf(bb);
    }

    public Set<CFG_Edge> outgoingEdgesOf(BasicBlock bb)
    {
        return _cfg.outgoingEdgesOf(bb);
    }

    public Set<BasicBlock> getNodes()
    {
        return _cfg.vertexSet();
    }

    private Label getNewLabel()
    {
        return _scope.getNewLabel();
    }

    private BasicBlock createNewBB(Label l, DirectedGraph<BasicBlock, CFG_Edge> g, Map<Label, BasicBlock> bbMap)
    {
        BasicBlock b = new BasicBlock(this, l);
        bbMap.put(b._label, b);
        g.addVertex(b);
        return b;
    }

    private BasicBlock createNewBB(DirectedGraph<BasicBlock, CFG_Edge> g, Map<Label, BasicBlock> bbMap)
    {
        return createNewBB(getNewLabel(), g, bbMap);
    }

    public void build(List<IR_Instr> instrs)
    {
        // Map of label & basic blocks which are waiting for a bb with that label
        Map<Label, List<BasicBlock>> forwardRefs = new HashMap<Label, List<BasicBlock>>();

        // Map of label & basic blocks with that label
        Map<Label, BasicBlock> bbMap = new HashMap<Label, BasicBlock>();

        DirectedGraph<BasicBlock, CFG_Edge> g = new DefaultDirectedGraph<BasicBlock, CFG_Edge>(
                                                    new EdgeFactory<BasicBlock, CFG_Edge>() {
                                                        public CFG_Edge createEdge(BasicBlock s, BasicBlock d) { return new CFG_Edge(s, d); }
                                                    });

        // Dummy entry basic block (see note at end to see why)
        _entryBB = createNewBB(g, bbMap);

        // First real bb
        BasicBlock firstBB = new BasicBlock(this, getNewLabel());
        g.addVertex(firstBB);

        // Build the rest!
        List<BasicBlock> retBBs = new ArrayList<BasicBlock>();  // This will be the list of bbs that have a 'return' instruction
        BasicBlock currBB  = firstBB;
        BasicBlock newBB   = null;
        boolean    bbEnded = false;
        boolean    bbEndedWithControlXfer = false;
        for (IR_Instr i: instrs) {
            Operation iop = i._op;
            if (iop.startsBasicBlock()) {
                Label l = ((LABEL_Instr)i)._lbl;
                newBB = createNewBB(l, g, bbMap);
                if (!bbEndedWithControlXfer)  // Jump instruction bbs dont add an edge to the succeeding bb by default
                   g.addEdge(currBB, newBB);
                currBB = newBB;

                // Add forward reference edges
                List<BasicBlock> readers = forwardRefs.get(l);
                if (readers != null) {
                    for (BasicBlock b: readers)
                        g.addEdge(b, newBB);
                }
                bbEnded = false;
                bbEndedWithControlXfer = false;
            }
            else if (bbEnded) {
                newBB = createNewBB(g, bbMap);
                g.addEdge(currBB, newBB); // currBB cannot be null!
                currBB = newBB;
                currBB.addInstr(i);
                bbEnded = false;
                bbEndedWithControlXfer = false;
            }
            else if (iop.endsBasicBlock()) {
                bbEnded = true;
                currBB.addInstr(i);
                Label tgt;
                if (i instanceof BRANCH_Instr) {
                    tgt = ((BRANCH_Instr)i).getJumpTarget();
                }
                else if (i instanceof JUMP_Instr) {
                    tgt = ((JUMP_Instr)i).getJumpTarget();
                    bbEndedWithControlXfer = true;
                }
                // CASE IR instructions are dummy instructions 
                // -- all when/then clauses have been converted into if-then-else blocks
                else if (i instanceof CASE_Instr) {
                    tgt = null;
                }
                // SSS FIXME: To be done
                else if (i instanceof BREAK_Instr) {
                    tgt = null;
                    bbEndedWithControlXfer = true;
                }
                else if (i instanceof RETURN_Instr) {
                    tgt = null;
                    retBBs.add(currBB);
                    bbEndedWithControlXfer = true;
                }
                else {
                    tgt = null;
                }

                if (tgt != null) {
                    BasicBlock tgtBB = bbMap.get(tgt);
                    if (tgtBB != null) {
                        g.addEdge(currBB, tgtBB);
                    }
                    else {
                        // Add a forward reference from tgt -> currBB
                        List<BasicBlock> frefs = forwardRefs.get(tgt);
                        if (frefs == null) {
                            frefs = new ArrayList<BasicBlock>();
                            forwardRefs.put(tgt, frefs);
                        }
                        frefs.add(currBB);
                    }
                }
            }
            else {
               currBB.addInstr(i);
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
        // * last bb     -> dummy exit (only if the last bb didn't end with a control transfer!
        _exitBB = createNewBB(g, bbMap);
        g.addEdge(_entryBB, _exitBB)._type = CFG_Edge_Type.DUMMY_EDGE;
        g.addEdge(_entryBB, firstBB)._type = CFG_Edge_Type.DUMMY_EDGE;
        for (BasicBlock rb: retBBs)
            g.addEdge(rb, _exitBB)._type = CFG_Edge_Type.DUMMY_EDGE;
        if (!bbEndedWithControlXfer)
            g.addEdge(currBB, _exitBB)._type = CFG_Edge_Type.DUMMY_EDGE;

        _cfg = g;
    }

    public String toStringInstrs()
    {
        StringBuffer buf = new StringBuffer();
        for (BasicBlock b: getNodes())
            buf.append(b.toStringInstrs());

        return buf.toString();
    }
}

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
import org.jruby.compiler.ir.operands.Label;

import org.jgrapht.*;
import org.jgrapht.graph.*;

public class CFG
{
    public static class CFG_Edge
    {
        final public BasicBlock _src;
        final public BasicBlock _dst;

        public CFG_Edge(BasicBlock s, BasicBlock d)
        {
            _src = s;
            _dst = d;
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
        _entryBB = new BasicBlock(this, getNewLabel());
        _exitBB  = new BasicBlock(this, getNewLabel());
    }

    public DirectedGraph getGraph()
    {
        return _cfg;
    }

    public int getNextBBId()
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

    public void build(List<IR_Instr> instrs)
    {
        // Map of label & basic blocks which are waiting for a bb with that label
        Map<Label, List<BasicBlock>> forwardRefs = new HashMap<Label, List<BasicBlock>>();

        // Map of label & basic blocks with that label
        Map<Label, BasicBlock> bbMap = new HashMap<Label, BasicBlock>();
        bbMap.put(_entryBB._label, _entryBB);
        bbMap.put(_exitBB._label, _exitBB);

        DirectedGraph<BasicBlock, CFG_Edge> g = new DefaultDirectedGraph<BasicBlock, CFG_Edge>(
                                                    new EdgeFactory<BasicBlock, CFG_Edge>() {
                                                        public CFG_Edge createEdge(BasicBlock s, BasicBlock d) { return new CFG_Edge(s, d); }
                                                    });
        g.addVertex(_entryBB);
        g.addVertex(_exitBB);

        BasicBlock currBB = _entryBB;
        BasicBlock newBB  = null;
        boolean    bbEnded = false;
        boolean    bbEndedWithJump = false;
        for (IR_Instr i: instrs) {
            Operation iop = i._op;
            if (iop.startsBasicBlock()) {
                Label l = ((LABEL_Instr)i)._lbl;
                newBB = new BasicBlock(this, l);
                bbMap.put(l, newBB);
                g.addVertex(newBB);
                if (!bbEndedWithJump)  // Jump instruction bbs dont add an edge to the succeeding bb by default
                   g.addEdge(currBB, newBB);
                currBB = newBB;
               
                // Add forward ref. edges
                List<BasicBlock> readers = forwardRefs.get(l);
                if (readers != null) {
                    for (BasicBlock b: readers)
                        g.addEdge(b, newBB);
                }
                bbEnded = false;
                bbEndedWithJump = false;
            }
            else if (bbEnded) {
                newBB = new BasicBlock(this, getNewLabel());
                g.addVertex(newBB);
                g.addEdge(currBB, newBB); // currBB cannot be null!
                currBB = newBB;
                bbEnded = false;
                bbEndedWithJump = false;
            }
            else if (iop.endsBasicBlock()) {
                currBB.addInstr(i);
                Label tgt;
                if (i instanceof BRANCH_Instr) {
                    tgt = ((BRANCH_Instr)i).getJumpTarget();
                }
                else if (i instanceof JUMP_Instr) {
                    tgt = ((JUMP_Instr)i).getJumpTarget();
                    bbEndedWithJump = true;
                }
                // CASE IR instructions are dummy instructions 
                // -- all when/then clauses have been converted into if-then-else blocks
                else if (i instanceof CASE_Instr) {
                    tgt = null;
                }
                // SSS FIXME: To be done
                else if (i instanceof BREAK_Instr) {
                    tgt = null;
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

                bbEnded = true;
            }
            else {
               currBB.addInstr(i);
            }
        }

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

package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.operands.Variable;

import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;

public class LiveVariableNode extends FlowGraphNode
{
/* ---------- Public fields, methods --------- */
    public LiveVariableNode(DataFlowProblem prob, BasicBlock n) { super(prob, n); }

    public void init()
    {
        _setSize = _prob.getDFVarsCount();
        _in = new BitSet(_setSize);
    }

    public void buildDataFlowVars(IR_Instr i) 
    {
        LiveVariablesProblem lvp = (LiveVariablesProblem)_prob;
        Variable v = i.getResult();
        if ((v != null) && (lvp.getDFVar(v) == null)) {
            lvp.addDFVar(v);
//            System.out.println("Adding df var for " + v + ":" + lvp.getDFVar(v)._id);
        }
    }

    public void initSolnForNode()
    {
        _tmp = new BitSet(_setSize);
    }

    public void compute_MEET(CFG_Edge edge, FlowGraphNode pred)
    {
//        System.out.println("computing meet for BB " + _bb.getID() + " with BB " + ((LiveVariableNode)pred)._bb.getID());
        // All variables live at the entry of 'pred' are also live at exit of this node
        _tmp.or(((LiveVariableNode)pred)._in);
    }

    public boolean applyTransferFunction()
    {
//        System.out.println("Apply TF for BB " + _bb.getID());
        LiveVariablesProblem lvp = (LiveVariablesProblem)_prob;

        // OUT = UNION(IN(succs))
        _out = (BitSet)_tmp.clone();

        // Traverse the instructions in this basic block in reverse order!
        List<IR_Instr> instrs = _bb.getInstrs();
        ListIterator<IR_Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            IR_Instr i = it.previous();

            // v is defined => It is no longer live before 'i'
            Variable v = i.getResult();
//            System.out.println("will clear flag for: " + v);
            if (v != null) {
                DataFlowVar dv = lvp.getDFVar(v);
                _tmp.clear(dv._id);
            }

            // Now, for all variables used by 'i' mark them live before 'i'
            for (Variable x: i.getUsedVariables()) {
                DataFlowVar dv = lvp.getDFVar(x);
//                System.out.println("will set flag for: " + x);
                // This can be null for vars used, but not defined!  Yes, the source program is buggy ..
                if (dv != null)
                    _tmp.set(dv._id);
            }
        }

            // IN is the same!
        if (_tmp.equals(_in)) {
            return false;
        }
            // IN changed!
        else {
            _in = _tmp;
            return true;
        }
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("\tVars Live on Entry: ");
        int count = 0;
        for (int i = 0; i < _in.size(); i++) {
            if (_in.get(i) == true) {
                count++;
                buf.append(' ').append(i);
                if (count % 10 == 0)
                    buf.append("\t\n");
            }
        }

        if (count % 10 != 0)
            buf.append("\t\t");

        buf.append("\n\tVars Live on Exit: ");
        count = 0;
        for (int i = 0; i < _out.size(); i++) {
            if (_out.get(i) == true) {
                count++;
                buf.append(' ').append(i);
                if (count % 10 == 0)
                    buf.append("\t\n");
            }
        }

        if (count % 10 != 0)
            buf.append("\t\t");

        return buf.append('\n').toString();
    }

/* ---------- Protected / package fields, methods --------- */
    void markDeadInstructions()
    {
        LiveVariablesProblem lvp = (LiveVariablesProblem)_prob;
        _tmp = (BitSet)_out.clone();

        // Traverse the instructions in this basic block in reverse order!
        // Mark as dead all instructions whose results are not used! 
        List<IR_Instr> instrs = _bb.getInstrs();
        ListIterator<IR_Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            IR_Instr i = it.previous();
//            System.out.println("DEAD?? " + i);
            Variable v = i.getResult();
            if (v != null) {
                DataFlowVar dv = lvp.getDFVar(v);
                    // If 'v' is not live at the instruction site, and it has no side effects, mark it dead!
                if ((_tmp.get(dv._id) == false) && !i.hasSideEffects()) {
//                    System.out.println("YES!");
                    i.markDead();
                }
                else if (_tmp.get(dv._id) == false) {
//                    System.out.println("NO!  has side effects! Op is: " + i._op);
                }
                else {
//                    System.out.println("NO! LIVE result:" + v);
                    _tmp.clear(dv._id);
                }
            }
            else {
//                System.out.println("IGNORING! No result!");
            }

            // Do not mark this instruction's operands live if the instruction itself is dead!
            if (!i.isDead()) {
               for (Variable x: i.getUsedVariables()) {
                   DataFlowVar dv = lvp.getDFVar(x);
                   if (dv != null)
                       _tmp.set(dv._id);
               }
            }
        }
    }

/* ---------- Private fields, methods --------- */
    private BitSet _in;         // Variables live at entry of this node
    private BitSet _out;        // Variables live at exit of node
    private BitSet _tmp;        // Temporary set of live variables
    private int    _setSize;    // Size of the "_in" and "_out" bit sets 
}

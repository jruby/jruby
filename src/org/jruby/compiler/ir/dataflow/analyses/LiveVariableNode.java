package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.Collection;
import java.util.Set;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

public class LiveVariableNode extends FlowGraphNode {
    public LiveVariableNode(DataFlowProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        setSize = problem.getDFVarsCount();
        out = new BitSet(setSize);
    }

    private void addDFVar(Variable v) {
        LiveVariablesProblem lvp = (LiveVariablesProblem)problem;
        if ((v != null) && (lvp.getDFVar(v) == null)) {
            lvp.addDFVar(v);
//            System.out.println("Adding df var for " + v + ":" + lvp.getDFVar(v)._id);
        }
    }

    public void buildDataFlowVars(Instr i) {
//        System.out.println("BV: Processing: " + i);
        addDFVar(i.getResult());
        for (Variable x: i.getUsedVariables()) {
            addDFVar(x);
        }
    }

    public void initSolnForNode() {
        LiveVariablesProblem p = (LiveVariablesProblem) problem;
        
        in = new BitSet(setSize);
        
        if (basicBlock == p.getScope().cfg().getExitBB()) {
            Collection<Variable> lv = p.getVarsLiveOnExit();
            if ((lv != null) && !lv.isEmpty()) {
                for (Variable v: lv) {
                    in.set(p.getDFVar(v).getId());
                }
            }
        }
    }

    public void compute_MEET(BasicBlock source, FlowGraphNode pred) {
//        System.out.println("computing meet for BB " + _bb.getID() + " with BB " + ((LiveVariableNode)pred)._bb.getID());
        // All variables live at the entry of 'pred' are also live at exit of this node
        in.or(((LiveVariableNode) pred).out);
    }

    private LiveVariablesProblem processClosure(IRClosure cl, Collection<Variable> liveOnEntry) {
        LiveVariablesProblem lvp = new LiveVariablesProblem();
        lvp.initVarsLiveOnExit(liveOnEntry);
        lvp.setup(cl);
        lvp.compute_MOP_Solution();
        cl.setDataFlowSolution(lvp.getName(), lvp);

        return lvp;
    }

    public boolean applyTransferFunction() {
        LiveVariablesProblem lvp = (LiveVariablesProblem) problem;

        tmp = (BitSet) in.clone();
//        System.out.println("Apply TF for BB " + _bb.getID());
//        System.out.println("After MEET, df state is:\n" + toString());

        // Traverse the instructions in this basic block in reverse order!
        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            Instr i = it.previous();
//            System.out.println("TF: Processing: " + i);

            // v is defined => It is no longer live before 'i'
            Variable v = i.getResult();
            if (v != null) {
                DataFlowVar dv = lvp.getDFVar(v);
                tmp.clear(dv.getId());
//                System.out.println("cleared live flag for: " + v);
            }

            // Check if 'i' is a call and uses a closure!
            // If so, we need to process the closure for live variable info.
            if (i instanceof CallInstr) {
                CallInstr c = (CallInstr) i;
                // SSS FIXME: This relies on the local opt. pass having run already
                // so that the built closure from the previous instr. is propagated to the call site here.
                // Formalize this dependency somewhere?
                Operand o = c.getClosureArg();
//                   System.out.println("Processing closure: " + o + "-------");
                if ((o != null) && (o instanceof MetaObject)) {
                    IRClosure cl = (IRClosure)((MetaObject)o).scope;
                    if (c.isDataflowBarrier()) {
                        processClosure(cl, lvp.getAllVars());

//                        System.out.println(".. call is a data flow barrier ..");
                        // Mark all non-self local variables live if 'c' is a dataflow barrier!
                        for (Variable x: lvp.getNonSelfLocalVars()) tmp.set(lvp.getDFVar(x).getId());
                    } else {
                        // Propagate current LVA state through the closure
                        // SSS FIXME: Think through this .. Is there any way out of having
                        // to recompute the entire lva for the closure each time through?

                        // Collect variables live at this point.
                        Set<Variable> liveVars = new HashSet<Variable>();
                        for (int j = 0; j < tmp.size(); j++) {
                            if (tmp.get(j) == true) {
//                                System.out.println(lvp.getVariable(j) + " is live on exit of closure!");
                                liveVars.add(lvp.getVariable(j));
                            }
                        }
//                        System.out.println(" .. collected live on exit ..");

                        // Collect variables live out of the exception target node.  Since this call can directly jump to
                        // the rescue block (or scope exit) without executing the rest of the instructions in this bb, we
                        // have a control-flow edge from this call to that block.  Since we dont want to add a
                        // control-flow edge from pretty much very call to the rescuer/exit BB, we are handling it
                        // implicitly here.
                        if (c.canRaiseException()) {
                            BitSet etOut = ((LiveVariableNode)getExceptionTargetNode()).out;
                            for (int k = 0; k < etOut.size(); k++) {
                                if (etOut.get(k) == true) liveVars.add(lvp.getVariable(k));
                            }
                        }
                        // Run LVA on the closure
                        LiveVariablesProblem xlvp = processClosure(cl, liveVars);

//                        System.out.println("------- done with closure" + o);

                        // Collect variables live on entry of the closure and merge that info into the current problem.
                        for (Variable y: xlvp.getVarsLiveOnEntry()) {
                            DataFlowVar dv = lvp.getDFVar(y);
                            // This can be null for vars used, but not defined!  Yes, the source program is buggy ..
                            if (dv != null) {
//                                System.out.println(y + " is live on entry of the closure!");
                                tmp.set(dv.getId());
                            }
                        } 
                    }
                } else if (c.isDataflowBarrier()) {
//                    System.out.println(".. call is a data flow barrier ..");
                     // Mark all non-self local variables live if 'c' is a dataflow barrier!
                    for (Variable x: lvp.getNonSelfLocalVars()) {
                        tmp.set(lvp.getDFVar(x).getId());
                    }
                } else if (c.canRaiseException()) {
//                    System.out.println(".. can raise exception ..");
                    // Collect variables live out of the exception target node.  Since this call can directly jump to
                    // the rescue block (or scope exit) without executing the rest of the instructions in this bb, we
                    // have a control-flow edge from this call to that block.  Since we dont want to add a
                    // control-flow edge from pretty much very call to the rescuer/exit BB, we are handling it
                    // implicitly here.
                    BitSet etOut = ((LiveVariableNode)getExceptionTargetNode()).out;
                    for (int k = 0; k < etOut.size(); k++) {
                        if (etOut.get(k) == true) { 
                            tmp.set(k); 
//                            System.out.println("marking var live: " + lvp.getVariable(k)); 
                         }
                    }
                }
            }

            // Now, for all variables used by 'i', mark them live before 'i'
            for (Variable x: i.getUsedVariables()) {
                DataFlowVar dv = lvp.getDFVar(x);
                // This can be null for vars used, but not defined!  Yes, the source program is buggy ..
                if (dv != null) {
                    tmp.set(dv.getId());
//                    System.out.println("set live flag for: " + x);
                }
            }
        }

        if (tmp.equals(out)) { // OUT is the same!
            return false;
        } else { // OUT changed!
            out = tmp;
            return true;
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\tVars Live on Entry: ");
        int count = 0;
        for (int i = 0; i < in.size(); i++) {
            if (in.get(i) == true) {
                count++;
                buf.append(' ').append(i);
                if (count % 10 == 0) buf.append("\t\n");
            }
        }

        if (count % 10 != 0) buf.append("\t\t");

        buf.append("\n\tVars Live on Exit: ");
        count = 0;
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i) == true) {
                count++;
                buf.append(' ').append(i);
                if (count % 10 == 0) buf.append("\t\n");
            }
        }

        if (count % 10 != 0) buf.append("\t\t");

        return buf.append('\n').toString();
    }

/* ---------- Protected / package fields, methods --------- */
    void markDeadInstructions() {
//        System.out.println("dead processing for " + _bb.getID());
        LiveVariablesProblem lvp = (LiveVariablesProblem) problem;

        if (in == null) {
           // 'in' cannot be null for reachable bbs
           // This bb is unreachable! (or we have a mighty bug!)
           // Mark everything dead in here!
           for (Instr i: basicBlock.getInstrs()) {
               i.markDead();
           }

           return;
        }

        tmp = (BitSet) in.clone();

        // Traverse the instructions in this basic block in reverse order!
        // Mark as dead all instructions whose results are not used! 
        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            Instr i = it.previous();
//            System.out.println("DEAD?? " + i);
            Variable v = i.getResult();
            if (v != null) {
                DataFlowVar dv = lvp.getDFVar(v);
                    // If 'v' is not live at the instruction site, and it has no side effects, mark it dead!
                if ((tmp.get(dv.getId()) == false) && !i.hasSideEffects() && !i.getOperation().isDebugOp()) {
//                    System.out.println("YES!");
                    i.markDead();
                    it.remove();
                } else if (tmp.get(dv.getId()) == false) {
//                    System.out.println("NO!  has side effects! Op is: " + i.operation);
                } else {
//                    System.out.println("NO! LIVE result:" + v);
                    tmp.clear(dv.getId());
                }
            } else if (!i.hasSideEffects() && !i.getOperation().isDebugOp() && !i.transfersControl()) {
                 i.markDead();
                 it.remove();
            } else {
//                System.out.println("IGNORING! No result!");
            }

            if (i instanceof CallInstr) {
                CallInstr c = (CallInstr) i;
                if (c.isDataflowBarrier()) {
                    // Mark all non-self local variables live if 'c' is a dataflow barrier!
                    for (Variable x: lvp.getNonSelfLocalVars()) {
                        tmp.set(lvp.getDFVar(x).getId());
                    }
                } else {
                    if (c.canRaiseException()) {
                        // Collect variables live out of the exception target node.  Since this call can directly jump to
                        // the rescue block (or scope exit) without executing the rest of the instructions in this bb, we
                        // have a control-flow edge from this call to that block.  Since we dont want to add a
                        // control-flow edge from pretty much very call to the rescuer/exit BB, we are handling it
                        // implicitly here.
                        BitSet etOut = ((LiveVariableNode)getExceptionTargetNode()).out;
                        for (int k = 0; k < etOut.size(); k++) {
                            if (etOut.get(k) == true) tmp.set(k);
                        }
                    }

                    Operand o = c.getClosureArg();
                    if ((o != null) && (o instanceof MetaObject)) {
                        IRClosure cl = (IRClosure) ((MetaObject)o).scope;
                        LiveVariablesProblem xlvp = (LiveVariablesProblem)cl.getDataFlowSolution(lvp.getName());
                        // Collect variables live on entry and merge that info into the current problem.
                        for (Variable y: xlvp.getVarsLiveOnEntry()) {
                            DataFlowVar dv = lvp.getDFVar(y);
                            // This can be null for vars used, but not defined!  Yes, the source program is buggy ..
                            if (dv != null) tmp.set(dv.getId());
                        } 
                    }
                }
            }

            // Do not mark this instruction's operands live if the instruction itself is dead!
            if (!i.isDead()) {
               for (Variable x: i.getUsedVariables()) {
                   DataFlowVar dv = lvp.getDFVar(x);
                   if (dv != null) tmp.set(dv.getId());
               }
            }
        }
    }

    BitSet getLiveInBitSet() {
        return in;
    }

    BitSet getLiveOutBitSet() {
        return this.out;
    }

    private BitSet in;         // Variables live at entry of this node
    private BitSet out;        // Variables live at exit of node
    private BitSet tmp;        // Temporary set of live variables
    private int setSize;    // Size of the "this.in" and "this.out" bit sets 
}

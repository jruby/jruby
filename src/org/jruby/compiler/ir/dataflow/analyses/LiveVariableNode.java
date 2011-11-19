package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.CallBase;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.WrappedIRClosure;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.Collection;
import java.util.Set;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import org.jruby.compiler.ir.instructions.ResultInstr;

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
        LiveVariablesProblem lvp = (LiveVariablesProblem) problem;
        if (!lvp.dfVarExists(v)) lvp.addDFVar(v);
        // System.out.println("Adding df var for " + v + ":" + lvp.getDFVar(v).id);
    }

    public void buildDataFlowVars(Instr i) {
        // System.out.println("BV: Processing: " + i);        
        if (i instanceof ResultInstr) addDFVar(((ResultInstr) i).getResult());

        for (Variable x: i.getUsedVariables()) {
            addDFVar(x);
        }
    }

    public void initSolnForNode() {
        LiveVariablesProblem p = (LiveVariablesProblem) problem;
        
        in = new BitSet(setSize);
        
        if (basicBlock == p.getScope().cfg().getExitBB()) {
            Collection<LocalVariable> lv = p.getVarsLiveOnScopeExit();
            if (lv != null && !lv.isEmpty()) {
                for (Variable v: lv) {
                    in.set(p.getDFVar(v).getId());
                }
            }
        }
    }

    public void compute_MEET(BasicBlock source, FlowGraphNode pred) {
        // System.out.println("computing meet for BB " + basicBlock.getID() + " with BB " + ((LiveVariableNode)pred).basicBlock.getID());
        // All variables live at the entry of 'pred' are also live at exit of this node
        in.or(((LiveVariableNode) pred).out);
    }

    private void processClosureLVP(LiveVariablesProblem cl_lvp, Collection<LocalVariable> varsLiveOnScopeExit) {
    }

    public boolean applyTransferFunction() {
        LiveVariablesProblem lvp = (LiveVariablesProblem) problem;

        tmp = (BitSet) in.clone();
         // System.out.println("Apply TF for BB " + basicBlock.getID());
         // System.out.println("After MEET, df state is:\n" + toString());

        // Traverse the instructions in this basic block in reverse order!
        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            Instr i = it.previous();
            // System.out.println("TF: Processing: " + i);

            // v is defined => It is no longer live before 'i'
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr) i).getResult();
                tmp.clear(lvp.getDFVar(v).getId());
            }

            // Check if 'i' is a call and uses a closure!
            // If so, we need to process the closure for live variable info.
            if (i instanceof CallBase) {
                CallBase c = (CallBase) i;
                Operand  o = c.getClosureArg();
                // System.out.println("Processing closure: " + o + "-------");
                if ((o != null) && (o instanceof WrappedIRClosure)) {
                    IRClosure cl = ((WrappedIRClosure)o).getClosure();
                    LiveVariablesProblem cl_lvp = (LiveVariablesProblem)cl.getDataFlowSolution(DataFlowConstants.LVP_NAME);
                    if (cl_lvp == null) {
                        cl_lvp = new LiveVariablesProblem();
                        cl_lvp.setup(cl, lvp.getNonSelfLocalVars());
                        cl.setDataFlowSolution(cl_lvp.getName(), cl_lvp);
                    }

                    // Collect live local variables at this point.
                    Set<LocalVariable> liveVars = new HashSet<LocalVariable>();
                    for (int j = 0; j < tmp.size(); j++) {
                        if (tmp.get(j) == true) {
                            Variable v = lvp.getVariable(j);
                            if (v instanceof LocalVariable) liveVars.add((LocalVariable)v);
                        }
                    }

                    // Collect variables live on entry of the closure -- they could all be live on exit as well (conservative, but safe).
                    //
                    //   def foo
                    //     i = 0; 
                    //     loop { i += 1; break if i > n }
                    //   end 
                    //
                    // Here, i is not live outside the closure, but it is clearly live on exit of the closure because
                    // it is reused on the next iteration.  In the absence of information about the call accepting the closure,
                    // we have to assume that all vars live on exit from the closure will be live on entry into the closure as well
                    // because of looping.
                    List<Variable> liveOnEntryBefore = cl_lvp.getVarsLiveOnScopeEntry();
                    for (Variable y: liveOnEntryBefore) {
                        if (y instanceof LocalVariable) liveVars.add((LocalVariable)y);
                    } 

                    // Collect variables live out of the exception target node.  Since this call can directly jump to
                    // the rescue block (or scope exit) without executing the rest of the instructions in this bb, we
                    // have a control-flow edge from this call to that block.  Since we dont want to add a
                    // control-flow edge from pretty much every call to the rescuer/exit BB, we are handling it
                    // implicitly here.
                    if (c.canRaiseException()) {
                        BitSet etOut = ((LiveVariableNode)getExceptionTargetNode()).out;
                        for (int k = 0; k < etOut.size(); k++) {
                            if (etOut.get(k) == true) {
                                Variable v = lvp.getVariable(k);
                                if (v instanceof LocalVariable) liveVars.add((LocalVariable)v);
                            }
                        }
                    }

                    // Run LVA on the closure to propagate current LVA state through the closure
                    // SSS FIXME: Think through this .. Is there any way out of having
                    // to recompute the entire lva for the closure each time through?
                    cl_lvp.setVarsLiveOnScopeExit(liveVars);
                    cl_lvp.compute_MOP_Solution();

                    // Check if liveOnScopeEntry added new vars -- if so, rerun.
                    // NOTE: This is conservative since we are not checking if some vars got deleted.
                    // But, this conservativeness guarantees forward progress of the analysis.
                    boolean changed;
                    List<Variable> liveOnEntryAfter;
                    do { 
                        changed = false;
                        liveOnEntryAfter = cl_lvp.getVarsLiveOnScopeEntry();
                        for (Variable y: liveOnEntryAfter) {
                            if (y instanceof LocalVariable) {
                                LocalVariable ly = (LocalVariable)y;
                                if (!liveVars.contains(ly)) {
                                    changed = true;
                                    liveVars.add(ly);
                                }
                            }
                        }

                        if (changed) {
                            cl_lvp.setVarsLiveOnScopeExit(liveVars);
                            cl_lvp.compute_MOP_Solution();
                        }
                    } while (changed);

                    // Merge live on closure entry info into the current problem.
                    for (Variable y: liveOnEntryAfter) {
                        DataFlowVar dv = lvp.getDFVar(y);
                        // This can be null for vars used, but not defined!  Yes, the source program is buggy ..
                        if (dv != null) tmp.set(dv.getId());
                    }
                }

                // If this is a dataflow barrier -- mark all local vars but %self and %block live
                if (c.isDataflowBarrier()) {
                    // System.out.println(".. call is a data flow barrier ..");
                    // Mark all non-self, non-block local variables live if 'c' is a dataflow barrier!
                    for (Variable x: lvp.getNonSelfLocalVars()) {
                        if (!x.isImplicitBlockArg()) tmp.set(lvp.getDFVar(x).getId());
                    }
                } else if (c.canRaiseException()) {
                    // System.out.println(".. can raise exception ..");
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
            }

            // Now, for all variables used by 'i', mark them live before 'i'
            for (Variable x: i.getUsedVariables()) {
                DataFlowVar dv = lvp.getDFVar(x);
                // This can be null for vars used, but not defined!  Yes, the source program is buggy ..
                if (dv != null) {
                    tmp.set(dv.getId());
                    // System.out.println("set live flag for: " + x);
                }
            }
        }

        // System.out.println("After TF, df state is:\n" + toString());

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
        // System.out.println("-- Identifying dead instructions for " + basicBlock.getID() + " -- ");
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
            // System.out.println("DEAD?? " + i);
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr) i).getResult();
                DataFlowVar dv = lvp.getDFVar(v);
                    // If 'v' is not live at the instruction site, and it has no side effects, mark it dead!
                // System.out.println("df var for " + v + " is " + dv.getId());
                if ((tmp.get(dv.getId()) == false) && i.canBeDeleted()) {
                    // System.out.println("YES!");
                    i.markDead();
                    it.remove();
                    if (v.isImplicitBlockArg()) lvp.getScope().markUnusedImplicitBlockArg();
                } else if (tmp.get(dv.getId()) == false) {
                    // System.out.println("NO! has side effects! Op is: " + i.getOperation());
                } else {
                    // System.out.println("NO! LIVE result:" + v);
                    tmp.clear(dv.getId());
                }
            } else if (i.canBeDeleted()) {
                 i.markDead();
                 it.remove();
            } else {
                // System.out.println("IGNORING! No result!");
            }

            if (i instanceof CallBase) {
                CallBase c = (CallBase) i;
                Operand  o = c.getClosureArg();
                if ((o != null) && (o instanceof WrappedIRClosure)) {
                    IRClosure cl = ((WrappedIRClosure)o).getClosure();
                    LiveVariablesProblem cl_lvp = (LiveVariablesProblem)cl.getDataFlowSolution(lvp.getName());
                    // Collect variables live on entry and merge that info into the current problem.
                    for (Variable y: cl_lvp.getVarsLiveOnScopeEntry()) {
                        DataFlowVar dv = lvp.getDFVar(y);
                        // This can be null for vars used, but not defined!  Yes, the source program is buggy ..
                        if (dv != null) tmp.set(dv.getId());
                    } 
                } else if (c.isDataflowBarrier()) {
                    // Mark all non-self, non-block local variables live if 'c' is a dataflow barrier!
                    for (Variable x: lvp.getNonSelfLocalVars()) {
                        if (!x.isImplicitBlockArg()) tmp.set(lvp.getDFVar(x).getId());
                    }
                } else if (c.canRaiseException()) {
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

package org.jruby.ir.dataflow.analyses;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowConstants;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.dataflow.DataFlowVar;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.util.Edge;

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
    }

    public void buildDataFlowVars(Instr i) {
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
        // System.out.println("Init state for BB " + basicBlock.getID() + " is " + toString());
    }

    public void compute_MEET(Edge e, BasicBlock source, FlowGraphNode pred) {
        // System.out.println("computing meet for BB " + basicBlock.getID() + " with BB " + ((LiveVariableNode)pred).basicBlock.getID());
        // All variables live at the entry of 'pred' are also live at exit of this node
        in.or(((LiveVariableNode) pred).out);
    }

    private void markAllVariablesLive(LiveVariablesProblem lvp, BitSet living, Collection<? extends Variable> variableList) {
        for (Variable variable: variableList) {
            markVariableLive(lvp, living, variable);
        }
    }

    private void markVariableLive(LiveVariablesProblem lvp, BitSet living, Variable x) {
        DataFlowVar dv = lvp.getDFVar(x);

        // A buggy Ruby program that uses but does not assign a value to a var
        // will be null.
        if (dv != null) living.set(dv.getId());
    }

    public boolean applyTransferFunction() {
        // System.out.println("After MEET, df state for " + basicBlock.getID() + " is:\n" + toString());
        LiveVariablesProblem lvp = (LiveVariablesProblem) problem;
        boolean scopeBindingHasEscaped = lvp.getScope().bindingHasEscaped();

        BitSet living = (BitSet) in.clone();

        // Traverse the instructions in this basic block in reverse order!
        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            Instr i = it.previous();
            // System.out.println("TF: Processing: " + i);

            // v is defined => It is no longer live before 'i'
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr) i).getResult();
                living.clear(lvp.getDFVar(v).getId());
            }

            // Check if 'i' is a call and uses a closure!
            // If so, we need to process the closure for live variable info.
            if (i instanceof CallBase) {
                CallBase c = (CallBase) i;
                Operand  o = c.getClosureArg(null);
                // System.out.println("Processing closure: " + o + "-------");
                if (o != null && o instanceof WrappedIRClosure) {
                    IRClosure cl = ((WrappedIRClosure)o).getClosure();
                    LiveVariablesProblem cl_lvp = (LiveVariablesProblem)cl.getDataFlowSolution(DataFlowConstants.LVP_NAME);
                    if (cl_lvp == null) {
                        cl_lvp = new LiveVariablesProblem(cl, lvp.getNonSelfLocalVars());
                        cl.setDataFlowSolution(cl_lvp.getName(), cl_lvp);
                    }

                    // Collect live local variables at this point.
                    Set<LocalVariable> liveVars = new HashSet<LocalVariable>();
                    for (int j = 0; j < living.size(); j++) {
                        if (living.get(j)) {
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
                            if (etOut.get(k)) {
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
                    markAllVariablesLive(lvp, living, liveOnEntryAfter);
                }

                // If this is a dataflow barrier -- mark all local vars but %self and %block live
                if (scopeBindingHasEscaped || c.targetRequiresCallersBinding()) {
                    // System.out.println(".. call is a data flow barrier ..");
                    // Mark all non-self, non-block local variables live if 'c' is a dataflow barrier!
                    for (Variable x: lvp.getNonSelfLocalVars()) {
                        if (!x.isImplicitBlockArg()) living.set(lvp.getDFVar(x).getId());
                    }
                } else if (c.canRaiseException()) {
                    makeOutExceptionVariablesLiving(living);
                }
            } else if (i.canRaiseException()) {
                makeOutExceptionVariablesLiving(living);
            }

            // Now, for all variables used by 'i', mark them live before 'i'
            markAllVariablesLive(lvp, living, i.getUsedVariables());
        }

        // System.out.println("After TF, df state is:\n" + toString());

        if (living.equals(out)) { // OUT is the same!
            return false;
        } else { // OUT changed!
            out = living;
            return true;
        }
    }

    /**
     * Collect variables live out of the exception target node.  Since this instr. can directly jump to
     * the rescue block (or scope exit) without executing the rest of the instructions in this bb, we
     * have a control-flow edge from this instr. to that block.  Since we dont want to add a
     * control-flow edge from pretty much every instr. to the rescuer/exit BB, we are handling it
     * implicitly here.
     */
    private void makeOutExceptionVariablesLiving(BitSet living) {
        BitSet etOut = ((LiveVariableNode)getExceptionTargetNode()).out;

        for (int i = 0; i < etOut.size(); i++) {
            if (etOut.get(i)) living.set(i);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\tVars Live on Entry: ");
        int count = 0;
        for (int i = 0; i < in.size(); i++) {
            if (in.get(i)) {
                count++;
                buf.append(' ').append(i);
                if (count % 10 == 0) buf.append("\t\n");
            }
        }

        if (count % 10 != 0) buf.append("\t\t");

        buf.append("\n\tVars Live on Exit: ");
        count = 0;
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i)) {
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
        IRScope scope = lvp.getScope();
        boolean scopeBindingHasEscaped = scope.bindingHasEscaped();

        if (in == null) {
           // 'in' cannot be null for reachable bbs
           // This bb is unreachable! (or we have a mighty bug!)
           // Mark everything dead in here!
           for (Instr i: basicBlock.getInstrs()) {
               i.markDead();
           }

           return;
        }

        BitSet living = (BitSet) in.clone();

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
                if (living.get(dv.getId())) {
                    living.clear(dv.getId());
                    // System.out.println("NO! LIVE result:" + v);
                } else if (i.canBeDeleted(scope)) {
                    // System.out.println("YES!");
                    i.markDead();
                    it.remove();
                    if (v.isImplicitBlockArg()) lvp.getScope().markUnusedImplicitBlockArg();
                } else {
                    // System.out.println("NO! has side effects! Op is: " + i.getOperation());
                }
            } else if (i.canBeDeleted(scope)) {
                 i.markDead();
                 it.remove();
            } else {
                // System.out.println("IGNORING! No result!");
            }

            if (i instanceof CallBase) {
                CallBase c = (CallBase) i;
                Operand  o = c.getClosureArg(null);
                if (o != null && o instanceof WrappedIRClosure) {
                    IRClosure cl = ((WrappedIRClosure)o).getClosure();
                    LiveVariablesProblem cl_lvp = (LiveVariablesProblem)cl.getDataFlowSolution(lvp.getName());
                    // Collect variables live on entry and merge that info into the current problem.
                    markAllVariablesLive(lvp, living, cl_lvp.getVarsLiveOnScopeEntry());
                } else if (scopeBindingHasEscaped || c.targetRequiresCallersBinding()) {
                    // Mark all non-self, non-block local variables live if 'c' is a dataflow barrier!
                    for (Variable x: lvp.getNonSelfLocalVars()) {
                        if (!x.isImplicitBlockArg()) living.set(lvp.getDFVar(x).getId());
                    }
                } else if (c.canRaiseException()) {
                    makeOutExceptionVariablesLiving(living);
                }
            } else if (i.canRaiseException()) {
                makeOutExceptionVariablesLiving(living);
            }

            // Do not mark this instruction's operands live if the instruction itself is dead!
            if (!i.isDead()) markAllVariablesLive(lvp, living, i.getUsedVariables());
        }
    }

    BitSet getLiveInBitSet() {
        return in;
    }

    BitSet getLiveOutBitSet() {
        return out;
    }

    private BitSet in;         // Variables live at entry of this node
    private BitSet out;        // Variables live at exit of node
    private int setSize;    // Size of the "this.in" and "this.out" bit sets
}

package org.jruby.ir.dataflow.analyses;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowConstants;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.util.Edge;

public class LiveVariableNode extends FlowGraphNode<LiveVariablesProblem, LiveVariableNode> {
    public LiveVariableNode(LiveVariablesProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        setSize = problem.getDFVarsCount();
        out = new BitSet(setSize);
    }

    private void addDFVar(Variable v) {
        if (!problem.dfVarExists(v)) problem.addDFVar(v);
    }

    @Override
    public void buildDataFlowVars(Instr i) {
        // FIXME: We could potentially have used a Set<Variable> to represent live variables
        // rather than use a BitSet. BitSet operations (meet/set/get) could be more
        // efficient. However, given that we have to getDFVar(..) for every variable
        // in every instruction when analysing it before accessing the BitSet, unsure
        // if the bitset really buys us anything!
        //
        // StoreLocalVarPlacement and LoadLocalVarPlacement analyses use
        // Set<LocalVariable> rather than BitSets.
        if (i instanceof ResultInstr) addDFVar(((ResultInstr) i).getResult());

        for (Variable x: i.getUsedVariables()) {
            addDFVar(x);
        }
    }

    @Override
    public void applyPreMeetHandler() {
        in = new BitSet(setSize);

        if (basicBlock.isExitBB()) {
            Collection<LocalVariable> lv = problem.getVarsLiveOnScopeExit();
            if (lv != null && !lv.isEmpty()) {
                for (Variable v: lv) {
                    in.set(problem.getDFVar(v));
                }
            }
        }
        // System.out.println("Init state for BB " + basicBlock.getID() + " is " + toString());
    }

    @Override
    public void compute_MEET(Edge e, LiveVariableNode pred) {
        // System.out.println("computing meet for BB " + basicBlock.getID() + " with BB " + pred.basicBlock.getID());
        // All variables live at the entry of 'pred' are also live at exit of this node
        in.or(pred.out);
    }

    private void markAllVariablesLive(LiveVariablesProblem lvp, BitSet living, Collection<? extends Variable> variableList) {
        for (Variable variable: variableList) {
            markVariableLive(lvp, living, variable);
        }
    }

    private void markVariableLive(LiveVariablesProblem lvp, BitSet living, Variable x) {
        Integer dv = lvp.getDFVar(x);

        // A buggy Ruby program that uses but does not assign a value to a var
        // will be null.
        if (dv != null) living.set(dv);
    }

    @Override
    public void initSolution() {
        living = (BitSet) in.clone();
    }

    @Override
    public void applyTransferFunction(Instr i) {
        boolean scopeBindingHasEscaped = problem.getScope().bindingHasEscaped();

        // v is defined => It is no longer live before 'i'
        if (i instanceof ResultInstr) {
            Variable v = ((ResultInstr) i).getResult();
            living.clear(problem.getDFVar(v));
        }

        // Check if 'i' is a call and uses a closure!
        // If so, we need to process the closure for live variable info.
        if (i instanceof ClosureAcceptingInstr) {
            Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
            // System.out.println("Processing closure: " + o + "-------");
            if (o != null && o instanceof WrappedIRClosure) {
                IRClosure cl = ((WrappedIRClosure)o).getClosure();
                LiveVariablesProblem cl_lvp = (LiveVariablesProblem) cl.getDataFlowSolution(DataFlowConstants.LVP_NAME);
                if (cl_lvp == null) {
                    cl_lvp = new LiveVariablesProblem(cl, problem.getNonSelfLocalVars());
                    cl.setDataFlowSolution(cl_lvp.getName(), cl_lvp);
                }

                // Add all living local variables.
                Set<LocalVariable> liveVars = problem.addLiveLocalVars(new HashSet<LocalVariable>(), living);

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
                if (i.canRaiseException()) problem.addLiveLocalVars(liveVars, getExceptionTargetNode().out);

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
                markAllVariablesLive(problem, living, liveOnEntryAfter);
            }

            // If this is a dataflow barrier -- mark all local vars but %self and %block live
            if (scopeBindingHasEscaped) {
                // System.out.println(".. call is a data flow barrier ..");
                // Mark all non-self, non-block local variables live if 'c' is a dataflow barrier!
                for (Variable x: problem.getNonSelfLocalVars()) {
                    if (!x.isImplicitBlockArg()) living.set(problem.getDFVar(x));
                }
            }
        }

        // NOTE: This is unnecessary in the case of calls in scopes where
        // the binding has escaped since the if (scopeBindingHasEscapd) check above
        // would have handled it. But, extra readability of the DRY-ed version is
        // worth the the little bit of extra work.
        if (i.canRaiseException()) {
            makeOutExceptionVariablesLiving(living);
        }

        // Now, for all variables used by 'i', mark them live before 'i'
        markAllVariablesLive(problem, living, i.getUsedVariables());
    }

    @Override
    public boolean solutionChanged() {
        return !living.equals(out);
    }

    @Override
    public void finalizeSolution() {
        out = living;
    }

    /**
     * Collect variables live out of the exception target node.  Since this instr. can directly jump to
     * the rescue block (or scope exit) without executing the rest of the instructions in this bb, we
     * have a control-flow edge from this instr. to that block.  Since we dont want to add a
     * control-flow edge from pretty much every instr. to the rescuer/exit BB, we are handling it
     * implicitly here.
     */
    private void makeOutExceptionVariablesLiving(BitSet living) {
        BitSet etOut = getExceptionTargetNode().out;

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
        IRScope scope = problem.getScope();
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

        initSolution();

        // Traverse the instructions in this basic block in reverse order!
        // Mark as dead all instructions whose results are not used!
        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            Instr i = it.previous();
            // System.out.println("DEAD?? " + i);
            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr) i).getResult();
                Integer dv = problem.getDFVar(v);

                // If 'v' is not live at the instruction site, and it has no side effects, mark it dead!
                // System.out.println("df var for " + v + " is " + dv.getId());
                if (living.get(dv)) {
                    living.clear(dv);
                    // System.out.println("NO! LIVE result:" + v);
                } else if (i.canBeDeleted(scope)) {
                    // System.out.println("YES!");
                    i.markDead();
                    it.remove();
                    if (v.isImplicitBlockArg()) problem.getScope().getFlags().add(IRFlags.HAS_UNUSED_IMPLICIT_BLOCK_ARG);
                } else {
                    // System.out.println("NO! has side effects! Op is: " + i.getOperation());
                }
            } else if (i.canBeDeleted(scope)) {
                 i.markDead();
                 it.remove();
            } else {
                // System.out.println("IGNORING! No result!");
            }

            if (i instanceof ClosureAcceptingInstr) {
                Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
                if (o != null && o instanceof WrappedIRClosure) {
                    IRClosure cl = ((WrappedIRClosure)o).getClosure();
                    LiveVariablesProblem cl_lvp = (LiveVariablesProblem)cl.getDataFlowSolution(problem.getName());
                    // Collect variables live on entry and merge that info into the current problem.
                    markAllVariablesLive(problem, living, cl_lvp.getVarsLiveOnScopeEntry());
                } else if (scopeBindingHasEscaped) {
                    // Mark all non-self, non-block local variables live if 'c' is a dataflow barrier!
                    for (Variable x: problem.getNonSelfLocalVars()) {
                        if (!x.isImplicitBlockArg()) living.set(problem.getDFVar(x));
                    }
                }
            }

            // NOTE: This is unnecessary in the case of calls in scopes where
            // the binding has escaped since the if (scopeBindingHasEscapd) check above
            // would have handled it. But, extra readability of the DRY-ed version is
            // worth the the little bit of extra work.
            if (i.canRaiseException()) {
                makeOutExceptionVariablesLiving(living);
            }

            // Do not mark this instruction's operands live if the instruction itself is dead!
            if (!i.isDead()) markAllVariablesLive(problem, living, i.getUsedVariables());
        }
    }

    BitSet getLiveInBitSet() {
        return in;
    }

    BitSet getLiveOutBitSet() {
        return out;
    }

    private BitSet in;      // Variables live at entry of this node
    private BitSet out;     // Variables live at exit of node
    private BitSet living;  // Temporary state while applying transfer function
    private int setSize;    // Size of the "this.in" and "this.out" bit sets
}

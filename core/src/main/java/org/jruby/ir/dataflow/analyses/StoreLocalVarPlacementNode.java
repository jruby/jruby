package org.jruby.ir.dataflow.analyses;

import org.jruby.dirgra.Edge;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.*;
import org.jruby.ir.operands.*;
import org.jruby.ir.representations.BasicBlock;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class StoreLocalVarPlacementNode extends FlowGraphNode<StoreLocalVarPlacementProblem, StoreLocalVarPlacementNode> {
    public StoreLocalVarPlacementNode(StoreLocalVarPlacementProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        outDirtyVars = new HashSet<LocalVariable>();

        // For rescue entries, we allocate <in> once and never clear it on each pass.
        if (getBB().isRescueEntry()) inDirtyVars = new HashSet<LocalVariable>();
    }

    @Override
    public void buildDataFlowVars(Instr i) {
        // Nothing to do -- because we are going to simply use non-closure, non-self, non-block LocalVariables as our data flow variables
        // rather than build a new data flow type for it
    }

    @Override
    public void applyPreMeetHandler() {
        // For rescue entries, <in> is handled specially
        if (!getBB().isRescueEntry()) inDirtyVars = new HashSet<LocalVariable>();
    }

    @Override
    public void compute_MEET(Edge e, StoreLocalVarPlacementNode pred) {
        // Ignore rescue entries -- dirty vars are handled specially for these
        if (!pred.basicBlock.isRescueEntry()) inDirtyVars.addAll(pred.outDirtyVars);
    }

    @Override
    public void initSolution() {
        dirtyVars = new HashSet<LocalVariable>(inDirtyVars);
    }

    @Override
    public void applyTransferFunction(Instr i) {
        IRScope scope = problem.getScope();
        boolean scopeBindingHasEscaped = scope.bindingHasEscaped();

        // Process closure accepting instrs specially -- these are the sites of binding stores!
        if (i instanceof ClosureAcceptingInstr) {
            Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
            // At this site, a binding will get allocated if it has not been already!
            if (o != null && o instanceof WrappedIRClosure) {
                // In this first pass, the current scope and the call's closure are considered
                // independent of each other which means any variable that is used by the variable
                // will get spilled into the binding.  This is clearly conservative, but simplifies
                // the analysis.
                IRClosure cl = ((WrappedIRClosure) o).getClosure();

                // If the call is a dataflow barrier, we have to spill everything here
                boolean spillAllVars = scopeBindingHasEscaped;

                // - If all variables have to be spilled, then those variables will no longer be dirty after the call site
                // - If a variable is used in the closure (FIXME: Strictly only those vars that are live at the call site --
                //   but we dont have this info!), it has to be spilt. So, these variables are no longer dirty after the call site.
                // - If a variable is (re)defined in the closure, it will always be loaded after the call. So, we have to always
                //   spill it before the call in the scenario that the closure never gets executed! So, it won't be dirty after
                //   the call site.
                Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                for (LocalVariable v : dirtyVars) {
                    if (spillAllVars || cl.usesLocalVariable(v) || cl.definesLocalVariable(v)) {
                        newDirtyVars.remove(v);
                    }
                }
                dirtyVars = newDirtyVars;
            } else if (scopeBindingHasEscaped) { // Call has no closure && it requires stores
                dirtyVars.clear();
            } else {
                // All variables not local to the current scope have to be always spilled because of
                // multi-threading scenarios where some other scope could load this variable concurrently.
                //
                // Allocate a new hash-set and modify it to get around ConcurrentModificationException on dirtyVars
                Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                for (LocalVariable v : dirtyVars) {
                    if ((v instanceof ClosureLocalVariable) && !((ClosureLocalVariable)v).isDefinedLocally()) {
                        newDirtyVars.remove(v);
                    }
                }
                dirtyVars = newDirtyVars;
            }
        }

        if (scopeBindingHasEscaped && (i.getOperation() == Operation.PUT_GLOBAL_VAR)) {
            // global-var tracing can execute closures set up in previous trace-var calls
            // in which case we would have the 'scopeBindingHasEscaped' flag set to true
            dirtyVars.clear();
        }

        // If this instruction can raise an exception and we are going to be rescued,
        // spill all dirty vars before the instruction!
        if (i.canRaiseException() && hasExceptionsRescued()) {
            dirtyVars.clear();
        }

        if (i instanceof ResultInstr) {
            Variable v = ((ResultInstr) i).getResult();

            // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
            if (v instanceof LocalVariable && !v.isSelf()) dirtyVars.add((LocalVariable) v);
        }

        if (i.getOperation().isReturn()) dirtyVars.clear();
    }

    @Override
    public boolean solutionChanged() {
        return !outDirtyVars.equals(dirtyVars);
    }

    @Override
    public void finalizeSolution() {
        outDirtyVars = dirtyVars;
    }

    @Override
    public String toString() {
        return "";
    }

    public boolean addStores(Map<Operand, Operand> varRenameMap, Set<LocalVariable> excTargetDirtyVars) {
        IRScope scope = problem.getScope();

        boolean addedStores            = false;
        boolean isClosure              = scope instanceof IRClosure;
        boolean scopeBindingHasEscaped = scope.bindingHasEscaped();

        ListIterator<Instr> instrs    = basicBlock.getInstrs().listIterator();

        initSolution();

        // If this is the exit BB, we need a binding store on exit only for vars that are both:
        //
        //   (a) dirty,
        //   (b) live on exit from the closure
        //       condition reqd. because the variable could be dirty but not used outside.
        //         Ex: s=0; a.each { |i| j = i+1; sum += j; }; puts sum
        //       i,j are dirty inside the block, but not used outside

        if (basicBlock.isExitBB()) {
            LiveVariablesProblem lvp = scope.getLiveVariablesProblem();
            java.util.Collection<LocalVariable> liveVars = lvp.getVarsLiveOnScopeExit();
            if (liveVars != null) {
                dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
            } else {
                dirtyVars.clear();
            }
        }

        while (instrs.hasNext()) {
            Instr i = instrs.next();

            // Process closure accepting instrs specially -- these are the sites of binding stores!
            if (i instanceof ClosureAcceptingInstr) {
                Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
                if (o != null && o instanceof WrappedIRClosure) {
                    IRClosure cl = ((WrappedIRClosure) o).getClosure();

                    // Add before call -- hence instrs.previous & instrs.next
                    instrs.previous();

                    // If the call is a dataflow barrier, we have to spill everything here
                    boolean spillAllVars = scopeBindingHasEscaped;

                    // Unless we have to spill everything, spill only those dirty variables that are:
                    // - used in the closure (FIXME: Strictly only those vars that are live at the call site -- but we dont have this info!)
                    Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                    for (LocalVariable v : dirtyVars) {
                        // We have to spill the var that is defined in the closure as well because the load var pass
                        // will attempt to load the var always.  So, if the call doesn't actually call the closure,
                        // we'll be in trouble in that scenario!
                        if (spillAllVars || cl.usesLocalVariable(v) || cl.definesLocalVariable(v)) {
                            addedStores = true;
                            instrs.add(new StoreLocalVarInstr(problem.getLocalVarReplacement(v, varRenameMap), scope, v));
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                    instrs.next();
                } else if (scopeBindingHasEscaped) { // Call has no closure && it requires stores
                    // Add before call -- hence instrs.previous & instrs.next
                    instrs.previous();
                    for (LocalVariable v : dirtyVars) {
                        addedStores = true;
                        instrs.add(new StoreLocalVarInstr(problem.getLocalVarReplacement(v, varRenameMap), scope, v));
                    }
                    instrs.next();
                    dirtyVars.clear();
                } else {
                    instrs.previous();

                    // All variables not local to the current scope have to be always spilled because of
                    // multi-threading scenarios where some other scope could load this variable concurrently.
                    //
                    // Allocate a new hash-set and modify it to get around ConcurrentModificationException on dirtyVars
                    Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                    for (LocalVariable v : dirtyVars) {
                        // SSS FIXME: I guess we cannot use v.getScopeDepth() > 0 because the variable could be a cloned
                        // instance from a different depth and that could mislead us. See if there is a way to fix this.
                        // If we introduced 'definingScope' in all local variables, we could simply check for scope match
                        // without the instanceof check here.
                        if (   (v instanceof ClosureLocalVariable && !((ClosureLocalVariable)v).isDefinedLocally())
                            || (!(v instanceof ClosureLocalVariable) && scope.getScopeType().isClosureType()))
                        {
                            addedStores = true;
                            instrs.add(new StoreLocalVarInstr(problem.getLocalVarReplacement(v, varRenameMap), scope, v));
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                    instrs.next();
                }
            } else if ((isClosure && (i instanceof ReturnInstr)) || (i instanceof BreakInstr)) {
                // At closure return and break instructions (both of which are exits from the closure),
                // we need a binding store on exit only for vars that are both:
                //
                //   (a) dirty,
                //   (b) live on exit from the closure
                //       condition reqd. because the variable could be dirty but not used outside.
                //         Ex: s=0; a.each { |i| j = i+1; sum += j; return if j < 5; sum += 1; }; puts sum
                //       i,j are dirty inside the block, but not used outside
                //
                // If this also happens to be exit BB, we would have intersected already earlier -- so no need to do it again!

                if (!basicBlock.isExitBB()) {
                    LiveVariablesProblem lvp = scope.getLiveVariablesProblem();
                    java.util.Collection<LocalVariable> liveVars = lvp.getVarsLiveOnScopeExit();
                    if (liveVars != null) {
                        dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
                    } else {
                        dirtyVars.clear();
                    }
                }

                // Add before call
                instrs.previous();
                boolean f = problem.addClosureExitStoreLocalVars(instrs, dirtyVars, varRenameMap);
                addedStores = addedStores || f;
                instrs.next();

                // Nothing is dirty anymore -- everything that needs spilling has been spilt
                dirtyVars.clear();
            }

            if (scopeBindingHasEscaped && (i.getOperation() == Operation.PUT_GLOBAL_VAR
                    || i.getOperation() == Operation.THREAD_POLL)) {
                // 1. Global-var tracing can execute closures set up in previous trace-var calls
                // in which case we would have the 'scopeBindingHasEscaped' flag set to true.
                // 2. Threads can update bindings, so we treat thread poll boundaries the same way.
                instrs.previous();
                for (LocalVariable v : dirtyVars) {
                    addedStores = true;
                    instrs.add(new StoreLocalVarInstr(problem.getLocalVarReplacement(v, varRenameMap), scope, v));
                }
                instrs.next();
                dirtyVars.clear();
            }

            if (i.canRaiseException()) {
                if (hasExceptionsRescued()) {
                    // If exceptions will be rescued, spill every dirty var here
                    // Add before excepting instr -- hence instrs.previous & instrs.next
                    instrs.previous();
                    for (LocalVariable v : dirtyVars) {
                        addedStores = true;
                        instrs.add(new StoreLocalVarInstr(problem.getLocalVarReplacement(v, varRenameMap), scope, v));
                    }
                    instrs.next();
                    dirtyVars.clear();
                } else if (excTargetDirtyVars != null) {
                    // If exceptions won't be rescued, pass them back to be spilled in the global ensure block
                    excTargetDirtyVars.addAll(dirtyVars);
                }
            }

            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr) i).getResult();

                // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
                if (v instanceof LocalVariable && !v.isSelf()) {
                    LocalVariable lv = (LocalVariable) v;
                    dirtyVars.add(lv);

                    // Make sure there is a replacement tmp-var allocated for lv
                    problem.getLocalVarReplacement(lv, varRenameMap);
                }
            }
        }

        // If this is the exit BB, add binding stores for all vars that are still dirty
        if (basicBlock.isExitBB()) {
            // Last instr could be a return -- so, move iterator one position back
            if (instrs.hasPrevious()) instrs.previous();
            boolean f = problem.addClosureExitStoreLocalVars(instrs, dirtyVars, varRenameMap);
            addedStores = addedStores || f;
        }

        return addedStores;
    }

    Set<LocalVariable> inDirtyVars;   // On entry to flow graph node:  Variables that need to be stored to the heap binding
    Set<LocalVariable> outDirtyVars;  // On exit from flow graph node: Variables that need to be stored to the heap binding
    Set<LocalVariable> dirtyVars;     // Temporary state while applying transfer function
}

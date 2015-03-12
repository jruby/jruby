package org.jruby.ir.dataflow.analyses;

import org.jruby.dirgra.Edge;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.*;
import org.jruby.ir.operands.*;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class LoadLocalVarPlacementNode extends FlowGraphNode<LoadLocalVarPlacementProblem, LoadLocalVarPlacementNode> {
    public LoadLocalVarPlacementNode(LoadLocalVarPlacementProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        inRequiredLoads = new HashSet<LocalVariable>();
        outRequiredLoads = new HashSet<LocalVariable>();
    }

    @Override
    public void buildDataFlowVars(Instr i) {
        // Nothing to do -- because we are going to simply use non-closure, non-self, non-block LocalVariables as our data flow variables
        // rather than build a new data flow type for it
    }

    @Override
    public void applyPreMeetHandler() {
        if (basicBlock.isExitBB()) inRequiredLoads = problem.getLoadsOnScopeExit();
    }

    @Override
    public void compute_MEET(Edge e, LoadLocalVarPlacementNode pred) {
        inRequiredLoads.addAll(pred.outRequiredLoads);
    }

    @Override
    public void initSolution() {
        reqdLoads = new HashSet<LocalVariable>(inRequiredLoads);
    }

    @Override
    public void applyTransferFunction(Instr i) {
        IRScope scope = problem.getScope();
        boolean scopeBindingHasEscaped = scope.bindingHasEscaped();

        // Right away, clear the variable defined by this instruction -- it doesn't have to be loaded!
        if (i instanceof ResultInstr) {
            reqdLoads.remove(((ResultInstr) i).getResult());
        }

        // Process closure accepting instrs specially -- these are the sites of binding loads!
        if (i instanceof ClosureAcceptingInstr) {
            Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
            if (o != null && o instanceof WrappedIRClosure) {
                IRClosure cl = ((WrappedIRClosure) o).getClosure();

                // Variables defined in the closure do not need to be loaded anymore at
                // program points before the call, because they will be loaded after the
                // call completes to fetch the latest value.
                //
                // Allocate a new hash-set and modify it to get around ConcurrentModificationException on reqdLoads
                Set<LocalVariable> newReqdLoads = new HashSet<LocalVariable>(reqdLoads);
                for (LocalVariable v: reqdLoads) {
                    if (cl.definesLocalVariable(v)) newReqdLoads.remove(v);
                }
                reqdLoads = newReqdLoads;
            }

            // In this case, we are going to blindly load everything -- so, at the call site, pending loads dont carry over!
            if (scopeBindingHasEscaped) {
                reqdLoads.clear();
            } else {
                // All variables not defined in the current scope have to be always loaded
                // because of multi-threading scenarios where some other scope
                // could update this variable concurrently.
                //
                // Allocate a new hash-set and modify it to get around ConcurrentModificationException on reqdLoads
                Set<LocalVariable> newReqdLoads = new HashSet<LocalVariable>(reqdLoads);
                for (LocalVariable v: reqdLoads) {
                    if (!scope.definesLocalVariable(v)) newReqdLoads.remove(v);
                }
                reqdLoads = newReqdLoads;
            }
        } else if (scopeBindingHasEscaped && (i.getOperation() == Operation.PUT_GLOBAL_VAR)) {
            // global-var tracing can execute closures set up in previous trace-var calls
            // in which case we would have the 'scopeBindingHasEscaped' flag set to true
            reqdLoads.clear();
        }

        if (i.getOperation() == Operation.BINDING_STORE) {
            LocalVariable lv = ((StoreLocalVarInstr)i).getLocalVar();
            if (!lv.isSelf()) reqdLoads.add(lv);
        } else {
            // The variables used as arguments will need to be loaded
            // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
            for (Variable x : i.getUsedVariables()) {
                if (x instanceof LocalVariable && !x.isSelf()) {
                    reqdLoads.add((LocalVariable)x);
                }
            }
        }
    }

    @Override
    public boolean solutionChanged() {
        // At the beginning of the scope and rescue block entries, required loads can be discarded
        // since all these loads will be executed there.
        if (basicBlock.isEntryBB() || basicBlock.isRescueEntry()) reqdLoads.clear();

        //System.out.println("\n For CFG " + getCFG() + " BB " + _bb.getID());
        //System.out.println("\t--> IN reqd loads   : " + java.util.Arrays.toString(_inReqdLoads.toArray()));
        //System.out.println("\t--> OUT reqd loads  : " + java.util.Arrays.toString(_outReqdLoads.toArray()));
        return !outRequiredLoads.equals(reqdLoads);
    }

    @Override
    public void finalizeSolution() {

        outRequiredLoads = reqdLoads;
    }

    @Override
    public String toString() {
        return "";
    }

    private TemporaryLocalVariable getLocalVarReplacement(LocalVariable v, IRScope scope, Map<Operand, Operand> varRenameMap) {
         TemporaryLocalVariable value = (TemporaryLocalVariable)varRenameMap.get(v);
         if (value == null) {
             value = scope.getNewTemporaryVariableFor(v);
             varRenameMap.put(v, value);
         }
         return value;
    }

    public void addLoads(Map<Operand, Operand> varRenameMap) {
        IRScope scope                  = problem.getScope();
        boolean isEvalScript           = scope instanceof IREvalScript;
        boolean scopeBindingHasEscaped = scope.bindingHasEscaped();

        List<Instr>         instrs    = basicBlock.getInstrs();
        ListIterator<Instr> it        = instrs.listIterator(instrs.size());

        initSolution();
        while (it.hasPrevious()) {
            Instr i = it.previous();

            // Right away, clear the variable defined by this instruction -- it doesn't have to be loaded!
            if (i instanceof ResultInstr) reqdLoads.remove(((ResultInstr) i).getResult());

            // Process closure accepting instrs specially -- these are the sites of binding loads!
            if (i instanceof ClosureAcceptingInstr) {
                Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
                if (o != null && o instanceof WrappedIRClosure) {
                    IRClosure cl = ((WrappedIRClosure) o).getClosure();

                    // Only those variables that are defined in the closure, and are in the required loads set
                    // will need to be loaded from the binding after the call!  Rest can wait ..
                    it.next();
                    for (Iterator<LocalVariable> iter = reqdLoads.iterator(); iter.hasNext();) {
                        LocalVariable v = iter.next();
                        if (cl.definesLocalVariable(v)) {
                            it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                            it.previous();
                            iter.remove();
                        }
                    }
                    it.previous();
                }

                // In this case, we are going to blindly load everything
                if (scopeBindingHasEscaped) {
                    it.next();
                    for (LocalVariable v: reqdLoads) {
                        it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                        it.previous();
                    }
                    it.previous();
                    reqdLoads.clear();
                } else {
                    // All variables not defined in the current scope have to be always loaded
                    // because of multi-threading scenarios where some other scope
                    // could update this variable concurrently.
                    it.next();
                    for (Iterator<LocalVariable> iter = reqdLoads.iterator(); iter.hasNext();) {
                        LocalVariable v = iter.next();
                        if (!scope.definesLocalVariable(v)) {
                            it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                            it.previous();
                            iter.remove();
                        }
                    }
                    it.previous();
                }
            } else if (scopeBindingHasEscaped && (i.getOperation() == Operation.PUT_GLOBAL_VAR
                    || i.getOperation() == Operation.THREAD_POLL)) {
                // 1. Global-var tracing can execute closures set up in previous trace-var calls
                // in which case we would have the 'scopeBindingHasEscaped' flag set to true.
                // 2. Threads can update bindings, so we treat thread poll boundaries the same way.
                it.next();
                for (LocalVariable v : reqdLoads) {
                    it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                    it.previous();
                }
                it.previous();
                reqdLoads.clear();
            }

            if (i.getOperation() == Operation.BINDING_STORE) {
                LocalVariable lv = ((StoreLocalVarInstr)i).getLocalVar();
                if (!lv.isSelf()) {
                    reqdLoads.add(lv);
                    // SSS FIXME: Why is this reqd again?  Document with example
                    // Make sure there is a replacement var for all local vars
                    getLocalVarReplacement(lv, scope, varRenameMap);
                }
            } else {
                // The variables used as arguments will need to be loaded
                // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
                for (Variable v : i.getUsedVariables()) {
                    if (!(v instanceof LocalVariable)) continue;

                    LocalVariable lv = (LocalVariable)v;
                    if (!lv.isSelf()) {
                        reqdLoads.add(lv);
                        // SSS FIXME: Why is this reqd again?  Document with example
                        // Make sure there is a replacement var for all local vars
                        getLocalVarReplacement(lv, scope, varRenameMap);
                    }
                }
            }
        }

        // Add loads on entry of a rescue block.
        if (basicBlock.isRescueEntry()) {
            for (LocalVariable v : reqdLoads) {
                it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
            }
        }

        // Load first use of variables in closures
        if (scope instanceof IRClosure && basicBlock.isEntryBB()) {
            // System.out.println("\n[In Entry BB] For CFG " + getCFG() + ":");
            // System.out.println("\t--> Reqd loads   : " + java.util.Arrays.toString(reqdLoads.toArray()));
            for (LocalVariable v : reqdLoads) {
                if (scope.usesLocalVariable(v) || scope.definesLocalVariable(v)) {
                    if (isEvalScript || !(v instanceof ClosureLocalVariable) || !((ClosureLocalVariable)v).isDefinedLocally()) {
                        it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                    }
                }
            }
        }
    }

    Set<LocalVariable> inRequiredLoads;  // On entry to flow graph node:  Variables that need to be loaded from the heap binding
    Set<LocalVariable> outRequiredLoads; // On exit from flow graph node: Variables that need to be loaded from the heap binding
    Set<LocalVariable> reqdLoads;        // Temporary state while applying transfer function
}

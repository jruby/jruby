package org.jruby.ir.dataflow.analyses;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.LoadLocalVarInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.util.Edge;

public class LoadLocalVarPlacementNode extends FlowGraphNode {
    public LoadLocalVarPlacementNode(DataFlowProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        inRequiredLoads = new HashSet<LocalVariable>();
        outRequiredLoads = new HashSet<LocalVariable>();
    }

    public void buildDataFlowVars(Instr i) {
        // Nothing to do -- because we are going to simply use non-closure, non-self, non-block LocalVariables as our data flow variables
        // rather than build a new data flow type for it
    }

    public void initSolnForNode() {
        if (basicBlock == problem.getScope().cfg().getExitBB()) {
            inRequiredLoads = ((LoadLocalVarPlacementProblem) problem).getLoadsOnScopeExit();
        }
    }

    public void compute_MEET(Edge e, BasicBlock source, FlowGraphNode pred) {
        LoadLocalVarPlacementNode n = (LoadLocalVarPlacementNode) pred;
        inRequiredLoads.addAll(n.outRequiredLoads);
    }

    public boolean applyTransferFunction() {
        IRScope scope = problem.getScope();
        boolean scopeBindingHasEscaped = scope.bindingHasEscaped();

        Set<LocalVariable>  reqdLoads = new HashSet<LocalVariable>(inRequiredLoads);
        List<Instr>         instrs    = basicBlock.getInstrs();
        ListIterator<Instr> it        = instrs.listIterator(instrs.size());

        while (it.hasPrevious()) {
            Instr i = it.previous();
            // System.out.println("-----\nInstr " + i);
            // System.out.println("Before: " + java.util.Arrays.toString(reqdLoads.toArray()));

            // Right away, clear the variable defined by this instruction -- it doesn't have to be loaded!
            if (i instanceof ResultInstr) reqdLoads.remove(((ResultInstr) i).getResult());

            // Process calls specially -- these are the sites of binding loads!
            if (i instanceof CallBase) {
                CallBase call = (CallBase) i;
                Operand o = call.getClosureArg(null);
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
                if (scopeBindingHasEscaped || call.targetRequiresCallersBinding()) {
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
                    if ((x instanceof LocalVariable) && !((LocalVariable)x).isSelf()) {
                        reqdLoads.add((LocalVariable)x);
                    }
                }
            }
            // System.out.println("After: " + java.util.Arrays.toString(reqdLoads.toArray()));
        }

        // At the beginning of the scope and rescue block entries, required loads can be discarded
        // since all these loads will be executed there.
        if ((basicBlock == problem.getScope().cfg().getEntryBB()) || basicBlock.isRescueEntry()) {
            reqdLoads.clear();
        }

        if (outRequiredLoads.equals(reqdLoads)) {
            //System.out.println("\n For CFG " + problem.getCFG() + " BB " + _bb.getID());
            //System.out.println("\t--> IN reqd loads   : " + java.util.Arrays.toString(_inReqdLoads.toArray()));
            //System.out.println("\t--> OUT reqd loads  : " + java.util.Arrays.toString(_outReqdLoads.toArray()));
            return false;
        } else {
            outRequiredLoads = reqdLoads;
            return true;
        }
    }

    @Override
    public String toString() {
        return "";
    }

    private TemporaryVariable getLocalVarReplacement(LocalVariable v, IRScope scope, Map<Operand, Operand> varRenameMap) {
         TemporaryVariable value = (TemporaryVariable)varRenameMap.get(v);
         if (value == null) {
             value = scope.getNewTemporaryVariable("%t_" + v.getName());
             varRenameMap.put(v, value);
         }
         return value;
    }

    public void addLoads(Map<Operand, Operand> varRenameMap) {
        LoadLocalVarPlacementProblem blp = (LoadLocalVarPlacementProblem) problem;

        IRScope scope                  = blp.getScope();
        boolean isEvalScript           = scope instanceof IREvalScript;
        boolean scopeBindingHasEscaped = scope.bindingHasEscaped();

        List<Instr>         instrs    = basicBlock.getInstrs();
        ListIterator<Instr> it        = instrs.listIterator(instrs.size());
        Set<LocalVariable>  reqdLoads = new HashSet<LocalVariable>(inRequiredLoads);

        while (it.hasPrevious()) {
            Instr i = it.previous();

            // Right away, clear the variable defined by this instruction -- it doesn't have to be loaded!
            if (i instanceof ResultInstr) reqdLoads.remove(((ResultInstr) i).getResult());

            if (i instanceof CallBase) {
                CallBase call = (CallBase) i;
                Operand o = call.getClosureArg(null);
                if (o != null && o instanceof WrappedIRClosure) {
                    IRClosure cl = ((WrappedIRClosure) o).getClosure();

                    // Only those variables that are defined in the closure, and are in the required loads set
                    // will need to be loaded from the binding after the call!  Rest can wait ..
                    //
                    // Allocate a new hash-set and modify it to get around ConcurrentModificationException on reqdLoads
                    Set<LocalVariable> newReqdLoads = new HashSet<LocalVariable>(reqdLoads);
                    it.next();
                    for (LocalVariable v : reqdLoads) {
                        if (cl.definesLocalVariable(v)) {
                            it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                            it.previous();
                            newReqdLoads.remove(v);
                        }
                    }
                    it.previous();
                    reqdLoads = newReqdLoads;
                }

                // In this case, we are going to blindly load everything
                if (scopeBindingHasEscaped || call.targetRequiresCallersBinding()) {
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
                    //
                    // Allocate a new hash-set and modify it to get around ConcurrentModificationException on reqdLoads
                    Set<LocalVariable> newReqdLoads = new HashSet<LocalVariable>(reqdLoads);
                    it.next();
                    for (LocalVariable v: reqdLoads) {
                        if (!scope.definesLocalVariable(v)) {
                            it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                            it.previous();
                            newReqdLoads.remove(v);
                        }
                    }
                    it.previous();
                    reqdLoads = newReqdLoads;
                }
            } else if (scopeBindingHasEscaped && (i.getOperation() == Operation.PUT_GLOBAL_VAR)) {
                // global-var tracing can execute closures set up in previous trace-var calls
                // in which case we would have the 'scopeBindingHasEscaped' flag set to true
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
        if ((scope instanceof IRClosure) && (basicBlock == problem.getScope().cfg().getEntryBB())) {
            // System.out.println("\n[In Entry BB] For CFG " + problem.getScope().cfg() + ":");
            // System.out.println("\t--> Reqd loads   : " + java.util.Arrays.toString(reqdLoads.toArray()));
            for (LocalVariable v : reqdLoads) {
                if (scope.usesLocalVariable(v) || scope.definesLocalVariable(v)) {
                    if (isEvalScript || !(v instanceof ClosureLocalVariable) || (scope != ((ClosureLocalVariable)v).definingScope)) {
                        it.add(new LoadLocalVarInstr(scope, getLocalVarReplacement(v, scope, varRenameMap), v));
                    }
                }
            }
        }
    }

    // On entry to flow graph node:  Variables that need to be loaded from the heap binding
    Set<LocalVariable> inRequiredLoads;
    // On exit from flow graph node: Variables that need to be loaded from the heap binding
    Set<LocalVariable> outRequiredLoads;
}

package org.jruby.ir.dataflow.analyses;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceiveJRubyExceptionInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.operands.*;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

// This problem tries to find places to insert binding stores -- for spilling local variables onto a heap store
// It does better than spilling all local variables to the heap at all call sites.  This is similar to a
// available expressions analysis in that it tries to propagate availability of stores through the flow graph.
public class StoreLocalVarPlacementProblem extends DataFlowProblem<StoreLocalVarPlacementProblem, StoreLocalVarPlacementNode> {
    public static final String NAME = "Placement of local-var stores";

    private boolean scopeHasLocalVarStores;
    private boolean scopeHasUnrescuedExceptions;

    public StoreLocalVarPlacementProblem() {
        super(DataFlowProblem.DF_Direction.FORWARD);
    }

    @Override
    public String getName() {
        return "Binding Stores Placement Analysis";
    }

    @Override
    public StoreLocalVarPlacementNode buildFlowGraphNode(BasicBlock bb) {
        return new StoreLocalVarPlacementNode(this, bb);
    }

    @Override
    public String getDataFlowVarsForOutput() {
        return "";
    }

    public boolean scopeHasLocalVarStores() {
        return scopeHasLocalVarStores;
    }

    public boolean scopeHasUnrescuedExceptions() {
        return scopeHasUnrescuedExceptions;
    }

    TemporaryLocalVariable getLocalVarReplacement(LocalVariable v, Map<Operand, Operand> varRenameMap) {
         TemporaryLocalVariable value = (TemporaryLocalVariable)varRenameMap.get(v);
         if (value == null) {
             value = getScope().getNewTemporaryVariableFor(v);
             varRenameMap.put(v, value);
         }
         return value;
    }

    boolean addClosureExitStoreLocalVars(ListIterator<Instr> instrs, Set<LocalVariable> dirtyVars, Map<Operand, Operand> varRenameMap) {
        IRScope scope        = getScope();
        boolean addedStores  = false;
        boolean isEvalScript = scope instanceof IREvalScript;
        for (LocalVariable v : dirtyVars) {
            if (isEvalScript || !(v instanceof ClosureLocalVariable) || !((ClosureLocalVariable)v).isDefinedLocally()) {
                addedStores = true;
                instrs.add(new StoreLocalVarInstr(scope, getLocalVarReplacement(v, varRenameMap), v));
            }
        }
        return addedStores;
    }

    public void addStores(Map<Operand, Operand> varRenameMap) {
        /* --------------------------------------------------------------------
         * If this is a closure, introduce a global ensure block that spills
         * into the binding the union of dirty vars from all call sites that
         * aren't protected by any other rescue or ensure block.
         *
         * This conservative scenario ensures that no matter what call site
         * we receive an exception from, when we exit the closure, all dirty
         * vars from the parent scope have been stored.
         * -------------------------------------------------------------------- */
        boolean mightRequireGlobalEnsureBlock = false;

        Set<LocalVariable> dirtyVars = null;
        IRScope cfgScope = getScope();
        CFG     cfg      = cfgScope.getCFG();

        this.scopeHasLocalVarStores      = false;
        this.scopeHasUnrescuedExceptions = false;

        if (cfgScope instanceof IRClosure) {
            mightRequireGlobalEnsureBlock = true;
            dirtyVars = new HashSet<LocalVariable>();
        }

        // Add local-var stores
        for (StoreLocalVarPlacementNode bspn: flowGraphNodes) {
            boolean bbAddedStores;
            // SSS: This is highly conservative.  If the bb has an exception raising instr.
            // and we dont have a rescuer, only then do we have unrescued exceptions.
            // Right now, we are only checking for rescuers.
            boolean bbHasUnrescuedExceptions = !bspn.hasExceptionsRescued();
            if (mightRequireGlobalEnsureBlock && bbHasUnrescuedExceptions) {
                bbAddedStores = bspn.addStores(varRenameMap, dirtyVars);
            } else {
                bbAddedStores = bspn.addStores(varRenameMap, null);
            }

            scopeHasUnrescuedExceptions = scopeHasUnrescuedExceptions || bbHasUnrescuedExceptions;
            scopeHasLocalVarStores      = scopeHasLocalVarStores || bbAddedStores;
        }

        // Allocate global-ensure block, if necessary
        if ((mightRequireGlobalEnsureBlock == true) && !dirtyVars.isEmpty()) {
            ListIterator<Instr> instrs;
            BasicBlock geb = cfg.getGlobalEnsureBB();
            if (geb == null) {
                Variable exc = cfgScope.createTemporaryVariable();
                geb = new BasicBlock(cfg, Label.getGlobalEnsureBlockLabel());
                geb.addInstr(new ReceiveJRubyExceptionInstr(exc)); // JRuby implementation exception handling
                geb.addInstr(new ThrowExceptionInstr(exc));
                cfg.addGlobalEnsureBB(geb);
            }

            instrs = geb.getInstrs().listIterator(geb.getInstrs().size());
            Instr i = instrs.previous();
            // Assumption: Last instr should always be a control-transfer instruction
            assert i.getOperation().transfersControl(): "Last instruction of GEB in scope: " + getScope() + " is " + i + ", not a control-xfer instruction";
            addClosureExitStoreLocalVars(instrs, dirtyVars, varRenameMap);
        }
    }
}

package org.jruby.ir.dataflow.analyses;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

// This problem tries to find places to insert binding stores -- for spilling local variables onto a heap store
// It does better than spilling all local variables to the heap at all call sites.  This is similar to a
// available expressions analysis in that it tries to propagate availability of stores through the flow graph.
public class StoreLocalVarPlacementProblem extends DataFlowProblem {
    public static final String NAME = "Placement of local-var stores";

    private boolean scopeHasLocalVarStores;
    private boolean scopeHasUnrescuedExceptions;

    public StoreLocalVarPlacementProblem() {
        super(DataFlowProblem.DF_Direction.FORWARD);
    }

    public String getName() {
        return "Binding Stores Placement Analysis";
    }

    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) {
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

        CFG     cfg      = getScope().cfg();
        IRScope cfgScope = cfg.getScope();

        this.scopeHasLocalVarStores      = false;
        this.scopeHasUnrescuedExceptions = false;

        if (cfgScope instanceof IRClosure) {
            mightRequireGlobalEnsureBlock = true;
            dirtyVars = new HashSet<LocalVariable>();
        }

        // Add local-var stores
        for (FlowGraphNode n : flowGraphNodes) {
            StoreLocalVarPlacementNode bspn = (StoreLocalVarPlacementNode) n;
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
        BasicBlock geb = null;
        if ((mightRequireGlobalEnsureBlock == true) && !dirtyVars.isEmpty()) {
            Variable exc = cfgScope.getNewTemporaryVariable();
            geb = new BasicBlock(cfg, new Label("_GLOBAL_ENSURE_BLOCK"));
            geb.addInstr(new ReceiveExceptionInstr(exc, false)); // No need to check type since it is not used before rethrowing
            for (LocalVariable v : dirtyVars) {
                Operand value = varRenameMap.get(v);
                if (value == null) {
                    value = cfgScope.getNewTemporaryVariable("%t_" + v.getName());
                    varRenameMap.put(v, value);
                }
                geb.addInstr(new StoreLocalVarInstr(value, (IRClosure) cfgScope, v));
            }
            geb.addInstr(new ThrowExceptionInstr(exc));
            cfg.addGlobalEnsureBB(geb);
        }
    }
}

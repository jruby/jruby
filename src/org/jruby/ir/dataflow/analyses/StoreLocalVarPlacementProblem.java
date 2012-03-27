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
//
// We have piggybacked the problem of identifying sites where binding allocation instrutions are necessary.  So,
// strictly speaking, this is a AND of two independent dataflow analyses -- we are doing these together for
// efficiency reasons, and also because the binding allocation problem is also a forwards flow problem and is a
// relatively straightforward analysis.
public class StoreLocalVarPlacementProblem extends DataFlowProblem {

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

    public void addStoreAndBindingAllocInstructions(Map<Operand, Operand> varRenameMap) {
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
        CFG cfg = getScope().cfg();
        IRScope cfgScope = cfg.getScope();
        if (cfgScope instanceof IRClosure) {
            mightRequireGlobalEnsureBlock = true;
            dirtyVars = new HashSet<LocalVariable>();
        }

        for (FlowGraphNode n : flowGraphNodes) {
            StoreLocalVarPlacementNode bspn = (StoreLocalVarPlacementNode) n;
            if (mightRequireGlobalEnsureBlock && !cfg.bbIsProtected(bspn.getBB())) {
                bspn.addStoreAndBindingAllocInstructions(varRenameMap, dirtyVars);
            } else {
                bspn.addStoreAndBindingAllocInstructions(varRenameMap, null);
            }
        }

        if ((mightRequireGlobalEnsureBlock == true) && !dirtyVars.isEmpty()) {
            BasicBlock geb = new BasicBlock(cfg, new Label("_GLOBAL_ENSURE_BLOCK"));
            Variable exc = cfgScope.getNewTemporaryVariable();
            geb.addInstr(new ReceiveExceptionInstr(exc));
            for (LocalVariable v : dirtyVars) {
                Operand value = varRenameMap.get(v);
                if (value == null) {
                    value = cfgScope.getNewTemporaryVariable("%t_" + v.getName());
                    varRenameMap.put(v, value);
                }
                geb.addInstr(new StoreLocalVarInstr(value, (IRClosure) cfgScope, v));
            }
            geb.addInstr(new ThrowExceptionInstr(exc));
            cfg.addGlobalEnsureBlock(geb);
        }
    }
}

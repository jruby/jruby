package org.jruby.ir.dataflow.analyses;

import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.representations.BasicBlock;

import java.util.Map;
import java.util.Set;

public class LoadLocalVarPlacementProblem extends DataFlowProblem<LoadLocalVarPlacementProblem, LoadLocalVarPlacementNode> {
    public LoadLocalVarPlacementProblem() {
        super(DataFlowProblem.DF_Direction.BACKWARD);
        initLoadsOnExit = new java.util.HashSet<LocalVariable>();
        bindingHasEscaped = false;
    }

    @Override
    public String getName() {
        return "Binding Loads Placement Analysis";
    }

    @Override
    public LoadLocalVarPlacementNode buildFlowGraphNode(BasicBlock bb) {
        return new LoadLocalVarPlacementNode(this, bb);
    }

    @Override
    public String getDataFlowVarsForOutput() {
        return "";
    }

    public void initLoadsOnScopeExit(Set<LocalVariable> loads) {
        initLoadsOnExit = loads;
    }

    public Set<LocalVariable> getLoadsOnScopeExit() {
        return initLoadsOnExit;
    }

    public boolean bindingHasEscaped() {
        return bindingHasEscaped;
    }

    public void setBindingHasEscaped(boolean flag) {
        bindingHasEscaped = flag;
    }

    public void addLoads(Map<Operand, Operand> varRenameMap) {
        for (LoadLocalVarPlacementNode blpn: flowGraphNodes) {
            blpn.addLoads(varRenameMap);
        }
    }

    private Set<LocalVariable> initLoadsOnExit;

    // Has this method's (or the containing method's binding in the case of a closure) binding escaped?
    private boolean bindingHasEscaped;
}

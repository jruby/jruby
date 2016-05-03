package org.jruby.ir.dataflow.analyses;

import java.util.HashSet;
import java.util.Set;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

/**
 */
public class AddMissingAssignsProblem extends DataFlowProblem<AddMissingAssignsProblem, AddMissingAssignsNode> {
    public static final String NAME = "Live Variables Analysis";

    private Set<Variable> variables;

    public AddMissingAssignsProblem(IRScope scope) {
        super(DataFlowProblem.DF_Direction.FORWARD);
        setup(scope);
        variables = new HashSet<>();
    }

    protected void addVariable(Variable variable) {
        variables.add(variable);
    }

    protected boolean hasVariable(Variable variable) {
        return variables.contains(variable);
    }

    @Override
    public AddMissingAssignsNode buildFlowGraphNode(BasicBlock bb) {
        return new AddMissingAssignsNode(this, bb);
    }

    @Override
    public String getName() {
        return NAME;
    }
}

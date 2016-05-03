package org.jruby.ir.dataflow.analyses;

import java.util.BitSet;
import java.util.HashSet;
import org.jruby.dirgra.Edge;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

/**
  */
public class AddMissingAssignsNode extends FlowGraphNode<AddMissingAssignsProblem, AddMissingAssignsNode> {
    private HashSet out;
    private HashSet use;
    private HashSet def;

    public AddMissingAssignsNode(AddMissingAssignsProblem problem, BasicBlock bb) {
        super(problem, bb);

        out = new HashSet();
        use = new HashSet();
        def = new HashSet();
    }

    @Override
    public void buildDataFlowVars(Instr i) {
        if (i instanceof ResultInstr) registerVariable(((ResultInstr) i).getResult());

        for (Variable x: i.getUsedVariables()) {
            registerVariable(x);
        }
    }

    @Override
    public void applyPreMeetHandler() {
    }

    @Override
    public void compute_MEET(Edge e, AddMissingAssignsNode pred) {
        use.retainAll(pred.out);
    }

    @Override
    public void applyTransferFunction(Instr instr) {
        if (instr instanceof ResultInstr) def.add(((ResultInstr) instr).getResult());

        for (Variable variable: instr.getUsedVariables()) {
            use.add(variable);
        }
    }

    @Override
    public void finalizeSolution() {
        out = def;
    }

    @Override
    public boolean solutionChanged() {
        return !out.equals(def);
    }

    private void registerVariable(Variable variable) {
        if (!problem.hasVariable(variable)) problem.addVariable(variable);
    }
}

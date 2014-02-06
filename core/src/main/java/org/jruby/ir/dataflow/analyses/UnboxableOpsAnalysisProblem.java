package org.jruby.ir.dataflow.analyses;

import java.util.HashMap;
import java.util.Map;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.dataflow.DataFlowConstants;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

// This problem tries to find unboxable (currently, Float and Fixnum) operands
// by inferring types and optimistically assuming that Float and Fixnum numeric
// operations have not been / will not be modified in those classes.
//
// This is a very simplistic type analysis. Doesn't analyze Array/Splat/Range
// operands and array/splat dereference/iteration since that requires assumptions
// to be made about those classes.
//
// In this analysis, we will treat:
// * 'null' as Dataflow.TOP
// * a concrete class as known type
// * 'Object.class' as Dataflow.BOTTOM
//
// Type of a variable will change at most twice: TOP --> class --> BOTTOM

public class UnboxableOpsAnalysisProblem extends DataFlowProblem<UnboxableOpsAnalysisProblem, UnboxableOpsAnalysisNode> {
    public final static String NAME = "UnboxableOpsAnalysis";

    public UnboxableOpsAnalysisProblem() {
        super(DataFlowProblem.DF_Direction.FORWARD);
    }

    @Override
    public String getName() {
        return "Unboxable Operands Analysis";
    }

    @Override
    public UnboxableOpsAnalysisNode buildFlowGraphNode(BasicBlock bb) {
        return new UnboxableOpsAnalysisNode(this, bb);
    }

    @Override
    public String getDataFlowVarsForOutput() {
        return "";
    }

    public void unbox() {
        // System.out.println("---------------- SCOPE BEFORE unboxing ----------------");
        // System.out.println("\nInstrs:\n" + getScope().cfg().toStringInstrs());
        Map<Variable, TemporaryLocalVariable> unboxMap = new HashMap<Variable, TemporaryLocalVariable>();
        for (UnboxableOpsAnalysisNode n : generateWorkList()) {
            n.unbox(unboxMap);
        }

        // SSS FIXME: This should be done differently
        getScope().setDataFlowSolution(DataFlowConstants.LVP_NAME, null);
    }
}

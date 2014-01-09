package org.jruby.ir.dataflow;

import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.UnboxableOpsAnalysisProblem;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class DataFlowConstants {
    public static final String LVP_NAME = LiveVariablesProblem.NAME;
    public static final String LLVP_NAME = (new LoadLocalVarPlacementProblem()).getName();
    public static final String SLVP_NAME = (new StoreLocalVarPlacementProblem()).getName();
    public static final String UNBOXING = UnboxableOpsAnalysisProblem.NAME;
}

package org.jruby.ir.dataflow;

import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.UnboxableOpsAnalysisProblem;

public class DataFlowConstants {
    public static final String LVP_NAME = LiveVariablesProblem.NAME;
    public static final String LLVP_NAME = (new LoadLocalVarPlacementProblem()).getName();
    public static final String SLVP_NAME = (new StoreLocalVarPlacementProblem()).getName();
    public static final String UNBOXING = UnboxableOpsAnalysisProblem.NAME;
}

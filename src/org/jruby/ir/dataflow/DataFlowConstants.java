package org.jruby.ir.dataflow;

import java.util.List;

import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.dataflow.analyses.LoadLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class DataFlowConstants {
    public static final String LVP_NAME = LiveVariablesProblem.NAME;
    public static final String LLVP_NAME = (new LoadLocalVarPlacementProblem()).getName();
    public static final String SLVP_NAME = (new StoreLocalVarPlacementProblem()).getName();

    /* Lattice TOP, BOTTOM, ANY values -- these will be used during dataflow analyses */

    public static final Operand TOP    = new LatticeTop();
    public static final Operand BOTTOM = new LatticeBottom();
    public static final Operand ANY    = new Anything();
  
    private static class LatticeBottom extends Operand {
        @Override
        public void addUsedVariables(List<Variable> l) { 
            /* Nothing to do */
        }

        @Override
        public Operand cloneForInlining(InlinerInfo ii) {
            return this;
        }

        @Override
        public String toString() {
            return "bottom";
        }
    }
  
    private static class LatticeTop extends Operand {
        @Override
        public void addUsedVariables(List<Variable> l) { 
            /* Nothing to do */
        }

        @Override
        public Operand cloneForInlining(InlinerInfo ii) {
            return this;
        }
        @Override
        public String toString() {
            return "top";
        }
    }
  
    private static class Anything extends Operand {
        @Override
        public void addUsedVariables(List<Variable> l) { 
            /* Nothing to do */
        }

        @Override
        public Operand cloneForInlining(InlinerInfo ii) {
            return this;
        }
        @Override
        public String toString() {
            return "anything";
        }
    }
}

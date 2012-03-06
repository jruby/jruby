package org.jruby.compiler.ir.dataflow;

import java.util.List;

import org.jruby.compiler.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.compiler.ir.dataflow.analyses.BindingLoadPlacementProblem;
import org.jruby.compiler.ir.dataflow.analyses.BindingStorePlacementProblem;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class DataFlowConstants {
    public static final String LVP_NAME = LiveVariablesProblem.NAME;
    public static final String BLP_NAME = (new BindingLoadPlacementProblem()).getName();
    public static final String BSP_NAME = (new BindingStorePlacementProblem()).getName();

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

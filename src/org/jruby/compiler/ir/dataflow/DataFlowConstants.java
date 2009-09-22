package org.jruby.compiler.ir.dataflow;

import org.jruby.compiler.ir.operands.Operand;

public class DataFlowConstants
{
    /* Lattice TOP, BOTTOM, ANY values -- these will be used during dataflow analyses */

    public static final Operand TOP    = new LatticeTop();
    public static final Operand BOTTOM = new LatticeBottom();
    public static final Operand ANY    = new Anything();
  
    private static class LatticeBottom extends Operand
    {
        LatticeBottom() { }
        public String toString() { return "bottom"; }
    }
  
    private static class LatticeTop extends Operand
    {
        LatticeTop() { }
        public String toString() { return "top"; }
    }
  
    private static class Anything extends Operand
    {
        Anything() { }
        public String toString() { return "anything"; }
    }
}

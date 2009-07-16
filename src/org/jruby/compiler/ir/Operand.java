package org.jruby.compiler.ir;

public abstract class Operand
{
// ---------- These methods below are used during compile-time optimizations ------- 
    public boolean isConstant() { return false; }

    public Operand getSimplifiedValue() { return this; }

    // SSS FIXME: Premature optimization of GET_ARRAY ... make this part of a pass of peephole optimization!
    // if (getSubArray) is false, returns the 'index' element of the array, else returns the subarray starting at that element
    public Operand fetchCompileTimeArrayElement(int index, boolean getSubArray) { return null; }

//    public abstract Operand toArray();

// ---------- Only static definitions further below ---------
    public static final Operand TOP    = new LatticeTop();
    public static final Operand BOTTOM = new LatticeBottom();
    public static final Operand ANY    = new Anything();
  
    /* Lattice TOP, BOTTOM, ANY values */
    private static class LatticeBottom extends Operand
    {
        LatticeBottom() { }
       
        public String toString() { return "bottom"; }
    }
  
    private static class LatticeTop extends Operand
    {
        LatticeTop() { }
       
        public String toString() { return "top"; }
        public Operand Compute_CP_Meet(Operand op) { return op; }
    }
  
    private static class Anything extends Operand
    {
        Anything() { }
       
        public String toString() { return "anything"; }
        public Operand Compute_CP_Meet(Operand op) { return op; }
    }
}

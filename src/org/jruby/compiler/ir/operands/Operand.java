package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;
import org.jruby.compiler.ir.IR_Class;

public abstract class Operand
{
    public static final Operand[] EMPTY_ARRAY = new Operand[0];

// ---------- These methods below are used during compile-time optimizations ------- 
    public boolean isConstant() { return false; }

    // Arrays, Ranges, etc. are compound values
    // Variables, fixnums, floats, etc. are "atomic" values
    public boolean isNonAtomicValue() { return false; }

    // getSimplifiedOperand returns the value of this operand, fully simplified
    // getSimplifiedOperand returns the operand in a form that can be materialized into bytecode, if it cannot be completely optimized away
    //
    // The value is used during optimizations and propagated through the IR.  But, it is thrown away after that.
    // But, the operand form is used for constructing the compound objects represented by the operand.
    //
    // Example: a = [1], b = [3,4], c = [a,b], d = [2,c]
    //   -- getValue(c) = [1,[3,4]];     getSimplifiedOperand(c) = [1, b]
    //   -- getValue(d) = [2,[1,[3,4]]]; getSimplifiedOperand(d) = [2, c]
    //
    // Note that a,b,c,d are all objects, and c has a reference to objects a and b, and d has a reference to c.
    // So, if contents of a or b is modified, the "simplified value"s of c and d also change!  This difference
    // is captured by these two methods.
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) { return this; }
    public Operand getValue(Map<Operand, Operand> valueMap) { return this; }

    // if (getSubArray) is false, returns the 'index' element of the array, else returns the subarray starting at that element
    public Operand fetchCompileTimeArrayElement(int index, boolean getSubArray) { return null; }

    // Get the target class of this operand, if we know it!
    public IR_Class getTargetClass() { return null; }

    /** Append the list of variables used in this operand to the input list */
    public void addUsedVariables(List<Variable> l) { /* Nothing to do by default */ }
}

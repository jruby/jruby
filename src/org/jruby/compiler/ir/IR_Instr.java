package org.jruby.compiler.ir;

// A generic IR instruction is of the form: v = OP(arg_array, attribute_array)

import java.util.Arrays;

//
// Specialized forms:
//   v = OP(arg1, arg2, attribute_array); Ex: v = ADD(v1, v2)
//   v = OP(arg, attribute_array);        Ex: v = NOT(v1)
//
// _attributes store information about the operands of the instruction that have
// been collected as part of analysis.  For more information, see documentation
// in Attribute.java
//
// Ex: v = BOXED_FIXNUM(n)
//     v = HAS_TYPE(Fixnum)

public abstract class IR_Instr
{
    public final Operation _op;
    public final Variable  _result; 

        // Used during optimization passes to propagate type and other information
    private Attribute[] _attributes;

        // Is this instruction live or dead?  During optimization passes, if this instruction
        // causes no side-effects and the result of the instruction is not needed by anyone else,
        // we can remove this instruction altogether without affecting program correctness.
    private boolean _isDead;

    public IR_Instr(Operation op)
    {
       _op = op;
		 _result = null;
    }

    public IR_Instr(Operation op, Variable res)
    {
        _op = op;
        _result = res;
        _attributes = null;
        _isDead = false;
    }

    public void markDead() { _isDead = true; }
    public boolean isDead() { return _isDead; }

    public abstract Operand[] getOperands();

/**
	 public Variable getResult() { return _result; }
	 public abstract List<Operand> getOperands();

        // Does this instruction have side effects as a result of its operation
        // This information is used in optimization phases to impact dead code elimination
        // and other optimization passes
    public abstract boolean hasSideEffects();
**/

    public String toString() {
        return "\t" + (_result == null ? "" : _result + " = ") + _op;
    }
}

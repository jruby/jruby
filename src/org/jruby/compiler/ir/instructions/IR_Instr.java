package org.jruby.compiler.ir.instructions;

// A generic IR instruction is of the form: v = OP(arg_array, attribute_array)

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Attribute;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

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

    public String toString() {
        return "\t" + (isDead() ? "[DEAD]" : "") + (_result == null ? "" : _result + " = ") + _op;
    }

    public Variable getResult() { return _result; }

        // Does this instruction have side effects as a result of its operation
        // This information is used in optimization phases to impact dead code elimination
        // and other optimization passes
    public boolean hasSideEffects() { return _op.hasSideEffects(); }

    public void markDead() { _isDead = true; }
    public boolean isDead() { return _isDead; }

/* --------- "Abstract"/"please-override" methods --------- */

    /* Array of all operands for this instruction */
    public abstract Operand[] getOperands();

    /* List of all variables used by all operands of this instruction */
    public List<Variable> getUsedVariables()
    {
        ArrayList<Variable> vars = new ArrayList<Variable>();
        for (Operand o: getOperands()) {
            o.addUsedVariables(vars);
        }

		  return vars;
    }

    /* 
     * This method takes as input a map of operands to their values, and outputs
     *
     * If the value map provides a value for any of the instruction's operands
     * this method is expected to replace the original operands with the simplified values.
     * It is not required that it do so -- code correctness is not compromised by failure
     * to simplify
     */
    public abstract void simplifyOperands(Map<Operand, Operand> valueMap);

    /* 
     * This method takes as input a map of operands to their values, and outputs
     * the result of this instruction.
     *
     * If the value map provides a value for any of the instruction's operands
     * the expectation is that the operand will be replaced with the simplified value.
     * It is not required that it do so -- code correctness is not compromised by failure
     * to simplify.
     *
     * @param valueMap Mapping from operands to their simplified values
     * @returns simplified result / output of this instruction
     */
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap)
    {
        simplifyOperands(valueMap);
        // By default, no simplifications!
        return null;
    }
}

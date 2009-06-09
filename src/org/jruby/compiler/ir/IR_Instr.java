package org.jruby.compiler.ir;

// A generic IR instruction is of the form: v = OP(arg_array, attribute_array)
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
	public final Operand   _result; 

	public Attribute[] _attributes;

	public IR_Instr(Operation op, Operand res)
	{
		_op = op;
		_result = res;
	}
}

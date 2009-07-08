package org.jruby.compiler.ir;

// Implements the defined? keyword in ruby.
// This will be implemented in the JRuby runtime.
public class IS_DEFINED_Instr extends OneOperandInstr
{
    public IS_DEFINED_Instr(Variable result, Operand arg)
    {
        super(Operation.IS_DEFINED, result, arg);
    }
}

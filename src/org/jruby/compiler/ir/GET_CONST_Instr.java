package org.jruby.compiler.ir;

public class GET_CONST_Instr extends TwoOperandInstr
{
    public GET_CONST_Instr(Variable dest, IR_Scope scope, String constName)
    {
        super(Operation.GET_CONST, dest, new MetaObject(scope), new Reference(fieldName));
    }

    public GET_CONST_Instr(Variable dest, Operand scopeOrObj, String constName)
    {
        super(Operation.GET_CONST, dest, scopeOrObj, new Reference(fieldName));
    }
}

package org.jruby.compiler.ir;

public class PUT_CONST_Instr extends PUT_Instr
{
    public PUT_CONST_Instr(IR_Scope scope, String constName, Operand val)
    {
        super(Operation.PUT_CONST, new MetaObject(scope), constName, val);
    }

    public PUT_CONST_Instr(Operand scopeOrObj, String constName, Operand val)
    {
        super(Operation.PUT_CONST, scopeOrObj, constName, val);
    }
}

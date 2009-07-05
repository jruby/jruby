package org.jruby.compiler.ir;

public class PUT_CONST_Instr extends IR_Instr
{
    final public Operand _value;
    final public Operand _scopeOrObj;
    final public String  _constName;

    public PUT_CONST_Instr(IR_Scope scope, String constName, Operand val)
    {
        super(Operation.PUT_CONST);
        _scopeOrObj = new MetaObject(scope);
        _constName = constName;
        _value = val;
    }

    public PUT_CONST_Instr(Operand scopeOrObj, String constName, Operand val)
    {
        super(Operation.PUT_CONST);
        _scopeOrObj = scopeOrObj;
        _constName = constName;
        _value = val;
    }
}

package org.jruby.compiler.ir;

public class PUT_GLOBAL_VAR_Instr extends IR_Instr
{
    final public Operand _value;
    final public GlobalVariable _gvar;

    public PUT_GLOBAL_VAR_Instr(String varName, Operand value)
    {
        super(Operation.PUT_GLOBAL_VAR);
        _gvar = new GlobalVariable(varName);
        _value = value;
    }

    public Operand[] getOperands() {
        return new Operand[] {_value};
    }
}

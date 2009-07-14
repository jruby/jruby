package org.jruby.compiler.ir;

public class GET_ARRAY_Instr extends IR_Instr
{
    public final Operand _array;
    public final int     _index;

    public GET_ARRAY_Instr(Variable dest, Operand array, int index)
    {
        super(Operation.GET_ARRAY, dest);
        _array = array;
        _index = index;
    }

    public String toString() { return "\t" + _result + " = " + _array + "[" + _index + "] (GET_ARRAY)"; }
}

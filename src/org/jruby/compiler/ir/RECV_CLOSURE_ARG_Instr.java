package org.jruby.compiler.ir;

public class RECV_CLOSURE_ARG_Instr extends IR_Instr
{
    int     _argIndex;
    boolean _restOfArgArray;

    public RECV_CLOSURE_ARG_Instr(Variable dest, int argIndex, boolean restOfArgArray)
    {
        super(Operation.RECV_CLOSURE_ARG, dest);
        _argIndex = argIndex;
        _restOfArgArray = restOfArgArray;
    }

    public String toString() { return super.toString() + "(" + _argIndex + (_restOfArgArray ? ", ALL" : "") + ")"; }
}

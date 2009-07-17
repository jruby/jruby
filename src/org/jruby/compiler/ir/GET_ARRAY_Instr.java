package org.jruby.compiler.ir;

// NOTE: A negative index signifies "rest" of the array starting at the index value
public class GET_ARRAY_Instr extends OneOperandInstr
{
    public final int     _index;
    public final boolean _all;  // If true, returns the rest of the array starting at the index

    public GET_ARRAY_Instr(Variable dest, Operand array, int index, boolean getRestOfArray)
    {
        super(Operation.GET_ARRAY, dest, array);
        _index = index;
        _all   = getRestOfArray; 
    }

    public String toString() { return "\t" + _result + " = " + _arg + "[" + _index + (_all ? ":END" : "") + "] (GET_ARRAY)"; }

    public Operand getSimplifiedValue()
    {
        return _arg.fetchCompileTimeArrayElement(_index, _all);
    }
}

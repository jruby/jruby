package org.jruby.compiler.ir;

public class GET_CVAR_Instr extends OneOperandInstr
{
    final public String   _varName;

    public GET_CVAR_Instr(Variable dest, Operand scope, String varName)
    {
        super(Operation.GET_CVAR, dest, getParentmostScope(scope));
        _varName = varName;
    }

    public static Operand getParentmostScope(Operand scope) {
        while ((scope instanceof MetaObject) && !(((MetaObject)scope)._scope instanceof IR_Class))
            scope = ((MetaObject)scope)._scope.getParent();

        return scope;
    }

    public String toString() { return super.toString() + "(" + _arg + "." + _varName + ")"; }
}

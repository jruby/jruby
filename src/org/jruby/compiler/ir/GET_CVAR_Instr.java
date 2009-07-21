package org.jruby.compiler.ir;

public class GET_CVAR_Instr extends GET_Instr
{
    public GET_CVAR_Instr(Variable dest, Operand scope, String varName)
    {
        super(Operation.GET_CVAR, dest, getParentmostScope(scope), varName);
    }

    public static Operand getParentmostScope(Operand scope) {
        while ((scope instanceof MetaObject) && !(((MetaObject)scope)._scope instanceof IR_Class))
            scope = ((MetaObject)scope)._scope.getParent();

        return scope;
    }
}

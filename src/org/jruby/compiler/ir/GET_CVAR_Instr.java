package org.jruby.compiler.ir;

public class GET_CVAR_Instr extends IR_Instr
{
    final public Operand  _scope;
    final public String   _varName;

    public GET_CVAR_Instr(Variable dest, Operand scope, String varName)
    {
        super(Operation.GET_CVAR, dest);
        _varName = varName;

        // Walk up the scope tree right now as much as possible, to avoid run-time walking
        // SSS FIXME: Any reason why this might break in the presence of ruby's dynamic resolution?  What might break?
        while ((scope instanceof MetaObject) && !(((MetaObject)scope)._scope instanceof IR_Class))
            scope = ((MetaObject)scope)._scope.getParent();

        _scope = scope;
    }

    public String toString() { return super.toString() + "(" + _scope + "." + _varName + ")"; }
}

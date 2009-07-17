package org.jruby.compiler.ir;

public class PUT_CVAR_Instr extends IR_Instr
{
    final public Operand  _scope;
    final public String   _varName;
    final public Operand  _value;

    public PUT_CVAR_Instr(Operand scope, String varName, Operand value)
    {
        super(Operation.PUT_CVAR);
		  _varName = varName;
		  _value = value;

        // Walk up the scope tree right now as much as possible, to avoid run-time walking
        // SSS FIXME: Any reason why this might break in the presence of ruby's dynamic resolution?  What might break?
        while ((scope instanceof MetaObject) && !(((MetaObject)scope)._scope instanceof IR_Class))
            scope = ((MetaObject)scope)._scope.getParent();

		  _scope = scope;
    }

    public String toString() { return "\tPUT_CVAR(" + _scope + "." + _varName + ") = " + _value; }

    public Operand[] getOperands() {
        return new Operand[] {_scope, _value};
    }
}

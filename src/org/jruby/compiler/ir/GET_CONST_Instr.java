package org.jruby.compiler.ir;

// NOTE: the scopeOrObj operand can be a dynamic scope.
//
// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.
//
public class GET_CONST_Instr extends TwoOperandInstr
{
    public GET_CONST_Instr(Variable dest, IR_Scope scope, String constName)
    {
        super(Operation.GET_CONST, dest, new MetaObject(scope), new Reference(fieldName));
    }

    public GET_CONST_Instr(Variable dest, Operand scopeOrObj, String constName)
    {
        super(Operation.GET_CONST, dest, scopeOrObj, new Reference(fieldName));
    }
}

package org.jruby.compiler.ir;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accummulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
public class IR_Closure implements IR_ScopeImpl
{
    public IR_Closure(IR_Scope parent) { super(parent); }

    public Operand getConstantValue(String constRef)
    {
           // Constants are defined in classes & modules, not in closures
           // So, this reference is actually defined in the containing class/module
       return _parent.getConstantValue(constRef);  
    }

    public void setConstantValue(String constRef, Operand val) 
    { 
       // SSS FIXME: Throw an exception here?
    }
}

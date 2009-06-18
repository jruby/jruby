package org.jruby.compiler.ir;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accummulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
public class IR_Closure implements IR_BaseContext
{
    public IR_Closure(IR_BuilderContext parent) { super(parent); }
}

package org.jruby.compiler.ir;

public class IR_Class implements IR_ScopeImpl
{
    final public String _className;

    public IR_Class(IR_Scope parent, String className) 
    { 
       super(parent); 
       _className = className;
    }
}

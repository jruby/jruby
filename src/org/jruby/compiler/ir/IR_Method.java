package org.jruby.compiler.ir;

import org.jruby.util.JavaNameMangler;

public class IR_Method extends IR_ScopeImpl
{
    String  _name;        // Ruby name 
    String  _irName;      // Generated name
    boolean _isInstanceMethod;

    public IR_Method(IR_Scope parent, String name, boolean isInstanceMethod)
    {
        super(parent);
        _name = name;
        if (root && Boolean.getBoolean("jruby.compile.toplevel")) {
            _irName = name;
        } else {
            String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
            // FIXME: What is this script business here?
            _irName = "method__" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;
        }

        _isInstanceMethod = isInstanceMethod;

        // SSS: FIXME: More stuff needs to happen here with the parent scope
    }

    public Operand getConstantValue(String constRef)
    {
           // Constants are defined in classes & modules, not in methods!
           // So, this reference is actually defined in the containing class/module
       return _parent.getConstantValue(constRef);  
    }

    public void setConstantValue(String constRef, Operand val) 
    { 
       // SSS FIXME: Throw an exception here?
    }
}

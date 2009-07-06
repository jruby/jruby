package org.jruby.compiler.ir;

public class IR_Method extends IR_ScopeImpl
{
    String  _name;        // Ruby name 
    boolean _isInstanceMethod;

    public final Label _startLabel;   // Label for the start of the method
    public final Label _endLabel;   // Label for the end of the method

    public IR_Method(IR_Scope parent, String name, String javaName, boolean isInstanceMethod)
    {
        this(parent, name, isInstanceMethod);
    }

    public IR_Method(IR_Scope parent, String name, boolean isInstanceMethod)
    {
        super(parent);
        _name = name;
        _isInstanceMethod = isInstanceMethod;
        _startLabel = getNewLabel("_METH_START_");
        _endLabel   = getNewLabel("_METH_END_");
    }

    public Operand getConstantValue(String constRef)
    {
            // Constants are defined in classes & modules, not in methods!
            // So, this reference is actually defined in the containing class/module
        if (_parent instanceof MetaObject) {
            return ((MetaObject)_parent)._scope.getConstantValue(constRef);  
        }
        else {
            Variable cv = getNewVariable();
            addInstr(new GET_CONST_Instr(cv, _parent, constRef));
            return cv;
        }
    }

    public void setConstantValue(String constRef, Operand val) 
    { 
        // SSS FIXME: Throw an exception here?
    }

    public String toString() {
        return "Method: " +
                "\n  name: " + _name +
                super.toString();
    }
}

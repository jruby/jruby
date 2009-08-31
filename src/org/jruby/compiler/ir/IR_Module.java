package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Module extends IR_ScopeImpl
{
    public final String _name;

    public IR_Module(IR_Scope parent, IR_Scope lexicalParent, String name)
    { 
        super(parent, lexicalParent);
        _name = name;
    }

    public IR_Module(Operand parent, IR_Scope lexicalParent, String name)
    { 
        super(parent, lexicalParent);
        _name = name;
    }

    public IR_Method getInstanceMethod(String name)
    {
        for (IR_Method m: _methods) {
            if (m._isInstanceMethod && m._name.equals(name))
                return m;
        }

        return null;
    }

    public IR_Method getClassMethod(String name)
    {
        for (IR_Method m: _methods)
            if (!m._isInstanceMethod && _name.equals(name))
                return m;

        return null;
    }

    public String toString() {
        return "Module: " + _name + super.toString();
    }
}

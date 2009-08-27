package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Module extends IR_ScopeImpl
{
    public final String _moduleName;

    public IR_Module(IR_Scope parent, IR_Scope lexicalParent, String name)
    { 
        super(parent, lexicalParent);
        _moduleName = name;
    }

    public IR_Module(Operand parent, IR_Scope lexicalParent, String name)
    { 
        super(parent, lexicalParent);
        _moduleName = name;
    }

    public String toString() {
        return "Module: " +
                "\n  moduleName: " + _moduleName +
                super.toString();
    }
}

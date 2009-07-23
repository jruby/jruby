package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Module extends IR_ScopeImpl
{
    public final String _moduleName;

    public IR_Module(IR_Scope parent, String name) 
    { 
        super(parent); 
        _moduleName = name;
    }

    public IR_Module(Operand parent, String name) 
    { 
        super(parent); 
        _moduleName = name;
    }
}

package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;

public class IR_Script extends IR_ScopeImpl
{
    public final String    _fileName;    // SSS FIXME: Should this be a string literal or a string?
    public final IR_Class  _dummyClass;  // Dummy class for the script

    public IR_Script(String className, String sourceName)
    {
        super((IR_Scope)null, null);
        _fileName = sourceName;
        _dummyClass = new IR_Class(this, this, null, "__SCRIPT_ROOT__", false);
		  addClass(_dummyClass);
    }

    public Operand getFileName() { return new StringLiteral(_fileName); }

    public IR_Method getRootMethod() { return _dummyClass.getRootMethod(); }

    public IR_Class getRootClass() { return _dummyClass; }

    public String toString() {
        return "Script: file: " + getFileName() + super.toString();
    }
}

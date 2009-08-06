package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;

public class IR_Script extends IR_ScopeImpl
{
    public final String    _fileName;    // SSS FIXME: Should this be a string literal or a string?
    public final IR_Class  _dummyClass;  // Dummy class for the script
    public final IR_Method _dummyMethod; // Dummy top-level method for the script -- added to the dummy class

    public IR_Script(String className, String sourceName)
    {
        super((IR_Scope)null, null);
        _fileName = sourceName;

            // Build a dummy class: SSS FIXME: What name for the class?
        _dummyClass = new IR_Class(this, this, null, "_DUMMY_", false);

        // SSS FIXME: Set other appropriate JVM flags on the class ... see line below from StandardASMCompiler.java
        // classWriter.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER,getClassname(), null, p(AbstractScript.class), null);

            // Build a dummy static method for the dummy class
        _dummyMethod = new IR_Method(_dummyClass, _dummyClass, "__file__", "__file__", false);
        _dummyClass.addMethod(_dummyMethod);
    }

    public Operand getFileName() { return new StringLiteral(_fileName); }

    public IR_Method getRootMethod() { return _dummyMethod; }

    public IR_Class getRootClass() { return _dummyClass; }

    public String toString() {
        return "Script: " +
                "\n  file: " + getFileName() +
                "\n  class:\n" + getRootClass() +
                super.toString();
    }
}

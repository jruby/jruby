package org.jruby.compiler.ir;

public class IR_Class implements IR_BuilderContext
{
    IR_BuilderContext _container;   // The container for this class (can be a script)

      // Delegate method to the containing script/module/class
    public StringLiteral getFileName() { return _container.getFileName(); }
}

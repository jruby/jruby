package org.jruby.compiler.ir;

public class IR_Module implements IR_BuilderContext
{
    IR_BuilderContext _container;   // The container for this method (can be a script)

      // Delegate method to the containing module/script
    public StringLiteral getFileName() { return _container.getFileName(); }
}

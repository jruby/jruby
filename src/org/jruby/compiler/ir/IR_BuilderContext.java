package org.jruby.compiler.ir;

// SSS FIXME: Probably should rename this to IR_Scope?
// Easier to understand and it is in any case a scope, not just a IR builder context!
public interface IR_BuilderContext
{
        // scripts
    public void addClass(IR_Class c);

        // scripts, classes, and modules
    public void addMethod(IR_Method m);

        // scripts, classes, modules, methods, and closures
    public void addInstr(IR_Instr i);

        // create a new variable
    public Variable getNewVariable();

        // create a new variable using the prefix
    public Variable getNewVariable(String prefix);

        // scripts
    public StringLiteral getFileName();
}

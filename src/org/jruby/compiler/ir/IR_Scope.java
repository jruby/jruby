package org.jruby.compiler.ir;

// SSS FIXME: Probably should rename this to IR_Scope?
// Easier to understand and it is in any case a scope, not just a IR builder context!
public interface IR_Scope
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

        // get "self"
    public Variable getSelf();

        // scripts
    public String getFileName();

        // Tries to load at compile-time the constant referred to by 'constRef'.
        // This might be possible if the constant is defined and is not a forward reference
        // to a value that will be defined later in the class.
    public Operand getConstantValue(String constRef);

        // Tries to load at compile-time the constant referred to by 'constRef'.
        // This might be possible if the constant is defined and is not a forward reference
        // to a value that will be defined later in the class.
    public Operand setConstantValue(String constRef);
}

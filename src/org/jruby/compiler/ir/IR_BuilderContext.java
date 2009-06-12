package org.jruby.compiler.ir;

public interface IR_BuilderContext
{
        // scripts
    public void addClass(IR_Class c);

        // scripts, classes, and modules
    public void addMethod(IR_Method m);

        // methods, scripts, classes, and modules
    public void addInstr(IR_Instr i);

    // create a new variable
    public Variable getNewVariable(String name);
}

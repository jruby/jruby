package org.jruby.compiler.ir;

import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;

// Script, Module, Class, Method, Closure
public interface IR_Scope
{
        // Returns the containing parent scope -- can be a dynamic value (hence Operand)!
    public Operand getParent();

        // scripts
    public void addClass(IR_Class c);

        // scripts and modules
    public void addModule(IR_Module m);

        // scripts, classes, and modules
    public void addMethod(IR_Method m);

        // methods and closures
    public void addInstr(IR_Instr i);

        // create a new variable using the prefix
    public Variable getNewVariable(String prefix);

        // create a new temporary variable
    public Variable getNewVariable();

        // Get a new label using the provided label prefix
    public Label getNewLabel(String lblPrefix);

        // Get a new label using a generic prefix
    public Label getNewLabel();

        // get "self"
    public Variable getSelf();

        // Build the CFG for this scope -- supported only by methods & closures
    public CFG buildCFG();

        // Get the control flow graph for this scope -- only valid for methods & closures
    public CFG getCFG(); 

        // Tries to load at compile-time the constant referred to by 'constRef'.
        // This might be possible if the constant is defined and is not a forward reference
        // to a value that will be defined later in the class.
    public Operand getConstantValue(String constRef);

        // Tries to load at compile-time the constant referred to by 'constRef'.
        // This might be possible if the constant is defined and is not a forward reference
        // to a value that will be defined later in the class.
    public void setConstantValue(String constRef, Operand value);

        // While processing loops, this returns the loop that we are processing.
    public IR_Loop getCurrentLoop();

        // Record the loop we are beginning to process
    public void startLoop(IR_Loop l);

        // Indicate that we are done processing the loop
    public void endLoop(IR_Loop l);

        // Run the passed in compiler pass on this scope!
    public void runCompilerPass(CompilerPass opt);
}

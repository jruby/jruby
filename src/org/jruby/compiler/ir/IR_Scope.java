package org.jruby.compiler.ir;

import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

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

        // Record that newName is a new method name for method with oldName
        // This is for the 'alias' keyword which resolves method names in the static compile/parse-time context
    public void recordMethodAlias(String newName, String oldName);

        // Unalias 'name' and return new name
    public String unaliasMethodName(String name);

        // Get the next available unique closure id for closures in this scope
    public int getNextClosureId();

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

        // Tries to load at compile-time the constant referred to by 'constRef'.
        // This might be possible if the constant is defined and is not a forward reference
        // to a value that will be defined later in the class.
    public Operand getConstantValue(String constRef);

        // Tries to load at compile-time the constant referred to by 'constRef'.
        // This might be possible if the constant is defined and is not a forward reference
        // to a value that will be defined later in the class.
    public void setConstantValue(String constRef, Operand value);

        // Run the passed in compiler pass on this scope!
    public void runCompilerPass(CompilerPass opt);
}

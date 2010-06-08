package org.jruby.compiler.ir;

import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.LocalVariable;

/**
 *  Script, Module, Class, Method, Closure
 */
public interface IRScope {
    /**
     *  Returns the containing parent scope
     */
    public Operand getContainer();

    /**
     *  Returns the lexical scope that contains this scope definition
     */
    public IRScope getLexicalParent();

    /**
     * Returns the nearest module/class from this scope which may be itself.
     */
    public IRModule getNearestModule();

    /**
     *  scripts
     */
    public void addClass(IRClass c);

    /**
     *  scripts and modules
     */
    public void addModule(IRModule m);

    /**
     *  methods and closures
     */
    public void addInstr(Instr i);

    /**
     *  Record that newName is a new method name for method with oldName
     * This is for the 'alias' keyword which resolves method names in the
     * static compile/parse-time context
     */
    public void recordMethodAlias(String newName, String oldName);

    /**
     *  Unalias 'name' and return new name
     */
    public String unaliasMethodName(String name);

    /**
     *  Get the next available unique closure id for closures in this scope
     */
    public int getNextClosureId();

    /**
     *  create a new temporary variable
     */
    public Variable getNewTemporaryVariable();

    /**
     * How many temporary variables are in this scope?
     */
    public int getTemporaryVariableSize();

    /**
     * Get Local Variable from this scope
     */
    public LocalVariable getLocalVariable(String name);

    /**
     *  Get a new label using the provided label prefix
     */
    public Label getNewLabel(String lblPrefix);

    /**
     *  Get a new label using a generic prefix
     */
    public Label getNewLabel();

    /**
     *  get "self"
     */
    public Variable getSelf();

    /**
     *  Tries to load at compile-time the constant referred to by 'constRef'.
     * This might be possible if the constant is defined and is not a forward
     * reference to a value that will be defined later in the class.
     */
    public Operand getConstantValue(String constRef);

    /**
     * Tries to load at compile-time the constant referred to by 'constRef'.
     * This might be possible if the constant is defined and is not a forward
     * reference to a value that will be defined later in the class.
     */
    public void setConstantValue(String constRef, Operand value);

    /**
     *  Run the passed in compiler pass on this scope!
     */
    public void runCompilerPass(CompilerPass opt);
}

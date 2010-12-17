package org.jruby.compiler.ir;

import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;

/**
 * IRScope is the interface for all lexically scoped constructs: Script, Module,
 * Class, Method, and Closure.  It is important to understand that a single class (e.g. MyFoo)
 * may be opened lexically in several locations in source code.  Each one of these locations will
 * have their own instance of an IRScope.
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
     */
    public StaticScope getStaticScope();

    /**
     * How many temporary variables are in this scope?
     */
    public int getTemporaryVariableSize();

    /**
     * How many renamed variables are in this scope?
     */
    public int getRenamedVariableSize();

    /**
     * Get Local Variable from this scope
     */
    public LocalVariable getLocalVariable(String name);

    public String getName();

    /**
     *  Get a new label using the provided label prefix
     */
    public Label getNewLabel(String lblPrefix);

    /**
     *  Get a new label using a generic prefix
     */
    public Label getNewLabel();

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

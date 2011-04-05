package org.jruby.compiler.ir;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.TemporaryClosureVariable;
import org.jruby.compiler.ir.operands.TemporaryVariable;
import org.jruby.compiler.ir.operands.RenamedVariable;
import org.jruby.compiler.ir.compiler_pass.AddBindingInstructions;
import org.jruby.compiler.ir.compiler_pass.CFG_Builder;
import org.jruby.compiler.ir.compiler_pass.LiveVariableAnalysis;
import org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination;
import org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass;
import org.jruby.parser.StaticScope;

/**
 * Right now, this class abstracts 5 different scopes: Script, Module, Class, 
 * Method, and Closure.
 *
 * Script, Module, and Class are containers and "non-execution" scopes.
 * Method and Closure are the only two "execution" scopes.
 *
 * In the compiler-land, IR_* versions of these scopes encapsulate only as much 
 * information as is required to convert Ruby code into equivalent Java code.
 *
 * But, in the non-compiler land, there will be a corresponding java object for
 * each of these scopes which encapsulates the runtime semantics and data needed
 * for implementing them.  In the case of Module, Class, and Method, they also
 * happen to be instances of the corresponding Ruby classes -- so, in addition
 * to providing code that help with this specific ruby implementation, they also
 * have code that let them behave as ruby instances of their corresponding
 * classes.  Script and Closure have no such Ruby companions, as far as I can
 * tell.
 *
 * Examples:
 * - the runtime class object might have refs. to the runtime method objects.
 * - the runtime method object might have a slot for a heap frame (for when it
 *   has closures that need access to the method's local variables), it might
 *   have version information, it might have references to other methods that
 *   were optimized with the current version number, etc.
 * - the runtime closure object will have a slot for a heap frame (for when it 
 *   has closures within) and might get reified as a method in the java land
 *   (but inaccessible in ruby land).  So, passing closures in Java land might
 *   be equivalent to passing around the method handles.
 *
 * and so on ...
 */
public abstract class IRScopeImpl implements IRScope {
    // SSS FIXME: Dumb design leaking a live operand into a non-operand!!
    Operand container;       // Parent container for this context
    RubyModule containerModule; // Live version of container
    IRScope lexicalParent;  // Lexical parent scope

    private String name;

    // ENEBO: These collections are initliazed on construction, but the rest
    //   are init()'d.  This can't be right can it?

    // oldName -> newName for methods
    private Map<String, String> aliases = new HashMap<String, String>();

    // Index values to guarantee we don't assign same internal index twice
    private int nextClosureIndex = 0;

    // Keeps track of types of prefix indexes for variables and labels
    private Map<String, Integer> nextVarIndex = new HashMap<String, Integer>();

    private StaticScope staticScope;

    public IRScopeImpl(IRScope lexicalParent, Operand container, String name, StaticScope staticScope) {
        this.lexicalParent = lexicalParent;
        this.container = container;
        this.name = name;
        this.staticScope = staticScope;
    }

    // Update the containing scope
    public void setContainer(Operand o) {
        container = o;
    }

    // Returns the containing scope!
    public Operand getContainer() {
        return container;
    }

    public RubyModule getContainerModule() {
//        System.out.println("GET: container module of " + getName() + " with hc " + hashCode() + " to " + containerModule.getName());
        return containerModule;
    }

    public IRScope getLexicalParent() {
        return lexicalParent;
    }

    public IRModule getNearestModule() {
        IRScope current = lexicalParent;

        while (current != null && !(current instanceof IRModule) && !(current instanceof IRScript)) {
            current = current.getLexicalParent();
        }
        
        if (current instanceof IRScript) { // Possible we are a method at top-level.
            current = ((IRScript) current).getRootClass();
        }

        return (IRModule) current;
    }

    public int getNextClosureId() {
        nextClosureIndex++;
        
        return nextClosureIndex;
    }

    public Variable getNewTemporaryClosureVariable(int closureId) {
        return new TemporaryClosureVariable(closureId, allocateNextPrefixedName("%cl_" + closureId));
    }

    public Variable getNewTemporaryVariable() {
        return new TemporaryVariable(allocateNextPrefixedName("%v"));
    }

    // Generate a new variable for inlined code (just for ease of debugging, use differently named variables for inlined code)
    public Variable getNewInlineVariable() {
        return new RenamedVariable("%i", allocateNextPrefixedName("%i"));
    }

    public int getTemporaryVariableSize() {
        return getPrefixCountSize("%v");
    }

    public int getRenamedVariableSize() {
        return getPrefixCountSize("%i");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { // This is for IRClosure ;(
        this.name = name;
    }
    
    public abstract String getScopeName();

    public Label getNewLabel(String prefix) {
        return new Label(prefix + "_" + allocateNextPrefixedName(prefix));
    }

    public Label getNewLabel() {
        return getNewLabel("LBL");
    }

    // Enebo: We should just make n primitive int and not take the hash hit
    private int allocateNextPrefixedName(String prefix) {
        int index = getPrefixCountSize(prefix);
        
        nextVarIndex.put(prefix, index + 1);
        
        return index;
    }

    protected int getPrefixCountSize(String prefix) {
        Integer index = nextVarIndex.get(prefix);

        if (index == null) return 0;

        return index.intValue();
    }

    public StaticScope getStaticScope() {
        return staticScope;
    }

    public void addInstr(Instr i) {
        throw new RuntimeException("Encountered instruction add in a non-execution scope!");
    }

    // Record that newName is a new method name for method with oldName
    // This is for the 'alias' keyword which resolves method names in the static compile/parse-time context
    public void recordMethodAlias(String newName, String oldName) {
        aliases.put(oldName, newName);
    }

    // Unalias 'name' and return new name
    public String unaliasMethodName(String name) {
        String n = name;
        String a = null;
        do {
            a = aliases.get(n);
            if (a != null) n = a;
        } while (a != null);

        return n;
    }

    public List<Instr> getInstrs() {
        return null;
    }

    @Override
    public String toString() {
        return getScopeName() + " " + getName();
    }

    public void runCompilerPassOnNestedScopes(CompilerPass p) { }

    public void runCompilerPass(CompilerPass p) {
        boolean isPreOrder = p.isPreOrder();

        if (isPreOrder) p.run(this);

        runCompilerPassOnNestedScopes(p);

        if (!isPreOrder) p.run(this);
    }

    /* Run any necessary passes to get the IR ready for interpretation */
    public void prepareForInterpretation() {
        // SSS FIXME: We should configure different optimization levels
        // and run different kinds of analysis depending on time budget.  Accordingly, we need to set
        // IR levels/states (basic, optimized, etc.) and the
        // ENEBO: If we use a MT optimization mechanism we cannot mutate CFG
        // while another thread is using it.  This may need to happen on a clone()
        // and we may need to update the method to return the new method.  Also,
        // if this scope is held in multiple locations how do we update all references?
        runCompilerPass(new LocalOptimizationPass());
        runCompilerPass(new CFG_Builder());
        runCompilerPass(new LiveVariableAnalysis());
        runCompilerPass(new DeadCodeElimination());
        runCompilerPass(new AddBindingInstructions());
    }

    public String toStringInstrs() {
        return "";
    }

    public String toStringVariables() {
        return "";
    }
}

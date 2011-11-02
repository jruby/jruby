package org.jruby.compiler.ir;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.TemporaryVariable;
import org.jruby.compiler.ir.operands.RenamedVariable;
import org.jruby.compiler.ir.compiler_pass.AddBindingInstructions;
import org.jruby.compiler.ir.compiler_pass.CFGBuilder;
import org.jruby.compiler.ir.compiler_pass.IRPrinter;
import org.jruby.compiler.ir.compiler_pass.InlineTest;
import org.jruby.compiler.ir.compiler_pass.LinearizeCFG;
import org.jruby.compiler.ir.compiler_pass.LiveVariableAnalysis;
import org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination;
import org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass;
import org.jruby.parser.StaticScope;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger("IRScope");

    private IRScope lexicalParent;  // Lexical parent scope
    private RubyModule containerModule; // Live version of container

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

    public IRScopeImpl(IRScope lexicalParent, String name, StaticScope staticScope) {
        this.lexicalParent = lexicalParent;
        this.name = name;
        this.staticScope = staticScope;
    }

    public RubyModule getContainerModule() {
//        System.out.println("GET: container module of " + getName() + " with hc " + hashCode() + " to " + containerModule.getName());
        return containerModule;
    }

    public IRScope getLexicalParent() {
        return lexicalParent;
    }

    public IRModule getNearestModule() {
        IRScope current = this;

        while (current != null && !((current instanceof IRModule) || (current instanceof IRScript) || (current instanceof IREvalScript))) {
            current = current.getLexicalParent();
        }

        // In eval mode, we dont have a lexical view of what module we are nested in
        // because binding_eval, class_eval, module_eval, instance_eval can switch
        // around the lexical scope for evaluation to be something else.
        if (current instanceof IREvalScript) {
            return null;
        }

        if (current instanceof IRScript) { // Possible we are a method at top-level.
            current = ((IRScript) current).getRootClass();
        }

        return (IRModule) current;
    }
    
    public IRMethod getNearestMethod() {
        IRScope current = this;

        while (current != null && !(current instanceof IRMethod)) {
            current = current.getLexicalParent();
        }
        
        assert current instanceof IRMethod : "All scopes must be surrounded by at least one method";
        
        return (IRMethod) current;
    }

    /**
     * Returns the top level executable scope
     */
    public IRScope getTopLevelScope() {
        IRScope current = this;

        while (!(current instanceof IREvalScript) && !(current instanceof IRScript)) {
            current = current.getLexicalParent();
        }
        
        return current;
    }

    public int getNextClosureId() {
        nextClosureIndex++;
        
        return nextClosureIndex;
    }

    public Variable getNewTemporaryVariable() {
        return new TemporaryVariable(allocateNextPrefixedName("%v"));
    }

    // Generate a new variable for inlined code (for ease of debugging, use differently named variables for inlined code)
    public Variable getNewInlineVariable() {
        // Use the temporary variable counters for allocating temporary variables
        return new RenamedVariable("%i", allocateNextPrefixedName("%v"));
    }

    public int getTemporaryVariableSize() {
        return getPrefixCountSize("%v");
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
    protected int allocateNextPrefixedName(String prefix) {
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
        return Collections.EMPTY_LIST;
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
        // Should be an execution scope
        if (!(this instanceof IRExecutionScope)) return;

        // forcibly clear out the shared eval-scope variable allocator each time this method executes
        ((IRExecutionScope)this).initEvalScopeVariableAllocator(true); 

        // SSS FIXME: We should configure different optimization levels
        // and run different kinds of analysis depending on time budget.  Accordingly, we need to set
        // IR levels/states (basic, optimized, etc.) and the
        // ENEBO: If we use a MT optimization mechanism we cannot mutate CFG
        // while another thread is using it.  This may need to happen on a clone()
        // and we may need to update the method to return the new method.  Also,
        // if this scope is held in multiple locations how do we update all references?

        printPass("Before local optimization pass");
        runCompilerPass(new LocalOptimizationPass());
        printPass("After local optimization pass");

        runCompilerPass(new CFGBuilder());
        if (!RubyInstanceConfig.IR_TEST_INLINER.equals("none")) {
            if (RubyInstanceConfig.IR_COMPILER_DEBUG) {
                LOG.info("Asked to inline " + RubyInstanceConfig.IR_TEST_INLINER);
            }
            runCompilerPass(new InlineTest(RubyInstanceConfig.IR_TEST_INLINER));
            runCompilerPass(new LocalOptimizationPass());
            printPass("After inline");
        }        
        if (RubyInstanceConfig.IR_LIVE_VARIABLE) runCompilerPass(new LiveVariableAnalysis());
        if (RubyInstanceConfig.IR_DEAD_CODE) runCompilerPass(new DeadCodeElimination());
        if (RubyInstanceConfig.IR_DEAD_CODE) printPass("After DCE ");
        runCompilerPass(new LinearizeCFG());
        printPass("After CFG Linearize");
    }
    
    private void printPass(String message) {
        if (RubyInstanceConfig.IR_COMPILER_DEBUG) {
            LOG.info("################## " + message + "##################");
            runCompilerPass(new IRPrinter());        
        }
    }

    public String toStringInstrs() {
        return "";
    }

    public String toStringVariables() {
        return "";
    }

    /* Record a begin block -- not all scope implementations can handle them */
    public void recordBeginBlock(IRClosure beginBlockClosure) {
        throw new RuntimeException("BEGIN blocks cannot be added to: " + this.getClass().getName());
    }

    /* Record an end block -- not all scope implementations can handle them */
    public void recordEndBlock(IRClosure endBlockClosure) {
        throw new RuntimeException("END blocks cannot be added to: " + this.getClass().getName());
    }
}

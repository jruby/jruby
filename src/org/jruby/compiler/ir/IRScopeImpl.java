package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.PutConstInstr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.ClassMetaObject;
import org.jruby.compiler.ir.operands.ModuleMetaObject;
import org.jruby.compiler.ir.operands.TemporaryClosureVariable;
import org.jruby.compiler.ir.operands.TemporaryVariable;
import org.jruby.compiler.ir.operands.RenamedVariable;
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
    Operand container;       // Parent container for this context
    IRScope lexicalParent;  // Lexical parent scope

    private String name;

    // ENEBO: These collections are initliazed on construction, but the rest
    //   are init()'d.  This can't be right can it?  They are also final...
    
    // Modules, classes, and methods defined in this LEXICAL scope. In many
    // cases, the lexical scoping and class/method hierarchies are the same.
    final public List<IRModule> modules = new ArrayList<IRModule>();
    final public List<IRClass> classes = new ArrayList<IRClass>();
    // oldName -> newName for methods
    private Map<String, String> aliases = new HashMap<String, String>();

    // ENEBO: This is also only for lexical score too right?
    private Map<String, Operand> contants = new HashMap<String, Operand>();

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

    // Returns the containing scope!
    public Operand getContainer() {
        return container;
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

    public void addModule(IRModule m) {
        setConstantValue(m.getName(), new ModuleMetaObject(m));
        modules.add(m);
    }

    public void addClass(IRClass c) {
        setConstantValue(c.getName(), new ClassMetaObject(c));
        classes.add(c);
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

    // Sometimes the value can be retrieved at "compile time".  If we succeed, nothing like it!
    // We might not .. for the following reasons:
    // 1. The constant is missing,
    // 2. The reference is a forward-reference,
    // 3. The constant's value is only known at run-time on first-access (but, this is runtime, isn't it??)
    // 4. Our compiler isn't able to right away infer that this is a constant.
    //
    // SSS FIXME:
    // 1. The operand can be a literal array, range, or hash -- hence Operand
    //    because Array, Range, and Hash derive from Operand and not Constant ...
    //    Is there a way to fix this impedance mismatch?
    // 2. It should be possible to handle the forward-reference case by creating a new
    //    ForwardReference operand and then inform the scope of the forward reference
    //    which the scope can fix up when the reference gets defined.  At code-gen time,
    //    if the reference is unresolved, when a value is retrieved for the forward-ref
    //    and we get a null, we can throw a ConstMissing exception!  Not sure!
    //
    public Operand getConstantValue(String constRef) {
//        System.out.println("Looking in " + this + " for constant: " + constRef);
        Operand cv = contants.get(constRef);
        Operand p = container;
        // SSS FIXME: Traverse up the scope hierarchy to find the constant as long as the container is a static scope
        if ((cv == null) && (p != null) && (p instanceof MetaObject)) {
            // Can be null for IR_Script meta objects
            if (((MetaObject) p).scope == null) {
//                System.out.println("Looking for core class: " + constRef);
                IRClass coreClass = IRModule.getCoreClass(constRef);

                return coreClass != null ? new ClassMetaObject(coreClass) : null;
            }
            cv = ((MetaObject) p).scope.getConstantValue(constRef);
        }

        return cv;
    }

    public void setConstantValue(String constRef, Operand val) {
        if (val.isConstant()) contants.put(constRef, val);

        if (this instanceof IRModule) {
            ((IRModule) this).getRootMethod().addInstr(new PutConstInstr(this, constRef, val));
        }
    }

    public Map getConstants() {
        return Collections.unmodifiableMap(contants);
    }

    @Override
    public String toString() {
        return getScopeName() + " " + getName() +
                (contants.isEmpty() ? "" : "\n  constants: " + contants);
    }

    protected void runCompilerPassOnNestedScopes(CompilerPass p) {
        for (IRScope m : modules) {
            m.runCompilerPass(p);
        }

        for (IRScope c : classes) {
            c.runCompilerPass(p);
        }
    }

    public void runCompilerPass(CompilerPass p) {
        boolean isPreOrder = p.isPreOrder();

        if (isPreOrder) p.run(this);

        runCompilerPassOnNestedScopes(p);

        if (!isPreOrder) p.run(this);
    }

    public String toStringInstrs() {
        return "";
    }

    public String toStringVariables() {
        return "";
    }
}

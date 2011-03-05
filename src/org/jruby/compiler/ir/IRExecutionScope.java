package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveClosureInstr;
import org.jruby.compiler.ir.instructions.RubyInternalCallInstr;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.parser.StaticScope;

/* IR_Method and IR_Closure -- basically scopes that represent execution contexts.
 * This is just an abstraction over methods and closures */
public abstract class IRExecutionScope extends IRScopeImpl {
    private List<Instr>   instructions;   // List of IR instructions for this method
    private CFG              cfg;      // Control flow graph for this scope
    private List<IRClosure> closures; // List of (nested) closures in this scope

    /* *****************************************************************************************************
     * Does this execution scope (applicable only to methods) receive a block and use it in such a way that
     * all of the caller's local variables need to be materialized into a heap binding?
     * Ex: 
     *    def foo(&b)
     *     eval 'puts a', b
     *    end
     *  
     *    def bar
     *      a = 1
     *      foo {} # prints out '1'
     *    end
     *
     * Here, 'foo' can access all of bar's variables because it captures the caller's closure.
     *
     * There are 2 scenarios when this can happen (even this is conservative -- but, good enough for now)
     * 1. This method receives an explicit block argument (in this case, the block can be stored, passed around,
     *    eval'ed against, called, etc.).  
     *    CAVEAT: This is conservative ... it may not actually be stored & passed around, evaled, called, ...
     * 2. This method has a 'super' call (ZSuper AST node -- RUBY_INTERNALS_CALL_Instr(MethAddr.ZSUPER, ..) IR instr)
     *    In this case, the parent (in the inheritance hierarchy) can access the block and store it, etc.  So, in reality,
     *    rather than assume that the parent will always do this, we can query the parent, if we can precisely identify
     *    the parent method (which in the face of Ruby's dynamic hierarchy, we cannot).  So, be pessimistic.
     *
     * This logic was extracted from an email thread on the JRuby mailing list -- Yehuda Katz & Charles Nutter
     * contributed this analysis above.
     * ********************************************************************************************************/
    private boolean canCaptureCallersBinding;

    /* ****************************************************************************
     * Does this scope define code, i.e. does it (or anybody in the downward call chain)
     * do class_eval, module_eval? In the absence of any other information, we default
     * to yes -- which basically leads to pessimistic but safe optimizations.  But, for
     * library and internal methods, this might be false.
     * **************************************************************************** */
    private boolean canModifyCode;

    /* ****************************************************************************
     * Does this scope (if a closure, applies to the nearest method ancestor) require a binding to be materialized?
     * Yes if any of the following holds true:
     * - calls 'Proc.new'
     * - calls 'eval'
     * - calls 'call' (could be a call on a stored block which could be local!)
     * - calls 'send' and we cannot resolve the message (method name) that is being sent!
     * - calls methods that can access the caller's binding
     * - calls a method which we cannot resolve now!
     * - has a call whose closure requires a binding
     * **************************************************************************** */
    private boolean requiresBinding;

    // NOTE: Since we are processing ASTs, loop bodies are processed in depth-first manner
    // with outer loops encountered before inner loops, and inner loops finished before outer ones.
    //
    // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
    // This lets us implement next/redo/break/retry easily for the non-closure cases
    private Stack<IRLoop> loopStack;

    protected int requiredArgs = 0;
    protected int optionalArgs = 0;
    protected int restArg = -1;

    private void init() {
        instructions = new ArrayList<Instr>();
        closures = new ArrayList<IRClosure>();
        loopStack = new Stack<IRLoop>();

        // All flags are true by default!
        canModifyCode = true;
        canCaptureCallersBinding = true;
        requiresBinding = true;
    }

    public IRExecutionScope(IRScope lexicalParent, Operand container, String name, StaticScope staticScope) {
        super(lexicalParent, container, name, staticScope);
        init();
    }

    public void addClosure(IRClosure c) {
        closures.add(c);
    }

    @Override
    public void addInstr(Instr i) {
        instructions.add(i);
    }

    public void startLoop(IRLoop l) {
        loopStack.push(l);
    }

    public void endLoop(IRLoop l) {
        loopStack.pop(); /* SSS FIXME: Do we need to check if l is same as whatever popped? */
    }

    public IRLoop getCurrentLoop() {
        return loopStack.isEmpty() ? null : loopStack.peek();
    }

    public List<IRClosure> getClosures() {
        return closures;
    }

    // SSS FIXME: Deprecated!  Going forward, all instructions should come from the CFG
    @Override
    public List<Instr> getInstrs() {
        return instructions;
    }

    public IRMethod getClosestMethodAncestor() {
        IRExecutionScope s = this;
        while (!(s instanceof IRMethod)) {
            s = (IRExecutionScope)s.getLexicalParent();
        }

        return (IRMethod) s;
    }

    public void setCodeModificationFlag(boolean f) { 
        canModifyCode = f;
    }

    public boolean modifiesCode() { 
        return canModifyCode;
    }

    public boolean requiresBinding() {
        return requiresBinding;
    }

    public boolean canCaptureCallersBinding() {
        return canCaptureCallersBinding;
    }

    public CFG buildCFG() {
        cfg = new CFG(this);
        cfg.build(instructions);
        return cfg;
    }

    // Get the control flow graph for this scope
    public CFG getCFG() {
        return cfg;
    }

    // Nothing to do -- every compiler pass decides whether to
    // run it on nested closures or not.
    @Override
    public void runCompilerPassOnNestedScopes(CompilerPass p) { }

    public void computeExecutionScopeFlags() {
        // init
        canModifyCode = true;
        canCaptureCallersBinding = false;
        requiresBinding = false;

        // recompute flags -- we could be calling this method different times
        // definitely once after ir generation and local optimizations propagates constants locally
        // but potentially at a later time after doing ssa generation and constant propagation
        boolean receivesClosureArg = false;
        for (Instr i: getInstrs()) {
            if (i instanceof ReceiveClosureInstr)
                receivesClosureArg = true;

            // SSS FIXME: Should we build a ZSUPER IR Instr rather than have this code here?
            if ((i instanceof RubyInternalCallInstr) && (((CallInstr) i).getMethodAddr() == MethAddr.ZSUPER))
                canCaptureCallersBinding = true;

            if (i instanceof CallInstr) {
                CallInstr call = (CallInstr) i;
                if (call.requiresBinding())
                    requiresBinding = true;

                // If this method receives a closure arg, and this call is an eval that has more than 1 argument,
                // it could be using the closure as a binding -- which means it could be using pretty much any
                // variable from the caller's binding!
                if (receivesClosureArg && call.canBeEval() && (call.getCallArgs().length > 1))
                    canCaptureCallersBinding = true;
            }
        }
    }

    @Override
    public String toStringInstrs() {
        StringBuilder b = new StringBuilder();

        int i = 0;
        for (Instr instr : instructions) {
            if (i > 0) b.append("\n");
            
            b.append("  ").append(i).append('\t').append(instr);
            
            i++;
        }

        if (!closures.isEmpty()) {
            b.append("\n\n------ Closures encountered in this scope ------\n");
            for (IRClosure c: closures)
                b.append(c.toStringBody());
            b.append("------------------------------------------------\n");
        }

        return b.toString();
    }

    @Override
    public String toStringVariables() {
        Map<Variable, Integer> ends = new HashMap<Variable, Integer>();
        Map<Variable, Integer> starts = new HashMap<Variable, Integer>();
        SortedSet<Variable> variables = new TreeSet<Variable>();
        
        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instr instr = instructions.get(i);
            Variable var = instr.result;

            if (var != null) {
                variables.add(var);
                starts.put(var, i);
            }

            for (Operand operand : instr.getOperands()) {
                if (operand != null && operand instanceof Variable && ends.get((Variable)operand) == null) {
                    ends.put((Variable)operand, i);
                    variables.add((Variable)operand);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Variable var : variables) {
            Integer end = ends.get(var);
            if (end != null) { // Variable is actually used somewhere and not dead
                if (i > 0) sb.append("\n");
                i++;
                sb.append("    " + var + ": " + starts.get(var) + "-" + end);
            }
        }

        return sb.toString();
    }

    @Interp
    public Iterator<LocalVariable> getLiveLocalVariables() {
        Map<LocalVariable, Integer> ends = new HashMap<LocalVariable, Integer>();
        Map<LocalVariable, Integer> starts = new HashMap<LocalVariable, Integer>();
        Set<LocalVariable> variables = new TreeSet<LocalVariable>();

        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instr instr = instructions.get(i);

            // TODO: Instruction encode whether arguments are optional/required/block
            // TODO: PErhaps this should be part of allocate and not have a generic
            //    getLiveLocalVariables...perhaps we just need this to setup static scope

            Variable variable = instr.result;

            if (variable != null && variable instanceof LocalVariable) {
                variables.add((LocalVariable) variable);
                starts.put((LocalVariable) variable, i);
            } 

            for (Operand operand : instr.getOperands()) {
                if (!(operand instanceof LocalVariable)) continue;

                variable = (LocalVariable) operand;

                if (ends.get((LocalVariable) variable) == null) {
                    ends.put((LocalVariable) variable, i);
                    variables.add((LocalVariable) variable);
                }
            }
        }

        return variables.iterator();
    }

    /**
     * Create and (re)assign a static scope.  In general local variables should
     * never change even if we optimize more, but I was not positive so I am
     * pretending this can change over time.  The obvious secondary benefit
     * to storing this on execution scope is we can grab it when we allocate
     * static scopes for all closures.
     *
     * Note: We are missing a distinct life-cycle point to run methods like this
     * since this method can be modified at any point.  Not being fully set up
     * is less of an issue since we are calling this when we construct a live
     * runtime version of the method/closure etc, but for profiled optimizations
     * this is less clear if/when we can run this. <-- Assumes this ever needs
     * changing.
     *
     * @param parent scope should be non-null for all closures and null for methods
     */
    @Interp
    public StaticScope allocateStaticScope(StaticScope parent) {
        Iterator<LocalVariable> variables = getLiveLocalVariables();
        StaticScope scope = constructStaticScope(parent);

        while (variables.hasNext()) {
            LocalVariable variable = variables.next();
            int destination = scope.addVariable(variable.getName());
            System.out.println("Allocating " + variable + " to " + destination);

                    // Ick: Same Variable objects are not used for all references to the same variable.  S
                    // o setting destination on one will not set them on all
            variable.setLocation(destination);
        }

        return scope;
    }

    @Interp
    public void calculateParameterCounts() {
        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instr instr = instructions.get(i);
        }
    }

    /**
     * Closures and Methods have different static scopes.  This returns the
     * correct instance.
     *
     * @param parent scope should be non-null for all closures and null for methods
     * @return a newly allocated static scope
     */
    @Interp
    protected abstract StaticScope constructStaticScope(StaticScope parent);

    // ENEBO: Can this always be the same variable?  Then SELF comparison could compare against this?
    public Variable getSelf() {
        return getLocalVariable("%self");
    }

    public LocalVariable getLocalVariable(String name) {
        return getClosestMethodAncestor().getLocalVariable(name);
    }

    public int getLocalVariablesCount() {
        return getClosestMethodAncestor().getLocalVariablesCount();
    }
}

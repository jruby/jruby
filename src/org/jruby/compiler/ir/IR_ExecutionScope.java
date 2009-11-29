package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jruby.runtime.Frame;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.RECV_CLOSURE_Instr;
import org.jruby.compiler.ir.instructions.RUBY_INTERNALS_CALL_Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

/* IR_Method and IR_Closure -- basically scopes that represent execution contexts.
 * This is just an abstraction over methods and closures */
public abstract class IR_ExecutionScope extends IR_ScopeImpl
{
    private Frame            _frame;    // Heap frame for this execution scope -- allocated on demand.
    private List<IR_Instr>   _instrs;   // List of IR instructions for this method
    private CFG              _cfg;      // Control flow graph for this scope
    private List<IR_Closure> _closures; // List of (nested) closures in this scope

    /* *****************************************************************************************************
     * Does this execution scope (applicable only to methods) receive a block and use it in such a way that
     * all of the caller's local variables need to be stored in a heap frame?
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
    private boolean _canCaptureCallersFrame;

    /* ****************************************************************************
     * Does this scope define code, i.e. does it (or anybody in the downward call chain)
     * do class_eval, module_eval? In the absence of any other information, we default
     * to yes -- which basically leads to pessimistic but safe optimizations.  But, for
     * library and internal methods, this might be false.
     * **************************************************************************** */
    private boolean _canModifyCode;

    /* ****************************************************************************
     * Does this scope (if a closure, applies to the nearest method ancestor) require a heap frame?
     * Yes if any of the following holds true:
     * - calls 'Proc.new'
     * - calls 'eval'
     * - calls 'call' (could be a call on a stored block which could be local!)
     * - calls 'send' and we cannot resolve the message (method name) that is being sent!
     * - calls methods that can access the callers heap frame
     * - calls a method which we cannot resolve now!
     * - has a call whose closure requires a heap frame
     * **************************************************************************** */
    private boolean _requiresFrame;

    // NOTE: Since we are processing ASTs, loop bodies are processed in depth-first manner
    // with outer loops encountered before inner loops, and inner loops finished before outer ones.
    //
    // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
    // This lets us implement next/redo/break/retry easily for the non-closure cases
    private Stack<IR_Loop> _loopStack;

    private void init() {
        _instrs = new ArrayList<IR_Instr>();
        _closures = new ArrayList<IR_Closure>();
        _loopStack = new Stack<IR_Loop>();

        // All flags are true by default!
        _canModifyCode = true;
        _canCaptureCallersFrame = true;
        _requiresFrame = true;
    }

    public IR_ExecutionScope(IR_Scope parent, IR_Scope lexicalParent) {
        super(parent, lexicalParent);
        init();
    }

    public IR_ExecutionScope(Operand parent, IR_Scope lexicalParent) {
        super(parent, lexicalParent);
        init();
    }

    public void addClosure(IR_Closure c) {
        _closures.add(c);
    }

    public void addInstr(IR_Instr i) { 
        _instrs.add(i); 
    }

    public void startLoop(IR_Loop l) { 
        _loopStack.push(l);
    }

    public void endLoop(IR_Loop l) { 
        _loopStack.pop(); /* SSS FIXME: Do we need to check if l is same as whatever popped? */
    }

    public IR_Loop getCurrentLoop() { 
        return _loopStack.isEmpty() ? null : _loopStack.peek();
    }

    public List<IR_Closure> getClosures() { 
        return _closures;
    }

    // SSS FIXME: Deprecated!  Going forward, all instructions should come from the CFG
    public List<IR_Instr> getInstrs() { 
        return _instrs;
    }

    public void setCodeModificationFlag(boolean f) { 
        _canModifyCode = f;
    }

    public boolean modifiesCode() { 
        return _canModifyCode; 
    }

    public boolean requiresFrame() {
        return _requiresFrame;
    }

    public boolean canCaptureCallersFrame() {
        return _canCaptureCallersFrame;
    }

    public CFG buildCFG() {
        _cfg = new CFG(this);
        _cfg.build(_instrs);
        return _cfg;
    }

    // Get the control flow graph for this scope
    public CFG getCFG() {
        return _cfg;
    }

/**
    public void runCompilerPass(CompilerPass p) {
        boolean isPreOrder =  p.isPreOrder();
        if (isPreOrder)
            p.run(this);

        runCompilerPassOnNestedScopes(p);
        if (!_closures.isEmpty())
            for (IR_Closure c: _closures)
                c.runCompilerPass(p);

        if (!isPreOrder)
            p.run(this);
    }
**/

    public void computeExecutionScopeFlags() {
        // init
        _canModifyCode = true;
        _canCaptureCallersFrame = false;
        _requiresFrame = false;

        // recompute flags -- we could be calling this method different times
        // definitely once after ir generation and local optimizations propagates constants locally
        // but potentially at a later time after doing ssa generation and constant propagation
        boolean receivesClosureArg = false;
        for (IR_Instr i: getInstrs()) {
            if (i instanceof RECV_CLOSURE_Instr)
                receivesClosureArg = true;

            // SSS FIXME: Should we build a ZSUPER IR Instr rather than have this code here?
            if ((i instanceof RUBY_INTERNALS_CALL_Instr) && (((CALL_Instr)i).getMethodAddr() == MethAddr.ZSUPER))
                _canCaptureCallersFrame = true;

            if (i instanceof CALL_Instr) {
                CALL_Instr call = (CALL_Instr)i;
                if (call.requiresFrame())
                    _requiresFrame = true;

                // If this method receives a closure arg, and this call is an eval that has more than 1 argument,
                // it could be using the closure as a binding -- which means it could be using pretty much any
                // variable from the caller's frame!
                if (receivesClosureArg && call.canBeEval() && (call.getNumArgs() > 1))
                    _canCaptureCallersFrame = true;
            }
        }
    }

    public String toStringInstrs() {
        StringBuilder b = new StringBuilder();

        int i = 0;
        for (IR_Instr instr : _instrs) {
            if (i > 0) b.append("\n");
            b.append("  ").append(i).append('\t');
            if (instr.isDead())
                b.append("[DEAD]");
            b.append(instr);
            i++;
        }

        if (!_closures.isEmpty()) {
            b.append("\n\n------ Closures encountered in this scope ------\n");
            for (IR_Closure c: _closures)
                b.append(c.toStringBody());
            b.append("------------------------------------------------\n");
        }

        return b.toString();
    }

    public String toStringVariables() {
        StringBuilder sb = new StringBuilder();
        Map<Variable, Integer> ends = new HashMap<Variable, Integer>();
        Map<Variable, Integer> starts = new HashMap<Variable, Integer>();
        SortedSet<Variable> variables = new TreeSet<Variable>();
        
        for (int i = _instrs.size() - 1; i >= 0; i--) {
            IR_Instr instr = _instrs.get(i);
            Variable var = instr._result;

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

        int i = 0;
        for (Variable var : variables) {
            Integer end = ends.get(var);
            if (end == null) {
                // variable is never read, variable is never live
            } else {
                if (i > 0) sb.append("\n");
                i++;
                sb.append("    " + var + ": " + starts.get(var) + "-" + end);
            }
        }

        return sb.toString();
    }
}

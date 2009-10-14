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
import org.jruby.compiler.ir.operands.Operand;
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

        // NOTE: Since we are processing ASTs, loop bodies are processed in depth-first manner
        // with outer loops encountered before inner loops, and inner loops finished before outer ones.
        //
        // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
        // This lets us implement next/redo/break/retry easily for the non-closure cases
    private Stack<IR_Loop> _loopStack;

    private void init()
    {
        _instrs = new ArrayList<IR_Instr>();
        _closures = new ArrayList<IR_Closure>();
        _loopStack = new Stack<IR_Loop>();
    }

    public IR_ExecutionScope(IR_Scope parent, IR_Scope lexicalParent)
    {
        super(parent, lexicalParent);
        init();
    }

    public IR_ExecutionScope(Operand parent, IR_Scope lexicalParent)
    {
        super(parent, lexicalParent);
        init();
    }

    public void addClosure(IR_Closure c)
    {
        _closures.add(c);
    }

    public void addInstr(IR_Instr i)
    { 
        _instrs.add(i); 
    }

    public void startLoop(IR_Loop l) { _loopStack.push(l); }

    public void endLoop(IR_Loop l) { _loopStack.pop(); /* SSS FIXME: Do we need to check if l is same as whatever popped? */ }

    public IR_Loop getCurrentLoop() { return _loopStack.isEmpty() ? null : _loopStack.peek(); }

    public List<IR_Closure> getClosures()
    {
        return _closures;
    }

    // SSS FIXME: Deprecated!  Going forward, all instructions should come from the CFG
    public List<IR_Instr> getInstrs() { return _instrs; }

    public CFG buildCFG()
    {
        _cfg = new CFG(this);
        _cfg.build(_instrs);
        return _cfg;
    }

    // Get the control flow graph for this scope
    public CFG getCFG()
    {
        return _cfg;
    }

/**
    public void runCompilerPass(CompilerPass p)
    {
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

package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accumulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
// Their parents are always execution scopes.
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.ClosureLocalVariable;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.TemporaryClosureVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveClosureArgInstr;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;

public class IRClosure extends IRExecutionScope {
    public final Label startLabel; // Label for the start of the closure (used to implement redo)
    public final Label endLabel;   // Label for the end of the closure (used to implement retry)
    public final int closureId;    // Unique id for this closure within the nearest ancestor method.

    private int nestingDepth;      // How many nesting levels within a method is this closure nested in?

    private BlockBody body;

    // Oy, I have a headache!
    // for-loop body closures are special in that they dont really define a new variable scope.
    // They just silently reuse the parent scope.  This changes how variables are allocated (see IRMethod.java).
    private boolean isForLoopBody;

    // Has this closure been inlined into a method? If yes, its independent existence has come to an end
    // because it has very likely been integrated into another scope and we should no longer do anything
    // with the instructions as an independent closure scope.
    private boolean hasBeenInlined;     

    // Block parameters
    private List<Operand> blockArgs;

    public IRClosure(IRScope lexicalParent, boolean isForLoopBody, StaticScope staticScope, Arity arity, int argumentType) {
        this(lexicalParent, staticScope, isForLoopBody ? "_FOR_LOOP_" : "_CLOSURE_");
        this.isForLoopBody = isForLoopBody;
        this.hasBeenInlined = false;
        this.blockArgs = new ArrayList<Operand>();
        if (!IRBuilder.inIRGenOnlyMode()) {
            this.body = new InterpretedIRBlockBody(this, arity, argumentType);
            if ((staticScope != null) && !isForLoopBody) ((IRStaticScope)staticScope).setIRScope(this);
        } else {
            this.body = null;
        }
    }

    // Used by IREvalScript
    protected IRClosure(IRScope lexicalParent, StaticScope staticScope, String prefix) {
        super(lexicalParent, null, staticScope);
        this.isForLoopBody = false;
        this.startLabel = getNewLabel(prefix + "START");
        this.endLabel = getNewLabel(prefix + "END");
        this.closureId = lexicalParent.getNextClosureId();
        setName(prefix + closureId);
        this.body = null;

        // set nesting depth
        int n = 0;
        IRScope s = this;
        while (s instanceof IRClosure) {
            s = ((IRClosure)s).getLexicalParent();
            n++;
        }
        this.nestingDepth = n;
    }

    @Override
    public int getNextClosureId() {
        return getLexicalParent().getNextClosureId();
    }

    @Override
    public int getTemporaryVariableSize() {
        return getPrefixCountSize("%cl_" + closureId);
    }

    @Override
    public Variable getNewTemporaryVariable() {
        return new TemporaryClosureVariable(closureId, allocateNextPrefixedName("%cl_" + closureId));
    }

    @Override
    public Label getNewLabel() {
        return getNewLabel("CL" + closureId + "_LBL");
    }

    public String getScopeName() {
        return "Closure";
    }

    public boolean isForLoopBody() {
        return isForLoopBody;
    }

    @Override
    public void addInstr(Instr i) {
        // Accumulate block arguments
        if (i instanceof ReceiveClosureArgInstr) blockArgs.add(((ReceiveClosureArgInstr) i).isRestOfArgArray() ? new Splat(i.getResult()) : i.getResult());

        super.addInstr(i);
    }

    public Operand[] getBlockArgs() { 
        return blockArgs.toArray(new Operand[blockArgs.size()]);
    }

    public String toStringBody() {
        StringBuilder buf = new StringBuilder();
        buf.append(getName()).append(" = { \n");

        if (getCFG() != null) {
            buf.append("\nCFG:\n").append(getCFG());
        } else {
            buf.append(toStringInstrs());
        }
        buf.append("\n}\n\n");
        return buf.toString();
    }

    @Override
    protected StaticScope constructStaticScope(StaticScope parent) {
        return IRStaticScopeFactory.newIRBlockScope(parent);
    }

    public BlockBody getBlockBody() {
        return body;
    }

    public void markInlined() {
        this.hasBeenInlined = true;
    }

    public boolean hasBeenInlined() {
        return this.hasBeenInlined;
    }

    public LocalVariable findExistingLocalVariable(String name) {
        LocalVariable lvar = localVars.getVariable(name);
        if (lvar != null) return lvar;
        else return ((IRExecutionScope)getLexicalParent()).findExistingLocalVariable(name);
    }

    public LocalVariable getNewLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = new ClosureLocalVariable(this, name, 0, localVars.nextSlot);
        localVars.putVariable(name, lvar);
        return lvar;
    }

    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        if (isForLoopBody) return getLexicalParent().getLocalVariable(name, scopeDepth);

        LocalVariable lvar = findExistingLocalVariable(name);
        if (lvar == null) {
            lvar = getNewLocalVariable(name, scopeDepth);
        } else if (lvar.getScopeDepth() != scopeDepth) {
            // Create a copy of the variable usable at a different scope depth
            lvar = lvar.cloneForDepth(scopeDepth);
        }

        return lvar;
    }

    public int getNestingDepth() {
        return nestingDepth;
    }

    public LocalVariable getImplicitBlockArg() {
        return getLocalVariable("%block", getNestingDepth());
    }
}

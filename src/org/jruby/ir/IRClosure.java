package org.jruby.ir;

import java.util.List;
import java.util.ArrayList;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accumulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
// Their parents are always execution scopes.
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.TemporaryClosureVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.instructions.ReceiveRestArgBase;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;
import org.jruby.runtime.InterpretedIRBlockBody19;

public class IRClosure extends IRScope {
    public final Label startLabel; // Label for the start of the closure (used to implement redo)
    public final Label endLabel;   // Label for the end of the closure (used to implement retry)
    public final int closureId;    // Unique id for this closure within the nearest ancestor method.

    private int nestingDepth;      // How many nesting levels within a method is this closure nested in?

    private BlockBody body;

    // for-loop body closures are special in that they dont really define a new variable scope.
    // They just silently reuse the parent scope.  This changes how variables are allocated (see IRMethod.java).
    private boolean isForLoopBody;

    // Block parameters
    private List<Operand> blockArgs;

    /** The parameter names, for Proc#parameters */
    private String[] parameterList;

    /** Used by cloning code */
    private IRClosure(IRClosure c, IRScope lexicalParent) {
        super(c, lexicalParent);
        this.closureId = lexicalParent.getNextClosureId();
        setName("_CLOSURE_CLONE_" + closureId);
        this.startLabel = getNewLabel(getName() + "_START");
        this.endLabel = getNewLabel(getName() + "_END");
        this.body = (c.body instanceof InterpretedIRBlockBody19) ? new InterpretedIRBlockBody19(this, c.body.arity(), c.body.getArgumentType())
                                                                 : new InterpretedIRBlockBody(this, c.body.arity(), c.body.getArgumentType());
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, boolean isForLoopBody,
            int lineNumber, StaticScope staticScope, Arity arity, int argumentType, boolean is1_9) {
        this(manager, lexicalParent, lexicalParent.getFileName(), lineNumber, staticScope, isForLoopBody ? "_FOR_LOOP_" : "_CLOSURE_");
        this.isForLoopBody = isForLoopBody;
        this.blockArgs = new ArrayList<Operand>();
        
        if (!IRBuilder.inIRGenOnlyMode()) {
            this.body = is1_9 ? new InterpretedIRBlockBody19(this, arity, argumentType)
                              : new InterpretedIRBlockBody(this, arity, argumentType);
            if ((staticScope != null) && !isForLoopBody) ((IRStaticScope)staticScope).setIRScope(this);
        } else {
            this.body = null;
        }

        // set nesting depth -- after isForLoopBody value is set
        int n = 0;
        IRScope s = this;
        while (s instanceof IRClosure) {
            if (!s.isForLoopBody()) n++;
            s = s.getLexicalParent();
        }
        this.nestingDepth = n;
    }

    // Used by IREvalScript
    protected IRClosure(IRManager manager, IRScope lexicalParent, String fileName, int lineNumber, StaticScope staticScope, String prefix) {
        super(manager, lexicalParent, null, fileName, lineNumber, staticScope);
        
        this.isForLoopBody = false;
        this.startLabel = getNewLabel(prefix + "START");
        this.endLabel = getNewLabel(prefix + "END");
        this.closureId = lexicalParent.getNextClosureId();
        setName(prefix + closureId);
        this.body = null;
        this.parameterList = new String[] {};

        // set nesting depth
        int n = 0;
        IRScope s = this;
        while (s instanceof IRClosure) {
            if (!s.isForLoopBody()) n++;
            s = s.getLexicalParent();
        }
        this.nestingDepth = n;
    }

    public void setParameterList(String[] parameterList) {
        this.parameterList = parameterList;
    }

    public String[] getParameterList() {
        return this.parameterList;
    }

    @Override
    public int getNextClosureId() {
        return getLexicalParent().getNextClosureId();
    }

    @Override
    public LocalVariable getNewFlipStateVariable() {
        throw new RuntimeException("Cannot get flip variables from closures.");
    }

    @Override
    public TemporaryVariable getNewTemporaryVariable() {
        temporaryVariableIndex++;
        return new TemporaryClosureVariable(closureId, temporaryVariableIndex);
    }

    public TemporaryVariable getNewTemporaryVariable(String name) {
        temporaryVariableIndex++;
        return new TemporaryClosureVariable(name, temporaryVariableIndex);
    }    

    @Override
    public Label getNewLabel() {
        return getNewLabel("CL" + closureId + "_LBL");
    }

    public String getScopeName() {
        return "Closure";
    }

    @Override
    public boolean isForLoopBody() {
        return isForLoopBody;
    }

    @Override
    public boolean isTopLocalVariableScope() {
        return false;
    }

    @Override
    public boolean isFlipScope() {
        return false;
    }

    @Override
    public void addInstr(Instr i) {
        // Accumulate block arguments
        if (i instanceof ReceiveRestArgBase) blockArgs.add(new Splat(((ReceiveRestArgBase)i).getResult()));
        else if (i instanceof ReceiveArgBase) blockArgs.add(((ReceiveArgBase) i).getResult());

        super.addInstr(i);
    }

    public Operand[] getBlockArgs() { 
        return blockArgs.toArray(new Operand[blockArgs.size()]);
    }

    public String toStringBody() {
        StringBuilder buf = new StringBuilder();
        buf.append(getName()).append(" = { \n");

        CFG c = getCFG();
        if (c != null) {
            buf.append("\nCFG:\n").append(c.toStringGraph()).append("\nInstructions:\n").append(c.toStringInstrs());
        } else {
            buf.append(toStringInstrs());
        }
        buf.append("\n}\n\n");
        return buf.toString();
    }

    public BlockBody getBlockBody() {
        return body;
    }

    @Override
    public LocalVariable findExistingLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = localVars.getVariable(name);
        if (lvar != null) return lvar;

        int newDepth = isForLoopBody ? scopeDepth : scopeDepth - 1;

        return newDepth >= 0 ? getLexicalParent().findExistingLocalVariable(name, newDepth) : null;
    }

    public LocalVariable getNewLocalVariable(String name, int depth) {
        if (isForLoopBody) return getLexicalParent().getNewLocalVariable(name, depth);

        if (depth == 0) {
            LocalVariable lvar = new ClosureLocalVariable(this, name, 0, localVars.nextSlot);
            localVars.putVariable(name, lvar);
            return lvar;
        } else {
            return getLexicalParent().getNewLocalVariable(name, depth-1);
        }
    }

    @Override
    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        if (isForLoopBody) return getLexicalParent().getLocalVariable(name, scopeDepth);

        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, scopeDepth);
        // Create a copy of the variable usable at the right depth
        if (lvar.getScopeDepth() != scopeDepth) lvar = lvar.cloneForDepth(scopeDepth);

        return lvar;
    }
    
    public int getNestingDepth() {
        return nestingDepth;
    }

    public LocalVariable getImplicitBlockArg() {
        // SSS: FIXME: Ugly! We cannot use 'getLocalVariable(Variable.BLOCK, getNestingDepth())' because
        // of scenario 3. below.  Can we clean up this code?
        //
        // 1. If the variable has previously been defined, return a copy usable at the closure's nesting depth.
        // 2. If not, and if the closure is ultimately nested within a method, build a local variable that will 
        //    be defined in that method.
        // 3. If not, and if the closure is not nested within a method, the closure can never receive a block.
        //    So, we could return 'null', but it creates problems for IR generation.  So, for this scenario,
        //    we simply create a dummy var at depth 0 (meaning, it is local to the closure itself) and return it.
        LocalVariable blockVar = findExistingLocalVariable(Variable.BLOCK, getNestingDepth());
        if (blockVar != null) {
            // Create a copy of the variable usable at the right depth
            if (blockVar.getScopeDepth() != getNestingDepth()) blockVar = blockVar.cloneForDepth(getNestingDepth());
        } else {
            IRScope s = this;
            while (s instanceof IRClosure) s = s.getLexicalParent();

            if (s instanceof IRMethod) {
                blockVar = s.getNewLocalVariable(Variable.BLOCK, 0);
                // Create a copy of the variable usable at the right depth
                if (getNestingDepth() != 0) blockVar = blockVar.cloneForDepth(getNestingDepth());
            } else {
                // Dummy var
                blockVar = getNewLocalVariable(Variable.BLOCK, 0);
            }
        }
        return blockVar;
    }

    public IRClosure cloneForClonedInstr(InlinerInfo ii) {
        IRClosure clonedClosure = new IRClosure(this, ii.getNewLexicalParentForClosure());
        clonedClosure.isForLoopBody = this.isForLoopBody;
        clonedClosure.nestingDepth  = this.nestingDepth;
        clonedClosure.parameterList = this.parameterList;

        // Create a new inliner info object
        ii = ii.cloneForCloningClosure(clonedClosure);

        // clone the cfg, and all instructions
        clonedClosure.setCFG(getCFG().cloneForCloningClosure(clonedClosure, ii));

        return clonedClosure;
    }
}

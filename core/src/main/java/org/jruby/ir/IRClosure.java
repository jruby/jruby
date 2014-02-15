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
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryClosureVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.instructions.ReceiveExceptionBase;
import org.jruby.ir.instructions.ReceiveJRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.operands.TemporaryVariableType;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;
import org.objectweb.asm.Handle;

public class IRClosure extends IRScope {
    public final Label startLabel; // Label for the start of the closure (used to implement redo)
    public final Label endLabel;   // Label for the end of the closure (used to implement retry)
    public final int closureId;    // Unique id for this closure within the nearest ancestor method.

    private int nestingDepth;      // How many nesting levels within a method is this closure nested in?

    private BlockBody body;

    private boolean isBeginEndBlock;

    // Block parameters
    private List<Operand> blockArgs;

    /** The parameter names, for Proc#parameters */
    private String[] parameterList;
    private Arity arity;
    private int argumentType;
    public boolean addedGEBForUncaughtBreaks;
    private Handle handle;

    // Used by IREvalScript as well
    protected IRClosure(IRManager manager, IRScope lexicalParent, String fileName, int lineNumber, StaticScope staticScope, String prefix) {
        super(manager, lexicalParent, null, fileName, lineNumber, staticScope);

        this.startLabel = getNewLabel(prefix + "START");
        this.endLabel = getNewLabel(prefix + "END");
        this.closureId = lexicalParent.getNextClosureId();
        setName(prefix + closureId);
        this.body = null;
        this.parameterList = new String[] {};

        // set nesting depth
        int n = 0;
        IRScope s = this.getLexicalParent();
        while (s instanceof IRClosure) {
            n++;
            s = s.getLexicalParent();
        }
        this.nestingDepth = n;
    }

    /** Used by cloning code */
    protected IRClosure(IRClosure c, IRScope lexicalParent, String prefix) {
        super(c, lexicalParent);
        this.closureId = lexicalParent.getNextClosureId();
        setName(prefix + closureId);
        this.startLabel = getNewLabel(getName() + "_START");
        this.endLabel = getNewLabel(getName() + "_END");
        if (getManager().isDryRun()) {
            this.body = null;
        } else {
            this.body = new InterpretedIRBlockBody(this, c.body.arity(), c.body.getArgumentType());
        }
        this.addedGEBForUncaughtBreaks = false;
        this.blockArgs = new ArrayList<Operand>();
        this.arity = c.arity;
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Arity arity, int argumentType) {
        this(manager, lexicalParent, lineNumber, staticScope, arity, argumentType, "_CLOSURE_");
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Arity arity, int argumentType, String prefix) {
        this(manager, lexicalParent, lexicalParent.getFileName(), lineNumber, staticScope, prefix);
        this.blockArgs = new ArrayList<Operand>();
        this.argumentType = argumentType;
        this.arity = arity;
        lexicalParent.addClosure(this);

        if (getManager().isDryRun()) {
            this.body = null;
        } else {
            this.body = new InterpretedIRBlockBody(this, arity, argumentType);
            if (staticScope != null) {
                staticScope = getStaticScope();
                ((IRStaticScope)staticScope).setIRScope(this);
            }
        }

        this.nestingDepth++;
    }

    public void setBeginEndBlock() {
        this.isBeginEndBlock = true;
    }

    public boolean isBeginEndBlock() {
        return isBeginEndBlock;
    }

    public void setParameterList(String[] parameterList) {
        this.parameterList = parameterList;
        if (!getManager().isDryRun()) {
            ((InterpretedIRBlockBody)this.body).setParameterList(parameterList);
        }
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
    public TemporaryLocalVariable getNewTemporaryVariable() {
        return getNewTemporaryVariable(TemporaryVariableType.CLOSURE);
    }

    @Override
    public TemporaryLocalVariable getNewTemporaryVariable(TemporaryVariableType type) {
        if (type == TemporaryVariableType.CLOSURE) {
            temporaryVariableIndex++;
            return new TemporaryClosureVariable(closureId, temporaryVariableIndex);
        }

        return super.getNewTemporaryVariable(type);
    }

    @Override
    public Label getNewLabel() {
        return getNewLabel("CL" + closureId + "_LBL");
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.CLOSURE;
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
        if (i instanceof ReceiveRestArgInstr) blockArgs.add(new Splat(((ReceiveRestArgInstr)i).getResult()));
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
        LocalVariable lvar = lookupExistingLVar(name);
        if (lvar != null) return lvar;

        int newDepth = scopeDepth - 1;

        return newDepth >= 0 ? getLexicalParent().findExistingLocalVariable(name, newDepth) : null;
    }

    public LocalVariable getNewLocalVariable(String name, int depth) {
        if (depth == 0 && !(this instanceof IRFor)) {
            LocalVariable lvar = new ClosureLocalVariable(this, name, 0, getStaticScope().addVariableThisScope(name));
            localVars.put(name, lvar);
            return lvar;
        } else {
            IRScope s = this;
            int     d = depth;
            do {
                // account for for-loops
                while (s instanceof IRFor) {
                    depth++;
                    s = s.getLexicalParent();
                }

                // walk up
                d--;
                if (d >= 0) s = s.getLexicalParent();
            } while (d >= 0);

            return s.getNewLocalVariable(name, 0).cloneForDepth(depth);
        }
    }

    @Override
    public LocalVariable getLocalVariable(String name, int depth) {
        // AST doesn't seem to be implementing shadowing properly and sometimes
        // has the wrong depths which screws up variable access. So, we implement
        // shadowing here by searching for an existing local var from depth 0 and upwards.
        //
        // Check scope depths for 'a' in the closure in the following snippet:
        //
        //   "a = 1; foo(1) { |(a)| a }"
        //
        // In "(a)", it is 0 (correct), but in the body, it is 1 (incorrect)

        LocalVariable lvar = null;
        IRScope s = this;
        int d = depth;
        do {
            // account for for-loops
            while (s instanceof IRFor) {
                depth++;
                s = s.getLexicalParent();
            }

            // lookup
            lvar = s.lookupExistingLVar(name);

            // walk up
            d--;
            if (d >= 0) s = s.getLexicalParent();
        } while (lvar == null && d >= 0);

        if (lvar == null) {
            // Create a new var at requested/adjusted depth
            lvar = s.getNewLocalVariable(name, 0).cloneForDepth(depth);
        } else {
            // Find # of lexical scopes we walked up to find 'lvar'.
            // We need a copy of 'lvar' usable at that depth
            int lvarDepth = depth - (d + 1);
            if (lvar.getScopeDepth() != lvarDepth) lvar = lvar.cloneForDepth(lvarDepth);
        }

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

    protected IRClosure cloneForInlining(InlinerInfo ii, IRClosure clone) {
        clone.nestingDepth  = this.nestingDepth;
        clone.parameterList = this.parameterList;

        // Create a new inliner info object
        InlinerInfo clonedII = ii.cloneForCloningClosure(clone);

        if (getCFG() != null) {
            // Clone the cfg
            CFG clonedCFG = new CFG(clone);
            clone.setCFG(clonedCFG);
            clonedCFG.cloneForCloningClosure(getCFG(), clone, clonedII);
        } else {
            // Clone the instruction list
            for (Instr i: getInstrs()) {
                Instr clonedInstr = i.cloneForInlining(clonedII);
                if (clonedInstr instanceof CallBase) {
                    CallBase call = (CallBase)clonedInstr;
                    Operand block = call.getClosureArg(null);
                    if (block instanceof WrappedIRClosure) clone.addClosure(((WrappedIRClosure)block).getClosure());
                }
                clone.addInstr(clonedInstr);
            }
        }

        return clone;
    }

    public IRClosure cloneForInlining(InlinerInfo ii) {
        // FIXME: This is buggy! Is this not dependent on clone-mode??
        IRClosure clonedClosure = new IRClosure(this, ii.getNewLexicalParentForClosure(), "_CLOSURE_CLONE_");

        return cloneForInlining(ii, clonedClosure);
    }

    // Add a global-ensure-block to catch uncaught breaks
    // This is usually required only if this closure is being
    // used as a lambda, but it is safe to add this for any closure

    protected boolean addGEBForUncaughtBreaks() {
        // Nothing to do if already done
        if (addedGEBForUncaughtBreaks) {
            return false;
        }

        CFG        cfg = cfg();
        BasicBlock geb = cfg.getGlobalEnsureBB();
        if (geb == null) {
            geb = new BasicBlock(cfg, new Label("_GLOBAL_ENSURE_BLOCK", 0));
            Variable exc = getNewTemporaryVariable();
            geb.addInstr(new ReceiveJRubyExceptionInstr(exc)); // JRuby implementation exception
            // Handle uncaught break and non-local returns using runtime helpers
            Variable ret = getNewTemporaryVariable();
            geb.addInstr(new RuntimeHelperCall(ret, "handleBreakAndReturnsInLambdas", new Operand[]{exc} ));
            geb.addInstr(new ReturnInstr(ret));
            cfg.addGlobalEnsureBB(geb);
        } else {
            // SSS FIXME: Assumptions:
            //
            // First instr is a 'ReceiveExceptionBase'
            // Last instr is a 'ThrowExceptionInstr' -- replaced by handleBreakAndReturnsInLambdas

            List<Instr> instrs = geb.getInstrs();
            Variable exc = ((ReceiveExceptionBase)instrs.get(0)).getResult();
            Variable ret = getNewTemporaryVariable();
            instrs.set(instrs.size()-1, new RuntimeHelperCall(ret, "handleBreakAndReturnsInLambdas", new Operand[]{exc} ));
            geb.addInstr(new ReturnInstr(ret));
        }

        // Update scope
        addedGEBForUncaughtBreaks = true;

        return true;
    }

    @Override
    public void setName(String name) {
        // We can distinguish closures only with parent scope name
        String fullName = getLexicalParent().getName() + name;
        super.setName(fullName);
    }

    public Arity getArity() {
        return arity;
    }

    public int getArgumentType() {
        return argumentType;
    }

    public void setHandle(Handle handle) {
        this.handle = handle;
    }

    public Handle getHandle() {
        return handle;
    }
}

package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import org.jruby.ir.instructions.*;
import org.jruby.ir.interpreter.ClosureInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.IRBlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;
import org.jruby.runtime.Signature;
import org.objectweb.asm.Handle;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accumulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
// Their parents are always execution scopes.

public class IRClosure extends IRScope {
    public final Label startLabel; // Label for the start of the closure (used to implement redo)
    public final Label endLabel;   // Label for the end of the closure (used to implement retry)
    public final int closureId;    // Unique id for this closure within the nearest ancestor method.

    private boolean isBeginEndBlock;

    private Signature signature;

    /** Added for interp/JIT purposes */
    private IRBlockBody body;

    /** Added for JIT purposes */
    private Handle handle;

    // Used by other constructions and by IREvalScript as well
    protected IRClosure(IRManager manager, IRScope lexicalParent, String fileName, int lineNumber, StaticScope staticScope, String prefix) {
        super(manager, lexicalParent, null, fileName, lineNumber, staticScope);

        this.startLabel = getNewLabel(prefix + "START");
        this.endLabel = getNewLabel(prefix + "END");
        this.closureId = lexicalParent.getNextClosureId();
        setName(prefix + closureId);
        this.body = null;
    }

    /** Used by cloning code */
    /* Inlining generates a new name and id and basic cloning will reuse the originals name */
    protected IRClosure(IRClosure c, IRScope lexicalParent, int closureId, String fullName) {
        super(c, lexicalParent);
        this.closureId = closureId;
        super.setName(fullName);
        this.startLabel = getNewLabel(getName() + "_START");
        this.endLabel = getNewLabel(getName() + "_END");
        if (getManager().isDryRun()) {
            this.body = null;
        } else {
            this.body = new InterpretedIRBlockBody(this, c.body.getSignature());
        }

        this.signature = c.signature;
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, "_CLOSURE_");
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, String prefix) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, prefix, false);
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, String prefix, boolean isBeginEndBlock) {
        this(manager, lexicalParent, lexicalParent.getFileName(), lineNumber, staticScope, prefix);
        this.signature = signature;
        lexicalParent.addClosure(this);

        if (getManager().isDryRun()) {
            this.body = null;
        } else {
            this.body = new InterpretedIRBlockBody(this, signature);
            if (staticScope != null && !isBeginEndBlock) {
                staticScope.setIRScope(this);
                staticScope.setScopeType(this.getScopeType());
            }
        }
    }

    @Override
    public InterpreterContext allocateInterpreterContext(List<Instr> instructions) {
        interpreterContext = new ClosureInterpreterContext(this, instructions);

        return interpreterContext;
    }

    public void setBeginEndBlock() {
        this.isBeginEndBlock = true;
    }

    public boolean isBeginEndBlock() {
        return isBeginEndBlock;
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
    public TemporaryLocalVariable createTemporaryVariable() {
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

    public String toStringBody() {
        return new StringBuilder(getName()).append(" = {\n").append(toStringInstrs()).append("\n}\n\n").toString();
    }

    public BlockBody getBlockBody() {
        return body;
    }

    @Override
    protected LocalVariable findExistingLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = lookupExistingLVar(name);
        if (lvar != null) return lvar;

        int newDepth = scopeDepth - 1;

        return newDepth >= 0 ? getLexicalParent().findExistingLocalVariable(name, newDepth) : null;
    }

    public LocalVariable getNewLocalVariable(String name, int depth) {
        if (depth == 0 && !(this instanceof IRFor)) {
            LocalVariable lvar = new ClosureLocalVariable(name, 0, getStaticScope().addVariableThisScope(name));
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

        LocalVariable lvar;
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

    protected IRClosure cloneForInlining(CloneInfo ii, IRClosure clone) {
        // SSS FIXME: This is fragile. Untangle this state.
        // Why is this being copied over to InterpretedIRBlockBody?
        clone.isBeginEndBlock = this.isBeginEndBlock;

        SimpleCloneInfo clonedII = ii.cloneForCloningClosure(clone);

//        if (getCFG() != null) {
//            clone.setCFG(getCFG().clone(clonedII, clone));
//        } else {
        List<Instr> newInstrs = new ArrayList<>(interpreterContext.getInstructions().length);

        for (Instr i: interpreterContext.getInstructions()) {
            newInstrs.add(i.clone(clonedII));
        }

        clone.allocateInterpreterContext(newInstrs);

//        }

        return clone;
    }

    public IRClosure cloneForInlining(CloneInfo ii) {
        IRClosure clonedClosure;
        IRScope lexicalParent = ii.getScope();

        if (ii instanceof SimpleCloneInfo && !((SimpleCloneInfo)ii).isEnsureBlockCloneMode()) {
            clonedClosure = new IRClosure(this, lexicalParent, closureId, getName());
        } else {
            int id = lexicalParent.getNextClosureId();
            String fullName = lexicalParent.getName() + "_CLOSURE_CLONE_" + id;
            clonedClosure = new IRClosure(this, lexicalParent, id, fullName);
        }

        // WrappedIRClosure should always have a single unique IRClosure in them so we should
        // not end up adding n copies of the same closure as distinct clones...
        lexicalParent.addClosure(clonedClosure);

        return cloneForInlining(ii, clonedClosure);
    }

    @Override
    public void setName(String name) {
        // We can distinguish closures only with parent scope name
        super.setName(getLexicalParent().getName() + name);
    }

    public Signature getSignature() {
        return signature;
    }

    public void setHandle(Handle handle) {
        this.handle = handle;
    }

    public Handle getHandle() {
        return handle;
    }
}

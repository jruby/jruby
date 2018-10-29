package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jruby.RubySymbol;
import org.jruby.ast.DefNode;
import org.jruby.ast.IterNode;
import org.jruby.ir.instructions.*;
import org.jruby.ir.interpreter.ClosureInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.IRBlockBody;
import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;
import org.jruby.runtime.Signature;
import org.jruby.util.ByteList;
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

    // We allow closures who happen to be assigned to calls named 'defined_method' to save the original
    // AST so we can attempt to convert those blocks to full methods.
    private IterNode source;

    // Argument description
    protected ArgumentDescriptor[] argDesc = ArgumentDescriptor.EMPTY_ARRAY;

    /** Added for interp/JIT purposes */
    private IRBlockBody body;

    /** Added for JIT purposes */
    private Handle handle;

    // Used by other constructions and by IREvalScript as well
    protected IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, ByteList prefix) {
        super(manager, lexicalParent, null, lineNumber, staticScope);

        this.startLabel = getNewLabel(prefix + "START");
        this.endLabel = getNewLabel(prefix + "END");
        this.closureId = lexicalParent.getNextClosureId();
        ByteList name = prefix.dup();
        name.append(Integer.toString(closureId).getBytes());
        setName(manager.getRuntime().newSymbol(name));
        this.body = null;
    }

    /** Used by cloning code */
    /* Inlining generates a new name and id and basic cloning will reuse the originals name */
    protected IRClosure(IRClosure c, IRScope lexicalParent, int closureId, RubySymbol fullName) {
        super(c, lexicalParent);
        this.closureId = closureId;
        super.setName(fullName);
        this.startLabel = getNewLabel(getId() + "_START");
        this.endLabel = getNewLabel(getId() + "_END");
        if (getManager().isDryRun()) {
            this.body = null;
        } else {
            boolean shouldJit = getManager().getInstanceConfig().getCompileMode().shouldJIT();
            this.body = shouldJit ? new MixedModeIRBlockBody(c, c.getSignature()) : new InterpretedIRBlockBody(c, c.getSignature());
        }

        this.signature = c.signature;
    }

    private static final ByteList CLOSURE = new ByteList(new byte[] {'_', 'C', 'L', 'O', 'S', 'U', 'R', 'E', '_'});

    // Used by persistence.  Knowledge of coverage not needed here since it is already instrumented into the instrs
    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, CLOSURE, false);
    }

    // Used by iter + lambda by IRBuilder
    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, boolean needsCoverage) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, CLOSURE, false, needsCoverage);
    }


    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, ByteList prefix) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, prefix, false);
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, ByteList prefix, boolean isBeginEndBlock) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, prefix, isBeginEndBlock, false);
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope,
                     Signature signature, ByteList prefix, boolean isBeginEndBlock, boolean needsCoverage) {
        this(manager, lexicalParent, lineNumber, staticScope, prefix);
        this.signature = signature;
        lexicalParent.addClosure(this);

        if (getManager().isDryRun()) {
            this.body = null;
        } else {
            boolean shouldJit = manager.getInstanceConfig().getCompileMode().shouldJIT();
            this.body = shouldJit ? new MixedModeIRBlockBody(this, signature) : new InterpretedIRBlockBody(this, signature);
            if (staticScope != null && !isBeginEndBlock) {
                staticScope.setIRScope(this);
                staticScope.setScopeType(this.getScopeType());
            }
        }

        if (needsCoverage) getFlags().add(IRFlags.CODE_COVERAGE);
    }


    @Override
    public InterpreterContext allocateInterpreterContext(List<Instr> instructions) {
        interpreterContext = new ClosureInterpreterContext(this, instructions);

        return interpreterContext;
    }

    @Override
    public InterpreterContext allocateInterpreterContext(Callable<List<Instr>> instructions) {
        try {
            interpreterContext = new ClosureInterpreterContext(this, instructions);
        } catch (Exception e) {
            Helpers.throwException(e);
        }

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
        return new StringBuilder(getId()).append(" = {\n").append(toStringInstrs()).append("\n}\n\n").toString();
    }

    public BlockBody getBlockBody() {
        return body;
    }

    // FIXME: This is too strict.  We can use any closure which does not dip below the define_method closure.  This
    // will deopt any nested block which dips out of itself.
    public boolean isNestedClosuresSafeForMethodConversion() {
        for (IRClosure closure: getClosures()) {
            if (!closure.isNestedClosuresSafeForMethodConversion()) return false;
        }

        return !getFlags().contains(IRFlags.ACCESS_PARENTS_LOCAL_VARIABLES);
    }

    // FIXME: this is needs to be ByteList
    public IRMethod convertToMethod(RubySymbol name) {
        // We want variable scoping to be the same as a method and not see outside itself.
        if (source == null ||
            getFlags().contains(IRFlags.ACCESS_PARENTS_LOCAL_VARIABLES) ||  // Built methods cannot search down past method scope
            getFlags().contains(IRFlags.RECEIVES_CLOSURE_ARG) ||            // we pass in captured block at define_method as block so explicits ones not supported
            !isNestedClosuresSafeForMethodConversion()) {
            source = null;
            return null;
        }

        DefNode def = source;
        source = null;

        // FIXME: This should be bytelist from param vs being made (see above).
        return new IRMethod(getManager(), getLexicalParent(), def, name, true,  getLine(), getStaticScope(), getFlags().contains(IRFlags.CODE_COVERAGE));
    }

    public void setSource(IterNode iter) {
        source = iter;
    }

    @Override
    protected LocalVariable findExistingLocalVariable(RubySymbol name, int scopeDepth) {
        LocalVariable lvar = lookupExistingLVar(name);
        if (lvar != null) return lvar;

        int newDepth = scopeDepth - 1;

        if (newDepth >= 0) {
            lvar = getLexicalParent().findExistingLocalVariable(name, newDepth);

            if (lvar != null) flags.add(IRFlags.ACCESS_PARENTS_LOCAL_VARIABLES);
        }

        return lvar;
    }

    public LocalVariable getNewLocalVariable(RubySymbol name, int depth) {
        if (depth == 0 && !(this instanceof IRFor)) {
            LocalVariable lvar = new ClosureLocalVariable(name, 0, getStaticScope().addVariableThisScope(name.idString()));
            localVars.put(name, lvar);
            return lvar;
        } else {
            // IRFor does not have it's own state
            if (!(this instanceof IRFor)) flags.add(IRFlags.ACCESS_PARENTS_LOCAL_VARIABLES);
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
    public LocalVariable getLocalVariable(RubySymbol name, int depth) {
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
        if (depth > 0 && !(this instanceof IRFor)) flags.add(IRFlags.ACCESS_PARENTS_LOCAL_VARIABLES);

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

    private static final ByteList CLOSURE_CLONE =
            new ByteList(new byte[] {'_', 'C', 'L', 'O', 'S', 'U', 'R', 'E', '_', 'C', 'L', 'O', 'N', 'E', '_'});

    public IRClosure cloneForInlining(CloneInfo ii) {
        IRClosure clonedClosure;
        IRScope lexicalParent = ii.getScope();

        if (ii instanceof SimpleCloneInfo && !((SimpleCloneInfo)ii).isEnsureBlockCloneMode()) {
            clonedClosure = new IRClosure(this, lexicalParent, closureId, getName());
        } else {
            int id = lexicalParent.getNextClosureId();
            ByteList fullName = lexicalParent.getName().getBytes().dup();
            fullName.append(CLOSURE_CLONE);
            fullName.append(new Integer(id).toString().getBytes());
            clonedClosure = new IRClosure(this, lexicalParent, id, getManager().runtime.newSymbol(fullName));
        }

        // WrappedIRClosure should always have a single unique IRClosure in them so we should
        // not end up adding n copies of the same closure as distinct clones...
        lexicalParent.addClosure(clonedClosure);

        return cloneForInlining(ii, clonedClosure);
    }

    @Override
    public void setName(RubySymbol name) {
        ByteList newName = getLexicalParent().getName().getBytes().dup();

        newName.append(name.getBytes());

        super.setName(getManager().getRuntime().newSymbol(newName));
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

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return argDesc;
    }


    /**
     * Set upon completion of IRBuild of this IRClosure.
     */
    public void setArgumentDescriptors(ArgumentDescriptor[] argDesc) {
        this.argDesc = argDesc;
    }
}

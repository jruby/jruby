package org.jruby.ir;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.jruby.RubySymbol;
import org.jruby.ast.DefNode;
import org.jruby.ast.IterNode;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ir.instructions.*;
import org.jruby.ir.interpreter.ClosureInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.persistence.IRWriter;
import org.jruby.ir.persistence.IRWriterEncoder;
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

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accumulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
// Their parents are always execution scopes.

public class IRClosure extends IRScope {
    public final int closureId;    // Unique id for this closure within the nearest ancestor method.

    private boolean isEND;         // Does this represent and END { } closure?

    private Signature signature;

    // We allow closures who happen to be assigned to calls named 'defined_method' to save the original
    // AST so we can attempt to convert those blocks to full methods.
    private IterNode source;

    // Argument description
    protected ArgumentDescriptor[] argDesc = ArgumentDescriptor.EMPTY_ARRAY;

    /** Added for interp/JIT purposes */
    private IRBlockBody body;

    // Used by other constructions and by IREvalScript as well
    protected IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, ByteList prefix, int coverageMode) {
        super(manager, lexicalParent, null, lineNumber, staticScope, coverageMode);

        this.closureId = lexicalParent.getNextClosureId();
        ByteList name = prefix.dup();
        name.append(Integer.toString(closureId).getBytes());
        setByteName(name);
    }

    protected IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, ByteList prefix) {
        this(manager, lexicalParent, lineNumber, staticScope, prefix, CoverageData.NONE);
    }

    // Used by IREvalScript
    protected IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, int closureId, ByteList fullName) {
        super(manager, lexicalParent, null, lineNumber, staticScope);

        this.closureId = closureId;
        super.setByteName(fullName);
    }

    /** Used by cloning code for inlining */
    /* Inlining generates a new name and id and basic cloning will reuse the originals name */
    protected IRClosure(IRClosure c, IRScope lexicalParent, int closureId, ByteList fullName) {
        super(c, lexicalParent);
        this.closureId = closureId;
        super.setByteName(fullName);

        isEND = c.isEND;

        this.signature = c.signature;
    }

    private static final ByteList CLOSURE = new ByteList(new byte[] {'_', 'C', 'L', 'O', 'S', 'U', 'R', 'E', '_'});

    // Used by persistence.  Knowledge of coverage not needed here since it is already instrumented into the instrs
    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, CLOSURE, false);
    }

    // Used by iter + lambda by IRBuilder
    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, int coverageMode) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, CLOSURE, false, coverageMode);
    }


    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, ByteList prefix) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, prefix, false);
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, ByteList prefix, boolean isBeginEndBlock) {
        this(manager, lexicalParent, lineNumber, staticScope, signature, prefix, isBeginEndBlock, CoverageData.NONE);
    }

    public IRClosure(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope,
                     Signature signature, ByteList prefix, boolean isBeginEndBlock, int coverageMode) {
        this(manager, lexicalParent, lineNumber, staticScope, prefix);
        this.signature = signature;
        lexicalParent.addClosure(this);

        if (staticScope != null) {
            staticScope.setIRScope(this);
            staticScope.setScopeType(this.getScopeType());
        }
    }


    @Override
    public InterpreterContext allocateInterpreterContext(List<Instr> instructions, int temporaryVariableCount, EnumSet<IRFlags> flags) {
        interpreterContext = new ClosureInterpreterContext(this, instructions, temporaryVariableCount, flags);

        return interpreterContext;
    }

    @Override
    public InterpreterContext allocateInterpreterContext(Supplier<List<Instr>> instructions, int temporaryVariableCount, EnumSet<IRFlags> flags) {
        try {
            interpreterContext = new ClosureInterpreterContext(this, instructions, temporaryVariableCount, flags);
        } catch (Exception e) {
            Helpers.throwException(e);
        }

        return interpreterContext;
    }

    public void setIsEND() {
        isEND = true;
    }

    public boolean isEND() {
        return isEND;
    }

    @Override
    public int getNextClosureId() {
        return getLexicalParent().getNextClosureId();
    }

    @Override
    public Label getNewLabel() {
        return getNewLabel(getManager().getClosurePrefix(closureId));
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.CLOSURE;
    }

    @Override
    public boolean isTopLocalVariableScope() {
        return false;
    }

    public String toStringBody() {
        return new StringBuilder(getId()).append(" = {\n").append(toStringInstrs()).append("\n}\n\n").toString();
    }

    public BlockBody getBlockBody() {
        BlockBody body = this.body;

        if (body != null) return body;

        boolean shouldJit = getManager().getInstanceConfig().getCompileMode().shouldJIT();
        return this.body = shouldJit ? new MixedModeIRBlockBody(this, signature) : new InterpretedIRBlockBody(this, signature);
    }

    // FIXME: This is too strict.  We can use any closure which does not dip below the define_method closure.  This
    // will deopt any nested block which dips out of itself.
    public boolean isNestedClosuresSafeForMethodConversion() {
        for (IRClosure closure: getClosures()) {
            if (!closure.isNestedClosuresSafeForMethodConversion()) return false;
        }

        return !accessesParentsLocalVariables();
    }

    public IRMethod convertToMethod(ByteList name) {
        // We want variable scoping to be the same as a method and not see outside itself.
        if (source == null ||
            accessesParentsLocalVariables() ||  // Built methods cannot search down past method scope
            receivesClosureArg() ||             // we pass in captured block at define_method as block so explicits ones not supported
            usesZSuper() ||                     // methods defined from closures cannot use zsuper
            !isNestedClosuresSafeForMethodConversion()) {
            source = null;
            return null;
        }

        DefNode def = source;
        source = null;

        return new IRMethod(getManager(), getLexicalParent(), def, name, true,  getLine(), getStaticScope().duplicate(), getCoverageMode());
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

            if (lvar != null) setAccessesParentsLocalVariables();
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
            if (!(this instanceof IRFor)) setAccessesParentsLocalVariables();
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
        if (depth > 0 && !(this instanceof IRFor)) setAccessesParentsLocalVariables();

        if (depth == 0) return s.getNewLocalVariable(name, 0);

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
            // Note on this null check: Create a new var at eventual scope which has not been created
            // because the hard scope does not have a definition for it yet.  The reason for this can
            // be:
            //   a) initialization of lvar happens after first closure which uses the lvar
            //   b) ensure blocks get processed before the body for the ensure so it is possible
            // a nested closure in the ensure will find an lvar first.
            lvar = s.getNewLocalVariable(name, 0).cloneForDepth(depth);
        } else {
            // Find # of lexical scopes we walked up to find 'lvar'.
            // We need a copy of 'lvar' usable at that depth
            int lvarDepth = depth - (d + 1);
            if (lvar.getScopeDepth() != lvarDepth) lvar = lvar.cloneForDepth(lvarDepth);
        }

        return lvar;
    }

    // FIXME: This is all using interpreterContext but it should be using Full????
    protected IRClosure cloneForInlining(CloneInfo ii, IRClosure clone) {
        SimpleCloneInfo clonedII = ii.cloneForCloningClosure(clone);

//        if (getCFG() != null) {
//            clone.setCFG(getCFG().clone(clonedII, clone));
//        } else {
        List<Instr> newInstrs = new ArrayList<>(interpreterContext.getInstructions().length);

        for (Instr i: interpreterContext.getInstructions()) {
            newInstrs.add(i.clone(clonedII));
        }

        clone.allocateInterpreterContext(newInstrs, interpreterContext.getTemporaryVariableCount(), interpreterContext.getFlags());

//        }

        return clone;
    }

    private static final ByteList CLOSURE_CLONE =
            new ByteList(new byte[] {'_', 'C', 'L', 'O', 'S', 'U', 'R', 'E', '_', 'C', 'L', 'O', 'N', 'E', '_'}, false);

    public IRClosure cloneForInlining(CloneInfo ii) {
        IRClosure clonedClosure;
        IRScope lexicalParent = ii.getScope();

        if (ii instanceof SimpleCloneInfo && !((SimpleCloneInfo) ii).isEnsureBlockCloneMode()) {
            clonedClosure = new IRClosure(this, lexicalParent, closureId, getByteName());
        } else {
            int id = lexicalParent.getNextClosureId();
            ByteList fullName = lexicalParent.getByteName();
            fullName = fullName != null ? fullName.dup() : new ByteList();
            fullName.append(CLOSURE_CLONE);
            fullName.append(Integer.toString(id).getBytes());
            clonedClosure = new IRClosure(this, lexicalParent, id, fullName);
        }

        // WrappedIRClosure should always have a single unique IRClosure in them so we should
        // not end up adding n copies of the same closure as distinct clones...
        lexicalParent.addClosure(clonedClosure);

        return cloneForInlining(ii, clonedClosure);
    }

    @Override
    public void setByteName(ByteList name) {
        ByteList newName = getLexicalParent().getByteName();

        newName = newName == null ? new ByteList() : newName.dup();
        newName.append(name);

        super.setByteName(newName);
    }

    public Signature getSignature() {
        return signature;
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

    public void persistScopeHeader(IRWriterEncoder file) {
        super.persistScopeHeader(file);

        if (getScopeType() == IRScopeType.CLOSURE) {
            if (IRWriter.shouldLog(file)) System.out.println("IRClosure.persistScopeHeader: type       = " + isEND());
            file.encode(isEND());
        }
        if (IRWriter.shouldLog(file)) System.out.println("IRClosure.persistScopeHeader: type       = " + getSignature());
        file.encode(getSignature());
    }
}

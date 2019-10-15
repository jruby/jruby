package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.ast.DefNode;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.util.ByteList;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // Argument description
    protected ArgumentDescriptor[] argDesc = ArgumentDescriptor.EMPTY_ARRAY;

    private volatile DefNode defNode;

    public IRMethod(IRManager manager, IRScope lexicalParent, DefNode defn, ByteList name,
            boolean isInstanceMethod, int lineNumber, StaticScope staticScope, boolean needsCodeCoverage) {
        super(manager, lexicalParent, name, lineNumber, staticScope);

        this.defNode = defn;
        this.isInstanceMethod = isInstanceMethod;

        if (needsCodeCoverage) getFlags().add(IRFlags.CODE_COVERAGE);

        if (!getManager().isDryRun() && staticScope != null) {
            staticScope.setIRScope(this);
        }
    }

    @Override
    public boolean hasBeenBuilt() {
        return defNode == null;
    }

    public final InterpreterContext lazilyAcquireInterpreterContext() {
        if (!hasBeenBuilt()) buildMethodImpl();

        return interpreterContext;
    }

    private synchronized void buildMethodImpl() {
        if (hasBeenBuilt()) return;

        IRBuilder.topIRBuilder(getManager(), this).
                defineMethodInner(defNode, getLexicalParent(), getFlags().contains(IRFlags.CODE_COVERAGE)); // sets interpreterContext
        this.defNode = null;
    }

    public synchronized BasicBlock[] prepareForCompilation() {
        buildMethodImpl();

        return super.prepareForCompilation();
    }

    @Override
    public IRScopeType getScopeType() {
        return isInstanceMethod ? IRScopeType.INSTANCE_METHOD : IRScopeType.CLASS_METHOD;
    }

    @Override
    protected LocalVariable findExistingLocalVariable(RubySymbol name, int scopeDepth) {
        assert scopeDepth == 0: "Local variable depth in IRMethod should always be zero (" + name + " had depth of " + scopeDepth + ")";
        return localVars.get(name);
    }

    @Override
    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, scopeDepth);
        return lvar;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return argDesc;
    }


    /**
     * Set upon completion of IRBuild of this IRMethod.
     */
    public void setArgumentDescriptors(ArgumentDescriptor[] argDesc) {
        this.argDesc = argDesc;
    }
}

package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.ast.DefNode;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // Argument description
    protected ArgumentDescriptor[] argDesc = ArgumentDescriptor.EMPTY_ARRAY;

    private DefNode defn;

    public IRMethod(IRManager manager, IRScope lexicalParent, DefNode defn, RubySymbol name,
            boolean isInstanceMethod, int lineNumber, StaticScope staticScope, boolean needsCodeCoverage) {
        super(manager, lexicalParent, name, lineNumber, staticScope);

        this.defn = defn;
        this.isInstanceMethod = isInstanceMethod;

        if (needsCodeCoverage) getFlags().add(IRFlags.CODE_COVERAGE);

        if (!getManager().isDryRun() && staticScope != null) {
            staticScope.setIRScope(this);
        }
    }

    @Override
    public boolean hasBeenBuilt() {
        return defn == null;
    }

    public synchronized InterpreterContext lazilyAcquireInterpreterContext() {
        if (!hasBeenBuilt()) {
            IRBuilder.topIRBuilder(getManager(), this).defineMethodInner(defn, getLexicalParent(), getFlags().contains(IRFlags.CODE_COVERAGE));

            defn = null;
        }

        return interpreterContext;
    }

    public synchronized BasicBlock[] prepareForCompilation() {
        if (!hasBeenBuilt()) lazilyAcquireInterpreterContext();

        BasicBlock[] bbs = super.prepareForCompilation();

        return bbs;
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

package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;

import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.parser.StaticScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

public class IRScript extends IRScopeImpl {
    private final IRClass dummyClass;  // Dummy class for the script

    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;

    public IRScript(String className, String sourceName, StaticScope staticScope) {
        super((IRScope) null, sourceName, staticScope);
        dummyClass = new IRClass(this, null, "[script]:" + sourceName, staticScope);
    }

    public StringLiteral getFileName() {
        return new StringLiteral(getName());
    }

    @Override
    public String getScopeName() {
        return "Script";
    }

    public IRMethod getRootMethod() {
        return dummyClass.getRootMethod();
    }

    public IRClass getRootClass() {
        return dummyClass;
    }

    @Override
    public String toString() {
        return "Script: file: " + getFileName() + super.toString();
    }

    public LocalVariable getLocalVariable(String name, int depth) {
        throw new UnsupportedOperationException("This should be happening on Root Method instead");
    }

    public void runCompilerPass(CompilerPass p) {
        dummyClass.runCompilerPass(p);
    }

    /* Record a begin block -- not all scope implementations can handle them */
    public void recordBeginBlock(IRClosure beginBlockClosure) {
        if (beginBlocks == null) beginBlocks = new ArrayList<IRClosure>();
        beginBlocks.add(beginBlockClosure);
    }

    /* Record an end block -- not all scope implementations can handle them */
    public void recordEndBlock(IRClosure endBlockClosure) {
        if (endBlocks == null) endBlocks = new ArrayList<IRClosure>();
        endBlocks.add(endBlockClosure);
    }

    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }

    public List<IRClosure> getEndBlocks() {
        return endBlocks;
    }
}

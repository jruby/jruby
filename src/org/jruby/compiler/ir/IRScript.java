package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;

import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.parser.StaticScope;

// FIXME: I made this IRModule because any methods placed in top-level script goes
// into something which an IRScript is basically a module that is special in that
// it represents a lexical unit.  Fix what now?
public class IRScript extends IRModule {
    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;

    public IRScript(String className, String sourceName, StaticScope staticScope) {
        super((IRScope) null, sourceName, staticScope);
    }

    public StringLiteral getFileName() {
        return new StringLiteral(getName());
    }

    @Override
    public String getScopeName() {
        return "Script";
    }

    @Override
    public String toString() {
        return "Script: file: " + getFileName() + super.toString();
    }

    /* Record a begin block -- not all scope implementations can handle them */
    @Override
    public void recordBeginBlock(IRClosure beginBlockClosure) {
        if (beginBlocks == null) beginBlocks = new ArrayList<IRClosure>();
        beginBlocks.add(beginBlockClosure);
    }

    /* Record an end block -- not all scope implementations can handle them */
    @Override
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

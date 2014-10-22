package org.jruby.ir;

import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.interpreter.ScriptBodyInterpreterContext;
import org.jruby.parser.StaticScope;

import java.util.ArrayList;
import java.util.List;

public class IRScriptBody extends IRScope {
    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;

    public IRScriptBody(IRManager manager, String sourceName, StaticScope staticScope) {
        super(manager, null, sourceName, sourceName, 0, staticScope);
        if (!getManager().isDryRun() && staticScope != null) {
            staticScope.setIRScope(this);
            staticScope.setScopeType(this.getScopeType());
        }
    }

    @Override
    public InterpreterContext allocateInterpreterContext(Instr[] instructionList) {
        return new ScriptBodyInterpreterContext(this, instructionList);
    }

    @Override
    public int getNearestModuleReferencingScopeDepth() {
        return 0;
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.SCRIPT_BODY;
    }

    @Override
    public String toString() {
        return "Script: file: " + getFileName() + super.toString();
    }

    /* Record a begin block -- not all scope implementations can handle them */
    @Override
    public void recordBeginBlock(IRClosure beginBlockClosure) {
        if (beginBlocks == null) beginBlocks = new ArrayList<IRClosure>();
        beginBlockClosure.setBeginEndBlock();
        beginBlocks.add(beginBlockClosure);
    }

    /* Record an end block -- not all scope implementations can handle them */
    @Override
    public void recordEndBlock(IRClosure endBlockClosure) {
        if (endBlocks == null) endBlocks = new ArrayList<IRClosure>();
        endBlockClosure.setBeginEndBlock();
        endBlocks.add(endBlockClosure);
    }

    @Override
    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }

    @Override
    public List<IRClosure> getEndBlocks() {
        return endBlocks;
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }
}

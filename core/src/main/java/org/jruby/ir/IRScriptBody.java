package org.jruby.ir;

import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.BeginEndInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

import java.util.ArrayList;
import java.util.List;

public class IRScriptBody extends IRScope {
    private List<IRClosure> beginBlocks;
    private DynamicScope toplevelScope;

    public IRScriptBody(IRManager manager, String sourceName, StaticScope staticScope) {
        super(manager, null, sourceName, sourceName, 0, staticScope);
        this.toplevelScope = null;
        if (!getManager().isDryRun() && staticScope != null) {
            staticScope.setIRScope(this);
            staticScope.setScopeType(this.getScopeType());
        }
    }

    public DynamicScope getToplevelScope() {
        return toplevelScope;
    }

    public void setTopLevelBindingScope(DynamicScope tlbScope) {
        this.toplevelScope = tlbScope;
    }

    @Override
    public InterpreterContext allocateInterpreterContext(Instr[] instructionList, boolean rebuild) {
        return new BeginEndInterpreterContext(this, instructionList, rebuild);
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

    @Override
    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }
}

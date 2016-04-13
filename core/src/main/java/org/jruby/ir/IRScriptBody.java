package org.jruby.ir;

import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.BeginEndInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class IRScriptBody extends IRScope {
    private List<IRClosure> beginBlocks;
    private DynamicScope toplevelScope;
    private String fileName;

    public IRScriptBody(IRManager manager, String sourceName, StaticScope staticScope) {
        super(manager, null, sourceName, 0, staticScope);
        this.toplevelScope = null;
        this.fileName = sourceName;

        if (!getManager().isDryRun() && staticScope != null) {
            staticScope.setIRScope(this);
        }
    }

    public DynamicScope getToplevelScope() {
        return toplevelScope;
    }

    public void setTopLevelBindingScope(DynamicScope tlbScope) {
        this.toplevelScope = tlbScope;
    }

    @Override
    public InterpreterContext allocateInterpreterContext(List<Instr> instructions) {
        interpreterContext = new BeginEndInterpreterContext(this, instructions);

        return interpreterContext;
    }

    @Override
    public InterpreterContext allocateInterpreterContext(Callable<List<Instr>> instructions) {
        try {
            interpreterContext = new BeginEndInterpreterContext(this, instructions);
        } catch (Exception e) {
            Helpers.throwException(e);
        }

        return interpreterContext;
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
        if (beginBlocks == null) beginBlocks = new ArrayList<>();
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

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }
}

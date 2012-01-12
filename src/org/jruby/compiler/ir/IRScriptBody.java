package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;

import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;

// FIXME: I made this IRModule because any methods placed in top-level script goes
// into something which an IRScript is basically a module that is special in that
// it represents a lexical unit.  Fix what now?
public class IRScriptBody extends IRScope {
    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;

    public IRScriptBody(String className, String sourceName, StaticScope staticScope) {
        super(null, sourceName, sourceName, staticScope);
        if (!IRBuilder.inIRGenOnlyMode()) {
            if (staticScope != null) ((IRStaticScope)staticScope).setIRScope(this);
        }
    }

    @Override
    public IRScope getNearestModuleReferencingScope() {
        return this;
    }
    
    @Override
    public LocalVariable getImplicitBlockArg() {
        assert false: "A Script body never accepts block args";
        
        return null;
    }
    
    public String getScopeName() {
        return "ScriptBody";
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
    
    @Override
    public boolean isScriptScope() {
        return true;
    }
}

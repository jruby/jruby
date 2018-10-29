package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jruby.EvalType;
import org.jruby.RubySymbol;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.BeginEndInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Helpers;
import org.jruby.util.ByteList;

public class IREvalScript extends IRClosure {
    private List<IRClosure> beginBlocks;
    private EvalType evalType;
    private String fileName;

    private static final ByteList EVAL_ = new ByteList(new byte[] {'E', 'V', 'A', 'L', '_'});

    public IREvalScript(IRManager manager, IRScope lexicalParent, String fileName,
            int lineNumber, StaticScope staticScope, EvalType evalType) {
        super(manager, lexicalParent, lineNumber, staticScope, EVAL_);

        this.evalType = evalType;
        this.fileName = fileName;

        if (!getManager().isDryRun() && staticScope != null) {
            // SSS FIXME: This is awkward!
            if (evalType == EvalType.MODULE_EVAL) {
                staticScope.setScopeType(getScopeType());
            } else {
                IRScope s = lexicalParent;
                while (s instanceof IREvalScript) {
                    s = s.getLexicalParent();
                }
                staticScope.setScopeType(s.getScopeType());
            }
        }
    }

    @Override
    public InterpreterContext allocateInterpreterContext(List<Instr> instructions) {
        interpreterContext =  new BeginEndInterpreterContext(this, instructions);

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
    public Label getNewLabel() {
        return getNewLabel("EV" + closureId + "_LBL");
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.EVAL_SCRIPT;
    }

    public boolean isModuleOrInstanceEval() {
        return evalType == EvalType.MODULE_EVAL || evalType == EvalType.INSTANCE_EVAL;
    }

    /* Record a begin block -- not all scope implementations can handle them */
    @Override
    public void recordBeginBlock(IRClosure beginBlockClosure) {
        if (beginBlocks == null) beginBlocks = new ArrayList<>();
        beginBlockClosure.setBeginEndBlock();
        beginBlocks.add(beginBlockClosure);
    }

    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }

    @Override
    public LocalVariable getNewFlipStateVariable() {
        ByteList flipVarName = new ByteList(("%flip_" + allocateNextPrefixedName("%flip")).getBytes());
        RubySymbol name = getManager().getRuntime().newSymbol(flipVarName);
        LocalVariable v = lookupExistingLVar(name);

        return v == null ? getNewLocalVariable(name, 0) : v;
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }

    @Override
    public boolean isFlipScope() {
        return true;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFile() {
        return fileName;
    }
}

package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import org.jruby.EvalType;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.BeginEndInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;

public class IREvalScript extends IRClosure {
    private List<IRClosure> beginBlocks;
    private EvalType evalType;

    public IREvalScript(IRManager manager, IRScope lexicalParent, String fileName,
            int lineNumber, StaticScope staticScope, EvalType evalType) {
        super(manager, lexicalParent, fileName, lineNumber, staticScope, "EVAL_");

        this.evalType = evalType;

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
    public InterpreterContext allocateInterpreterContext(Instr[] instructionList, boolean rebuild) {
        return new BeginEndInterpreterContext(this, instructionList, rebuild);
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
        String flipVarName = "%flip_" + allocateNextPrefixedName("%flip");
        LocalVariable v = lookupExistingLVar(flipVarName);

        return v == null ? getNewLocalVariable(flipVarName, 0) : v;
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }

    @Override
    public boolean isFlipScope() {
        return true;
    }
}

package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import org.jruby.EvalType;
import org.jruby.RubyModule;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.BeginEndInterpreterContext;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class IREvalScript extends IRClosure {
    private static final Logger LOG = LoggerFactory.getLogger("IREvalScript");

    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;
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
    public InterpreterContext allocateInterpreterContext(Instr[] instructionList) {
        return new BeginEndInterpreterContext(this, instructionList);
    }

    @Override
    public Label getNewLabel() {
        return getNewLabel("EV" + closureId + "_LBL");
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.EVAL_SCRIPT;
    }

    @Override
    public Operand[] getBlockArgs() {
        return new Operand[0];
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

    /* Record an end block -- not all scope implementations can handle them */
    @Override
    public void recordEndBlock(IRClosure endBlockClosure) {
        if (endBlocks == null) endBlocks = new ArrayList<>();
        endBlockClosure.setBeginEndBlock();
        endBlocks.add(endBlockClosure);
    }

    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }

    public List<IRClosure> getEndBlocks() {
        return endBlocks;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, Block block, String backtraceName) {
        if (IRRuntimeHelpers.isDebug()) {
            LOG.info("Graph:\n" + cfg().toStringGraph());
            LOG.info("CFG:\n" + cfg().toStringInstrs());
        }

        InterpreterContext ic = prepareForInterpretation();

        // FIXME: Do not push new empty arg array in every time
        return Interpreter.INTERPRET_EVAL(context, self, ic, clazz, new IRubyObject[] {}, backtraceName, block, null);
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

package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import org.jruby.RubyModule;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class IREvalScript extends IRClosure {
    private static final Logger LOG = LoggerFactory.getLogger("IREvalScript");

    private IRScope nearestNonEvalScope;
    private int     nearestNonEvalScopeDepth;
    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;

    public IREvalScript(IRManager manager, IRScope lexicalParent, String fileName,
            int lineNumber, StaticScope staticScope) {
        super(manager, lexicalParent, fileName, lineNumber, staticScope, "EVAL_");

        int n = 0;
        IRScope s = lexicalParent;
        while (s instanceof IREvalScript) {
            n++;
            s = s.getLexicalParent();
        }

        this.nearestNonEvalScope = s;
        this.nearestNonEvalScopeDepth = n;
        this.nearestNonEvalScope.initEvalScopeVariableAllocator(false);
    }

    @Override
    public Label getNewLabel() {
        return getNewLabel("EV" + closureId + "_LBL");
    }

    @Override
    public String getScopeName() {
        return "EvalScript";
    }

    @Override
    public Operand[] getBlockArgs() {
        return new Operand[0];
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

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, DynamicScope evalScope, Block block, String backtraceName) {
        if (IRRuntimeHelpers.isDebug()) {
            LOG.info("CFG:\n" + cfg());
        }
        try {
            context.pushScope(evalScope);

            // Since IR introduces additional local vars, we may need to grow the dynamic scope.
            // To do that, IREvalScript has to tell the dyn-scope how many local vars there are.
            // Since the same static scope (the scope within which the eval string showed up)
            // might be shared by multiple eval-scripts, we cannot 'setIRScope(this)' once and
            // forget about it.  We need to set this right before we are ready to grow the
            // dynamic scope local var space.
            ((IRStaticScope)getStaticScope()).setIRScope(this);
            evalScope.growIfNeeded();

            // FIXME: Do not push new empty arg array in every time
            return Interpreter.INTERPRET_EVAL(context, self, this, clazz, new IRubyObject[] {}, backtraceName, block, null);
        }
        finally {
            context.popScope();
        }
    }

    @Override
    public LocalVariable findExistingLocalVariable(String name, int scopeDepth) {
        // Look in the nearest non-eval scope's shared eval scope vars first.
        // If you dont find anything there, look in the nearest non-eval scope's regular vars.
        LocalVariable lvar = nearestNonEvalScope.evalScopeVars.getVariable(name);
        if ((lvar != null) || scopeDepth == 0) return lvar;
        else return nearestNonEvalScope.findExistingLocalVariable(name, scopeDepth-nearestNonEvalScopeDepth-1);
    }

    @Override
    public LocalVariable getNewLocalVariable(String name, int depth) {
        assert depth == nearestNonEvalScopeDepth: "Local variable depth in IREvalScript:getNewLocalVariable must be " + nearestNonEvalScopeDepth + ".  Got " + depth;
        LocalVariable lvar = new ClosureLocalVariable(this, name, 0, nearestNonEvalScope.evalScopeVars.nextSlot);
        nearestNonEvalScope.evalScopeVars.putVariable(name, lvar);
        return lvar;
    }

    @Override
    public LocalVariable getNewFlipStateVariable() {
        return getLocalVariable("%flip_" + allocateNextPrefixedName("%flip"), 0);
    }

    @Override
    public int getUsedVariablesCount() {
        return 1 + nearestNonEvalScope.evalScopeVars.nextSlot + getPrefixCountSize("%flip");
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }

    @Override
    public boolean isTopLocalVariableScope() {
        return false;
    }

    @Override
    public boolean isFlipScope() {
        return true;
    }
}

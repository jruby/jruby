package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import org.jruby.RubyModule;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.representations.CFG;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

// FIXME: I made this IRModule because any methods placed in top-level script goes
// into something which an IRScript is basically a module that is special in that
// it represents a lexical unit.  Fix what now?
public class IRScriptBody extends IRScope {
    private static final Logger LOG = LoggerFactory.getLogger("IRScriptBody");

    private List<IRClosure> beginBlocks;
    private List<IRClosure> endBlocks;

    public IRScriptBody(IRManager manager, String className, String sourceName,
            StaticScope staticScope) {
        super(manager, null, sourceName, sourceName, 0, staticScope);

        if (!getManager().isDryRun()) {
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

    public IRubyObject interpret(ThreadContext context, IRubyObject self) {
        prepareForInterpretation(false);

        String name = "(root)";
        if (IRRuntimeHelpers.isDebug()) {
            LOG.info("Executing '" + name + "'");
            CFG cfg = getCFG();
            LOG.info("Graph:\n" + cfg.toStringGraph());
            LOG.info("CFG:\n" + cfg.toStringInstrs());
        }

        // We get the live object ball rolling here.
        // This give a valid value for the top of this lexical tree.
        // All new scopes can then retrieve and set based on lexical parent.
        StaticScope scope = getStaticScope();
        RubyModule currModule = scope.getModule();
        if (currModule == null) {
            // SSS FIXME: Looks like this has to do with Kernel#load
            // and the wrap parameter. Figure it out and document it here.
            currModule = context.getRuntime().getObject();
        }

        IRubyObject retVal;
        try {
            scope.setModule(currModule);
            context.preMethodScopeOnly(currModule, scope);
            context.setCurrentVisibility(Visibility.PRIVATE);

            Interpreter.runBeginEndBlocks(getBeginBlocks(), context, self, null);
            retVal = Interpreter.INTERPRET_ROOT(context, self, this, currModule, name);
            Interpreter.runBeginEndBlocks(getEndBlocks(), context, self, null);

            Interpreter.dumpStats();
        } catch (IRBreakJump bj) {
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
        } finally {
            context.popRubyClass();
            context.popScope();
        }

        return retVal;
    }
}

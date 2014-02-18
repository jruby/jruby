package org.jruby.ir;

import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;

/**
 * Represents a 'for' loop
 */
public class IRFor extends IRClosure {
    public IRFor(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Arity arity, int argumentType) {
        super(manager, lexicalParent, lineNumber, IRStaticScopeFactory.newIRBlockScope(staticScope), arity, argumentType, "_FOR_LOOP_");
    }

    /** Used by cloning code */
    private IRFor(IRClosure c, IRScope lexicalParent) {
        super(c, lexicalParent, "_FOR_LOOP_CLONE_");
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.FOR;
    }

    @Override
    public IRClosure cloneForInlining(InlinerInfo ii) {
        // FIXME: This is buggy! Is this not dependent on clone-mode??
        IRClosure clonedClosure = new IRFor(this, ii.getNewLexicalParentForClosure());
        return cloneForInlining(ii, clonedClosure);
    }
}

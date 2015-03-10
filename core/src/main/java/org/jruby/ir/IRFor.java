package org.jruby.ir;

import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Signature;

/**
 * Represents a 'for' loop
 */
public class IRFor extends IRClosure {
    public IRFor(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, String labelPrefix) {
        super(manager, lexicalParent, lineNumber, StaticScopeFactory.newIRBlockScope(staticScope), signature, labelPrefix, labelPrefix == "_BEGIN_");
    }

    public IRFor(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature) {
        this(manager, lexicalParent, lineNumber, StaticScopeFactory.newIRBlockScope(staticScope), signature, "_FOR_LOOP_");
    }

    /** Used by cloning code */
    private IRFor(IRClosure c, IRScope lexicalParent, int id, String fullName) {
        super(c, lexicalParent, id, fullName);
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.FOR;
    }

    @Override
    public IRClosure cloneForInlining(CloneInfo ii) {
        IRClosure clonedClosure;
        IRScope lexicalParent = ii.getScope();

        if (ii instanceof SimpleCloneInfo) {
            clonedClosure = new IRFor(this, lexicalParent, closureId, getName());
        } else {
            int id = lexicalParent.getNextClosureId();
            String fullName = lexicalParent.getName() + "_FOR_LOOP_CLONE_" + id;
            clonedClosure = new IRFor(this, lexicalParent, id, fullName);
        }

        return cloneForInlining(ii, clonedClosure);
    }
}

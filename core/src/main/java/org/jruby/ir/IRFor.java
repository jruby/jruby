package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.Signature;
import org.jruby.util.ByteList;

/**
 * Represents a 'for' loop
 */
public class IRFor extends IRClosure {
    private static final ByteList FOR_LOOP = new ByteList(new byte[] {'_', 'F', 'O', 'R', '_', 'L', 'O', 'O', 'P', '_'});
    public static final ByteList _BEGIN_ = new ByteList(new byte[] {'_', 'B', 'E', 'G', 'I', 'N', '_'});

    public IRFor(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature, ByteList labelPrefix) {
        super(manager, lexicalParent, lineNumber, StaticScopeFactory.newIRBlockScope(staticScope), signature, labelPrefix, labelPrefix.equals(_BEGIN_));
    }

    public IRFor(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Signature signature) {
        this(manager, lexicalParent, lineNumber, StaticScopeFactory.newIRBlockScope(staticScope), signature, FOR_LOOP);
    }

    /** Used by cloning code */
    private IRFor(IRClosure c, IRScope lexicalParent, int id, RubySymbol fullName) {
        super(c, lexicalParent, id, fullName);
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.FOR;
    }

    private static final ByteList FOR_LOOP_CLONE = new ByteList(new byte[] {'_', 'F', 'O', 'R', '_', 'L', 'O', 'O', 'P', '_', 'C', 'L', 'O', 'N', 'E', '_'});

    @Override
    public IRClosure cloneForInlining(CloneInfo ii) {
        IRClosure clonedClosure;
        IRScope lexicalParent = ii.getScope();

        if (ii instanceof SimpleCloneInfo) {
            clonedClosure = new IRFor(this, lexicalParent, closureId, getName());
        } else {
            int id = lexicalParent.getNextClosureId();
            ByteList fullName = lexicalParent.getName().getBytes().dup();
            fullName.append(FOR_LOOP_CLONE);
            fullName.append(Integer.toString(id).getBytes());
            clonedClosure = new IRFor(this, lexicalParent, id, getManager().getRuntime().newSymbol(fullName));
        }

        return cloneForInlining(ii, clonedClosure);
    }
}

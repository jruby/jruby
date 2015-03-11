package org.jruby.ir.operands;

import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class TemporaryCurrentScopeVariable extends TemporaryLocalVariable {
    // First four scopes are so common and this operand is immutable so we share them.
    public static final TemporaryCurrentScopeVariable[] CURRENT_SCOPE_VARIABLE = {
            new TemporaryCurrentScopeVariable(0), new TemporaryCurrentScopeVariable(1), new TemporaryCurrentScopeVariable(2), new TemporaryCurrentScopeVariable(3), new TemporaryCurrentScopeVariable(4)
    };

    public static TemporaryCurrentScopeVariable ScopeVariableFor(int depth) {
        return depth < CURRENT_SCOPE_VARIABLE.length ? CURRENT_SCOPE_VARIABLE[depth] : new TemporaryCurrentScopeVariable(depth);
    }
    
    public TemporaryCurrentScopeVariable(int offset) {
        super(offset);
    }

    @Override
    public TemporaryVariableType getType() {
        return TemporaryVariableType.CURRENT_SCOPE;
    }

    @Override
    public String getName() {
        return "%current_scope";
    }

    @Override
    public Variable clone(SimpleCloneInfo ii) {
        return this;
    }

    public static TemporaryCurrentScopeVariable decode(IRReaderDecoder d) {
        return ScopeVariableFor(d.decodeInt());
    }
}

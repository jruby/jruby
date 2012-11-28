/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.parser;

/**
 * This scope is used solely for evals. All eval calls under a given scope live
 * in the same "eval" scope, which is always based on this type of static
 * scope. Also, for purposes of flip-flops, this acts like a local scope.
 */
public class EvalStaticScope extends BlockStaticScope {
    protected EvalStaticScope(StaticScope parentScope) {
        super(parentScope, new String[0]);
    }

    protected EvalStaticScope(StaticScope parentScope, String[] names) {
        super(parentScope, names);
    }
    
    @Override
    public StaticScope getLocalScope() {
        return this;
    }

    @Override
    public Type getType() {
        return Type.EVAL;
    }
}

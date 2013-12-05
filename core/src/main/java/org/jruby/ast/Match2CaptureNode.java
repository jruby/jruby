package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Match2CaptureNode extends Match2Node {
    // Allocated locals that the regexp will assign after performing a match
    private int[] scopeOffsets;

    public Match2CaptureNode(ISourcePosition position, Node receiverNode, Node valueNode,
            int[] scopeOffsets) {
        super(position, receiverNode, valueNode);

        this.scopeOffsets = scopeOffsets;
    }

    public int[] getScopeOffsets() {
        return scopeOffsets;
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject result = super.interpret(runtime, context, self, aBlock);

        Helpers.updateScopeWithCaptures(context, scopeOffsets, result);

        return result;
    }
}

package org.jruby.ast;

import org.jruby.lexer.yacc.ISourcePosition;

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
}

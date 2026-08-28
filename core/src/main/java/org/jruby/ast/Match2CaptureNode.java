package org.jruby.ast;

public class Match2CaptureNode extends Match2Node {
    // Allocated locals that the regexp will assign after performing a match
    private final int[] scopeOffsets;

    public Match2CaptureNode(int line, Node receiverNode, Node valueNode,
            int[] scopeOffsets) {
        super(line, receiverNode, valueNode);

        this.scopeOffsets = scopeOffsets;
    }

    public int[] getScopeOffsets() {
        return scopeOffsets;
    }
}

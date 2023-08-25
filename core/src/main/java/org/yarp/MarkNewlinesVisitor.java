package org.yarp;

// Keep in sync with Ruby MarkNewlinesVisitor
final class MarkNewlinesVisitor extends AbstractNodeVisitor<Void> {

    private final Nodes.Source source;
    private boolean[] newlineMarked;

    MarkNewlinesVisitor(Nodes.Source source, boolean[] newlineMarked) {
        this.source = source;
        this.newlineMarked = newlineMarked;
    }

    @Override
    public Void visitBlockNode(Nodes.BlockNode node) {
        boolean[] oldNewlineMarked = this.newlineMarked;
        this.newlineMarked = new boolean[oldNewlineMarked.length];
        try {
            return super.visitBlockNode(node);
        } finally {
            this.newlineMarked = oldNewlineMarked;
        }
    }

    @Override
    public Void visitLambdaNode(Nodes.LambdaNode node) {
        boolean[] oldNewlineMarked = this.newlineMarked;
        this.newlineMarked = new boolean[oldNewlineMarked.length];
        try {
            return super.visitLambdaNode(node);
        } finally {
            this.newlineMarked = oldNewlineMarked;
        }
    }

    @Override
    public Void visitIfNode(Nodes.IfNode node) {
        node.setNewLineFlag(this.source, this.newlineMarked);
        return super.visitIfNode(node);
    }

    @Override
    public Void visitUnlessNode(Nodes.UnlessNode node) {
        node.setNewLineFlag(this.source, this.newlineMarked);
        return super.visitUnlessNode(node);
    }

    @Override
    public Void visitStatementsNode(Nodes.StatementsNode node) {
        for (Nodes.Node child : node.body) {
            child.setNewLineFlag(this.source, this.newlineMarked);
        }
        return super.visitStatementsNode(node);
    }

    @Override
    protected Void defaultVisit(Nodes.Node node) {
        node.visitChildNodes(this);
        return null;
    }

}

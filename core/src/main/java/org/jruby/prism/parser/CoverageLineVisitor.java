package org.jruby.prism.parser;

import org.prism.AbstractNodeVisitor;
import org.prism.Nodes;

public class CoverageLineVisitor extends AbstractNodeVisitor<Void> {

    private final Nodes.Source source;
    private int[] newlineMarked;

    CoverageLineVisitor(Nodes.Source source, int[] newlineMarked) {
        this.source = source;
        this.newlineMarked = newlineMarked;
    }

    @Override
    public Void visitCallNode(Nodes.CallNode node) {
        mark(node);
        return super.visitCallNode(node);
    }

    @Override
    public Void visitClassNode(Nodes.ClassNode node) {
        mark(node);
        return super.visitClassNode(node);
    }

    @Override
    public Void visitIfNode(Nodes.IfNode node) {
        mark(node);
        return super.visitIfNode(node);
    }

    @Override
    public Void visitDefNode(Nodes.DefNode node) {
        mark(node);
        return super.visitDefNode(node);
    }

    @Override
    public Void visitModuleNode(Nodes.ModuleNode node) {
        mark(node);
        return super.visitModuleNode(node);
    }

    @Override
    public Void visitUnlessNode(Nodes.UnlessNode node) {
        mark(node);
        return super.visitUnlessNode(node);
    }

    @Override
    public Void visitStatementsNode(Nodes.StatementsNode node) {
        for (Nodes.Node child : node.body) {
            mark(child);
        }
        return super.visitStatementsNode(node);
    }

    @Override
    protected Void defaultVisit(Nodes.Node node) {
        node.visitChildNodes(this);
        return null;
    }

    private Nodes.Node mark(Nodes.Node node) {
        int line = source.line(node.startOffset);
        if (line == 0) System.out.println("node: " + node);
        if (line > 0) newlineMarked[line - 1] = 0;
        return node;
    }
}
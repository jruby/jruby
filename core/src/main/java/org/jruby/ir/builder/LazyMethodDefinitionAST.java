package org.jruby.ir.builder;

import org.jruby.ast.Colon3Node;
import org.jruby.ast.DefNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;

import java.util.ArrayList;
import java.util.List;

public class LazyMethodDefinitionAST implements LazyMethodDefinition<Node, DefNode, WhenNode, RescueBodyNode, Colon3Node, HashNode> {
    private final DefNode node;

    public LazyMethodDefinitionAST(DefNode node) {
        this.node = node;
    }

    @Override
    public int getEndLine() {
        return node.getEndLine();
    }

    @Override
    public List<String> getMethodData() {
        List<String> ivarNames = new ArrayList<>(1);

        node.getBodyNode().accept(new AbstractNodeVisitor<Object>() {
            @Override
            protected Object defaultVisit(Node node) {
                if (node == null) return null;

                if (node instanceof InstVarNode) {
                    ivarNames.add(((InstVarNode) node).getName().idString());
                } else if (node instanceof InstAsgnNode) {
                    ivarNames.add(((InstAsgnNode) node).getName().idString());
                }

                node.childNodes().forEach(this::defaultVisit);

                return null;
            }
        });

        return ivarNames;
    }

    @Override
    public DefNode getMethod() {
        return node;
    }

    @Override
    public Node getMethodBody() {
        return node.getBodyNode();
    }

    @Override
    public IRBuilder<Node, DefNode, WhenNode, RescueBodyNode, Colon3Node, HashNode> getBuilder(IRManager manager, IRMethod methodScope) {
        return manager.getBuilderFactory().newIRBuilder(manager, methodScope, null, null);
    }
}

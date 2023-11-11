package org.jruby.ir.builder;

import org.jcodings.Encoding;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.prism.AbstractNodeVisitor;
import org.prism.Nodes;
import org.prism.Nodes.ConstantPathNode;
import org.prism.Nodes.DefNode;
import org.prism.Nodes.InstanceVariableReadNode;
import org.prism.Nodes.InstanceVariableWriteNode;
import org.prism.Nodes.Node;
import org.prism.Nodes.RescueNode;
import org.prism.Nodes.WhenNode;

import java.util.ArrayList;
import java.util.List;

public class LazyMethodDefinitionPrism implements LazyMethodDefinition<Node, DefNode, WhenNode, RescueNode, ConstantPathNode, Nodes.HashPatternNode> {
    private final Nodes.Source nodeSource;
    private DefNode node;
    private byte[] source;

    final private Encoding encoding;

    public LazyMethodDefinitionPrism(byte[] source, Nodes.Source nodeSource, Encoding encoding, DefNode node) {
        this.source = source;
        this.node = node;
        this.nodeSource = nodeSource;
        this.encoding = encoding;
    }
    @Override
    public int getEndLine() {
        return nodeSource.line(node.endOffset()) - 1;
    }

    @Override
    public List<String> getMethodData() {
        List<String> ivarNames = new ArrayList<>();

        if (node.body != null) {
            node.body.accept(new AbstractNodeVisitor<Object>() {
                @Override
                protected Object defaultVisit(Node node) {
                    if (node == null) return null;

                    if (node instanceof InstanceVariableReadNode) {
                        ivarNames.add(((InstanceVariableReadNode) node).name.idString());
                    } else if (node instanceof InstanceVariableWriteNode) {
                        ivarNames.add(((InstanceVariableWriteNode) node).name.idString());
                    }

                    Node[] children = node.childNodes();

                    for (int i = 0; i < children.length; i++) {
                        defaultVisit(children[i]);
                    }

                    return null;
                }
            });
        }

        return ivarNames;
    }


    @Override
    public DefNode getMethod() {
        return node;
    }

    @Override
    public Node getMethodArgs() {
        return node.parameters;
    }

    @Override
    public Node getMethodBody() {
        return node.body;
    }

    @Override
    public IRBuilder<Node, DefNode, WhenNode, RescueNode, ConstantPathNode, Nodes.HashPatternNode> getBuilder(IRManager manager, IRMethod methodScope) {
        IRBuilder<Node, DefNode, WhenNode, RescueNode, ConstantPathNode, Nodes.HashPatternNode> builder = IRBuilder.newIRBuilder(manager, methodScope, null, encoding, true);

        ((IRBuilderPrism) builder).source = source;
        ((IRBuilderPrism) builder).nodeSource = nodeSource;

        return builder;
    }
}

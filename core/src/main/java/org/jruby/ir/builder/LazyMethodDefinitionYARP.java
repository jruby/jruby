package org.jruby.ir.builder;

import org.jruby.Ruby;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.util.ByteList;
import org.yarp.AbstractNodeVisitor;
import org.yarp.Nodes;
import org.yarp.Nodes.DefNode;
import org.yarp.Nodes.InstanceVariableReadNode;
import org.yarp.Nodes.InstanceVariableWriteNode;
import org.yarp.Nodes.Node;
import org.yarp.Nodes.WhenNode;

import java.util.ArrayList;
import java.util.List;

public class LazyMethodDefinitionYARP implements LazyMethodDefinition<Node, DefNode, WhenNode> {
    private DefNode node;
    private Ruby runtime;
    private byte[] source;

    public LazyMethodDefinitionYARP(Ruby runtime, byte[] source, DefNode node) {
        this.runtime = runtime;
        this.source = source;
        this.node = node;
    }
    @Override
    public int getEndLine() {
        // FIXME: need valid end line
        return 0;
    }

    @Override
    public List<String> getMethodData() {
        List<String> ivarNames = new ArrayList<>();

        node.statements.accept(new AbstractNodeVisitor<Object>() {
            @Override
            protected Object defaultVisit(Node node) {
                if (node == null) return null;

                if (node instanceof InstanceVariableReadNode) {
                    ivarNames.add(runtime.newSymbol(byteListFrom(node)).idString());
                } else if (node instanceof InstanceVariableWriteNode) {
                    ivarNames.add(runtime.newSymbol(byteListFrom(((InstanceVariableWriteNode) node).name_loc)).idString());
                }

                Node[] children = node.childNodes();

                for (int i = 0; i < children.length; i++) {
                    defaultVisit(children[i]);
                }

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
    public Node getMethodArgs() {
        return node.parameters;
    }

    @Override
    public Node getMethodBody() {
        return node.statements;
    }

    @Override
    public IRBuilder<Node, DefNode, WhenNode> getBuilder(IRManager manager, IRMethod methodScope) {
        IRBuilder<Node, DefNode, WhenNode> builder = IRBuilder.newIRBuilder(manager, methodScope, null, node);

        ((IRBuilderYARP) builder).source = source;

        return builder;
    }

    private ByteList byteListFrom(Nodes.Location location) {
        return new ByteList(source, location.startOffset, location.endOffset - location.startOffset);
    }

    private ByteList byteListFrom(Node node) {
        return new ByteList(source, node.startOffset, node.endOffset - node.startOffset);
    }

}

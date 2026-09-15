package org.jruby.prism.builder;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.builder.IRBuilder;
import org.jruby.ir.builder.LazyMethodDefinition;
import org.jruby.util.ByteList;
import org.ruby_lang.prism.AbstractNodeVisitor;
import org.ruby_lang.prism.Nodes;
import org.ruby_lang.prism.Nodes.ConstantPathNode;
import org.ruby_lang.prism.Nodes.DefNode;
import org.ruby_lang.prism.Nodes.InstanceVariableReadNode;
import org.ruby_lang.prism.Nodes.InstanceVariableWriteNode;
import org.ruby_lang.prism.Nodes.Node;
import org.ruby_lang.prism.Nodes.RescueNode;
import org.ruby_lang.prism.Nodes.WhenNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import static org.jruby.api.Convert.asSymbol;

public class LazyMethodDefinitionPrism implements LazyMethodDefinition<Node, DefNode, WhenNode, RescueNode, ConstantPathNode, Nodes.HashPatternNode> {
    private final Ruby runtime;
    private final Nodes.Source nodeSource;
    private DefNode node;
    private byte[] source;
    private IdentityHashMap<byte[], RubySymbol> symbols;

    final private Encoding encoding;

    public LazyMethodDefinitionPrism(Ruby runtime, byte[] source, Nodes.Source nodeSource, Encoding encoding,
                                     DefNode node, IdentityHashMap<byte[], RubySymbol> symbols) {
        this.runtime = runtime;
        this.source = source;
        this.node = node;
        this.nodeSource = nodeSource;
        this.encoding = encoding;
        this.symbols = symbols;
    }
    @Override
    public int getEndLine() {
        return nodeSource.line(node.endOffset()) - 1;
    }

    @Override
    public List<String> getMethodData() {
        List<String> ivarNames = new ArrayList<>();
        var context = runtime.getCurrentContext();

        if (node.body != null) {
            node.body.accept(new AbstractNodeVisitor<Object>() {
                @Override
                protected Object defaultVisit(Node node) {
                    if (node == null) return null;

                    if (node instanceof InstanceVariableReadNode ivar) {
                        ivarNames.add(asSymbol(context, new ByteList(ivar.name, encoding)).idString());
                    } else if (node instanceof InstanceVariableWriteNode ivar) {
                        ivarNames.add(asSymbol(context, new ByteList(ivar.name, encoding)).idString());
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
    public Node getMethodBody() {
        return node.body;
    }

    @Override
    public IRBuilder<Node, DefNode, WhenNode, RescueNode, ConstantPathNode, Nodes.HashPatternNode> getBuilder(IRManager manager, IRMethod methodScope) {
        IRBuilder<Node, DefNode, WhenNode, RescueNode, ConstantPathNode, Nodes.HashPatternNode> builder = manager.getBuilderFactory().newIRBuilder(manager, methodScope, null, encoding);

        ((IRBuilderPrism) builder).setSymbols(symbols);
        ((IRBuilderPrism) builder).setSourceFrom(nodeSource, source);

        return builder;
    }
}

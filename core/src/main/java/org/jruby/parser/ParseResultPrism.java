package org.jruby.parser;

import org.jruby.ParseResult;
import org.jruby.ir.builder.IRBuilderPrism;
import org.jruby.runtime.DynamicScope;
import org.prism.Nodes;

public class ParseResultPrism implements ParseResult {
    StaticScope rootScope;
    Nodes.ProgramNode root;

    Nodes.Source nodeSource;
    String fileName;
    byte[] source;

    public ParseResultPrism(String fileName, byte[] source, Nodes.ProgramNode root, Nodes.Source nodeSource) {
        this.root = root;
        this.fileName = fileName;
        this.source = source;
        this.nodeSource = nodeSource;
    }

    public DynamicScope getDynamicScope() {
        return null;
    }

    // This is only used for non-eval uses.  Eval sets its own and builds through a different code path.
    @Override
    public StaticScope getStaticScope() {
        if (rootScope == null) {
            rootScope = IRBuilderPrism.createStaticScopeFrom(fileName, root.locals, StaticScope.Type.LOCAL, null);
        }

        return rootScope;
    }

    @Override
    public int getLine() {
        return 0;
    }

    @Override
    public String getFile() {
        return fileName;
    }

    // FIXME: Missing
    @Override
    public int getCoverageMode() {
        return 0;
    }

    public Nodes.ProgramNode getRoot() {
        return root;
    }

    public byte[] getSource() {
        return source;
    }

    public Object getAST() {
        return root;
    }

    public Nodes.Source getSourceNode() {
        return nodeSource;
    }
}

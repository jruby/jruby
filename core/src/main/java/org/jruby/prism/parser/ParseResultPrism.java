package org.jruby.prism.parser;

import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.prism.builder.IRBuilderPrism;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.prism.Nodes;

public class ParseResultPrism implements ParseResult {
    final Encoding encoding;
    StaticScope rootScope;
    final Nodes.ProgramNode root;

    final Nodes.Source nodeSource;
    final String fileName;
    final byte[] source;
    final int coverageMode;

    DynamicScope toplevelScope;

    public ParseResultPrism(String fileName, byte[] source, Nodes.ProgramNode root, Nodes.Source nodeSource,
                            Encoding encoding, int coverageMode) {
        this.root = root;
        this.fileName = fileName;
        this.source = source;
        this.nodeSource = nodeSource;
        this.encoding = encoding;
        this.coverageMode = coverageMode;
    }

    public void setDynamicScope(DynamicScope scope) {
        this.toplevelScope = scope;
        this.rootScope = scope.getStaticScope();
    }

    public DynamicScope getDynamicScope() {
        if (rootScope == null) getStaticScope();
        return toplevelScope;
    }

    // This is only used for non-eval uses.  Eval sets its own and builds through a different code path.
    @Override
    public StaticScope getStaticScope() {
        if (rootScope == null) {
            rootScope = IRBuilderPrism.createStaticScopeFrom(fileName, root.locals, StaticScope.Type.LOCAL, null);
            toplevelScope = DynamicScope.newDynamicScope(rootScope);
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

    @Override
    public int getCoverageMode() {
        return coverageMode;
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

    public Encoding getEncoding() {
        return encoding;
    }
}

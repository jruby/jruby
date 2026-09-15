package org.jruby.prism.parser;

import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.parser.StaticScope;
import org.jruby.prism.builder.IRBuilderPrism;
import org.jruby.runtime.DynamicScope;
import org.jruby.util.ByteList;
import org.ruby_lang.prism.Nodes;

import java.util.IdentityHashMap;

import static org.jruby.api.Convert.asSymbol;

public class ParseResultPrism implements ParseResult {
    final Encoding encoding;
    StaticScope rootScope;
    Nodes.ProgramNode root;
    IdentityHashMap<byte[], RubySymbol> symbols = new IdentityHashMap<>();

    final Nodes.Source nodeSource;
    final String fileName;
    final byte[] source;
    final int coverageMode;
    final Ruby runtime;

    DynamicScope toplevelScope;

    public ParseResultPrism(Ruby runtime, String fileName, byte[] source, Nodes.ProgramNode root,
                            Nodes.Source nodeSource, Encoding encoding, int coverageMode) {
        this.runtime = runtime;
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
            rootScope = IRBuilderPrism.createStaticScopeFrom(fileName, symbols(root.locals), StaticScope.Type.LOCAL, null);
            rootScope.setModule(runtime.getObject());
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

    public IdentityHashMap<byte[], RubySymbol> getSymbols() {
        return symbols;
    }

    public Nodes.ProgramNode getRoot() {
        return root;
    }

    public void setRoot(Nodes.ProgramNode root) {
        this.root = root;
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

    public RubySymbol[] symbols(byte[][] tokens) {
        var context = runtime.getCurrentContext();
        var names = new RubySymbol[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            names[i] = asSymbol(context, new ByteList(tokens[i], encoding));
        }
        return names;
    }
}

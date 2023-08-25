package org.yarp;

import org.jruby.ParseResult;
import org.jruby.ir.builder.IRBuilderYARP;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public class YarpParseResult implements ParseResult {
    StaticScope rootScope;
    Nodes.ProgramNode root;
    String fileName;
    byte[] source;

    public YarpParseResult( String fileName, byte[] source, Nodes.ProgramNode root) {
        this.root = root;
        this.fileName = fileName;
        this.source = source;
    }

    public DynamicScope getDynamicScope() {
        return null;
    }

    @Override
    public StaticScope getStaticScope() {
        // FIXME: How does this work for evals?
        if (rootScope == null) rootScope = IRBuilderYARP.createStaticScopeFrom(fileName, root.locals, StaticScope.Type.LOCAL, null);
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
}

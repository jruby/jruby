/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.ArrayParseNode;
import org.jruby.truffle.parser.ast.BlockArgParseNode;
import org.jruby.truffle.parser.ast.BlockParseNode;
import org.jruby.truffle.parser.ast.ClassVarAsgnParseNode;
import org.jruby.truffle.parser.ast.ClassVarDeclParseNode;
import org.jruby.truffle.parser.ast.DAsgnParseNode;
import org.jruby.truffle.parser.ast.KeywordRestArgParseNode;
import org.jruby.truffle.parser.ast.ListParseNode;
import org.jruby.truffle.parser.ast.LocalAsgnParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.OptArgParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.RestArgParseNode;
import org.jruby.truffle.parser.ast.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects paramter names from a JRuby AST.
 */
public class ParameterCollector extends AbstractNodeVisitor<Object> {

    private final List<String> parameters = new ArrayList<>();

    public List<String> getParameters() {
        return new ArrayList<>(parameters);
    }

    @Override
    protected Object defaultVisit(ParseNode node) {
       return null;
    }

    @Override
    public Object visitArgsNode(ArgsParseNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitArgumentNode(ArgumentParseNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitArrayNode(ArrayParseNode node) {
        visitChildren(node);
        return null;
    }
    @Override
    public Object visitBlockArgNode(BlockArgParseNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitBlockNode(BlockParseNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitClassVarAsgnNode(ClassVarAsgnParseNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitClassVarDeclNode(ClassVarDeclParseNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDAsgnNode(DAsgnParseNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitListNode(ListParseNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgnParseNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitOptArgNode(OptArgParseNode node) {
        parameters.add(node.getName());
        node.getValue().accept(this);
        return null;
    }

    @Override
    public Object visitLocalAsgnNode(LocalAsgnParseNode node) {
        parameters.add(node.getName());
        node.getValueNode().accept(this);
        return null;
    }

    @Override
    public Object visitRestArgNode(RestArgParseNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitKeywordRestArgNode(KeywordRestArgParseNode node) {
        parameters.add(node.getName());
        return null;
    }

}

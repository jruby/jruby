/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import org.jruby.ast.*;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects paramter names from a JRuby AST.
 */
public class ParameterCollector extends AbstractNodeVisitor<Object> {

    private final List<String> parameters = new ArrayList<>();
    private final List<String> keywords = new ArrayList<>();

    public List<String> getParameters() {
        return new ArrayList<>(parameters);
    }

    @Override
    protected Object defaultVisit(Node node) {
       return null;
    }

    @Override
    public Object visitArgsNode(ArgsNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitKeywordArgNode(KeywordArgNode node) {
        keywords.add(((INameNode) node.childNodes().get(0)).getName());
        return null;
    }

    @Override
    public Object visitArgumentNode(ArgumentNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitArrayNode(ArrayNode node) {
        visitChildren(node);
        return null;
    }
    @Override
    public Object visitBlockArgNode(BlockArgNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitBlockNode(BlockNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitClassVarAsgnNode(ClassVarAsgnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitClassVarDeclNode(ClassVarDeclNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDAsgnNode(DAsgnNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitListNode(ListNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgnNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitOptArgNode(OptArgNode node) {
        parameters.add(node.getName());
        node.getValue().accept(this);
        return null;
    }

    @Override
    public Object visitLocalAsgnNode(LocalAsgnNode node) {
        parameters.add(node.getName());
        node.getValueNode().accept(this);
        return null;
    }

    @Override
    public Object visitRestArgNode(RestArgNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitKeywordRestArgNode(KeywordRestArgNode node) {
        parameters.add(node.getName());
        return null;
    }

    public String[] getKeywords() {
        return keywords.toArray(new String[keywords.size()]);
    }

}

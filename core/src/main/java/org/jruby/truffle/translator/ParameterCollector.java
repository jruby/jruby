/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import org.jruby.ast.*;
import org.jruby.ast.visitor.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects paramter names from a JRuby AST.
 */
public class ParameterCollector implements NodeVisitor {

    private final List<String> parameters = new ArrayList<>();

    public List<String> getParameters() {
        return new ArrayList<>(parameters);
    }

    @Override
    public Object visitAliasNode(AliasNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitAndNode(AndNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitArgsNode(ArgsNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitArgsCatNode(ArgsCatNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitArgsPushNode(ArgsPushNode node) {
        throw new UnsupportedOperationException(node.toString());
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
    public Object visitAttrAssignNode(AttrAssignNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitBackRefNode(BackRefNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitBeginNode(BeginNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitBignumNode(BignumNode node) {
        throw new UnsupportedOperationException(node.toString());
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
    public Object visitBlockPassNode(BlockPassNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitBreakNode(BreakNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitConstDeclNode(ConstDeclNode node) {
        throw new UnsupportedOperationException(node.toString());
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
    public Object visitClassVarNode(ClassVarNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitCallNode(CallNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitCaseNode(CaseNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitClassNode(ClassNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitColon2Node(Colon2Node node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitColon3Node(Colon3Node node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitComplexNode(ComplexNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitConstNode(ConstNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDAsgnNode(DAsgnNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitDRegxNode(DRegexpNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDStrNode(DStrNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDSymbolNode(DSymbolNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDVarNode(DVarNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDXStrNode(DXStrNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDefinedNode(DefinedNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDefnNode(DefnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDefsNode(DefsNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitDotNode(DotNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitEncodingNode(EncodingNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitEnsureNode(EnsureNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitEvStrNode(EvStrNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitFCallNode(FCallNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitFalseNode(FalseNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitFixnumNode(FixnumNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitFlipNode(FlipNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitFloatNode(FloatNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitForNode(ForNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitGlobalAsgnNode(GlobalAsgnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitGlobalVarNode(GlobalVarNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitHashNode(HashNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitInstAsgnNode(InstAsgnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitInstVarNode(InstVarNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitIfNode(IfNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitIterNode(IterNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitKeywordArgNode(KeywordArgNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitKeywordRestArgNode(KeywordRestArgNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitLambdaNode(LambdaNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitListNode(ListNode node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitLiteralNode(LiteralNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitLocalAsgnNode(LocalAsgnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitLocalVarNode(LocalVarNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgn19Node node) {
        visitChildren(node);
        return null;
    }

    @Override
    public Object visitMatch2Node(Match2Node node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitMatch3Node(Match3Node node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitMatchNode(MatchNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitModuleNode(ModuleNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitNewlineNode(NewlineNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitNextNode(NextNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitNilNode(NilNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitNthRefNode(NthRefNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitOpElementAsgnNode(OpElementAsgnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitOpAsgnNode(OpAsgnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitOpAsgnAndNode(OpAsgnAndNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitOpAsgnOrNode(OpAsgnOrNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitOptArgNode(OptArgNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitOrNode(OrNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitPreExeNode(PreExeNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitPostExeNode(PostExeNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRationalNode(RationalNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRedoNode(RedoNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRegexpNode(RegexpNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRescueBodyNode(RescueBodyNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRescueNode(RescueNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRestArgNode(RestArgNode node) {
        parameters.add(node.getName());
        return null;
    }

    @Override
    public Object visitRetryNode(RetryNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitReturnNode(ReturnNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitRootNode(RootNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitSClassNode(SClassNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitSelfNode(SelfNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitSplatNode(SplatNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitStrNode(StrNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitSuperNode(SuperNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitSValueNode(SValueNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitSymbolNode(SymbolNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitToAryNode(ToAryNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitTrueNode(TrueNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitUndefNode(UndefNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitUntilNode(UntilNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitVAliasNode(VAliasNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitVCallNode(VCallNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitWhenNode(WhenNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitWhileNode(WhileNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitXStrNode(XStrNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitYieldNode(YieldNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitZArrayNode(ZArrayNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    @Override
    public Object visitZSuperNode(ZSuperNode node) {
        throw new UnsupportedOperationException(node.toString());
    }

    private void visitChildren(Node node) {
        for (Node child: node.childNodes()) {
            if (child != null) {
                child.accept(this);
            }
        }
    }

}

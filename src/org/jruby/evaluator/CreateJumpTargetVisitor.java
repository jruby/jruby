/*
 * Copyright (C) 2004 Jan Arne Petersen
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * 
 */
package org.jruby.evaluator;

import org.jruby.ast.*;
import org.jruby.ast.visitor.NodeVisitor;

import java.util.Iterator;

public class CreateJumpTargetVisitor implements NodeVisitor {
    private Object target;

    public CreateJumpTargetVisitor(Object target) {
        this.target = target;
    }
    
    private void visit(Node node) {
        if (node != null) {
            node.accept(this);
        }
    }

    public void visitAliasNode(AliasNode iVisited) {
    }

    public void visitAndNode(AndNode iVisited) {
        visit(iVisited.getFirstNode());
        visit(iVisited.getSecondNode());
    }


    public void visitArgsNode(ArgsNode iVisited) {
        // FIXME
    }

    public void visitArgsCatNode(ArgsCatNode iVisited) {
        visit(iVisited.getFirstNode());
        visit(iVisited.getSecondNode());
    }
    public void visitArrayNode(ArrayNode iVisited) {
        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            visit((Node) iterator.next());
        }

    }

    public void visitAttrSetNode(AttrSetNode iVisited) {
    }

    public void visitBackRefNode(BackRefNode iVisited) {
    }

    public void visitBeginNode(BeginNode iVisited) {
        // FIXME
        visit(iVisited.getBodyNode());
    }

    public void visitBlockArgNode(BlockArgNode iVisited) {
    }

    public void visitBlockNode(BlockNode iVisited) {
        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            visit((Node) iterator.next());
        }
    }

    public void visitBlockPassNode(BlockPassNode iVisited) {
        visit(iVisited.getBodyNode());
        visit(iVisited.getIterNode());
    }

    public void visitBreakNode(BreakNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }

    public void visitConstDeclNode(ConstDeclNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }


    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }

    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }


    public void visitClassVarNode(ClassVarNode iVisited) {
    }

    public void visitCallNode(CallNode iVisited) {
        // FIXME
        visit(iVisited.getReceiverNode());
    }

    public void visitCaseNode(CaseNode iVisited) {
        visit(iVisited.getCaseNode());
        visit(iVisited.getFirstWhenNode());
    }

    public void visitClassNode(ClassNode iVisited) {
        // FIXME
        visit(iVisited.getSuperNode());
        visit(iVisited.getBodyNode());
    }

    public void visitColon2Node(Colon2Node iVisited) {
        visit(iVisited.getLeftNode());
    }

    public void visitColon3Node(Colon3Node iVisited) {
    }

    public void visitConstNode(ConstNode iVisited) {
    }

    public void visitDAsgnNode(DAsgnNode iVisited) {
        // FIXME 
        visit(iVisited.getValueNode());
    }

    public void visitDRegxNode(DRegexpNode iVisited) {
        // FIXME
        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            visit((Node) iterator.next());
        }
    }


    public void visitDStrNode(DStrNode iVisited) {
        // FIXME
        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            visit((Node) iterator.next());
        }
    }

    public void visitDSymbolNode(DSymbolNode iVisited) {
        // FIXME
        Iterator iterator = iVisited.getNode().iterator();
        while (iterator.hasNext()) {
            visit((Node) iterator.next());
        }
    }

    public void visitDVarNode(DVarNode iVisited) {
    }

    public void visitDXStrNode(DXStrNode iVisited) {
        // FIXME
        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            visit((Node) iterator.next());
        }
    }

    public void visitDefinedNode(DefinedNode iVisited) {
        // FIXME
        visit(iVisited.getExpressionNode());
    }

    public void visitDefnNode(DefnNode iVisited) {
    }

    public void visitDefsNode(DefsNode iVisited) {
    }

    public void visitDotNode(DotNode iVisited) {
        visit(iVisited.getBeginNode());
        visit(iVisited.getEndNode());
    }

    public void visitEnsureNode(EnsureNode iVisited) {
        visit(iVisited.getBodyNode());
        visit(iVisited.getEnsureNode());
    }

    public void visitEvStrNode(EvStrNode iVisited) {
        visit(iVisited.getBody());
    }

    public void visitFCallNode(FCallNode iVisited) {
    }

    public void visitFalseNode(FalseNode iVisited) {
    }

    public void visitFlipNode(FlipNode iVisited) {
        visit(iVisited.getBeginNode());
        visit(iVisited.getEndNode());
    }

    public void visitForNode(ForNode iVisited) {
        visit(iVisited.getBodyNode());
        visit(iVisited.getIterNode());
        // FIXME
        visit(iVisited.getVarNode());
    }

    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }

    public void visitGlobalVarNode(GlobalVarNode iVisited) {
    }

    public void visitHashNode(HashNode iVisited) {
        // FIXME
        if (iVisited.getListNode() != null) {
            Iterator iterator = iVisited.getListNode().iterator();
            while (iterator.hasNext()) {
                visit((Node) iterator.next());
            }
        }
    }

    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }


    public void visitInstVarNode(InstVarNode iVisited) {
    }

    public void visitIfNode(IfNode iVisited) {
        // FIXME
        visit(iVisited.getCondition());
        visit(iVisited.getThenBody());
        visit(iVisited.getElseBody());
    }

    public void visitIterNode(IterNode iVisited) {
        visit(iVisited.getBodyNode());
        visit(iVisited.getIterNode());
        // FIXME
        visit(iVisited.getVarNode());
    }

    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }

    public void visitLocalVarNode(LocalVarNode iVisited) {
    }

    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        // FIXME
        visit(iVisited.getValueNode());
    }

    public void visitMatch2Node(Match2Node iVisited) {
        // FIXME
        visit(iVisited.getReceiverNode());
        // FIXME
        visit(iVisited.getValueNode());
    }

    public void visitMatch3Node(Match3Node iVisited) {
        // FIXME
        visit(iVisited.getReceiverNode());
        // FIXME
        visit(iVisited.getValueNode());
    }

    public void visitMatchNode(MatchNode iVisited) {
        visit(iVisited.getRegexpNode());
    }

    public void visitModuleNode(ModuleNode iVisited) {
        visit(iVisited.getBodyNode());
    }

    public void visitNewlineNode(NewlineNode iVisited) {
        visit(iVisited.getNextNode());
    }

    public void visitNextNode(NextNode iVisited) {
        // FIXME
    }

    public void visitNilNode(NilNode iVisited) {
    }

    public void visitNotNode(NotNode iVisited) {
        // FIXME
        visit(iVisited.getConditionNode());
    }

    public void visitNthRefNode(NthRefNode iVisited) {
    }

    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        // FIXME
        visit(iVisited.getReceiverNode());
        visit(iVisited.getValueNode());
    }

    public void visitOpAsgnNode(OpAsgnNode iVisited) {
        // FIXME
        visit(iVisited.getReceiverNode());
        visit(iVisited.getValueNode());
    }

    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        visit(iVisited.getFirstNode());
        visit(iVisited.getSecondNode());
    }

    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        visit(iVisited.getFirstNode());
        visit(iVisited.getSecondNode());
    }

    public void visitOptNNode(OptNNode iVisited) {
        visit(iVisited.getBodyNode());
    }

    public void visitOrNode(OrNode iVisited) {
        visit(iVisited.getFirstNode());
        visit(iVisited.getSecondNode());
    }

    public void visitPostExeNode(PostExeNode iVisited) {
    }

    public void visitRedoNode(RedoNode iVisited) {
    }

    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        visit(iVisited.getBodyNode());
        visit(iVisited.getExceptionNodes());
        visit(iVisited.getOptRescueNode());
    }

    public void visitRescueNode(RescueNode iVisited) {
        visit(iVisited.getBodyNode());
        visit(iVisited.getElseNode());
        visit(iVisited.getRescueNode());        
    }

    public void visitRetryNode(RetryNode iVisited) {
    }

    public void visitReturnNode(ReturnNode iVisited) {
        iVisited.setTarget(target);
    }

    public void visitSClassNode(SClassNode iVisited) {
        // FIXME
        visit(iVisited.getReceiverNode());
        visit(iVisited.getBodyNode());
    }

    public void visitScopeNode(ScopeNode iVisited) {
        visit(iVisited.getBodyNode());
    }

    public void visitSelfNode(SelfNode iVisited) {
    }
    
    public void visitSplatNode(SplatNode iVisited) {
        visit(iVisited.getValue());
    }
    
    public void visitStrNode(StrNode iVisited) {
    }
    
    public void visitSValueNode(SValueNode iVisited) {
        // FIXME
        visit(iVisited.getValue());
    }

    public void visitSuperNode(SuperNode iVisited) {
    }

    public void visitToAryNode(ToAryNode iVisited) {
        // FIXME
        visit(iVisited.getValue());
    }
    
    public void visitTrueNode(TrueNode iVisited) {
    }

    public void visitUndefNode(UndefNode iVisited) {
    }

    public void visitUntilNode(UntilNode iVisited) {
        // FIXME
        visit(iVisited.getConditionNode());
        visit(iVisited.getBodyNode());
    }

    public void visitVAliasNode(VAliasNode iVisited) {
    }

    public void visitVCallNode(VCallNode iVisited) {
    }

    public void visitWhenNode(WhenNode iVisited) {
        visit(iVisited.getBodyNode());
        visit(iVisited.getExpressionNodes());
        visit(iVisited.getNextCase());
    }

    public void visitWhileNode(WhileNode iVisited) {
        // FIXME
        visit(iVisited.getConditionNode());
        visit(iVisited.getBodyNode());
    }

    public void visitXStrNode(XStrNode iVisited) {
   }

    public void visitYieldNode(YieldNode iVisited) {
    }

    public void visitZArrayNode(ZArrayNode iVisited) {
    }

    public void visitZSuperNode(ZSuperNode iVisited) {
    }

    public void visitBignumNode(BignumNode iVisited) {
    }

    public void visitFixnumNode(FixnumNode iVisited) {
    }

    public void visitFloatNode(FloatNode iVisited) {
    }

    public void visitRegexpNode(RegexpNode iVisited) {
    }

    public void visitSymbolNode(SymbolNode iVisited) {
    }
}
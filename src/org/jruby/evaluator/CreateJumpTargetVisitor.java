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

import java.util.Iterator;

import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrSetNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.visitor.NodeVisitor;

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
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

import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.visitor.AbstractVisitor;

public class CreateJumpTargetVisitor extends AbstractVisitor {
    private Object target;

    public CreateJumpTargetVisitor(Object target) {
        this.target = target;
    }
    
    private void visit(Node node) {
        if (node != null) {
            node.accept(this);
        }
    }

    public void visitAndNode(AndNode iVisited) {
        visit(iVisited.getFirstNode());
        visit(iVisited.getSecondNode());
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

    public void visitBeginNode(BeginNode iVisited) {
        // FIXME
        visit(iVisited.getBodyNode());
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

    public void visitNotNode(NotNode iVisited) {
        // FIXME
        visit(iVisited.getConditionNode());
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

    public void visitSplatNode(SplatNode iVisited) {
        visit(iVisited.getValue());
    }
    
    public void visitSValueNode(SValueNode iVisited) {
        // FIXME
        visit(iVisited.getValue());
    }

    public void visitToAryNode(ToAryNode iVisited) {
        // FIXME
        visit(iVisited.getValue());
    }
    
    public void visitUntilNode(UntilNode iVisited) {
        // FIXME
        visit(iVisited.getConditionNode());
        visit(iVisited.getBodyNode());
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

    protected void visitNode(Node iVisited) {
        // do nothing
    }
}
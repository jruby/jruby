/*
 * DefaultIteratorVisitor.java - a default iterator visitor
 * Created on 05. November 2001, 21:46
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.ast.visitor;

import java.util.*;

import org.ablaf.ast.*;
import org.jruby.ast.*;
import org.jruby.ast.ExpandArrayNode;

/**
 * Default iterator visitor.
 * This visitor will iterate over all the nodes using the semantic
 * which is used when compiling or interpreting the tree.
 * This visitor will then used call the accept method for each node with
 * its payload as the visitor to accept.
 * @see NodeVisitor
 * @author Benoit Cerrina
 * @version $Revision$
 **/
public class DefaultIteratorVisitor implements NodeVisitor {
    protected NodeVisitor _Payload;

    /**
     * Constructs a DefaultIteratorVisitor.
     * The payload visitor will be accepted by each node wich the
     * IteratorVisitor iterates over.
     * @param iPayload the payload for this visitor
     **/
    public DefaultIteratorVisitor(NodeVisitor iPayload) {
        _Payload = iPayload;
    }

    public void visitAliasNode(AliasNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitAndNode(AndNode iVisited) {
        iVisited.getFirstNode().accept(this);
        iVisited.accept(_Payload);
        iVisited.getSecondNode().accept(this);
    }


    public void visitArgsNode(ArgsNode iVisited) {
        iVisited.accept(_Payload);
        if (iVisited.getOptArgs() != null) {
            iVisited.getOptArgs().accept(this);
        }
    }


    public void visitArrayNode(ArrayNode iVisited) {
        iVisited.accept(_Payload);
        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            ((INode) iterator.next()).accept(this);
        }

    }

    public void visitAttrSetNode(AttrSetNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitBackRefNode(BackRefNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitBeginNode(BeginNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitBlockArgNode(BlockArgNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitBlockNode(BlockNode iVisited) {
        iVisited.accept(_Payload);
        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            ((INode) iterator.next()).accept(this);
        }
    }

    public void visitBlockPassNode(BlockPassNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitBreakNode(BreakNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitConstDeclNode(ConstDeclNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getValueNode().accept(this);
    }


    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getValueNode().accept(this);
    }

    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        iVisited.accept(_Payload);
    }


    public void visitClassVarNode(ClassVarNode iVisited) {
        iVisited.accept(_Payload);
    }
	/**
	 * @fixme iteration not correctly defined
	 **/
    public void visitCallNode(CallNode iVisited) {
        iVisited.getReceiverNode().accept(this);
        //  FIXME
        /*for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
            node.getHeadNode().accept(this);
        }*/
        iVisited.accept(_Payload);
    }

    public void visitCaseNode(CaseNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitClassNode(ClassNode iVisited) {
        iVisited.accept(_Payload);
        if (iVisited.getSuperNode() != null) {
            iVisited.getSuperNode().accept(this);
        }
        //NOTE: suprised that this is not used
        // It can be used.
        iVisited.getBodyNode().accept(this);
    }

    public void visitColon2Node(Colon2Node iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitColon3Node(Colon3Node iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitConstNode(ConstNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitDAsgnNode(DAsgnNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getValueNode().accept(this);
    }

    public void visitDRegxNode(DRegexpNode iVisited) {
        iVisited.accept(_Payload);
    }


    public void visitDStrNode(DStrNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitDVarNode(DVarNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitDXStrNode(DXStrNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitDefinedNode(DefinedNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitDefnNode(DefnNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getBodyNode().accept(this);
    }

    public void visitDefsNode(DefsNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getReceiverNode().accept(this);
        iVisited.getBodyNode().accept(this);
    }

    public void visitDotNode(DotNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitEnsureNode(EnsureNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitEvStrNode(EvStrNode iVisited) {
        iVisited.accept(_Payload);
    }

	/** @fixme iteration not correctly defined*/
    public void visitFCallNode(FCallNode iVisited) {
        iVisited.accept(_Payload);
        // FIXME
        /*for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
            node.getHeadNode().accept(this);
        }*/
    }

    public void visitFalseNode(FalseNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitFlipNode(FlipNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitForNode(ForNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getValueNode().accept(this);
    }

    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitHashNode(HashNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getValueNode().accept(this);
    }


    public void visitInstVarNode(InstVarNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitIfNode(IfNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getCondition().accept(this);
        iVisited.getThenBody().accept(this);
        if (iVisited.getElseBody() != null) {
            iVisited.getElseBody().accept(this);
        }
    }

    public void visitIterNode(IterNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getValueNode().accept(this);
    }

    public void visitLocalVarNode(LocalVarNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitMatch2Node(Match2Node iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitMatch3Node(Match3Node iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitMatchNode(MatchNode iVisited) {
        iVisited.accept(_Payload);
    }


    public void visitModuleNode(ModuleNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getBodyNode().accept(this);
    }

    public void visitNewlineNode(NewlineNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getNextNode().accept(this);
    }

    public void visitNextNode(NextNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitNilNode(NilNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitNotNode(NotNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getConditionNode().accept(this);
    }

    public void visitNthRefNode(NthRefNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitOpAsgnNode(OpAsgnNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitOptNNode(OptNNode iVisited) {
        iVisited.accept(_Payload);
        iVisited.getBodyNode().accept(this);
    }

    public void visitOrNode(OrNode iVisited) {
        iVisited.getFirstNode().accept(this);
        iVisited.accept(_Payload);
        iVisited.getSecondNode().accept(this);
    }

    public void visitPostExeNode(PostExeNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitRedoNode(RedoNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        iVisited.accept(_Payload);
        // XXX iVisited.getBodyNode().accept(this);
    }

    public void visitRescueNode(RescueNode iVisited) {
        iVisited.accept(_Payload);
        /* XXX iVisited.getHeadNode().accept(this);
        Node lElseNode = iVisited.getElseNode();
        if (lElseNode != null)
            lElseNode.accept(this);
        for (Node body = iVisited.getResqNode(); body != null; body = iVisited.getHeadNode()) {
            Node lArgsNode = body.getArgsNode();
            for (; lArgsNode != null; lArgsNode = lArgsNode.getNextNode())
                lArgsNode.getHeadNode().accept(this);
            body.accept(this);
        }*/
    }

    public void visitRestArgsNode(RestArgsNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitRetryNode(RetryNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitReturnNode(ReturnNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitSClassNode(SClassNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitScopeNode(ScopeNode iVisited) {
        iVisited.accept(_Payload);
        if (iVisited.getBodyNode() != null) {
            iVisited.getBodyNode().accept(this);
        }
    }

    public void visitSelfNode(SelfNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitStrNode(StrNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitSuperNode(SuperNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitTrueNode(TrueNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitUndefNode(UndefNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitUntilNode(UntilNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitVAliasNode(VAliasNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitVCallNode(VCallNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitWhenNode(WhenNode iVisited) {
        iVisited.accept(_Payload);
        /* XXX iVisited.getConditionNode().accept(this);
        iVisited.getBodyNode().accept(this);
        */
    }

    public void visitWhileNode(WhileNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitXStrNode(XStrNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitYieldNode(YieldNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitZArrayNode(ZArrayNode iVisited) {
        iVisited.accept(_Payload);
    }

    public void visitZSuperNode(ZSuperNode iVisited) {
        iVisited.accept(_Payload);
    }

    /**
     * @see NodeVisitor#visitBignumNode(BignumNode)
     */
    public void visitBignumNode(BignumNode iVisited) {
        iVisited.accept(_Payload);
    }

    /**
     * @see NodeVisitor#visitFixnumNode(FixnumNode)
     */
    public void visitFixnumNode(FixnumNode iVisited) {
        iVisited.accept(_Payload);
    }

    /**
     * @see NodeVisitor#visitFloatNode(FloatNode)
     */
    public void visitFloatNode(FloatNode iVisited) {
        iVisited.accept(_Payload);
    }

    /**
     * @see NodeVisitor#visitRegexpNode(RegexpNode)
     */
    public void visitRegexpNode(RegexpNode iVisited) {
        iVisited.accept(_Payload);
    }

    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitSymbolNode(SymbolNode iVisited) {
        iVisited.accept(_Payload);
    }

    /**
     * @see NodeVisitor#visitExpandArrayNode(ExpandArrayNode)
     */
    public void visitExpandArrayNode(ExpandArrayNode iVisited) {
        iVisited.accept(_Payload);
    }
}

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
package org.jruby.nodes.visitor;

import org.jruby.nodes.*;

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
	public DefaultIteratorVisitor(NodeVisitor iPayload)
	{_Payload = iPayload;}


	public void visitAliasNode(AliasNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitAndNode(AndNode iVisited) {
		iVisited.getFirstNode().accept(this);
		iVisited.accept(_Payload);
		iVisited.getSecondNode().accept(this);
	}

	public void visitArgsCatNode(ArgsCatNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitArgsNode(ArgsNode iVisited) {
		iVisited.accept(_Payload);
		Node lOptNode = iVisited.getOptNode();
		if (lOptNode != null) {
			lOptNode.accept(this);
		}
	}

	public void visitArgsPushNode(ArgsPushNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitArrayNode(ArrayNode iVisited) {
		iVisited.accept(_Payload);
		for (Node node = iVisited; node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
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
		for (Node node = iVisited; node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
		}
	}

	public void visitBlockPassNode(BlockPassNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitBreakNode(BreakNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitCDeclNode(CDeclNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
	}

	public void visitCFuncNode(CFuncNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitCVAsgnNode(CVAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
	}

	public void visitCVDeclNode(CVDeclNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitCVar2Node(CVar2Node iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitCVarNode(CVarNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitCallNode(CallNode iVisited) {
		iVisited.getRecvNode().accept(this);
		for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
		}
		iVisited.accept(_Payload);
	}

	public void visitCaseNode(CaseNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitClassNode(ClassNode iVisited) {
		iVisited.accept(_Payload);
		Node lSuperNode = iVisited.getSuperNode();
		if (lSuperNode != null)
			lSuperNode.accept(this);
		//NOTE: suprised that this is not used
		//iVisited.getBodyNode().accept(this);
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

	public void visitDAsgnCurrNode(DAsgnCurrNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
	}

	public void visitDAsgnNode(DAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
	}

	public void visitDRegxNode(DRegxNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitDRegxOnceNode(DRegxOnceNode iVisited) {
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
		iVisited.getDefnNode().accept(this);
	}

	public void visitDefsNode(DefsNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getRecvNode().accept(this);
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

	public void visitFBodyNode(FBodyNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitFCallNode(FCallNode iVisited) {
		iVisited.accept(_Payload);
		for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
		}
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

	public void visitGAsgnNode(GAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
	}

	public void visitGVarNode(GVarNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitHashNode(HashNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitIAsgnNode(IAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
	}

	public void visitIFuncNode(IFuncNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitIVarNode(IVarNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitIfNode(IfNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getConditionNode().accept(this);
		iVisited.getBodyNode().accept(this);
		Node lElseNode = iVisited.getElseNode();
		if (lElseNode != null)
			iVisited.getElseNode().accept(this);
	}

	public void visitIterNode(IterNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitLAsgnNode(LAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
	}

	public void visitLVarNode(LVarNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitLitNode(LitNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitMAsgnNode(MAsgnNode iVisited) {
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

	public void visitMethodNode(MethodNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitModuleNode(ModuleNode iVisited) {
		iVisited.accept(_Payload);
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
		iVisited.getBodyNode().accept(this);
	}

	public void visitNthRefNode(NthRefNode iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitOpAsgn1Node(OpAsgn1Node iVisited) {
		iVisited.accept(_Payload);
	}

	public void visitOpAsgn2Node(OpAsgn2Node iVisited) {
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
		iVisited.getBodyNode().accept(this);
	}

	public void visitRescueNode(RescueNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getHeadNode().accept(this);
		Node lElseNode = iVisited.getElseNode();
		if (lElseNode != null)
			lElseNode.accept(this);
		for(Node body = iVisited.getResqNode(); body != null; body = iVisited.getHeadNode())
		{
			Node lArgsNode = body.getArgsNode();
			for (;lArgsNode != null ; lArgsNode = lArgsNode.getNextNode())
				lArgsNode.getHeadNode().accept(this);
			body.accept(this);
		}
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
		Node lNext = iVisited.getNextNode();
		if (lNext != null) {
			lNext.accept(this);
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
        iVisited.getConditionNode().accept(this);
        iVisited.getBodyNode().accept(this);
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
}


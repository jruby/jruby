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
		_Payload.visitAliasNode(iVisited);
	}

	public void visitAndNode(AndNode iVisited) {
		iVisited.getFirstNode().accept(this);
		_Payload.visitAndNode(iVisited);
		
		iVisited.getSecondNode().accept(this);
	}

	public void visitArgsCatNode(ArgsCatNode iVisited) {
		_Payload.visitArgsCatNode(iVisited);
	}

	public void visitArgsNode(ArgsNode iVisited) {
		_Payload.visitArgsNode(iVisited);
		Node lOptNode = iVisited.getOptNode();
		if (lOptNode != null) {
			lOptNode.accept(this);
		}
	}

	public void visitArgsPushNode(ArgsPushNode iVisited) {
		_Payload.visitArgsPushNode(iVisited);
	}

	public void visitArrayNode(ArrayNode iVisited) {
		_Payload.visitArrayNode(iVisited);
		for (Node node = iVisited; node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
		}
	}

	public void visitAttrSetNode(AttrSetNode iVisited) {
		_Payload.visitAttrSetNode(iVisited);
	}

	public void visitBackRefNode(BackRefNode iVisited) {
		_Payload.visitBackRefNode(iVisited);
	}

	public void visitBeginNode(BeginNode iVisited) {
		_Payload.visitBeginNode(iVisited);
	}

	public void visitBlockArgNode(BlockArgNode iVisited) {
		_Payload.visitBlockArgNode(iVisited);
	}

	public void visitBlockNode(BlockNode iVisited) {
		_Payload.visitBlockNode(iVisited);
		for (Node node = iVisited; node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
		}
	}

	public void visitBlockPassNode(BlockPassNode iVisited) {
		_Payload.visitBlockPassNode(iVisited);
	}

	public void visitBreakNode(BreakNode iVisited) {
		_Payload.visitBreakNode(iVisited);
	}

	public void visitCDeclNode(CDeclNode iVisited) {
		_Payload.visitCDeclNode(iVisited);
		iVisited.getValueNode().accept(this);
	}

	public void visitCFuncNode(CFuncNode iVisited) {
		_Payload.visitCFuncNode(iVisited);
	}

	public void visitCVAsgnNode(CVAsgnNode iVisited) {
		_Payload.visitCVAsgnNode(iVisited);
		iVisited.getValueNode().accept(this);
	}

	public void visitCVDeclNode(CVDeclNode iVisited) {
		_Payload.visitCVDeclNode(iVisited);
	}

	public void visitCVar2Node(CVar2Node iVisited) {
		_Payload.visitCVar2Node(iVisited);
	}

	public void visitCVarNode(CVarNode iVisited) {
		_Payload.visitCVarNode(iVisited);
	}

	public void visitCallNode(CallNode iVisited) {
		iVisited.getRecvNode().accept(this);
		for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
		}
		_Payload.visitCallNode(iVisited);
	}

	public void visitCaseNode(CaseNode iVisited) {
		_Payload.visitCaseNode(iVisited);
	}

	public void visitClassNode(ClassNode iVisited) {
		_Payload.visitClassNode(iVisited);
		Node lSuperNode = iVisited.getSuperNode();
		if (lSuperNode != null)
			lSuperNode.accept(this);
		//NOTE: suprised that this is not used
		//iVisited.getBodyNode().accept(this);
	}

	public void visitColon2Node(Colon2Node iVisited) {
		_Payload.visitColon2Node(iVisited);
	}

	public void visitColon3Node(Colon3Node iVisited) {
		_Payload.visitColon3Node(iVisited);
	}

	public void visitConstNode(ConstNode iVisited) {
		_Payload.visitConstNode(iVisited);
	}

	public void visitDAsgnCurrNode(DAsgnCurrNode iVisited) {
		_Payload.visitDAsgnCurrNode(iVisited);
		iVisited.getValueNode().accept(this);
	}

	public void visitDAsgnNode(DAsgnNode iVisited) {
		_Payload.visitDAsgnNode(iVisited);
		iVisited.getValueNode().accept(this);
	}

	public void visitDRegxNode(DRegxNode iVisited) {
		_Payload.visitDRegxNode(iVisited);
	}

	public void visitDRegxOnceNode(DRegxOnceNode iVisited) {
		_Payload.visitDRegxOnceNode(iVisited);
	}

	public void visitDStrNode(DStrNode iVisited) {
		_Payload.visitDStrNode(iVisited);
	}

	public void visitDVarNode(DVarNode iVisited) {
		_Payload.visitDVarNode(iVisited);
	}

	public void visitDXStrNode(DXStrNode iVisited) {
		_Payload.visitDXStrNode(iVisited);
	}

	public void visitDefinedNode(DefinedNode iVisited) {
		_Payload.visitDefinedNode(iVisited);
	}

	public void visitDefnNode(DefnNode iVisited) {
		_Payload.visitDefnNode(iVisited);
		iVisited.getDefnNode().accept(this);
	}

	public void visitDefsNode(DefsNode iVisited) {
		_Payload.visitDefsNode(iVisited);
		iVisited.getRecvNode().accept(this);
	}

	public void visitDotNode(DotNode iVisited) {
		_Payload.visitDotNode(iVisited);
	}

	public void visitEnsureNode(EnsureNode iVisited) {
		_Payload.visitEnsureNode(iVisited);
	}

	public void visitEvStrNode(EvStrNode iVisited) {
		_Payload.visitEvStrNode(iVisited);
	}

	public void visitFBodyNode(FBodyNode iVisited) {
		_Payload.visitFBodyNode(iVisited);
	}

	public void visitFCallNode(FCallNode iVisited) {
		_Payload.visitFCallNode(iVisited);
		for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
			node.getHeadNode().accept(this);
		}
	}

	public void visitFalseNode(FalseNode iVisited) {
		_Payload.visitFalseNode(iVisited);
	}

	public void visitFlipNode(FlipNode iVisited) {
		_Payload.visitFlipNode(iVisited);
	}

	public void visitForNode(ForNode iVisited) {
		_Payload.visitForNode(iVisited);
	}

	public void visitGAsgnNode(GAsgnNode iVisited) {
		_Payload.visitGAsgnNode(iVisited);
		iVisited.getValueNode().accept(this);
	}

	public void visitGVarNode(GVarNode iVisited) {
		_Payload.visitGVarNode(iVisited);
	}

	public void visitHashNode(HashNode iVisited) {
		_Payload.visitHashNode(iVisited);
	}

	public void visitIAsgnNode(IAsgnNode iVisited) {
		_Payload.visitIAsgnNode(iVisited);
		iVisited.getValueNode().accept(this);
	}

	public void visitIFuncNode(IFuncNode iVisited) {
		_Payload.visitIFuncNode(iVisited);
	}

	public void visitIVarNode(IVarNode iVisited) {
		_Payload.visitIVarNode(iVisited);
	}

	public void visitIfNode(IfNode iVisited) {
		_Payload.visitIfNode(iVisited);
		iVisited.getConditionNode().accept(this);
		iVisited.getBodyNode().accept(this);
		Node lElseNode = iVisited.getElseNode();
		if (lElseNode != null)
			iVisited.getElseNode().accept(this);
	}

	public void visitIterNode(IterNode iVisited) {
		_Payload.visitIterNode(iVisited);
	}

	public void visitLAsgnNode(LAsgnNode iVisited) {
		_Payload.visitLAsgnNode(iVisited);
		iVisited.getValueNode().accept(this);
	}

	public void visitLVarNode(LVarNode iVisited) {
		_Payload.visitLVarNode(iVisited);
	}

	public void visitLitNode(LitNode iVisited) {
		_Payload.visitLitNode(iVisited);
	}

	public void visitMAsgnNode(MAsgnNode iVisited) {
		_Payload.visitMAsgnNode(iVisited);
	}

	public void visitMatch2Node(Match2Node iVisited) {
		_Payload.visitMatch2Node(iVisited);
	}

	public void visitMatch3Node(Match3Node iVisited) {
		_Payload.visitMatch3Node(iVisited);
	}

	public void visitMatchNode(MatchNode iVisited) {
		_Payload.visitMatchNode(iVisited);
	}

	public void visitMethodNode(MethodNode iVisited) {
		_Payload.visitMethodNode(iVisited);
	}

	public void visitModuleNode(ModuleNode iVisited) {
		_Payload.visitModuleNode(iVisited);
	}

	public void visitNewlineNode(NewlineNode iVisited) {
		_Payload.visitNewlineNode(iVisited);
		iVisited.getNextNode().accept(this);
	}

	public void visitNextNode(NextNode iVisited) {
		_Payload.visitNextNode(iVisited);
	}

	public void visitNilNode(NilNode iVisited) {
		_Payload.visitNilNode(iVisited);
	}

	public void visitNotNode(NotNode iVisited) {
		_Payload.visitNotNode(iVisited);
		iVisited.getBodyNode().accept(this);
	}

	public void visitNthRefNode(NthRefNode iVisited) {
		_Payload.visitNthRefNode(iVisited);
	}

	public void visitOpAsgn1Node(OpAsgn1Node iVisited) {
		_Payload.visitOpAsgn1Node(iVisited);
	}

	public void visitOpAsgn2Node(OpAsgn2Node iVisited) {
		_Payload.visitOpAsgn2Node(iVisited);
	}

	public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
		_Payload.visitOpAsgnAndNode(iVisited);
	}

	public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
		_Payload.visitOpAsgnOrNode(iVisited);
	}

	public void visitOptNNode(OptNNode iVisited) {
		_Payload.visitOptNNode(iVisited);
		iVisited.getBodyNode().accept(this);
	}

	public void visitOrNode(OrNode iVisited) {
		iVisited.getFirstNode().accept(this);
		_Payload.visitOrNode(iVisited);
		iVisited.getSecondNode().accept(this);
	}

	public void visitPostExeNode(PostExeNode iVisited) {
		_Payload.visitPostExeNode(iVisited);
	}

	public void visitRedoNode(RedoNode iVisited) {
		_Payload.visitRedoNode(iVisited);
	}

	public void visitRescueBodyNode(RescueBodyNode iVisited) {
		_Payload.visitRescueBodyNode(iVisited);
		iVisited.getBodyNode().accept(this);
	}

	public void visitRescueNode(RescueNode iVisited) {
		_Payload.visitRescueNode(iVisited);
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
		_Payload.visitRestArgsNode(iVisited);
	}

	public void visitRetryNode(RetryNode iVisited) {
		_Payload.visitRetryNode(iVisited);
	}

	public void visitReturnNode(ReturnNode iVisited) {
		_Payload.visitReturnNode(iVisited);
	}

	public void visitSClassNode(SClassNode iVisited) {
		_Payload.visitSClassNode(iVisited);
	}

	public void visitScopeNode(ScopeNode iVisited) {
		_Payload.visitScopeNode(iVisited);
		Node lNext = iVisited.getNextNode();
		if (lNext != null) {
			lNext.accept(this);
		}
	}

	public void visitSelfNode(SelfNode iVisited) {
		_Payload.visitSelfNode(iVisited);
	}

	public void visitStrNode(StrNode iVisited) {
		_Payload.visitStrNode(iVisited);
	}

	public void visitSuperNode(SuperNode iVisited) {
		_Payload.visitSuperNode(iVisited);
	}

	public void visitTrueNode(TrueNode iVisited) {
		_Payload.visitTrueNode(iVisited);
	}

	public void visitUndefNode(UndefNode iVisited) {
		_Payload.visitUndefNode(iVisited);
	}

	public void visitUntilNode(UntilNode iVisited) {
		_Payload.visitUntilNode(iVisited);
	}

	public void visitVAliasNode(VAliasNode iVisited) {
		_Payload.visitVAliasNode(iVisited);
	}

	public void visitVCallNode(VCallNode iVisited) {
		_Payload.visitVCallNode(iVisited);
	}

	public void visitWhenNode(WhenNode iVisited) {
		_Payload.visitWhenNode(iVisited);
        iVisited.getConditionNode().accept(this);
        iVisited.getBodyNode().accept(this);
	}

	public void visitWhileNode(WhileNode iVisited) {
		_Payload.visitWhileNode(iVisited);
	}

	public void visitXStrNode(XStrNode iVisited) {
		_Payload.visitXStrNode(iVisited);
	}

	public void visitYieldNode(YieldNode iVisited) {
		_Payload.visitYieldNode(iVisited);
	}

	public void visitZArrayNode(ZArrayNode iVisited) {
		_Payload.visitZArrayNode(iVisited);
	}

	public void visitZSuperNode(ZSuperNode iVisited) {
		_Payload.visitZSuperNode(iVisited);
	}
}


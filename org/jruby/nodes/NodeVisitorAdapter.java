/*
 * NodeVisitorAdapter.java - a default implementation of the NodeVisitor interface
 * Created on 05. November 2001, 21:46
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
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

package org.jruby.nodes;
/**
 * Adapter for the NodeVisitor interface.
 * each visit method is implemented by calling
 * the #visit(Node) method which can be overriden.
 * @see NodeVisitor
 * @author Benoit Cerrina
 * @version $Revision$
 **/
public class NodeVisitorAdapter implements NodeVisitor
{
	boolean _expandBlocks = false;
	/**
	 * Modifies the expandblocks property.
	 * the expandblocks property directs the way the 
	 * visitor visits the blocks, if it is false (the default)
	 * the Blocks will be visited according to their logical
	 * structure (the head block is represented then only the
	 * contents of the link blocks are represented).
	 * if the property is true the physical structure is visited
	 * with each individual block being visited.
	 **/
	public void setExpandBlocks(boolean iExpandBlock)
	{
		_expandBlocks = iExpandBlock;
	}
	protected void visit(Node iVisited)
	{
		visit(iVisited, true);
	}
	protected void visit(Node iVisited, boolean mayLeave) {}
	protected void leave(Node iVisited) {}
	public void visitAliasNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitAndNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitArgsCatNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitArgsNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitArgsPushNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitArrayNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitAttrSetNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitBackRefNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitBeginNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitBlockArgNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitBlockNode(Node iVisited)
	{
		visit(iVisited);
		if (!_expandBlocks)
		{
			for (Node node = iVisited; node != null; node = node.getNextNode())
				node.getHeadNode().accept(this);
		}
		else
		{
			iVisited.getHeadNode().accept(this);
			Node lNext = iVisited.getNextNode();
			if (lNext != null)
				lNext.accept(this);
		}
		

		leave(iVisited);
	}
	public void visitBlockPassNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitBreakNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCDeclNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCFuncNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCRefNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCVAsgnNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCVDeclNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCVar2Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCVarNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCallNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitCaseNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitClassNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitColon2Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitColon3Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitConstNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDAsgnCurrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDAsgnNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDRegxNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDRegxOnceNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDStrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDVarNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDXStrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDefinedNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDefnNode(Node iVisited)
	{
		visit(iVisited);
		iVisited.getDefnNode().accept(this);
		leave(iVisited);
	}
	public void visitDefsNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitDotNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitEnsureNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitEvStrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitFBodyNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitFCallNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitFalseNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitFlip2Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitFlip3Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitForNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitGAsgnNode(Node iVisited)
	{
		visit(iVisited);
		iVisited.getValueNode().accept(this);
		leave(iVisited);
	}
	public void visitGVarNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitHashNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitIAsgnNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitIFuncNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitIVarNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitIfNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitIterNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitLAsgnNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitLVarNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitLitNode(Node iVisited)
	{
		visit(iVisited, false);
	}
	public void visitMAsgnNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitMatch2Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitMatch3Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitMatchNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitMethodNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitModuleNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitNewlineNode(Node iVisited)
	{
		visit(iVisited);
		iVisited.getNextNode().accept(this);
		leave(iVisited);
	}
	public void visitNextNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitNilNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitNodeFactory(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitNotNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitNthRefNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitOpAsgn1Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitOpAsgn2Node(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitOpAsgnAndNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitOpAsgnOrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitOptNNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitOrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitPostExeNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitRedoNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitRescueBodyNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitRescueNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitRestArgsNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitRetryNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitReturnNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitSClassNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitScopeNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitSelfNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitStrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitSuperNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitTrueNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitUndefNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitUntilNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitVAliasNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitVCallNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitWhenNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitWhileNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitXStrNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitYieldNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitZArrayNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
	public void visitZSuperNode(Node iVisited)
	{
		visit(iVisited);
		leave(iVisited);
	}
}

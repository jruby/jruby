/*
 * NodeVisitor.java - a visitor for a node in jruby's AST
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
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * this interface is implemented in the NodeVisitorAdapter which 
 * can be used as a base class when creating a new Visitor.
 * @see NodeVisitorAdapter
 * @author Benoit Cerrina
 * @version $Revision$
 **/
public interface NodeVisitor
{
	public void visitAliasNode(Node iVisited);
	public void visitAndNode(Node iVisited);
	public void visitArgsCatNode(Node iVisited);
	public void visitArgsNode(Node iVisited);
	public void visitArgsPushNode(Node iVisited);
	public void visitArrayNode(Node iVisited);
	public void visitAttrSetNode(Node iVisited);
	public void visitBackRefNode(Node iVisited);
	public void visitBeginNode(Node iVisited);
	public void visitBlockArgNode(Node iVisited);
	public void visitBlockNode(Node iVisited);
	public void visitBlockPassNode(Node iVisited);
	public void visitBreakNode(Node iVisited);
	public void visitCDeclNode(Node iVisited);
	public void visitCFuncNode(Node iVisited);
	public void visitCRefNode(Node iVisited);
	public void visitCVAsgnNode(Node iVisited);
	public void visitCVDeclNode(Node iVisited);
	public void visitCVar2Node(Node iVisited);
	public void visitCVarNode(Node iVisited);
	public void visitCallNode(Node iVisited);
	public void visitCaseNode(Node iVisited);
	public void visitClassNode(Node iVisited);
	public void visitColon2Node(Node iVisited);
	public void visitColon3Node(Node iVisited);
	public void visitConstNode(Node iVisited);
	public void visitDAsgnCurrNode(Node iVisited);
	public void visitDAsgnNode(Node iVisited);
	public void visitDRegxNode(Node iVisited);
	public void visitDRegxOnceNode(Node iVisited);
	public void visitDStrNode(Node iVisited);
	public void visitDVarNode(Node iVisited);
	public void visitDXStrNode(Node iVisited);
	public void visitDefinedNode(Node iVisited);
	public void visitDefnNode(Node iVisited);
	public void visitDefsNode(Node iVisited);
	public void visitDotNode(Node iVisited);
	public void visitEnsureNode(Node iVisited);
	public void visitEvStrNode(Node iVisited);
	public void visitFBodyNode(Node iVisited);
	public void visitFCallNode(Node iVisited);
	public void visitFalseNode(Node iVisited);
	public void visitFlip2Node(Node iVisited);
	public void visitFlip3Node(Node iVisited);
	public void visitForNode(Node iVisited);
	public void visitGAsgnNode(Node iVisited);
	public void visitGVarNode(Node iVisited);
	public void visitHashNode(Node iVisited);
	public void visitIAsgnNode(Node iVisited);
	public void visitIFuncNode(Node iVisited);
	public void visitIVarNode(Node iVisited);
	public void visitIfNode(Node iVisited);
	public void visitIterNode(Node iVisited);
	public void visitLAsgnNode(Node iVisited);
	public void visitLVarNode(Node iVisited);
	public void visitLitNode(Node iVisited);
	public void visitMAsgnNode(Node iVisited);
	public void visitMatch2Node(Node iVisited);
	public void visitMatch3Node(Node iVisited);
	public void visitMatchNode(Node iVisited);
	public void visitMethodNode(Node iVisited);
	public void visitModuleNode(Node iVisited);
	public void visitNewlineNode(Node iVisited);
	public void visitNextNode(Node iVisited);
	public void visitNilNode(Node iVisited);
	public void visitNode(Node iVisited);
	public void visitNodeFactory(Node iVisited);
	public void visitNotNode(Node iVisited);
	public void visitNthRefNode(Node iVisited);
	public void visitOpAsgn1Node(Node iVisited);
	public void visitOpAsgn2Node(Node iVisited);
	public void visitOpAsgnAndNode(Node iVisited);
	public void visitOpAsgnOrNode(Node iVisited);
	public void visitOptNNode(Node iVisited);
	public void visitOrNode(Node iVisited);
	public void visitPostExeNode(Node iVisited);
	public void visitRedoNode(Node iVisited);
	public void visitRescueBodyNode(Node iVisited);
	public void visitRescueNode(Node iVisited);
	public void visitRestArgsNode(Node iVisited);
	public void visitRetryNode(Node iVisited);
	public void visitReturnNode(Node iVisited);
	public void visitSClassNode(Node iVisited);
	public void visitScopeNode(Node iVisited);
	public void visitSelfNode(Node iVisited);
	public void visitStrNode(Node iVisited);
	public void visitSuperNode(Node iVisited);
	public void visitTrueNode(Node iVisited);
	public void visitUndefNode(Node iVisited);
	public void visitUntilNode(Node iVisited);
	public void visitVAliasNode(Node iVisited);
	public void visitVCallNode(Node iVisited);
	public void visitWhenNode(Node iVisited);
	public void visitWhileNode(Node iVisited);
	public void visitXStrNode(Node iVisited);
	public void visitYieldNode(Node iVisited);
	public void visitZArrayNode(Node iVisited);
	public void visitZSuperNode(Node iVisited);
}

/*
 * NodeVisitor.java - a visitor for a node in jruby's AST
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

import org.ablaf.ast.visitor.*;
import org.jruby.ast.*;

/**
 * Visitor interface to be implemented by visitors of the jRuby AST.
 * each node will call the visit method appropriate to its type.
 * this interface is implemented in the NodeVisitorAdapter which 
 * can be used as a base class when creating a new Visitor.
 * @see NodeVisitorAdapter
 * @author Benoit Cerrina
 * @version $Revision$
 **/
public interface NodeVisitor extends INodeVisitor {
    public void visitAliasNode(AliasNode iVisited);
    public void visitAndNode(AndNode iVisited);
    public void visitArgsNode(ArgsNode iVisited);
    public void visitArrayNode(ArrayNode iVisited);
    public void visitAttrSetNode(AttrSetNode iVisited);
    public void visitBackRefNode(BackRefNode iVisited);
    public void visitBeginNode(BeginNode iVisited);
    public void visitBignumNode(BignumNode iVisited);
    public void visitBlockArgNode(BlockArgNode iVisited);
    public void visitBlockNode(BlockNode iVisited);
    public void visitBlockPassNode(BlockPassNode iVisited);
    public void visitBreakNode(BreakNode iVisited);
    public void visitConstDeclNode(ConstDeclNode iVisited);
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited);
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited);
    public void visitClassVarNode(ClassVarNode iVisited);
    public void visitCallNode(CallNode iVisited);
    public void visitCaseNode(CaseNode iVisited);
    public void visitClassNode(ClassNode iVisited);
    public void visitColon2Node(Colon2Node iVisited);
    public void visitColon3Node(Colon3Node iVisited);
    public void visitConstNode(ConstNode iVisited);
    public void visitDAsgnNode(DAsgnNode iVisited);
    public void visitDRegxNode(DRegexpNode iVisited);
    public void visitDStrNode(DStrNode iVisited);
    public void visitDVarNode(DVarNode iVisited);
    public void visitDXStrNode(DXStrNode iVisited);
    public void visitDefinedNode(DefinedNode iVisited);
    public void visitDefnNode(DefnNode iVisited);
    public void visitDefsNode(DefsNode iVisited);
    public void visitDotNode(DotNode iVisited);
    public void visitEnsureNode(EnsureNode iVisited);
    public void visitEvStrNode(EvStrNode iVisited);
    public void visitExpandArrayNode(ExpandArrayNode iVisited);
    public void visitFCallNode(FCallNode iVisited);
    public void visitFalseNode(FalseNode iVisited);
    public void visitFixnumNode(FixnumNode iVisited);
    public void visitFlipNode(FlipNode iVisited);
    public void visitFloatNode(FloatNode iVisited);
    public void visitForNode(ForNode iVisited);
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited);
    public void visitGlobalVarNode(GlobalVarNode iVisited);
    public void visitHashNode(HashNode iVisited);
    public void visitInstAsgnNode(InstAsgnNode iVisited);
    public void visitInstVarNode(InstVarNode iVisited);
    public void visitIfNode(IfNode iVisited);
    public void visitIterNode(IterNode iVisited);
    public void visitLocalAsgnNode(LocalAsgnNode iVisited);
    public void visitLocalVarNode(LocalVarNode iVisited);
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited);
    public void visitMatch2Node(Match2Node iVisited);
    public void visitMatch3Node(Match3Node iVisited);
    public void visitMatchNode(MatchNode iVisited);
    public void visitModuleNode(ModuleNode iVisited);
    public void visitNewlineNode(NewlineNode iVisited);
    public void visitNextNode(NextNode iVisited);
    public void visitNilNode(NilNode iVisited);
    public void visitNotNode(NotNode iVisited);
    public void visitNthRefNode(NthRefNode iVisited);
    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited);
    public void visitOpAsgnNode(OpAsgnNode iVisited);
    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited);
    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited);
    public void visitOptNNode(OptNNode iVisited);
    public void visitOrNode(OrNode iVisited);
    public void visitPostExeNode(PostExeNode iVisited);
    public void visitRedoNode(RedoNode iVisited);
    public void visitRegexpNode(RegexpNode iVisited);
    public void visitRescueBodyNode(RescueBodyNode iVisited);
    public void visitRescueNode(RescueNode iVisited);
    public void visitRestArgsNode(RestArgsNode iVisited);
    public void visitRetryNode(RetryNode iVisited);
    public void visitReturnNode(ReturnNode iVisited);
    public void visitSClassNode(SClassNode iVisited);
    public void visitScopeNode(ScopeNode iVisited);
    public void visitSelfNode(SelfNode iVisited);
    public void visitStrNode(StrNode iVisited);
    public void visitSuperNode(SuperNode iVisited);
    public void visitSymbolNode(SymbolNode iVisited);
    public void visitTrueNode(TrueNode iVisited);
    public void visitUndefNode(UndefNode iVisited);
    public void visitUntilNode(UntilNode iVisited);
    public void visitVAliasNode(VAliasNode iVisited);
    public void visitVCallNode(VCallNode iVisited);
    public void visitWhenNode(WhenNode iVisited);
    public void visitWhileNode(WhileNode iVisited);
    public void visitXStrNode(XStrNode iVisited);
    public void visitYieldNode(YieldNode iVisited);
    public void visitZArrayNode(ZArrayNode iVisited);
    public void visitZSuperNode(ZSuperNode iVisited);
}
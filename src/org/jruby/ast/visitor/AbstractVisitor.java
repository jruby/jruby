/*
 * AbstractVisitor.java - An abstract helper visitor class.
 * Created on 18.02.2002, 17:59:41
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.ablaf.ast.INode;
import org.jruby.ast.*;

/** This visitor calls by default the visitNode method for each visited Node.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractVisitor implements NodeVisitor {
    
    /** This method is called by default for each visited Node.
     * 
     * You have to overwrite this method.
     * 
     */
    protected abstract void visitNode(INode iVisited);

    public void visitNullNode() {
        visitNode(null);
    }
    
    public void acceptNode(INode node) {
        if (node == null) {
            visitNullNode();
        } else {
        	node.accept(this);
        }
    }

    /**
     * @see NodeVisitor#visitAliasNode(AliasNode)
     */
    public void visitAliasNode(AliasNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitAndNode(AndNode)
     */
    public void visitAndNode(AndNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitArgsNode(ArgsNode)
     */
    public void visitArgsNode(ArgsNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitArrayNode(ArrayNode)
     */
    public void visitArrayNode(ArrayNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitAttrSetNode(AttrSetNode)
     */
    public void visitAttrSetNode(AttrSetNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public void visitBackRefNode(BackRefNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitBeginNode(BeginNode)
     */
    public void visitBeginNode(BeginNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitBlockArgNode(BlockArgNode)
     */
    public void visitBlockArgNode(BlockArgNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitBlockNode(BlockNode)
     */
    public void visitBlockNode(BlockNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitBlockPassNode(BlockPassNode)
     */
    public void visitBlockPassNode(BlockPassNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitBreakNode(BreakNode)
     */
    public void visitBreakNode(BreakNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitCDeclNode(CDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitCVAsgnNode(CVAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitCVDeclNode(CVDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitCVarNode(CVarNode)
     */
    public void visitClassVarNode(ClassVarNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitCaseNode(CaseNode)
     */
    public void visitCaseNode(CaseNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitClassNode(ClassNode)
     */
    public void visitClassNode(ClassNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitColon2Node(Colon2Node)
     */
    public void visitColon2Node(Colon2Node iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitColon3Node(Colon3Node)
     */
    public void visitColon3Node(Colon3Node iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitConstNode(ConstNode)
     */
    public void visitConstNode(ConstNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDRegxNode(DRegxNode)
     */
    public void visitDRegxNode(DRegexpNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitDStrNode(DStrNode)
     */
    public void visitDStrNode(DStrNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDVarNode(DVarNode)
     */
    public void visitDVarNode(DVarNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDXStrNode(DXStrNode)
     */
    public void visitDXStrNode(DXStrNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public void visitDefinedNode(DefinedNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDefnNode(DefnNode)
     */
    public void visitDefnNode(DefnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDefsNode(DefsNode)
     */
    public void visitDefsNode(DefsNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitDotNode(DotNode)
     */
    public void visitDotNode(DotNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitEnsureNode(EnsureNode)
     */
    public void visitEnsureNode(EnsureNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitEvStrNode(EvStrNode)
     */
    public void visitEvStrNode(EvStrNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitFCallNode(FCallNode)
     */
    public void visitFCallNode(FCallNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitFalseNode(FalseNode)
     */
    public void visitFalseNode(FalseNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitFlipNode(FlipNode)
     */
    public void visitFlipNode(FlipNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitForNode(ForNode)
     */
    public void visitForNode(ForNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitGAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitGVarNode(GVarNode)
     */
    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitHashNode(HashNode)
     */
    public void visitHashNode(HashNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitIAsgnNode(InstVarAsgnNode)
     */
    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitIVarNode(IVarNode)
     */
    public void visitInstVarNode(InstVarNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitIfNode(IfNode)
     */
    public void visitIfNode(IfNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitIterNode(IterNode)
     */
    public void visitIterNode(IterNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitLAsgnNode(LAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitLVarNode(LocalVarNode)
     */
    public void visitLocalVarNode(LocalVarNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitMAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitMatch2Node(Match2Node)
     */
    public void visitMatch2Node(Match2Node iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitMatch3Node(Match3Node)
     */
    public void visitMatch3Node(Match3Node iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitMatchNode(MatchNode)
     */
    public void visitMatchNode(MatchNode iVisited) {
        visitNode(iVisited);
    }

    
    /**
     * @see NodeVisitor#visitModuleNode(ModuleNode)
     */
    public void visitModuleNode(ModuleNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public void visitNewlineNode(NewlineNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitNextNode(NextNode)
     */
    public void visitNextNode(NextNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitNilNode(NilNode)
     */
    public void visitNilNode(NilNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitNotNode(NotNode)
     */
    public void visitNotNode(NotNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitNthRefNode(NthRefNode)
     */
    public void visitNthRefNode(NthRefNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitOpAsgn1Node(OpAsgn1Node)
     */
    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitOpAsgn2Node(OpAsgn2Node)
     */
    public void visitOpAsgnNode(OpAsgnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitOpAsgnAndNode(OpAsgnAndNode)
     */
    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitOpAsgnOrNode(OpAsgnOrNode)
     */
    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitOptNNode(OptNNode)
     */
    public void visitOptNNode(OptNNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitOrNode(OrNode)
     */
    public void visitOrNode(OrNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitPostExeNode(PostExeNode)
     */
    public void visitPostExeNode(PostExeNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitRedoNode(RedoNode)
     */
    public void visitRedoNode(RedoNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitRescueBodyNode(RescueBodyNode)
     */
    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitRescueNode(RescueNode)
     */
    public void visitRescueNode(RescueNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitRestArgsNode(RestArgsNode)
     */
    public void visitRestArgsNode(RestArgsNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitRetryNode(RetryNode)
     */
    public void visitRetryNode(RetryNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitReturnNode(ReturnNode)
     */
    public void visitReturnNode(ReturnNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitSClassNode(SClassNode)
     */
    public void visitSClassNode(SClassNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitScopeNode(ScopeNode)
     */
    public void visitScopeNode(ScopeNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitSelfNode(SelfNode)
     */
    public void visitSelfNode(SelfNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitStrNode(StrNode)
     */
    public void visitStrNode(StrNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitSuperNode(SuperNode)
     */
    public void visitSuperNode(SuperNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitTrueNode(TrueNode)
     */
    public void visitTrueNode(TrueNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitUndefNode(UndefNode)
     */
    public void visitUndefNode(UndefNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitUntilNode(UntilNode)
     */
    public void visitUntilNode(UntilNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitVAliasNode(VAliasNode)
     */
    public void visitVAliasNode(VAliasNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitVCallNode(VCallNode)
     */
    public void visitVCallNode(VCallNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitWhenNode(WhenNode)
     */
    public void visitWhenNode(WhenNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitWhileNode(WhileNode)
     */
    public void visitWhileNode(WhileNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitXStrNode(XStrNode)
     */
    public void visitXStrNode(XStrNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitYieldNode(YieldNode)
     */
    public void visitYieldNode(YieldNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitZArrayNode(ZArrayNode)
     */
    public void visitZArrayNode(ZArrayNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
        visitNode(iVisited);
    }
    /**
     * @see NodeVisitor#visitBignumNode(BignumNode)
     */
    public void visitBignumNode(BignumNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitFixnumNode(FixnumNode)
     */
    public void visitFixnumNode(FixnumNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitFloatNode(FloatNode)
     */
    public void visitFloatNode(FloatNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitRegexpNode(RegexpNode)
     */
    public void visitRegexpNode(RegexpNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitSymbolNode(SymbolNode iVisited) {
        visitNode(iVisited);
    }

    /**
     * @see NodeVisitor#visitExpandArrayNode(ExpandArrayNode)
     */
    public void visitExpandArrayNode(ExpandArrayNode iVisited) {
        visitNode(iVisited);
    }
}
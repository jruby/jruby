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

import org.jruby.ast.*;

/** This visitor calls by default the visitNode method for each visited Node.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractVisitor implements NodeVisitor {
    
    /**
     * This method is called by default for each visited Node.
     */
    protected abstract void visitNode(Node iVisited);

    public void visitNullNode() {
        visitNode(null);
    }
    
    public void acceptNode(Node node) {
        if (node == null) {
            visitNullNode();
        } else {
        	node.accept(this);
        }
    }

    public void visitAliasNode(AliasNode iVisited) {
        visitNode(iVisited);
    }

    public void visitAndNode(AndNode iVisited) {
        visitNode(iVisited);
    }

    public void visitArgsCatNode(ArgsCatNode iVisited) {
        visitNode(iVisited);
    }

    public void visitArgsNode(ArgsNode iVisited) {
        visitNode(iVisited);
    }

    public void visitArrayNode(ArrayNode iVisited) {
        visitNode(iVisited);
    }

    public void visitAttrSetNode(AttrSetNode iVisited) {
        visitNode(iVisited);
    }

    public void visitBackRefNode(BackRefNode iVisited) {
        visitNode(iVisited);
    }

    public void visitBeginNode(BeginNode iVisited) {
        visitNode(iVisited);
    }

    public void visitBlockArgNode(BlockArgNode iVisited) {
        visitNode(iVisited);
    }

    public void visitBlockNode(BlockNode iVisited) {
        visitNode(iVisited);
    }

    public void visitBlockPassNode(BlockPassNode iVisited) {
        visitNode(iVisited);
    }

    public void visitBreakNode(BreakNode iVisited) {
        visitNode(iVisited);
    }

    public void visitConstDeclNode(ConstDeclNode iVisited) {
        visitNode(iVisited);
    }

    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        visitNode(iVisited);
    }

    public void visitClassVarNode(ClassVarNode iVisited) {
        visitNode(iVisited);
    }

    public void visitCallNode(CallNode iVisited) {
        visitNode(iVisited);
    }

    public void visitCaseNode(CaseNode iVisited) {
        visitNode(iVisited);
    }

    public void visitClassNode(ClassNode iVisited) {
        visitNode(iVisited);
    }

    public void visitColon2Node(Colon2Node iVisited) {
        visitNode(iVisited);
    }

    public void visitColon3Node(Colon3Node iVisited) {
        visitNode(iVisited);
    }

    public void visitConstNode(ConstNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDAsgnNode(DAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDRegxNode(DRegexpNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDStrNode(DStrNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDSymbolNode(DSymbolNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDVarNode(DVarNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDXStrNode(DXStrNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDefinedNode(DefinedNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDefnNode(DefnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDefsNode(DefsNode iVisited) {
        visitNode(iVisited);
    }

    public void visitDotNode(DotNode iVisited) {
        visitNode(iVisited);
    }

    public void visitEnsureNode(EnsureNode iVisited) {
        visitNode(iVisited);
    }

    public void visitEvStrNode(EvStrNode iVisited) {
        visitNode(iVisited);
    }

    public void visitFCallNode(FCallNode iVisited) {
        visitNode(iVisited);
    }

    public void visitFalseNode(FalseNode iVisited) {
        visitNode(iVisited);
    }

    public void visitFlipNode(FlipNode iVisited) {
        visitNode(iVisited);
    }

    public void visitForNode(ForNode iVisited) {
        visitNode(iVisited);
    }

    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        visitNode(iVisited);
    }

    public void visitHashNode(HashNode iVisited) {
        visitNode(iVisited);
    }

    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitInstVarNode(InstVarNode iVisited) {
        visitNode(iVisited);
    }

    public void visitIfNode(IfNode iVisited) {
        visitNode(iVisited);
    }

    public void visitIterNode(IterNode iVisited) {
        visitNode(iVisited);
    }

    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitLocalVarNode(LocalVarNode iVisited) {
        visitNode(iVisited);
    }

    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitMatch2Node(Match2Node iVisited) {
        visitNode(iVisited);
    }

    public void visitMatch3Node(Match3Node iVisited) {
        visitNode(iVisited);
    }

    public void visitMatchNode(MatchNode iVisited) {
        visitNode(iVisited);
    }

    public void visitModuleNode(ModuleNode iVisited) {
        visitNode(iVisited);
    }

    public void visitNewlineNode(NewlineNode iVisited) {
        visitNode(iVisited);
    }

    public void visitNextNode(NextNode iVisited) {
        visitNode(iVisited);
    }

    public void visitNilNode(NilNode iVisited) {
        visitNode(iVisited);
    }

    public void visitNotNode(NotNode iVisited) {
        visitNode(iVisited);
    }

    public void visitNthRefNode(NthRefNode iVisited) {
        visitNode(iVisited);
    }

    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitOpAsgnNode(OpAsgnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        visitNode(iVisited);
    }

    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        visitNode(iVisited);
    }

    public void visitOptNNode(OptNNode iVisited) {
        visitNode(iVisited);
    }

    public void visitOrNode(OrNode iVisited) {
        visitNode(iVisited);
    }

    public void visitPostExeNode(PostExeNode iVisited) {
        visitNode(iVisited);
    }

    public void visitRedoNode(RedoNode iVisited) {
        visitNode(iVisited);
    }

    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        visitNode(iVisited);
    }

    public void visitRescueNode(RescueNode iVisited) {
        visitNode(iVisited);
    }

    public void visitRetryNode(RetryNode iVisited) {
        visitNode(iVisited);
    }

    public void visitReturnNode(ReturnNode iVisited) {
        visitNode(iVisited);
    }

    public void visitSClassNode(SClassNode iVisited) {
        visitNode(iVisited);
    }

    public void visitScopeNode(ScopeNode iVisited) {
        visitNode(iVisited);
    }

    public void visitSelfNode(SelfNode iVisited) {
        visitNode(iVisited);
    }
    
    public void visitSplatNode(SplatNode iVisited) {
        visitNode(iVisited);
    }

    public void visitStrNode(StrNode iVisited) {
        visitNode(iVisited);
    }

    public void visitSValueNode(SValueNode iVisited) {
        visitNode(iVisited);
    }

    public void visitSuperNode(SuperNode iVisited) {
        visitNode(iVisited);
    }

    public void visitToAryNode(ToAryNode iVisited) {
        visitNode(iVisited);
    }

    public void visitTrueNode(TrueNode iVisited) {
        visitNode(iVisited);
    }

    public void visitUndefNode(UndefNode iVisited) {
        visitNode(iVisited);
    }

    public void visitUntilNode(UntilNode iVisited) {
        visitNode(iVisited);
    }

    public void visitVAliasNode(VAliasNode iVisited) {
        visitNode(iVisited);
    }

    public void visitVCallNode(VCallNode iVisited) {
        visitNode(iVisited);
    }

    public void visitWhenNode(WhenNode iVisited) {
        visitNode(iVisited);
    }

    public void visitWhileNode(WhileNode iVisited) {
        visitNode(iVisited);
    }

    public void visitXStrNode(XStrNode iVisited) {
        visitNode(iVisited);
    }

    public void visitYieldNode(YieldNode iVisited) {
        visitNode(iVisited);
    }

    public void visitZArrayNode(ZArrayNode iVisited) {
        visitNode(iVisited);
    }

    public void visitZSuperNode(ZSuperNode iVisited) {
        visitNode(iVisited);
    }
    
    public void visitBignumNode(BignumNode iVisited) {
        visitNode(iVisited);
    }

    public void visitFixnumNode(FixnumNode iVisited) {
        visitNode(iVisited);
    }

    public void visitFloatNode(FloatNode iVisited) {
        visitNode(iVisited);
    }

    public void visitRegexpNode(RegexpNode iVisited) {
        visitNode(iVisited);
    }

    public void visitSymbolNode(SymbolNode iVisited) {
        visitNode(iVisited);
    }
}

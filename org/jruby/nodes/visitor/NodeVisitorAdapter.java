/*
 * NodeVisitorAdapter.java - a default implementation of the NodeVisitor interface
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
 * Adapter for the NodeVisitor interface.
 * each visit method is implemented by calling
 * the #visit(Node) method which can be overriden.
 * 
 * @see NodeVisitor
 * @author Benoit Cerrina
 * @version $Revision: 1.4 $
 **/
public class NodeVisitorAdapter implements NodeVisitor {
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
    public void setExpandBlocks(boolean iExpandBlock) {
        _expandBlocks = iExpandBlock;
    }

    protected void visit(Node iVisited) {
        visit(iVisited, true);
    }

    protected void visit(Node iVisited, boolean mayLeave) {
    }

    protected void leave(Node iVisited) {
    }

    public void visitAliasNode(AliasNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitAndNode(AndNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitArgsCatNode(ArgsCatNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitArgsNode(ArgsNode iVisited) {
        visit(iVisited);
        Node lOptNode = iVisited.getOptNode();
        if (lOptNode != null) {
            lOptNode.accept(this);
        }

        leave(iVisited);
    }

    public void visitArgsPushNode(ArgsPushNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitArrayNode(ArrayNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitAttrSetNode(AttrSetNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitBackRefNode(BackRefNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitBeginNode(BeginNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitBlockArgNode(BlockArgNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitBlockNode(BlockNode iVisited) {
        visit(iVisited);
        if (!_expandBlocks) {
            for (Node node = iVisited; node != null; node = node.getNextNode()) {
                node.getHeadNode().accept(this);
            }
        } else {
            iVisited.getHeadNode().accept(this);
            Node lNext = iVisited.getNextNode();
            if (lNext != null) {
                lNext.accept(this);
            }
        }
        leave(iVisited);
    }

    public void visitBlockPassNode(BlockPassNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitBreakNode(BreakNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitCDeclNode(CDeclNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitCFuncNode(CFuncNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    /**
     * TODO: delete
     */
    public void visitCRefNode(CRefNode iVisited) {
        visit(iVisited);
        Node lNext = iVisited.getNextNode();
        if (lNext != null) {
            lNext.accept(this);
        }
        leave(iVisited);
    }

    public void visitCVAsgnNode(CVAsgnNode iVisited) {
        visit(iVisited);
        iVisited.getValueNode().accept(this);
        leave(iVisited);
    }

    public void visitCVDeclNode(CVDeclNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitCVar2Node(CVar2Node iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitCVarNode(CVarNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitCallNode(CallNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitCaseNode(CaseNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitClassNode(ClassNode iVisited) {
        visit(iVisited);
        //FIXME not done yet just quick testing
        iVisited.getBodyNode().accept(this);
        leave(iVisited);
    }

    public void visitColon2Node(Colon2Node iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitColon3Node(Colon3Node iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitConstNode(ConstNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDAsgnCurrNode(DAsgnCurrNode iVisited) {
        visit(iVisited);
        iVisited.getValueNode().accept(this);
        leave(iVisited);
    }

    public void visitDAsgnNode(DAsgnNode iVisited) {
        visit(iVisited);
        iVisited.getValueNode().accept(this);
        leave(iVisited);
    }

    public void visitDRegxNode(DRegxNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDRegxOnceNode(DRegxOnceNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDStrNode(DStrNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDVarNode(DVarNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDXStrNode(DXStrNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDefinedNode(DefinedNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDefnNode(DefnNode iVisited) {
        visit(iVisited);
        iVisited.getDefnNode().accept(this);
        leave(iVisited);
    }

    public void visitDefsNode(DefsNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitDotNode(DotNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitEnsureNode(EnsureNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitEvStrNode(EvStrNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitFBodyNode(FBodyNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitFCallNode(FCallNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitFalseNode(FalseNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitFlipNode(FlipNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitForNode(ForNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitGAsgnNode(GAsgnNode iVisited) {
        visit(iVisited);
        iVisited.getValueNode().accept(this);
        leave(iVisited);
    }

    public void visitGVarNode(GVarNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitHashNode(HashNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitIAsgnNode(IAsgnNode iVisited) {
        visit(iVisited);
        iVisited.getValueNode().accept(this);
        leave(iVisited);
    }

    public void visitIFuncNode(IFuncNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitIVarNode(IVarNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitIfNode(IfNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitIterNode(IterNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitLAsgnNode(LAsgnNode iVisited) {
        visit(iVisited);
        iVisited.getValueNode().accept(this);
        leave(iVisited);
    }

    public void visitLVarNode(LVarNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitLitNode(LitNode iVisited) {
        visit(iVisited, false);
        leave(iVisited);
    }

    public void visitMAsgnNode(MAsgnNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitMatch2Node(Match2Node iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitMatch3Node(Match3Node iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitMatchNode(MatchNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitMethodNode(MethodNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitModuleNode(ModuleNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitNewlineNode(NewlineNode iVisited) {
        visit(iVisited);
        iVisited.getNextNode().accept(this);
        leave(iVisited);
    }

    public void visitNextNode(NextNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitNilNode(NilNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitNotNode(NotNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitNthRefNode(NthRefNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitOpAsgn1Node(OpAsgn1Node iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitOpAsgn2Node(OpAsgn2Node iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitOptNNode(OptNNode iVisited) {
        visit(iVisited);
        iVisited.getBodyNode().accept(this);
        leave(iVisited);
    }

    public void visitOrNode(OrNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitPostExeNode(PostExeNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitRedoNode(RedoNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitRescueNode(RescueNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitRestArgsNode(RestArgsNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitRetryNode(RetryNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitReturnNode(ReturnNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitSClassNode(SClassNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitScopeNode(ScopeNode iVisited) {
        visit(iVisited);
        Node lNext = iVisited.getNextNode();
        if (lNext != null) {
            lNext.accept(this);
        }
        leave(iVisited);
    }

    public void visitSelfNode(SelfNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitStrNode(StrNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitSuperNode(SuperNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitTrueNode(TrueNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitUndefNode(UndefNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitUntilNode(UntilNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitVAliasNode(VAliasNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitVCallNode(VCallNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitWhenNode(WhenNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitWhileNode(WhileNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitXStrNode(XStrNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitYieldNode(YieldNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitZArrayNode(ZArrayNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }

    public void visitZSuperNode(ZSuperNode iVisited) {
        visit(iVisited);
        leave(iVisited);
    }
}
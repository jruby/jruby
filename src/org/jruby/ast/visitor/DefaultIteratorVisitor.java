/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast.visitor;

import java.util.Iterator;

import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.evaluator.Instruction;

/**
 * Default iterator visitor. This visitor will iterate over all the nodes using
 * the semantic which is used when compiling or interpreting the tree. This
 * visitor will then used call the accept method for each node with its payload
 * as the visitor to accept.
 * 
 * @see NodeVisitor
 * @author Benoit Cerrina
 */
public class DefaultIteratorVisitor implements NodeVisitor {
	protected NodeVisitor _Payload;

	/**
	 * Constructs a DefaultIteratorVisitor. The payload visitor will be accepted
	 * by each node wich the IteratorVisitor iterates over.
	 * 
	 * @param iPayload
	 *            the payload for this visitor
	 */
	public DefaultIteratorVisitor(NodeVisitor iPayload) {
		_Payload = iPayload;
	}

	public Instruction visitAliasNode(AliasNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitAndNode(AndNode iVisited) {
		iVisited.getFirstNode().accept(this);
		iVisited.accept(_Payload);
		iVisited.getSecondNode().accept(this);
		return null;
	}

	public Instruction visitArgsNode(ArgsNode iVisited) {
		iVisited.accept(_Payload);
		if (iVisited.getOptArgs() != null) {
			iVisited.getOptArgs().accept(this);
		}
		return null;
	}

	// XXXEnebo - Just guessed.
	public Instruction visitArgsCatNode(ArgsCatNode iVisited) {
		iVisited.accept(_Payload);
		if (iVisited.getFirstNode() != null) {
			iVisited.getFirstNode().accept(this);
		}
		if (iVisited.getSecondNode() != null) {
			iVisited.getSecondNode().accept(this);
		}
		return null;
	}

	public Instruction visitArrayNode(ArrayNode iVisited) {
		iVisited.accept(_Payload);
		Iterator iterator = iVisited.iterator();
		while (iterator.hasNext()) {
			((Node) iterator.next()).accept(this);
		}

		return null;
	}

	public Instruction visitBackRefNode(BackRefNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitBeginNode(BeginNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitBlockArgNode(BlockArgNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitBlockNode(BlockNode iVisited) {
		iVisited.accept(_Payload);
		Iterator iterator = iVisited.iterator();
		while (iterator.hasNext()) {
			((Node) iterator.next()).accept(this);
		}
		return null;
	}

	public Instruction visitBlockPassNode(BlockPassNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitBreakNode(BreakNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
		return null;
	}

	public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
		return null;
	}

	public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitClassVarNode(ClassVarNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/**
	 * @fixme iteration not correctly defined
	 */
	public Instruction visitCallNode(CallNode iVisited) {
		iVisited.getReceiverNode().accept(this);
		//  FIXME
		/*
		 * for (Node node = iVisited.getArgsNode(); node != null; node =
		 * node.getNextNode()) { node.getHeadNode().accept(this); }
		 */
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitCaseNode(CaseNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitClassNode(ClassNode iVisited) {
		iVisited.accept(_Payload);
		if (iVisited.getSuperNode() != null) {
			iVisited.getSuperNode().accept(this);
		}
		//NOTE: suprised that this is not used
		// It can be used.
		iVisited.getBodyNode().accept(this);
		return null;
	}

	public Instruction visitColon2Node(Colon2Node iVisited) {
		if (iVisited.getLeftNode() != null) {
			iVisited.getLeftNode().accept(this);
		}
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitColon3Node(Colon3Node iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitConstNode(ConstNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitDAsgnNode(DAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
		return null;
	}

	public Instruction visitDRegxNode(DRegexpNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitDStrNode(DStrNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/**
	 * @see NodeVisitor#visitDSymbolNode(DSymbolNode)
	 */
	public Instruction visitDSymbolNode(DSymbolNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitDVarNode(DVarNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitDXStrNode(DXStrNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitDefinedNode(DefinedNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitDefnNode(DefnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getBodyNode().accept(this);
		return null;
	}

	public Instruction visitDefsNode(DefsNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getReceiverNode().accept(this);
		iVisited.getBodyNode().accept(this);
		return null;
	}

	public Instruction visitDotNode(DotNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitEnsureNode(EnsureNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitEvStrNode(EvStrNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/** @fixme iteration not correctly defined */
	public Instruction visitFCallNode(FCallNode iVisited) {
		iVisited.accept(_Payload);
		// FIXME
		/*
		 * for (Node node = iVisited.getArgsNode(); node != null; node =
		 * node.getNextNode()) { node.getHeadNode().accept(this); }
		 */
		return null;
	}

	public Instruction visitFalseNode(FalseNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitFlipNode(FlipNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitForNode(ForNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
		return null;
	}

	public Instruction visitGlobalVarNode(GlobalVarNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitHashNode(HashNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
		return null;
	}

	public Instruction visitInstVarNode(InstVarNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitIfNode(IfNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getCondition().accept(this);
		iVisited.getThenBody().accept(this);
		if (iVisited.getElseBody() != null) {
			iVisited.getElseBody().accept(this);
		}
		return null;
	}

	public Instruction visitIterNode(IterNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getValueNode().accept(this);
		return null;
	}

	public Instruction visitLocalVarNode(LocalVarNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitMatch2Node(Match2Node iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitMatch3Node(Match3Node iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitMatchNode(MatchNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitModuleNode(ModuleNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getBodyNode().accept(this);
		return null;
	}

	public Instruction visitNewlineNode(NewlineNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getNextNode().accept(this);
		return null;
	}

	public Instruction visitNextNode(NextNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitNilNode(NilNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitNotNode(NotNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getConditionNode().accept(this);
		return null;
	}

	public Instruction visitNthRefNode(NthRefNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitOptNNode(OptNNode iVisited) {
		iVisited.accept(_Payload);
		iVisited.getBodyNode().accept(this);
		return null;
	}

	public Instruction visitOrNode(OrNode iVisited) {
		iVisited.getFirstNode().accept(this);
		iVisited.accept(_Payload);
		iVisited.getSecondNode().accept(this);
		return null;
	}

	public Instruction visitPostExeNode(PostExeNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitRedoNode(RedoNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitRescueBodyNode(RescueBodyNode iVisited) {
		iVisited.accept(_Payload);
		// XXX iVisited.getBodyNode().accept(this);
		return null;
	}

	public Instruction visitRescueNode(RescueNode iVisited) {
		iVisited.accept(_Payload);
		/*
		 * XXX iVisited.getHeadNode().accept(this); Node lElseNode =
		 * iVisited.getElseNode(); if (lElseNode != null)
		 * lElseNode.accept(this); for (Node body = iVisited.getResqNode(); body !=
		 * null; body = iVisited.getHeadNode()) { Node lArgsNode =
		 * body.getArgsNode(); for (; lArgsNode != null; lArgsNode =
		 * lArgsNode.getNextNode()) lArgsNode.getHeadNode().accept(this);
		 * body.accept(this); }
		 */
		return null;
	}

	public Instruction visitRetryNode(RetryNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitReturnNode(ReturnNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitSClassNode(SClassNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitScopeNode(ScopeNode iVisited) {
		iVisited.accept(_Payload);
		if (iVisited.getBodyNode() != null) {
			iVisited.getBodyNode().accept(this);
		}
		return null;
	}

	public Instruction visitSelfNode(SelfNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitSplatNode(SplatNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitStrNode(StrNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitSValueNode(SValueNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitSuperNode(SuperNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitToAryNode(ToAryNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitTrueNode(TrueNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitUndefNode(UndefNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitUntilNode(UntilNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitVAliasNode(VAliasNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitVCallNode(VCallNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitWhenNode(WhenNode iVisited) {
		iVisited.accept(_Payload);
		/*
		 * XXX iVisited.getConditionNode().accept(this);
		 * iVisited.getBodyNode().accept(this);
		 */
		return null;
	}

	public Instruction visitWhileNode(WhileNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitXStrNode(XStrNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitYieldNode(YieldNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitZArrayNode(ZArrayNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	public Instruction visitZSuperNode(ZSuperNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/**
	 * @see NodeVisitor#visitBignumNode(BignumNode)
	 */
	public Instruction visitBignumNode(BignumNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/**
	 * @see NodeVisitor#visitFixnumNode(FixnumNode)
	 */
	public Instruction visitFixnumNode(FixnumNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/**
	 * @see NodeVisitor#visitFloatNode(FloatNode)
	 */
	public Instruction visitFloatNode(FloatNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/**
	 * @see NodeVisitor#visitRegexpNode(RegexpNode)
	 */
	public Instruction visitRegexpNode(RegexpNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}

	/**
	 * @see NodeVisitor#visitSymbolNode(SymbolNode)
	 */
	public Instruction visitSymbolNode(SymbolNode iVisited) {
		iVisited.accept(_Payload);
		return null;
	}
}
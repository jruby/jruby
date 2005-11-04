/*******************************************************************************
 * BEGIN LICENSE BLOCK *** Version: CPL 1.0/GPL 2.0/LGPL 2.1
 * 
 * The contents of this file are subject to the Common Public License Version
 * 1.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>Copyright (C)
 * 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"), in
 * which case the provisions of the GPL or the LGPL are applicable instead of
 * those above. If you wish to allow use of your version of this file only under
 * the terms of either the GPL or the LGPL, and not to allow others to use your
 * version of this file under the terms of the CPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and other
 * provisions required by the GPL or the LGPL. If you do not delete the
 * provisions above, a recipient may use your version of this file under the
 * terms of any one of the CPL, the GPL or the LGPL. END LICENSE BLOCK ****
 ******************************************************************************/
package org.jruby.evaluator;

import java.util.Iterator;

import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.visitor.AbstractVisitor;

public class CreateJumpTargetVisitor extends AbstractVisitor {
	private Object target;

	public CreateJumpTargetVisitor(Object target) {
		this.target = target;
	}

	private Instruction visit(Node node) {
		if (node != null) {
			node.accept(this);
		}
		return null;
	}

	public Instruction visitAndNode(AndNode iVisited) {
		visit(iVisited.getFirstNode());
		visit(iVisited.getSecondNode());
		return null;
	}

	public Instruction visitArgsCatNode(ArgsCatNode iVisited) {
		visit(iVisited.getFirstNode());
		visit(iVisited.getSecondNode());
		return null;
	}

	public Instruction visitArrayNode(ArrayNode iVisited) {
		Iterator iterator = iVisited.iterator();
		while (iterator.hasNext()) {
			visit((Node) iterator.next());
		}

		return null;
	}

	public Instruction visitBeginNode(BeginNode iVisited) {
		// FIXME
		visit(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitBlockNode(BlockNode iVisited) {
		Iterator iterator = iVisited.iterator();
		while (iterator.hasNext()) {
			visit((Node) iterator.next());
		}
		return null;
	}

	public Instruction visitBlockPassNode(BlockPassNode iVisited) {
		visit(iVisited.getBodyNode());
		visit(iVisited.getIterNode());
		return null;
	}

	public Instruction visitBreakNode(BreakNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitCallNode(CallNode iVisited) {
		// FIXME
		visit(iVisited.getReceiverNode());
		return null;
	}

	public Instruction visitCaseNode(CaseNode iVisited) {
		visit(iVisited.getCaseNode());
		visit(iVisited.getFirstWhenNode());
		return null;
	}

	public Instruction visitClassNode(ClassNode iVisited) {
		// FIXME
		visit(iVisited.getSuperNode());
		visit(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitColon2Node(Colon2Node iVisited) {
		visit(iVisited.getLeftNode());
		return null;
	}

	public Instruction visitDAsgnNode(DAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitDRegxNode(DRegexpNode iVisited) {
		// FIXME
		Iterator iterator = iVisited.iterator();
		while (iterator.hasNext()) {
			visit((Node) iterator.next());
		}
		return null;
	}

	public Instruction visitDStrNode(DStrNode iVisited) {
		// FIXME
		Iterator iterator = iVisited.iterator();
		while (iterator.hasNext()) {
			visit((Node) iterator.next());
		}
		return null;
	}

	public Instruction visitDSymbolNode(DSymbolNode iVisited) {
		// FIXME
		Iterator iterator = iVisited.getNode().iterator();
		while (iterator.hasNext()) {
			visit((Node) iterator.next());
		}
		return null;
	}

	public Instruction visitDXStrNode(DXStrNode iVisited) {
		// FIXME
		Iterator iterator = iVisited.iterator();
		while (iterator.hasNext()) {
			visit((Node) iterator.next());
		}
		return null;
	}

	public Instruction visitDefinedNode(DefinedNode iVisited) {
		// FIXME
		visit(iVisited.getExpressionNode());
		return null;
	}

	public Instruction visitDotNode(DotNode iVisited) {
		visit(iVisited.getBeginNode());
		visit(iVisited.getEndNode());
		return null;
	}

	public Instruction visitEnsureNode(EnsureNode iVisited) {
		visit(iVisited.getBodyNode());
		visit(iVisited.getEnsureNode());
		return null;
	}

	public Instruction visitEvStrNode(EvStrNode iVisited) {
		visit(iVisited.getBody());
		return null;
	}

	public Instruction visitFlipNode(FlipNode iVisited) {
		visit(iVisited.getBeginNode());
		visit(iVisited.getEndNode());
		return null;
	}

	public Instruction visitForNode(ForNode iVisited) {
		visit(iVisited.getBodyNode());
		visit(iVisited.getIterNode());
		// FIXME
		visit(iVisited.getVarNode());
		return null;
	}

	public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitHashNode(HashNode iVisited) {
		// FIXME
		if (iVisited.getListNode() != null) {
			Iterator iterator = iVisited.getListNode().iterator();
			while (iterator.hasNext()) {
				visit((Node) iterator.next());
			}
		}
		return null;
	}

	public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitIfNode(IfNode iVisited) {
		// FIXME
		visit(iVisited.getCondition());
		visit(iVisited.getThenBody());
		visit(iVisited.getElseBody());
		return null;
	}

	public Instruction visitIterNode(IterNode iVisited) {
		visit(iVisited.getBodyNode());
		visit(iVisited.getIterNode());
		// FIXME
		visit(iVisited.getVarNode());
		return null;
	}

	public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitMatch2Node(Match2Node iVisited) {
		// FIXME
		visit(iVisited.getReceiverNode());
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitMatch3Node(Match3Node iVisited) {
		// FIXME
		visit(iVisited.getReceiverNode());
		// FIXME
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitMatchNode(MatchNode iVisited) {
		visit(iVisited.getRegexpNode());
		return null;
	}

	public Instruction visitModuleNode(ModuleNode iVisited) {
		visit(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitNewlineNode(NewlineNode iVisited) {
		visit(iVisited.getNextNode());
		return null;
	}

	public Instruction visitNotNode(NotNode iVisited) {
		// FIXME
		visit(iVisited.getConditionNode());
		return null;
	}

	public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getReceiverNode());
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
		// FIXME
		visit(iVisited.getReceiverNode());
		visit(iVisited.getValueNode());
		return null;
	}

	public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
		visit(iVisited.getFirstNode());
		visit(iVisited.getSecondNode());
		return null;
	}

	public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
		visit(iVisited.getFirstNode());
		visit(iVisited.getSecondNode());
		return null;
	}

	public Instruction visitOptNNode(OptNNode iVisited) {
		visit(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitOrNode(OrNode iVisited) {
		visit(iVisited.getFirstNode());
		visit(iVisited.getSecondNode());
		return null;
	}

	public Instruction visitRescueBodyNode(RescueBodyNode iVisited) {
		visit(iVisited.getBodyNode());
		visit(iVisited.getExceptionNodes());
		visit(iVisited.getOptRescueNode());
		return null;
	}

	public Instruction visitRescueNode(RescueNode iVisited) {
		visit(iVisited.getBodyNode());
		visit(iVisited.getElseNode());
		visit(iVisited.getRescueNode());
		return null;
	}

	public Instruction visitReturnNode(ReturnNode iVisited) {
		iVisited.setTarget(target);
		return null;
	}

	public Instruction visitSClassNode(SClassNode iVisited) {
		// FIXME
		visit(iVisited.getReceiverNode());
		visit(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitScopeNode(ScopeNode iVisited) {
		visit(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitSplatNode(SplatNode iVisited) {
		visit(iVisited.getValue());
		return null;
	}

	public Instruction visitSValueNode(SValueNode iVisited) {
		// FIXME
		visit(iVisited.getValue());
		return null;
	}

	public Instruction visitToAryNode(ToAryNode iVisited) {
		// FIXME
		visit(iVisited.getValue());
		return null;
	}

	public Instruction visitUntilNode(UntilNode iVisited) {
		// FIXME
		visit(iVisited.getConditionNode());
		visit(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitWhenNode(WhenNode iVisited) {
		visit(iVisited.getBodyNode());
		visit(iVisited.getExpressionNodes());
		visit(iVisited.getNextCase());
		return null;
	}

	public Instruction visitWhileNode(WhileNode iVisited) {
		// FIXME
		visit(iVisited.getConditionNode());
		visit(iVisited.getBodyNode());
		return null;
	}

	protected Instruction visitNode(Node iVisited) {
		// do nothing
		return null;
	}
}